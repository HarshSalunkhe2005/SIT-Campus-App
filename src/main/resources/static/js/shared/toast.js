/**
 * SIT Campus App — Shared Toast Notification
 * Include this before any page-specific JS.
 * Usage: showToast('Message', 'success' | 'error')
 */
function showToast(msg, type = 'success') {
    const t = document.getElementById('sit-toast');
    if (!t) return;
    t.textContent = msg;
    t.className = `sit-toast sit-toast--${type} sit-toast--show`;
    clearTimeout(t._timer);
    t._timer = setTimeout(() => t.classList.remove('sit-toast--show'), 3500);
}
