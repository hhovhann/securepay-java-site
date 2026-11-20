# SecurePay public website (Java pilot)

Setup, deployment and the content owner's AI workflow: [SETUP.md](SETUP.md). What can be changed and how to ask: [PAUL-GUIDE.md](PAUL-GUIDE.md).

SecurePay's public website (public.securepay.ae) rebuilt on the main-site stack: **Java 17, Spring MVC, JSP, HTML, CSS and JavaScript**, packaged as a WAR for **Apache Tomcat 11**. It replaces the WordPress site page for page, at the same addresses, with the same text, images, forms and search-engine tags. The content owner can still update it without IT, by asking Claude in plain English (see [PAUL-GUIDE.md](PAUL-GUIDE.md)).

## Stack

| | |
| --- | --- |
| Language | Java 17 (compiled for 17, tested on Corretto 17; also tested on 21 and 25) |
| Web | Spring Framework 7.0 (Spring MVC), no Spring Boot |
| Views | JSP + JSTL 3 (Jakarta Pages), plain HTML/CSS/JavaScript |
| Server | Apache Tomcat 11 (Jakarta Servlet 6.1); the WAR is deployed as `ROOT.war` |
| Content | YAML files in `content/`, packaged into the WAR and checked at start-up |
| Container (optional) | Amazon Corretto 17 on Amazon Linux 2023 + Tomcat 11.0.29 |

## Run it

```sh
docker compose up --build -d web        # build on Java 17 (runs all tests), then run in Tomcat → http://localhost:8081
docker compose --profile preview up -d --build   # content preview: edits in ./content appear on refresh → http://localhost:8082
```

Without Docker (needs JDK 17+): `./mvnw verify` builds `target/securepay-website.war`; copy it to an existing Tomcat 11 as `webapps/ROOT.war`. The site also works under a sub-path (e.g. `webapps/website.war` → `/website/`); internal links follow the deployment path.

## Layout

```
content/                 Website text, links and image references (what content requests change)
  site.yml               menu, footer, logo, 404 page
  guide.yml              SecurePay Guide panel text, messages and approved facts
  pages/*.yml            one file per page
src/main/java/ae/securepay/website/
  WebAppInitializer, WebConfig    Spring MVC set-up (no web.xml servlet config)
  content/   ContentStore (loads + validates content), ContentValidator (rules), Html (escaping/formatting for JSP), SitePage (addresses)
  web/       PageController, NotFoundAdvice (404), SeoController (robots.txt, sitemap.xml), SiteFilter (security headers)
  guide/     GuideController (POST /api/guide/ask), GuideService (matching), GuideRules (refusals), RateLimiter
src/main/webapp/
  WEB-INF/jsp/           page templates (layout/top.jspf, layout/bottom.jspf, one JSP per page)
  WEB-INF/securepay.tld  sp: functions the JSPs use to print content safely
  assets/                CSS, JavaScript, images, hotel-group logos, Terms PDF
tools/                   migration checks (visual_compare.py, content_check.py, seo_check.py)
.claude/skills/          procedures Claude follows for content requests
CLAUDE.md                rules for AI changes
```

## How content changes work

1. The content owner asks Claude (GitHub issue mentioning `@claude`, or the Claude app) in plain English.
2. Claude follows [CLAUDE.md](CLAUDE.md) and the skills in `.claude/skills/`: it edits only `content/` (and supplied images), never invents text, builds and tests on Java 17, previews the page and shows before/after.
3. It opens a pull request with the request quoted and each change listed. `.github/CODEOWNERS` lets the content owner approve content-only changes; anything else needs IT.
4. New pages need no code: a content file with `layout: "blocks"` builds a page from the site's existing design blocks (intro, image, facts, cards, details, text, call to action).
5. CI (`.github/workflows/ci.yml`) builds and tests on Java 17, runs the WAR in Tomcat 11, requests every page, flags non-content changes, and confirms the build on Java 21 and 25.

Content rules (in `ContentValidator`, run at start-up and in the tests): no HTML in content, links only to existing pages, sections or files (or `https:`, `mailto:`, `tel:`), required search-engine title and summary on every page, valid HubSpot IDs, workflow steps that match, and every content value must actually appear on its page (`RenderedSiteTest`).

## What was migrated, and how it was checked

- **Pages and addresses:** Home, About Us, Services, Contact Us, Partners, Privacy Policy, General Terms & Conditions, at the same paths. `/about-us` redirects to `/about-us/` (301); unknown addresses return a 404 page.
- **Text:** `tools/content_check.py` compares every text block on the live pages with this site: 205 of 205 present, word for word. `tools/content_check.py --extra` checks the other way round and lists text on this site that is not on the live site, footer and email fallback form included: 70 pieces, all from the redesign (see Design).
  - Home: the payment-workflow diagram and explainer, the SecurePay Guide section and panel, and labels such as "Benefits" and "01 / Bookings".
  - About Us: fact and card labels such as "Serving hotels since" and "01 / Global".
  - Contact Us and Partners: the contact line beside each form, and the email fallback form shown if HubSpot cannot load.
  - Elsewhere: short labels such as "SecurePay Services", "LinkedIn" in the footer, and "SecurePay" above the Terms.
  - Privacy Policy: none.

  SecurePay approves or removes each addition before go-live; they are in `content/` like all other text.
- **Search-engine tags:** `tools/seo_check.py`: titles, descriptions, Open Graph/Twitter tags and canonical addresses match the live site on all seven pages. `robots.txt` and `sitemap.xml` are served by the application.
- **Design:** the pages use the SecurePay Modern redesign (the WordPress theme pilot), not the current live theme. `tools/visual_compare.py` compares full-page screenshots with that theme running in WordPress at laptop (1280px) and phone (390px) widths: 0.00% differing pixels on all seven pages. The phone menu and desktop drop-down were compared item by item. One deliberate difference since then: below 1240px wide, the home-page workflow diagram covered the intro text and buttons, in the theme as well. On phones and tablets it now sits under the buttons, and on smaller laptops the text keeps clear of it (last rules in `assets/css/theme.css`). Checked with no overlap from 360px to 1920px. Also changed on purpose, again inherited from the theme (last rules in `assets/css/pages.css`):
  - On phones and tablets, the page photo and the service sections and cards (Services, About Us, Partners, Privacy Policy) ran to the screen edges. They now keep the page heading's margins (28px, 18px on phones). Between 1240px and 1296px the photo is up to 16px narrower so it lines up with the heading. From 1296px up nothing changes.
  - On phones, the floating "Ask the SecurePay Guide" button covered the copyright line at the end of every page. The footer now leaves room for it.
  - The 18 hotel-group logos on Home show in their own colours, as on the live site. The theme showed them grey and faded until pointed at, so phone visitors never saw the colours (`assets/css/theme.css`).
  - Checked on all pages at 23 sizes from 320px to 1920px, plus phones held sideways: no sideways scrolling, nothing past the screen edges, no text touching them, and the menu and the Guide open and fit on screen.
- **Images:** the page images, logo, icons and social card were copied from the WordPress media library into `assets/img/`, so nothing depends on the WordPress server.
- **Forms:** the same HubSpot forms (portal 2577418, region na1; Customer Contact `4f64ef09-…`, Partnership `cab91460-…`), with the email fallback if HubSpot cannot load. Forms were not submitted during testing.
- **Analytics:** the live pages contain the HubSpot WordPress plugin's page-type snippet but load no tracking script, so there was nothing to carry over. Confirm with SecurePay whether tracking should be added.

Before switching over: approve the content and logos, decide redirects for any old WordPress URLs linked from elsewhere (for example files under `/wp-content/uploads/`), point the proxy or load balancer at Tomcat, and keep the WordPress site as the rollback until the new site is confirmed.

## Open points

This is a working pilot built outside SecurePay's systems. To run it as SecurePay's site:

| Open point | Why it matters | What is needed |
| --- | --- | --- |
| Where the code lives | The site is meant to rejoin SecurePay's Java codebase. | Either its own repository, or a module or WAR in the main one. `CLAUDE.md` and `.claude/skills/` move with it, and the CODEOWNERS paths are adjusted so Paul still approves only content. |
| Versions | Spring Framework 7 needs Tomcat 11 (Jakarta Servlet 6.1). | Match the main site: Spring 6.2 for Tomcat 10.1, or Spring 5.3 and `javax` for Tomcat 9 (see Versions and compatibility). |
| Deployment | CI builds and tests, but nothing deploys yet. | Merge to `main`, then SecurePay's pipeline deploys to staging and then to production (SETUP.md, sections 4 and 6). |
| Preview Paul can open | The preview runs at `localhost:8082`, which only works on a developer's machine. | Screenshots of the changed pages posted on each pull request, or a staging address. |
| Approvals | `.github/CODEOWNERS` has placeholder names. | SecurePay's GitHub users or teams. |
| Content decisions | Some text and claims need SecurePay's confirmation. | Approve or remove the 70 redesign additions (Text, above). Confirm which numbers are current, hotel-group logo permissions, analytics and consent, and redirects for old WordPress addresses. |

## Security

- **Output escaping:** JSP prints `${...}` unescaped, so every value is printed through an `sp:` function (`Html`): text is HTML-escaped and only three formatting marks are turned into markup. `JspSafetyTest` fails the build if a page prints a raw value.
- **Links:** only site paths, anchors, `https:`, `mailto:` and `tel:` are linked; anything else (for example `javascript:`) is shown as text or replaced with `#`.
- **Content loading:** YAML is read with SnakeYAML's `SafeConstructor` (plain maps, lists and strings only), and invalid content stops the deployment instead of going live.
- **Headers and sessions:** `X-Content-Type-Options`, `X-Frame-Options: SAMEORIGIN`, `Referrer-Policy` and `Permissions-Policy` on every response; no sessions or cookies. The container runs Tomcat as a non-root user and its error pages do not reveal the server version.
- **SecurePay Guide API** (`POST /api/guide/ask`): 1–500 characters; card numbers, codes, passwords and key-like tokens are refused (in the browser before sending, and again on the server); prompt-override attempts are refused; cross-site requests are rejected; 20 questions per visitor address per 10 minutes (fixed window, per Tomcat instance). Answers are the approved facts in `content/guide.yml` with a link to the source page. No AI model is connected.
- **Behind a load balancer or CDN:** configure Tomcat's `RemoteIpValve` so `request.getRemoteAddr()` is the visitor's address. Only requests from the listed proxies are trusted, so the header cannot be spoofed:

  ```xml
  <!-- conf/server.xml, inside <Host> -->
  <Valve className="org.apache.catalina.valves.RemoteIpValve"
         internalProxies="10\.\d{1,3}\.\d{1,3}\.\d{1,3}"
         remoteIpHeader="x-forwarded-for" protocolHeader="x-forwarded-proto"/>
  ```

  For Cloudflare, use `remoteIpHeader="cf-connecting-ip"` and list Cloudflare's ranges in `trustedProxies`.
- **Next steps for production:**
  - a Content-Security-Policy, once the HubSpot domains in use are confirmed
  - HSTS at the proxy
  - pin the GitHub Actions to commit SHAs and the Docker base images to digests
  - turn on branch protection for `main` before the `@claude` workflow is enabled, so the agent can only open pull requests
  - under a flood from more than 50,000 visitor addresses, the Guide's rate limiter clears its table on each request; put a CDN or WAF rate rule in front if that is a concern

## Versions and compatibility

Spring Framework 7 requires Jakarta Servlet 6.1, which means Tomcat 11. If the main site runs Tomcat 10.1, the same code builds against Spring Framework 6.2 (change `spring.version` and the servlet API to 6.0 in `pom.xml`); check that line's support status first. Tomcat 9 (`javax.*`) would need the older Spring 5.3 line, which is out of open-source support. Java 17 is the target; CI also runs the tests on Java 21 and 25.

## Tests

`./mvnw verify` (or `docker compose build web`) runs 47 tests:

- `RenderedSiteTest` deploys the WAR's content into an embedded Tomcat 11 and checks every page, all content values, redirects, the 404 page, headers, assets, `robots.txt`, `sitemap.xml`, the Guide API, and deployment under a sub-path.
- `ContentValidatorTest`: the published content passes, and broken content (HTML, bad links, missing files or sections, missing search summary, invalid HubSpot ID) is caught.
- `GuideTest`, `GuideControllerTest`, `RateLimiterTest`: answers and sources, refusals, input limits, origin check and the per-visitor limit.
- `BlockPagesTest`: a new page made only of content (every block type, a menu link, sitemap entry, redirect) renders in Tomcat; mistakes such as an unknown block type, a reserved address, a missing intro or a broken section link are explained.
- `HtmlTest`, `JspSafetyTest`: escaping, link rules and the "every value escaped" rule.
