/**
 * SIT Campus App - Login Logic
 * Depends on: shared/api.js, shared/toast.js
 */

document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.querySelector('.sit-auth-form');
    if (!loginForm) return;

    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault(); // Prevent standard HTML form submission

        const email = document.getElementById('email').value.trim();
        const password = document.getElementById('password').value.trim();
        const submitBtn = loginForm.querySelector('button[type="submit"]');

        if (!email || !password) {
            showToast('Email and password are required', 'error');
            return;
        }

        // Visual feedback
        const originalText = submitBtn.textContent;
        submitBtn.textContent = 'Authenticating...';
        submitBtn.disabled = true;

        try {
            // Call the REST API using our new api wrapper
            const response = await api.post('/auth/login', { email, password });
            
            // Save the JWT token
            localStorage.setItem('jwt_token', response.token);
            
            // Save user details for the UI
            const nameToSave = response.departmentName || response.name || response.first_name || 'User';
            const idToSave = response.departmentId || response.userId || '';
            
            localStorage.setItem('user_name', nameToSave);
            localStorage.setItem('user_role', response.role);
            localStorage.setItem('user_id', idToSave);

            showToast('Login successful! Redirecting...', 'success');

            // Redirect based on role
            setTimeout(() => {
                if (response.role === 'ADMIN') {
                    window.location.href = '../admin/hub.html';
                } else if (response.role === 'DEPARTMENT') {
                    window.location.href = '../dept/kanban.html';
                } else {
                    window.location.href = '../student/dashboard.html';
                }
            }, 1000);

        } catch (error) {
            // Revert button and show error
            submitBtn.textContent = originalText;
            submitBtn.disabled = false;
            showToast(error.message || 'Invalid email or password', 'error');
        }
    });
});
