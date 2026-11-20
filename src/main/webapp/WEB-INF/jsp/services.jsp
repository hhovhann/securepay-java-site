<%@ include file="/WEB-INF/jsp/layout/top.jspf" %>
<%-- Services. Text comes from content/pages/services.yml. --%>
<main id="main" class="page-shell services-page layout-constrained"><div class="group alignwide page-intro"><p class="eyebrow">${sp:text(page.intro.eyebrow)}</p><h1>${sp:text(page.intro.title)}</h1><p class="section-intro">${sp:text(page.intro.text)}</p><nav class="service-tabs"><c:forEach items="${page.intro.tabs}" var="tab"><a href="${sp:url(tab.url)}">${sp:text(tab.label)}</a></c:forEach></nav></div>
<figure class="source-page-visual services-page-visual"><img src="${sp:url(page.image.src)}" alt="${sp:plain(page.image.alt)}" loading="lazy"></figure>
<c:forEach items="${page.sections}" var="section"><section class="service-detail" id="${sp:attr(section.id)}"><span>${sp:text(section.eyebrow)}</span><h2>${sp:text(section.title)}</h2>
<p>${sp:text(section.text)}</p>
<c:forEach items="${section.groups}" var="group"><h3>${sp:text(group.title)}</h3><p>${sp:text(group.text)}</p><ul><c:forEach items="${group.points}" var="point"><li>${sp:text(point)}</li></c:forEach></ul>
</c:forEach></section>
</c:forEach>
<div class="inline-cta"><strong>${sp:text(page.cta.title)}</strong><a href="${sp:url(page.cta.url)}">${sp:text(page.cta.link_label)}</a></div></main>
<%@ include file="/WEB-INF/jsp/layout/bottom.jspf" %>
