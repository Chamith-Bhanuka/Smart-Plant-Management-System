document.addEventListener('DOMContentLoaded', () => {
    const darkBtn = document.getElementById('dark-btn');
    const lightBtn = document.getElementById('light-btn');
    const globalSearch = document.getElementById('global-search');

    let currentTheme = 'dark';
    try {
        currentTheme = localStorage.getItem('theme') || 'dark';
    } catch (e) {
        currentTheme = 'dark';
    }

    setTheme(currentTheme);
    darkBtn.addEventListener('click', () => setTheme('dark'));
    lightBtn.addEventListener('click', () => setTheme('light'));

    // Global search functionality
    globalSearch.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            performGlobalSearch();
        }
    });

    animateCards();
});

function setTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    document.getElementById('dark-btn').classList.toggle('active', theme === 'dark');
    document.getElementById('light-btn').classList.toggle('active', theme === 'light');
    try {
        localStorage.setItem('theme', theme);
    } catch (e) {
        console.log('Theme preference could not be saved');
    }
}

function performGlobalSearch() {
    const searchTerm = document.getElementById('global-search').value.toLowerCase().trim();
    const plantDatabase = {
        'tomato': {
            name: 'Tomato Plant', zone: 'Zone A-1',
            image: 'https://images.unsplash.com/photo-1592845675346-c573b4011e95?w=800&h=600&fit=crop&crop=center',
            age: '45 days', stage: 'Flowering', harvest: '25 days', yield: '2.3 kg'
        },
        'lettuce': {
            name: 'Lettuce Crop', zone: 'Zone B-3',
            image: 'https://images.unsplash.com/photo-1556996433-6b2a33604a18?w=800&h=600&fit=crop&crop=center',
            age: '25 days', stage: 'Vegetative', harvest: '15 days', yield: '0.8 kg'
        },
        'corn': {
            name: 'Corn Stalk', zone: 'Zone C-5',
            image: 'https://images.unsplash.com/photo-1541539292558-52819035a396?w=800&h=600&fit=crop&crop=center',
            age: '60 days', stage: 'Tasseling', harvest: '30 days', yield: '1.5 kg'
        }
    };

    const plantData = plantDatabase[searchTerm];
    if (plantData) {
        document.getElementById('plant-image').src = plantData.image;
        document.getElementById('plant-title-heading').textContent = 'Plant Health Analysis';
        document.getElementById('plant-zone').textContent = `${plantData.name} - ${plantData.zone}`;
        document.getElementById('stat-age').textContent = plantData.age;
        document.getElementById('stat-stage').textContent = plantData.stage;
        document.getElementById('stat-harvest').textContent = plantData.harvest;
        document.getElementById('stat-yield').textContent = plantData.yield;

        // Scroll to main panel
        document.querySelector('.main-panel').scrollIntoView({ behavior: 'smooth' });
    } else {
        alert('Plant not found in database. Try "tomato", "lettuce", or "corn".');
    }
}

const optimalConditions = {
    temperature: { optimal: '20-26°C', current: '24°C', status: 'good', description: 'Temperature is within the optimal range.' },
    humidity: { optimal: '60-70%', current: '68%', status: 'good', description: 'Humidity levels are ideal.' },
    soil_moisture: { optimal: '50-70%', current: '45%', status: 'warning', description: 'Soil moisture is slightly below optimal range. Consider irrigation.' },
    light_intensity: { optimal: '1000-1500 lux', current: '850 lux', status: 'poor', description: 'Light intensity is below optimal range. Consider supplemental lighting.' },
    wind_speed: { optimal: '5-15 km/h', current: '12 km/h', status: 'good', description: 'Wind speed provides adequate air circulation.' },
    pressure: { optimal: '1010-1020 hPa', current: '1013 hPa', status: 'good', description: 'Atmospheric pressure is normal.' },
    soil_temp: { optimal: '18-24°C', current: '22°C', status: 'good', description: 'Soil temperature is perfect for root development.' },
    ph_level: { optimal: '6.0-7.0', current: '6.8', status: 'good', description: 'pH level is excellent for nutrient availability.' },
    precipitation: { optimal: '0-2 mm/hr', current: '0 mm', status: 'good', description: 'No precipitation, ideal for scheduled irrigation.' },
    uv_index: { optimal: '3-7', current: '5', status: 'good', description: 'Moderate UV index, good for photosynthesis without causing scorch.' },
    cloud_cover: { optimal: '< 40%', current: '35%', status: 'good', description: 'Low cloud cover ensures good light availability.' },
    evapotranspiration: { optimal: '3-5 mm/day', current: '3.2 mm/day', status: 'good', description: 'Normal water loss rate from soil and plants.' }
};

function showComparison(type) {
    const modal = document.getElementById('comparisonModal');
    const titleEl = document.getElementById('modalTitle');
    const grid = document.getElementById('comparisonGrid');
    const data = optimalConditions[type];

    const element = document.getElementById(`${type}-metric`) || document.getElementById(`${type}-detail`);
    if (!element || !data) {
        console.error("Could not find element or data for type:", type);
        return;
    }

    const labelEl = element.querySelector('.metric-label, .detail-label');
    titleEl.textContent = `${labelEl.textContent} Analysis`;

    const statusClass = data.status === 'good' ? 'status-optimal' : data.status === 'warning' ? 'status-warning' : 'status-critical';
    const statusText = data.status === 'good' ? 'Optimal' : data.status === 'warning' ? 'Warning' : 'Critical';

    grid.innerHTML = `
        <div class="comparison-item"><div class="comparison-label">Current Reading</div><div class="comparison-value">${data.current}</div><div class="sensor-status ${statusClass}">${statusText}</div></div>
        <div class="comparison-item"><div class="comparison-label">Optimal Range</div><div class="comparison-value">${data.optimal}</div></div>
        <div class="comparison-item" style="grid-column: 1 / -1;"><div class="comparison-label">Analysis & Recommendations</div><p class="comparison-description">${data.description}</p></div>`;
    modal.classList.add('visible');
}

function closeComparison() {
    document.getElementById('comparisonModal').classList.remove('visible');
}

let isDiseaseMode = false;
function toggleHealthMode() {
    const healthContent = document.getElementById('healthContent');
    const healthScore = document.getElementById('healthScore');
    isDiseaseMode = !isDiseaseMode;
    if (isDiseaseMode) {
        healthScore.className = 'health-score-badge score-critical';
        healthScore.innerHTML = `45 <span>Health</span>`;
        healthContent.innerHTML = `<div class="health-status disease-alert"><h4 style="margin-bottom: 0.5rem; color: var(--danger);">Disease Alert: Nutrient Deficiency</h4><p>Early signs of nitrogen deficiency observed. Yellowing on lower leaves indicates potential nutrient stress.</p><button class="diagnosis-btn" onclick="toggleHealthMode()"><i class="fas fa-arrow-left"></i> Back to General Status</button></div>`;
    } else {
        healthScore.className = 'health-score-badge score-warning';
        healthScore.innerHTML = `72 <span>Health</span>`;
        healthContent.innerHTML = `<div class="health-status"><p>Plant is in flowering stage with healthy root development. Growth rate is within normal parameters.</p><button class="diagnosis-btn" onclick="toggleHealthMode()"><i class="fas fa-search"></i> Check for Diseases</button></div>`;
    }
}

function refreshData() {
    const refreshBtn = document.getElementById('refreshBtn');
    const refreshIcon = refreshBtn.querySelector('.fa-sync-alt');
    refreshBtn.disabled = true;
    refreshIcon.style.animation = 'spin 1s linear infinite';
    setTimeout(() => {
        refreshBtn.disabled = false;
        refreshIcon.style.animation = '';
    }, 1500);
}

function showSettings() {
    alert('⚙️ Settings Panel Opened');
}

function animateCards() {
    document.querySelectorAll('.card').forEach((card, index) => {
        card.style.opacity = '0';
        card.style.transform = 'translateY(30px)';
        card.style.transition = 'all 0.6s cubic-bezier(0.165, 0.84, 0.44, 1)';
        setTimeout(() => {
            card.style.opacity = '1';
            card.style.transform = 'translateY(0)';
        }, index * 80);
    });
}

document.getElementById('comparisonModal').addEventListener('click', (e) => {
    if (e.target.id === 'comparisonModal') closeComparison();
});

document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') closeComparison();
});

const styleSheet = document.createElement("style");
styleSheet.innerText = "@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }";
document.head.appendChild(styleSheet);