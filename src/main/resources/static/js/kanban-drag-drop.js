/**
 * SIT Campus App — Kanban Drag & Drop
 * Requires proof image when moving to IN_PROGRESS or RESOLVED.
 * Depends on: shared/toast.js, shared/image-upload.js
 */

(function () {
    'use strict';

    const STATUSES     = ['PENDING', 'IN_PROGRESS', 'RESOLVED'];
    const STATUS_ORDER = { PENDING: 0, IN_PROGRESS: 1, RESOLVED: 2 };
    const board        = document.getElementById('kanbanBoard');
    if (!board) return;

    let dragCard    = null;
    let placeholder = null;

    /* Pending move state (held while modal is open) */
    let pendingCard = null;
    let pendingNewStatus = null;
    let pendingOldStatus = null;
    let pendingBody  = null;
    let pendingAfter = null;

    /* ── Column counts ── */
    function updateCounts() {
        STATUSES.forEach(status => {
            const body  = document.getElementById('body-' + status);
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
        const col = this.closest('.sit-kanban-col');
        
        // Visual feedback: only highlight if it's a valid forward move
        if (dragCard) {
            const oldIdx = STATUS_ORDER[dragCard.dataset.status] || 0;
            const newIdx = STATUS_ORDER[col.dataset.status] || 0;
            if (newIdx >= oldIdx) {
                col.classList.add('sit-drop-over');
            }
        }
    }

    function onDragLeave(e) {
        if (!this.contains(e.relatedTarget)) {
            this.closest('.sit-kanban-col').classList.remove('sit-drop-over');
        }
    }

    function onDragOver(e) {
        e.preventDefault();
        e.dataTransfer.dropEffect = 'move';
        if (!dragCard) return;

        const col = this.closest('.sit-kanban-col');
        const oldIdx = STATUS_ORDER[dragCard.dataset.status] || 0;
        const newIdx = STATUS_ORDER[col.dataset.status] || 0;

        // BLOCK VISUAL PLACEHOLDER IF BACKWARD
        if (newIdx < oldIdx) {
            e.dataTransfer.dropEffect = 'none';
            placeholder && placeholder.remove();
            return;
        }

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

        /* Block reverse moves (only forward: PENDING (0) → IN_PROGRESS (1) → RESOLVED (2)) */
        const oldIdx = STATUS_ORDER[oldStatus] || 0;
        const newIdx = STATUS_ORDER[newStatus] || 0;

        if (newIdx < oldIdx) {
            showToast('Invalid Move: Status can only move forward (New → In Progress → Resolved).', 'error');
            return;
        }

        const afterCard = getDragAfterCard(body, e.clientY);

        /* Moves to RESOLVED require a proof image */
        if (newStatus === 'RESOLVED') {
            pendingCard = dragCard;
            pendingNewStatus = newStatus;
            pendingOldStatus = oldStatus;
            pendingBody = body;
            pendingAfter = afterCard;
            openModal(newStatus);
        } else {
            commitMove(dragCard, body, afterCard, oldStatus, newStatus, null);
        }
    }

    /* ────────── MODAL ────────── */

    const modal        = document.getElementById('statusModal');
    const modalClose   = document.getElementById('modalClose');
    const modalCancel  = document.getElementById('modalCancelBtn');
    const modalConfirm = document.getElementById('modalConfirmBtn');
    const modalSubtitle = document.getElementById('modalSubtitle');

    /* Use shared image-upload helper for modal upload */
    const modalUploader = initImageUpload({
        areaId:      'modalUploadArea',
        inputId:     'modalFileInput',
        promptId:    'modalUploadPrompt',
        previewId:   'modalImagePreview',
        imgId:       'modalPreviewImg',
        removeId:    'modalRemoveImage',
        errorId:     'modalImageError',
        requiredMsg: 'A proof image is required.',
        onSelect:    () => { modalConfirm.disabled = false; },
        onRemove:    () => { modalConfirm.disabled = true; }
    });

    function openModal(newStatus) {
        modalUploader.reset();
        modalConfirm.disabled = true;

        const label = newStatus === 'IN_PROGRESS' ? 'In Progress' : 'Resolved';
        modalSubtitle.textContent =
            `Upload a photo confirming this issue has been moved to "${label}". Required.`;

        modal.hidden = false;
        modal.focus();
    }

    function closeModal() {
        modal.hidden = true;
        pendingCard = pendingNewStatus = pendingOldStatus = pendingBody = pendingAfter = null;
    }

    modalClose.addEventListener('click', closeModal);
    modalCancel.addEventListener('click', closeModal);
    modal.addEventListener('click', e => { if (e.target === modal) closeModal(); });
    document.addEventListener('keydown', e => { if (e.key === 'Escape' && !modal.hidden) closeModal(); });

    /* Confirm button */
    modalConfirm.addEventListener('click', async () => {
        if (!modalUploader.hasFile()) {
            modalUploader.setError('Please attach a proof image before confirming.');
            return;
        }

        modalConfirm.disabled = true;
        modalConfirm.textContent = 'Saving…';

        const card = pendingCard;
        const newStatus = pendingNewStatus;
        const oldStatus = pendingOldStatus;
        const body = pendingBody;
        const after = pendingAfter;
        const file = modalUploader.getFile();

        closeModal();
        await commitMove(card, body, after, oldStatus, newStatus, file);
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
        card.setAttribute('data-status', newStatus);
        updateCounts();

        const id = card.dataset.id;
        if (!id) {
            showToast(`Moved to ${newStatus.replace('_', ' ')}`, 'success');
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
                showToast(`Issue #${id} → ${newStatus.replace('_', ' ')}`, 'success');
            } else {
                showToast(`Failed to update #${id}. Reverted.`, 'error');
                revertCard(card, oldStatus);
            }
        } catch {
            showToast('Connection failed. Change reverted.', 'error');
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
            card.setAttribute('data-status', oldStatus);
            oldStatus === 'RESOLVED'
                ? card.classList.add('sit-kanban-card--resolved')
                : card.classList.remove('sit-kanban-card--resolved');
            updateCounts();
        }
    }

})();