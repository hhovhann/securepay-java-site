# Setup and operation

How to run the SecurePay website locally, test it, deploy it to Tomcat, and set up the AI workflow the content owner uses. For what can be changed and how to ask, see [PAUL-GUIDE.md](PAUL-GUIDE.md); for the code, see [README.md](README.md).

## 1. What you need

| For | Needs |
| --- | --- |
| Running and testing locally | Docker Desktop (or Docker Engine with Compose) and Git. Nothing else: the build uses Java 17 inside Docker. |
| Building without Docker | JDK 17 or newer (the code targets Java 17). Maven is not needed: use `./mvnw`. |
| Production | Java 17 runtime and Apache Tomcat 11, or any container host for the provided image (Amazon Corretto 17 on Amazon Linux 2023). |
| Migration checks (optional) | Python 3.10+ and Playwright (`pip install playwright && playwright install chromium`). |
| The content owner's AI workflow | A GitHub repository (or GitLab/Bitbucket with an equivalent CI), the Claude GitHub app and an Anthropic API key, or Claude accounts for the Claude app. |

## 2. Run it locally

```sh
git clone <repository> securepay-website && cd securepay-website
docker compose up --build -d web
```

The first build downloads dependencies and takes a few minutes. It compiles on Java 17 and runs every test; if a test fails, the build stops and prints which one. When it finishes, open **http://localhost:8081**.

```sh
docker compose logs -f web      # Tomcat log
docker compose down             # stop
```

### Content preview (live editing)

```sh
docker compose --profile preview up -d --build
```

This serves **http://localhost:8082** from the `content/` folder on disk: save a change to a YAML file, refresh the page, and the change is there. If an edit breaks a content rule, the preview keeps showing the last good version and the reason is in `docker compose logs preview` (for example `pages/services.yml: sections[2].groups[1].points[3] contains HTML`).

## 3. Build and test

| Command | What it does |
| --- | --- |
| `docker compose build web` | Java 17 build plus all 47 tests, then the Tomcat 11 image |
| `./mvnw verify` | the same build and tests on your own JDK; produces `target/securepay-website.war` |
| `./mvnw test -Dtest=ContentValidatorTest` | only the content rules (fast) |

The tests render every page in a real Tomcat 11, check that every content value appears on its page, check links, images, redirects, the 404 page, security headers, `robots.txt`, `sitemap.xml`, the SecurePay Guide API, new block pages and deployment under a sub-path. See README, Tests.

Migration checks against the current site (run with the site at http://localhost:8081):

```sh
python3 tools/content_check.py   # every text block on public.securepay.ae is present
python3 tools/content_check.py --extra   # text on this site that is not on public.securepay.ae (redesign additions)
python3 tools/seo_check.py       # titles, descriptions, social tags and canonical addresses match
python3 tools/visual_compare.py [REFERENCE_URL]   # screenshot comparison with a reference copy of the current design
```

## 4. Deploy to Tomcat 11

**With your existing Tomcat 11 (Java 17):**

1. Build: `./mvnw verify` → `target/securepay-website.war`.
2. Copy it to `$CATALINA_BASE/webapps/ROOT.war` (site at the domain root, like today). Keep the previous WAR for rollback.
3. In `conf/server.xml`, inside `<Host>`:
   - hide server details on error pages:
     ```xml
     <Valve className="org.apache.catalina.valves.ErrorReportValve" showReport="false" showServerInfo="false"/>
     ```
   - behind a load balancer or CDN, so the Guide's per-visitor limit sees visitor addresses (list only your proxies):
     ```xml
     <Valve className="org.apache.catalina.valves.RemoteIpValve"
            internalProxies="10\.\d{1,3}\.\d{1,3}\.\d{1,3}"
            remoteIpHeader="x-forwarded-for" protocolHeader="x-forwarded-proto"/>
     ```
4. Start Tomcat and check `/`, `/services/`, `/robots.txt` and `/sitemap.xml`. Invalid content stops the application from starting, and the reason is in `catalina.out`.

**With the container image:** `docker build -t securepay-website .` produces an image with Amazon Corretto 17, Tomcat 11.0.29 (download checked against Apache's SHA-512) and the WAR as `ROOT.war`, running as a non-root user on port 8080.

**Rollback:** redeploy the previous WAR or image. Content is inside the WAR, so each deployment is a complete, versioned copy of the site.

**On AWS behind an Application Load Balancer** (adjust to SecurePay's own setup):

1. Run the site in one of two ways:
   - on an existing Tomcat 11 (EC2, Amazon Linux 2023), as above
   - as the container image, pushed to Amazon ECR and run as an ECS (Fargate) service on port 8080
2. Put it in its own target group with health check path `/`, which returns 200 only when the content is valid.
3. Add a listener rule that sends host `public.securepay.ae` to that target group. TLS stays on the load balancer (ACM certificate). Configure `RemoteIpValve` as above. `internalProxies` is a regular expression, not a CIDR range: for a load balancer in 10.0.0.0/16, use `10\.0\.\d{1,3}\.\d{1,3}`.
4. Rollback: switch the rule back to the WordPress target group, or redeploy the previous WAR or image.
5. Automatic deployment: after a merge to `main`, the pipeline builds the WAR or image (all tests run). It then deploys to staging and, once approved, to production: copy the WAR, or push the image and update the ECS service.

## 5. Switching over from WordPress

1. SecurePay approves the content, numbers and hotel-group logos on a staging deployment.
2. Confirm the forms on staging with HubSpot test records (same portal and form IDs as today).
3. Decide redirects for old WordPress addresses that are linked from elsewhere (for example files under `/wp-content/uploads/`); add them at the proxy or as Tomcat rewrite rules.
4. Point the domain or proxy at Tomcat. Keep the WordPress server available as the rollback until the new site is confirmed, then retire it.
5. Submit `https://public.securepay.ae/sitemap.xml` in Google Search Console.

## 6. Set up the content owner's AI workflow

The content owner (Paul) asks for changes in plain English; Claude follows `CLAUDE.md` and the skills in `.claude/skills/`, changes only `content/`, and opens a pull request.

1. **Repository:** push this project to GitHub (or your Git host).
2. **Protect `main`:** require pull requests, require the "Build and check" workflow to pass, and enable "Require review from Code Owners".
3. **Owners:** in `.github/CODEOWNERS`, replace `@securepay-it-placeholder` with IT's GitHub team or users and `@securepay-content-placeholder` with the content owner. Content-only changes can then be approved by the content owner; anything else needs IT.
4. **Claude on GitHub:** install the Claude GitHub app on the repository and add an `ANTHROPIC_API_KEY` repository secret (in Claude Code, `/install-github-app` walks through this). The workflow in `.github/workflows/claude.yml` then answers any issue or comment that mentions `@claude`.
5. **Or the Claude app:** give the content owner access to the repository in the Claude app (desktop or web). The same `CLAUDE.md` and skills apply.
6. **Deployment:** merge to `main` → your normal build and release (for example a pipeline that runs `./mvnw verify` and deploys the WAR). Merged content changes go live with the next deployment.
7. Give the content owner [PAUL-GUIDE.md](PAUL-GUIDE.md).

## 7. Extending the site

- **New pages without code:** `content/pages/<address>.yml` with `layout: "blocks"` (see `CLAUDE.md`, New pages, and the `add-website-page` skill).
- **A new block type** (for example a gallery or a form): add a `<c:when>` to `WEB-INF/jsp/blocks.jsp` using the existing CSS, add its required fields to `ContentValidator.BLOCKS`, document it in `CLAUDE.md`, and add a case to `BlockPagesTest`.
- **A new page with its own design:** add it to `SitePage`, add `WEB-INF/jsp/<slug>.jsp` and `content/pages/<slug>.yml`; `RenderedSiteTest` then checks that all of its content appears.
- **Rules for JSP changes:** print every value through an `sp:` function (`sp:text`, `sp:plain`, `sp:attr`, `sp:url`); `JspSafetyTest` fails otherwise.

## 8. Troubleshooting

| Symptom | Cause and fix |
| --- | --- |
| Build fails with `Website content has N problem(s)` | The list names each file and field; fix the content and rebuild. |
| A page shows 404 | The address must end with `/`, and new pages need `layout: "blocks"`. |
| Preview does not change | `docker compose logs preview` shows why the edit was not loaded. |
| Forms show "The form could not load" | HubSpot did not load (blocked or offline); the email fallback is shown instead. |
| Migration checks stop with `CERTIFICATE_VERIFY_FAILED` | Python from python.org on macOS has no certificate bundle. Run its `Install Certificates.command`, or prefix the command with `SSL_CERT_FILE=/etc/ssl/cert.pem`. |
| Guide says "Please wait a few minutes" | The per-visitor limit (20 questions per 10 minutes) was reached; behind a proxy, configure `RemoteIpValve`. |
