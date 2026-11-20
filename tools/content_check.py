#!/usr/bin/env python3
"""Check that every text block on the current SecurePay pages is present on the new pages.

Usage: python3 content_check.py [LIVE_HTML_DIR]
Without a directory it downloads the current pages from public.securepay.ae.
The new pages are read from NEW_SITE (default http://localhost:8081, this site running in Tomcat).
"""
from __future__ import annotations

import html
import os
import pathlib
import re
import sys
import urllib.request

LIVE = "https://public.securepay.ae"
NEW = os.environ.get("NEW_SITE", "http://localhost:8081")
PAGES = {"home": "/", "about-us": "/about-us/", "services": "/services/", "contact-us": "/contact-us/", "partners": "/partners/", "privacy-policy": "/privacy-policy/"}
# Navigation, footer and widget text is compared separately from page bodies.
SKIP = {"skip to content", "about us", "services", "contact us", "partners", "payment links", "smartsell promos", "gateway & apis", "revenue assurance"}


def fetch(url: str) -> str:
    request = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (content check)"})
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read().decode("utf-8", "replace")


def norm(text: str) -> str:
    text = html.unescape(text).replace(" ", " ").replace("︎", "")
    text = text.replace("’", "'").replace("‘", "'").replace("“", '"').replace("”", '"').replace("–", "-").replace("—", "-")
    text = re.sub(r"\s+", " ", text).strip().lower()
    return re.sub(r"\s+([.,;:!?])", r"\1", text)


def blocks(page_html: str) -> list[str]:
    start = page_html.find("<body")
    end = page_html.find("<footer")
    body = re.sub(r"<script.*?</script>|<style.*?</style>|<template.*?</template>", " ", page_html[start:end], flags=re.S)
    parts = re.split(r"<(?:h[1-6]|p|li|dt|figcaption)\b[^>]*>", body)
    out = []
    for part in parts[1:]:
        text = norm(re.sub(r"<[^>]+>", " ", re.split(r"</(?:h[1-6]|p|li|dt|figcaption)>", part)[0]))
        if text and text not in SKIP:
            out.append(text)
    return out


def flat(page_html: str) -> str:
    body = re.sub(r"<script.*?</script>|<style.*?</style>", " ", page_html, flags=re.S)
    return norm(re.sub(r"<[^>]+>", " ", body))


def present(block: str, new_text: str) -> bool:
    """A block counts as present verbatim, or (for comma-separated link lists) when every item is present."""
    if block.rstrip(".") in new_text:
        return True
    items = [item.strip() for item in block.split(",")]
    return len(items) > 2 and all(item and item in new_text for item in items)


def main() -> int:
    live_dir = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else None
    missing_total = 0
    for name, path in PAGES.items():
        live_file = live_dir / f"pub-{name}.html" if live_dir else None
        live_html = live_file.read_text(encoding="utf-8", errors="replace") if live_file and live_file.is_file() else fetch(LIVE + path)
        new_text = flat(fetch(NEW + path))
        live_blocks = blocks(live_html)
        missing = [b for b in live_blocks if not present(b, new_text)]
        missing_total += len(missing)
        print(f"{name:15} {len(live_blocks) - len(missing):3}/{len(live_blocks):3} live text blocks present")
        for block in missing:
            print(f"    MISSING: {block[:150]}")
    print("All live text present." if not missing_total else f"{missing_total} live text block(s) not found.")
    return 1 if missing_total else 0


if __name__ == "__main__":
    raise SystemExit(main())
