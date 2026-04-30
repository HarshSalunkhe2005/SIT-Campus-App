/**
 * SIT Campus App — Admin User Management JS (Refactored)
 */
document.addEventListener('DOMContentLoaded', async () => {
    'use strict';

    const token = localStorage.getItem('jwt_token');
    if (!token || localStorage.getItem('user_role') !== 'ADMIN') {
        window.location.href = '../auth/login.html';
        return;
    }

    const userTableBody = document.getElementById('userTableBody');
    const searchInput = document.getElementById('userSearchInput');
    const searchBtn = document.getElementById('userSearchBtn');

    let allUsers = [];

    async function loadUsers() {
        try {
            allUsers = await api.get('/admin/users');
            renderUsers(allUsers);
        } catch (err) {
            showToast('Failed to load users list.', 'error');
        }
    }

    function renderUsers(users) {
        userTableBody.innerHTML = '';
        if (users.length === 0) {
            userTableBody.innerHTML = '<tr><td colspan="5" style="text-align:center;">No users match your search.</td></tr>';
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

    /* ────────── SEARCH LOGIC ────────── */
    function performSearch() {
        const query = searchInput.value.toLowerCase().trim();
        if (!query) {
            renderUsers(allUsers);
            return;
        }
        const filtered = allUsers.filter(u => 
            (u.firstName + ' ' + u.lastName).toLowerCase().includes(query) ||
            (u.prn && u.prn.toLowerCase().includes(query)) ||
            u.email.toLowerCase().includes(query)
        );
        renderUsers(filtered);
    }

    searchBtn.addEventListener('click', performSearch);
    searchInput.addEventListener('keyup', (e) => {
        if (e.key === 'Enter') performSearch();
    });

    window.toggleUserStatus = function(email) {
        showToast(`User status toggle for ${email} coming in next update.`, 'info');
    };

    loadUsers();
});
