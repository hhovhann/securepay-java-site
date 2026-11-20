<%@ include file="/WEB-INF/jsp/layout/top.jspf" %>
<%-- Privacy Policy. Text comes from content/pages/privacy-policy.yml. --%>
<main id="main" class="page-shell legal-page layout-constrained"><div class="group alignwide page-intro"><p class="eyebrow">${sp:text(page.intro.eyebrow)}</p><h1>${sp:text(page.intro.title)}</h1></div>
<figure class="source-page-visual privacy-page-visual"><img src="${sp:url(page.image.src)}" alt="${sp:plain(page.image.alt)}" loading="lazy"></figure>
<article class="service-detail legal-copy">
<c:forEach items="${page.sections}" var="section"><h2>${sp:text(section.title)}</h2><c:forEach items="${section.paragraphs}" var="paragraph"><p>${sp:text(paragraph)}</p></c:forEach>
</c:forEach></article></main>
<%@ include file="/WEB-INF/jsp/layout/bottom.jspf" %>
