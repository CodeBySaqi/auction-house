/**
 * Chat Client - STOMP over SockJS for real-time messaging.
 * 
 * Connects to /ws endpoint, subscribes to /topic/chat/{conversationId},
 * and sends messages to /app/chat.send
 */
(function() {
    'use strict';

    const conversationId = document.getElementById('conversation-id')?.value;
    const currentUsername = document.getElementById('current-username')?.value;
    const messagesContainer = document.getElementById('chat-messages');
    const chatForm = document.getElementById('chat-form');
    const chatInput = document.getElementById('chat-input');
    const statusIndicator = document.getElementById('chat-status');

    if (!conversationId || !messagesContainer) {
        return; // Chat not available on this page
    }

    let stompClient = null;
    let reconnectAttempts = 0;
    const MAX_RECONNECT_ATTEMPTS = 5;

    /**
     * Connect to WebSocket server via SockJS + STOMP
     */
    function connect() {
        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);
        
        // Disable STOMP debug logging in production
        stompClient.debug = null;

        stompClient.connect({}, onConnected, onError);
    }

    /**
     * Called when WebSocket connection is established
     */
    function onConnected() {
        console.log('Connected to chat WebSocket');
        reconnectAttempts = 0;
        updateStatus('connected');

        // Subscribe to conversation topic
        stompClient.subscribe('/topic/chat/' + conversationId, onMessageReceived);

        // Load message history via REST
        loadMessageHistory();
    }

    /**
     * Called when connection fails
     */
    function onError(error) {
        console.error('WebSocket connection error:', error);
        updateStatus('disconnected');

        // Attempt reconnection with exponential backoff
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++;
            const delay = Math.min(1000 * Math.pow(2, reconnectAttempts), 30000);
            console.log('Reconnecting in ' + delay + 'ms (attempt ' + reconnectAttempts + '/' + MAX_RECONNECT_ATTEMPTS + ')');
            setTimeout(connect, delay);
        } else {
            showSystemMessage('Connection lost. Please refresh the page to reconnect.', 'error');
        }
    }

    /**
     * Called when a new message is received via WebSocket
     */
    function onMessageReceived(message) {
        const msg = JSON.parse(message.body);
        appendMessage(msg);
        scrollToBottom();
    }

    /**
     * Load message history via REST API
     */
    function loadMessageHistory() {
        fetch('/chat/' + conversationId + '/messages')
            .then(response => {
                if (!response.ok) throw new Error('Failed to load messages');
                return response.json();
            })
            .then(messages => {
                messagesContainer.innerHTML = '';
                if (messages.length === 0) {
                    showSystemMessage('No messages yet. Start the conversation!', 'info');
                } else {
                    messages.forEach(msg => appendMessage(msg));
                }
                scrollToBottom();
            })
            .catch(error => {
                console.error('Error loading messages:', error);
                showSystemMessage('Failed to load message history.', 'error');
            });
    }

    /**
     * Send a message via WebSocket
     */
    function sendMessage(event) {
        event.preventDefault();
        const content = chatInput.value.trim();

        if (!content || !stompClient || !stompClient.connected) {
            return;
        }

        const request = {
            conversationId: parseInt(conversationId),
            content: content
        };

        stompClient.send('/app/chat.send', {}, JSON.stringify(request));
        chatInput.value = '';
        chatInput.focus();
    }

    /**
     * Append a message to the chat UI
     */
    function appendMessage(msg) {
        // Remove any system messages
        const systemMsg = messagesContainer.querySelector('.system-message');
        if (systemMsg) systemMsg.remove();

        const isMine = msg.senderUsername === currentUsername;
        const messageDiv = document.createElement('div');
        messageDiv.className = 'flex ' + (isMine ? 'justify-end' : 'justify-start');

        const time = formatTime(msg.timestamp);
        
        messageDiv.innerHTML = 
            '<div class="max-w-xs lg:max-w-md px-4 py-3 rounded-2xl ' + 
            (isMine ? 'bg-g-blue text-white rounded-br-md' : 'bg-white border border-g-border text-g-text rounded-bl-md') + '">' +
                (isMine ? '' : '<p class="text-xs font-medium text-g-blue mb-1">' + escapeHtml(msg.senderUsername) + '</p>') +
                '<p class="text-sm whitespace-pre-wrap break-words">' + escapeHtml(msg.content) + '</p>' +
                '<p class="text-xs mt-1 ' + (isMine ? 'text-white/70' : 'text-g-text-secondary') + ' text-right">' + time + '</p>' +
            '</div>';

        messagesContainer.appendChild(messageDiv);
    }

    /**
     * Show a system message (e.g., connection status)
     */
    function showSystemMessage(text, type) {
        const existing = messagesContainer.querySelector('.system-message');
        if (existing) existing.remove();

        const div = document.createElement('div');
        div.className = 'system-message flex justify-center';
        
        const colorClass = type === 'error' ? 'text-g-red bg-red-50 border-red-200' : 'text-g-text-secondary bg-gray-50 border-gray-200';
        
        div.innerHTML = 
            '<div class="px-4 py-2 rounded-lg border text-xs ' + colorClass + '">' +
                escapeHtml(text) +
            '</div>';

        messagesContainer.appendChild(div);
    }

    /**
     * Update connection status indicator
     */
    function updateStatus(status) {
        if (!statusIndicator) return;

        if (status === 'connected') {
            statusIndicator.innerHTML = '<span class="w-2 h-2 bg-green-400 rounded-full"></span><span class="text-xs">Connected</span>';
        } else {
            statusIndicator.innerHTML = '<span class="w-2 h-2 bg-red-400 rounded-full"></span><span class="text-xs">Disconnected</span>';
        }
    }

    /**
     * Scroll messages container to bottom
     */
    function scrollToBottom() {
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    /**
     * Format timestamp for display
     */
    function formatTime(timestamp) {
        if (!timestamp) return '';
        const date = new Date(timestamp);
        const now = new Date();
        const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
        const msgDate = new Date(date.getFullYear(), date.getMonth(), date.getDate());

        const timeStr = date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

        if (msgDate.getTime() === today.getTime()) {
            return timeStr;
        } else {
            const daysDiff = Math.floor((today - msgDate) / (1000 * 60 * 60 * 24));
            if (daysDiff === 1) {
                return 'Yesterday ' + timeStr;
            } else if (daysDiff < 7) {
                return date.toLocaleDateString([], { weekday: 'short' }) + ' ' + timeStr;
            } else {
                return date.toLocaleDateString([], { month: 'short', day: 'numeric' }) + ' ' + timeStr;
            }
        }
    }

    /**
     * Escape HTML to prevent XSS
     */
    function escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.appendChild(document.createTextNode(text));
        return div.innerHTML;
    }

    // Event listeners
    if (chatForm) {
        chatForm.addEventListener('submit', sendMessage);
    }

    // Connect on page load
    connect();

})();
