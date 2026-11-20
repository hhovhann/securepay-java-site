<%@ include file="/WEB-INF/jsp/layout/top.jspf" %>
<%-- A page built from blocks (content/pages/<name>.yml with layout: blocks). Each block reuses the site's existing design. --%>
<main id="main" class="page-shell blocks-page layout-constrained">
<c:forEach items="${page.blocks}" var="block">
<c:choose>
<c:when test="${block.type eq 'intro'}">
<div class="group alignwide page-intro"<c:if test="${not empty block.id}"> id="${sp:attr(block.id)}"</c:if>><c:if test="${not empty block.eyebrow}"><p class="eyebrow">${sp:text(block.eyebrow)}</p></c:if><h1>${sp:text(block.title)}</h1><c:if test="${not empty block.text}"><p class="section-intro">${sp:text(block.text)}</p></c:if></div>
</c:when>
<c:when test="${block.type eq 'image'}">
<figure class="source-page-visual"<c:if test="${not empty block.id}"> id="${sp:attr(block.id)}"</c:if>><img src="${sp:url(block.src)}" alt="${sp:plain(block.alt)}" loading="lazy"></figure>
</c:when>
<c:when test="${block.type eq 'facts'}">
<div class="proof-strip about-proof"<c:if test="${not empty block.id}"> id="${sp:attr(block.id)}"</c:if>><c:forEach items="${block.items}" var="fact"><div><strong>${sp:text(fact.value)}</strong><span>${sp:text(fact.label)}</span></div></c:forEach></div>
</c:when>
<c:when test="${block.type eq 'cards'}">
<section class="service-detail"<c:if test="${not empty block.id}"> id="${sp:attr(block.id)}"</c:if>><c:if test="${not empty block.eyebrow}"><span>${sp:text(block.eyebrow)}</span></c:if><h2>${sp:text(block.title)}</h2><c:if test="${not empty block.text}"><p>${sp:text(block.text)}</p></c:if><div class="service-grid"><c:forEach items="${block.cards}" var="card"><article class="service-card"><c:if test="${not empty card.label}"><span class="card-index">${sp:text(card.label)}</span></c:if><h3>${sp:text(card.title)}</h3><c:if test="${not empty card.text}"><p>${sp:text(card.text)}</p></c:if><c:if test="${not empty card.points}"><ul><c:forEach items="${card.points}" var="point"><li>${sp:text(point)}</li></c:forEach></ul></c:if></article></c:forEach></div></section>
</c:when>
<c:when test="${block.type eq 'details'}">
<section class="service-detail"<c:if test="${not empty block.id}"> id="${sp:attr(block.id)}"</c:if>><c:if test="${not empty block.eyebrow}"><span>${sp:text(block.eyebrow)}</span></c:if><h2>${sp:text(block.title)}</h2>
<c:if test="${not empty block.text}"><p>${sp:text(block.text)}</p></c:if>
<c:forEach items="${block.groups}" var="group"><h3>${sp:text(group.title)}</h3><c:if test="${not empty group.text}"><p>${sp:text(group.text)}</p></c:if><c:if test="${not empty group.points}"><ul><c:forEach items="${group.points}" var="point"><li>${sp:text(point)}</li></c:forEach></ul></c:if>
</c:forEach></section>
</c:when>
<c:when test="${block.type eq 'text'}">
<article class="service-detail legal-copy"<c:if test="${not empty block.id}"> id="${sp:attr(block.id)}"</c:if>><c:if test="${not empty block.title}"><h2>${sp:text(block.title)}</h2></c:if><c:forEach items="${block.paragraphs}" var="paragraph"><p>${sp:text(paragraph)}</p></c:forEach></article>
</c:when>
<c:when test="${block.type eq 'cta'}">
<div class="inline-cta"<c:if test="${not empty block.id}"> id="${sp:attr(block.id)}"</c:if>><strong>${sp:text(block.title)}</strong><a href="${sp:url(block.url)}">${sp:text(block.link_label)}</a></div>
</c:when>
</c:choose>
</c:forEach>
</main>
<%@ include file="/WEB-INF/jsp/layout/bottom.jspf" %>
