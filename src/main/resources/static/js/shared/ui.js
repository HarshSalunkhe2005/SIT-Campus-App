/**
 * SIT Campus App: shared UI helpers (icons, app shell, status badges, dialogs, uploader).
 * Load after toast.js and api.js. Pages opt in to the shell with <body data-role="student|dept|admin" data-active="...">.
 */
const UI = (() => {
    'use strict';

    // ---------- icons (24x24 line icons, drawn for this app) ----------
    const ICONS = {
        home: '<path d="M3 11l9-8 9 8"/><path d="M5 10v10h14V10"/><path d="M10 20v-6h4v6"/>',
        plus: '<path d="M12 5v14M5 12h14"/>',
        list: '<path d="M8 6h13M8 12h13M8 18h13"/><path d="M3 6h.01M3 12h.01M3 18h.01"/>',
        file: '<path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z"/><path d="M14 3v5h5"/><path d="M9 13h6M9 17h6"/>',
        users: '<circle cx="9" cy="8" r="3.5"/><path d="M2.5 20c0-3.5 3-6 6.5-6s6.5 2.5 6.5 6"/><circle cx="17.5" cy="9" r="2.5"/><path d="M17 14c2.8 0 5 2 5 5"/>',
        building: '<rect x="4" y="3" width="16" height="18" rx="1"/><path d="M9 7h2M13 7h2M9 11h2M13 11h2"/><path d="M9 21v-4h6v4"/>',
        chart: '<path d="M4 20V10M10 20V4M16 20v-7M22 20H2"/>',
        logout: '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><path d="M16 17l5-5-5-5M21 12H9"/>',
        search: '<circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/>',
        check: '<path d="M5 12l5 5 9-10"/>',
        checkCircle: '<circle cx="12" cy="12" r="9"/><path d="M8 12.5l3 3 5-6"/>',
        clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
        progress: '<path d="M21 12a9 9 0 1 1-3-6.7"/><path d="M21 4v5h-5"/>',
        inbox: '<path d="M3 13h5l1 3h6l1-3h5"/><path d="M5 5h14l2 8v6H3v-6z"/>',
        lock: '<rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V8a4 4 0 0 1 8 0v3"/>',
        x: '<path d="M6 6l12 12M18 6L6 18"/>',
        chevronRight: '<path d="M9 6l6 6-6 6"/>',
        pin: '<path d="M12 21s7-6.2 7-11a7 7 0 0 0-14 0c0 4.8 7 11 7 11z"/><circle cx="12" cy="10" r="2.5"/>',
        image: '<rect x="3" y="4" width="18" height="16" rx="2"/><circle cx="9" cy="10" r="1.5"/><path d="M21 16l-5-5-8 8"/>',
        upload: '<path d="M12 16V4M7 9l5-5 5 5"/><path d="M4 16v3a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-3"/>',
        arrowUp: '<path d="M12 19V5M6 11l6-6 6 6"/>',
        trash: '<path d="M4 7h16M9 7V4h6v3"/><path d="M6 7l1 13h10l1-13M10 11v6M14 11v6"/>',
        edit: '<path d="M4 20h4L19 9l-4-4L4 16z"/><path d="M13.5 6.5l4 4"/>',
        mail: '<rect x="3" y="5" width="18" height="14" rx="2"/><path d="M3 7l9 6 9-6"/>',
        key: '<circle cx="8" cy="15" r="4"/><path d="M11 12l9-9M16 7l3 3M14 9l2 2"/>',
        user: '<circle cx="12" cy="8" r="4"/><path d="M4 21c0-4.4 3.6-7 8-7s8 2.6 8 7"/>',
        alert: '<path d="M12 3L2 21h20z"/><path d="M12 10v5M12 18h.01"/>',
        info: '<circle cx="12" cy="12" r="9"/><path d="M12 11v6M12 7.5h.01"/>',
        board: '<rect x="3" y="4" width="5" height="16" rx="1"/><rect x="10" y="4" width="5" height="10" rx="1"/><rect x="17" y="4" width="4" height="13" rx="1"/>',
        copy: '<rect x="9" y="9" width="11" height="11" rx="2"/><path d="M5 15V6a2 2 0 0 1 2-2h9"/>',
        shield: '<path d="M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z"/>',
        bolt: '<path d="M13 3L5 14h6l-1 7 8-11h-6z"/>',
        eye: '<path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/>',
        droplet: '<path d="M12 3s6 6.5 6 11a6 6 0 0 1-12 0c0-4.5 6-11 6-11z"/>',
        sparkle: '<path d="M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8z"/><path d="M19 16l.8 2.2L22 19l-2.2.8L19 22l-.8-2.2L16 19l2.2-.8z"/>',
        wifi: '<path d="M2 9a15 15 0 0 1 20 0M5.5 12.5a10 10 0 0 1 13 0M9 16a5 5 0 0 1 6 0M12 19.5h.01"/>',
        chair: '<path d="M7 11V5a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v6"/><path d="M5 11h14v4H5zM7 15v6M17 15v6"/>',
        dots: '<circle cx="6" cy="12" r="1.2"/><circle cx="12" cy="12" r="1.2"/><circle cx="18" cy="12" r="1.2"/>',
        camera: '<path d="M4 8h3l2-3h6l2 3h3a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1z"/><circle cx="12" cy="13" r="3.5"/>',
    };

    function icon(name, cls = '') {
        return `<svg class="icon ${cls}" viewBox="0 0 24 24" aria-hidden="true" focusable="false">${ICONS[name] || ''}</svg>`;
    }

    // ---------- formatting ----------
    const esc = (v) => escapeHtml(v);

    const STATUS = {
        PENDING: { label: 'New', cls: 'st-new', icon: 'inbox' },
        ASSIGNED: { label: 'New', cls: 'st-new', icon: 'inbox' },
        IN_PROGRESS: { label: 'In progress', cls: 'st-progress', icon: 'progress' },
        RESOLVED: { label: 'Resolved', cls: 'st-resolved', icon: 'checkCircle' },
        CLOSED: { label: 'Closed', cls: 'st-closed', icon: 'lock' },
    };

    function statusMeta(status) {
        return STATUS[status] || { label: String(status || 'Unknown'), cls: 'st-closed', icon: 'info' };
    }

    function statusBadge(status) {
        const m = statusMeta(status);
        return `<span class="badge ${m.cls}">${icon(m.icon)}${esc(m.label)}</span>`;
    }

    function priorityPill(priority) {
        const p = String(priority || 'MEDIUM').toUpperCase();
        const label = p.charAt(0) + p.slice(1).toLowerCase();
        return `<span class="pill ${p === 'HIGH' ? 'pri-high' : ''}">${esc(label)} priority</span>`;
    }

    function relTime(iso) {
        if (!iso) return '';
        const secs = Math.round((Date.now() - new Date(iso).getTime()) / 1000);
        if (secs < 60) return 'Just now';
        const mins = Math.round(secs / 60);
        if (mins < 60) return `${mins} min ago`;
        const hrs = Math.round(mins / 60);
        if (hrs < 24) return `${hrs} h ago`;
        const days = Math.round(hrs / 24);
        if (days < 30) return `${days} day${days === 1 ? '' : 's'} ago`;
        return fmtDate(iso, false);
    }

    function fmtDate(iso, withTime = true) {
        if (!iso) return '';
        const d = new Date(iso);
        const date = d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
        return withTime ? `${date}, ${d.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })}` : date;
    }

    function initials(name) {
        const parts = String(name || '?').trim().split(/\s+/);
        return ((parts[0] || '?')[0] + (parts.length > 1 ? parts[parts.length - 1][0] : '')).toUpperCase();
    }

    /** Progress steps for an issue, from the history the API returns. */
    function timeline(issue) {
        const first = {};
        (issue.history || []).forEach(e => {
            const key = e.status === 'PENDING' ? 'ASSIGNED' : e.status;
            if (!first[key]) first[key] = e.at;
        });
        const closed = !!first.CLOSED;
        const steps = [
            { title: 'Reported', at: issue.createdAt, done: true },
            { title: issue.departmentName ? `Assigned to ${issue.departmentName}` : 'Assigned to a department', at: first.ASSIGNED || issue.createdAt, done: true },
            { title: 'In progress', at: first.IN_PROGRESS, done: !!first.IN_PROGRESS || !!first.RESOLVED },
            { title: 'Resolved', at: first.RESOLVED, done: !!first.RESOLVED },
        ];
        if (closed) steps.push({ title: 'Closed', at: first.CLOSED, done: true });
        const lastDone = steps.map(s => s.done).lastIndexOf(true);
        return `<ol class="steps">${steps.map((s, i) => {
            const state = !s.done ? 'is-todo' : (i === lastDone && !closed && i < 3 ? 'is-current' : 'is-done');
            return `<li class="step ${state}">
                <span class="step-dot">${s.done ? icon('check') : ''}</span>
                <div><div class="step-title">${esc(s.title)}</div>${s.at && s.done ? `<div class="step-time">${esc(fmtDate(s.at))}</div>` : (s.done ? '' : '<div class="step-time">Not yet</div>')}</div>
            </li>`;
        }).join('')}</ol>`;
    }

    function empty({ icon: ic = 'inbox', title, text = '', action = '' }) {
        return `<div class="empty">${icon(ic, 'icon-lg')}<h3>${esc(title)}</h3>${text ? `<p>${esc(text)}</p>` : ''}${action}</div>`;
    }

    function skeleton(n = 3) {
        return Array.from({ length: n }, () => '<div class="skeleton"></div>').join('');
    }

    // ---------- dialogs ----------
    function openDialog(className, html) {
        const dlg = document.createElement('dialog');
        dlg.className = className;
        dlg.innerHTML = html;
        document.body.appendChild(dlg);
        dlg.addEventListener('close', () => dlg.remove());
        // a click on the dim backdrop (outside the dialog's own box) closes it
        dlg.addEventListener('click', (e) => {
            if (e.target !== dlg) return;
            const r = dlg.getBoundingClientRect();
            if (e.clientX < r.left || e.clientX > r.right || e.clientY < r.top || e.clientY > r.bottom) dlg.close();
        });
        dlg.showModal();
        return dlg;
    }

    function modal({ title, bodyHtml = '', footHtml = '' }) {
        const dlg = openDialog('', `<div class="modal" role="document">
            <div class="modal-head"><h2>${esc(title)}</h2>
              <button type="button" class="btn btn-ghost btn-icon btn-sm" data-close aria-label="Close">${icon('x')}</button></div>
            <div class="modal-body">${bodyHtml}</div>
            <div class="modal-foot">${footHtml}</div></div>`);
        dlg.querySelectorAll('[data-close]').forEach(b => b.addEventListener('click', () => dlg.close()));
        return dlg;
    }

    function confirm({ title, message, confirmText = 'Confirm', danger = false }) {
        return new Promise(resolve => {
            const dlg = modal({
                title,
                bodyHtml: `<p>${esc(message)}</p>`,
                footHtml: `<button type="button" class="btn btn-secondary" data-close>Cancel</button>
                           <button type="button" class="btn ${danger ? 'btn-primary' : 'btn-primary'}" data-ok>${esc(confirmText)}</button>`,
            });
            let result = false;
            dlg.querySelector('[data-ok]').addEventListener('click', () => { result = true; dlg.close(); });
            dlg.addEventListener('close', () => resolve(result));
        });
    }

    /** Shows a value that will not be shown again (a generated password) with a copy button. */
    function showSecret({ title, message, secret }) {
        const dlg = modal({
            title,
            bodyHtml: `<p>${esc(message)}</p><div class="secret-box"><span id="secretValue">${esc(secret)}</span>
                       <button type="button" class="btn btn-secondary btn-sm" id="copySecret">${icon('copy', 'icon-sm')} Copy</button></div>`,
            footHtml: '<button type="button" class="btn btn-primary" data-close>Done</button>',
        });
        dlg.querySelector('#copySecret').addEventListener('click', async () => {
            try { await navigator.clipboard.writeText(secret); showToast('Copied', 'success'); } catch (e) { showToast('Select and copy it manually', 'error'); }
        });
        return dlg;
    }

    function drawer({ title, subtitleHtml = '', bodyHtml = '', footHtml = '' }) {
        const dlg = openDialog('drawer', `<div class="drawer-head"><div><h2>${esc(title)}</h2>${subtitleHtml}</div>
              <button type="button" class="btn btn-ghost btn-icon btn-sm" data-close aria-label="Close">${icon('x')}</button></div>
            <div class="drawer-body">${bodyHtml}</div>
            ${footHtml ? `<div class="drawer-foot">${footHtml}</div>` : ''}`);
        dlg.querySelectorAll('[data-close]').forEach(b => b.addEventListener('click', () => dlg.close()));
        return dlg;
    }

    /** Full details of an issue in a side panel: description, people, photos and the progress timeline. */
    function issueDrawer(i, { showEmail = false, footHtml = '' } = {}) {
        const photos = (i.imageUrl || i.resolvedImageUrl) ? `<div class="section-title">Photos</div><div class="photo-row">
            ${i.imageUrl ? `<figure>${thumb(i.imageUrl, 'Reported photo')}<figcaption>Reported photo</figcaption></figure>` : ''}
            ${i.resolvedImageUrl ? `<figure>${thumb(i.resolvedImageUrl, 'Proof photo')}<figcaption>Proof of fix</figcaption></figure>` : ''}</div>` : '';
        return drawer({
            title: `#${Number(i.id)} ${i.location}`,
            subtitleHtml: `<div class="issue-top" style="margin:8px 0 0">${statusBadge(i.status)}${priorityPill(i.priority)}</div>`,
            bodyHtml: `<p>${esc(i.description)}</p>
                <div class="section-title">Details</div>
                <dl class="kv">
                    <dt>Category</dt><dd>${esc(i.category)}</dd>
                    <dt>Department</dt><dd>${esc(i.departmentName || 'Not assigned')}</dd>
                    <dt>Reported by</dt><dd>${esc(i.studentName || 'A student')}${showEmail && i.studentEmail ? `<br><span class="cell-sub">${esc(i.studentEmail)}</span>` : ''}</dd>
                    <dt>Reported</dt><dd>${esc(fmtDate(i.createdAt))}</dd>
                    <dt>Last update</dt><dd>${esc(fmtDate(i.updatedAt))}</dd>
                    <dt>Upvotes</dt><dd>${Number(i.upvoteCount) || 0}</dd>
                </dl>
                ${photos}
                <div class="section-title">Progress</div>${timeline(i)}`,
            footHtml,
        });
    }

    function lightbox(url, alt = 'Photo') {
        const dlg = openDialog('lightbox', `<img src="${esc(url)}" alt="${esc(alt)}">
            <button type="button" class="btn btn-secondary btn-icon lightbox-close" data-close aria-label="Close photo">${icon('x')}</button>`);
        dlg.querySelector('[data-close]').addEventListener('click', () => dlg.close());
    }

    document.addEventListener('click', (e) => {
        const el = e.target.closest('[data-lightbox]');
        if (el) lightbox(el.getAttribute('data-lightbox'), el.getAttribute('data-alt') || 'Photo');
    });

    /** Thumbnail button that opens the photo full size. */
    function thumb(path, alt) {
        const url = uploadUrl(path);
        return url ? `<button type="button" class="thumb" data-lightbox="${esc(url)}" data-alt="${esc(alt)}" aria-label="View photo: ${esc(alt)}" style="background:url('${esc(url)}') center/cover"></button>` : '';
    }

    // ---------- photo uploader ----------
    function uploader(container, { label = 'Photo', required = true, onChange } = {}) {
        const id = 'up' + Math.random().toString(36).slice(2, 8);
        container.innerHTML = `<div class="dropzone" id="${id}-zone" role="button" tabindex="0" aria-describedby="${id}-err">
              <input type="file" id="${id}" accept="image/jpeg,image/png,image/webp" hidden>
              <div id="${id}-prompt">${icon('upload', 'icon-lg')}<div><strong>Choose a photo</strong> or drag it here</div><div class="hint">JPEG, PNG or WebP, up to 5 MB</div></div>
              <div class="dropzone-preview" id="${id}-preview" hidden>
                <img id="${id}-img" alt="Selected photo preview"><div class="meta"><strong id="${id}-name"></strong><div class="hint" id="${id}-size"></div></div>
                <button type="button" class="btn btn-secondary btn-sm" id="${id}-remove">Remove</button></div></div>
            <div class="error-text" id="${id}-err" role="alert"></div>`;
        const zone = container.querySelector(`#${id}-zone`), input = container.querySelector(`#${id}`);
        const prompt = container.querySelector(`#${id}-prompt`), preview = container.querySelector(`#${id}-preview`);
        const img = container.querySelector(`#${id}-img`), err = container.querySelector(`#${id}-err`);
        let file = null;

        const setError = (m) => { err.textContent = m || ''; zone.classList.toggle('has-error', !!m); };
        function reset() {
            file = null; input.value = ''; img.removeAttribute('src');
            preview.hidden = true; prompt.hidden = false; setError('');
        }
        function pick(f) {
            setError('');
            if (!/^image\/(jpeg|png|webp)$/.test(f.type)) { setError('Only JPEG, PNG or WebP images are allowed.'); return; }
            if (f.size > 5 * 1024 * 1024) { setError('The image must be under 5 MB.'); return; }
            file = f;
            const reader = new FileReader();
            reader.onload = (ev) => { img.src = ev.target.result; };
            reader.readAsDataURL(f);
            container.querySelector(`#${id}-name`).textContent = f.name;
            container.querySelector(`#${id}-size`).textContent = `${(f.size / 1024).toFixed(0)} KB`;
            prompt.hidden = true; preview.hidden = false;
            if (onChange) onChange(file);
        }
        zone.addEventListener('click', (e) => { if (!e.target.closest(`#${id}-remove`)) input.click(); });
        zone.addEventListener('keydown', (e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); input.click(); } });
        zone.addEventListener('dragover', (e) => { e.preventDefault(); zone.classList.add('is-drag'); });
        zone.addEventListener('dragleave', () => zone.classList.remove('is-drag'));
        zone.addEventListener('drop', (e) => { e.preventDefault(); zone.classList.remove('is-drag'); if (e.dataTransfer.files[0]) pick(e.dataTransfer.files[0]); });
        input.addEventListener('change', () => { if (input.files[0]) pick(input.files[0]); });
        container.querySelector(`#${id}-remove`).addEventListener('click', (e) => {
            e.stopPropagation(); reset();
            if (required) setError('A photo is required.');
            if (onChange) onChange(null);
        });
        return { getFile: () => file, hasFile: () => !!file, setError, reset, input };
    }

    // ---------- app shell ----------
    const NAV = {
        student: [
            { id: 'report', label: 'Report an issue', icon: 'plus', href: 'dashboard.html#report' },
            { id: 'feed', label: 'Campus feed', icon: 'list', href: 'dashboard.html#feed' },
            { id: 'issues', label: 'My issues', icon: 'file', href: 'dashboard.html#issues' },
        ],
        dept: [
            { id: 'board', label: 'Issue board', icon: 'board', href: 'kanban.html' },
        ],
        admin: [
            { id: 'overview', label: 'Overview', icon: 'chart', href: 'hub.html' },
            { id: 'users', label: 'Students', icon: 'users', href: 'users.html' },
            { id: 'departments', label: 'Departments', icon: 'building', href: 'departments.html' },
        ],
    };
    const ROLE_LABEL = { student: 'Student', dept: 'Department', admin: 'Administrator' };

    function logout() {
        ['jwt_token', 'user_name', 'user_role', 'user_id'].forEach(k => localStorage.removeItem(k));
        window.location.href = '../auth/login.html';
    }

    function setActive(id) {
        document.querySelectorAll('[data-nav]').forEach(a => {
            if (a.dataset.nav === id) a.setAttribute('aria-current', 'page'); else a.removeAttribute('aria-current');
        });
    }

    function changePasswordDialog() {
        const dlg = modal({
            title: 'Change password',
            bodyHtml: `<form id="pwForm" class="stack" novalidate>
                <div class="field"><label class="label" for="pwCurrent">Current password</label><input class="input" id="pwCurrent" type="password" autocomplete="current-password" required></div>
                <div class="field"><label class="label" for="pwNew">New password</label><input class="input" id="pwNew" type="password" autocomplete="new-password" minlength="8" required><span class="hint">At least 8 characters.</span></div>
                <div class="error-text" id="pwErr" role="alert"></div></form>`,
            footHtml: `<button type="button" class="btn btn-secondary" data-close>Cancel</button>
                       <button type="submit" form="pwForm" class="btn btn-primary" id="pwSave">Change password</button>`,
        });
        dlg.querySelector('#pwForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const btn = dlg.querySelector('#pwSave'); btn.disabled = true;
            try {
                await api.post('/admin/password', { currentPassword: dlg.querySelector('#pwCurrent').value, newPassword: dlg.querySelector('#pwNew').value });
                dlg.close(); showToast('Password changed.', 'success');
            } catch (err) {
                dlg.querySelector('#pwErr').textContent = err.message || 'Could not change the password.'; btn.disabled = false;
            }
        });
    }

    function mountShell() {
        const role = document.body.dataset.role;
        if (!role || !NAV[role]) return;
        const content = document.getElementById('main');
        const name = localStorage.getItem('user_name') || ROLE_LABEL[role];
        const logo = '../../static/images/Symlogo333.jpg';
        const items = NAV[role];
        const navLinks = items.map(i => `<a href="${i.href}" data-nav="${i.id}">${icon(i.icon)}<span>${esc(i.label)}</span></a>`).join('');
        const bottomLinks = items.map(i => `<a href="${i.href}" data-nav="${i.id}">${icon(i.icon)}<span>${esc(i.label.replace('Report an issue', 'Report').replace('Campus feed', 'Feed'))}</span></a>`).join('');

        const app = document.createElement('div');
        app.className = 'app';
        app.innerHTML = `<aside class="sidebar" aria-label="Main navigation">
              <a class="brand" href="${items[0].href}"><img src="${logo}" alt="" width="44" height="44">
                <span class="brand-text"><span class="brand-name">SIT Campus</span><span class="brand-sub">Issue portal</span></span></a>
              <nav class="nav" aria-label="Sections">${navLinks}</nav>
              <div class="sidebar-foot"><span class="avatar" aria-hidden="true">${esc(initials(name))}</span>
                <div class="who"><strong>${esc(name)}</strong><span>${ROLE_LABEL[role]}</span></div>
                ${role === 'admin' ? `<button type="button" class="btn btn-ghost btn-icon btn-sm" id="pwBtn" aria-label="Change password" title="Change password">${icon('key')}</button>` : ''}
                <button type="button" class="btn btn-ghost btn-icon btn-sm" id="logoutBtn" aria-label="Log out" title="Log out">${icon('logout')}</button></div>
            </aside>
            <div class="main">
              <header class="topbar"><a class="brand" href="${items[0].href}"><img src="${logo}" alt="" width="36" height="36"><span class="brand-text"><span class="brand-name">SIT Campus</span></span></a>
                <div style="display:flex;gap:4px">${role === 'admin' ? `<button type="button" class="btn btn-ghost btn-icon btn-sm" id="pwBtn2" aria-label="Change password">${icon('key')}</button>` : ''}
                <button type="button" class="btn btn-ghost btn-icon btn-sm" id="logoutBtn2" aria-label="Log out">${icon('logout')}</button></div></header>
            </div>`;
        const skip = document.createElement('a');
        skip.className = 'skip-link'; skip.href = '#main'; skip.textContent = 'Skip to content';
        document.body.prepend(app);
        document.body.prepend(skip);
        app.querySelector('.main').appendChild(content);
        if (items.length > 1) {
            const bn = document.createElement('nav');
            bn.className = 'bottomnav'; bn.setAttribute('aria-label', 'Sections'); bn.innerHTML = bottomLinks;
            document.body.appendChild(bn);
        }
        const toast = document.createElement('div');
        toast.id = 'sit-toast'; toast.className = 'sit-toast'; toast.setAttribute('role', 'status'); toast.setAttribute('aria-live', 'polite');
        document.body.appendChild(toast);

        ['logoutBtn', 'logoutBtn2'].forEach(id => { const b = document.getElementById(id); if (b) b.addEventListener('click', logout); });
        ['pwBtn', 'pwBtn2'].forEach(id => { const b = document.getElementById(id); if (b) b.addEventListener('click', changePasswordDialog); });
        setActive(document.body.dataset.active || items[0].id);
    }

    document.addEventListener('DOMContentLoaded', mountShell);

    return {
        icon, esc, statusMeta, statusBadge, priorityPill, relTime, fmtDate, initials, timeline, empty, skeleton,
        modal, confirm, showSecret, drawer, issueDrawer, lightbox, thumb, uploader, setActive, logout,
    };
})();
