/**
 * SIT Campus App — Student Dashboard JS
 * Tabs, report form, campus feed, issue history, upvote.
 * Depends on: shared/toast.js, shared/image-upload.js
 */
(function () {
    'use strict';

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

    /* ────────── REPORT FORM — VALIDATION ────────── */

    const catSel    = document.getElementById('category');
    const locInp    = document.getElementById('location');
    const descTA    = document.getElementById('description');
    const descCount = document.getElementById('descCount');

    function validateField(field, errId, ok, msg) {
        const el = document.getElementById(errId);
        if (!ok) { el.textContent = msg; field.classList.add('sit-input--error'); return false; }
        el.textContent = ''; field.classList.remove('sit-input--error'); return true;
    }

    catSel.addEventListener('change', () =>
        validateField(catSel, 'categoryError', catSel.value !== '', 'Please select a category.'));

    locInp.addEventListener('input', () =>
        validateField(locInp, 'locationError', locInp.value.trim().length >= 3, 'Location too short.'));

    /* Merged: char count + validation in a single listener (#4) */
    descTA.addEventListener('input', () => {
        const len = descTA.value.length;
        descCount.textContent = `${len} / 500`;
        descCount.style.color = len < 20 ? '#ff4d4d' : '#666';
        validateField(descTA, 'descError', descTA.value.trim().length >= 20, 'Min 20 characters required.');
    });

    /* ────────── REPORT FORM — SUBMIT ────────── */

    document.getElementById('reportForm').addEventListener('submit', async e => {
        e.preventDefault();

        const ok = [
            validateField(catSel, 'categoryError', catSel.value !== '', 'Please select a category.'),
            validateField(locInp, 'locationError', locInp.value.trim().length >= 3, 'Location too short.'),
            validateField(descTA, 'descError', descTA.value.trim().length >= 20, 'Min 20 characters.'),
        ].every(Boolean);

        if (!uploader.hasFile()) {
            uploader.setError('A photo is required to submit a report.');
            if (ok) document.getElementById('uploadArea').scrollIntoView({ behavior: 'smooth', block: 'center' });
            return;
        }
        if (!ok) return;

        const btn = document.getElementById('reportSubmitBtn');
        btn.disabled = true; btn.textContent = 'Submitting…';
        try {
            const res = await fetch('/student/report', { method: 'POST', body: new FormData(e.target) });
            if (res.ok) {
                showToast('Issue reported successfully!', 'success');
                e.target.reset();
                descCount.textContent = '0 / 500';
                uploader.reset();
            } else {
                showToast((await res.text()) || 'Submission failed. Try again.', 'error');
            }
        } catch { showToast('Server connection failed.', 'error'); }
        finally { btn.disabled = false; btn.textContent = 'Submit Report'; }
    });

    /* ────────── SHARED: FILTER + SORT HELPER ────────── */

    function bindFilterSort(listId, emptyId, filterIds, sortId) {
        const list  = document.getElementById(listId);
        const empty = document.getElementById(emptyId);
        const filters = filterIds.map(id => document.getElementById(id));
        const sortEl  = document.getElementById(sortId);

        function apply() {
            let visible = 0;
            list.querySelectorAll('.sit-issue-card').forEach(card => {
                const show = filters.every(f => {
                    const key = f.dataset.filterKey;     // e.g. "status" or "category"
                    return f.value === 'ALL' || card.dataset[key] === f.value;
                });
                card.style.display = show ? 'flex' : 'none';
                if (show) visible++;
            });
            empty.style.display = visible === 0 ? 'flex' : 'none';
        }

        filters.forEach(f => f.addEventListener('change', apply));

        sortEl.addEventListener('change', () => {
            const cards = [...list.querySelectorAll('.sit-issue-card')];
            const mode = sortEl.value;
            cards.sort((a, b) => {
                if (mode === 'UPVOTES') return +b.dataset.upvotes - +a.dataset.upvotes;
                const da = new Date(a.dataset.date), db = new Date(b.dataset.date);
                return mode === 'DATE_DESC' ? db - da : da - db;
            });
            cards.forEach(c => list.appendChild(c));
        });
    }

    /* ────────── MY ISSUES — FILTER & SORT ────────── */

    bindFilterSort('issuesList', 'emptyState', ['filterStatus'], 'sortBy');

    /* ────────── CAMPUS FEED — FILTER & SORT ────────── */

    bindFilterSort('feedList', 'feedEmptyState', ['feedFilterStatus', 'feedFilterCategory'], 'feedSortBy');

    /* ────────── UPVOTE (event delegation on <main>) ────────── */

    document.querySelector('.sit-dashboard-main').addEventListener('click', async e => {
        const btn = e.target.closest('.sit-upvote-btn');
        if (!btn || btn.disabled) return;
        btn.disabled = true;
        const countEl = btn.querySelector('.sit-upvote-count');
        try {
            const res = await fetch(`/student/upvote/${btn.dataset.id}`, { method: 'POST' });
            if (res.ok) {
                const newVal = +countEl.textContent + 1;
                countEl.textContent = newVal;
                btn.closest('.sit-issue-card').dataset.upvotes = newVal;
                btn.classList.add('sit-upvote-btn--active');
                showToast('Upvoted!', 'success');
            } else {
                showToast('Already upvoted or an error occurred.', 'error');
                btn.disabled = false;
            }
        } catch { showToast('Connection failed.', 'error'); btn.disabled = false; }
    });

})();
