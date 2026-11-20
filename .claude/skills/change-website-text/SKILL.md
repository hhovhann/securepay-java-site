---
name: change-website-text
description: Change, correct or replace wording on the SecurePay website (headings, paragraphs, button labels, list points, search-engine title or summary). Use when Paul or anyone asks to reword, fix a typo, update a sentence or change text on a page.
---

# Change text on the website

1. **Find the text.** Search `content/` for a distinctive part of the current wording (`grep -rn "current words" content/`). If the request names a page, start with that page's file (see the table in `CLAUDE.md`). If the same text appears in several places, list them and ask Paul which to change. Never guess.
2. **Confirm the exact new wording.** Use Paul's words exactly. If he described the change ("make it shorter", "mention Visa") instead of giving the words, propose the exact wording and wait for his "yes" before editing. Never add facts, numbers or names he did not give.
3. **Edit only that value.** Keep the YAML structure, quotes and formatting marks (`\n`, `[label](url)`, `==accent==`). Keep the site's typographic apostrophes (’) unless Paul's text uses plain ones on purpose.
4. **Search-engine text.** If Paul changes a page heading, ask whether the page's `seo.title` and `seo.description` should change too. Do not change them unasked.
5. **Guide answers.** If the changed fact also appears in `content/guide.yml` (the SecurePay Guide's answers), tell Paul and offer to update it the same way.
6. Continue with the `publish-website-change` skill.
