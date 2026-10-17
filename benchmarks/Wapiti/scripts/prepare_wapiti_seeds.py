#!/usr/bin/env python3
"""Build Wapiti start URLs for known GET labs listed in a live sitemap."""
import argparse
import re
import xml.etree.ElementTree as ET
from pathlib import Path
from urllib.parse import urlencode, urlsplit

# Benign inputs from the controllers/UI templates. Include every sitemap level,
# including secure variants, so discovery does not depend on expected findings.
GET_INPUTS = {
    "XSSWithHtmlTagInjection": {"value": "test"},
    "XSSInImgTagAttribute": {"src": "/VulnerableApp/images/OWASP.png"},
    "ErrorBasedSQLInjectionVulnerability": {"id": "1"},
    "UnionBasedSQLInjectionVulnerability": {"id": "1"},
    "BlindSQLInjectionVulnerability": {"id": "1"},
    "PathTraversal": {"fileName": "UserInfo.json"},
    "CommandInjection": {"ipaddress": "127.0.0.1"},
    # VulnerableApp handles this URL with MetaDataServiceMock, without a network request.
    "SSRFVulnerability": {"fileurl": "http://169.254.169.254/latest/meta-data/"},
}


def prepare_seeds(sitemap: str, base_url: str) -> list[str]:
    """Rebase sitemap lab paths to the reachable app and supply real GET inputs."""
    base = urlsplit(base_url)
    if base.scheme not in ("http", "https") or not base.netloc or base.query or base.fragment:
        raise ValueError("base URL must be an HTTP(S) application root without query or fragment")
    prefix = base.path.rstrip("/") + "/"
    root_url = f"{base.scheme}://{base.netloc}{prefix}"
    root = ET.fromstring(sitemap)
    seeds = []
    seen = set()
    for node in root.iter("{http://www.sitemaps.org/schemas/sitemap/0.9}loc"):
        path = urlsplit((node.text or "").strip()).path
        if not path.startswith(prefix):
            continue
        relative = path[len(prefix):]
        match = re.fullmatch(r"([^/]+)/LEVEL_\d+", relative)
        if not match or match[1] not in GET_INPUTS:
            continue
        url = root_url + relative + "?" + urlencode(GET_INPUTS[match[1]])
        if url not in seen:
            seen.add(url)
            seeds.append(url)
    if not seeds:
        raise ValueError("No supported GET lab endpoints found in sitemap")
    return seeds


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sitemap", required=True, type=Path, help="Downloaded sitemap XML")
    parser.add_argument("--base-url", default="http://localhost/VulnerableApp/")
    parser.add_argument("--output", required=True, type=Path, help="One Wapiti start URL per line")
    args = parser.parse_args()
    try:
        seeds = prepare_seeds(args.sitemap.read_text(encoding="utf-8"), args.base_url)
    except (OSError, ValueError, ET.ParseError) as error:
        parser.exit(1, f"ERROR preparing sitemap seeds: {error}\n")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text("\n".join(seeds) + "\n", encoding="utf-8")
    print(f"Prepared {len(seeds)} GET lab seeds -> {args.output}")


if __name__ == "__main__":
    main()
