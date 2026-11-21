#!/usr/bin/env python3
"""Translate sqlmap's native single-target or bulk JSON report into benchmark findings."""
# Single-target report excerpt (URL shortened). Bulk reports wrap these objects in "targets".
# {"success": true, "data": [{"status": 1, "type": 0, "type_name": "TARGET",
#   "value": {"url": "https://example.invalid/page", "query": "id=1", "data": null}}]}
import argparse
from collections import Counter
import json
from pathlib import Path

NEGATIVE_CONCLUSION = 'all tested parameters do not appear to be injectable.'


def original_url(identity):
    url = identity['url']
    base, separator, fragment = url.partition('#')
    if '?' in base:
        return url
    query = identity.get('query')
    if query is not None and not isinstance(query, str):
        raise ValueError('Native TARGET record has an invalid query')
    return f'{base}?{query}{separator}{fragment}' if query else url


def convert(report, *, expected_urls=None):
    if not isinstance(report, dict) or report.get('success') is not True:
        raise ValueError('Missing successful native sqlmap report')
    targets = report.get('targets', [report])
    if not isinstance(targets, list) or not targets:
        raise ValueError('Native report contains no targets')
    if expected_urls is not None and (
            not expected_urls or any(not isinstance(url, str) or not url.strip() for url in expected_urls)):
        raise ValueError('Expected URL list must contain one URL per line')
    findings = []
    reported_urls = Counter()
    for target in targets:
        if not isinstance(target, dict) or target.get('success') is not True:
            raise ValueError('Incomplete native target report')
        errors = target.get('error')
        if (not isinstance(errors, list)
                or any(not isinstance(error, str) or not error.startswith(NEGATIVE_CONCLUSION)
                       for error in errors)):
            raise ValueError('sqlmap reported a scan error; inspect the native report')
        records = target.get('data')
        if (not isinstance(records, list) or not records
                or any(not isinstance(record, dict) or record.get('status') != 1
                       for record in records)):
            raise ValueError('Missing or partial native scan data')
        identity = next((record.get('value') for record in records if record.get('type') == 0), None)
        if not isinstance(identity, dict) or not isinstance(identity.get('url'), str) or not identity['url']:
            raise ValueError('Native TARGET record is missing its URL')
        url = original_url(identity)
        reported_urls[url] += 1
        confirmed = False
        for record in records:
            if record.get('type') != 1:
                continue
            injections = record.get('value')
            if not isinstance(injections, list):
                raise ValueError('Invalid native TECHNIQUES record')
            for injection in injections:
                if (not isinstance(injection, dict)
                        or not isinstance(injection.get('data'), list) or not injection['data']):
                    raise ValueError('Injection has no confirmed techniques')
                if any(not isinstance(technique, dict)
                       or not isinstance(technique.get('technique'), str)
                       or not technique['technique'].strip() for technique in injection['data']):
                    raise ValueError('Invalid native technique data')
                findings.append(dict(url=url,
                                     method='POST' if identity.get('data') is not None else 'GET',
                                     cwe='CWE-89'))
                confirmed = True
        if not confirmed and not errors:
            raise ValueError('No confirmed injection or completed negative conclusion')
    if expected_urls is not None:
        expected = Counter(expected_urls)
        missing = sum((expected - reported_urls).values())
        if missing:
            raise ValueError(f'Missing expected report entries: {missing}')
        unexpected = sum((reported_urls - expected).values())
        if unexpected:
            raise ValueError(f'Unexpected report entries: {unexpected}')
    return dict(tool='sqlmap', scanType='DAST', findings=findings)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--input', required=True, type=Path)
    parser.add_argument('--output', required=True, type=Path)
    parser.add_argument('--expected-urls', type=Path,
                        help='Check a saved report against a local list with one exact URL per line')
    args = parser.parse_args()
    try:
        expected_urls = args.expected_urls.read_text().splitlines() if args.expected_urls else None
        result = convert(json.loads(args.input.read_text()), expected_urls=expected_urls)
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(result, indent=2) + '\n')
    except (ValueError, OSError) as error:
        parser.exit(1, str(error) + '\n')


if __name__ == '__main__':
    main()
