/**
 * SIT Campus App - Login
 * Depends on: shared/api.js, shared/toast.js
 */

document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('loginForm');
    if (!form) return;

    const errorBox = document.getElementById('loginError');
    const submitBtn = document.getElementById('loginBtn');

    function showError(message) {
        errorBox.textContent = message;
        errorBox.hidden = false;
    }

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        errorBox.hidden = true;

        const email = document.getElementById('email').value.trim();
        const password = document.getElementById('password').value.trim();

        if (!email || !password) {
            showError('Enter your email and password.');
            return;
        }

        submitBtn.textContent = 'Logging in…';
        submitBtn.disabled = true;

        try {
            const response = await api.post('/auth/login', { email, password });

            localStorage.setItem('jwt_token', response.token);
            localStorage.setItem('user_name', response.departmentName || response.name || (response.role === 'ADMIN' ? 'Administrator' : 'User'));
            localStorage.setItem('user_role', response.role);
            localStorage.setItem('user_id', response.departmentId || '');

            const next = response.role === 'ADMIN' ? '../admin/hub.html'
                : response.role === 'DEPARTMENT' ? '../dept/kanban.html'
                : '../student/dashboard.html#report';
            window.location.href = next;
        } catch (error) {
            submitBtn.textContent = 'Log in';
            submitBtn.disabled = false;
            showError(error.message || 'Invalid email or password.');
        }
    });
});
