/**
 * SIT Campus App - Student dashboard: report an issue, campus feed, my issues.
 * Depends on: shared/toast.js, shared/api.js, shared/ui.js
 */
document.addEventListener('DOMContentLoaded', () => {
    'use strict';

    // Auth guard
    const token = localStorage.getItem('jwt_token');
    if (!token || localStorage.getItem('user_role') !== 'STUDENT') {
        window.location.href = '../auth/login.html';
        return;
    }

    const $ = (id) => document.getElementById(id);
    const esc = UI.esc;

    const CATEGORIES = [
        { value: 'ELECTRICAL', label: 'Electrical', icon: 'bolt' },
        { value: 'PLUMBING', label: 'Plumbing', icon: 'droplet' },
        { value: 'CIVIL', label: 'Civil / structural', icon: 'building' },
        { value: 'CLEANLINESS', label: 'Cleanliness', icon: 'sparkle' },
        { value: 'IT', label: 'IT / network', icon: 'wifi' },
        { value: 'FURNITURE', label: 'Furniture', icon: 'chair' },
        { value: 'OTHER', label: 'Other', icon: 'dots' },
    ];
    const PRIORITIES = [
        { value: 'LOW', label: 'Low' },
        { value: 'MEDIUM', label: 'Medium' },
        { value: 'HIGH', label: 'High' },
    ];
    // category the server stores (and returns) for each value the form sends
    const STORED_CATEGORY = { ELECTRICAL: 'ELECTRICAL', PLUMBING: 'PLUMBING', CIVIL: 'CIVIL', CLEANLINESS: 'CLEANING', IT: 'IT', FURNITURE: 'FURNITURE', OTHER: 'GENERAL' };

    let feedData = [];
    let mineData = [];

    /* ───────────── navigation between the three views ───────────── */

    const VIEWS = ['report', 'feed', 'issues'];
    function showView() {
        const name = VIEWS.includes(location.hash.slice(1)) ? location.hash.slice(1) : 'report';
        VIEWS.forEach(v => { $('view-' + v).hidden = v !== name; });
        UI.setActive(name);
        document.title = { report: 'Report an issue', feed: 'Campus feed', issues: 'My issues' }[name] + ' · SIT Campus App';
        if (name === 'feed') loadFeed();
        if (name === 'issues') loadMine();
        if (name === 'report') loadSimilarPool();
        window.scrollTo(0, 0);
    }
    window.addEventListener('hashchange', showView);

    /* ───────────── report form ───────────── */

    $('categoryGrid').innerHTML = CATEGORIES.map(c => `
        <div class="choice"><input type="radio" name="category" id="cat-${c.value}" value="${c.value}">
        <label for="cat-${c.value}">${UI.icon(c.icon)}<span>${esc(c.label)}</span></label></div>`).join('');
    $('priorityGroup').innerHTML = PRIORITIES.map(p => `
        <div class="choice"><input type="radio" name="priority" id="pri-${p.value}" value="${p.value}" ${p.value === 'MEDIUM' ? 'checked' : ''}>
        <label for="pri-${p.value}">${esc(p.label)}</label></div>`).join('');

    const uploader = UI.uploader($('photoUploader'), { required: true });
    const form = $('reportForm');

    $('description').addEventListener('input', () => {
        $('descCount').textContent = `${$('description').value.length} / 500`;
        $('descErr').textContent = '';
    });
    $('location').addEventListener('input', () => { $('locationErr').textContent = ''; showSimilar(); });
    form.querySelectorAll('input[name=category]').forEach(r => r.addEventListener('change', () => { $('categoryErr').textContent = ''; showSimilar(); }));
    form.addEventListener('reset', () => {
        uploader.reset();
        $('descCount').textContent = '0 / 500';
        ['categoryErr', 'locationErr', 'descErr'].forEach(id => { $(id).textContent = ''; });
        $('submitErr').hidden = true;
        setTimeout(showSimilar, 0);
    });

    function validate() {
        let ok = true;
        const category = form.querySelector('input[name=category]:checked');
        const location_ = $('location').value.trim();
        const description = $('description').value.trim();
        if (!category) { $('categoryErr').textContent = 'Choose a category.'; ok = false; }
        if (!location_) { $('locationErr').textContent = 'Say where the problem is.'; ok = false; }
        if (description.length < 20) { $('descErr').textContent = `Add a little more detail (${description.length} of at least 20 characters).`; ok = false; }
        if (!uploader.hasFile()) { uploader.setError('A photo is required.'); ok = false; }
        return ok;
    }

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        $('submitErr').hidden = true;
        if (!validate()) {
            const firstBad = document.querySelector('#reportForm .error-text:not(:empty)');
            if (firstBad) firstBad.scrollIntoView({ block: 'center', behavior: 'smooth' });
            return;
        }

        const btn = $('submitBtn');
        btn.disabled = true;
        btn.textContent = 'Submitting…';
        try {
            const complaint = {
                location: $('location').value.trim(),
                description: $('description').value.trim(),
                category: form.querySelector('input[name=category]:checked').value,
                priority: form.querySelector('input[name=priority]:checked').value,
            };
            const body = new FormData();
            body.append('complaint', new Blob([JSON.stringify(complaint)], { type: 'application/json' }));
            body.append('image', uploader.getFile());

            const response = await fetch(`${API_BASE_URL}/student/report`, {
                method: 'POST', headers: { Authorization: `Bearer ${token}` }, body,
            });
            if (!response.ok) throw new Error(await readErrorMessage(response, 'Could not submit the report.'));
            const created = await response.json();

            $('successId').textContent = `#${created.id}`;
            $('successDept').textContent = created.departmentName || 'the right department';
            form.hidden = true;
            $('successPanel').hidden = false;
            $('similarCard').hidden = true;
            feedData = []; mineData = [];
            window.scrollTo({ top: 0, behavior: 'smooth' });
        } catch (err) {
            $('submitErr').textContent = err.message;
            $('submitErr').hidden = false;
        } finally {
            btn.disabled = false;
            btn.textContent = 'Submit report';
        }
    });

    $('reportAnother').addEventListener('click', () => {
        form.reset();
        form.hidden = false;
        $('successPanel').hidden = true;
        loadSimilarPool();
        $('location').focus();
    });

    /* ───────────── similar open issues (while reporting) ───────────── */

    async function loadSimilarPool() {
        if (!feedData.length) {
            try { feedData = await api.get('/student/all-reports'); } catch (e) { return; }
        }
        showSimilar();
    }

    function showSimilar() {
        const picked = form.querySelector('input[name=category]:checked');
        const text = $('location').value.trim().toLowerCase();
        if (!picked && !text) { $('similarCard').hidden = true; return; }
        const category = picked ? STORED_CATEGORY[picked.value] : null;
        const matches = feedData.filter(i => i.status !== 'RESOLVED' && i.status !== 'CLOSED')
            .filter(i => (!category || (i.category || '').toUpperCase() === category) && (!text || (i.location || '').toLowerCase().includes(text)))
            .slice(0, 3);
        $('similarCard').hidden = matches.length === 0;
        $('similarList').innerHTML = matches.map(i => issueCard(i, { compact: true })).join('');
    }

    /* ───────────── issue card (feed and similar issues) ───────────── */

    const upvoted = (id) => !!localStorage.getItem(`upvoted_${id}`);

    function issueCard(i, { compact = false } = {}) {
        const photo = UI.thumb(i.imageUrl, `Photo of the issue at ${i.location}`);
        return `<article class="issue" data-id="${Number(i.id)}">
            <div>
                <div class="issue-top">${UI.statusBadge(i.status)}<span class="cat">${esc(i.category)}</span></div>
                <div class="issue-loc">${UI.icon('pin', 'icon-sm')}<span>${esc(i.location)}</span></div>
                <p class="issue-desc ${compact ? 'clamp-2' : 'clamp-3'}">${esc(i.description)}</p>
                <div class="issue-meta">
                    <span>${UI.icon('user', 'icon-sm')}${esc(i.studentName || 'A student')}</span>
                    <span>${UI.icon('clock', 'icon-sm')}${esc(UI.relTime(i.createdAt))}</span>
                    ${i.departmentName ? `<span>${UI.icon('building', 'icon-sm')}${esc(i.departmentName)}</span>` : ''}
                </div>
            </div>
            <div class="issue-side">
                ${compact ? '' : photo}
                <button type="button" class="upvote ${upvoted(i.id) ? 'is-active' : ''}" data-upvote="${Number(i.id)}" aria-label="Upvote this issue, ${Number(i.upvoteCount) || 0} so far">
                    ${UI.icon('arrowUp', 'icon-sm')}<span class="count">${Number(i.upvoteCount) || 0}</span>
                </button>
            </div>
        </article>`;
    }

    // one delegated handler for every upvote button on the page
    document.addEventListener('click', async (e) => {
        const btn = e.target.closest('[data-upvote]');
        if (!btn || btn.disabled) return;
        const id = btn.dataset.upvote;
        if (upvoted(id)) { showToast('You have already upvoted this issue.', 'error'); return; }
        btn.disabled = true;
        try {
            const updated = await api.post(`/student/upvote/${id}`);
            document.querySelectorAll(`[data-upvote="${CSS.escape(id)}"]`).forEach(b => {
                b.classList.add('is-active');
                b.querySelector('.count').textContent = updated.upvoteCount;
            });
            const cached = feedData.find(x => String(x.id) === id);
            if (cached) cached.upvoteCount = updated.upvoteCount;
            localStorage.setItem(`upvoted_${id}`, 'true');
            showToast('Upvoted. Thanks for flagging it.', 'success');
        } catch (err) {
            if (/already upvoted/i.test(err.message || '')) {
                localStorage.setItem(`upvoted_${id}`, 'true');
                btn.classList.add('is-active');
            }
            showToast(err.message || 'Upvote failed.', 'error');
        } finally {
            btn.disabled = false;
        }
    });

    /* ───────────── campus feed ───────────── */

    async function loadFeed() {
        const list = $('feedList');
        if (!feedData.length) {
            list.innerHTML = UI.skeleton(3);
            try {
                feedData = await api.get('/student/all-reports');
            } catch (err) {
                list.innerHTML = UI.empty({ icon: 'alert', title: 'Could not load the feed', text: 'Check your connection and try again.' });
                return;
            }
        }
        renderFeed();
    }

    function renderFeed() {
        const status = $('feedStatus').value;
        const category = $('feedCategory').value;
        const query = $('feedSearch').value.trim().toLowerCase();
        const sort = $('feedSort').value;

        let items = feedData.filter(i => {
            const st = i.status === 'ASSIGNED' ? 'PENDING' : i.status;
            if (status === 'OPEN' && (st === 'RESOLVED' || st === 'CLOSED')) return false;
            if (status === 'NEW' && st !== 'PENDING') return false;
            if (status === 'IN_PROGRESS' && st !== 'IN_PROGRESS') return false;
            if (status === 'RESOLVED' && st !== 'RESOLVED') return false;
            if (category !== 'ALL' && (i.category || '').toUpperCase() !== category) return false;
            if (query && !((i.location || '') + ' ' + (i.description || '')).toLowerCase().includes(query)) return false;
            return true;
        });
        items = [...items].sort((a, b) => sort === 'UPVOTES' ? (b.upvoteCount || 0) - (a.upvoteCount || 0)
            : sort === 'DATE_ASC' ? new Date(a.createdAt) - new Date(b.createdAt) : new Date(b.createdAt) - new Date(a.createdAt));

        $('feedList').innerHTML = items.length
            ? items.map(i => issueCard(i)).join('')
            : UI.empty({
                icon: 'search', title: 'No issues match',
                text: feedData.length ? 'Try a different filter or search.' : 'Nothing has been reported yet.',
                action: '<a class="btn btn-primary" href="#report">Report an issue</a>',
            });
    }

    ['feedStatus', 'feedCategory', 'feedSort'].forEach(id => $(id).addEventListener('change', renderFeed));
    $('feedSearch').addEventListener('input', renderFeed);

    /* ───────────── my issues ───────────── */

    async function loadMine() {
        const list = $('mineList');
        list.innerHTML = UI.skeleton(2);
        try {
            mineData = await api.get('/student/my-reports');
        } catch (err) {
            list.innerHTML = UI.empty({ icon: 'alert', title: 'Could not load your issues', text: 'Check your connection and try again.' });
            return;
        }
        renderMine();
    }

    function renderMine() {
        const norm = (s) => (s === 'ASSIGNED' ? 'PENDING' : s);
        const count = (st) => mineData.filter(i => norm(i.status) === st).length;
        $('mineKpis').innerHTML = [
            ['Total reported', mineData.length, ''],
            ['New', count('PENDING'), 'var(--st-new-fg)'],
            ['In progress', count('IN_PROGRESS'), '#b7791f'],
            ['Resolved', count('RESOLVED'), 'var(--st-resolved-fg)'],
        ].map(([label, n, color]) => `<div class="kpi"><div class="kpi-value">${n}</div>
            <div class="kpi-label">${color ? `<span class="kpi-dot" style="background:${color}"></span>` : ''}${esc(label)}</div></div>`).join('');

        const filter = $('mineStatus').value;
        const items = mineData.filter(i => filter === 'ALL' || (filter === 'NEW' ? norm(i.status) === 'PENDING' : i.status === filter));
        if (!items.length) {
            $('mineList').innerHTML = mineData.length
                ? UI.empty({ icon: 'search', title: 'No issues with that status', text: 'Pick another status above.' })
                : UI.empty({ icon: 'file', title: 'You have not reported anything yet', text: 'When you report a problem it will show up here with its progress.', action: '<a class="btn btn-primary" href="#report">Report an issue</a>' });
            return;
        }
        $('mineList').innerHTML = items.map((i, idx) => `<article class="issue" style="grid-template-columns:1fr" data-id="${Number(i.id)}">
            <div>
                <div class="issue-top">${UI.statusBadge(i.status)}<span class="cat">${esc(i.category)}</span><span class="pill">#${Number(i.id)}</span>${UI.priorityPill(i.priority)}</div>
                <div class="issue-loc">${UI.icon('pin', 'icon-sm')}<span>${esc(i.location)}</span></div>
                <p class="issue-desc clamp-2">${esc(i.description)}</p>
                <div class="issue-meta"><span>${UI.icon('clock', 'icon-sm')}Reported ${esc(UI.fmtDate(i.createdAt, false))}</span>
                    ${i.departmentName ? `<span>${UI.icon('building', 'icon-sm')}${esc(i.departmentName)}</span>` : ''}
                    <span>${UI.icon('arrowUp', 'icon-sm')}${Number(i.upvoteCount) || 0} upvote${i.upvoteCount === 1 ? '' : 's'}</span></div>
                <button type="button" class="btn btn-secondary btn-sm" data-toggle="${Number(i.id)}" aria-expanded="${idx === 0}" style="margin-top:12px">
                    ${UI.icon('chevronRight', 'icon-sm')}<span>${idx === 0 ? 'Hide progress' : 'Show progress'}</span></button>
                <div class="expand" id="expand-${Number(i.id)}" ${idx === 0 ? '' : 'hidden'}>
                    ${UI.timeline(i)}
                    ${(i.imageUrl || i.resolvedImageUrl) ? `<div class="section-title">Photos</div><div class="photo-row">
                        ${i.imageUrl ? `<figure>${UI.thumb(i.imageUrl, 'Your photo')}<figcaption>Your photo</figcaption></figure>` : ''}
                        ${i.resolvedImageUrl ? `<figure>${UI.thumb(i.resolvedImageUrl, 'Photo from the department')}<figcaption>From the department</figcaption></figure>` : ''}</div>` : ''}
                </div>
            </div>
        </article>`).join('');
    }

    $('mineStatus').addEventListener('change', renderMine);
    $('mineList').addEventListener('click', (e) => {
        const btn = e.target.closest('[data-toggle]');
        if (!btn) return;
        const panel = $('expand-' + btn.dataset.toggle);
        const open = panel.hidden;
        panel.hidden = !open;
        btn.setAttribute('aria-expanded', String(open));
        btn.querySelector('span').textContent = open ? 'Hide progress' : 'Show progress';
    });

    showView();
});
