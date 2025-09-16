document.addEventListener('DOMContentLoaded', () => {
    const API_BASE = 'http://localhost:8080';
    let accessToken = null;

    // Refresh token before each call
    async function refreshToken() {
        const res = await fetch(API_BASE + '/auth/refresh', {
            method: 'POST',
            credentials: 'include'
        });
        if (!res.ok) throw new Error('Not authenticated');
        const data = await res.json();
        accessToken = data.accessToken;
    }

    // API wrapper: auto-refresh + retry once
    async function api(path, opts = {}) {
        await refreshToken();
        opts.credentials = 'include';
        opts.headers = {
            ...(opts.headers || {}),
            'Authorization': 'Bearer ' + accessToken
        };
        let res = await fetch(API_BASE + path, opts);
        if (res.status === 401) {
            await refreshToken();
            opts.headers['Authorization'] = 'Bearer ' + accessToken;
            res = await fetch(API_BASE + path, opts);
        }
        return res;
    }

    // Chat state
    let conversations = [];
    let activeConvoId = null;

    // Plant selector state
    const plantPanel  = document.getElementById('plant-selector-panel');
    const plantBtn    = document.getElementById('plant-selector-btn');
    const plantGrid   = document.getElementById('plant-grid');
    const plantSearch = document.getElementById('plant-search');
    const prevPlant   = document.getElementById('prev-plant');
    const nextPlant   = document.getElementById('next-plant');
    const plantInfo   = document.getElementById('plant-page-info');
    let plants = [], page = 1, pageSize = 6;

    // Theme toggle
    const themeToggle = document.getElementById('theme-toggle');
    themeToggle.onclick = () => {
        const theme = document.documentElement.getAttribute('data-theme') === 'dark'
            ? 'light' : 'dark';
        document.documentElement.setAttribute('data-theme', theme);
        localStorage.setItem('smartfarm-theme', theme);
        themeToggle.innerHTML = theme === 'dark'
            ? '<i class="fas fa-sun"></i>'
            : '<i class="fas fa-moon"></i>';
    };
    // Initialize theme
    const saved = localStorage.getItem('smartfarm-theme') || 'dark';
    document.documentElement.setAttribute('data-theme', saved);
    themeToggle.innerHTML = saved === 'dark'
        ? '<i class="fas fa-sun"></i>'
        : '<i class="fas fa-moon"></i>';

    // New conversation
    document.getElementById('new-chat-btn').onclick = () => {
        const id = Date.now();
        conversations.unshift({ id, title: 'New Conversation', messages: [] });
        switchConversation(id);
    };

    // Render chat history
    function renderChatHistory() {
        const hist = document.getElementById('chat-history');
        hist.innerHTML = '';
        conversations.forEach(c => {
            const div = document.createElement('div');
            div.className = 'history-item' + (c.id === activeConvoId ? ' active' : '');
            div.dataset.id = c.id;
            div.innerHTML = `
            <span>${c.title}</span>
            <button class="delete-chat-btn" data-id="${c.id}">
              <i class="fas fa-trash"></i>
            </button>`;
            hist.appendChild(div);
        });
    }
    // Switch convo
    function switchConversation(id) {
        activeConvoId = id;
        renderChatHistory();
        renderMessages();
    }
    // Delete convo
    document.getElementById('chat-history').onclick = e => {
        const btn = e.target.closest('.delete-chat-btn');
        if (btn) {
            const id = +btn.dataset.id;
            conversations = conversations.filter(c => c.id !== id);
            if (activeConvoId === id) {
                activeConvoId = conversations.length ? conversations[0].id : null;
            }
            renderChatHistory();
            renderMessages();
        } else {
            const item = e.target.closest('.history-item');
            if (item) switchConversation(+item.dataset.id);
        }
    };

    // Render messages
    function renderMessages() {
        const msgs = document.getElementById('chat-messages');
        msgs.innerHTML = '';
        const convo = conversations.find(c => c.id === activeConvoId);
        if (!convo || !convo.messages.length) {
            msgs.innerHTML = `
            <div class="welcome-message">
              <h1>Hello, User</h1>
              <p style="color: var(--text-secondary)">
                How can I help with your farm today?
              </p>
            </div>`;
            return;
        }
        convo.messages.forEach(m => addMessageToDOM(m.content, m.sender));
    }
    // Add to DOM
    function addMessageToDOM(text, sender) {
        const msgs = document.getElementById('chat-messages');
        const div = document.createElement('div');
        div.className = `message ${sender}`;
        div.innerHTML = `
          <img src="${
            sender==='user'
                ? 'https://placehold.co/32x32/22c55e/FFF?text=U'
                : 'https://placehold.co/32x32/16a34a/FFF?text=AI'
        }" class="avatar"/>
          <div class="message-content">${text}</div>`;
        msgs.appendChild(div);
        msgs.scrollTop = msgs.scrollHeight;
    }
    // Add message
    function addMessage(text, sender) {
        if (!activeConvoId) {
            document.getElementById('new-chat-btn').click();
        }
        const convo = conversations.find(c => c.id === activeConvoId);
        convo.messages.push({ content: text, sender });
        if (convo.messages.length === 1 && sender==='user') {
            convo.title = text.slice(0,25) + (text.length>25?'...':'');
            renderChatHistory();
        }
        const wel = document.querySelector('.welcome-message');
        if (wel) wel.remove();
        addMessageToDOM(text, sender);
    }

    // Send user text to AI
    async function sendUserMessage() {
        const inp = document.getElementById('chat-input');
        const text = inp.value.trim();
        if (!text) return;
        addMessage(text, 'user');
        inp.value = '';
        try {
            const res = await api('/ai/query', {
                method: 'POST',
                headers: {'Content-Type':'application/json'},
                body: JSON.stringify({
                    question: text,
                    plantId: null,
                    sqlOnly: false
                })
            });
            if (!res.ok) {
                addMessage("I couldn't process that request.", 'ai');
                return;
            }
            const data = await res.json();
            addMessage(data.answer || 'No answer.', 'ai');
        } catch (err) {
            console.error(err);
            addMessage('Error communicating with AI.', 'ai');
        }
    }
    document.getElementById('send-btn').onclick = sendUserMessage;
    document.getElementById('chat-input').onkeypress = e => {
        if (e.key==='Enter') sendUserMessage();
    };

    // Plant selector logic
    async function loadPlants() {
        try {
            const q = encodeURIComponent(plantSearch.value.trim());
            const res = await api(`/plants/lookup?q=${q}`);
            plants = res.ok ? await res.json() : [];
            page = 1; renderPlants();
        } catch(e){ console.error(e); }
    }
    function renderPlants() {
        const filtered = plants;
        const total = filtered.length;
        const totalPages = Math.max(1, Math.ceil(total/pageSize));
        page = Math.min(page, totalPages);
        plantGrid.innerHTML = '';

        filtered.slice((page-1)*pageSize, page*pageSize).forEach(p => {

            const filename = p.image_path ? p.image_path.split('/').pop() : null;
            const imgUrl = filename ? `http://localhost:8080/uploads/${filename}` : 'https://placehold.co/48x48';
            console.log('Image URL', imgUrl);

            const c = document.createElement('div');
            c.className = 'plant-card';
            c.innerHTML = `
            <img src="${imgUrl}" alt=""/>
            <div>${p.common_name}</div>`;
            c.onclick = () => selectPlant(p.id, p.common_name);
            plantGrid.appendChild(c);
        });
        plantInfo.textContent = `Page ${page}/${totalPages}`;
        prevPlant.disabled = page<=1;
        nextPlant.disabled = page>=totalPages;
    }
    async function selectPlant(id, name) {
        plantPanel.classList.remove('open');

        const finalName = name.toLowerCase().includes("plant") ? name : `${name} plant`;

        addMessage(`Provide all information about my ${finalName}.`, 'user');
        //addMessage(`Provide all information about my ${name} plant.`, 'user');
        try {
            const res = await api(`/plants/details/${id}`);
            if (!res.ok) {
                addMessage('Could not load details.', 'ai');
                return;
            }
            const {data,summary} = await res.json();
            const infoLines = Object.entries(data)
                .map(([k,v])=>`<b>${k}:</b> ${v}`).join('<br/>');
            addMessage(infoLines, 'ai');
            addMessage(summary, 'ai');
        } catch(e){ console.error(e); }
    }
    // Toggle
    plantBtn.onclick = async e => {
        e.stopPropagation();
        plantPanel.classList.toggle('open');
        if (plantPanel.classList.contains('open')) await loadPlants();
    };
    document.addEventListener('click', e => {
        if (!plantPanel.contains(e.target) && !plantBtn.contains(e.target)) {
            plantPanel.classList.remove('open');
        }
    });
    plantSearch.oninput = () => loadPlants();
    prevPlant.onclick = () => { if(page>1){ page--; renderPlants(); } };
    nextPlant.onclick = () => { page++; renderPlants(); };

    // Initialize
    (async() => {
        await refreshToken();
        document.getElementById('new-chat-btn').click();
    })();
});