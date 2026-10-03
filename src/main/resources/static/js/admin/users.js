/**
 * SIT Campus App - Admin: students list (search, enable/disable, delete).
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
    let users = [];

    async function load() {
        $('userBody').innerHTML = `<tr><td colspan="5">${UI.skeleton(2)}</td></tr>`;
        try {
            users = await api.get('/admin/users');
        } catch (err) {
            showToast('Failed to load students.', 'error');
            users = [];
        }
        render();
    }

    function render() {
        const q = $('search').value.trim().toLowerCase();
        const filter = $('statusFilter').value;
        const rows = users.filter(u =>
            (filter === 'ALL' || (filter === 'ACTIVE') === !!u.isVerified)
            && (!q || [u.firstName, u.lastName, u.prn, u.email].join(' ').toLowerCase().includes(q)));

        $('userBody').innerHTML = rows.length ? rows.map(u => `<tr>
            <td><div class="cell-main">${esc(u.firstName)} ${esc(u.lastName)}</div><div class="cell-sub">${esc(u.email)}</div></td>
            <td>${esc(u.prn || 'Not given')}</td>
            <td>${esc(u.batchYear || '')}</td>
            <td>${u.isVerified
                ? `<span class="badge st-resolved">${UI.icon('checkCircle')}Active</span>`
                : `<span class="badge st-closed">${UI.icon('lock')}Inactive</span>`}</td>
            <td style="white-space:nowrap;text-align:right">
                <button type="button" class="btn btn-secondary btn-sm" data-action="toggle" data-email="${esc(u.email)}">${u.isVerified ? 'Disable' : 'Enable'}</button>
                <button type="button" class="btn btn-danger btn-sm" data-action="delete" data-email="${esc(u.email)}">${UI.icon('trash', 'icon-sm')}Delete</button>
            </td></tr>`).join('')
            : `<tr><td colspan="5">${UI.empty({ icon: 'users', title: users.length ? 'No students match' : 'No students yet', text: users.length ? 'Try a different search or filter.' : 'Students appear here after they register.' })}</td></tr>`;
    }

    async function toggle(email) {
        const u = users.find(x => x.email === email);
        if (u && u.isVerified && !(await UI.confirm({
            title: 'Disable this account?', message: `${u.firstName} ${u.lastName} will be logged out and will not be able to log in until you enable the account again.`, confirmText: 'Disable account',
        }))) return;
        try {
            await api.post(`/admin/user/${encodeURIComponent(email)}/toggle`);
            showToast('Account updated.', 'success');
            load();
        } catch (err) {
            showToast('Could not update the account.', 'error');
        }
    }

    async function remove(email) {
        const u = users.find(x => x.email === email);
        if (!(await UI.confirm({
            title: 'Delete this student?', message: `${u ? `${u.firstName} ${u.lastName}` : email} and every issue they reported will be permanently deleted. This cannot be undone.`, confirmText: 'Delete permanently', danger: true,
        }))) return;
        try {
            await api.request(`/admin/user/${encodeURIComponent(email)}`, { method: 'DELETE' });
            showToast('Student deleted.', 'success');
            load();
        } catch (err) {
            showToast('Could not delete the student.', 'error');
        }
    }

    $('userBody').addEventListener('click', (e) => {
        const btn = e.target.closest('button[data-action]');
        if (!btn) return;
        if (btn.dataset.action === 'toggle') toggle(btn.dataset.email);
        else remove(btn.dataset.email);
    });
    $('search').addEventListener('input', render);
    $('statusFilter').addEventListener('change', render);

    load();
});
