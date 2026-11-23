import copy
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

from convert_sqlmap_to_benchmark import convert, scanned_targets

FIXTURES = Path(__file__).resolve().parents[1] / 'fixtures'
SCRIPTS = Path(__file__).resolve().parent
URL = 'http://127.0.0.1:19090/VulnerableApp/UnionBasedSQLInjectionVulnerability/LEVEL_1'
LAB = 'http://h/VulnerableApp/ErrorBasedSQLInjectionVulnerability/'


def report(name='positive'):
    return json.loads((FIXTURES / (name + '.json')).read_text())


class ConverterTest(unittest.TestCase):
    def test_native_confirmed_injection_is_translated_without_a_runner_manifest(self):
        self.assertEqual(convert(report()), {
            'tool': 'sqlmap', 'scanType': 'DAST',
            'findings': [{'url': URL, 'method': 'GET', 'cwe': 'CWE-89'}]})

    def test_multiple_parameters_are_left_for_the_comparator_to_deduplicate(self):
        raw = report()
        raw['data'][1]['value'].append(copy.deepcopy(raw['data'][1]['value'][0]))
        raw['data'][1]['value'][1]['parameter'] = 'other'
        self.assertEqual(convert(raw)['findings'], [
            {'url': URL, 'method': 'GET', 'cwe': 'CWE-89'},
            {'url': URL, 'method': 'GET', 'cwe': 'CWE-89'}])

    def test_bulk_report_uses_each_targets_native_identity(self):
        first, second = report(), report()
        second['data'][0]['value']['url'] = 'http://lab/another?id=2'
        second['data'][0]['value']['data'] = 'search=test'
        raw = {'success': True, 'targets': [first, second], 'meta': second['meta']}
        self.assertEqual(convert(raw)['findings'], [
            {'url': URL, 'method': 'GET', 'cwe': 'CWE-89'},
            {'url': 'http://lab/another', 'method': 'POST', 'cwe': 'CWE-89'}])

    def test_query_and_fragment_are_dropped_from_the_finding_url(self):
        for url in ['https://example.invalid/page#details',
                    'https://example.invalid/page?view=a%2Fb&empty=#details']:
            raw = report()
            raw['data'][0]['value'].update(url=url, query='view=a%2Fb')
            with self.subTest(url=url):
                finding, = convert(raw)['findings']
                self.assertEqual(finding['url'], 'https://example.invalid/page')

    def test_completed_native_negative_is_empty(self):
        self.assertEqual(convert(report('negative'))['findings'], [])

    def test_transport_error_is_not_a_clean_negative(self):
        raw = report('negative')
        raw['error'] = ['unable to connect to the target URL']
        with self.assertRaises(ValueError):
            convert(raw)

    def test_partial_report_is_not_submitted(self):
        raw = report()
        raw['data'][1]['status'] = 0
        with self.assertRaises(ValueError):
            convert(raw)

    def test_malformed_technique_data_is_not_a_confirmed_injection(self):
        for data in [[], 'unfinished', {'technique': 'unfinished'}, [None], [{}],
                     [{'technique': ''}], [{'technique': ' '}], [{'technique': 1}]]:
            raw = report()
            raw['data'][1]['value'][0]['data'] = data
            with self.subTest(data=data), self.assertRaises(ValueError):
                convert(raw)

    def test_empty_or_missing_report_cannot_claim_a_completed_scan(self):
        for raw in [None, {}, {'success': True, 'data': [], 'error': []},
                    {'success': True, 'targets': []}]:
            with self.subTest(raw=raw), self.assertRaises(ValueError):
                convert(raw)

    def test_converter_cli_writes_findings_and_fails_without_output_on_error(self):
        with tempfile.TemporaryDirectory() as directory:
            source, output = Path(directory) / 'report.json', Path(directory) / 'findings.json'
            source.write_text(json.dumps(report()))
            command = [sys.executable, str(SCRIPTS / 'convert_sqlmap_to_benchmark.py'),
                       '--input', str(source), '--output', str(output)]
            result = subprocess.run(command, capture_output=True)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertEqual(json.loads(output.read_text())['findings'][0]['url'], URL)
            output.unlink()
            source.write_text(json.dumps({'success': False}))
            result = subprocess.run(command, capture_output=True)
            self.assertNotEqual(result.returncode, 0)
            self.assertFalse(output.exists())

    def test_offline_cli_rejects_missing_entries_before_writing_output(self):
        first, second = report(), report('negative')
        second['data'][0]['value']['url'] = 'https://example.invalid/another'
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / 'report.json'
            output = Path(directory) / 'findings.json'
            expected = Path(directory) / 'expected-urls.txt'
            expected.write_text(URL + '?id=1\nhttps://example.invalid/another?id=1\n')
            command = [sys.executable, str(SCRIPTS / 'convert_sqlmap_to_benchmark.py'),
                       '--input', str(source), '--output', str(output), '--expected-urls', str(expected)]
            source.write_text(json.dumps({'success': True, 'targets': [second, first]}))
            result = subprocess.run(command, capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertEqual(len(json.loads(output.read_text())['findings']), 1)
            output.unlink()

            for targets in ([first], [first, first]):
                with self.subTest(entries=len(targets)):
                    source.write_text(json.dumps({'success': True, 'targets': targets}))
                    result = subprocess.run(command, capture_output=True, text=True)
                    self.assertNotEqual(result.returncode, 0)
                    self.assertIn('Missing expected report entries: 1', result.stderr)
                    self.assertFalse(output.exists())


    def test_expected_targets_follow_sqlmap_bulk_loading(self):
        lines = [LAB + 'LEVEL_1?id=1', 'http://h/VulnerableApp/XSSVulnerability/LEVEL_1?id=1', LAB + 'LEVEL_1?id=1',
                 LAB + 'LEVEL_3', '  ' + LAB + 'LEVEL_4?id=1#note  ', '', '# ' + LAB + 'LEVEL_5?id=1']
        self.assertEqual(scanned_targets(lines), [
            LAB + 'LEVEL_1?id=1', 'http://h/VulnerableApp/XSSVulnerability/LEVEL_1?id=1', LAB + 'LEVEL_4?id=1'])

    def test_expected_urls_must_not_repeat_a_path(self):
        raw = {'success': True, 'targets': [report()]}
        self.assertEqual(len(convert(raw, expected_urls=[URL + '?id=1'])['findings']), 1)
        with self.assertRaisesRegex(ValueError, 'Expected URLs repeat a path'):
            convert(raw, expected_urls=[URL + '?id=1', URL + '?id=2'])
        with self.assertRaisesRegex(ValueError, 'Unexpected report entries: 1'):
            convert({'success': True, 'targets': [report(), report()]}, expected_urls=[URL + '?id=1'])

    def test_cli_checks_the_report_against_the_bulk_file(self):
        with tempfile.TemporaryDirectory() as directory:
            source, output = Path(directory) / 'report.json', Path(directory) / 'findings.json'
            targets = Path(directory) / 'targets.txt'
            source.write_text(json.dumps({'success': True, 'targets': [report()]}))
            command = [sys.executable, str(SCRIPTS / 'convert_sqlmap_to_benchmark.py'), '--input', str(source),
                       '--output', str(output), '--expected-urls', str(targets)]
            targets.write_text(URL + '?id=1\n# ' + LAB + 'LEVEL_1?id=1\n' + LAB + 'LEVEL_2\n')
            result = subprocess.run(command, capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)
            output.unlink()
            targets.write_text(URL + '?id=1\n' + LAB + 'LEVEL_2?id=1\n')
            result = subprocess.run(command, capture_output=True, text=True)
            self.assertIn('Missing expected report entries: 1', result.stderr)
            self.assertFalse(output.exists())


class SitemapTest(unittest.TestCase):
    def test_sitemap_urls_keep_paths_and_existing_queries_with_a_generic_seed(self):
        with tempfile.TemporaryDirectory() as directory:
            source, output = Path(directory) / 'sitemap.xml', Path(directory) / 'targets.txt'
            source.write_text('''<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
              <url><loc>http://java:9090/lab/LEVEL_1</loc></url>
              <url><loc>http://php/lab/LEVEL_2?search=two&amp;other=3</loc></url>
              <url><loc>http://jsp/native-name</loc></url>
            </urlset>''')
            result = subprocess.run([sys.executable, str(SCRIPTS / 'sitemap_to_targets.py'),
                                     '--sitemap', str(source), '--base-url', 'http://127.0.0.1:19090',
                                     '--output', str(output)], capture_output=True)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertEqual(output.read_text().splitlines(), [
                'http://127.0.0.1:19090/lab/LEVEL_1?id=1',
                'http://127.0.0.1:19090/lab/LEVEL_2?search=two&other=3',
                'http://127.0.0.1:19090/native-name?id=1'])

    def test_invalid_or_empty_sitemap_cannot_become_a_clean_scan(self):
        for content in ['<html>failed</html>', '<urlset/>', 'not XML']:
            with self.subTest(content=content), tempfile.TemporaryDirectory() as directory:
                source, output = Path(directory) / 'sitemap.xml', Path(directory) / 'targets.txt'
                source.write_text(content)
                result = subprocess.run([sys.executable, str(SCRIPTS / 'sitemap_to_targets.py'),
                                         '--sitemap', str(source), '--base-url', 'http://lab',
                                         '--output', str(output)], capture_output=True)
                self.assertNotEqual(result.returncode, 0)
                self.assertFalse(output.exists())


if __name__ == '__main__':
    unittest.main()
