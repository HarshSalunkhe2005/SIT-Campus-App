/**
 * SIT Campus App — Admin Hub JS
 * Table filter (status + dept) for the admin reports table.
 */
(function () {
    'use strict';

    const adminFilter     = document.getElementById('adminFilter');
    const adminDeptFilter = document.getElementById('adminDeptFilter');
    const adminEmptyState = document.getElementById('adminEmptyState');
    const tbody           = document.getElementById('adminIssuesTbody');

    if (!tbody) return;

    function applyAdminFilter() {
        const status = adminFilter.value;
        const dept   = adminDeptFilter.value;
        let vis = 0;
        tbody.querySelectorAll('tr').forEach(row => {
            const statusOk = status === 'ALL' || row.dataset.status === status;
            const deptOk   = dept   === 'ALL' || row.dataset.dept   === dept;
            row.style.display = statusOk && deptOk ? '' : 'none';
            if (statusOk && deptOk) vis++;
        });
        adminEmptyState.style.display = vis === 0 ? 'flex' : 'none';
    }

    adminFilter.addEventListener('change', applyAdminFilter);
    adminDeptFilter.addEventListener('change', applyAdminFilter);
})();
