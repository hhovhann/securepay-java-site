#!/usr/bin/env python3
"""Compare the Java site with a reference copy of the current site, page by page (migration check).

Usage: python tools/visual_compare.py [REFERENCE_URL] [NEW_URL]
Defaults: reference http://localhost:8080 (the WordPress review copy), new http://localhost:8081 (this site in Tomcat).
Writes out/compare/<width>/<page>-{reference,new,diff}.png and prints the share of differing pixels per page.
Animations are paused (reduced motion) so the screenshots are comparable. Needs only Playwright: the pixel comparison
runs in the browser.
"""
import base64
import pathlib
import sys

from playwright.sync_api import sync_playwright

REFERENCE = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080"
NEW = sys.argv[2] if len(sys.argv) > 2 else "http://localhost:8081"
PAGES = {"home": "/", "about-us": "/about-us/", "services": "/services/", "contact-us": "/contact-us/", "partners": "/partners/",
         "privacy-policy": "/privacy-policy/", "general-terms-conditions": "/general-terms-conditions/"}
WIDTHS = [(1280, 800), (390, 844)]
OUT = pathlib.Path(__file__).resolve().parent.parent / "out" / "compare"
FREEZE = ("*,*::before,*::after{animation:none!important;transition:none!important;caret-color:transparent!important}"
          ".hbspt-form,.hubspot-form-mount,.terms-viewer{visibility:hidden!important}")
DIFF_JS = """async ([a, b]) => {
  const load = src => new Promise(resolve => { const i = new Image(); i.onload = () => resolve(i); i.src = src; });
  const [ia, ib] = await Promise.all([load(a), load(b)]);
  const w = Math.max(ia.width, ib.width), h = Math.max(ia.height, ib.height);
  const draw = img => { const c = new OffscreenCanvas(w, h); const x = c.getContext('2d'); x.fillStyle = '#fff'; x.fillRect(0, 0, w, h); x.drawImage(img, 0, 0); return x.getImageData(0, 0, w, h); };
  const da = draw(ia).data, db = draw(ib), out = new OffscreenCanvas(w, h), ox = out.getContext('2d');
  ox.drawImage(ib, 0, 0); const od = ox.getImageData(0, 0, w, h);
  let differing = 0;
  for (let p = 0; p < da.length; p += 4) {
    const d = Math.max(Math.abs(da[p] - db.data[p]), Math.abs(da[p + 1] - db.data[p + 1]), Math.abs(da[p + 2] - db.data[p + 2]));
    if (d > 24) { differing++; od.data[p] = 255; od.data[p + 1] = 0; od.data[p + 2] = 80; }
  }
  ox.putImageData(od, 0, 0);
  const blob = await out.convertToBlob({type: 'image/png'});
  const bytes = new Uint8Array(await blob.arrayBuffer()); let s = ''; for (let i = 0; i < bytes.length; i += 32768) s += String.fromCharCode(...bytes.subarray(i, i + 32768));
  return {share: differing / (w * h) * 100, heights: [ia.height, ib.height], png: btoa(s)};
}"""


def shoot(page, url):
    page.goto(url, wait_until="load")
    page.add_style_tag(content=FREEZE)
    page.evaluate("document.querySelectorAll('img[loading=lazy]').forEach(i => i.loading = 'eager')")
    page.wait_for_function("[...document.images].every(i => i.complete)", timeout=30000)
    # HubSpot forms load from HubSpot's servers; wait until each form (or its email fallback) is in place.
    page.wait_for_function("[...document.querySelectorAll('[data-securepay-hubspot-form]')].every(m => m.dataset.state)", timeout=30000)
    page.wait_for_timeout(500)
    return page.screenshot(full_page=True)


def main():
    worst = 0.0
    with sync_playwright() as pw:
        browser = pw.chromium.launch()
        scratch = browser.new_page()
        scratch.set_content("<html><body></body></html>")
        for width, height in WIDTHS:
            folder = OUT / str(width)
            folder.mkdir(parents=True, exist_ok=True)
            context = browser.new_context(viewport={"width": width, "height": height}, reduced_motion="reduce")
            page = context.new_page()
            for name, path in PAGES.items():
                ref, new = shoot(page, REFERENCE + path), shoot(page, NEW + path)
                (folder / f"{name}-reference.png").write_bytes(ref)
                (folder / f"{name}-new.png").write_bytes(new)
                urls = ["data:image/png;base64," + base64.b64encode(x).decode() for x in (ref, new)]
                result = scratch.evaluate(DIFF_JS, urls)
                (folder / f"{name}-diff.png").write_bytes(base64.b64decode(result["png"]))
                worst = max(worst, result["share"])
                print(f"{width:>5} {name:<26} height {result['heights'][0]:>5} → {result['heights'][1]:>5}   differing pixels {result['share']:5.2f}%")
            context.close()
        browser.close()
    print(f"Largest difference: {worst:.2f}%  (images in {OUT})")


if __name__ == "__main__":
    main()
