/**
 * SIT Campus App - Department issue board (board + list views, drag and drop, proof photo).
 * Depends on: shared/toast.js, shared/api.js, shared/ui.js
 */
document.addEventListener('DOMContentLoaded', () => {
    'use strict';

    const role = localStorage.getItem('user_role');
    if (!localStorage.getItem('jwt_token') || role !== 'DEPARTMENT') {
        window.location.href = '../auth/login.html';
        return;
    }

    const $ = (id) => document.getElementById(id);
    const esc = UI.esc;
    const deptName = localStorage.getItem('user_name') || 'Department';
    const deptId = localStorage.getItem('user_id');
    $('boardTitle').textContent = `${deptName}: issue board`;
    document.title = `${deptName} · Issue board`;

    // forward-only workflow: New -> In progress -> Resolved
    const ORDER = { PENDING: 0, IN_PROGRESS: 1, RESOLVED: 2 };
    const norm = (s) => (s === 'ASSIGNED' ? 'PENDING' : s);

    let issues = [];
    let dragId = null;

    /* ───────────── data ───────────── */

    async function load() {
        if (!deptId) { showToast('Department ID missing. Please log in again.', 'error'); return; }
        Object.keys(ORDER).forEach(s => { $('body-' + s).innerHTML = UI.skeleton(1); });
        try {
            issues = await api.get(`/dept/queue/${encodeURIComponent(deptId)}`);
        } catch (err) {
            showToast('Failed to load issues: ' + err.message, 'error');
            issues = [];
        }
        render();
    }

    const visible = () => issues.filter(i => norm(i.status) in ORDER); // closed issues leave the board

    /* ───────────── rendering ───────────── */

    function actionButtons(i) {
        const st = norm(i.status);
        if (st === 'PENDING') {
            return `<button type="button" class="btn btn-secondary btn-sm" data-act="start" data-id="${Number(i.id)}">Start work</button>
                    <button type="button" class="btn btn-primary btn-sm" data-act="resolve" data-id="${Number(i.id)}">Resolve</button>`;
        }
        if (st === 'IN_PROGRESS') {
            return `<button type="button" class="btn btn-primary btn-sm" data-act="resolve" data-id="${Number(i.id)}">Resolve</button>`;
        }
        return '';
    }

    function card(i) {
        const photo = UI.thumb(i.imageUrl, `Reported photo, ${i.location}`);
        const proof = UI.thumb(i.resolvedImageUrl, `Proof photo, ${i.location}`);
        return `<article class="kcard" draggable="${norm(i.status) === 'RESOLVED' ? 'false' : 'true'}" data-id="${Number(i.id)}" data-status="${esc(norm(i.status))}">
            <div class="kcard-top"><span>#${Number(i.id)}</span>${i.priority === 'HIGH' ? '<span class="pill pri-high">High priority</span>' : ''}<span class="grow"></span>
                <span title="Upvotes" style="display:inline-flex;align-items:center;gap:3px">${UI.icon('arrowUp', 'icon-sm')}${Number(i.upvoteCount) || 0}</span></div>
            <div class="cat">${esc(i.category)}</div>
            <div class="kcard-loc">${esc(i.location)}</div>
            <p class="kcard-desc clamp-3">${esc(i.description)}</p>
            ${photo || proof ? `<div class="kcard-photo">${photo}${proof}</div>` : ''}
            <div class="kcard-foot"><span>${esc(i.studentName || 'Student')}</span><span>${esc(UI.relTime(i.createdAt))}</span></div>
            <div class="kcard-actions">
                <button type="button" class="btn btn-ghost btn-sm" data-act="details" data-id="${Number(i.id)}">Details</button>${actionButtons(i)}
            </div>
        </article>`;
    }

    function render() {
        const list = visible();
        Object.keys(ORDER).forEach(s => {
            const items = list.filter(i => norm(i.status) === s);
            $('count-' + s).textContent = items.length;
            $('body-' + s).innerHTML = items.length ? items.map(card).join('')
                : `<div class="col-empty">${s === 'PENDING' ? 'No new issues' : s === 'IN_PROGRESS' ? 'Nothing in progress' : 'Nothing resolved yet'}</div>`;
        });
        const count = (s) => list.filter(i => norm(i.status) === s).length;
        $('kpis').innerHTML = [
            ['Open issues', count('PENDING') + count('IN_PROGRESS'), ''],
            ['New', count('PENDING'), 'var(--st-new-fg)'],
            ['In progress', count('IN_PROGRESS'), '#b7791f'],
            ['Resolved', count('RESOLVED'), 'var(--st-resolved-fg)'],
        ].map(([label, n, color]) => `<div class="kpi"><div class="kpi-value">${n}</div>
            <div class="kpi-label">${color ? `<span class="kpi-dot" style="background:${color}"></span>` : ''}${esc(label)}</div></div>`).join('');
        renderList();
    }

    function renderList() {
        const filter = $('listStatus').value;
        const q = $('listSearch').value.trim().toLowerCase();
        const rows = visible().filter(i => {
            const st = norm(i.status);
            if (filter === 'OPEN' && st === 'RESOLVED') return false;
            if (filter !== 'OPEN' && filter !== 'ALL' && st !== filter) return false;
            return !q || ((i.location || '') + ' ' + (i.description || '')).toLowerCase().includes(q);
        });
        $('listBody').innerHTML = rows.length ? rows.map(i => `<tr class="is-clickable" data-row="${Number(i.id)}">
            <td><div class="cell-main">#${Number(i.id)} ${esc(i.location)}</div><div class="cell-sub truncate">${esc(i.category)}: ${esc(i.description)}</div></td>
            <td>${UI.priorityPill(i.priority)}</td>
            <td><span class="cell-sub">${esc(UI.relTime(i.createdAt))}</span></td>
            <td>${UI.statusBadge(i.status)}</td>
            <td style="white-space:nowrap">${actionButtons(i)}</td></tr>`).join('')
            : `<tr><td colspan="5">${UI.empty({ icon: 'inbox', title: 'No issues here', text: 'Nothing matches this filter.' })}</td></tr>`;
    }

    /* ───────────── status changes ───────────── */

    async function commit(id, status, file) {
        const issue = issues.find(x => Number(x.id) === Number(id));
        try {
            let updated = await api.put('/dept/status', { complaintId: id, status });
            if (file) {
                const form = new FormData();
                form.append('image', file);
                try {
                    updated = await api.postForm(`/dept/${id}/proof`, form);
                } catch (uploadErr) {
                    showToast(`Status saved, but the proof photo failed to upload: ${uploadErr.message}`, 'error');
                }
            }
            if (issue) Object.assign(issue, updated);
            showToast(`Issue #${id} moved to ${UI.statusMeta(status).label.toLowerCase()}`, 'success');
        } catch (err) {
            showToast(`Could not update #${id}: ${err.message}`, 'error');
        }
        render();
    }

    /** Resolving needs a proof photo, as before. */
    function askProof(id) {
        const issue = issues.find(x => Number(x.id) === Number(id));
        const dlg = UI.modal({
            title: 'Mark as resolved',
            bodyHtml: `<p>Attach a photo that shows the problem at ${esc(issue ? issue.location : 'the location')} is fixed. The student can see it.</p>
                       <div id="proofUploader" style="margin-top:14px"></div>`,
            footHtml: `<button type="button" class="btn btn-secondary" data-close>Cancel</button>
                       <button type="button" class="btn btn-primary" id="proofOk" disabled>Confirm resolved</button>`,
        });
        const up = UI.uploader(dlg.querySelector('#proofUploader'), { required: true, onChange: f => { dlg.querySelector('#proofOk').disabled = !f; } });
        dlg.querySelector('#proofOk').addEventListener('click', async () => {
            if (!up.hasFile()) { up.setError('A proof photo is required.'); return; }
            const file = up.getFile();
            dlg.close();
            await commit(id, 'RESOLVED', file);
        });
    }

    function move(id, target) {
        const issue = issues.find(x => Number(x.id) === Number(id));
        if (!issue) return;
        const from = norm(issue.status);
        if (from === target) return;
        if (ORDER[target] < ORDER[from]) {
            showToast('Issues can only move forward: New, then In progress, then Resolved.', 'error');
            return;
        }
        if (target === 'RESOLVED') askProof(id); else commit(id, target, null);
    }

    function showDetails(id) {
        const i = issues.find(x => Number(x.id) === Number(id));
        if (i) UI.issueDrawer(i, { showEmail: false });
    }

    /* ───────────── events ───────────── */

    document.getElementById('main').addEventListener('click', (e) => {
        const btn = e.target.closest('[data-act]');
        if (btn) {
            e.stopPropagation();
            const id = Number(btn.dataset.id);
            if (btn.dataset.act === 'start') move(id, 'IN_PROGRESS');
            else if (btn.dataset.act === 'resolve') move(id, 'RESOLVED');
            else if (btn.dataset.act === 'details') showDetails(id);
            return;
        }
        const row = e.target.closest('[data-row]');
        if (row && !e.target.closest('.thumb')) showDetails(Number(row.dataset.row));
    });

    // drag and drop
    const board = $('board');
    board.addEventListener('dragstart', (e) => {
        const c = e.target.closest('.kcard');
        if (!c) return;
        dragId = Number(c.dataset.id);
        c.classList.add('is-dragging');
        e.dataTransfer.effectAllowed = 'move';
        e.dataTransfer.setData('text/plain', String(dragId));
    });
    board.addEventListener('dragend', () => {
        dragId = null;
        board.querySelectorAll('.is-dragging').forEach(c => c.classList.remove('is-dragging'));
        board.querySelectorAll('.col').forEach(c => c.classList.remove('is-over'));
    });
    board.addEventListener('dragover', (e) => {
        const col = e.target.closest('.col');
        if (!col || dragId === null) return;
        e.preventDefault();
        board.querySelectorAll('.col').forEach(c => c.classList.toggle('is-over', c === col));
    });
    board.addEventListener('drop', (e) => {
        const col = e.target.closest('.col');
        if (!col || dragId === null) return;
        e.preventDefault();
        const id = dragId;
        board.querySelectorAll('.col').forEach(c => c.classList.remove('is-over'));
        move(id, col.dataset.status);
    });

    function setView(name) {
        const isBoard = name === 'board';
        $('boardView').hidden = !isBoard;
        $('listView').hidden = isBoard;
        $('viewBoard').setAttribute('aria-pressed', String(isBoard));
        $('viewList').setAttribute('aria-pressed', String(!isBoard));
        try { localStorage.setItem('dept_view', name); } catch (e) { /* ignore */ }
    }
    $('viewBoard').addEventListener('click', () => setView('board'));
    $('viewList').addEventListener('click', () => setView('list'));
    $('listStatus').addEventListener('change', renderList);
    $('listSearch').addEventListener('input', renderList);
    $('refreshBtn').addEventListener('click', load);

    setView(localStorage.getItem('dept_view') === 'list' ? 'list' : 'board');
    load();
});
