(() => {
  document.addEventListener('submit', event => {
    const form = event.target.closest('[data-securepay-mailto-form]');
    if (!form) return;
    event.preventDefault();
    const values = [...new FormData(form).entries()]
      .map(([name, value]) => [name, String(value).trim()])
      .filter(([, value]) => value)
      .map(([name, value]) => `${name}: ${value}`)
      .join('\n');
    const recipient = form.dataset.recipient || '';
    if (!/^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}$/i.test(recipient)) return;
    const subject = form.dataset.subject || 'SecurePay website enquiry';
    const url = `mailto:${recipient}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(values)}`;
    window.location.href = url;
  });
})();
