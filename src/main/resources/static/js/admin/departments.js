/**
 * SIT Campus App — Admin Department Management JS
 */
document.addEventListener('DOMContentLoaded', async () => {
    'use strict';

    // 1. Auth Guard
    const token = localStorage.getItem('jwt_token');
    if (!token || localStorage.getItem('user_role') !== 'ADMIN') {
        window.location.href = '../auth/login.html';
        return;
    }

    const deptTableBody = document.getElementById('deptTableBody');

    async function loadDepts() {
        try {
            const depts = await api.get('/admin/depts');
            renderDepts(depts);
        } catch (err) {
            console.error('Failed to load depts', err);
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
                    <td>${d.id}</td>
                    <td><strong>${d.name}</strong></td>
                    <td><span class="dept-type-badge">${d.type}</span></td>
                    <td>${d.email}</td>
                    <td>
                        <button class="sit-btn sit-btn--ghost" 
                                style="padding: 0.25rem 0.75rem; font-size: 0.875rem;"
                                onclick="editDept(${d.id})">Edit</button>
                    </td>
                </tr>
            `;
            deptTableBody.insertAdjacentHTML('beforeend', row);
        });
    }

    window.editDept = function(id) {
        showToast(`Edit department #${id} feature coming soon.`, 'info');
    };

    loadDepts();
});
