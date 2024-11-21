let ws;

function initWebSocket(url) {
    ws = new WebSocket(url);

    ws.onopen = () => {
        console.log('WebSocket is open now.');
    };

    ws.onmessage = (event) => {
        try {
            const data = JSON.parse(event.data);
            if (data.alert === "ping") {
                ws.send("js-pong");
            } else if (data.alert) {
                showAlert(data.alert, data.status);
            }
        } catch (e) {
            console.error("Error parsing JSON message:", e);
        }
    };

    ws.onclose = () => {
        console.log('WebSocket is closed now.');
    };

    ws.onerror = (error) => {
        console.error('WebSocket error observed:', error);
    };
}

function initWebSocketClient() {
    const wsUrl = 'ws://localhost:8080';
    initWebSocket(wsUrl);
}

function sendAction(action, delay = null) {
    const payload = { action: action };
    if (delay !== null) {
        payload.delay = delay;
    }
    ws.send(JSON.stringify(payload));
}

function confirmAction(action) {
    if (confirm("Confirm?")) {
        sendAction(action);
    }
}

function showAlert(message, status) {
    const alertBox = document.getElementById('alertBox');
    alertBox.textContent = message;
    alertBox.style.display = 'block';
    alertBox.className = 'alert ' + status;
    setTimeout(() => {
        alertBox.style.display = 'none';
    }, 5000);
}

function openModal() {
    document.getElementById('modal').style.display = 'block';
}

function closeModal() {
    document.getElementById('modal').style.display = 'none';
}

function applyDelay() {
    const delay = document.getElementById('delayInput').value;
    sendAction('netsh-cycle', delay);
    closeModal();
}

window.onload = initWebSocketClient;
