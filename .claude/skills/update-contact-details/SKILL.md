---
name: update-contact-details
description: Change SecurePay's contact details on the website, such as a sales, support or partnerships email address, a phone number, the LinkedIn link or the company address. Use when Paul asks to update an email, phone number or address.
---

# Update contact details everywhere

Contact details appear in several places. A partial change leaves the site inconsistent, so always change every occurrence in one go.

1. Search all content for the old value, in both written forms:
   - phone: `grep -rn "50 478 0251\|504780251" content/` (spaced display form and `tel:` form)
   - email: `grep -rni "sales@securepay.ae" content/` (the site writes some addresses with a capital first letter, such as `Sales@securepay.ae`; keep each place's style)
2. The usual places are: the footer (`content/site.yml`), the Contact Us intro text and form panel, the Partners enquiry text and form panel, each form's email fallback `recipient` and `message`, the `noscript` text, and the Guide's partner answer in `content/guide.yml`.
3. Show Paul the full list of places with before and after, and get his "yes" before editing.
4. For phone links, keep the display text spaced (`+971 50 478 0251`) and the link digits-only (`tel:+971504780251`).
5. Changing where a HubSpot form's submissions go is done in HubSpot, not here. Tell Paul if that is what he needs.

Continue with the `publish-website-change` skill.
