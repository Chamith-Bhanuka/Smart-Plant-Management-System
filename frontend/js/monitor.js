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

    // Global search across monitored plants
    globalSearch.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            performGlobalSearch();
        }
    });

    // Modal close on backdrop click
    document.getElementById('comparisonModal').addEventListener('click', (e) => {
        if (e.target.id === 'comparisonModal') closeComparison();
    });

    // ESC closes modal
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') closeComparison();
    });

    // Add simple spin animation (used by Refresh)
    const styleSheet = document.createElement('style');
    styleSheet.innerText = '@keyframes spin { from { transform: rotate(0deg);} to { transform: rotate(360deg);} }';
    document.head.appendChild(styleSheet);

    animateCards();
    loadMonitorData(); // fetch backend data on load
});

// ===== Theme =====
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

// ===== Global search (monitored plants) =====
let monitorCache = []; // array of PlantMonitorDTO
function performGlobalSearch() {
    const searchTerm = document.getElementById('global-search').value.toLowerCase().trim();
    if (!searchTerm) return;

    const plant = monitorCache.find((p) =>
        (p.scientificName && p.scientificName.toLowerCase().includes(searchTerm)) ||
        (p.commonName && p.commonName.toLowerCase().includes(searchTerm))
    );

    if (plant) {
        renderPlantMonitor(plant);
        document.querySelector('.main-panel').scrollIntoView({ behavior: 'smooth' });
    } else {
        alert('Plant not found in your monitored list.');
    }
}

// Keep token only in memory
let accessToken = null;

// Always refresh before a protected call
async function ensureAccessToken() {
    try {
        const res = await fetch('http://localhost:8080/auth/refresh', {
            method: 'POST',
            credentials: 'include', // important for cookie-based refresh
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

// Generic helper for GET/POST with current token
async function apiFetch(url, options = {}) {
    await ensureAccessToken();
    return fetch(url, {
        ...options,
        headers: {
            ...(options.headers || {}),
            'Authorization': `Bearer ${accessToken}`
        }
    });
}

// ===== Data loading & rendering =====
async function loadMonitorData() {
    try {
        const res = await apiFetch('http://localhost:8080/plants/monitor', { method: 'GET' });
        if (!res.ok) throw new Error('Failed to load monitor data');
        monitorCache = await res.json();

        const lastMonitor = monitorCache[monitorCache.length - 1];

        if (monitorCache.length > 0) {
            renderPlantMonitor(lastMonitor);
        } else {
            console.warn('No plants found for this user.');
            clearMonitorUI();
        }
    } catch (err) {
        console.error('Monitor load error:', err);
    }
}

function clearMonitorUI() {
    document.getElementById('plant-title-heading').textContent = 'Plant Health Analysis';
    document.getElementById('plant-zone').textContent = 'No plant selected';
    setMetricValue('temperature-metric', '--');
    setMetricValue('humidity-metric', '--');
    setMetricValue('soil_moisture-metric', '--');
    setMetricValue('light_intensity-metric', '--');
    setDetailValue('wind_speed-detail', '--');
    setDetailValue('precipitation-detail', '--');
    setDetailValue('uv_index-detail', '--');
    setDetailValue('cloud_cover-detail', '--');
    setDetailValue('evapotranspiration-detail', '--');
    setDetailValue('pressure-detail', '--');
}

// Render a single plant into the existing DOM
// function renderPlantMonitor(plant) {
//     // Title and meta
//     const sci = plant.scientificName || 'Plant';
//     const common = plant.commonName ? ` (${plant.commonName})` : '';
//     document.getElementById('plant-title-heading').textContent = `${sci}${common}`;
//     document.getElementById('plant-zone').textContent =
//         `${sci} - (${plant.latitude?.toFixed?.(4) || '?'}, ${plant.longitude?.toFixed?.(4) || '?'})`;
//
//     //Set plant image
//     const filename = plant.imagePath?.split('/').pop(); // Extract just the filename
//     if (filename) {
//         document.getElementById('plant-image').src = `http://localhost:8080/uploads/${filename}`;
//         console.log('Plant image loaded.');
//         console.log('File name: ', filename);
//     } else {
//         document.getElementById('plant-image').src = 'default-image.jpg'; // fallback if missing
//         console.log('Plant image missing.');
//     }
//
//     // Right sidebar: Weather details (from DB latest)
//     const w = plant.weather || {};
//     setDetailValue('wind_speed-detail', w.wind != null ? `${formatNumber(w.wind)} km/h` : '--');
//     setDetailValue('precipitation-detail', w.precipitation != null ? `${formatNumber(w.precipitation)} mm` : '--');
//     setDetailValue('uv_index-detail', w.uvIndex != null ? `${formatNumber(w.uvIndex)}` : '--');
//     setDetailValue('cloud_cover-detail', w.cloudCover != null ? `${formatNumber(w.cloudCover)}%` : '--');
//     setDetailValue('evapotranspiration-detail', w.evapotranspiration != null ? `${formatNumber(w.evapotranspiration)} mm/day` : '--');
//     setDetailValue('pressure-detail', w.pressure != null ? `${formatNumber(w.pressure)} hPa` : '--');
//
//     // Left metrics: Air temp & humidity from weather (DB)
//     setMetricValue('temperature-metric', w.airTemperature != null ? `${formatNumber(w.airTemperature)}°C` : '--');
//     setMetricValue('humidity-metric', w.airHumidity != null ? `${formatNumber(w.airHumidity)}%` : '--');
//
//     // Sensors handled separately; if present, render them
//     const s = plant.sensor || {};
//     setMetricValue('soil_moisture-metric', s.soilMoisture != null ? `${formatNumber(s.soilMoisture)}%` : '--');
//     setMetricValue('light_intensity-metric', s.lightIntensity != null ? `${formatNumber(s.lightIntensity)} lux` : '--');
//
//     // Build modal comparison map dynamically
//     buildOptimalConditionsMap(plant);
// }

async function renderPlantMonitor(plant) {
    // Title and meta
    const sci = plant.scientificName || 'Plant';
    const common = plant.commonName ? ` (${plant.commonName})` : '';
    document.getElementById('plant-title-heading').textContent = `${sci}${common}`;
    document.getElementById('plant-zone').textContent =
        `${sci} - (${plant.latitude?.toFixed?.(4) || '?'}, ${plant.longitude?.toFixed?.(4) || '?'})`;

    // ✅ Set plant image
    try {
        const refreshResponse = await fetch('http://localhost:8080/auth/refresh', {
            method: 'POST',
            credentials: 'include' // sends the refreshToken cookie
        });

        if (!refreshResponse.ok) {
            throw new Error("Refresh failed");
        }

        const refreshData = await refreshResponse.json();
        const newAccessToken = refreshData.accessToken;


        const res = await fetch("http://localhost:8080/uploads/last", {
            withCredentials: true, // important if you’re using cookie auth
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${newAccessToken}`
            },
            credentials: 'include'
        });

        if (res.ok) {
            const blob = await res.blob();
            const imgUrl = URL.createObjectURL(blob);
            document.getElementById("plant-image").src = imgUrl;
            console.log("Plant image loaded from /uploads/last");
        } else {
            document.getElementById("plant-image").src = "default-image.jpg";
            console.warn("No last image found, using fallback.");
        }
    } catch (e) {
        document.getElementById("plant-image").src = "default-image.jpg";
        console.error("Error loading last plant image:", e);
    }

    // Weather details (from DB latest)
    const w = plant.weather || {};
    setDetailValue('wind_speed-detail', w.wind != null ? `${formatNumber(w.wind)} km/h` : '--');
    setDetailValue('precipitation-detail', w.precipitation != null ? `${formatNumber(w.precipitation)} mm` : '--');
    setDetailValue('uv_index-detail', w.uvIndex != null ? `${formatNumber(w.uvIndex)}` : '--');
    setDetailValue('cloud_cover-detail', w.cloudCover != null ? `${formatNumber(w.cloudCover)}%` : '--');
    setDetailValue('evapotranspiration-detail', w.evapotranspiration != null ? `${formatNumber(w.evapotranspiration)} mm/day` : '--');
    setDetailValue('pressure-detail', w.pressure != null ? `${formatNumber(w.pressure)} hPa` : '--');

    // Left metrics: Air temp & humidity from weather (DB)
    setMetricValue('temperature-metric', w.airTemperature != null ? `${formatNumber(w.airTemperature)}°C` : '--');
    setMetricValue('humidity-metric', w.airHumidity != null ? `${formatNumber(w.airHumidity)}%` : '--');

    // Sensors handled separately
    const s = plant.sensor || {};
    setMetricValue('soil_moisture-metric', s.soilMoisture != null ? `${formatNumber(s.soilMoisture)}%` : '--');
    setMetricValue('light_intensity-metric', s.lightIntensity != null ? `${formatNumber(s.lightIntensity)} lux` : '--');

    // Optimal condition map
    buildOptimalConditionsMap(plant);
}

// ===== Helpers to set UI text =====
function setDetailValue(id, text) {
    const el = document.getElementById(id);
    if (!el) return;
    const valueEl = el.querySelector('.detail-value');
    if (valueEl) valueEl.textContent = text ?? '--';
}

function setMetricValue(id, text) {
    const el = document.getElementById(id);
    if (!el) return;
    const valueEl = el.querySelector('.metric-value');
    if (valueEl) valueEl.textContent = text ?? '--';
}

function formatNumber(n) {
    if (n == null || Number.isNaN(n)) return '--';
    const x = Number(n);
    return Number.isInteger(x) ? x : x.toFixed(1);
}

// ===== Optimal conditions map for modal (built from DB data) =====
let optimalConditionsMap = {};

function buildOptimalConditionsMap(plant) {
    const opt = plant.optimal || {};
    const w = plant.weather || {};
    const s = plant.sensor || {};

    optimalConditionsMap = {
        temperature: {
            optimal: rangeOrValue(opt.idealTemperature, '°C'),
            current: w.airTemperature != null ? `${formatNumber(w.airTemperature)}°C` : '--',
            status: statusFor(w.airTemperature, opt.idealTemperature, 2),
            description: `Target air temperature around ${formatNumber(opt.idealTemperature)}°C for ${opt.plantName || 'this plant'}.`
        },
        humidity: {
            optimal: rangeOrValue(opt.idealHumidity, '%'),
            current: w.airHumidity != null ? `${formatNumber(w.airHumidity)}%` : '--',
            status: statusFor(w.airHumidity, opt.idealHumidity, 5),
            description: `Aim for relative humidity near ${formatNumber(opt.idealHumidity)}%.`
        },
        soil_moisture: {
            optimal: '50-70%', // rule-of-thumb; add field later if you want DB-driven values
            current: s.soilMoisture != null ? `${formatNumber(s.soilMoisture)}%` : '--',
            status: qualitative(s.soilMoisture, 50, 70),
            description: 'Keep soil moisture in optimal range; irrigate if below.'
        },
        light_intensity: {
            optimal: opt.sunlightExposure || 'Full sun / Partial shade',
            current: s.lightIntensity != null ? `${formatNumber(s.lightIntensity)} lux` : '--',
            status: 'good',
            description: 'Adjust shade or supplemental lighting based on growth stage.'
        },
        wind_speed: {
            optimal: '5-15 km/h',
            current: w.wind != null ? `${formatNumber(w.wind)} km/h` : '--',
            status: qualitative(w.wind, 5, 15),
            description: 'Moderate wind improves airflow; too high risks damage.'
        },
        precipitation: {
            optimal: opt.idealRainfall != null ? `${formatNumber(opt.idealRainfall)} mm/day` : '--',
            current: w.precipitation != null ? `${formatNumber(w.precipitation)} mm` : '--',
            status: statusFor(w.precipitation, opt.idealRainfall, 2),
            description: 'Use rainfall + irrigation to meet daily water needs.'
        },
        uv_index: {
            optimal: '3-7',
            current: w.uvIndex != null ? `${formatNumber(w.uvIndex)}` : '--',
            status: qualitative(w.uvIndex, 3, 7),
            description: 'Moderate UV supports photosynthesis; very high may scorch leaves.'
        },
        cloud_cover: {
            optimal: '< 40%',
            current: w.cloudCover != null ? `${formatNumber(w.cloudCover)}%` : '--',
            status: w.cloudCover != null ? (w.cloudCover < 40 ? 'good' : 'warning') : 'warning',
            description: 'Lower cloud cover increases light availability.'
        },
        evapotranspiration: {
            optimal: '3-5 mm/day',
            current: w.evapotranspiration != null ? `${formatNumber(w.evapotranspiration)} mm/day` : '--',
            status: qualitative(w.evapotranspiration, 3, 5),
            description: 'Use ET to plan irrigation volume and timing.'
        },
        pressure: {
            optimal: '1010-1020 hPa',
            current: w.pressure != null ? `${formatNumber(w.pressure)} hPa` : '--',
            status: qualitative(w.pressure, 1010, 1020),
            description: 'Normal pressure reflects stable local weather.'
        }
    };
}

// Range/value helpers for modal
function rangeOrValue(center, unit) {
    if (center == null) return '--';
    // Show a friendly ± band for display; adjust as needed
    const delta = unit === '%' ? 5 : 2;
    const low = (Number(center) - delta).toFixed(0);
    const high = (Number(center) + delta).toFixed(0);
    return `${low}-${high} ${unit}`;
}

function statusFor(current, target, tol) {
    if (current == null || target == null) return 'warning';
    const diff = Math.abs(Number(current) - Number(target));
    if (diff <= tol) return 'good';
    if (diff <= tol * 2) return 'warning';
    return 'critical';
}

function qualitative(value, low, high) {
    if (value == null) return 'warning';
    const v = Number(value);
    if (v < low || v > high) return 'warning';
    return 'good';
}

// ===== Comparison modal (dynamic) =====
function showComparison(type) {
    const modal = document.getElementById('comparisonModal');
    const titleEl = document.getElementById('modalTitle');
    const grid = document.getElementById('comparisonGrid');
    const data = optimalConditionsMap[type];

    const element = document.getElementById(`${type}-metric`) || document.getElementById(`${type}-detail`);
    if (!element || !data) {
        console.error('Missing element or data for type:', type);
        return;
    }

    const labelEl = element.querySelector('.metric-label, .detail-label');
    titleEl.textContent = `${labelEl.textContent} Analysis`;

    const statusClass = data.status === 'good' ? 'status-optimal' : data.status === 'warning' ? 'status-warning' : 'status-critical';
    const statusText = data.status === 'good' ? 'Optimal' : data.status === 'warning' ? 'Warning' : 'Critical';

    grid.innerHTML = `
    <div class="comparison-item">
      <div class="comparison-label">Current Reading</div>
      <div class="comparison-value">${data.current}</div>
      <div class="sensor-status ${statusClass}">${statusText}</div>
    </div>
    <div class="comparison-item">
      <div class="comparison-label">Optimal Range</div>
      <div class="comparison-value">${data.optimal}</div>
    </div>
    <div class="comparison-item" style="grid-column: 1 / -1;">
      <div class="comparison-label">Analysis & Recommendations</div>
      <p class="comparison-description">${data.description}</p>
    </div>
  `;
    modal.classList.add('visible');
}

function closeComparison() {
    document.getElementById('comparisonModal').classList.remove('visible');
}

// ===== Health toggle (kept from your UI) =====
let isDiseaseMode = false;
function toggleHealthMode() {
    const healthContent = document.getElementById('healthContent');
    const healthScore = document.getElementById('healthScore');
    isDiseaseMode = !isDiseaseMode;
    if (isDiseaseMode) {
        healthScore.className = 'health-score-badge score-critical';
        healthScore.innerHTML = `45 <span>Health</span>`;
        healthContent.innerHTML = `<div class="health-status disease-alert">
      <h4 style="margin-bottom: 0.5rem; color: var(--danger);">Disease Alert: Nutrient Deficiency</h4>
      <p>Early signs of nitrogen deficiency observed. Yellowing on lower leaves indicates potential nutrient stress.</p>
      <button class="diagnosis-btn" onclick="toggleHealthMode()"><i class="fas fa-arrow-left"></i> Back to General Status</button>
    </div>`;
    } else {
        healthScore.className = 'health-score-badge score-warning';
        healthScore.innerHTML = `72 <span>Health</span>`;
        healthContent.innerHTML = `<div class="health-status">
      <p>Plant is in flowering stage with healthy root development. Growth rate is within normal parameters.</p>
      <button class="diagnosis-btn" onclick="toggleHealthMode()"><i class="fas fa-search"></i> Check for Diseases</button>
    </div>`;
    }
}

// ===== Refresh button: re-pull from backend =====
async function refreshData() {
    const refreshBtn = document.getElementById('refreshBtn');
    const refreshIcon = refreshBtn.querySelector('.fa-sync-alt');
    refreshBtn.disabled = true;
    refreshIcon.style.animation = 'spin 1s linear infinite';
    try {
        await loadMonitorData();
    } finally {
        setTimeout(() => {
            refreshBtn.disabled = false;
            refreshIcon.style.animation = '';
        }, 500);
    }
}

// ===== Settings stub (kept) =====
function showSettings() {
    alert('⚙️ Settings Panel Opened');
}

// ===== Entrance animation (kept) =====
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
