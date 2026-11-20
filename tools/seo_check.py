#!/usr/bin/env python3
"""Check that each page keeps the live site's search-engine title, description and canonical address.

Usage: python3 tools/seo_check.py   (NEW_SITE defaults to http://localhost:8081)
"""
import html
import os
import re
import sys
import urllib.request

LIVE = "https://public.securepay.ae"
NEW = os.environ.get("NEW_SITE", "http://localhost:8081")
PAGES = ["/", "/about-us/", "/services/", "/contact-us/", "/partners/", "/privacy-policy/", "/general-terms-conditions/"]


def head(url):
    request = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (content check)"})
    with urllib.request.urlopen(request, timeout=30) as response:
        text = response.read().decode("utf-8", "replace")
    text = text[:text.find("</head>")]
    tags = {"title": html.unescape(re.search(r"<title>(.*?)</title>", text, re.S).group(1).strip())}
    for name, value in re.findall(r'<meta\s+(?:name|property)="([^"]+)"\s+content="([^"]*)"', text):
        tags[name] = html.unescape(value)
    canonical = re.search(r'<link rel="canonical" href="([^"]+)"', text)
    tags["canonical"] = canonical.group(1) if canonical else None
    return tags


def main():
    problems = 0
    for path in PAGES:
        live, new = head(LIVE + path), head(NEW + path)
        for key in ("title", "description", "og:title", "og:description", "og:url", "twitter:title", "twitter:description", "canonical", "robots"):
            if live.get(key) != new.get(key):
                problems += 1
                print(f"{path} {key}: live {live.get(key)!r} / new {new.get(key)!r}")
        print(f"{path:<28} title and search tags {'match' if not problems else 'checked'}")
    print("All search tags match." if not problems else f"{problems} difference(s).")
    sys.exit(1 if problems else 0)


if __name__ == "__main__":
    main()
