/**
 * SIT Campus App — Admin User Management JS
 */
document.addEventListener('DOMContentLoaded', async () => {
    'use strict';

    // 1. Auth Guard
    const token = localStorage.getItem('jwt_token');
    if (!token || localStorage.getItem('user_role') !== 'ADMIN') {
        window.location.href = '../auth/login.html';
        return;
    }

    const userTableBody = document.getElementById('userTableBody');

    async function loadUsers() {
        try {
            const users = await api.get('/admin/users');
            renderUsers(users);
        } catch (err) {
            console.error('Failed to load users', err);
            showToast('Failed to load users list.', 'error');
        }
    }

    function renderUsers(users) {
        userTableBody.innerHTML = '';
        
        if (users.length === 0) {
            userTableBody.innerHTML = '<tr><td colspan="5" style="text-align:center;">No users found.</td></tr>';
            return;
        }

        users.forEach(u => {
            const row = `
                <tr>
                    <td><strong>${u.prn || 'N/A'}</strong><br><small style="color:var(--text-secondary)">${u.email}</small></td>
                    <td>${u.firstName} ${u.lastName}</td>
                    <td><span class="role-badge">Student</span></td>
                    <td>${u.isVerified ? '✅ Verified' : '❌ Pending'}</td>
                    <td>
                        <button class="sit-btn sit-btn--ghost" 
                                style="padding: 0.25rem 0.75rem; font-size: 0.875rem; color: var(--danger-color);"
                                onclick="toggleUserStatus('${u.email}')">
                            ${u.isVerified ? 'Disable' : 'Enable'}
                        </button>
                    </td>
                </tr>
            `;
            userTableBody.insertAdjacentHTML('beforeend', row);
        });
    }

    window.toggleUserStatus = function(email) {
        showToast(`Management for ${email} coming in next update.`, 'info');
    };

    loadUsers();
});
