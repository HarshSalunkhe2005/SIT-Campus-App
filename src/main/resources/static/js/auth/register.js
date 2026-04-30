/**
 * SIT Campus App - Student Registration & Verification Logic
 */

document.addEventListener('DOMContentLoaded', () => {

    // ----------------------------------------------------------------
    // 1. VERIFY PRN (Step 1)
    // ----------------------------------------------------------------
    const verifyForm = document.getElementById('verifyForm');
    if (verifyForm) {
        verifyForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const email = document.getElementById('email').value.trim();
            const prn = document.getElementById('prn').value.trim();
            const submitBtn = verifyForm.querySelector('button[type="submit"]');
            
            if (!email || !prn) {
                showToast('Email and PRN are required', 'error');
                return;
            }

            const originalText = submitBtn.textContent;
            submitBtn.textContent = 'Sending OTP...';
            submitBtn.disabled = true;

            try {
                // Call the auth register endpoint
                await api.post('/auth/register', { email, prn });
                
                // Save email to localStorage so step 2 knows who we are
                localStorage.setItem('pending_registration_email', email);
                
                showToast('OTP sent to your SIT email!', 'success');
                
                // Redirect to the password setup / OTP verification screen
                setTimeout(() => {
                    window.location.href = 'setup-password.html';
                }, 1000);

            } catch (error) {
                submitBtn.textContent = originalText;
                submitBtn.disabled = false;
                showToast(error.message || 'Failed to verify account', 'error');
            }
        });
    }

    // ----------------------------------------------------------------
    // 2. SETUP PASSWORD & VERIFY OTP (Step 2)
    // ----------------------------------------------------------------
    const setupForm = document.getElementById('setupForm');
    if (setupForm) {
        // Pre-fill email if we have it
        const pendingEmail = localStorage.getItem('pending_registration_email');
        if (!pendingEmail) {
            showToast('Session expired. Please restart verification.', 'error');
            setTimeout(() => { window.location.href = 'verify-prn.html'; }, 2000);
            return;
        }

        setupForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            // Collect all 6 OTP digits
            const otpInputs = document.querySelectorAll('.sit-otp-input');
            let otp = '';
            otpInputs.forEach(input => otp += input.value);

            const password = document.getElementById('password').value;
            const confirmPassword = document.getElementById('confirmPassword').value;
            const submitBtn = setupForm.querySelector('button[type="submit"]');

            if (otp.length !== 6) {
                showToast('Please enter the full 6-digit OTP', 'error');
                return;
            }
            if (password.length < 8) {
                showToast('Password must be at least 8 characters', 'error');
                return;
            }
            if (password !== confirmPassword) {
                showToast('Passwords do not match', 'error');
                return;
            }

            submitBtn.textContent = 'Verifying...';
            submitBtn.disabled = true;

            try {
                // First verify the OTP
                await api.post(`/auth/verify?email=${encodeURIComponent(pendingEmail)}&otp=${encodeURIComponent(otp)}`, {});
                
                // Then set the password
                await api.post('/auth/set-password', {
                    email: pendingEmail,
                    password: password
                });

                showToast('Account setup complete! Please log in.', 'success');
                
                // Clean up and redirect to login
                localStorage.removeItem('pending_registration_email');
                setTimeout(() => {
                    window.location.href = 'login.html';
                }, 1500);

            } catch (error) {
                submitBtn.textContent = 'Complete Setup';
                submitBtn.disabled = false;
                showToast(error.message || 'Verification failed. Incorrect OTP.', 'error');
            }
        });

        // Optional: Make OTP inputs automatically jump to the next box
        const otpBoxes = document.querySelectorAll('.sit-otp-input');
        otpBoxes.forEach((input, index) => {
            input.addEventListener('keyup', (e) => {
                if (e.key >= 0 && e.key <= 9 && index < otpBoxes.length - 1) {
                    otpBoxes[index + 1].focus();
                } else if (e.key === 'Backspace' && index > 0) {
                    otpBoxes[index - 1].focus();
                }
            });
        });
    }
});
