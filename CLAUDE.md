# SecurePay public website: rules for AI changes

This repository is SecurePay's public website (public.securepay.ae): Java 17, Spring MVC and JSP, deployed as a WAR on Apache Tomcat 11. The words, images and links on the site live in `content/` as YAML files. The JSP pages in `src/main/webapp/WEB-INF/jsp/` only lay that content out.

Most requests come from Paul, SecurePay's content owner, who is not a developer. He asks in plain English; you make the change, prove it works and open it for review. Use the skills in `.claude/skills/` for his requests. They are the step-by-step procedures for each kind of change.

## Content requests (Paul): what you may change

- Edit only files in `content/`. To add or replace an image, put the file Paul supplied in `src/main/webapp/assets/img/` (logos in `src/main/webapp/assets/brands/`) and reference it from content.
- Never change Java, JSP, CSS, JavaScript, `pom.xml`, the Dockerfile or CI files for a content request. If a request needs that (a new page, a new kind of section, a layout or colour change, a new form), stop and say it needs IT (Phil) and why. You can draft the request for IT.

## Never invent

- Use Paul's exact wording. Do not rewrite, "improve", shorten or translate his text unless he asks you to.
- Never make up text, numbers, statistics, customer names, logos, testimonials, prices, certifications, dates or claims. If something is missing or unclear (which page? what exact words? which image?), ask Paul before changing anything.
- Legal pages (`privacy-policy.yml`, `general-terms-conditions.yml` and its PDF) change only with wording or a file Paul says SecurePay has approved, copied exactly.
- Do not change HubSpot portal or form IDs, form fields, the Guide's safety messages or section `id`s (they are web addresses such as `/services/#payment-links`) unless Paul explicitly asks and IT agrees.
- Contact details (emails, phone numbers) appear in several places. Change all of them together (see the `update-contact-details` skill).

## Where things are

| Page | Address | File |
| --- | --- | --- |
| Home | `/` | `content/pages/home.yml` |
| About Us | `/about-us/` | `content/pages/about-us.yml` |
| Services | `/services/` | `content/pages/services.yml` |
| Contact Us | `/contact-us/` | `content/pages/contact-us.yml` |
| Partners | `/partners/` | `content/pages/partners.yml` |
| Privacy Policy | `/privacy-policy/` | `content/pages/privacy-policy.yml` |
| General Terms & Conditions | `/general-terms-conditions/` | `content/pages/general-terms-conditions.yml` |
| Menu, footer, logo, 404 page | every page | `content/site.yml` |
| SecurePay Guide (chat) facts and messages | every page | `content/guide.yml` |

New pages built from blocks are also in `content/pages/` (see New pages). Each page file starts with `seo` (the title and summary search engines show), then its sections in the order they appear on the page. Lists (`cards`, `points`, `items`, `logos`, `steps`, `sections`) show in file order: add, remove or reorder entries to change the page.

## New pages

A new page needs no code: create `content/pages/<address>.yml` (lower-case words joined by dashes; the page appears at `/<address>/` and in the sitemap) with `layout: "blocks"`, an `seo` title and description, and a list of `blocks`. Each block reuses the site's existing design:

| Block `type` | Fields (required in bold) | Looks like |
| --- | --- | --- |
| `intro` (must be first) | eyebrow, **title**, text | the heading area at the top of About Us |
| `image` | **src**, **alt** | the black-and-white page image |
| `facts` | **items** (value, label) | the facts strip on About Us |
| `cards` | eyebrow, **title**, text, **cards** (label, title, text, points) | "Why Work With Us?" cards |
| `details` | id, eyebrow, **title**, text, groups (title, text, points) | a Services section |
| `text` | title, **paragraphs** | Privacy Policy sections |
| `cta` | **title**, **link_label**, **url** | the "Want a Demo?" bar |

Any block can have an `id` (lower-case, dashes) so it can be linked as `/<address>/#<id>`. To show the page in the menu or footer, add a link to it in `content/site.yml`. Use the `add-website-page` skill. A page that needs a form, a new kind of block or a different design is an IT change.

## Text formatting

Content is plain text. Three marks are available, and anything else (HTML, Markdown headings, bold) is not:

- A new line inside a value becomes a line break: `"Get paid.\n==Smarter.=="`.
- `[label](address)` becomes a link. Addresses: a site path such as `/contact-us/` or `/services/#smartsell`, `https://…`, `mailto:…` or `tel:+971…`.
- `==text==` shows the text in the teal accent colour (used in page headings).

Keep values in double quotes. Typographic apostrophes and dashes (’ – “ ”) are used on the current site; keep them as written.

## Check every change before opening it for review

1. `docker compose build web` builds the site on Java 17 and runs all tests, including the content rules: every link and image must exist, no HTML in content, required fields present, and every content value must appear on its page. A failed rule names the file and field to fix.
2. `docker compose --profile preview up -d --build` serves the edited content at http://localhost:8082. Open the changed page and confirm the change looks right on a laptop and a phone width.
3. Show Paul exactly what changed: the old and new text, side by side, and the page address.

## Publishing

- Work on a branch named `content/<short-description>`; never commit to `main`.
- Commit message: what changed, in Paul's terms (for example `Services: add "Pay by Link reminders" point under Payment Links`).
- Open a pull request using the template: quote Paul's request word for word, list each change as before/after, and list the checks you ran. Content-only changes can be approved by the content owner; anything else needs IT review (see `.github/CODEOWNERS`).

## Code changes (IT)

For developers: see `README.md`. Run the full checks (`docker compose build web`, then `tools/visual_compare.py`, `tools/content_check.py` and `tools/seo_check.py` while migrating). JSP pages must print every value through an `sp:` function (`sp:text`, `sp:plain`, `sp:attr`, `sp:url`); `JspSafetyTest` fails otherwise.
