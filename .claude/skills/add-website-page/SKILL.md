---
name: add-website-page
description: Create a new page on the SecurePay website from the existing design blocks (intro, image, facts, cards, details, text, call to action), or remove a page that was created this way. Use when Paul asks for a new page such as a case study, a product page or an announcement.
---

# Add a new page from blocks

1. **Agree the content first.** Ask Paul for: the page address (for example `case-studies`), the heading, every piece of text word for word, any images (as files), and where it should be linked from (menu, footer or an existing page). Do not write the page's text yourself; if Paul gives notes rather than final wording, propose wording and wait for his "yes".
2. **Choose blocks** that fit his content (see the table in `CLAUDE.md`, New pages). The first block is always `intro`. Suggest the layout in plain words ("a heading, then three cards, then a Want a Demo? bar") and confirm.
3. **Create** `content/pages/<address>.yml`:

   ```yaml
   layout: "blocks"
   seo:
     title: "<Page title> | SecurePay"
     description: "<one or two sentences Paul approves>"
   blocks:
     - type: "intro"
       eyebrow: "<SMALL LABEL>"
       title: "<Heading>"
       text: "<Intro paragraph>"
     - type: "cards"
       title: "<Section heading>"
       cards:
         - title: "<Card heading>"
           text: "<Card text>"
     - type: "cta"
       title: "Want a Demo?"
       link_label: "Contact us ↗︎"
       url: "/contact-us/"
   ```

4. **Images** go in `src/main/webapp/assets/img/pages/` (see the `replace-website-image` skill).
5. **Links:** add the menu or footer link in `content/site.yml` only if Paul asked for it. Linking from another page uses `[label](/<address>/)` in that page's text.
6. **Removing a page** created this way: delete its file and every link to it (`grep -rn "/<address>/" content/`). The seven original pages (Home, About Us, Services, Contact Us, Partners, Privacy Policy, Terms) can only be removed by IT.
7. Continue with the `publish-website-change` skill. The content rules will name any missing field, wrong block type or broken link.
