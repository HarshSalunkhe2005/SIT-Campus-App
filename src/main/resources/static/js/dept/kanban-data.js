/**
 * SIT Campus App - Department Kanban Data Loading
 */

document.addEventListener('DOMContentLoaded', async () => {
    // 1. Ensure user is a department
    const role = localStorage.getItem('user_role');
    if (role !== 'DEPARTMENT' && role !== 'ADMIN') {
        window.location.href = '../auth/login.html';
        return;
    }

    // 2. Set dynamic dashboard title
    const deptName = localStorage.getItem('user_name') || 'Department';
    const deptId = localStorage.getItem('user_id');
    document.getElementById('dept-board-title').textContent = `${deptName} — Issue Board`;

    if (!deptId) {
        showToast('Department ID missing. Please log in again.', 'error');
        return;
    }

    // 3. Fetch real complaints for THIS department
    try {
        // Clear dummy cards first
        document.getElementById('body-PENDING').innerHTML = '';
        document.getElementById('body-IN_PROGRESS').innerHTML = '';
        document.getElementById('body-RESOLVED').innerHTML = '';

        document.getElementById('count-PENDING').textContent = '0';
        document.getElementById('count-IN_PROGRESS').textContent = '0';
        document.getElementById('count-RESOLVED').textContent = '0';

        // Load data from backend using global api
        const complaints = await api.get(`/dept/queue/${deptId}`);

        let counts = { PENDING: 0, IN_PROGRESS: 0, RESOLVED: 0 };

        complaints.forEach(issue => {
            let status = issue.status || 'PENDING';
            // Map ASSIGNED to PENDING for the Kanban column display
            if (status === 'ASSIGNED') status = 'PENDING';

            if (counts[status] !== undefined) counts[status]++;

            const photo = uploadUrl(issue.imageUrl);
            const proof = uploadUrl(issue.resolvedImageUrl);
            const cardHTML = `
                <div class="sit-kanban-card ${status === 'RESOLVED' ? 'sit-kanban-card--resolved' : ''}"
                     draggable="true"
                     data-id="${Number(issue.id)}"
                     data-status="${escapeHtml(status)}"
                     role="listitem">
                    <div class="sit-kanban-card__top">
                        <span class="sit-kanban-card__id">#${Number(issue.id)}</span>
                        <span class="sit-kanban-card__upvotes">▲ ${Number(issue.upvoteCount) || 0}</span>
                    </div>
                    <h4 class="sit-kanban-card__title">${escapeHtml(issue.category)}</h4>
                    <p class="sit-kanban-card__location">📍 <span>${escapeHtml(issue.location)}</span></p>
                    <p class="sit-kanban-card__desc">${escapeHtml(issue.description)}</p>
                    ${photo || proof ? `<p class="sit-kanban-card__desc">${photo ? `<a href="${escapeHtml(photo)}" target="_blank" rel="noopener">📷 Reported photo</a>` : ''}${photo && proof ? ' · ' : ''}${proof ? `<a href="${escapeHtml(proof)}" target="_blank" rel="noopener">✅ Proof photo</a>` : ''}</p>` : ''}
                    <div class="sit-kanban-card__footer">
                        <span class="sit-kanban-card__reporter">${escapeHtml(issue.studentName || 'Student')}</span>
                        <span class="sit-kanban-card__date">${escapeHtml(new Date(issue.createdAt).toLocaleDateString('en-GB', { day: '2-digit', month: 'short' }))}</span>
                    </div>
                </div>
            `;

            const colBody = document.getElementById(`body-${status}`);
            if (colBody) colBody.insertAdjacentHTML('beforeend', cardHTML);
        });

        // Update counts
        document.getElementById('count-PENDING').textContent = counts.PENDING;
        document.getElementById('count-IN_PROGRESS').textContent = counts.IN_PROGRESS;
        document.getElementById('count-RESOLVED').textContent = counts.RESOLVED;

        // Re-initialize drag and drop logic from kanban-drag-drop.js
        if (typeof initDragAndDrop === 'function') {
            initDragAndDrop();
        }

    } catch (error) {
        showToast('Failed to load issues: ' + error.message, 'error');
    }
});
