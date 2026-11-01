/*
 * Knowledge Base Helpdesk — client-side enhancements.
 *
 * Everything here is progressive enhancement over server-rendered pages:
 * forms, links and search all work without JavaScript. Modules are wired up
 * by data attributes so templates stay declarative.
 */
(function () {
    'use strict';

    var root = document.documentElement;
    var desktop = window.matchMedia('(min-width: 1024px)');
    var prefersDark = window.matchMedia('(prefers-color-scheme: dark)');

    function store(key, value) {
        try {
            if (value === null) localStorage.removeItem(key);
            else localStorage.setItem(key, value);
        } catch (e) { /* storage unavailable */ }
    }

    function icon(name, cls) {
        return '<svg class="icon ' + (cls || '') + '" aria-hidden="true"><use href="#i-' + name + '"/></svg>';
    }

    /* ---------------------------------------------------------------- Toasts */

    var toastRegion = document.querySelector('.toast-region');
    var toastIcons = { success: 'check-circle', warning: 'alert-triangle', error: 'alert-circle', info: 'info' };

    function dismissToast(toast) {
        if (!toast || toast.classList.contains('is-leaving')) return;
        toast.classList.add('is-leaving');
        setTimeout(function () { toast.remove(); }, 220);
    }

    function armToast(toast) {
        var timer;
        var start = function () { timer = setTimeout(function () { dismissToast(toast); }, 5000); };
        var stop = function () { clearTimeout(timer); };
        toast.addEventListener('mouseenter', stop);
        toast.addEventListener('mouseleave', start);
        toast.addEventListener('focusin', stop);
        toast.addEventListener('focusout', start);
        var close = toast.querySelector('.toast-close');
        if (close) close.addEventListener('click', function () { dismissToast(toast); });
        start();
    }

    function showToast(message, type) {
        if (!toastRegion) return;
        type = toastIcons[type] ? type : 'info';
        var toast = document.createElement('div');
        toast.className = 'toast toast-' + type;
        toast.setAttribute('role', type === 'error' ? 'alert' : 'status');
        toast.innerHTML = icon(toastIcons[type], 'toast-icon') +
            '<p class="toast-text"></p>' +
            '<button type="button" class="icon-btn icon-btn-sm toast-close" aria-label="Dismiss notification">' +
            icon('x') + '</button>';
        toast.querySelector('.toast-text').textContent = message;
        toastRegion.appendChild(toast);
        armToast(toast);
    }

    document.querySelectorAll('.toast').forEach(armToast);
    window.kbToast = showToast;

    /* ----------------------------------------------------------------- Theme */

    function effectiveTheme() {
        var explicit = root.getAttribute('data-theme');
        if (explicit) return explicit;
        return prefersDark.matches ? 'dark' : 'light';
    }

    function syncThemeButtons() {
        var next = effectiveTheme() === 'dark' ? 'light' : 'dark';
        document.querySelectorAll('[data-action="toggle-theme"]').forEach(function (btn) {
            btn.setAttribute('aria-label', 'Switch to ' + next + ' theme');
            btn.setAttribute('title', 'Switch to ' + next + ' theme');
        });
    }

    document.addEventListener('click', function (event) {
        if (!event.target.closest('[data-action="toggle-theme"]')) return;
        var next = effectiveTheme() === 'dark' ? 'light' : 'dark';
        root.setAttribute('data-theme', next);
        store('kb-theme', next);
        syncThemeButtons();
    });

    prefersDark.addEventListener('change', syncThemeButtons);
    syncThemeButtons();

    /* ----------------------------------------------------- Sidebar & drawer */

    var sidebar = document.getElementById('sidebar');
    var scrim = document.querySelector('.scrim');
    var menuBtn = document.querySelector('[data-action="open-nav"]');
    var collapseBtn = document.querySelector('[data-action="toggle-sidebar"]');

    function syncCollapse() {
        if (!collapseBtn) return;
        var collapsed = root.getAttribute('data-sidebar') === 'collapsed';
        collapseBtn.setAttribute('aria-expanded', String(!collapsed));
        collapseBtn.setAttribute('data-tooltip', collapsed ? 'Expand sidebar' : 'Collapse sidebar');
        collapseBtn.querySelector('.nav-text').textContent = collapsed ? 'Expand sidebar' : 'Collapse sidebar';
    }

    if (collapseBtn) {
        collapseBtn.addEventListener('click', function () {
            var collapsed = root.getAttribute('data-sidebar') === 'collapsed';
            if (collapsed) root.removeAttribute('data-sidebar');
            else root.setAttribute('data-sidebar', 'collapsed');
            store('kb-sidebar', collapsed ? null : 'collapsed');
            syncCollapse();
        });
        syncCollapse();
    }

    function openNav() {
        if (!sidebar) return;
        root.classList.add('nav-open');
        if (scrim) scrim.hidden = false;
        if (menuBtn) menuBtn.setAttribute('aria-expanded', 'true');
        var first = sidebar.querySelector('.nav-item.active') || sidebar.querySelector('.nav-item');
        if (first) setTimeout(function () { first.focus(); }, 60);
    }

    function closeNav(restoreFocus) {
        if (!root.classList.contains('nav-open')) return;
        root.classList.remove('nav-open');
        if (scrim) scrim.hidden = true;
        if (menuBtn) {
            menuBtn.setAttribute('aria-expanded', 'false');
            if (restoreFocus) menuBtn.focus();
        }
    }

    if (menuBtn) menuBtn.addEventListener('click', openNav);
    document.querySelectorAll('[data-action="close-nav"]').forEach(function (el) {
        el.addEventListener('click', function () { closeNav(true); });
    });
    desktop.addEventListener('change', function (e) { if (e.matches) closeNav(false); });

    if (sidebar) {
        sidebar.addEventListener('keydown', function (event) {
            if (event.key !== 'Tab' || !root.classList.contains('nav-open')) return;
            var focusable = Array.prototype.filter.call(
                sidebar.querySelectorAll('a[href], button:not([disabled])'),
                function (el) { return el.offsetParent !== null; });
            if (!focusable.length) return;
            var first = focusable[0];
            var last = focusable[focusable.length - 1];
            if (event.shiftKey && document.activeElement === first) {
                event.preventDefault();
                last.focus();
            } else if (!event.shiftKey && document.activeElement === last) {
                event.preventDefault();
                first.focus();
            }
        });
    }

    /* ------------------------------------------------------- Dropdown menus */

    function menuItems(panel) {
        return Array.prototype.slice.call(panel.querySelectorAll('[role="menuitem"]'));
    }

    function closeMenu(menu, restoreFocus) {
        var trigger = menu.querySelector('[data-menu-trigger]');
        var panel = menu.querySelector('[role="menu"]');
        if (!panel || panel.hidden) return;
        panel.hidden = true;
        trigger.setAttribute('aria-expanded', 'false');
        if (restoreFocus) trigger.focus();
    }

    function openMenu(menu, focusLast) {
        var trigger = menu.querySelector('[data-menu-trigger]');
        var panel = menu.querySelector('[role="menu"]');
        panel.hidden = false;
        trigger.setAttribute('aria-expanded', 'true');
        var items = menuItems(panel);
        if (items.length) items[focusLast ? items.length - 1 : 0].focus();
    }

    document.querySelectorAll('[data-menu]').forEach(function (menu) {
        var trigger = menu.querySelector('[data-menu-trigger]');
        var panel = menu.querySelector('[role="menu"]');
        if (!trigger || !panel) return;

        trigger.addEventListener('click', function () {
            if (panel.hidden) openMenu(menu, false);
            else closeMenu(menu, false);
        });

        trigger.addEventListener('keydown', function (event) {
            if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
                event.preventDefault();
                openMenu(menu, event.key === 'ArrowUp');
            }
        });

        panel.addEventListener('keydown', function (event) {
            var items = menuItems(panel);
            var index = items.indexOf(document.activeElement);
            if (event.key === 'ArrowDown') {
                event.preventDefault();
                items[(index + 1) % items.length].focus();
            } else if (event.key === 'ArrowUp') {
                event.preventDefault();
                items[(index - 1 + items.length) % items.length].focus();
            } else if (event.key === 'Home') {
                event.preventDefault();
                items[0].focus();
            } else if (event.key === 'End') {
                event.preventDefault();
                items[items.length - 1].focus();
            } else if (event.key === 'Tab') {
                closeMenu(menu, false);
            }
        });
    });

    document.addEventListener('click', function (event) {
        document.querySelectorAll('[data-menu]').forEach(function (menu) {
            if (!menu.contains(event.target)) closeMenu(menu, false);
        });
    });

    /* ------------------------------------------------------- Global keys */

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape') {
            var openMenuEl = document.querySelector('[data-menu] [data-menu-trigger][aria-expanded="true"]');
            if (openMenuEl) {
                closeMenu(openMenuEl.closest('[data-menu]'), true);
                return;
            }
            closeNav(true);
            return;
        }

        if (event.key === '/' && !event.ctrlKey && !event.metaKey && !event.altKey) {
            var target = event.target;
            var typing = target.isContentEditable || /^(INPUT|TEXTAREA|SELECT)$/.test(target.tagName);
            if (typing) return;
            var search = document.querySelector('[data-search-primary]') || document.querySelector('[data-shortcut="/"]');
            if (search) {
                event.preventDefault();
                search.focus();
                search.select();
            }
        }
    });

    /* ---------------------------------------------------- Confirm dialog */

    var dialog = document.getElementById('confirm-dialog');
    var pendingForm = null;

    function askConfirm(form) {
        var title = form.getAttribute('data-confirm-title') || 'Are you sure?';
        var body = form.getAttribute('data-confirm') || 'This action cannot be undone.';
        var action = form.getAttribute('data-confirm-action') || 'Confirm';

        if (!dialog || typeof dialog.showModal !== 'function') {
            if (window.confirm(title + '\n\n' + body)) {
                form.dataset.confirmed = 'true';
                form.submit();
            }
            return;
        }

        pendingForm = form;
        dialog.querySelector('.dialog-title').textContent = title;
        dialog.querySelector('.dialog-body').textContent = body;
        dialog.querySelector('[data-dialog-confirm]').textContent = action;
        dialog.showModal();
        dialog.querySelector('[data-dialog-cancel]').focus();
    }

    document.addEventListener('submit', function (event) {
        var form = event.target;
        if (!form.hasAttribute('data-confirm') || form.dataset.confirmed === 'true') return;
        event.preventDefault();
        askConfirm(form);
    });

    if (dialog) {
        dialog.querySelector('[data-dialog-cancel]').addEventListener('click', function () {
            dialog.close();
        });

        dialog.querySelector('[data-dialog-confirm]').addEventListener('click', function () {
            var form = pendingForm;
            var btn = this;
            if (!form) return;
            btn.disabled = true;
            btn.textContent = 'Deleting…';
            form.dataset.confirmed = 'true';
            form.submit();
        });

        dialog.addEventListener('click', function (event) {
            if (event.target === dialog) dialog.close();
        });

        dialog.addEventListener('close', function () {
            pendingForm = null;
            var btn = dialog.querySelector('[data-dialog-confirm]');
            btn.disabled = false;
        });
    }

    /* ---------------------------------------------- Submit loading states */

    function setLoading(button, label) {
        if (!button) return;
        button.classList.add('is-loading');
        button.setAttribute('aria-disabled', 'true');
        var text = button.querySelector('[data-label]');
        if (text && label) text.textContent = label;
    }

    /* ----------------------------------------------------- Form validation */

    function fieldOf(control) {
        return control.closest('.field');
    }

    function clearError(control) {
        var field = fieldOf(control);
        if (!field) return;
        field.classList.remove('has-error');
        control.removeAttribute('aria-invalid');
        var error = field.querySelector('.field-error');
        if (error) error.remove();
    }

    function showError(control, message) {
        var field = fieldOf(control);
        if (!field) return;
        clearError(control);
        field.classList.add('has-error');
        control.setAttribute('aria-invalid', 'true');
        var error = document.createElement('p');
        error.className = 'field-error';
        error.id = control.id + '-error';
        error.innerHTML = icon('alert-circle');
        error.appendChild(document.createTextNode(message));
        field.appendChild(error);
        var describedBy = (control.getAttribute('aria-describedby') || '').split(' ').filter(function (id) {
            return id && id !== error.id;
        });
        describedBy.push(error.id);
        control.setAttribute('aria-describedby', describedBy.join(' '));
    }

    document.querySelectorAll('form[data-validate]').forEach(function (form) {
        form.noValidate = true;
        var controls = Array.prototype.slice.call(form.querySelectorAll('[required]'));

        controls.forEach(function (control) {
            control.addEventListener('input', function () {
                if (fieldOf(control) && fieldOf(control).classList.contains('has-error') && control.value.trim()) {
                    clearError(control);
                }
            });
            control.addEventListener('blur', function () {
                if (control.dataset.touched !== 'true') return;
                if (!control.value.trim()) showError(control, control.getAttribute('data-required-message') || 'This field is required.');
            });
            control.addEventListener('input', function () { control.dataset.touched = 'true'; }, { once: true });
        });

        form.addEventListener('submit', function (event) {
            if (form.dataset.submitting === 'true') {
                event.preventDefault();
                return;
            }

            var invalid = controls.filter(function (control) { return !control.value.trim(); });
            if (invalid.length) {
                event.preventDefault();
                invalid.forEach(function (control) {
                    showError(control, control.getAttribute('data-required-message') || 'This field is required.');
                });
                invalid[0].focus();
                return;
            }

            form.dataset.submitting = 'true';
            setLoading(form.querySelector('[data-submit]'), form.getAttribute('data-loading-label'));
        });
    });

    // A page restored from the back/forward cache keeps its JS state. Reset
    // submitted forms so their buttons are usable again (e.g. after pressing
    // Back to the sign-in page).
    window.addEventListener('pageshow', function (event) {
        if (!event.persisted) return;
        document.querySelectorAll('form[data-submitting="true"]').forEach(function (form) {
            delete form.dataset.submitting;
            var button = form.querySelector('[data-submit]');
            if (!button) return;
            button.classList.remove('is-loading');
            button.removeAttribute('aria-disabled');
        });
    });

    /* ------------------------------------------------- Unsaved-change guard */

    document.querySelectorAll('form[data-dirty-guard]').forEach(function (form) {
        var indicator = form.querySelector('[data-dirty-indicator]');
        var snapshot = function () { return new URLSearchParams(new FormData(form)).toString(); };
        var initial = snapshot();
        var dirty = false;

        form.addEventListener('input', function () {
            dirty = snapshot() !== initial;
            if (indicator) indicator.hidden = !dirty;
        });
        form.addEventListener('change', function () {
            dirty = snapshot() !== initial;
            if (indicator) indicator.hidden = !dirty;
        });

        window.addEventListener('beforeunload', function (event) {
            if (dirty && form.dataset.submitting !== 'true') {
                event.preventDefault();
                event.returnValue = '';
            }
        });
    });

    /* ----------------------------------------------------- Character counts */

    document.querySelectorAll('[data-counter]').forEach(function (control) {
        var output = document.getElementById(control.getAttribute('data-counter'));
        if (!output) return;
        var max = parseInt(control.getAttribute('maxlength'), 10);
        var update = function () {
            var count = control.value.trim().length;
            output.textContent = max
                ? count.toLocaleString() + ' / ' + max.toLocaleString()
                : count + (count === 1 ? ' character' : ' characters');
            output.classList.toggle('char-count-near', !!max && count > max * 0.9);
        };
        control.addEventListener('input', update);
        update();
    });

    /* --------------------------------------------------------------- Login */

    document.querySelectorAll('[data-password-toggle]').forEach(function (btn) {
        var input = document.getElementById(btn.getAttribute('aria-controls'));
        if (!input) return;
        btn.addEventListener('click', function () {
            var show = input.type === 'password';
            input.type = show ? 'text' : 'password';
            btn.setAttribute('aria-pressed', String(show));
            btn.setAttribute('aria-label', show ? 'Hide password' : 'Show password');
            btn.innerHTML = icon(show ? 'eye-off' : 'eye');
            input.focus();
        });
    });

    document.querySelectorAll('[data-demo-user]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var user = document.getElementById('username');
            var pass = document.getElementById('password');
            if (!user || !pass) return;
            user.value = btn.getAttribute('data-demo-user');
            pass.value = btn.getAttribute('data-demo-pass');
            [user, pass].forEach(clearError);
            var submit = document.querySelector('#login-form [data-submit]');
            if (submit) submit.focus();
        });
    });

    /* ---------------------------------------------------------- Highlight */

    function escapeRegExp(text) {
        return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    }

    function highlight(scope, keyword) {
        if (!scope || !keyword) return;
        var pattern = new RegExp('(' + escapeRegExp(keyword) + ')', 'gi');
        scope.querySelectorAll('[data-highlight]').forEach(function (el) {
            var walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT);
            var nodes = [];
            while (walker.nextNode()) nodes.push(walker.currentNode);
            nodes.forEach(function (node) {
                if (!pattern.test(node.nodeValue)) return;
                pattern.lastIndex = 0;
                var frag = document.createDocumentFragment();
                node.nodeValue.split(pattern).forEach(function (part, i) {
                    if (!part) return;
                    if (i % 2 === 1) {
                        var mark = document.createElement('mark');
                        mark.textContent = part;
                        frag.appendChild(mark);
                    } else {
                        frag.appendChild(document.createTextNode(part));
                    }
                });
                node.parentNode.replaceChild(frag, node);
            });
        });
    }

    /* -------------------------------------------------------- Live search */

    var liveForm = document.querySelector('form[data-live-search]');
    var region = document.getElementById('results-region');

    if (region) highlight(region, region.getAttribute('data-keyword'));

    if (liveForm && region && window.fetch && window.DOMParser) {
        var input = liveForm.querySelector('input[name="keyword"]');
        var busy = liveForm.querySelector('.search-busy');
        var sortSelect = liveForm.querySelector('select[name="sort"]');
        var timer = null;
        var controller = null;
        var lastQuery = new URLSearchParams(new FormData(liveForm)).toString();

        var run = function () {
            var params = new URLSearchParams(new FormData(liveForm));
            Array.from(params.keys()).forEach(function (key) {
                if (!params.get(key)) params.delete(key);
            });
            var query = params.toString();
            if (query === lastQuery) return;
            lastQuery = query;

            var url = liveForm.getAttribute('action') + (query ? '?' + query : '');
            if (controller) controller.abort();
            controller = new AbortController();
            region.setAttribute('aria-busy', 'true');
            if (busy) busy.hidden = false;

            fetch(url, { signal: controller.signal, credentials: 'same-origin', headers: { 'X-Requested-With': 'fetch' } })
                .then(function (response) {
                    if (!response.ok) throw new Error('HTTP ' + response.status);
                    return response.text();
                })
                .then(function (html) {
                    var doc = new DOMParser().parseFromString(html, 'text/html');
                    var fresh = doc.getElementById('results-region');
                    if (!fresh) {
                        // Session expired or unexpected page: let the server decide.
                        window.location.href = url;
                        return;
                    }
                    region.innerHTML = fresh.innerHTML;
                    region.setAttribute('data-keyword', fresh.getAttribute('data-keyword') || '');
                    ['filter-chips', 'view-toggle'].forEach(function (id) {
                        var current = document.getElementById(id);
                        var next = doc.getElementById(id);
                        if (current && next) current.innerHTML = next.innerHTML;
                    });
                    highlight(region, region.getAttribute('data-keyword'));
                    history.replaceState(null, '', url);
                })
                .catch(function (error) {
                    if (error.name === 'AbortError') return;
                    lastQuery = null;
                    showToast('Search could not be updated. Press Enter to try again.', 'error');
                })
                .finally(function () {
                    region.removeAttribute('aria-busy');
                    if (busy) busy.hidden = true;
                });
        };

        input.addEventListener('input', function () {
            clearTimeout(timer);
            timer = setTimeout(run, 350);
        });

        if (sortSelect) {
            sortSelect.addEventListener('change', function () {
                clearTimeout(timer);
                run();
            });
        }
    }

    /* ------------------------------------------------------- Article tools */

    document.querySelectorAll('[data-action="copy-link"]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var url = window.location.href.split('#')[0];
            if (!navigator.clipboard) {
                showToast('Copying is not supported in this browser.', 'warning');
                return;
            }
            navigator.clipboard.writeText(url).then(function () {
                showToast('Link copied to clipboard.', 'success');
            }, function () {
                showToast('Could not copy the link.', 'error');
            });
        });
    });

    document.querySelectorAll('[data-action="print"]').forEach(function (btn) {
        btn.addEventListener('click', function () { window.print(); });
    });
})();
