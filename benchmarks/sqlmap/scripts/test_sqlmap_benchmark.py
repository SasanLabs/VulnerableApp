import copy
import json
from pathlib import Path
import tempfile
import unittest
import subprocess
import sys
from io import StringIO
from unittest.mock import patch

from convert_sqlmap_to_benchmark import convert
from run_sqlmap import diagnostics, discover_targets, run

FIXTURES = Path(__file__).resolve().parents[1] / 'fixtures'
URL = 'http://127.0.0.1:19090/VulnerableApp/UnionBasedSQLInjectionVulnerability/LEVEL_1'
NEGATIVE = 'all tested parameters do not appear to be injectable.'


def report(name='positive'):
    return json.loads((FIXTURES / (name + '.json')).read_text())


def envelope(raw=None, url=URL, **updates):
    run = dict(url=url, method='GET', completed=True, returncode=0,
               report=raw if raw is not None else report(), diagnostics=[])
    run.update(updates)
    return dict(schemaVersion=1, sqlmapVersion='sqlmap/1.10.10#stable', targetCount=1, runs=[run])


class ConverterTest(unittest.TestCase):
    def test_confirmed_native_techniques_become_one_cwe_finding(self):
        self.assertEqual(convert(envelope()), {
            'tool': 'sqlmap', 'scanType': 'DAST',
            'findings': [{'url': URL, 'method': 'GET', 'cwe': 'CWE-89'}]})

    def test_multiple_parameters_and_runs_do_not_duplicate_endpoint(self):
        raw = report()
        raw['data'][1]['value'].append(copy.deepcopy(raw['data'][1]['value'][0]))
        raw['data'][1]['value'][1]['parameter'] = 'other'
        wrapped = envelope(raw)
        wrapped['runs'] *= 2
        wrapped['targetCount'] = 2
        self.assertEqual(len(convert(wrapped)['findings']), 1)

    def test_completed_negative_is_empty_not_failed(self):
        raw = report('negative')
        wrapped = envelope(raw, url=raw['meta']['url'], diagnostics=[NEGATIVE + ' Try more tests.'])
        self.assertEqual(convert(wrapped)['findings'], [])

    def test_empty_report_without_negative_conclusion_is_incomplete(self):
        raw = report('negative')
        with self.assertRaises(ValueError):
            convert(envelope(raw, url=raw['meta']['url']))

    def test_failed_timeout_nonzero_or_errored_scan_never_becomes_clean(self):
        for changes in [dict(completed=False), dict(returncode=1), dict(report=None),
                        dict(diagnostics=['unable to connect to target']),
                        dict(diagnostics=[NEGATIVE, 'connection lost'])]:
            with self.subTest(changes=changes), self.assertRaises(ValueError):
                wrapped = envelope()
                wrapped['runs'][0].update(changes)
                convert(wrapped)

    def test_native_error_and_partial_techniques_are_rejected(self):
        for field in ['error', 'partial', 'techniques', 'version', 'success']:
            raw = report()
            if field == 'error': raw['error'] = ['connection refused']
            if field == 'partial': raw['data'][1]['status'] = 0
            if field == 'techniques': raw['data'][1]['value'][0]['data'] = []
            if field == 'version': raw['meta']['sqlmap_version'] = 'sqlmap/other'
            if field == 'success': raw['success'] = False
            with self.subTest(field=field), self.assertRaises(ValueError):
                convert(envelope(raw))

    def test_any_stable_release_is_accepted_but_dev_builds_are_not(self):
        raw = report()
        raw['meta']['sqlmap_version'] = 'sqlmap/1.10.11#stable'
        wrapped = envelope(raw)
        wrapped['sqlmapVersion'] = 'sqlmap/1.10.11#stable'
        self.assertEqual(len(convert(wrapped)['findings']), 1)
        raw['meta']['sqlmap_version'] = wrapped['sqlmapVersion'] = 'sqlmap/1.10.10.31#dev'
        with self.assertRaises(ValueError): convert(wrapped)

    def test_report_must_belong_to_invoked_target(self):
        with self.assertRaises(ValueError):
            convert(envelope(url=URL.replace('LEVEL_1', 'LEVEL_2')))

    def test_original_url_is_cleaned_without_changing_route_or_method(self):
        raw = report()
        raw['meta']['url'] = URL + '/?id=1#payload'
        raw['data'][0]['value']['url'] = URL + '/'
        result = convert(envelope(raw, url=URL + '/?id=1#payload'))
        self.assertEqual(result['findings'][0]['url'], URL)
        raw['data'][1]['value'][0]['place'] = 'POST'
        result = convert(envelope(raw, method='POST'))
        self.assertEqual(result['findings'][0]['method'], 'POST')

    def test_method_mismatch_is_not_guessed(self):
        with self.assertRaises(ValueError): convert(envelope(method='POST'))

    def test_missing_planned_target_is_incomplete(self):
        wrapped = envelope()
        wrapped['targetCount'] = 2
        with self.assertRaises(ValueError): convert(wrapped)

    def test_cli_writes_valid_findings_and_rejects_failed_scan(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source, output = root / 'runs.json', root / 'findings.json'
            source.write_text(json.dumps(envelope()))
            script = Path(__file__).with_name('convert_sqlmap_to_benchmark.py')
            result = subprocess.run([sys.executable, str(script), '--input', str(source),
                                     '--output', str(output)], capture_output=True)
            self.assertEqual(result.returncode, 0)
            self.assertEqual(json.loads(output.read_text())['findings'][0]['cwe'], 'CWE-89')
            output.unlink()
            source.write_text(json.dumps(envelope(completed=False)))
            result = subprocess.run([sys.executable, str(script), '--input', str(source),
                                     '--output', str(output)], capture_output=True)
            self.assertNotEqual(result.returncode, 0)
            self.assertFalse(output.exists())

    def test_invalid_or_missing_runs_rejected(self):
        for raw in [None, {}, {'schemaVersion': 1, 'runs': []}]:
            with self.subTest(raw=raw), self.assertRaises(ValueError): convert(raw)


class RunnerTest(unittest.TestCase):
    def test_native_diagnostics_distinguish_negative_from_transport_error(self):
        self.assertEqual(diagnostics('\r[12:00:01] [CRITICAL] ' + NEGATIVE + '\n'), [NEGATIVE])
        self.assertEqual(diagnostics('[12:00:01] [ERROR] connection refused\n'), ['connection refused'])
        self.assertTrue(diagnostics('Traceback (most recent call last):\n'))
        self.assertEqual(diagnostics('[12:00:01] [WARNING] HTTP 500\n'), [])

    def test_live_metadata_drives_levels_and_preserves_secure_controls(self):
        rows = [dict(url=URL, method='GET', variant='UNSECURE', vulnerabilityTypes=['UNION_BASED_SQL_INJECTION']),
                dict(url=URL.replace('LEVEL_1', 'LEVEL_99'), method='GET', variant='SECURE', vulnerabilityTypes=['UNION_BASED_SQL_INJECTION']),
                dict(url=URL.replace('UnionBasedSQLInjectionVulnerability', 'XSS'), method='GET', variant='UNSECURE', vulnerabilityTypes=['REFLECTED_XSS'])]
        selected = discover_targets(rows, 'http://127.0.0.1:19090/VulnerableApp')
        self.assertEqual([t['url'].split('/')[-1] for t in selected], ['LEVEL_1', 'LEVEL_99'])
        self.assertEqual(selected[1]['variant'], 'SECURE')

    def test_foreign_origin_or_unsupported_method_does_not_expand_scan(self):
        for changes in [dict(url=URL.replace('127.0.0.1', 'example.org')), dict(method='POST')]:
            row = dict(url=URL, method='GET', variant='UNSECURE', vulnerabilityTypes=['UNION_BASED_SQL_INJECTION'])
            row.update(changes)
            with self.subTest(changes=changes), self.assertRaises(ValueError):
                discover_targets([row], 'http://127.0.0.1:19090/VulnerableApp')

    def test_process_deadline_preserves_failure_artifacts_and_refuses_submission(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            scanner = root / 'scanner'
            scanner.mkdir()
            (scanner / 'sqlmap.py').write_text(
                "import sys,time\n"
                "if '--version' in sys.argv: print('1.10.11#stable')\n"
                "else: time.sleep(5)\n")
            rows = [dict(url=URL, method='GET', variant='UNSECURE',
                         vulnerabilityTypes=['UNION_BASED_SQL_INJECTION'])]
            with patch('run_sqlmap.urlopen', return_value=StringIO(json.dumps(rows))):
                with self.assertRaises(ValueError):
                    run('http://127.0.0.1:19090/VulnerableApp', scanner, '1.10.11', root / 'out', 0.1)
            manifest = json.loads((root / 'out' / 'runs.json').read_text())
            self.assertEqual(manifest['sqlmapVersion'], 'sqlmap/1.10.11#stable')
            self.assertFalse(manifest['runs'][0]['completed'])
            self.assertIn('timeout', manifest['runs'][0]['diagnostics'][0])

    def test_scanner_must_be_the_resolved_stable_tag(self):
        for printed, tag in [('1.10.10.31#dev', '1.10.10'), ('1.10.10#stable', '1.10.11')]:
            with self.subTest(printed=printed, tag=tag), tempfile.TemporaryDirectory() as directory:
                scanner = Path(directory) / 'scanner'
                scanner.mkdir()
                (scanner / 'sqlmap.py').write_text('print(%r)\n' % printed)
                with patch('run_sqlmap.urlopen') as fetch, self.assertRaises(ValueError):
                    run('http://127.0.0.1:19090/VulnerableApp', scanner, tag,
                        Path(directory) / 'out', 1)
                fetch.assert_not_called()

    def test_missing_sql_ground_truth_is_not_clean_scan(self):
        with self.assertRaises(ValueError): discover_targets([], 'http://127.0.0.1:19090/VulnerableApp')


if __name__ == '__main__': unittest.main()
