#!/usr/bin/env python3
"""Translate sqlmap's native single-target or bulk JSON report into benchmark findings."""
import argparse
import json
from pathlib import Path

NEGATIVE_CONCLUSION = 'all tested parameters do not appear to be injectable.'


def convert(report):
    if not isinstance(report, dict) or report.get('success') is not True:
        raise ValueError('Missing successful native sqlmap report')
    targets = report.get('targets', [report])
    if not isinstance(targets, list) or not targets:
        raise ValueError('Native report contains no targets')
    findings = []
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
                findings.append(dict(url=identity['url'],
                                     method='POST' if identity.get('data') is not None else 'GET',
                                     cwe='CWE-89'))
                confirmed = True
        if not confirmed and not errors:
            raise ValueError('No confirmed injection or completed negative conclusion')
    return dict(tool='sqlmap', scanType='DAST', findings=findings)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--input', required=True, type=Path)
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    try:
        result = convert(json.loads(args.input.read_text()))
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(result, indent=2) + '\n')
    except (ValueError, OSError) as error:
        parser.exit(1, str(error) + '\n')


if __name__ == '__main__':
    main()
