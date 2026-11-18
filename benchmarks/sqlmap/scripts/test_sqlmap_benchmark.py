import copy
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

from convert_sqlmap_to_benchmark import convert

FIXTURES = Path(__file__).resolve().parents[1] / 'fixtures'
SCRIPTS = Path(__file__).resolve().parent
URL = 'http://127.0.0.1:19090/VulnerableApp/UnionBasedSQLInjectionVulnerability/LEVEL_1'


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
            {'url': 'http://lab/another?id=2', 'method': 'POST', 'cwe': 'CWE-89'}])

    def test_completed_native_negative_is_empty(self):
        self.assertEqual(convert(report('negative-1.10.11'))['findings'], [])

    def test_transport_error_is_not_a_clean_negative(self):
        raw = report('negative-1.10.11')
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
