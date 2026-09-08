const authSection = document.getElementById("authSection");

const registerSection = document.getElementById("registerSection");
const loginSection = document.getElementById("loginSection");

const registerEmail = document.getElementById("registerEmail");
const registerPassword = document.getElementById("registerPassword");
const registerButton = document.getElementById("registerButton");
const registerStatus = document.getElementById("registerStatus");

const loginEmail = document.getElementById("loginEmail");
const loginPassword = document.getElementById("loginPassword");
const loginButton = document.getElementById("loginButton");
const loginStatus = document.getElementById("loginStatus");

const showLoginButton = document.getElementById("showLoginButton");
const showRegisterButton = document.getElementById("showRegisterButton");

const agentSection = document.getElementById("agentSection");
const logoutButton = document.getElementById("logoutButton");

const audioInput = document.getElementById("audioInput");
const sendButton = document.getElementById("sendButton");

const chatMessages = document.getElementById("chatMessages");
const status = document.getElementById("status");


console.log("Voice Agent app.js loaded");


/*
 * Initial application state
 */

const existingToken = localStorage.getItem("accessToken");

if (existingToken) {

    console.log("Existing access token found.");

    showAgentSection();

} else {

    console.log("No access token found.");

    showRegisterSection();
}


/*
 * Switch to Login
 */

showLoginButton.addEventListener("click", () => {

    showLoginSection();

});


/*
 * Switch to Registration
 */

showRegisterButton.addEventListener("click", () => {

    showRegisterSection();

});


/*
 * Registration
 */

registerButton.addEventListener("click", async () => {

    const email = registerEmail.value.trim();
    const password = registerPassword.value;

    if (!email) {

        registerStatus.textContent =
            "Email is required.";

        return;
    }

    if (!password) {

        registerStatus.textContent =
            "Password is required.";

        return;
    }

    registerButton.disabled = true;

    registerStatus.textContent =
        "Creating account...";


    try {

        console.log("Sending registration request.");

        const response = await fetch(
            "/api/auth/register",
            {
                method: "POST",

                headers: {
                    "Content-Type": "application/json"
                },

                body: JSON.stringify({
                    email: email,
                    password: password
                })
            }
        );


        console.log(
            "Registration response status:",
            response.status
        );


        if (!response.ok) {

            const errorText =
                await response.text();

            throw new Error(
                "Registration failed: HTTP " +
                response.status +
                " " +
                errorText
            );
        }


        const registrationResponse =
            await response.json();

        console.log(
            "Registration successful:",
            registrationResponse
        );


        registerStatus.textContent =
            "Registration successful. Please login.";


        /*
         * Put the registered email into login form.
         */

        loginEmail.value = email;


        /*
         * Move user to login.
         */

        showLoginSection();


    } catch (error) {

        console.error(
            "Registration error:",
            error
        );

        registerStatus.textContent =
            error.message;

    } finally {

        registerButton.disabled = false;
    }

});


/*
 * Login
 */

loginButton.addEventListener("click", async () => {

    const email = loginEmail.value.trim();
    const password = loginPassword.value;


    if (!email) {

        loginStatus.textContent =
            "Email is required.";

        return;
    }


    if (!password) {

        loginStatus.textContent =
            "Password is required.";

        return;
    }


    loginButton.disabled = true;

    loginStatus.textContent =
        "Logging in...";


    try {

        console.log("Sending login request.");


        const response = await fetch(
            "/api/auth/login",
            {
                method: "POST",

                headers: {
                    "Content-Type": "application/json"
                },

                body: JSON.stringify({
                    email: email,
                    password: password
                })
            }
        );


        console.log(
            "Login response status:",
            response.status
        );


        if (!response.ok) {

            const errorText =
                await response.text();

            throw new Error(
                "Login failed: HTTP " +
                response.status +
                " " +
                errorText
            );
        }


        const loginResponse =
            await response.json();


        console.log("Login successful.");


        if (!loginResponse.accessToken) {

            throw new Error(
                "Login succeeded but no access token was returned."
            );
        }


        /*
         * Store JWT.
         */

        localStorage.setItem(
            "accessToken",
            loginResponse.accessToken
        );


        localStorage.setItem(
            "tokenType",
            loginResponse.tokenType || "Bearer"
        );


        loginStatus.textContent =
            "Login successful.";


        /*
         * Open voice agent.
         */

        showAgentSection();


    } catch (error) {

        console.error(
            "Login error:",
            error
        );

        loginStatus.textContent =
            error.message;

    } finally {

        loginButton.disabled = false;
    }

});


/*
 * Logout
 */

logoutButton.addEventListener("click", () => {

    localStorage.removeItem("accessToken");
    localStorage.removeItem("tokenType");

    console.log("User logged out.");

    showRegisterSection();

});


/*
 * Voice Agent
 */

sendButton.addEventListener("click", async () => {

    console.log("Send button clicked");


    const token =
        localStorage.getItem("accessToken");


    /*
     * JWT must exist.
     */

    if (!token) {

        console.error(
            "No access token found."
        );

        showLoginSection();

        return;
    }


    /*
     * Get selected audio file.
     */

    const file =
        audioInput.files[0];


    if (!file) {

        status.textContent =
            "Please select an audio file first.";

        return;
    }


    console.log(
        "Selected file:",
        file.name
    );


    console.log(
        "File type:",
        file.type
    );


    console.log(
        "File size:",
        file.size
    );


    /*
     * Display user message.
     */

    addMessage(
        "You",
        "Audio: " + file.name,
        "user-message"
    );


    status.textContent =
        "Sending audio to voice agent...";


    sendButton.disabled = true;


    try {

        const formData =
            new FormData();


        formData.append(
            "file",
            file
        );


        console.log(
            "Calling protected endpoint:"
        );


        console.log(
            "POST /api/final/process"
        );


        /*
         * IMPORTANT:
         *
         * Do NOT manually set Content-Type.
         *
         * Browser sets multipart/form-data
         * boundary automatically.
         */

        const response =
            await fetch(
                "/api/final/process",
                {
                    method: "POST",

                    headers: {
                        "Authorization":
                            "Bearer " + token
                    },

                    body: formData
                }
            );


        console.log(
            "Backend HTTP status:",
            response.status
        );


        /*
         * Authentication/authorization failure.
         */

        if (
            response.status === 401 ||
            response.status === 403
        ) {

            localStorage.removeItem(
                "accessToken"
            );

            localStorage.removeItem(
                "tokenType"
            );


            showLoginSection();


            throw new Error(
                "Authentication failed. Please login again."
            );
        }


        /*
         * Other backend error.
         */

        if (!response.ok) {

            const errorText =
                await response.text();


            throw new Error(
                "Backend returned HTTP " +
                response.status +
                ": " +
                errorText
            );
        }


        console.log(
            "Authenticated request successful."
        );


        /*
         * Receive MP3.
         */

        const audioBlob =
            await response.blob();


        console.log(
            "Received audio:",
            audioBlob.type,
            audioBlob.size,
            "bytes"
        );


        if (audioBlob.size === 0) {

            throw new Error(
                "Backend returned empty audio."
            );
        }


        /*
         * Display agent message.
         *
         * Currently the backend endpoint
         * returns only audio, so we cannot
         * display the actual Gemma response
         * yet.
         */

        addMessage(
            "Agent",
            "Voice response received.",
            "agent-message"
        );


        /*
         * Create audio URL.
         */

        const audioUrl =
            URL.createObjectURL(
                audioBlob
            );


        const audio =
            new Audio(audioUrl);


        audio.addEventListener(
            "ended",
            () => {

                URL.revokeObjectURL(
                    audioUrl
                );

                status.textContent =
                    "Ready";

                sendButton.disabled =
                    false;
            }
        );


        status.textContent =
            "Playing response...";


        /*
         * Play through the user's
         * default audio output/headphones.
         */

        await audio.play();


    } catch (error) {

        console.error(
            "Voice agent error:",
            error
        );


        addMessage(
            "Agent",
            "Error: " + error.message,
            "agent-message"
        );


        status.textContent =
            "Error";


        sendButton.disabled =
            false;
    }

});


/*
 * Add chat message
 */

function addMessage(
    label,
    content,
    messageClass
) {

    const message =
        document.createElement("div");


    message.className =
        "message " + messageClass;


    const messageLabel =
        document.createElement("div");


    messageLabel.className =
        "message-label";


    messageLabel.textContent =
        label;


    const messageContent =
        document.createElement("div");


    messageContent.className =
        "message-content";


    messageContent.textContent =
        content;


    message.appendChild(
        messageLabel
    );


    message.appendChild(
        messageContent
    );


    chatMessages.appendChild(
        message
    );


    chatMessages.scrollTop =
        chatMessages.scrollHeight;
}


/*
 * Show registration
 */

function showRegisterSection() {

    authSection.hidden = false;

    registerSection.hidden = false;

    loginSection.hidden = true;

    agentSection.hidden = true;

}


/*
 * Show login
 */

function showLoginSection() {

    authSection.hidden = false;

    registerSection.hidden = true;

    loginSection.hidden = false;

    agentSection.hidden = true;

}


/*
 * Show voice agent
 */

function showAgentSection() {

    authSection.hidden = true;

    registerSection.hidden = true;

    loginSection.hidden = true;

    agentSection.hidden = false;

    status.textContent =
        "Ready";
}