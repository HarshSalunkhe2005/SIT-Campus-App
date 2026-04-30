/**
 * SIT Campus App - Global API Wrapper
 * This file connects the frontend (port 5500) to the Spring Boot backend (port 8080).
 * It automatically attaches the JWT token to every request.
 */

const API_BASE_URL = 'http://localhost:8080';

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
                localStorage.removeItem('jwt_token');
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
