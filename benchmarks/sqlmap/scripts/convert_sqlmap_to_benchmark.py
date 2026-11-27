#!/usr/bin/env python3
"""Translate sqlmap's native single-target or bulk JSON report into benchmark findings."""
# Single-target report excerpt (URL shortened). Bulk reports wrap these objects in "targets".
# {"success": true, "data": [{"status": 1, "type": 0, "type_name": "TARGET",
#   "value": {"url": "https://example.invalid/page", "query": "id=1", "data": null}}]}
import argparse
from collections import Counter
import json
from pathlib import Path
import re

NEGATIVE_CONCLUSION = 'all tested parameters do not appear to be injectable.'
# Logged during tests that still end in an injection or the negative conclusion.
NEGATIVE_NOTES = ('heuristics detected that the target is protected by some kind of WAF/IPS',
                  'there was an error checking the stability of page because of lack of content.')
# sqlmap stops before testing when the first response is a 401, or an HTTP error with an empty body.
NOT_TESTED = ('unable to retrieve page content',
              'not authorized, try to provide right HTTP authentication')
OUTCOMES = (NEGATIVE_CONCLUSION,) + NOT_TESTED


def target_url(url):
    """The comparator ignores the query when matching, so findings carry the path only."""
    return url.split('#', 1)[0].split('?', 1)[0]


def scanned_targets(lines):
    """Follows sqlmap's -m loading: '#' comments, query required, no repeats."""
    targets = {}
    for line in lines:
        line = line.split('#', 1)[0].strip()
        if line and re.match(r'[^ ]+\?(.+)', line):
            targets.setdefault(line)
    return list(targets)


def convert(report, *, expected_urls=None):
    if not isinstance(report, dict) or report.get('success') is not True:
        raise ValueError('Missing successful native sqlmap report')
    targets = report.get('targets', [report])
    if not isinstance(targets, list) or not targets:
        raise ValueError('Native report contains no targets')
    if expected_urls is not None and (
            not expected_urls or any(not isinstance(url, str) or not url.strip() for url in expected_urls)):
        raise ValueError('Expected URL list must contain one URL per line')
    expected = None
    if expected_urls is not None:
        expected = Counter(target_url(url) for url in expected_urls)
        # sqlmap skips a repeated path when its parameters were already tested.
        if max(expected.values()) > 1:
            raise ValueError('Expected URLs repeat a path')
    findings = []
    reported_urls = Counter()
    for target in targets:
        if not isinstance(target, dict) or target.get('success') is not True:
            raise ValueError('Incomplete native target report')
        errors = target.get('error')
        if (not isinstance(errors, list)
                or any(not isinstance(error, str) or not error.startswith(OUTCOMES + NEGATIVE_NOTES)
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
        url = target_url(identity['url'])
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
        if not confirmed and not any(error.startswith(OUTCOMES) for error in errors):
            raise ValueError('No confirmed injection or completed negative conclusion')
    if expected is not None:
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
                        help='Check the report against the UTF-8 sqlmap bulk file it was scanned from')
    args = parser.parse_args()
    try:
        expected_urls = (scanned_targets(args.expected_urls.read_text(encoding='utf-8').splitlines())
                         if args.expected_urls else None)
        result = convert(json.loads(args.input.read_text()), expected_urls=expected_urls)
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(result, indent=2) + '\n')
    except (ValueError, OSError) as error:
        parser.exit(1, str(error) + '\n')


if __name__ == '__main__':
    main()
