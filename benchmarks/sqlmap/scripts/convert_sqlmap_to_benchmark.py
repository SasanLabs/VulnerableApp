#!/usr/bin/env python3
"""Convert completed sqlmap 1.10.10 native JSON reports to benchmark findings."""
import argparse
import json
from pathlib import Path
from urllib.parse import urlsplit, urlunsplit

SQLMAP_REVISION = '65b6e7b4acb13a736614a1ab73b0908476f3c24a'
SQLMAP_VERSION = 'sqlmap/1.10.10#stable'
NEGATIVE_CONCLUSION = 'all tested parameters do not appear to be injectable.'


def clean_url(value):
    if not isinstance(value, str):
        raise ValueError('Missing target URL')
    parts = urlsplit(value.strip())
    if parts.scheme not in ('http', 'https') or not parts.netloc or parts.username or parts.password:
        raise ValueError('Expected an HTTP(S) target URL without credentials')
    path = parts.path.split(';', 1)[0].rstrip('/') or '/'
    return urlunsplit((parts.scheme, parts.netloc, path, '', ''))


def convert(envelope):
    if not isinstance(envelope, dict) or envelope.get('schemaVersion') != 1:
        raise ValueError('Expected a sqlmap benchmark run manifest (schemaVersion 1)')
    runs = envelope.get('runs')
    if envelope.get('sqlmapVersion') != SQLMAP_VERSION or not isinstance(runs, list) or not runs:
        raise ValueError('Missing runs or unsupported sqlmap version')
    if envelope.get('targetCount') != len(runs):
        raise ValueError('Not all planned targets were scanned')
    findings, seen = [], set()
    for run in runs:
        if not isinstance(run, dict) or run.get('completed') is not True or run.get('returncode') != 0:
            raise ValueError('Scan failed or did not complete; inspect run artifacts')
        url = clean_url(run.get('url'))
        method = str(run.get('method', '')).upper()
        if method not in ('GET', 'POST'):
            raise ValueError('Unknown request method')
        messages = run.get('diagnostics')
        if not isinstance(messages, list) or any(not isinstance(m, str) for m in messages):
            raise ValueError('Missing scanner diagnostics')
        if any(not m.startswith(NEGATIVE_CONCLUSION) for m in messages):
            raise ValueError('sqlmap reported an error: ' + '; '.join(messages))
        raw = run.get('report')
        if not isinstance(raw, dict) or raw.get('success') is not True or raw.get('error') != []:
            raise ValueError('Native sqlmap report is missing or reports an error')
        meta = raw.get('meta', {})
        if (meta.get('sqlmap_version') != SQLMAP_VERSION or meta.get('api_version') != 2
                or clean_url(meta.get('url')) != url):
            raise ValueError('Native report version or target differs from the invocation')
        records = raw.get('data')
        if not isinstance(records, list):
            raise ValueError('Native report data must be a list')
        confirmed = False
        for record in records:
            if not isinstance(record, dict) or record.get('status') != 1:
                raise ValueError('Incomplete native report record')
            if record.get('type') == 0:
                if clean_url(record.get('value', {}).get('url')) != url:
                    raise ValueError('Native TARGET record differs from the invocation')
            if record.get('type') != 1:
                continue
            injections = record.get('value')
            if not isinstance(injections, list):
                raise ValueError('Invalid TECHNIQUES record')
            for injection in injections:
                if (not isinstance(injection, dict) or injection.get('place') != method
                        or not isinstance(injection.get('parameter'), str)
                        or not injection.get('parameter')
                        or not isinstance(injection.get('data'), list)
                        or not injection['data']):
                    raise ValueError('Injection lacks a confirmed technique or matching request location')
                if any(not isinstance(t, dict) or not t.get('technique') for t in injection['data']):
                    raise ValueError('Invalid confirmed technique')
                confirmed = True
        if not confirmed and not messages:
            raise ValueError('No confirmed injection or explicit completed negative conclusion')
        if confirmed and messages:
            raise ValueError('Conflicting positive and negative conclusions')
        if confirmed and (url, method) not in seen:
            seen.add((url, method))
            findings.append(dict(url=url, method=method, cwe='CWE-89'))
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
        print('Converted %d confirmed endpoint finding(s)' % len(result['findings']))
    except (ValueError, OSError) as error:
        parser.exit(1, str(error) + '\n')


if __name__ == '__main__':
    main()
