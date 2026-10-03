/**
 * SIT Campus App - Global API Wrapper
 * This file connects the frontend (port 5500) to the Spring Boot backend (port 8080).
 * It automatically attaches the JWT token to every request.
 */

// Override before this script loads (window.API_BASE_URL = 'https://api.example.com') to point at another backend.
const API_BASE_URL = window.API_BASE_URL || 'http://localhost:8080';

/** Escapes text for safe use inside HTML (element content and quoted attribute values). */
function escapeHtml(value) {
    return String(value ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

/** Absolute URL for a path the API returned, e.g. an uploaded photo ("/uploads/x.png"). Empty when there is none. */
function uploadUrl(path) {
    return path && path.startsWith('/uploads/') ? `${API_BASE_URL}${path}` : '';
}

/** Reads the message out of a failed fetch Response ({"error": "..."} or plain text). */
async function readErrorMessage(response, fallback = 'Something went wrong. Please try again.') {
    const text = await response.text();
    try {
        const data = JSON.parse(text);
        return data.error || data.message || fallback;
    } catch (e) {
        return text || fallback;
    }
}

const api = {
    /**
     * Get the stored JWT token.
     */
    getToken() {
        return localStorage.getItem('jwt_token');
    },

    /**
     * Check if user is logged in.
     */
    isAuthenticated() {
        return !!this.getToken();
    },

    /**
     * Perform a GET request.
     */
    async get(endpoint) {
        return this.request(endpoint, { method: 'GET' });
    },

    /**
     * Perform a POST request with JSON payload.
     */
    async post(endpoint, data) {
        return this.request(endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
    },

    /**
     * Perform a PUT request with JSON payload.
     */
    async put(endpoint, data) {
        return this.request(endpoint, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
    },

    /**
     * Perform a POST with multipart form data (the browser sets the content type).
     */
    async postForm(endpoint, formData) {
        return this.request(endpoint, { method: 'POST', body: formData });
    },

    /**
     * Core fetch logic that automatically handles the token and URL.
     */
    async request(endpoint, options = {}) {
        const url = `${API_BASE_URL}${endpoint}`;
        const headers = { ...options.headers };

        // Attach JWT token if it exists
        const token = this.getToken();
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const config = {
            ...options,
            headers,
        };

        try {
            const response = await fetch(url, config);

            // Handle Unauthorized (e.g. token expired)
            if (response.status === 401 || response.status === 403) {
                ['jwt_token', 'user_name', 'user_role', 'user_id'].forEach(k => localStorage.removeItem(k));
                window.location.href = '/templates/auth/login.html';
                throw new Error('Session expired. Please log in again.');
            }

            // Check if response is empty (like a 204 No Content)
            const text = await response.text();
            if (!text) {
                return { success: response.ok };
            }

            // Attempt to parse JSON
            let data;
            try {
                data = JSON.parse(text);
            } catch (e) {
                // If it's not JSON (maybe plain text error), just return it
                data = text;
            }

            if (!response.ok) {
                const errorMsg = data.error || data.message || 'An error occurred';
                throw new Error(errorMsg);
            }

            return data;

        } catch (error) {
            console.error(`API Error on ${endpoint}:`, error);
            throw error;
        }
    }
};

// Make it globally available
window.api = api;
