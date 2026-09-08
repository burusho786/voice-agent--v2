/**
 * Voice Agent — Authentication Logic
 *
 * Handles:
 * - Login form submission
 * - Registration form submission
 * - Toggle between login and register views
 * - JWT token storage in localStorage
 * - Error and success message display
 *
 * API Contract:
 * - POST /api/auth/login → { accessToken, tokenType, expiresIn }
 * - POST /api/auth/register → registration response
 */

(function () {
    'use strict';

    // ------------------------------------------------------------------
    // DOM Element References
    // ------------------------------------------------------------------

    const loginForm = document.getElementById('loginForm');
    const registerForm = document.getElementById('registerForm');
    const toggleLink = document.getElementById('toggleLink');
    const toggleText = document.getElementById('toggleText');
    const errorMessage = document.getElementById('errorMessage');
    const successMessage = document.getElementById('successMessage');

    const loginEmail = document.getElementById('loginEmail');
    const loginPassword = document.getElementById('loginPassword');

    const registerName = document.getElementById('registerName');
    const registerEmail = document.getElementById('registerEmail');
    const registerPassword = document.getElementById('registerPassword');

    // ------------------------------------------------------------------
    // Configuration
    // ------------------------------------------------------------------

    const API_BASE_URL = window.location.origin;
    const JWT_STORAGE_KEY = 'jwt_token';
    const TOKEN_TYPE_STORAGE_KEY = 'jwt_token_type';

    // ------------------------------------------------------------------
    // Utility Functions
    // ------------------------------------------------------------------

    function showError(message) {
        errorMessage.textContent = message;
        errorMessage.style.display = 'block';
        successMessage.style.display = 'none';
    }

    function showSuccess(message) {
        successMessage.textContent = message;
        successMessage.style.display = 'block';
        errorMessage.style.display = 'none';
    }

    function hideMessages() {
        errorMessage.style.display = 'none';
        successMessage.style.display = 'none';
    }

    function setButtonLoading(button, isLoading) {
        if (isLoading) {
            button.dataset.originalText = button.textContent;
            button.disabled = true;
            button.innerHTML = '<span class="spinner"></span> Please wait...';
        } else {
            button.disabled = false;
            button.textContent = button.dataset.originalText || 'Submit';
        }
    }

    function storeAuthData(token, tokenType) {
        localStorage.setItem(JWT_STORAGE_KEY, token);
        localStorage.setItem(TOKEN_TYPE_STORAGE_KEY, tokenType || 'Bearer');
    }

    function getToken() {
        return localStorage.getItem(JWT_STORAGE_KEY);
    }

    function getTokenType() {
        return localStorage.getItem(TOKEN_TYPE_STORAGE_KEY) || 'Bearer';
    }

    function clearAuthData() {
        localStorage.removeItem(JWT_STORAGE_KEY);
        localStorage.removeItem(TOKEN_TYPE_STORAGE_KEY);
    }

    function redirectToHome() {
        window.location.href = '/index.html';
    }

    // ------------------------------------------------------------------
    // Toggle Between Login and Register
    // ------------------------------------------------------------------

    let isRegisterMode = false;

    function toggleMode() {
        isRegisterMode = !isRegisterMode;
        hideMessages();

        if (isRegisterMode) {
            loginForm.style.display = 'none';
            registerForm.style.display = 'flex';
            toggleText.innerHTML =
                'Already have an account? ' +
                '<a href="#" id="toggleLink">Sign in here</a>';
        } else {
            loginForm.style.display = 'flex';
            registerForm.style.display = 'none';
            toggleText.innerHTML =
                'Don\'t have an account? ' +
                '<a href="#" id="toggleLink">Register here</a>';
        }

        // Re-bind the toggle link after innerHTML change
        const newToggleLink = document.getElementById('toggleLink');
        newToggleLink.addEventListener('click', function (event) {
            event.preventDefault();
            toggleMode();
        });
    }

    // ------------------------------------------------------------------
    // Login Handler
    // ------------------------------------------------------------------

    async function handleLogin(event) {
        event.preventDefault();
        hideMessages();

        const email = loginEmail.value.trim();
        const password = loginPassword.value;

        // Basic client-side validation
        if (!email || !password) {
            showError('Please enter both email and password.');
            return;
        }

        const loginButton = loginForm.querySelector('button[type="submit"]');
        setButtonLoading(loginButton, true);

        try {
            const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    email: email,
                    password: password
                })
            });

            const data = await response.json();

            if (response.ok) {
                // Extract token from LoginResponse DTO
                // Fields: accessToken, tokenType, expiresIn
                const accessToken = data.accessToken;
                const tokenType = data.tokenType || 'Bearer';
                const expiresIn = data.expiresIn || 3600;

                if (!accessToken) {
                    throw new Error('No access token received from server');
                }

                // Store the JWT token and token type
                storeAuthData(accessToken, tokenType);

                showSuccess('Login successful! Redirecting...');

                // Redirect to the main application after a short delay
                setTimeout(redirectToHome, 500);
            } else {
                const errorMsg =
                    data.message ||
                    data.error ||
                    'Login failed. Please check your credentials.';
                showError(errorMsg);
            }

        } catch (error) {
            console.error('Login error:', error);
            showError(
                'Unable to connect to the server. ' +
                'Please try again later.'
            );
        } finally {
            setButtonLoading(loginButton, false);
        }
    }

    // ------------------------------------------------------------------
    // Registration Handler
    // ------------------------------------------------------------------

    async function handleRegister(event) {
        event.preventDefault();
        hideMessages();

        const name = registerName.value.trim();
        const email = registerEmail.value.trim();
        const password = registerPassword.value;

        // Basic client-side validation
        if (!name || !email || !password) {
            showError('Please fill in all fields.');
            return;
        }

        if (password.length < 6) {
            showError('Password must be at least 6 characters long.');
            return;
        }

        if (!isValidEmail(email)) {
            showError('Please enter a valid email address.');
            return;
        }

        const registerButton =
            registerForm.querySelector('button[type="submit"]');
        setButtonLoading(registerButton, true);

        try {
            const response = await fetch(
                `${API_BASE_URL}/api/auth/register`,
                {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        name: name,
                        email: email,
                        password: password
                    })
                }
            );

            const data = await response.json();

            if (response.ok) {
                showSuccess(
                    'Registration successful! ' +
                    'You can now sign in.'
                );

                // Clear the registration form
                registerName.value = '';
                registerEmail.value = '';
                registerPassword.value = '';

                // Switch to login mode after a short delay
                setTimeout(function () {
                    if (isRegisterMode) {
                        toggleMode();
                    }
                    // Pre-fill the login email
                    loginEmail.value = email;
                }, 1500);

            } else {
                const errorMsg =
                    data.message ||
                    data.error ||
                    'Registration failed. Please try again.';
                showError(errorMsg);
            }

        } catch (error) {
            console.error('Registration error:', error);
            showError(
                'Unable to connect to the server. ' +
                'Please try again later.'
            );
        } finally {
            setButtonLoading(registerButton, false);
        }
    }

    // ------------------------------------------------------------------
    // Validation Helpers
    // ------------------------------------------------------------------

    function isValidEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }

    // ------------------------------------------------------------------
    // Redirect if Already Authenticated
    // ------------------------------------------------------------------

    function checkExistingSession() {
        const token = getToken();

        if (token) {
            // Token exists, redirect to main app
            redirectToHome();
        }
    }

    // ------------------------------------------------------------------
    // Event Listeners
    // ------------------------------------------------------------------

    loginForm.addEventListener('submit', handleLogin);
    registerForm.addEventListener('submit', handleRegister);

    toggleLink.addEventListener('click', function (event) {
        event.preventDefault();
        toggleMode();
    });

    // ------------------------------------------------------------------
    // Initialize
    // ------------------------------------------------------------------

    checkExistingSession();
})();