/*
 * Embeds SecurePay's existing HubSpot forms the same way the current site does
 * (hsFormsOnReady queue, portal 2577418, region na1). Each mount names a <template>
 * holding an email fallback that is shown only if the HubSpot form fails to render.
 */
(() => {
  const mounts = [...document.querySelectorAll('[data-securepay-hubspot-form]')];
  if (!mounts.length) return;

  const showFallback = mount => {
    if (mount.dataset.state === 'ready' || mount.dataset.state === 'fallback') return;
    const template = document.getElementById(mount.dataset.fallback || '');
    if (!template) return;
    mount.dataset.state = 'fallback';
    mount.replaceChildren(template.content.cloneNode(true));
  };

  const create = mount => {
    const target = mount.querySelector('.hbspt-form');
    if (!target || !target.id) return showFallback(mount);
    try {
      window.hbspt.forms.create({
        portalId: Number(mount.dataset.portalId),
        formId: mount.dataset.formId,
        target: `#${target.id}`,
        region: mount.dataset.region || 'na1',
        onFormReady: () => { mount.dataset.state = 'ready'; },
      });
    } catch (_error) {
      showFallback(mount);
    }
  };

  window.hsFormsOnReady = window.hsFormsOnReady || [];
  mounts.forEach(mount => {
    if (window.hbspt && window.hbspt.forms) {
      create(mount);
    } else {
      window.hsFormsOnReady.push(() => create(mount));
    }
    window.setTimeout(() => {
      const rendered = mount.querySelector('.hbspt-form form, .hbspt-form iframe');
      if (rendered) {
        mount.dataset.state = 'ready';
        return;
      }
      showFallback(mount);
    }, 12000);
  });
})();
