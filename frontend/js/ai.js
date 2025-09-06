document.addEventListener('DOMContentLoaded', () => {
    // --- ELEMENT SELECTORS ---
    const themeToggle = document.getElementById('theme-toggle');
    const menuToggle = document.getElementById('menu-toggle');
    const sidebar = document.getElementById('sidebar');
    const chatMessages = document.getElementById('chat-messages');
    const chatInput = document.getElementById('chat-input');
    const sendBtn = document.getElementById('send-btn');
    const newChatBtn = document.getElementById('new-chat-btn');
    const chatHistoryContainer = document.getElementById('chat-history');

    // --- PLANT SELECTOR ELEMENTS ---
    const plantSelectorBtn = document.getElementById('plant-selector-btn');
    const plantSelectorPanel = document.getElementById('plant-selector-panel');
    const plantGrid = document.getElementById('plant-grid');
    const plantSearchInput = document.getElementById('plant-search-input');
    const prevPageBtn = document.getElementById('prev-page-btn');
    const nextPageBtn = document.getElementById('next-page-btn');
    const pageInfo = document.getElementById('page-info');

    // --- STATE MANAGEMENT ---
    let conversations = [];
    let activeConversationId = null;
    let plantData = [];
    let plantPagination = { currentPage: 1, itemsPerPage: 4, searchTerm: '' };

    // --- THEME MANAGEMENT ---
    function applyTheme(theme) {
        document.documentElement.setAttribute('data-theme', theme);
        themeToggle.innerHTML = theme === 'dark' ? '<i class="fas fa-sun"></i>' : '<i class="fas fa-moon"></i>';
        localStorage.setItem('smartfarm-theme', theme);
    }
    themeToggle.addEventListener('click', () => {
        const newTheme = document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
        applyTheme(newTheme);
    });

    // --- RESPONSIVE SIDEBAR ---
    menuToggle.addEventListener('click', () => sidebar.classList.toggle('open'));
    document.addEventListener('click', (e) => {
        if (!sidebar.contains(e.target) && !menuToggle.contains(e.target) && sidebar.classList.contains('open')) {
            sidebar.classList.remove('open');
        }
    });

    // --- CHAT HISTORY & CONVERSATION LOGIC ---
    function renderChatHistory() {
        chatHistoryContainer.innerHTML = '';
        conversations.forEach(convo => {
            const historyItem = document.createElement('div');
            historyItem.className = 'history-item';
            historyItem.dataset.id = convo.id;
            if (convo.id === activeConversationId) {
                historyItem.classList.add('active');
            }
            historyItem.innerHTML = `
                        <span>${convo.title}</span>
                        <button class="delete-chat-btn" data-id="${convo.id}"><i class="fas fa-trash"></i></button>
                    `;
            chatHistoryContainer.appendChild(historyItem);
        });
    }

    function switchConversation(id) {
        activeConversationId = id;
        renderChatHistory();
        renderMessages();
    }

    function startNewConversation() {
        const newConvo = {
            id: Date.now(),
            title: 'New Conversation',
            messages: []
        };
        conversations.unshift(newConvo);
        switchConversation(newConvo.id);
    }

    function deleteConversation(id) {
        conversations = conversations.filter(c => c.id !== id);
        if (activeConversationId === id) {
            activeConversationId = conversations.length > 0 ? conversations[0].id : null;
        }
        renderChatHistory();
        renderMessages();
    }

    chatHistoryContainer.addEventListener('click', (e) => {
        const historyItem = e.target.closest('.history-item');
        const deleteBtn = e.target.closest('.delete-chat-btn');

        if (deleteBtn) {
            e.stopPropagation();
            const id = parseInt(deleteBtn.dataset.id);
            deleteConversation(id);
        } else if (historyItem) {
            const id = parseInt(historyItem.dataset.id);
            switchConversation(id);
        }
    });

    newChatBtn.addEventListener('click', startNewConversation);

    // --- MESSAGE HANDLING ---
    function renderMessages() {
        chatMessages.innerHTML = '';
        const activeConvo = conversations.find(c => c.id === activeConversationId);

        if (!activeConvo || activeConvo.messages.length === 0) {
            chatMessages.innerHTML = `<div class="welcome-message"><h1>Hello, User</h1><p style="color: var(--text-secondary)">How can I help with your farm today?</p></div>`;
            return;
        }

        activeConvo.messages.forEach(msg => {
            addMessageToDOM(msg.content, msg.sender);
        });
    }

    function addMessageToDOM(content, sender) {
        const messageEl = document.createElement('div');
        messageEl.className = `message ${sender}`;
        const avatarUrl = sender === 'user' ? 'https://placehold.co/32x32/22c55e/FFF?text=U' : 'https://placehold.co/32x32/16a34a/FFF?text=AI';
        messageEl.innerHTML = `<img src="${avatarUrl}" alt="${sender} avatar" class="avatar"><div class="message-content">${content}</div>`;
        chatMessages.appendChild(messageEl);
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }

    function addMessageToConversation(content, sender) {
        if (!activeConversationId) {
            startNewConversation();
        }
        const activeConvo = conversations.find(c => c.id === activeConversationId);
        activeConvo.messages.push({ content, sender });

        // Update conversation title with first user message
        if (activeConvo.messages.length === 1 && sender === 'user') {
            activeConvo.title = content.substring(0, 25) + (content.length > 25 ? '...' : '');
            renderChatHistory();
        }

        // Remove welcome message if it exists
        const welcomeMessage = chatMessages.querySelector('.welcome-message');
        if (welcomeMessage) welcomeMessage.remove();
        addMessageToDOM(content, sender);
    }

    function sendUserMessage() {
        const messageText = chatInput.value.trim();
        if (messageText) {
            addMessageToConversation(messageText, 'user');
            chatInput.value = '';
            setTimeout(() => {
                addMessageToConversation(`I'm processing your query about: "${messageText}". Please give me a moment.`, 'ai');
            }, 1000);
        }
    }

    sendBtn.addEventListener('click', sendUserMessage);
    chatInput.addEventListener('keypress', (e) => { if (e.key === 'Enter') sendUserMessage(); });

    // --- PLANT SELECTOR LOGIC ---
    function renderPlantSelector() {
        const filteredPlants = plantData.filter(p => p.name.toLowerCase().includes(plantPagination.searchTerm.toLowerCase()));
        const totalPages = Math.ceil(filteredPlants.length / plantPagination.itemsPerPage);
        plantPagination.currentPage = Math.min(plantPagination.currentPage, totalPages) || 1;

        const start = (plantPagination.currentPage - 1) * plantPagination.itemsPerPage;
        const end = start + plantPagination.itemsPerPage;
        const paginatedPlants = filteredPlants.slice(start, end);

        plantGrid.innerHTML = '';
        paginatedPlants.forEach(plant => {
            const card = document.createElement('div');
            card.className = 'plant-card';
            card.dataset.plantName = plant.name;
            card.innerHTML = `<img src="${plant.img}" alt="${plant.name}"><h4>${plant.name}</h4><p>${plant.location}</p>`;
            plantGrid.appendChild(card);
        });

        pageInfo.textContent = `Page ${plantPagination.currentPage} of ${totalPages || 1}`;
        prevPageBtn.disabled = plantPagination.currentPage === 1;
        nextPageBtn.disabled = plantPagination.currentPage === totalPages || totalPages === 0;
    }

    plantSelectorBtn.addEventListener('click', (e) => { e.stopPropagation(); plantSelectorPanel.classList.toggle('open'); });
    plantSearchInput.addEventListener('input', () => { plantPagination.searchTerm = plantSearchInput.value; plantPagination.currentPage = 1; renderPlantSelector(); });
    prevPageBtn.addEventListener('click', () => { if (plantPagination.currentPage > 1) { plantPagination.currentPage--; renderPlantSelector(); } });
    nextPageBtn.addEventListener('click', () => { plantPagination.currentPage++; renderPlantSelector(); });

    plantGrid.addEventListener('click', (e) => {
        const card = e.target.closest('.plant-card');
        if (card) {
            const plantName = card.dataset.plantName;
            addMessageToConversation(`Tell me about my ${plantName}.`, 'user');
            plantSelectorPanel.classList.remove('open');
            setTimeout(() => {
                addMessageToConversation(`Of course. Analyzing the latest data for your ${plantName}. The current soil moisture is 55%, which is optimal.`, 'ai');
            }, 1000);
        }
    });

    document.addEventListener('click', (e) => {
        if (!plantSelectorPanel.contains(e.target) && !plantSelectorBtn.contains(e.target)) {
            plantSelectorPanel.classList.remove('open');
        }
    });

    // --- INITIALIZATION ---
    function initialize() {
        applyTheme(localStorage.getItem('smartfarm-theme') || 'dark');
        // Mock data for plants
        plantData = [
            { name: 'Heirloom Tomato', location: 'Zone A', img: 'https://placehold.co/80x80/ef4444/FFF?text=Tomato' },
            { name: 'Bell Pepper', location: 'Zone B', img: 'https://placehold.co/80x80/22c55e/FFF?text=Pepper' },
            { name: 'Cucumber', location: 'Greenhouse', img: 'https://placehold.co/80x80/16a34a/FFF?text=Cucumber' },
            { name: 'Strawberry', location: 'Hydroponics', img: 'https://placehold.co/80x80/dc2626/FFF?text=Berry' },
            { name: 'Lettuce', location: 'Vertical Farm', img: 'https://placehold.co/80x80/84cc16/FFF?text=Lettuce' },
            { name: 'Carrot', location: 'Field 3', img: 'https://placehold.co/80x80/f97316/FFF?text=Carrot' },
            { name: 'Basil', location: 'Herb Garden', img: 'https://placehold.co/80x80/10b981/FFF?text=Basil' },
        ];
        renderPlantSelector();
        startNewConversation();
    }

    initialize();
});