(function () {
    const API_BASE = 'http://localhost:8080';

    //global state
    let accessToken = null;
    let currentUserEmail = null;
    let currentFilterType = 'all'; // 'all' | 'mine' | 'search'
    let currentSearchTerm = '';
    let currentPage = 0;
    let pageSize = 10;
    let posts = []; // current page posts

    //elements
    const themeToggleBtn = document.getElementById('theme-toggle');
    const postFeed = document.getElementById('post-feed');
    const trendingTopicsContainer = document.getElementById('trending-topics-container');
    const userPostsCount = document.getElementById('user-posts-count');
    const userTotalVotes = document.getElementById('user-total-votes');
    const mainSearchInput = document.getElementById('main-search-input');
    const myPostsBtn = document.getElementById('my-posts-btn');
    const allPostsBtn = document.getElementById('all-posts-btn');
    const activeFilter = document.getElementById('active-filter');
    const filterText = document.getElementById('filter-text');
    const clearFilter = document.getElementById('clear-filter');

    const createPostBtn = document.getElementById('create-post-btn');
    const createPostModal = document.getElementById('create-post-modal');
    const viewPostModal = document.getElementById('view-post-modal');
    const postIdInput = document.getElementById('post-id-input');
    const postTitleInput = document.getElementById('post-title-input');
    const postContentInput = document.getElementById('post-content-input');
    const postTagsInput = document.getElementById('post-tags-input');
    const imageUploadInput = document.getElementById('image-upload-input');
    const imagePreview = document.getElementById('image-preview');
    const modalFormTitle = document.getElementById('modal-form-title');
    const editPostBtn = document.getElementById('edit-post-btn');
    const deletePostBtn = document.getElementById('delete-post-btn');
    const commentForm = document.getElementById('comment-form');

    // theme toggle-
    function applyTheme(theme) {
        document.documentElement.setAttribute('data-theme', theme);
        themeToggleBtn.innerHTML = theme === 'dark'
            ? '<i class="fas fa-sun"></i>'
            : '<i class="fas fa-moon"></i>';
        localStorage.setItem('smartfarm-theme', theme);
    }
    themeToggleBtn.addEventListener('click', () => {
        const newTheme = document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
        applyTheme(newTheme);
    });
    applyTheme(localStorage.getItem('smartfarm-theme') || 'dark');

    // refresh
    async function refreshToken() {
        const res = await fetch(`${API_BASE}/auth/refresh`, {
            method: 'POST',
            credentials: 'include'
        });
        if (!res.ok) throw new Error('Not authenticated');
        const data = await res.json();
        accessToken = data.accessToken;
    }

    // global method for every call
    async function api(path, options = {}, tryOnce = true) {
        await refreshToken();
        const headers = options.headers ? { ...options.headers } : {};
        headers['Authorization'] = `Bearer ${accessToken}`;
        const res = await fetch(API_BASE + path, { ...options, headers, credentials: 'include' });
        if (res.status === 401 && tryOnce) {
            await refreshToken();
            headers['Authorization'] = `Bearer ${accessToken}`;
            return fetch(API_BASE + path, { ...options, headers, credentials: 'include' });
        }
        return res;
    }

    // utils
    function openModal(m) { m.classList.add('open'); document.body.style.overflow = 'hidden'; }
    function closeModal(m) { m.classList.remove('open'); document.body.style.overflow = ''; }
    function formatDate(ts) {
        if (!ts) return '';
        const d = new Date(ts);
        const now = new Date();
        const diff = now - d;
        const days = Math.floor(diff / (1000 * 60 * 60 * 24));
        if (days === 0) return 'Today';
        if (days === 1) return 'Yesterday';
        if (days < 7) return `${days} days ago`;
        return d.toLocaleDateString();
    }

    // current user
    async function loadCurrentUser() {
        const res = await api('/auth/me');
        if (res.ok) {
            const data = await res.json();
            currentUserEmail = data.email;
        } else {
            currentUserEmail = null;
        }
    }

    // trending
    async function loadTrending() {
        const res = await api('/learn/trending');
        const data = await res.json();
        trendingTopicsContainer.innerHTML = '';
        Object.entries(data).forEach(([tag, count]) => {
            const el = document.createElement('div');
            el.className = 'topic-tag';
            el.textContent = `#${tag} (${count})`;
            el.addEventListener('click', () => {
                currentFilterType = 'search';
                currentSearchTerm = tag;
                filterText.textContent = `#${tag}`;
                activeFilter.style.display = 'block';
                loadPosts();
            });
            trendingTopicsContainer.appendChild(el);
        });
    }

    // post list
    function renderPostsList(page) {
        postFeed.innerHTML = '';
        if (!page || !page.content || page.content.length === 0) {
            postFeed.innerHTML = `
        <div style="text-align:center; padding:3rem; color:var(--text-secondary);">
          <i class="fas fa-search" style="font-size:3rem; margin-bottom:1rem; opacity:0.5;"></i>
          <h3>No posts found</h3>
          <p>Try adjusting your search or filter.</p>
        </div>`;
            return;
        }

        posts = page.content;
        console.log('upVotes', posts[0].upVotes);
        console.log('downVotes', posts[0].downVotes);
        console.log('title', posts[0].title);

        posts.forEach(post => {
            const card = document.createElement('div');
            card.className = 'post-card';
            card.innerHTML = `
        ${post.coverImageUrl ? `<img src="${post.coverImageUrl}" class="post-cover-image" alt="cover">` : ''}
        <div class="post-body">
          <div class="post-header">
            <img src="https://placehold.co/40x40/22c55e/FFF?text=${(post.authorName || 'U').charAt(0).toUpperCase()}" class="user-avatar">
            <div>
              <div class="author-name">${post.authorName || post.authorEmail}</div>
              <div style="font-size:0.8rem; color:var(--text-secondary);">${formatDate(post.createdAt)}</div>
            </div>
          </div>
          <h3 class="post-title">${post.title}</h3>
          <p class="post-content-preview">${post.content}</p>
          ${post.tags && post.tags.length ? `
            <div class="post-tags">
              ${post.tags.map(t => `<span class="post-tag">#${t}</span>`).join('')}
            </div>` : ''}
          <div class="post-footer">
            <div class="vote-section">
              <div class="vote-buttons">
                <button class="vote-btn upvote ${post.userVote === 'up' ? 'active' : ''}" data-post-id="${post.id}" data-vote="up"><i class="fas fa-arrow-up"></i></button>
                <button class="vote-btn downvote ${post.userVote === 'down' ? 'active' : ''}" data-post-id="${post.id}" data-vote="down"><i class="fas fa-arrow-down"></i></button>
              </div>
              <div class="vote-counts">
                <span class="upvote-count">+${post.upVotes}</span>
                <span class="downvote-count">-${post.downVotes}</span>
              </div>
            </div>
            <div class="post-meta">
              <div><i class="fas fa-comment"></i> <span class="comment-count" data-post-id="${post.id}">—</span></div>
            </div>
          </div>
        </div>
      `;

            // vote handlers
            card.querySelectorAll('.vote-btn').forEach(btn => {
                btn.addEventListener('click', async (e) => {
                    e.stopPropagation();
                    const postId = Number(btn.dataset.postId);
                    const voteType = btn.dataset.vote;
                    const wasActive = btn.classList.contains('active');
                    const nextVote = wasActive ? 'none' : voteType;

                    const res = await api(`/learn/posts/${postId}/vote`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ vote: nextVote })
                    });
                    const updated = await res.json();

                    // refresh page after vote
                    posts = posts.map(p => p.id === updated.id ? updated : p);
                    renderPostsList({ content: posts });
                    updateUserStatsQuick();
                });
            });

            // open modal
            card.addEventListener('click', () => openPostModal(post.id));

            postFeed.appendChild(card);
            // load comment counts
            loadCommentCount(post.id);
        });
    }

    async function loadCommentCount(postId) {
        const res = await api(`/learn/posts/${postId}/comments`);
        const data = await res.json();
        const ct = document.querySelector(`.comment-count[data-post-id="${postId}"]`);
        if (ct) ct.textContent = data.length;
    }

    async function loadPosts() {
        const params = new URLSearchParams();
        params.set('page', currentPage);
        params.set('size', pageSize);
        params.set('filter', currentFilterType === 'mine' ? 'mine' : 'all');
        if (currentFilterType === 'search' && currentSearchTerm) params.set('search', currentSearchTerm);

        const res = await api(`/learn/posts?${params.toString()}`);
        const page = await res.json();
        renderPostsList(page);
        await updateUserStats(); // accurate stats pulled from "mine"
    }

    // ===== post model =====
    async function openPostModal(postId) {
        const res = await api(`/learn/posts/${postId}`);
        const post = await res.json();

        document.getElementById('view-post-title').textContent = post.title;

        const cover = document.getElementById('view-post-cover');
        if (post.coverImageUrl) {
            cover.src = post.coverImageUrl;
            cover.style.display = 'block';
        } else {
            cover.style.display = 'none';
        }

        document.getElementById('view-post-header').innerHTML = `
      <img src="https://placehold.co/40x40/22c55e/FFF?text=${(post.authorName || 'U').charAt(0).toUpperCase()}" class="user-avatar">
      <div>
        <div class="author-name">${post.authorName || post.authorEmail}</div>
        <div style="font-size:0.8rem; color:var(--text-secondary);">${formatDate(post.createdAt)}</div>
        ${post.tags && post.tags.length ? `
          <div class="post-tags" style="margin-top:0.5rem;">
            ${post.tags.map(t => `<span class="post-tag">#${t}</span>`).join('')}
          </div>` : ''}
      </div>`;

        document.getElementById('view-post-content').innerHTML = (post.content || '').replace(/\n/g, '<br>');

        // edit/delete visibility
        const isMine = (currentUserEmail && post.authorEmail && currentUserEmail === post.authorEmail);
        editPostBtn.style.display = isMine ? 'inline-flex' : 'none';
        deletePostBtn.style.display = isMine ? 'inline-flex' : 'none';

        viewPostModal.dataset.currentPostId = post.id;
        openModal(viewPostModal);
        await renderComments(post.id);
    }

    async function renderComments(postId) {
        const res = await api(`/learn/posts/${postId}/comments`);
        const comments = await res.json();
        const list = document.getElementById('comments-list');
        list.innerHTML = '';
        if (!comments.length) {
            list.innerHTML = '<p style="text-align:center; color: var(--text-secondary); padding:2rem;">No comments yet. Be the first to comment!</p>';
            return;
        }
        comments.forEach((c, idx) => {
            const el = document.createElement('div');
            el.className = 'comment';
            el.style.animationDelay = `${idx * 0.1}s`;
            el.innerHTML = `
        <div class="comment-header">
          <img src="https://placehold.co/32x32/22c55e/FFF?text=${(c.authorName || 'U').charAt(0).toUpperCase()}" class="user-avatar" style="width:28px; height:28px;">
          <div class="comment-author">${c.authorName || c.authorEmail}</div>
        </div>
        <p>${c.text}</p>`;
            list.appendChild(el);
        });
    }

    // submit comment
    commentForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const postId = Number(viewPostModal.dataset.currentPostId);
        const input = e.currentTarget.querySelector('input');
        const text = input.value.trim();
        if (!text) return;
        await api(`/learn/posts/${postId}/comments`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ text })
        });
        input.value = '';
        await renderComments(postId);
        await loadCommentCount(postId);
    });

    // create / edit / delete
    createPostBtn.addEventListener('click', () => {
        modalFormTitle.textContent = 'Create a New Post';
        document.getElementById('create-post-form').reset();
        postIdInput.value = '';
        imagePreview.style.display = 'none';
        imageUploadInput.value = '';
        openModal(createPostModal);
    });

    imageUploadInput.addEventListener('change', () => {
        const file = imageUploadInput.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onload = (e) => { imagePreview.src = e.target.result; imagePreview.style.display = 'block'; };
            reader.readAsDataURL(file);
        }
    });

    document.getElementById('create-post-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const editingId = postIdInput.value;
        const meta = {
            title: postTitleInput.value,
            content: postContentInput.value,
            tags: postTagsInput.value
                .split(',')
                .map(t => t.trim().toLowerCase().replace(/[^a-z0-9-]/g, ''))
                .filter(Boolean)
        };
        const fd = new FormData();
        fd.append('meta', new Blob([JSON.stringify(meta)], { type: 'application/json' }));
        if (imageUploadInput.files[0]) fd.append('cover', imageUploadInput.files[0]);

        const path = editingId ? `/learn/posts/${editingId}` : '/learn/posts';
        const method = editingId ? 'PUT' : 'POST';
        await api(path, { method, body: fd });
        closeModal(createPostModal);
        await loadPosts();
        await loadTrending();
    });

    editPostBtn.addEventListener('click', async () => {
        const postId = Number(viewPostModal.dataset.currentPostId);
        const res = await api(`/learn/posts/${postId}`);
        const post = await res.json();
        closeModal(viewPostModal);
        modalFormTitle.textContent = 'Edit Your Post';
        postIdInput.value = post.id;
        postTitleInput.value = post.title;
        postContentInput.value = post.content;
        postTagsInput.value = (post.tags || []).join(', ');
        imagePreview.style.display = 'none';
        imageUploadInput.value = '';
        openModal(createPostModal);
    });

    deletePostBtn.addEventListener('click', async () => {
        const postId = Number(viewPostModal.dataset.currentPostId);
        if (!confirm('Are you sure you want to delete this post?')) return;
        await api(`/learn/posts/${postId}`, { method: 'DELETE' });
        closeModal(viewPostModal);
        await loadPosts();
        await loadTrending();
    });

    // filters & Search
    myPostsBtn.addEventListener('click', () => {
        currentFilterType = 'mine';
        currentSearchTerm = '';
        filterText.textContent = 'My Posts';
        activeFilter.style.display = 'block';
        loadPosts();
    });

    allPostsBtn.addEventListener('click', () => {
        currentFilterType = 'all';
        currentSearchTerm = '';
        activeFilter.style.display = 'none';
        loadPosts();
    });

    clearFilter.addEventListener('click', () => {
        currentFilterType = 'all';
        currentSearchTerm = '';
        activeFilter.style.display = 'none';
        mainSearchInput.value = '';
        loadPosts();
    });

    let searchDebounce;
    mainSearchInput.addEventListener('input', (e) => {
        clearTimeout(searchDebounce);
        searchDebounce = setTimeout(() => {
            const term = e.target.value.trim();
            if (term) {
                currentFilterType = 'search';
                currentSearchTerm = term;
                filterText.textContent = `"${term}"`;
                activeFilter.style.display = 'block';
            } else {
                currentFilterType = 'all';
                currentSearchTerm = '';
                activeFilter.style.display = 'none';
            }
            loadPosts();
        }, 300);
    });

    // user stats
    async function updateUserStats() {
        // Fetch only the user's posts to compute accurate totals
        const res = await api(`/learn/posts?filter=mine&page=0&size=1000`);
        if (!res.ok) {
            userPostsCount.textContent = '—';
            userTotalVotes.textContent = '—';
            return;
        }
        const minePage = await res.json();
        const mine = minePage.content || [];
        userPostsCount.textContent = minePage.totalElements ?? mine.length;
        const totalVotes = mine.reduce((sum, p) => sum + (p.upVotes - p.downVotes), 0);
        userTotalVotes.textContent = totalVotes;
    }

    // fast update based on current "posts" if we just voted
    function updateUserStatsQuick() {
        if (!currentUserEmail) return;
        const mine = posts.filter(p => p.authorEmail === currentUserEmail);
        if (mine.length) {
            userPostsCount.textContent = '—'; // leave accurate count to full refresh
            const totalVotes = mine.reduce((sum, p) => sum + (p.upVotes - p.downVotes), 0);
            userTotalVotes.textContent = totalVotes;
        }
    }

    // modal close handlers
    [createPostModal, viewPostModal].forEach(m => {
        m.addEventListener('click', (e) => {
            if (e.target.classList.contains('modal-overlay') || e.target.closest('.modal-close-btn')) {
                closeModal(m);
            }
        });
    });

    // initialize
    (async function init() {
        await loadCurrentUser();
        await loadPosts();
        await loadTrending();
    })();
})();