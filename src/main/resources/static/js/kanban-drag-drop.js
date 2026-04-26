/**
 * SIT Campus App — Kanban Drag & Drop
 * Requires proof image when moving to IN_PROGRESS or RESOLVED.
 */

(function () {
    'use strict';

    const STATUSES = ['PENDING', 'IN_PROGRESS', 'RESOLVED'];
    const board = document.getElementById('kanbanBoard');
    if (!board) return;

    let dragCard = null;
    let placeholder = null;

    /* ── Pending move state (held while modal is open) ── */
    let pendingCard = null;
    let pendingNewStatus = null;
    let pendingOldStatus = null;
    let pendingBody = null;
    let pendingAfter = null;

    /* ── Toast ── */
    function showToast(msg, type = 'success') {
        const t = document.getElementById('sit-toast');
        if (!t) return;
        t.textContent = msg;
        t.className = `sit-toast sit-toast--${type} sit-toast--show`;
        clearTimeout(t._timer);
        t._timer = setTimeout(() => t.classList.remove('sit-toast--show'), 3500);
    }

    /* ── Column counts ── */
    function updateCounts() {
        STATUSES.forEach(status => {
            const body = document.getElementById('body-' + status);
            const badge = document.getElementById('count-' + status);
            if (body && badge)
                badge.textContent = body.querySelectorAll('.sit-kanban-card').length;
        });
    }

    function makePlaceholder() {
        const el = document.createElement('div');
        el.className = 'sit-kanban-placeholder';
        return el;
    }

    /* ── Attach drag to card ── */
    function attachDrag(card) {
        card.addEventListener('dragstart', onDragStart);
        card.addEventListener('dragend', onDragEnd);
    }

    document.querySelectorAll('.sit-kanban-card').forEach(attachDrag);
    updateCounts();

    document.querySelectorAll('.sit-kanban-col__body').forEach(body => {
        body.addEventListener('dragover', onDragOver);
        body.addEventListener('dragenter', onDragEnter);
        body.addEventListener('dragleave', onDragLeave);
        body.addEventListener('drop', onDrop);
    });

    /* ────────── DRAG EVENTS ────────── */

    function onDragStart(e) {
        dragCard = this;
        dragCard.classList.add('sit-dragging');
        e.dataTransfer.effectAllowed = 'move';
        e.dataTransfer.setData('text/plain', dragCard.dataset.id);
        placeholder = makePlaceholder();
    }

    function onDragEnd() {
        dragCard && dragCard.classList.remove('sit-dragging');
        placeholder && placeholder.remove();
        placeholder = null;
        dragCard = null;
        document.querySelectorAll('.sit-kanban-col')
            .forEach(c => c.classList.remove('sit-drop-over'));
    }

    function onDragEnter(e) {
        e.preventDefault();
        this.closest('.sit-kanban-col').classList.add('sit-drop-over');
    }

    function onDragLeave(e) {
        if (!this.contains(e.relatedTarget)) {
            this.closest('.sit-kanban-col').classList.remove('sit-drop-over');
            placeholder && placeholder.remove();
        }
    }

    function onDragOver(e) {
        e.preventDefault();
        e.dataTransfer.dropEffect = 'move';
        if (!dragCard) return;
        const afterCard = getDragAfterCard(this, e.clientY);
        afterCard ? this.insertBefore(placeholder, afterCard) : this.appendChild(placeholder);
    }

    function onDrop(e) {
        e.preventDefault();
        if (!dragCard) return;

        const body = this;
        const col = body.closest('.sit-kanban-col');
        const newStatus = col.dataset.status;
        const oldStatus = dragCard.dataset.status;

        col.classList.remove('sit-drop-over');
        placeholder && placeholder.remove();

        if (newStatus === oldStatus) return;

        /* ── Block reverse moves (only forward: PENDING → IN_PROGRESS → RESOLVED) ── */
        const STATUS_ORDER = { 'PENDING': 0, 'IN_PROGRESS': 1, 'RESOLVED': 2 };
        if (STATUS_ORDER[newStatus] < STATUS_ORDER[oldStatus]) {
            showToast('Cannot move issues backwards. Status can only move forward.', 'error');
            return;
        }

        const afterCard = getDragAfterCard(body, e.clientY);

        /* Moves to IN_PROGRESS or RESOLVED require a proof image */
        if (newStatus === 'IN_PROGRESS' || newStatus === 'RESOLVED') {
            /* Store pending move details */
            pendingCard = dragCard;
            pendingNewStatus = newStatus;
            pendingOldStatus = oldStatus;
            pendingBody = body;
            pendingAfter = afterCard;
            openModal(newStatus);
        } else {
            /* PENDING — no image needed */
            commitMove(dragCard, body, afterCard, oldStatus, newStatus, null);
        }
    }

    /* ────────── MODAL ────────── */

    const modal = document.getElementById('statusModal');
    const modalClose = document.getElementById('modalClose');
    const modalCancel = document.getElementById('modalCancelBtn');
    const modalConfirm = document.getElementById('modalConfirmBtn');
    const modalSubtitle = document.getElementById('modalSubtitle');
    const modalUpload = document.getElementById('modalUploadArea');
    const modalFileIn = document.getElementById('modalFileInput');
    const modalPrompt = document.getElementById('modalUploadPrompt');
    const modalPreview = document.getElementById('modalImagePreview');
    const modalImg = document.getElementById('modalPreviewImg');
    const modalRemove = document.getElementById('modalRemoveImage');
    const modalError = document.getElementById('modalImageError');

    let modalFile = null;

    function openModal(newStatus) {
        modalFile = null;
        modalFileIn.value = '';
        modalImg.src = '';
        modalPreview.style.display = 'none';
        modalPrompt.style.display = 'flex';
        modalError.textContent = '';
        modalConfirm.disabled = true;
        modalUpload.classList.remove('sit-upload-area--error');

        const label = newStatus === 'IN_PROGRESS' ? 'In Progress' : 'Resolved';
        modalSubtitle.textContent =
            `Upload a photo confirming this issue has been moved to "${label}". Required.`;

        modal.hidden = false;
        modal.focus();
    }

    function closeModal() {
        modal.hidden = true;
        pendingCard = pendingNewStatus = pendingOldStatus = pendingBody = pendingAfter = null;
        modalFile = null;
    }

    modalClose.addEventListener('click', closeModal);
    modalCancel.addEventListener('click', closeModal);
    modal.addEventListener('click', e => { if (e.target === modal) closeModal(); });
    document.addEventListener('keydown', e => { if (e.key === 'Escape' && !modal.hidden) closeModal(); });

    /* Upload area click */
    modalUpload.addEventListener('click', e => {
        if (!e.target.closest('#modalRemoveImage')) modalFileIn.click();
    });

    /* Drag-over inside modal */
    modalUpload.addEventListener('dragover', e => {
        e.preventDefault(); modalUpload.classList.add('sit-upload-area--drag');
    });
    modalUpload.addEventListener('dragleave', () => modalUpload.classList.remove('sit-upload-area--drag'));
    modalUpload.addEventListener('drop', e => {
        e.preventDefault();
        modalUpload.classList.remove('sit-upload-area--drag');
        if (e.dataTransfer.files[0]) handleModalFile(e.dataTransfer.files[0]);
    });

    modalFileIn.addEventListener('change', () => {
        if (modalFileIn.files[0]) handleModalFile(modalFileIn.files[0]);
    });

    modalRemove.addEventListener('click', e => {
        e.stopPropagation();
        modalFile = null;
        modalFileIn.value = '';
        modalImg.src = '';
        modalPreview.style.display = 'none';
        modalPrompt.style.display = 'flex';
        modalError.textContent = 'A proof image is required.';
        modalUpload.classList.add('sit-upload-area--error');
        modalConfirm.disabled = true;
    });

    function handleModalFile(file) {
        modalError.textContent = '';
        modalUpload.classList.remove('sit-upload-area--error');

        if (!file.type.startsWith('image/')) {
            modalError.textContent = 'Only image files are allowed.';
            modalUpload.classList.add('sit-upload-area--error');
            return;
        }
        if (file.size > 5 * 1024 * 1024) {
            modalError.textContent = 'Image must be under 5 MB.';
            modalUpload.classList.add('sit-upload-area--error');
            return;
        }

        modalFile = file;
        const reader = new FileReader();
        reader.onload = ev => {
            modalImg.src = ev.target.result;
            modalPrompt.style.display = 'none';
            modalPreview.style.display = 'flex';
        };
        reader.readAsDataURL(file);
        modalConfirm.disabled = false;
    }

    /* Confirm button */
    modalConfirm.addEventListener('click', async () => {
        if (!modalFile) {
            modalError.textContent = 'Please attach a proof image before confirming.';
            modalUpload.classList.add('sit-upload-area--error');
            return;
        }

        modalConfirm.disabled = true;
        modalConfirm.textContent = 'Saving…';

        const card = pendingCard;
        const newStatus = pendingNewStatus;
        const oldStatus = pendingOldStatus;
        const body = pendingBody;
        const after = pendingAfter;

        closeModal();
        await commitMove(card, body, after, oldStatus, newStatus, modalFile);
        modalConfirm.textContent = 'Confirm Move';
    });

    /* ────────── COMMIT MOVE ────────── */

    async function commitMove(card, body, afterCard, oldStatus, newStatus, imageFile) {
        /* Move card visually */
        afterCard ? body.insertBefore(card, afterCard) : body.appendChild(card);

        if (newStatus === 'RESOLVED') {
            card.classList.add('sit-kanban-card--resolved');
        } else {
            card.classList.remove('sit-kanban-card--resolved');
        }
        card.dataset.status = newStatus;
        updateCounts();

        const id = card.dataset.id;
        if (!id || id.startsWith('demo')) {
            showToast(`🔄 Moved to ${newStatus.replace('_', ' ')}`, 'success');
            return;
        }

        /* Build multipart payload */
        const formData = new FormData();
        formData.append('status', newStatus);
        if (imageFile) formData.append('proofImage', imageFile);

        try {
            const res = await fetch(`/dept/issue/${id}/status`, {
                method: 'PATCH',
                body: formData
            });
            if (res.ok) {
                showToast(`✅ Issue #${id} → ${newStatus.replace('_', ' ')}`, 'success');
            } else {
                showToast(`❌ Failed to update #${id}. Reverted.`, 'error');
                revertCard(card, oldStatus);
            }
        } catch {
            showToast('❌ Connection failed. Change reverted.', 'error');
            revertCard(card, oldStatus);
        }
    }

    /* ────────── HELPERS ────────── */

    function getDragAfterCard(container, y) {
        const cards = [...container.querySelectorAll('.sit-kanban-card:not(.sit-dragging)')];
        return cards.reduce((closest, card) => {
            const box = card.getBoundingClientRect();
            const offset = y - box.top - box.height / 2;
            if (offset < 0 && offset > closest.offset)
                return { offset, element: card };
            return closest;
        }, { offset: Number.NEGATIVE_INFINITY }).element;
    }

    function revertCard(card, oldStatus) {
        const oldBody = document.getElementById('body-' + oldStatus);
        if (oldBody) {
            oldBody.appendChild(card);
            card.dataset.status = oldStatus;
            oldStatus === 'RESOLVED'
                ? card.classList.add('sit-kanban-card--resolved')
                : card.classList.remove('sit-kanban-card--resolved');
            updateCounts();
        }
    }

})();