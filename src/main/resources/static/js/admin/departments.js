/**
 * SIT Campus App — Admin Department Management JS (Refactored)
 */
document.addEventListener('DOMContentLoaded', async () => {
    'use strict';

    const token = localStorage.getItem('jwt_token');
    if (!token || localStorage.getItem('user_role') !== 'ADMIN') {
        window.location.href = '../auth/login.html';
        return;
    }

    const deptTableBody = document.getElementById('deptTableBody');
    const addDeptBtn = document.getElementById('addDeptBtn');

    async function loadDepts() {
        try {
            const depts = await api.get('/admin/depts');
            renderDepts(depts);
        } catch (err) {
            showToast('Failed to load departments.', 'error');
        }
    }

    function renderDepts(depts) {
        deptTableBody.innerHTML = '';
        if (depts.length === 0) {
            deptTableBody.innerHTML = '<tr><td colspan="5" style="text-align:center;">No departments found.</td></tr>';
            return;
        }

        depts.forEach(d => {
            const row = `
                <tr>
                    <td>${Number(d.id)}</td>
                    <td><strong>${escapeHtml(d.name)}</strong></td>
                    <td><span class="dept-type-badge">${escapeHtml(d.type)}</span></td>
                    <td>${escapeHtml(d.email)}</td>
                    <td>
                        <button class="sit-btn sit-btn--ghost"
                                style="padding: 0.25rem 0.75rem; font-size: 0.875rem; color: var(--primary-color);"
                                onclick="editDept(${Number(d.id)})">Edit</button>
                        <button class="sit-btn sit-btn--ghost"
                                style="padding: 0.25rem 0.75rem; font-size: 0.875rem; color: var(--danger-color);"
                                onclick="deleteDept(${Number(d.id)})">Delete</button>
                    </td>
                </tr>
            `;
            deptTableBody.insertAdjacentHTML('beforeend', row);
        });
    }

    addDeptBtn.addEventListener('click', async () => {
        const name = prompt("Enter Department Name:");
        if (!name) return;
        const type = prompt("Enter Category (e.g. Electrical, IT):");
        const email = prompt("Enter Department Email:");
        const password = prompt("Login password for this department (leave blank to generate one):") || '';

        try {
            const created = await api.post('/admin/dept', { name, type, email, password });
            showToast('Department added!', 'success');
            if (created.initialPassword) {
                // shown only this once: the server stores just a hash
                alert(`Department login created.\n\nEmail: ${created.email}\nPassword: ${created.initialPassword}\n\nCopy this now, it will not be shown again.`);
            }
            loadDepts();
        } catch (err) {
            showToast(err.message || 'Failed to add department.', 'error');
        }
    });

    window.editDept = async function(id) {
        const name = prompt("New Dept Name:");
        if (!name) return;
        const type = prompt("New Category:");
        const email = prompt("New Email:");
        const password = prompt("New login password (leave blank to keep the current one):") || '';

        try {
            await api.put(`/admin/dept/${id}`, { name, type, email, password });
            showToast('Department updated!', 'success');
            loadDepts();
        } catch (err) {
            showToast(err.message || 'Failed to update department.', 'error');
        }
    };

    window.deleteDept = async function(id) {
        if (!confirm(`Permanently delete department #${id} and all its reports?`)) return;
        try {
            await api.request(`/admin/dept/${id}`, { method: 'DELETE' });
            showToast('Department deleted.', 'success');
            loadDepts();
        } catch (err) {
            showToast('Failed to delete department.', 'error');
        }
    };

    loadDepts();
});
