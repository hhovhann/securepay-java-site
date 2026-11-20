---
name: publish-website-change
description: Check, preview and open a SecurePay website content change for approval. Use after any content edit, or when Paul asks to publish, preview or check his change.
---

# Check, preview and open the change for approval

1. **Scope check.** `git status` and `git diff --stat`: a content request must only touch `content/` and, for supplied images, `src/main/webapp/assets/img/` or `src/main/webapp/assets/brands/`. If anything else changed, undo it or explain why it is needed and mark the change for IT review.
2. **Build and test (Java 17).** Run `docker compose build web`. The build runs every test, including the content rules. If it fails, read the message (it names the file and field), fix the content and run it again. Do not change code or tests to make a content change pass.
3. **Preview.** Run `docker compose --profile preview up -d --build` and open the changed page at http://localhost:8082 (for example http://localhost:8082/services/#payment-links). Check it at laptop width and at phone width (390px). If a browser tool is available, take a screenshot of the changed area.
4. **Show Paul.** The page address, then each change as **Before:** … / **After:** …, plus the screenshot if you have one. Ask him to confirm it is right.
5. **Open it for approval** (after Paul confirms):
   - branch `content/<short-description>`, commit with a message in Paul's terms
   - pull request using `.github/pull_request_template.md`: quote his request word for word, list before/after, list the checks run
   - content-only changes can be approved by the content owner; IT is asked automatically for anything else
6. Tell Paul what happens next: once approved, the change is merged and goes live with the next deployment.
