/*
 * SecurePay site behaviour: the main menu, counting numbers, the payment workflow and the SecurePay Guide.
 * All visible text comes from the page (rendered from content/*.yml); nothing here is page copy.
 */
(() => {
  // Main menu: full-screen on phones, dropdown on larger screens.
  const nav = document.querySelector('[data-site-nav]');
  if (nav) {
    const panel = nav.querySelector('.nav-panel');
    const openButton = nav.querySelector('[data-nav-open]');
    const closeButton = nav.querySelector('[data-nav-close]');
    const setMenu = open => {
      panel.classList.toggle('is-menu-open', open);
      document.documentElement.classList.toggle('has-modal-open', open);
      openButton.setAttribute('aria-expanded', open ? 'true' : 'false');
      if (open) {
        panel.setAttribute('role', 'dialog');
        panel.setAttribute('aria-modal', 'true');
        panel.setAttribute('aria-label', 'Menu');
        closeButton.focus();
      } else {
        panel.removeAttribute('role');
        panel.removeAttribute('aria-modal');
        panel.removeAttribute('aria-label');
      }
    };
    openButton.addEventListener('click', () => setMenu(true));
    closeButton.addEventListener('click', () => { setMenu(false); openButton.focus(); });
    nav.querySelectorAll('[data-nav-submenu-toggle]').forEach(toggle => toggle.addEventListener('click', () => {
      const item = toggle.closest('[data-nav-submenu]');
      const open = !item.classList.contains('is-open');
      item.classList.toggle('is-open', open);
      toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
    }));
    document.addEventListener('keydown', event => {
      if (event.key !== 'Escape') return;
      if (panel.classList.contains('is-menu-open')) { setMenu(false); openButton.focus(); }
      nav.querySelectorAll('[data-nav-submenu].is-open').forEach(item => {
        item.classList.remove('is-open');
        item.querySelector('[data-nav-submenu-toggle]').setAttribute('aria-expanded', 'false');
      });
    });
    document.addEventListener('click', event => {
      nav.querySelectorAll('[data-nav-submenu].is-open').forEach(item => {
        if (!item.contains(event.target)) {
          item.classList.remove('is-open');
          item.querySelector('[data-nav-submenu-toggle]').setAttribute('aria-expanded', 'false');
        }
      });
    });
  }

  // Published numbers count up when they scroll into view ("130,000+", "$US 1.5 B+").
  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const statNumbers = [...document.querySelectorAll('.stat-value')].filter(el => /\d/.test(el.textContent));
  if (statNumbers.length && !reducedMotion && 'IntersectionObserver' in window) {
    const counterObserver = new IntersectionObserver(entries => entries.forEach(entry => {
      if (!entry.isIntersecting || entry.target.dataset.counted) return;
      const el = entry.target; el.dataset.counted = 'true';
      const finalText = el.textContent.trim();
      const parts = finalText.match(/^(.*?)(\d[\d,]*(?:\.\d+)?)(.*)$/);
      if (!parts) return;
      const [, prefix, number, suffix] = parts;
      const target = Number(number.replace(/,/g, ''));
      const decimals = number.includes('.') ? number.split('.')[1].length : 0;
      const grouped = number.includes(',');
      const started = performance.now(); const duration = 1200;
      el.setAttribute('aria-label', finalText);
      const frame = now => {
        const t = Math.min(1, (now - started) / duration); const eased = 1 - Math.pow(1 - t, 3);
        const value = t < 1 ? target * eased : target;
        const formatted = new Intl.NumberFormat('en-GB', {maximumFractionDigits: decimals, minimumFractionDigits: decimals, useGrouping: grouped}).format(decimals ? value : Math.round(value));
        el.textContent = t < 1 ? prefix + formatted + suffix : finalText;
        if (t < 1) requestAnimationFrame(frame);
      }; requestAnimationFrame(frame); counterObserver.unobserve(el);
    }), {threshold: 0.65});
    statNumbers.forEach(el => counterObserver.observe(el));
  }

  // Payment workflow: the hero stages and the four steps select the same detail text.
  const flowButtons = document.querySelectorAll('[data-flow-step]');
  flowButtons.forEach(button => button.addEventListener('click', () => {
    const step = button.dataset.flowStep;
    const source = document.querySelector(`.journey-step[data-flow-step="${CSS.escape(step)}"]`);
    flowButtons.forEach(other => {
      const selected = other.dataset.flowStep === step;
      other.classList.toggle('is-active', selected);
      if (other.getAttribute('role') === 'tab') other.setAttribute('aria-selected', selected ? 'true' : 'false');
      else other.setAttribute('aria-pressed', selected ? 'true' : 'false');
    });
    const titleEl = document.querySelector('[data-flow-title]');
    const copyEl = document.querySelector('[data-flow-copy]');
    if (source && titleEl) titleEl.textContent = source.dataset.detailTitle || '';
    if (source && copyEl) copyEl.textContent = source.dataset.detail || '';
  }));

  // SecurePay Guide.
  const shell = document.querySelector('[data-guide]');
  if (!shell) return;
  const body = shell.querySelector('[data-guide-messages]');
  const form = shell.querySelector('[data-guide-form]');
  const input = form.querySelector('input');
  const endpoint = shell.dataset.endpoint;
  const messages = { removed: shell.dataset.msgRemoved, refused: shell.dataset.msgRefused, unavailable: shell.dataset.msgUnavailable };

  // Same rules as GuideRules.java: sensitive data is removed here, before anything is sent.
  const sensitiveLabel = '(?:credit card number|card number|card details|cvv|cvc|security code|booking reference|booking ref|reservation number|confirmation number|account number|password|passcode|api key|secret key|access token)s?';
  const sensitive = [
    /(?:^|\D)(?:\d[ -]?){13,19}(?!\d)/,
    new RegExp(`\\b${sensitiveLabel}\\b\\W{0,4}(?:(?:is|was)\\W{1,3})?[a-z0-9_-]*\\d[a-z0-9_-]*`, 'i'),
    new RegExp(`\\b${sensitiveLabel}\\s*[:=]\\s*\\S{4,}`, 'i'),
    /\b(?=[a-z0-9_-]*\d)(?=[a-z0-9_-]*[a-z])[a-z0-9_-]{28,}\b/i,
  ];
  const hasSensitiveData = text => sensitive.some(pattern => pattern.test(text));
  const promptAttack = /\b(?:ignore|disregard|override|forget)\s+(?:(?:all|any|the|these|those)\s+)*(?:instructions|prompts?|guidelines)\b|\b(?:ignore|disregard|override|forget)\b.{0,25}\b(?:previous|prior|above|earlier|your|system|developer)\b.{0,15}\b(?:instructions|rules|prompts?|guidelines|messages?)\b|\b(?:system prompt|hidden prompt|developer message|initial instructions|jailbreak|roleplay as|pretend (?:to be|you are)|you are now (?:a|an|my)|dan mode)\b|\b(?:reveal|repeat|print|show|display|leak|output)\b.{0,25}\byour\s+(?:instructions|prompt|rules|api key|secrets?|credentials|configuration)\b/i;

  function appendMessage(content, kind, sources) {
    const item = document.createElement('div');
    item.className = `guide-message ${kind === 'user' ? 'guide-user' : 'guide-assistant'}`;
    item.textContent = content;
    if (sources?.length) {
      item.append(document.createElement('br'));
      sources.forEach((entry, index) => {
        const link = document.createElement('a');
        link.className = 'guide-source';
        link.href = entry.url;
        link.textContent = `${index ? ' · ' : 'Source: '}${entry.label} →`;
        item.append(link);
      });
    }
    body.append(item);
    body.scrollTop = body.scrollHeight;
  }

  function submit(question) {
    const value = question.trim().slice(0, 500);
    if (!value) return;
    input.value = '';
    if (hasSensitiveData(value)) { appendMessage(messages.removed, 'assistant'); return; }
    if (promptAttack.test(value)) { appendMessage(messages.refused, 'assistant'); return; }
    appendMessage(value, 'user');
    const unavailable = () => appendMessage(messages.unavailable, 'assistant');
    if (!endpoint || !window.fetch) { unavailable(); return; }
    // AbortSignal.timeout() is missing in older browsers, so time out with a controller.
    const controller = window.AbortController ? new AbortController() : null;
    const timer = controller ? window.setTimeout(() => controller.abort(), 22000) : null;
    let request;
    try {
      request = fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ question: value }),
        credentials: 'same-origin',
        signal: controller ? controller.signal : undefined,
      });
    } catch (error) {
      request = Promise.reject(error);
    }
    request.then(response => response.json().then(result => result, () => null))
      .then(result => {
        if (result && typeof result.answer === 'string') appendMessage(result.answer, 'assistant', result.sources || []);
        else if (result && typeof result.error === 'string') appendMessage(result.error, 'assistant');
        else unavailable();
      }, unavailable)
      .then(() => { if (timer) window.clearTimeout(timer); });
  }

  let returnFocus = null;
  function open(event) {
    returnFocus = event.currentTarget;
    shell.hidden = false;
    input.focus();
  }
  function close() {
    shell.hidden = true;
    returnFocus?.focus();
  }
  document.querySelectorAll('[data-guide-open]').forEach(button => button.addEventListener('click', open));
  shell.querySelector('[data-guide-close]').addEventListener('click', close);
  shell.addEventListener('click', event => { if (event.target === shell) close(); });
  document.addEventListener('keydown', event => {
    if (shell.hidden) return;
    if (event.key === 'Escape') close();
    if (event.key === 'Tab') {
      const focusable = [...shell.querySelectorAll('button:not([disabled]), input:not([disabled]), a[href]')];
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
      else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
    }
  });
  body.querySelectorAll('[data-question]').forEach(button => button.addEventListener('click', () => submit(button.dataset.question)));
  form.addEventListener('submit', event => { event.preventDefault(); submit(input.value); });
})();
