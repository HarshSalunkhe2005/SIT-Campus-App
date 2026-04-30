/**
 * SIT Campus App — Admin Hub JS (Refactored for Real API)
 */
document.addEventListener('DOMContentLoaded', async () => {
    'use strict';

    // 1. Auth Guard
    const token = localStorage.getItem('jwt_token');
    if (!token || localStorage.getItem('user_role') !== 'ADMIN') {
        window.location.href = '../auth/login.html';
        return;
    }

    const tbody = document.getElementById('adminIssuesTbody');
    const deptList = document.getElementById('deptList');
    const adminFilter = document.getElementById('adminFilter');
    const adminDeptFilter = document.getElementById('adminDeptFilter');

    let allComplaints = [];

    /* ────────── DATA LOADING ────────── */

    async function loadAdminData() {
        try {
            // Load Stats
            const stats = await api.get('/admin/stats');
            updateStatCards(stats);

            // Load Complaints
            allComplaints = await api.get('/admin/all-complaints');
            
            // Load Departments (for filters and performance)
            const depts = await api.get('/admin/depts');
            populateDeptFilter(depts);
            renderDeptPerformance(depts, allComplaints);

            // Initial Table Render
            renderTable(allComplaints);

        } catch (err) {
            console.error('Failed to load admin data', err);
            showToast('Failed to load dashboard data.', 'error');
        }
    }

    function updateStatCards(stats) {
        document.querySelector('.sit-stat-card--total .sit-stat-number').textContent = stats.total;
        document.querySelector('.sit-stat-card--pending .sit-stat-number').textContent = stats.assigned + stats.pending;
        document.querySelector('.sit-stat-card--inprogress .sit-stat-number').textContent = stats.inProgress;
        document.querySelector('.sit-stat-card--resolved .sit-stat-number').textContent = stats.resolved;
        
        // Active depts stat card (we'll update this after loading depts)
    }

    function populateDeptFilter(depts) {
        adminDeptFilter.innerHTML = '<option value="ALL">All Depts</option>';
        depts.forEach(d => {
            const opt = document.createElement('option');
            opt.value = d.name;
            opt.textContent = d.name;
            adminDeptFilter.appendChild(opt);
        });
        document.querySelector('.sit-stat-card--depts .sit-stat-number').textContent = depts.length;
    }

    function renderTable(complaints) {
        tbody.innerHTML = '';
        const statusFilter = adminFilter.value;
        const deptFilter = adminDeptFilter.value;

        const filtered = complaints.filter(c => {
            const sMatch = statusFilter === 'ALL' || c.status === statusFilter || (statusFilter === 'PENDING' && c.status === 'ASSIGNED');
            const dMatch = deptFilter === 'ALL' || c.departmentName === deptFilter;
            return sMatch && dMatch;
        });

        if (filtered.length === 0) {
            document.getElementById('adminEmptyState').style.display = 'flex';
            return;
        }
        document.getElementById('adminEmptyState').style.display = 'none';

        filtered.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

        filtered.forEach(c => {
            const dateStr = new Date(c.createdAt).toLocaleDateString('en-GB', { day: '2-digit', month: 'short' });
            const row = `
                <tr data-id="${c.id}" data-status="${c.status}" data-dept="${c.departmentName}">
                    <td class="sit-table__id">#${c.id}</td>
                    <td>${c.category}</td>
                    <td>${c.location}</td>
                    <td>${c.studentName || 'Anon'}</td>
                    <td><span class="sit-status-badge sit-status-badge--${c.status.toLowerCase()}">${c.status.replace('_', ' ')}</span></td>
                    <td>${dateStr}</td>
                    <td><span class="sit-upvote-pill">▲ ${c.upvoteCount || 0}</span></td>
                    <td>
                        <button class="sit-btn-icon sit-btn-icon--danger" onclick="deleteIssue(${c.id})" title="Delete Report">🗑️</button>
                    </td>
                </tr>
            `;
            tbody.insertAdjacentHTML('beforeend', row);
        });
    }

    function renderDeptPerformance(depts, complaints) {
        deptList.innerHTML = '';
        depts.forEach(dept => {
            const deptComplaints = complaints.filter(c => c.departmentName === dept.name);
            const total = deptComplaints.length;
            const resolved = deptComplaints.filter(c => c.status === 'RESOLVED').length;
            const pct = total === 0 ? 0 : Math.round((resolved / total) * 100);

            const html = `
                <div class="sit-dept-row">
                    <div class="sit-dept-row__header">
                        <span class="sit-dept-name">${dept.name}</span>
                        <span class="sit-dept-total">${total} issues</span>
                    </div>
                    <div class="sit-progress-bar">
                        <div class="sit-progress-bar__fill" style="width: ${pct}%" role="progressbar"></div>
                    </div>
                    <div class="sit-dept-row__meta">
                        <span class="sit-dept-resolved">${resolved} resolved</span>
                        <span class="sit-dept-pct">${pct}%</span>
                    </div>
                </div>
            `;
            deptList.insertAdjacentHTML('beforeend', html);
        });
    }

    /* ────────── FILTERS ────────── */
    adminFilter.addEventListener('change', () => renderTable(allComplaints));
    adminDeptFilter.addEventListener('change', () => renderTable(allComplaints));

    /* ────────── ACTIONS ────────── */
    window.deleteIssue = async function(id) {
        if (!confirm(`Delete report #${id}?`)) return;
        try {
            // Reusing updateStatus to 'CLOSED' as a soft delete for now, 
            // or we could add a DELETE endpoint to backend.
            await api.put('/admin/status', { complaintId: id, status: 'CLOSED' });
            showToast(`Report #${id} closed.`, 'success');
            loadAdminData();
        } catch (err) {
            showToast('Action failed.', 'error');
        }
    };

    // Load everything
    loadAdminData();
});
