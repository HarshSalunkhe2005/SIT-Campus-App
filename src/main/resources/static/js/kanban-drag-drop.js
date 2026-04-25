/**
 * SIT Campus App — Kanban Drag & Drop
 * Handles HTML5 drag-and-drop between columns,
 * fires PATCH /dept/issue/{id}/status on drop,
 * and keeps column counts in sync.
 */

(function () {
    'use strict';

    const STATUSES   = ['PENDING', 'IN_PROGRESS', 'RESOLVED'];
    const board      = document.getElementById('kanbanBoard');
    if (!board) return;

    let dragCard     = null;
    let placeholder  = null;

    /* ── Helpers ── */
    function showToast(msg, type = 'success') {
        const t = document.getElementById('sit-toast');
        if (!t) return;
        t.textContent = msg;
        t.className   = `sit-toast sit-toast--${type} sit-toast--show`;
        clearTimeout(t._timer);
        t._timer = setTimeout(() => t.classList.remove('sit-toast--show'), 3500);
    }

    function updateCounts() {
        STATUSES.forEach(status => {
            const body  = document.getElementById('body-' + status);
            const badge = document.getElementById('count-' + status);
            if (body && badge) {
                badge.textContent = body.querySelectorAll('.sit-kanban-card').length;
            }
        });
    }

    function makePlaceholder() {
        const el = document.createElement('div');
        el.className = 'sit-kanban-placeholder';
        return el;
    }

    function getColumnBody(el) {
        return el.closest('.sit-kanban-col__body');
    }

    /* ── Attach drag events to a card ── */
    function attachDrag(card) {
        card.addEventListener('dragstart', onDragStart);
        card.addEventListener('dragend',   onDragEnd);
    }

    /* ── Init: attach to all existing cards ── */
    document.querySelectorAll('.sit-kanban-card').forEach(attachDrag);
    updateCounts();

    /* ── Column drop zones ── */
    document.querySelectorAll('.sit-kanban-col__body').forEach(body => {
        body.addEventListener('dragover',  onDragOver);
        body.addEventListener('dragenter', onDragEnter);
        body.addEventListener('dragleave', onDragLeave);
        body.addEventListener('drop',      onDrop);
    });

    /* ────────── EVENT HANDLERS ────────── */

    function onDragStart(e) {
        dragCard = this;
        dragCard.classList.add('sit-dragging');
        e.dataTransfer.effectAllowed = 'move';
        e.dataTransfer.setData('text/plain', dragCard.dataset.id);
        placeholder = makePlaceholder();
    }

    function onDragEnd() {
        dragCard.classList.remove('sit-dragging');
        placeholder && placeholder.remove();
        placeholder = null;
        dragCard    = null;
        document.querySelectorAll('.sit-kanban-col').forEach(c => c.classList.remove('sit-drop-over'));
    }

    function onDragEnter(e) {
        e.preventDefault();
        this.closest('.sit-kanban-col').classList.add('sit-drop-over');
    }

    function onDragLeave(e) {
        /* only fire if leaving the column entirely */
        if (!this.contains(e.relatedTarget)) {
            this.closest('.sit-kanban-col').classList.remove('sit-drop-over');
            placeholder && placeholder.remove();
        }
    }

    function onDragOver(e) {
        e.preventDefault();
        e.dataTransfer.dropEffect = 'move';
        if (!dragCard) return;

        const body      = this;
        const afterCard = getDragAfterCard(body, e.clientY);

        if (afterCard) {
            body.insertBefore(placeholder, afterCard);
        } else {
            body.appendChild(placeholder);
        }
    }

    async function onDrop(e) {
        e.preventDefault();
        if (!dragCard) return;

        const body      = this;
        const col       = body.closest('.sit-kanban-col');
        const newStatus = col.dataset.status;
        const oldStatus = dragCard.dataset.status;

        col.classList.remove('sit-drop-over');
        placeholder && placeholder.remove();

        /* Same column — no-op */
        if (newStatus === oldStatus) return;

        /* Move card visually */
        const afterCard = getDragAfterCard(body, e.clientY);
        if (afterCard) {
            body.insertBefore(dragCard, afterCard);
        } else {
            body.appendChild(dragCard);
        }

        /* Update resolved style */
        if (newStatus === 'RESOLVED') {
            dragCard.classList.add('sit-kanban-card--resolved');
        } else {
            dragCard.classList.remove('sit-kanban-card--resolved');
        }

        dragCard.dataset.status = newStatus;
        updateCounts();

        /* ── API call ── */
        const id = dragCard.dataset.id;
        if (id && !id.startsWith('demo')) {
            try {
                const res = await fetch(`/dept/issue/${id}/status`, {
                    method:  'PATCH',
                    headers: { 'Content-Type': 'application/json' },
                    body:    JSON.stringify({ status: newStatus })
                });
                if (res.ok) {
                    showToast(`✅ Issue #${id} → ${newStatus.replace('_', ' ')}`, 'success');
                } else {
                    showToast(`❌ Failed to update #${id}. Reverted.`, 'error');
                    revertCard(dragCard, oldStatus);
                }
            } catch {
                showToast('❌ Connection failed. Change reverted.', 'error');
                revertCard(dragCard, oldStatus);
            }
        } else {
            /* Demo card: just show toast */
            showToast(`🔄 Moved to ${newStatus.replace('_', ' ')}`, 'success');
        }
    }

    /* ── Utility: find the card the mouse is above ── */
    function getDragAfterCard(container, y) {
        const cards = [...container.querySelectorAll('.sit-kanban-card:not(.sit-dragging)')];
        return cards.reduce((closest, card) => {
            const box    = card.getBoundingClientRect();
            const offset = y - box.top - box.height / 2;
            if (offset < 0 && offset > closest.offset) {
                return { offset, element: card };
            }
            return closest;
        }, { offset: Number.NEGATIVE_INFINITY }).element;
    }

    /* ── Revert card to old column on API failure ── */
    function revertCard(card, oldStatus) {
        const oldBody = document.getElementById('body-' + oldStatus);
        if (oldBody) {
            oldBody.appendChild(card);
            card.dataset.status = oldStatus;
            if (oldStatus === 'RESOLVED') {
                card.classList.add('sit-kanban-card--resolved');
            } else {
                card.classList.remove('sit-kanban-card--resolved');
            }
            updateCounts();
        }
    }

})();