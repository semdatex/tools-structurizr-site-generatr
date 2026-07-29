// Fills the version switcher from versions.json in the site root, instead of the list being
// baked into every page at generation time. That keeps a rendered version's pages independent
// of which other versions the site carries, so they stay correct when versions are added or
// removed without re-rendering them (e.g. when they are restored from a build cache).
(function () {
  function render(container, versions, siteRoot, currentVersion) {
    var branches = versions.branches || [];
    var tags = versions.tags || [];

    function item(name) {
      var link = document.createElement('a');
      link.className = 'navbar-item';
      link.href = siteRoot + encodeURIComponent(name) + '/';
      link.textContent = name;
      if (name === currentVersion) link.setAttribute('aria-current', 'true');
      return link;
    }

    var fragment = document.createDocumentFragment();
    branches.forEach(function (branch) {
      fragment.appendChild(item(branch));
    });

    if (tags.length > 0) {
      var divider = document.createElement('hr');
      divider.className = 'navbar-divider';
      fragment.appendChild(divider);

      var label = document.createElement('div');
      label.className = 'navbar-item has-text-grey-light';
      label.textContent = 'Tags';
      fragment.appendChild(label);

      tags.forEach(function (tag) {
        fragment.appendChild(item(tag));
      });
    }

    container.replaceChildren(fragment);
  }

  function init() {
    var container = document.getElementById('version-switcher-items');
    if (!container) return;

    var versionsUrl = container.getAttribute('data-versions-url');
    var currentVersion = container.getAttribute('data-current-version');
    if (!versionsUrl) return;

    // versions.json sits in the site root, so its URL minus the file name is the root — the
    // base every sibling version directory hangs off. Works at any mount point and depth.
    var siteRoot = versionsUrl.replace(/versions\.json$/, '');

    fetch(versionsUrl)
      .then(function (response) {
        if (!response.ok) throw new Error('versions.json: HTTP ' + response.status);
        return response.json();
      })
      .then(function (versions) {
        render(container, versions, siteRoot, currentVersion);
      })
      .catch(function (error) {
        // Leave the switcher empty rather than breaking the page; the current version is still
        // shown as the dropdown label and every other link on the page keeps working.
        console.warn('Could not load the version switcher:', error);
      });
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
  else init();
})();
