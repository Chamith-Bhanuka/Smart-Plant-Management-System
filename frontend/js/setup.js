let accessToken = null;

async function ensureAccessToken() {
    try {
        const res = await fetch('http://localhost:8080/auth/refresh', {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' }
        });

        if (!res.ok) throw new Error("Failed to refresh token");

        const data = await res.json();
        accessToken = data.accessToken;
        console.log("♻️ Got new token:", accessToken);
    } catch (e) {
        console.warn("❌ Refresh failed. Redirecting to login...");
        window.location.href = 'http://localhost:63343/frontend/auth.html';
    }
}

async function submitPlantSetup(formData) {
    try {
        await ensureAccessToken();

        const response = await fetch('http://localhost:8080/plants/setup', {
            method: 'POST',
            withCredentials: true,
            headers: {
                'Authorization': `Bearer ${accessToken}`
            },
            body: buildFormData(formData),
        });

        return await response.json();
    } catch (error) {
        console.error('Setup failed:', error);
        throw error;
    }
}

function buildFormData(formData) {
    const fd = new FormData();
    fd.append('image', formData.plantImageFile);  // ✅ always a File
    fd.append('latitude', parseFloat(formData.location.split(',')[0]));
    fd.append('longitude', parseFloat(formData.location.split(',')[1]));
    fd.append('environment', formData.environment);
    console.log('Image: ', formData.plantImageFile);
    console.log('latitude: ', formData.latitude);
    console.log('longitude: ', formData.longitude);
    console.log('environment: ', formData.environment);
    return fd;
}

// Utility: convert URL → File
async function urlToFile(url, filename = 'plant.jpg', mimeType = 'image/jpeg') {
    const res = await fetch(url);
    const blob = await res.blob();
    return new File([blob], filename, { type: mimeType });
}

document.addEventListener('DOMContentLoaded', () => {
    //state
    let currentStep = 1;
    const totalSteps = 4;
    const formData = {
        plantImage: null,
        plantImageFile: null,
        location: null,
        environment: null
    };

    //selectors
    const stepIndicator = document.getElementById('step-indicator');
    const prevBtn = document.getElementById('prev-btn');
    const nextBtn = document.getElementById('next-btn');
    const formSteps = document.querySelectorAll('.form-step');
    const imageUploadInput = document.getElementById('image-upload-input');
    const optionCards = document.querySelectorAll('.option-card');
    const webSearchContainer = document.getElementById('web-search-container');
    const webSearchInput = document.getElementById('web-search-input');
    const webSearchBtn = document.getElementById('web-search-btn');
    const imageDropZone = document.getElementById('image-drop-zone');
    const themeToggle = document.getElementById('theme-toggle');

    const mapSearchInput = document.getElementById('map-search-input');
    const mapSearchBtn = document.getElementById('map-search-btn');

    const beeContainer = document.getElementById('bee-delivery-container');
    const deliveryBee = document.getElementById('delivery-bee');
    const beeCarriedImage = document.getElementById('bee-carried-image');

    const loadingScreen = document.getElementById('esp32-loading-screen');
    const loadingProgressBar = document.getElementById('loading-progress-bar');
    const loadingStatusText = document.getElementById('loading-status-text');

    //theme toggle
    function toggleTheme() {
        const currentTheme = document.documentElement.getAttribute('data-theme');
        const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
        document.documentElement.setAttribute('data-theme', newTheme);
    }
    themeToggle.addEventListener('click', toggleTheme);
    document.documentElement.setAttribute('data-theme', 'dark');

    //map
    let map, marker;
    function initMap() {
        map = L.map('map').setView([51.505, -0.09], 13);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19,
            attribution: '© OpenStreetMap'
        }).addTo(map);

        map.on('click', function(e) {
            if (marker) map.removeLayer(marker);
            marker = L.marker(e.latlng).addTo(map);
            formData.location = `${e.latlng.lat.toFixed(4)}, ${e.latlng.lng.toFixed(4)}`;
            document.getElementById('location-text').textContent = formData.location;
            validateStep();
        });
    }

    async function searchLocation(query) {
        try {
            const response = await fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query)}&limit=1`);
            const results = await response.json();
            if (results && results.length > 0) {
                const result = results[0];
                const lat = parseFloat(result.lat);
                const lon = parseFloat(result.lon);

                map.setView([lat, lon], 13);
                if (marker) map.removeLayer(marker);
                marker = L.marker([lat, lon]).addTo(map);
                formData.location = `${lat.toFixed(4)}, ${lon.toFixed(4)}`;
                document.getElementById('location-text').textContent = `${result.display_name.split(',')[0]}, ${result.display_name.split(',')[1]}`;
                validateStep();
            } else {
                alert('Location not found.');
            }
        } catch (error) {
            console.error('Error searching location:', error);
            alert('Error searching for location.');
        }
    }
    mapSearchBtn.addEventListener('click', () => {
        const query = mapSearchInput.value.trim();
        if (query) searchLocation(query);
    });
    mapSearchInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            const query = mapSearchInput.value.trim();
            if (query) searchLocation(query);
        }
    });

    //navigation
    function updateStepView() {
        formSteps.forEach(step => {
            step.classList.remove('active');
            if (parseInt(step.dataset.step) === currentStep) step.classList.add('active');
        });
        stepIndicator.textContent = `Step ${currentStep} of ${totalSteps}`;
        prevBtn.disabled = currentStep === 1;
        nextBtn.textContent = currentStep === totalSteps ? 'Save & Monitor' : 'Next';
        if (currentStep === 3 && !map) setTimeout(() => initMap(), 100);
        validateStep();
    }
    function goToNextStep() {
        if (currentStep < totalSteps) {
            currentStep++;
            updateStepView();
            if(currentStep === 4) {
                document.getElementById('summary-location').textContent = formData.location || 'Not Selected';
                document.getElementById('summary-environment').textContent = formData.environment || 'Not Selected';
            }
        } else {
            nextBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Starting Monitor...';
            nextBtn.disabled = true;
            submitPlantSetup(formData);
            setTimeout(() => {
                alert('🌱 Configuration saved! Monitoring started.');
                nextBtn.innerHTML = 'Save & Monitor';
                nextBtn.disabled = false;
            }, 2000);

            // window.location.href = 'http://localhost:63343/frontend/monitor.html';
        }
    }
    function goToPrevStep() {
        if (currentStep > 1) {
            currentStep--;
            updateStepView();
        }
    }
    nextBtn.addEventListener('click', goToNextStep);
    prevBtn.addEventListener('click', goToPrevStep);

    //validation
    function validateStep() {
        let isValid = false;
        switch(currentStep) {
            case 1: isValid = formData.plantImageFile !== null; break;
            case 2: isValid = true; break;
            case 3: isValid = formData.location && formData.environment; break;
            case 4: isValid = true; break;
        }
        nextBtn.disabled = !isValid;
    }

    //esp32 loading
    function showESP32Loading() {
        loadingScreen.classList.add('active');
        const steps = [
            { progress: 20, text: 'Connecting to camera', delay: 500 },
            { progress: 40, text: 'Initializing sensor', delay: 800 },
            { progress: 60, text: 'Adjusting focus', delay: 1200 },
            { progress: 80, text: 'Capturing image', delay: 1000 },
            { progress: 100, text: 'Processing image', delay: 800 }
        ];
        let i = 0;
        function update() {
            if (i < steps.length) {
                const s = steps[i];
                setTimeout(() => {
                    loadingProgressBar.style.width = s.progress + '%';
                    loadingStatusText.textContent = s.text;
                    i++; update();
                }, s.delay);
            } else {
                setTimeout(async () => {
                    loadingScreen.classList.remove('active');
                    const sampleImage = 'https://images.unsplash.com/photo-1416879595882-3373a0480b5b?w=300&h=300&fit=crop';
                    formData.plantImageFile = await urlToFile(sampleImage);
                    handleImageSelection(sampleImage);
                }, 500);
            }
        }
        update();
    }

    //bee animation
    function handleImageSelection(imageUrl) {
        formData.plantImage = imageUrl;
        beeCarriedImage.src = imageUrl;
        document.getElementById('summary-image').src = imageUrl;

        const startX = -100;
        const startY = Math.random() * (window.innerHeight * 0.4) + (window.innerHeight * 0.1);
        const dropZone = imageDropZone.getBoundingClientRect();
        const endX = dropZone.left + (dropZone.width / 2) - 60;
        const endY = dropZone.top + (dropZone.height / 2) - 50;

        deliveryBee.style.transform = `translate(${startX}px, ${startY}px)`;
        beeContainer.classList.add('active');

        setTimeout(() => {
            deliveryBee.style.transform = `translate(${endX}px, ${endY}px)`;
        }, 300);

        setTimeout(() => {
            beeContainer.classList.remove('active');
            imageDropZone.innerHTML = `<img src="${imageUrl}" class="dropped-image" alt="Plant">`;
            imageDropZone.classList.add('has-image');
            createSparkles(endX + 40, endY + 40);
            validateStep();
        }, 4500);
    }

    function createSparkles(x, y) {
        for (let i = 0; i < 8; i++) {
            const sparkle = document.createElement('div');
            sparkle.innerHTML = '✨';
            sparkle.style.position = 'fixed';
            sparkle.style.left = x + 'px';
            sparkle.style.top = y + 'px';
            sparkle.style.fontSize = '1.5rem';
            sparkle.style.pointerEvents = 'none';
            sparkle.style.zIndex = '1001';
            sparkle.style.animation = `sparkle-${i} 1s ease-out forwards`;
            const style = document.createElement('style');
            style.textContent = `
              @keyframes sparkle-${i} {
                0% { opacity: 1; transform: translate(0, 0) scale(0.5); }
                100% { opacity: 0; transform: translate(${(Math.random()-0.5)*100}px, ${(Math.random()-0.5)*100}px) scale(1.5); }
              }`;
            document.head.appendChild(style);
            document.body.appendChild(sparkle);
            setTimeout(() => { sparkle.remove(); style.remove(); }, 1000);
        }
    }

    //step1 events
    optionCards.forEach(card => {
        card.addEventListener('click', () => {
            const source = card.dataset.source;
            webSearchContainer.classList.remove('active');
            if (source === 'upload') {
                imageUploadInput.click();
            } else if (source === 'web') {
                webSearchContainer.classList.add('active');
                webSearchInput.focus();
            } else if (source === 'esp32') {
                showESP32Loading();
            }
        });
    });

    imageUploadInput.addEventListener('change', (event) => {
        if (event.target.files && event.target.files[0]) {
            const file = event.target.files[0];
            formData.plantImageFile = file;   // ✅ keep File for backend
            const reader = new FileReader();
            reader.onload = (e) => {
                handleImageSelection(e.target.result);
            };
            reader.readAsDataURL(file);
        }
    });

    function performWebSearch() {
        const query = webSearchInput.value.trim();
        if (!query) return;
        webSearchBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
        const samples = [
            'https://images.unsplash.com/photo-1416879595882-3373a0480b5b?w=300&h=300&fit=crop',
            'https://images.unsplash.com/photo-1592150621744-aca64f48394a?w=300&h=300&fit=crop',
            'https://images.unsplash.com/photo-1585320806297-9794b3e4eeae?w=300&h=300&fit=crop',
            'https://images.unsplash.com/photo-1574281254017-5d7c16f4bf36?w=300&h=300&fit=crop'
        ];
        setTimeout(async () => {
            const randomImage = samples[Math.floor(Math.random()*samples.length)];
            formData.plantImageFile = await urlToFile(randomImage);
            handleImageSelection(randomImage);
            webSearchBtn.innerHTML = '<i class="fas fa-search"></i>';
        }, 1500);
    }
    webSearchBtn.addEventListener('click', performWebSearch);
    webSearchInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') performWebSearch();
    });

    //step3
    document.querySelectorAll('.env-radio').forEach(radio => {
        radio.addEventListener('change', (e) => {
            formData.environment = e.target.value;
            document.getElementById('summary-environment').textContent = formData.environment;
            validateStep();
        });
    });

    updateStepView();
});
