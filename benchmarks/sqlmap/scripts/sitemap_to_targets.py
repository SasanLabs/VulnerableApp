#!/usr/bin/env python3
"""Adapt a deployed lab's sitemap to sqlmap's bulk URL input."""
import argparse
from pathlib import Path
from urllib.parse import urlsplit, urlunsplit
import xml.etree.ElementTree as ET


def targets(sitemap):
    urls = []
    for loc in ET.fromstring(sitemap).findall('{*}url/{*}loc'):
        source = urlsplit((loc.text or '').strip())
        if source.scheme not in ('http', 'https') or not source.netloc:
            raise ValueError('Sitemap entry must be an HTTP(S) URL')
        urls.append(urlunsplit((source.scheme, source.netloc, source.path, source.query or 'id=1', '')))
    if not urls:
        raise ValueError('Sitemap contains no target URLs')
    return urls


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--sitemap', required=True, type=Path)
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    try:
        urls = targets(args.sitemap.read_text())
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text('\n'.join(urls) + '\n')
    except (ValueError, OSError, ET.ParseError) as error:
        parser.exit(1, str(error) + '\n')


if __name__ == '__main__':
    main()
