document.addEventListener('DOMContentLoaded', () => {
    // Elements
    const themeToggle = document.getElementById('theme-toggle');
    const menuToggle = document.getElementById('menu-toggle');
    const sidebar = document.getElementById('sidebar');
    const chatMessages = document.getElementById('chat-messages');
    const chatInput = document.getElementById('chat-input');
    const sendBtn = document.getElementById('send-btn');
    const newChatBtn = document.getElementById('new-chat-btn');
    const chatHistoryContainer = document.getElementById('chat-history');

    const plantSelectorBtn = document.getElementById('plant-selector-btn');
    const plantSelectorPanel = document.getElementById('plant-selector-panel');
    const plantGrid = document.getElementById('plant-grid');
    const plantSearchInput = document.getElementById('plant-search-input');
    const prevPageBtn = document.getElementById('prev-page-btn');
    const nextPageBtn = document.getElementById('next-page-btn');
    const pageInfo = document.getElementById('page-info');

    // State
    const API_BASE = 'http://localhost:8080';
    let accessToken = null;
    let conversations = [];
    let activeConversationId = null;
    let plantPagination = { currentPage: 1, itemsPerPage: 6, searchTerm: '' };
    let plantsLookup = [];     // {id, name, species}
    let selectedPlant = null;  // {id, name}

    // Theme
    function applyTheme(theme) {
        document.documentElement.setAttribute('data-theme', theme);
        themeToggle.innerHTML = theme === 'dark' ? '<i class="fas fa-sun"></i>' : '<i class="fas fa-moon"></i>';
        localStorage.setItem('smartfarm-theme', theme);
    }
    themeToggle.addEventListener('click', () => {
        const newTheme = document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
        applyTheme(newTheme);
    });
    applyTheme(localStorage.getItem('smartfarm-theme') || 'dark');

    // Sidebar
    menuToggle.addEventListener('click', () => sidebar.classList.toggle('open'));
    document.addEventListener('click', (e) => {
        if (!sidebar.contains(e.target) && !menuToggle.contains(e.target) && sidebar.classList.contains('open')) {
            sidebar.classList.remove('open');
        }
    });

    // Auth helpers
    async function refreshToken() {
        const res = await fetch(`${API_BASE}/auth/refresh`, { method: 'POST', credentials: 'include' });
        if (!res.ok) throw new Error('Not authenticated');
        const data = await res.json();
        accessToken = data.accessToken;
    }
    async function api(path, options = {}, retry = true) {
        await refreshToken();
        const headers = options.headers ? {...options.headers} : {};
        headers['Authorization'] = 'Bearer ' + accessToken;
        const res = await fetch(API_BASE + path, { ...options, headers, credentials: 'include' });
        if (res.status === 401 && retry) {
            await refreshToken();
            headers['Authorization'] = 'Bearer ' + accessToken;
            return fetch(API_BASE + path, { ...options, headers, credentials: 'include' });
        }
        return res;
    }

    // Conversations
    function renderChatHistory() {
        chatHistoryContainer.innerHTML = '';
        conversations.forEach(convo => {
            const el = document.createElement('div');
            el.className = 'history-item' + (convo.id === activeConversationId ? ' active' : '');
            el.dataset.id = convo.id;
            el.innerHTML = `
        <span>${convo.title}</span>
        <button class="delete-chat-btn" data-id="${convo.id}"><i class="fas fa-trash"></i></button>
      `;
            chatHistoryContainer.appendChild(el);
        });
    }
    function switchConversation(id) {
        activeConversationId = id;
        renderChatHistory();
        renderMessages();
    }
    function startNewConversation() {
        const c = { id: Date.now(), title: 'New Conversation', messages: [] };
        conversations.unshift(c);
        switchConversation(c.id);
    }
    function deleteConversation(id) {
        conversations = conversations.filter(c => c.id !== id);
        if (activeConversationId === id) activeConversationId = conversations.length ? conversations[0].id : null;
        renderChatHistory();
        renderMessages();
    }
    chatHistoryContainer.addEventListener('click', (e) => {
        const item = e.target.closest('.history-item');
        const del = e.target.closest('.delete-chat-btn');
        if (del) {
            e.stopPropagation();
            deleteConversation(Number(del.dataset.id));
        } else if (item) {
            switchConversation(Number(item.dataset.id));
        }
    });
    newChatBtn.addEventListener('click', startNewConversation);

    // Messages
    function renderMessages() {
        chatMessages.innerHTML = '';
        const convo = conversations.find(c => c.id === activeConversationId);
        if (!convo || !convo.messages.length) {
            chatMessages.innerHTML = `<div class="welcome-message"><h1>Hello, User</h1><p style="color: var(--text-secondary)">How can I help with your farm today?</p></div>`;
            return;
        }
        convo.messages.forEach(m => addMessageToDOM(m.content, m.sender));
    }
    function addMessageToDOM(content, sender) {
        const el = document.createElement('div');
        el.className = `message ${sender}`;
        const avatarUrl = sender === 'user' ? 'https://placehold.co/32x32/22c55e/FFF?text=U' : 'https://placehold.co/32x32/16a34a/FFF?text=AI';
        el.innerHTML = `<img src="${avatarUrl}" class="avatar"><div class="message-content">${content}</div>`;
        chatMessages.appendChild(el);
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }
    function addMessage(content, sender) {
        if (!activeConversationId) startNewConversation();
        const convo = conversations.find(c => c.id === activeConversationId);
        convo.messages.push({ content, sender });
        if (convo.messages.length === 1 && sender === 'user') {
            convo.title = content.substring(0, 25) + (content.length > 25 ? '...' : '');
            renderChatHistory();
        }
        const welcome = chatMessages.querySelector('.welcome-message');
        if (welcome) welcome.remove();
        addMessageToDOM(content, sender);
    }

    async function sendUserMessage() {
        const text = chatInput.value.trim();
        if (!text) return;

        // Augment the question with plant name for user readability; pass plantId to backend
        const displayText = selectedPlant ? `${text} (Plant: ${selectedPlant.name})` : text;

        addMessage(displayText, 'user');
        chatInput.value = '';

        // Call backend
        try {
            const res = await api('/ai/query', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    question: text,
                    plantId: selectedPlant ? selectedPlant.id : null,
                    sqlOnly: false
                })
            });
            if (!res.ok) {
                addMessage("I couldn't process that request. Please try again.", 'ai');
                return;
            }
            const data = await res.json();

            // Optional: show SQL for debug (toggle/comment out as needed)
            // addMessage(`<pre><code>${data.sql}</code></pre>`, 'ai');

            addMessage(data.answer || 'No answer available.', 'ai');

        } catch (e) {
            console.error(e);
            addMessage('Something went wrong while talking to the AI.', 'ai');
        }
    }

    sendBtn.addEventListener('click', sendUserMessage);
    chatInput.addEventListener('keypress', (e) => { if (e.key === 'Enter') sendUserMessage(); });

    // Plant selector
    function paginate(arr) {
        const filtered = arr.filter(p => p.name.toLowerCase().includes(plantPagination.searchTerm.toLowerCase()));
        const totalPages = Math.ceil(filtered.length / plantPagination.itemsPerPage) || 1;
        plantPagination.currentPage = Math.min(plantPagination.currentPage, totalPages);
        const start = (plantPagination.currentPage - 1) * plantPagination.itemsPerPage;
        return { list: filtered.slice(start, start + plantPagination.itemsPerPage), totalPages };
    }
    function renderPlantSelector() {
        const { list, totalPages } = paginate(plantsLookup);
        plantGrid.innerHTML = '';
        list.forEach(p => {
            const card = document.createElement('div');
            card.className = 'plant-card';
            card.dataset.plantId = p.id;
            card.dataset.plantName = p.name;
            card.innerHTML = `<img src="https://placehold.co/80x80/22c55e/FFF?text=${p.name.charAt(0).toUpperCase()}"><h4>${p.name}</h4><p>${p.species || ''}</p>`;
            plantGrid.appendChild(card);
        });
        pageInfo.textContent = `Page ${plantPagination.currentPage} of ${totalPages}`;
        prevPageBtn.disabled = plantPagination.currentPage === 1;
        nextPageBtn.disabled = plantPagination.currentPage === totalPages;
    }

    plantSelectorBtn.addEventListener('click', async (e) => {
        e.stopPropagation();
        plantSelectorPanel.classList.toggle('open');
        if (plantSelectorPanel.classList.contains('open') && !plantsLookup.length) {
            await searchPlants(''); // initial load
        }
    });
    plantSearchInput.addEventListener('input', async () => {
        plantPagination.searchTerm = plantSearchInput.value;
        plantPagination.currentPage = 1;
        await searchPlants(plantPagination.searchTerm);
    });
    prevPageBtn.addEventListener('click', () => { if (plantPagination.currentPage > 1) { plantPagination.currentPage--; renderPlantSelector(); } });
    nextPageBtn.addEventListener('click', () => { plantPagination.currentPage++; renderPlantSelector(); });

    plantGrid.addEventListener('click', (e) => {
        const card = e.target.closest('.plant-card');
        if (!card) return;
        selectedPlant = { id: Number(card.dataset.plantId), name: card.dataset.plantName };
        plantSelectorPanel.classList.remove('open');
        addMessage(`Selected plant: ${selectedPlant.name}`, 'ai');
    });

    document.addEventListener('click', (e) => {
        if (!plantSelectorPanel.contains(e.target) && !plantSelectorBtn.contains(e.target)) {
            plantSelectorPanel.classList.remove('open');
        }
    });

    async function searchPlants(q) {
        try {
            const res = await api(`/plants/lookup?q=${encodeURIComponent(q || '')}`);
            if (!res.ok) return;
            plantsLookup = await res.json(); // [{id, name, species}]
            renderPlantSelector();
        } catch (e) {
            console.error('Plant lookup error', e);
        }
    }

    // Init
    (async function init() {
        await refreshToken();
        applyTheme(localStorage.getItem('smartfarm-theme') || 'dark');
        startNewConversation();
    })();
});