/**
 * SIT Campus App - Admin overview: statistics, department performance, all issues.
 * Depends on: shared/toast.js, shared/api.js, shared/ui.js
 */
document.addEventListener('DOMContentLoaded', () => {
    'use strict';

    if (!localStorage.getItem('jwt_token') || localStorage.getItem('user_role') !== 'ADMIN') {
        window.location.href = '../auth/login.html';
        return;
    }

    const $ = (id) => document.getElementById(id);
    const esc = UI.esc;
    const norm = (s) => (s === 'ASSIGNED' ? 'PENDING' : s);

    let complaints = [];
    let depts = [];
    let stats = null;

    /* ───────────── data ───────────── */

    async function load() {
        $('issueBody').innerHTML = `<tr><td colspan="6">${UI.skeleton(2)}</td></tr>`;
        try {
            [stats, complaints, depts] = await Promise.all([
                api.get('/admin/stats'), api.get('/admin/all-complaints'), api.get('/admin/depts'),
            ]);
        } catch (err) {
            showToast('Failed to load the dashboard: ' + err.message, 'error');
            return;
        }
        $('deptFilter').innerHTML = '<option value="ALL">All departments</option>'
            + depts.map(d => `<option value="${esc(d.name)}">${esc(d.name)}</option>`).join('');
        render();
    }

    /* ───────────── rendering ───────────── */

    function render() {
        renderKpis();
        renderBreakdown();
        renderDepts();
        renderIssues();
    }

    function renderKpis() {
        const s = stats;
        $('kpis').innerHTML = [
            ['Total issues', s.total, ''],
            ['New', s.pending + s.assigned, 'var(--st-new-fg)'],
            ['In progress', s.inProgress, '#b7791f'],
            ['Resolved', s.resolved, 'var(--st-resolved-fg)'],
            ['Closed', s.closed, 'var(--st-closed-fg)'],
            ['Departments', depts.length, ''],
        ].map(([label, n, color]) => `<div class="kpi"><div class="kpi-value">${Number(n) || 0}</div>
            <div class="kpi-label">${color ? `<span class="kpi-dot" style="background:${color}"></span>` : ''}${esc(label)}</div></div>`).join('');
    }

    function renderBreakdown() {
        const s = stats;
        const parts = [
            ['New', s.pending + s.assigned, 'var(--st-new-fg)'],
            ['In progress', s.inProgress, '#b7791f'],
            ['Resolved', s.resolved, 'var(--st-resolved-fg)'],
            ['Closed', s.closed, '#8a94a3'],
        ];
        if (!s.total) {
            $('breakdown').innerHTML = UI.empty({ icon: 'chart', title: 'No issues yet', text: 'The breakdown appears once students start reporting.' });
            return;
        }
        $('breakdown').innerHTML = `<div class="statusbar" role="img" aria-label="${parts.map(([l, n]) => `${l}: ${n}`).join(', ')}">
            ${parts.map(([l, n, c]) => `<span style="width:${(n / s.total) * 100}%;background:${c}" title="${esc(l)}: ${n}"></span>`).join('')}</div>
            <div class="legend">${parts.map(([l, n, c]) => `<span><span class="kpi-dot" style="background:${c}"></span>${esc(l)} <strong>${n}</strong></span>`).join('')}</div>`;
    }

    function renderDepts() {
        $('deptBody').innerHTML = depts.length ? depts.map(d => {
            const mine = complaints.filter(c => c.departmentName === d.name);
            const resolved = mine.filter(c => norm(c.status) === 'RESOLVED').length;
            const open = mine.filter(c => ['PENDING', 'IN_PROGRESS'].includes(norm(c.status))).length;
            const pct = mine.length ? Math.round((resolved / mine.length) * 100) : 0;
            return `<tr><td><div class="cell-main">${esc(d.name)}</div><div class="cell-sub">${mine.length} issue${mine.length === 1 ? '' : 's'}</div></td>
                <td class="num">${open}</td>
                <td><div style="display:flex;align-items:center;gap:8px"><div class="bar" style="flex:1" role="img" aria-label="${pct}% resolved"><span style="width:${pct}%"></span></div><span class="cell-sub" style="min-width:36px;text-align:right">${pct}%</span></div></td></tr>`;
        }).join('') : `<tr><td colspan="3">${UI.empty({ icon: 'building', title: 'No departments yet', action: '<a class="btn btn-primary" href="departments.html">Add a department</a>' })}</td></tr>`;
    }

    function renderIssues() {
        const q = $('search').value.trim().toLowerCase();
        const status = $('statusFilter').value;
        const dept = $('deptFilter').value;
        const rows = complaints.filter(c => {
            const st = norm(c.status);
            if (status === 'NEW' && st !== 'PENDING') return false;
            if (status !== 'ALL' && status !== 'NEW' && st !== status) return false;
            if (dept !== 'ALL' && c.departmentName !== dept) return false;
            return !q || [c.location, c.description, c.studentName, c.studentEmail, c.category].join(' ').toLowerCase().includes(q);
        });
        $('issueBody').innerHTML = rows.length ? rows.map(c => `<tr class="is-clickable" data-id="${Number(c.id)}" tabindex="0" aria-label="Open issue ${Number(c.id)}">
            <td><div class="cell-main">#${Number(c.id)} ${esc(c.location)}</div><div class="cell-sub truncate">${esc(c.category)}: ${esc(c.description)}</div></td>
            <td>${esc(c.departmentName || '')}</td>
            <td><div>${esc(c.studentName || '')}</div></td>
            <td><span class="cell-sub">${esc(UI.relTime(c.createdAt))}</span></td>
            <td>${UI.statusBadge(c.status)}</td>
            <td class="num">${Number(c.upvoteCount) || 0}</td></tr>`).join('')
            : `<tr><td colspan="6">${UI.empty({ icon: 'search', title: 'No issues match', text: complaints.length ? 'Try a different filter.' : 'Nothing has been reported yet.' })}</td></tr>`;
    }

    /* ───────────── details and actions ───────────── */

    function openIssue(id) {
        const c = complaints.find(x => Number(x.id) === Number(id));
        if (!c) return;
        const dlg = UI.issueDrawer(c, {
            showEmail: true,
            footHtml: `<label class="sr-only" for="statusSelect">Set status</label>
                <select class="select" id="statusSelect" style="width:auto">
                    <option value="ASSIGNED">New</option><option value="IN_PROGRESS">In progress</option>
                    <option value="RESOLVED">Resolved</option><option value="CLOSED">Closed</option></select>
                <button type="button" class="btn btn-primary" id="saveStatus">Update status</button>`,
        });
        dlg.querySelector('#statusSelect').value = norm(c.status) === 'PENDING' ? 'ASSIGNED' : c.status;
        dlg.querySelector('#saveStatus').addEventListener('click', async () => {
            const status = dlg.querySelector('#statusSelect').value;
            if (status === 'CLOSED' && !(await UI.confirm({ title: 'Close this issue?', message: 'Closing removes it from the department board. You can reopen it later by changing the status.', confirmText: 'Close issue' }))) return;
            try {
                await api.put('/admin/status', { complaintId: c.id, status });
                showToast(`Issue #${c.id} updated.`, 'success');
                dlg.close();
                load();
            } catch (err) {
                showToast(err.message || 'Could not update the status.', 'error');
            }
        });
    }

    $('issueBody').addEventListener('click', (e) => { const r = e.target.closest('[data-id]'); if (r && !e.target.closest('.thumb')) openIssue(r.dataset.id); });
    $('issueBody').addEventListener('keydown', (e) => { if (e.key === 'Enter') { const r = e.target.closest('[data-id]'); if (r) openIssue(r.dataset.id); } });
    ['statusFilter', 'deptFilter'].forEach(id => $(id).addEventListener('change', renderIssues));
    $('search').addEventListener('input', renderIssues);
    $('refreshBtn').addEventListener('click', load);

    load();
});
