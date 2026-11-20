---
name: add-or-remove-website-items
description: Add, remove or reorder items on the SecurePay website, such as a bullet point under a service, a service or benefit card, a hotel-group logo, a published number, a menu or footer link, a privacy-policy section or a SecurePay Guide topic. Use when Paul asks to add, delete, move or reorder something.
---

# Add, remove or reorder items

Items are YAML list entries (`points`, `cards`, `items`, `logos`, `sections`, `groups`, `menu`, `links`, `topics`). The page shows them in file order.

**Adding**
1. Find the list in the right page file (see `CLAUDE.md`). Copy the shape of a neighbouring entry so the new one has the same fields.
2. Fill every field from Paul's request. If a field has no value from Paul (a card's label number, a link address, a logo image), ask him. Never invent it.
3. Numbering labels such as `"04 / F&B & EVENTS"` or `"03"` follow their position. If adding or removing changes the order, renumber the labels in that list and tell Paul.
4. **Logos:** only add a hotel-group logo Paul supplies and confirms SecurePay may show. Put the file in `src/main/webapp/assets/brands/` (lower-case name, `.png`), then add `name` and `src`.
5. **Menu links** may only point to existing pages or sections. A link to a new page needs IT.
6. **Guide topics** (`content/guide.yml`): the `text` must come from SecurePay's published pages, `url` must be the page it comes from, and `terms` are the words visitors may use.

**Removing**
1. Find the entry and show Paul exactly what will disappear (and from which page) before deleting it.
2. If other places refer to it (a link to `/services/#smartsell`, a menu entry, a Guide topic linking to it), list them; the content rules fail on links to sections that no longer exist.
3. Removing a whole page or section type is an IT change.

Continue with the `publish-website-change` skill.
