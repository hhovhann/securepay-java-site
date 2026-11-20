---
name: replace-website-image
description: Replace or add an image on the SecurePay website, such as a page's black-and-white header image, the logo, the social-sharing image or a hotel-group logo. Use when Paul supplies an image file and asks to put it on the site.
---

# Replace or add an image

1. You need the actual image file from Paul. Never use an image from the web or generate one. If Paul has not attached it, ask for it.
2. Check it is a web image (`.png`, `.jpg`, `.webp` or `.svg`) and a sensible size (page images about 2000px wide and under 2 MB; logos under 200 KB). If it is far larger, say so and ask whether to resize it.
3. Save it with a short lower-case name:
   - page images: `src/main/webapp/assets/img/pages/<page>-<short-name>.<ext>`
   - hotel-group logos: `src/main/webapp/assets/brands/<name>.png`
   - site logo, icons and the social card: `src/main/webapp/assets/img/`
4. Point the content at it (`image.src` on the page, `logo`, `social_image` or `icon_*` in `content/site.yml`) and update the `alt` text. Alt text describes the image for people who cannot see it. Ask Paul if you are unsure what it shows.
5. Keep the old file until the change is approved; remove it in the same change only if nothing else uses it (`grep -rn "old-file-name" content/ src/`).
6. Images are shown in black and white on the page headers by design. Mention this if Paul sends a colour photo.

Continue with the `publish-website-change` skill.
