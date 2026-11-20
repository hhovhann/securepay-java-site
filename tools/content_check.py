#!/usr/bin/env python3
"""Check that every text block on the current SecurePay pages is present on the new pages.

Usage: python3 content_check.py [LIVE_HTML_DIR]
       python3 content_check.py [LIVE_HTML_DIR] --extra   (the other way round: text on the new pages, footer and
       email fallback included, that is not on the live site)
Without a directory it downloads the current pages from public.securepay.ae.
The new pages are read from NEW_SITE (default http://localhost:8081, this site running in Tomcat).
"""
from __future__ import annotations

import html
from html.parser import HTMLParser
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
    start = max(page_html.find("<body"), 0)
    end = page_html.find("<footer")
    end = end if end >= 0 else len(page_html)
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


# --extra: the other way round. Text on the new pages that is not on the live site, so each addition can be approved.
BLOCK_TAGS = {"body", "p", "h1", "h2", "h3", "h4", "h5", "h6", "li", "dt", "dd", "figcaption", "button", "label", "summary", "td",
              "th", "caption", "blockquote", "legend", "div", "section", "article", "header", "footer", "nav", "main", "aside",
              "figure", "ul", "ol", "dl", "table", "tr", "form", "select", "option", "noscript", "template"}
HIDDEN_TAGS = {"head", "script", "style", "svg"}


class TextBlocks(HTMLParser):
    """The text of each block element, with links, bold and other inline elements kept inside it, plus its separate text nodes."""

    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.hidden = 0
        self.open: list[tuple[str, list[str]]] = []
        self.found: list[tuple[str, list[str]]] = []

    def handle_starttag(self, tag, attrs):
        if tag in HIDDEN_TAGS:
            self.hidden += 1
        elif not self.hidden and tag in BLOCK_TAGS:
            self.open.append((tag, []))

    def handle_endtag(self, tag):
        if tag in HIDDEN_TAGS:
            self.hidden = max(0, self.hidden - 1)
        elif not self.hidden and any(name == tag for name, _ in self.open):
            while self.open:
                name, nodes = self.open.pop()
                self.emit(nodes)
                if name == tag:
                    break

    def handle_data(self, data):
        if not self.hidden and self.open:
            self.open[-1][1].append(data)

    def emit(self, nodes: list[str]) -> None:
        nodes = [norm(node) for node in nodes if norm(node)]
        if nodes:
            self.found.append((norm(" ".join(nodes)), nodes))

    def close(self):
        super().close()
        while self.open:
            self.emit(self.open.pop()[1])


def text_blocks(page_html: str) -> list[tuple[str, list[str]]]:
    parser = TextBlocks()
    parser.feed(page_html)
    parser.close()
    return parser.found


def on_live(text: str, live_pieces: set[str], live_text: str) -> bool:
    """One or two words count only if exactly that text stands alone on the live site; longer text must appear there as whole words."""
    text = text.rstrip(".")
    if text in live_pieces:
        return True
    if len(text.split()) < 3:
        return False
    return re.search(rf"(?<![a-z0-9]){re.escape(text)}(?![a-z0-9])", live_text) is not None


def extra(live_dir: pathlib.Path | None) -> int:
    """List text on the new pages that is not on the live site. These are additions to approve, not failures."""
    paths = dict(PAGES, **{"general-terms-conditions": "/general-terms-conditions/"})
    live_pages = []
    for name, path in paths.items():
        live_file = live_dir / f"pub-{name}.html" if live_dir else None
        live_pages.append(live_file.read_text(encoding="utf-8", errors="replace") if live_file and live_file.is_file() else fetch(LIVE + path))
    live_pieces = {piece.rstrip(".") for html_text in live_pages for full, nodes in text_blocks(html_text) for piece in [full, *nodes]}
    live_text = " ".join(flat(html_text) for html_text in live_pages)
    has_words = re.compile(r"[a-z]{2}")
    seen: set[str] = set()
    total = 0
    for name, path in paths.items():
        added = []
        for full, nodes in text_blocks(fetch(NEW + path)):
            if full in seen or not has_words.search(full):
                continue
            seen.add(full)
            # A block made only of live pieces (for example a number and its label) is not an addition.
            if on_live(full, live_pieces, live_text) or all(on_live(n, live_pieces, live_text) for n in nodes if has_words.search(n)):
                continue
            added.append(full)
        total += len(added)
        print(f"{name:25} {len(added):3} piece(s) of text not on the live site (text repeated on several pages is listed once)")
        for text in added:
            print(f"    + {text[:160]}")
    print(f"{total} piece(s) of text added in total.")
    return 0


def main() -> int:
    args = [arg for arg in sys.argv[1:] if arg != "--extra"]
    live_dir = pathlib.Path(args[0]) if args else None
    if "--extra" in sys.argv[1:]:
        return extra(live_dir)
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
