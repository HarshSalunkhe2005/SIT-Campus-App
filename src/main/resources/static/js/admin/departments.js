/**
 * SIT Campus App - Admin: departments (add, edit, reset password, delete).
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
    const KNOWN_TYPES = ['Electrical', 'Plumbing', 'Civil', 'Cleaning', 'IT', 'Furniture', 'Hostel', 'General'];
    let depts = [];

    async function load() {
        $('deptBody').innerHTML = `<tr><td colspan="4">${UI.skeleton(2)}</td></tr>`;
        try {
            depts = await api.get('/admin/depts');
        } catch (err) {
            showToast('Failed to load departments.', 'error');
            depts = [];
        }
        render();
    }

    function render() {
        $('deptBody').innerHTML = depts.length ? depts.map(d => `<tr>
            <td><div class="cell-main">${esc(d.name)}</div></td>
            <td><span class="pill">${esc(d.type)}</span></td>
            <td>${esc(d.email)}</td>
            <td style="white-space:nowrap;text-align:right">
                <button type="button" class="btn btn-secondary btn-sm" data-action="edit" data-id="${Number(d.id)}">${UI.icon('edit', 'icon-sm')}Edit</button>
                <button type="button" class="btn btn-danger btn-sm" data-action="delete" data-id="${Number(d.id)}">${UI.icon('trash', 'icon-sm')}Delete</button>
            </td></tr>`).join('')
            : `<tr><td colspan="4">${UI.empty({ icon: 'building', title: 'No departments yet', text: 'Add one for each category so reports have somewhere to go. Reports with no matching department go to "General".' })}</td></tr>`;
    }

    /** Add / edit form in a dialog. Resolves to the saved response, or null if cancelled. */
    function openForm(dept) {
        const editing = !!dept;
        const dlg = UI.modal({
            title: editing ? 'Edit department' : 'Add department',
            bodyHtml: `<form id="deptForm" class="stack" novalidate>
                <div class="field"><label class="label" for="dName">Name</label><input class="input" id="dName" maxlength="100" value="${esc(dept ? dept.name : '')}" placeholder="e.g. Library Department" required></div>
                <div class="field"><label class="label" for="dType">Category it handles</label>
                    <input class="input" id="dType" list="typeList" maxlength="30" value="${esc(dept ? dept.type : '')}" placeholder="e.g. Library" required>
                    <datalist id="typeList">${KNOWN_TYPES.map(t => `<option value="${esc(t)}"></option>`).join('')}</datalist>
                    <span class="hint">Matches the category students pick. Each category can have one department.</span></div>
                <div class="field"><label class="label" for="dEmail">Login email</label><input class="input" id="dEmail" type="email" value="${esc(dept ? dept.email : '')}" placeholder="library@sitpune.edu.in" required></div>
                <div class="field"><label class="label" for="dPass">${editing ? 'New password' : 'Password'}</label>
                    <input class="input" id="dPass" type="text" autocomplete="off" minlength="8" placeholder="${editing ? 'Leave blank to keep the current password' : 'Leave blank to generate one'}">
                    <span class="hint">${editing ? 'Set a new password if the department has forgotten theirs.' : 'If you leave it blank a strong password is generated and shown once.'}</span></div>
                <div class="error-text" id="dErr" role="alert"></div></form>`,
            footHtml: `<button type="button" class="btn btn-secondary" data-close>Cancel</button>
                       <button type="submit" form="deptForm" class="btn btn-primary" id="dSave">${editing ? 'Save changes' : 'Add department'}</button>`,
        });
        dlg.querySelector('#deptForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            const body = {
                name: dlg.querySelector('#dName').value.trim(),
                type: dlg.querySelector('#dType').value.trim(),
                email: dlg.querySelector('#dEmail').value.trim(),
                password: dlg.querySelector('#dPass').value,
            };
            const err = dlg.querySelector('#dErr');
            if (!body.name || !body.type || !body.email) { err.textContent = 'Fill in the name, category and email.'; return; }
            const save = dlg.querySelector('#dSave');
            save.disabled = true;
            try {
                const saved = editing ? await api.put(`/admin/dept/${dept.id}`, body) : await api.post('/admin/dept', body);
                dlg.close();
                showToast(editing ? 'Department updated.' : 'Department added.', 'success');
                if (saved.initialPassword) {
                    UI.showSecret({
                        title: 'Department login created',
                        message: `Share this with ${saved.name}. They log in with ${saved.email}. It will not be shown again.`,
                        secret: saved.initialPassword,
                    });
                }
                load();
            } catch (e2) {
                err.textContent = e2.message || 'Could not save the department.';
                save.disabled = false;
            }
        });
    }

    async function remove(dept) {
        if (!(await UI.confirm({
            title: 'Delete this department?',
            message: `${dept.name} and every issue assigned to it will be permanently deleted. This cannot be undone.`,
            confirmText: 'Delete permanently', danger: true,
        }))) return;
        try {
            await api.request(`/admin/dept/${dept.id}`, { method: 'DELETE' });
            showToast('Department deleted.', 'success');
            load();
        } catch (err) {
            showToast('Could not delete the department.', 'error');
        }
    }

    $('addBtn').addEventListener('click', () => openForm(null));
    $('deptBody').addEventListener('click', (e) => {
        const btn = e.target.closest('button[data-action]');
        if (!btn) return;
        const d = depts.find(x => Number(x.id) === Number(btn.dataset.id));
        if (!d) return;
        if (btn.dataset.action === 'edit') openForm(d); else remove(d);
    });

    load();
});
