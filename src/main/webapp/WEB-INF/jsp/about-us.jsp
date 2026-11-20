<%@ include file="/WEB-INF/jsp/layout/top.jspf" %>
<%-- About Us. Text comes from content/pages/about-us.yml. --%>
<main id="main" class="page-shell about-page layout-constrained"><div class="group alignwide page-intro"><p class="eyebrow">${sp:text(page.intro.eyebrow)}</p><h1>${sp:text(page.intro.title)}</h1><p class="section-intro">${sp:text(page.intro.text)}</p></div>
<figure class="source-page-visual about-page-visual"><img src="${sp:url(page.image.src)}" alt="${sp:plain(page.image.alt)}" loading="lazy"></figure>
<div class="proof-strip about-proof"><c:forEach items="${page.facts}" var="fact"><div><strong>${sp:text(fact.value)}</strong><span>${sp:text(fact.label)}</span></div></c:forEach></div>
<section class="service-detail"><span>${sp:text(page.why.eyebrow)}</span><h2>${sp:text(page.why.title)}</h2><div class="service-grid"><c:forEach items="${page.why.cards}" var="card"><article class="service-card"><span class="card-index">${sp:text(card.label)}</span><h3>${sp:text(card.title)}</h3><p>${sp:text(card.text)}</p></article></c:forEach></div></section>
<div class="inline-cta"><strong>${sp:text(page.cta.title)}</strong><a href="${sp:url(page.cta.url)}">${sp:text(page.cta.link_label)}</a></div></main>
<%@ include file="/WEB-INF/jsp/layout/bottom.jspf" %>
