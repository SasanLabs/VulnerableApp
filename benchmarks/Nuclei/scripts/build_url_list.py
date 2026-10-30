#!/usr/bin/env python3
"""
build_url_list.py
=================
Build a Nuclei target list from VulnerableApp's ``/scanner/dast`` ground truth.

``/scanner/dast`` returns every vulnerable endpoint as a bare URL plus the HTTP
method and the vulnerability types it exposes, but it deliberately does not
carry the parameter names: the UI drives each level with JavaScript, not plain
HTML forms, so there is no crawlable ``<form>`` for a scanner to mine either.
Nuclei's fuzzing templates mutate the query string of the URLs they are given,
so a bare URL has nothing to mutate and produces no findings.

This script re-attaches the query parameter each endpoint actually reads, so
the fuzzing templates have a value to replace. The parameter name is keyed off
the endpoint's class name (the path segment before ``/LEVEL_x``), which is the
same name the application uses to route the request. Endpoints with no known
parameter are emitted as bare URLs so a generic template can still flag them.

Usage
-----
    curl -s http://localhost/VulnerableApp/scanner/dast \\
        | python3 build_url_list.py --base-url http://localhost -o urls.txt
"""

import argparse
import json
import sys
from urllib.parse import urlparse

# Endpoint class name (lower-cased) -> query parameter it reads. Sourced from
# the static JS template bundled with each level, e.g.
#   templates/CommandInjection/LEVEL_1/CI_Level1.js
#       url + "?ipaddress=" + document.getElementById("ipaddress").value
_PARAM_BY_ENDPOINT = {
    "errorbasedsqlinjectionvulnerability": "id",
    "blindsqlinjectionvulnerability": "id",
    "unionbasedsqlinjectionvulnerability": "id",
    "commandinjection": "ipaddress",
    "pathtraversal": "fileName",
    "xsswithhtmltaginjection": "value",
    "xsswithnullbytesimgtagattribute": "value",
    "xssinimgtagattribute": "src",
    "persistentxssinhtmltagvulnerability": "comment",
    "ssrfvulnerability": "fileurl",
    "idorvulnerability": "id",
    "authenticationvulnerability": "username",
    "otpvulnerability": "otp",
    "sessionmanagementvulnerability": "action",
    "http3xxstatuscodebasedinjection": "returnTo",
    "metatagbasedinjection": "returnTo",
    "jwtvulnerability": "JWT",
    "samplevulnerability": "name",
}

# A benign value for each parameter. Nuclei replaces it with its own payloads,
# so the exact value only needs to keep the endpoint in a valid state.
_SAMPLE_BY_PARAM = {
    "username": "admin",
    "password": "admin",
    "JWT": "eyJhbGciOiJIUzI1NiJ9.e30.signature",
    "ipaddress": "127.0.0.1",
    "fileurl": "http://example.com",
    "src": "http://example.com",
    "fileName": "test.txt",
}

_DEFAULT_SAMPLE = "1"


def _endpoint_name(path: str) -> str:
    """Return the endpoint class name, the path segment before ``/LEVEL_x``."""
    segments = [segment for segment in path.split("/") if segment]
    # /VulnerableApp/<Endpoint>/<LEVEL>  ->  the middle segment
    if len(segments) >= 2:
        return segments[-2]
    return ""


def _param_for(endpoint: str):
    """Return the query parameter the endpoint reads, or ``None`` if unknown."""
    return _PARAM_BY_ENDPOINT.get(endpoint.lower())


def build_urls(scanner_payload: list, base_url: str) -> list:
    """
    Turn a ``/scanner/dast`` payload into a list of fuzzable URLs.

    ``scanner_payload`` is the JSON list of ``ScannerResponseBean`` objects
    (``url``, ``variant``, ``method``, ``vulnerabilityTypes``). ``base_url``
    replaces the scheme/host/port so the list points at the reachable
    instance, e.g. ``http://localhost`` when Nuclei shares the host network.
    """
    urls = []
    for entry in scanner_payload:
        if not isinstance(entry, dict):
            continue
        raw_url = entry.get("url") or ""
        path = urlparse(raw_url).path
        if not path:
            continue

        url = base_url.rstrip("/") + path
        param = _param_for(_endpoint_name(path))
        if param:
            sample = _SAMPLE_BY_PARAM.get(param, _DEFAULT_SAMPLE)
            url += ("&" if "?" in url else "?") + param + "=" + sample
        urls.append(url)
    return urls


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Build a Nuclei target list from a /scanner/dast payload."
    )
    parser.add_argument(
        "--base-url",
        default="http://localhost",
        help="Scheme/host/port to target (default: http://localhost).",
    )
    parser.add_argument(
        "--input",
        "-i",
        help="Path to a /scanner/dast JSON file. Reads stdin when omitted.",
    )
    parser.add_argument(
        "--output",
        "-o",
        help="Path to write the URL list. Writes to stdout when omitted.",
    )
    args = parser.parse_args()

    if args.input:
        with open(args.input, encoding="utf-8") as handle:
            payload = json.load(handle)
    else:
        payload = json.load(sys.stdin)

    urls = build_urls(payload, args.base_url)
    output = "\n".join(urls) + ("\n" if urls else "")

    if args.output:
        with open(args.output, "w", encoding="utf-8") as handle:
            handle.write(output)
        print(f"Wrote {len(urls)} target URL(s) to {args.output}", file=sys.stderr)
    else:
        sys.stdout.write(output)


if __name__ == "__main__":
    main()
