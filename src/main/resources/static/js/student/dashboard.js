/**
 * SIT Campus App — Student Dashboard JS (Refactored for JWT & Real API)
 */
document.addEventListener('DOMContentLoaded', async () => {
    'use strict';

    // 1. Auth Guard
    const token = localStorage.getItem('jwt_token');
    if (!token || localStorage.getItem('user_role') !== 'STUDENT') {
        window.location.href = '../auth/login.html';
        return;
    }

    // 2. Set UI Elements
    const studentName = localStorage.getItem('user_name') || 'Student';
    document.querySelector('.sit-page-title .sit-highlight').textContent = studentName;

    /* ────────── TAB SWITCHING ────────── */
    document.querySelectorAll('.sit-tab-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.sit-tab-btn').forEach(b => {
                b.classList.remove('sit-tab-btn--active');
                b.setAttribute('aria-selected', 'false');
            });
            document.querySelectorAll('.sit-tab-content').forEach(p => {
                p.classList.remove('sit-tab-content--active');
                p.hidden = true;
            });
            btn.classList.add('sit-tab-btn--active');
            btn.setAttribute('aria-selected', 'true');
            const panel = document.getElementById('tab-' + btn.dataset.tab);
            panel.classList.add('sit-tab-content--active');
            panel.hidden = false;

            // Trigger data load if switching to feed or history
            if (btn.dataset.tab === 'feed') loadCampusFeed();
            if (btn.dataset.tab === 'history') loadMyIssues();
        });
    });

    /* ────────── REPORT FORM — IMAGE UPLOAD ────────── */
    const uploader = initImageUpload({
        areaId:      'uploadArea',
        inputId:     'issueImage',
        promptId:    'uploadPrompt',
        previewId:   'imagePreview',
        imgId:       'previewImg',
        removeId:    'removeImage',
        errorId:     'imageError',
        requiredMsg: 'A photo is required to submit a report.'
    });

    /* ────────── REPORT FORM — SUBMIT ────────── */
    const reportForm = document.getElementById('reportForm');
    reportForm.addEventListener('submit', async e => {
        e.preventDefault();
        
        const btn = document.getElementById('reportSubmitBtn');
        if (!uploader.hasFile()) {
            uploader.setError('A photo is required to submit a report.');
            return;
        }

        btn.disabled = true;
        btn.textContent = 'Submitting…';

        try {
            const formData = new FormData(reportForm);
            
            // Build the ComplaintRequest JSON
            const complaintRequest = {
                location: formData.get('location'),
                description: formData.get('description'),
                // Backend detectCategory will override category based on desc keywords
                // but we can pass it if we want to align with entity fields
                priority: 'MEDIUM' 
            };

            // Multipart request: 'complaint' (JSON) and 'image' (File)
            const multipartBody = new FormData();
            multipartBody.append('complaint', new Blob([JSON.stringify(complaintRequest)], { type: 'application/json' }));
            multipartBody.append('image', formData.get('image'));

            const response = await fetch(`${api.BASE_URL}/student/report`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`
                },
                body: multipartBody
            });

            if (!response.ok) throw new Error(await response.text());

            showToast('Issue reported successfully!', 'success');
            reportForm.reset();
            uploader.reset();
            document.getElementById('descCount').textContent = '0 / 500';
            
            // Refresh stats
            loadStats();
        } catch (error) {
            showToast('Submission failed: ' + error.message, 'error');
        } finally {
            btn.disabled = false;
            btn.textContent = 'Submit Report';
        }
    });

    /* ────────── DATA LOADING ────────── */

    async function loadStats() {
        try {
            const complaints = await api.get('/student/my-reports');
            const counts = { TOTAL: complaints.length, PENDING: 0, IN_PROGRESS: 0, RESOLVED: 0 };
            complaints.forEach(c => {
                if (c.status === 'PENDING' || c.status === 'ASSIGNED') counts.PENDING++;
                else if (c.status === 'IN_PROGRESS') counts.IN_PROGRESS++;
                else if (c.status === 'RESOLVED') counts.RESOLVED++;
            });

            document.querySelector('.sit-stat-card--total .sit-stat-number').textContent = counts.TOTAL;
            document.querySelector('.sit-stat-card--pending .sit-stat-number').textContent = counts.PENDING;
            document.querySelector('.sit-stat-card--inprogress .sit-stat-number').textContent = counts.IN_PROGRESS;
            document.querySelector('.sit-stat-card--resolved .sit-stat-number').textContent = counts.RESOLVED;
        } catch (err) {
            console.error('Failed to load stats', err);
        }
    }

    async function loadCampusFeed() {
        const feedList = document.getElementById('feedList');
        feedList.innerHTML = '<p class="sit-loading">Loading campus feed...</p>';
        try {
            const reports = await api.get('/student/all-reports');
            renderIssues(feedList, reports, 'feedEmptyState', true);
        } catch (err) {
            feedList.innerHTML = '<p class="sit-error">Failed to load feed.</p>';
        }
    }

    async function loadMyIssues() {
        const historyList = document.getElementById('issuesList');
        historyList.innerHTML = '<p class="sit-loading">Loading your reports...</p>';
        try {
            const reports = await api.get('/student/my-reports');
            renderIssues(historyList, reports, 'emptyState', false);
        } catch (err) {
            historyList.innerHTML = '<p class="sit-error">Failed to load history.</p>';
        }
    }

    function renderIssues(container, issues, emptyId, showUpvote) {
        container.innerHTML = '';
        const emptyState = document.getElementById(emptyId);
        
        if (!issues || issues.length === 0) {
            emptyState.style.display = 'flex';
            return;
        }
        emptyState.style.display = 'none';

        issues.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

        issues.forEach(issue => {
            const dateStr = new Date(issue.createdAt).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
            const card = `
                <div class="sit-issue-card" data-status="${issue.status}" data-category="${issue.category}" data-upvotes="${issue.upvoteCount || 0}" data-date="${issue.createdAt}">
                    <div class="sit-issue-card__left">
                        <span class="sit-status-badge sit-status-badge--${issue.status.toLowerCase()}">${issue.status.replace('_', ' ')}</span>
                        <h4 class="sit-issue-card__title">${issue.category}</h4>
                        <p class="sit-issue-card__location">📍 <span>${issue.location}</span></p>
                        <p class="sit-issue-card__desc">${issue.description}</p>
                        <small class="sit-issue-card__date">
                            Reported by <strong>${issue.studentName || 'Anonymous'}</strong> · ${dateStr}
                        </small>
                    </div>
                    <div class="sit-issue-card__right">
                        <button class="sit-upvote-btn ${showUpvote ? '' : 'sit-upvote-btn--disabled'}" data-id="${issue.id}" ${showUpvote ? '' : 'disabled'}>
                            <span class="sit-upvote-icon">▲</span>
                            <span class="sit-upvote-count">${issue.upvoteCount || 0}</span>
                        </button>
                    </div>
                </div>
            `;
            container.insertAdjacentHTML('beforeend', card);
        });

        // Attach upvote listeners if applicable
        if (showUpvote) {
            container.querySelectorAll('.sit-upvote-btn').forEach(btn => {
                btn.addEventListener('click', async (e) => {
                    e.stopPropagation();
                    const id = btn.dataset.id;
                    try {
                        const updated = await api.post(`/student/upvote/${id}`);
                        btn.querySelector('.sit-upvote-count').textContent = updated.upvoteCount;
                        btn.classList.add('sit-upvote-btn--active');
                        showToast('Upvoted!', 'success');
                    } catch (err) {
                        showToast('Upvote failed.', 'error');
                    }
                });
            });
        }
    }

    // Initial Load
    loadStats();
});
