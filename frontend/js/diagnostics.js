let accessToken = null;

async function ensureAccessToken() {
    try {
        const res = await fetch('http://localhost:8080/auth/refresh', {
            method: 'POST',
            credentials: 'include'
        });
        if (!res.ok) throw new Error('Token refresh failed');
        const data = await res.json();
        accessToken = data.accessToken;
        return accessToken;
    } catch (err) {
        console.warn('Refresh failed, redirecting to login', err);
        window.location.href = 'http://localhost:63343/frontend/auth.html';
        throw err;
    }
}

async function apiFetch(url, options = {}) {
    await ensureAccessToken();
    return fetch(url, {
        ...options,
        credentials: 'include',
        headers: {
            ...(options.headers || {}),
            'Authorization': `Bearer ${accessToken}`,

            ...((options.body instanceof FormData) ? {} : { 'Content-Type': 'application/json' })
        }
    });
}

// Get images without authentication
async function fetchPublicResource(url) {
    return fetch(url, {
        method: 'GET',
        mode: 'cors',
        cache: 'default'
    });
}

document.addEventListener('DOMContentLoaded', () => {
    // ELEMENTS
    const themeToggle     = document.getElementById('theme-toggle');
    const dropArea        = document.getElementById('drop-area');
    const fileInput       = document.getElementById('file-input');
    const previewCont     = document.getElementById('image-preview-container');
    const imagePreview    = document.getElementById('image-preview');
    const clearBtn        = document.getElementById('clear-image-btn');
    const emptyState      = document.getElementById('empty-state');
    const loadingIndicator= document.getElementById('loading-indicator');
    const resultsContent  = document.getElementById('results-content');
    const predictionEl    = document.getElementById('model-prediction');
    const insightsEl      = document.getElementById('ai-insights');
    const caseNumberEl    = document.getElementById('case-number');
    const caseIdEl        = document.getElementById('case-id');

    function applyTheme(theme) {
        document.documentElement.setAttribute('data-theme', theme);
        themeToggle.innerHTML = theme === 'dark'
            ? '<i class="fas fa-sun"></i>'
            : '<i class="fas fa-moon"></i>';
        localStorage.setItem('smartfarm-theme', theme);
    }
    themeToggle.addEventListener('click', () => {
        const current = document.documentElement.getAttribute('data-theme');
        applyTheme(current === 'dark' ? 'light' : 'dark');
    });

    let currentCase = null;
    function generateCaseNumber() {
        const ts = Date.now().toString().slice(-6);
        const rnd= Math.floor(Math.random()*9000)+1000;
        return `SF${ts}${rnd}`;
    }
    function showCaseNumber(code) {
        currentCase = code;
        caseIdEl.textContent = code;
        caseNumberEl.style.display = 'flex';
        caseNumberEl.classList.add('fade-in');
    }
    function hideCaseNumber() {
        currentCase = null;
        caseNumberEl.style.display = 'none';
    }
    caseNumberEl.addEventListener('click', () => {
        if (!currentCase) return;
        navigator.clipboard.writeText(currentCase).then(() => {
            caseNumberEl.classList.add('copied');
            const txt = caseIdEl.textContent;
            caseIdEl.textContent = 'Copied!';
            setTimeout(() => {
                caseNumberEl.classList.remove('copied');
                caseIdEl.textContent = txt;
            }, 1200);
        });
    });

    // STATES
    function showEmptyState() {
        emptyState.style.display      = 'flex';
        loadingIndicator.style.display= 'none';
        resultsContent.style.display  = 'none';
    }
    function showLoadingState() {
        emptyState.style.display      = 'none';
        loadingIndicator.style.display= 'flex';
        resultsContent.style.display  = 'none';
    }

    // DROP & UPLOAD
    dropArea.addEventListener('click', () => fileInput.click());
    fileInput.addEventListener('change', e => handleFiles(e.target.files));

    ['dragenter','dragover','dragleave','drop'].forEach(evt => {
        dropArea.addEventListener(evt, preventDefaults, false);
        document.body.addEventListener(evt, preventDefaults, false);
    });
    function preventDefaults(e) { e.preventDefault(); e.stopPropagation(); }

    ['dragenter','dragover'].forEach(evt => {
        dropArea.addEventListener(evt, ()=> dropArea.classList.add('highlight'), false);
    });
    ['dragleave','drop'].forEach(evt => {
        dropArea.addEventListener(evt, ()=> dropArea.classList.remove('highlight'), false);
    });
    dropArea.addEventListener('drop', e => handleFiles(e.dataTransfer.files), false);

    function handleFiles(files) {
        if (!files.length) return;
        const file = files[0];
        if (!file.type.startsWith('image/')) {
            alert('Only image files allowed');
            return;
        }
        const reader = new FileReader();
        reader.onload = ev => {
            imagePreview.src = ev.target.result;
            previewCont.style.display = 'block';
            previewCont.classList.add('fade-in');
            const code = generateCaseNumber();
            showCaseNumber(code);
            triggerAnalysis();      // REAL API CALL
        };
        reader.readAsDataURL(file);
    }

    clearBtn.addEventListener('click', () => {
        fileInput.value = '';
        previewCont.style.display = 'none';
        hideCaseNumber();
        showEmptyState();
    });

    // Real analysis
    async function triggerAnalysis() {
        showLoadingState();
        try {
            const file = fileInput.files[0];
            if (!file) throw new Error('No file selected');
            const form = new FormData();
            form.append('file', file);

            const res = await apiFetch('http://localhost:8080/api/diagnosis', {
                method: 'POST',
                body: form
            });
            if (!res.ok) throw new Error(`Server returned ${res.status}`);

            const data = await res.json();

            // Show real case code
            showCaseNumber(data.caseCode);

            // Prediction + confidence
            predictionEl.innerHTML = `
                    ${data.label}
                    <span class="confidence-badge">
                        ${(data.confidence*100).toFixed(1)}%
                    </span>`;

            // Insights from Ollama Mistral
            insightsEl.innerHTML = data.insights;

            let filename = data.imageUrl;
            if (filename.includes('/')) {
                filename = filename.split('/').pop();
            }

            console.log('filename: ' + filename);

            const imageUrl = `http://localhost:8080/uploads/${filename}`;

            try {
                const response = await fetchPublicResource(imageUrl);
                if (response.ok) {
                    const blob = await response.blob();
                    const imageObjectURL = URL.createObjectURL(blob);
                    imagePreview.src = imageObjectURL;

                    imagePreview.onload = () => URL.revokeObjectURL(imageObjectURL);
                } else {
                    throw new Error(`HTTP ${response.status}`);
                }
            } catch (err) {
                console.error('Error loading image:', err);

                imagePreview.src = imageUrl;
            }

            loadingIndicator.style.display = 'none';
            resultsContent.style.display   = 'flex';
            resultsContent.classList.add('slide-up');

        } catch (err) {
            console.error('Diagnosis error', err);
            loadingIndicator.style.display = 'none';
            emptyState.style.display       = 'flex';
            alert('Diagnosis failed, please try again.');
        }
    }

    // Initialize
    const saved = localStorage.getItem('smartfarm-theme') || 'dark';
    applyTheme(saved);
    showEmptyState();

    // Ensure token is refreshed on page load
    ensureAccessToken().catch(err => {
        console.warn('Initial token refresh failed:', err);
    });
});