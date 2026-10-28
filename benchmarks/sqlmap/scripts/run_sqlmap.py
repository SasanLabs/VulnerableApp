#!/usr/bin/env python3
"""Scan the Java SQL-injection ground-truth endpoints on a controlled deployed app."""
import argparse
import json
from pathlib import Path
import re
import subprocess
import sys
from urllib.parse import urlencode, urlsplit
from urllib.request import urlopen

from convert_sqlmap_to_benchmark import SQLMAP_REVISION, SQLMAP_VERSION, clean_url, convert

FAMILIES = ('BlindSQLInjectionVulnerability', 'ErrorBasedSQLInjectionVulnerability',
            'UnionBasedSQLInjectionVulnerability')


def diagnostics(log):
    messages = re.findall(r'\[(?:ERROR|CRITICAL)\]\s*([^\r\n]+)', log)
    if 'Traceback (most recent call last)' in log:
        messages.append('Scanner traceback; inspect scan.log')
    return messages


def discover_targets(rows, base_url):
    if not isinstance(rows, list):
        raise ValueError('Expected the Java standalone /scanner/dast array')
    base = urlsplit(clean_url(base_url))
    prefix = base.path.rstrip('/') + '/'
    selected = {}
    for row in rows:
        if not isinstance(row, dict) or not isinstance(row.get('url'), str):
            raise ValueError('Invalid ground-truth row')
        parsed = urlsplit(row['url'])
        route = parsed.path[len(prefix):].split('/') if parsed.path.startswith(prefix) else []
        if len(route) != 2 or route[0] not in FAMILIES:
            continue
        if (parsed.scheme, parsed.netloc) != (base.scheme, base.netloc):
            raise ValueError('SQL ground truth points outside the configured app origin')
        if row.get('method') != 'GET' or parsed.query or parsed.fragment:
            raise ValueError('SQL runner supports GET endpoints with the id query parameter only')
        if row.get('variant') not in ('SECURE', 'UNSECURE'):
            raise ValueError('Unknown ground-truth variant')
        target = dict(url=clean_url(row['url']), method='GET', variant=row['variant'],
                      vulnerabilityTypes=row.get('vulnerabilityTypes', []))
        if target['url'] in selected and selected[target['url']] != target:
            raise ValueError('Conflicting ground-truth entries for one URL')
        selected[target['url']] = target
    if not selected:
        raise ValueError('No supported SQL-injection ground truth; refusing an empty scan')
    return list(selected.values())


def run(base_url, sqlmap_dir, output_dir, target_timeout):
    output_dir.mkdir(parents=True, exist_ok=False)
    sqlmap = sqlmap_dir.resolve() / 'sqlmap.py'
    version = subprocess.check_output([sys.executable, str(sqlmap), '--version'], text=True,
                                      timeout=15).strip()
    if 'sqlmap/' + version != SQLMAP_VERSION:
        raise ValueError('Expected sqlmap 1.10.10#stable, got ' + version)
    (output_dir / 'sqlmap-version.txt').write_text(version + '\n' + SQLMAP_REVISION + '\n')
    endpoint = base_url.rstrip('/') + '/scanner/dast'
    with urlopen(endpoint, timeout=15) as response:
        rows = json.load(response)
    (output_dir / 'ground-truth.json').write_text(json.dumps(rows, indent=2) + '\n')
    targets = discover_targets(rows, base_url)
    (output_dir / 'targets.json').write_text(json.dumps(targets, indent=2) + '\n')
    manifest = dict(schemaVersion=1, sqlmapVersion=SQLMAP_VERSION, targetCount=len(targets), runs=[])
    manifest_path = output_dir / 'runs.json'
    manifest_path.write_text(json.dumps(manifest, indent=2) + '\n')
    for index, target in enumerate(targets):
        directory = output_dir / ('target-%02d' % index)
        directory.mkdir()
        native_path = directory / 'report.json'
        command = [sys.executable, str(sqlmap), '-u', target['url'] + '?' + urlencode({'id': '1'}),
                   '-p', 'id', '--batch', '--disable-coloring', '--technique=BEU', '--level=1',
                   '--risk=1', '--threads=1', '--timeout=5', '--retries=0', '--flush-session',
                   '--report-json=' + str(native_path.resolve()),
                   '--output-dir=' + str((directory / 'session').resolve())]
        (directory / 'invocation.json').write_text(json.dumps(command, indent=2) + '\n')
        record = dict(target, completed=False, returncode=None, report=None, diagnostics=[])
        with (directory / 'scan.log').open('w') as log:
            try:
                result = subprocess.run(command, stdout=log, stderr=subprocess.STDOUT,
                                        timeout=target_timeout)
                record.update(completed=True, returncode=result.returncode)
            except subprocess.TimeoutExpired:
                record['diagnostics'].append('Per-target scan timeout exceeded')
        record['diagnostics'] += diagnostics((directory / 'scan.log').read_text(errors='replace'))
        if native_path.exists():
            try:
                record['report'] = json.loads(native_path.read_text())
            except ValueError:
                record['diagnostics'].append('Invalid native JSON report')
        manifest['runs'].append(record)
        manifest_path.write_text(json.dumps(manifest, indent=2) + '\n')
        print('Scanned %d/%d: %s' % (index + 1, len(targets), target['url']), flush=True)
    # A failed/incomplete scan retains artifacts but must not be submitted as a clean result.
    convert(manifest)
    return manifest


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base-url', required=True)
    parser.add_argument('--sqlmap-dir', required=True, type=Path)
    parser.add_argument('--output-dir', required=True, type=Path)
    parser.add_argument('--target-timeout', type=int, default=120)
    args = parser.parse_args()
    if not 1 <= args.target_timeout <= 300:
        parser.error('--target-timeout must be between 1 and 300 seconds')
    try:
        run(args.base_url, args.sqlmap_dir, args.output_dir, args.target_timeout)
    except (ValueError, OSError, subprocess.SubprocessError) as error:
        parser.exit(1, str(error) + '\n')


if __name__ == '__main__':
    main()
