// ----------------- App state -----------------
let accessToken = null;          // in-memory access token only
let selectedPlantId = null;     // currently selected plant (for refresh / snapshot)
let monitorCache = [];          // optional cached list of monitored plants (if used)
let optimalConditionsMap = {};  // used by comparison modal

// ----------------- Utilities -----------------
function formatNumber(n) {
    if (n == null || Number.isNaN(n)) return '--';
    const x = Number(n);
    return Number.isInteger(x) ? x : x.toFixed(1);
}

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

// ----------------- Token management -----------------
async function ensureAccessToken() {
    // Try to refresh using cookie-based refresh token
    try {
        const res = await fetch('http://localhost:8080/auth/refresh', {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' }
        });

        if (!res.ok) {
            throw new Error('refresh failed');
        }
        const data = await res.json();
        accessToken = data.accessToken;
        // console.log('♻️ Refreshed token (in-memory)');
        return accessToken;
    } catch (err) {
        // If refresh fails, redirect to login
        console.warn('Token refresh failed, redirecting to login.', err);
        // update this URL to your actual auth page if different
        window.location.href = 'http://localhost:63343/frontend/auth.html';
        throw err;
    }
}

// Generic fetch wrapper that ensures token and sets Authorization header
async function apiFetch(url, options = {}) {
    await ensureAccessToken();
    return fetch(url, {
        ...options,
        credentials: 'include', // always include cookies for refresh flow if needed
        headers: {
            ...(options.headers || {}),
            'Authorization': `Bearer ${accessToken}`,
            ...(options.headers && options.headers['Content-Type'] ? {} : { 'Content-Type': 'application/json' })
        }
    });
}

// ----------------- Theme & Entrance animation -----------------
function setTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    const darkBtn = document.getElementById('dark-btn');
    const lightBtn = document.getElementById('light-btn');
    if (darkBtn) darkBtn.classList.toggle('active', theme === 'dark');
    if (lightBtn) lightBtn.classList.toggle('active', theme === 'light');
    try {
        localStorage.setItem('theme', theme);
    } catch (e) {
        console.warn('Could not save theme preference');
    }
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

// add simple spin animation CSS
(function addSpinKeyframes() {
    const styleSheet = document.createElement('style');
    styleSheet.innerText = '@keyframes spin { from { transform: rotate(0deg);} to { transform: rotate(360deg);} }';
    document.head.appendChild(styleSheet);
})();

// ----------------- Search (debounced) & results rendering -----------------
const plantSearch = document.getElementById('plant-search');
const resultsBox = document.getElementById('search-results');

let searchTimer = null;
if (plantSearch) {
    plantSearch.addEventListener('input', () => {
        clearTimeout(searchTimer);
        const q = plantSearch.value.trim();
        if (!q) {
            resultsBox && resultsBox.classList.remove('open');
            if (resultsBox) resultsBox.innerHTML = '';
            return;
        }
        searchTimer = setTimeout(() => doSearch(q), 250);
    });
}

async function doSearch(q) {
    try {
        const res = await apiFetch(`http://localhost:8080/plants/search?q=${encodeURIComponent(q)}`, { method: 'GET' });
        if (!res.ok) {
            console.error('Search request failed:', res.status);
            resultsBox && (resultsBox.innerHTML = `<div class="search-item" style="grid-template-columns:1fr"><span>Search failed</span></div>`);
            resultsBox && resultsBox.classList.add('open');
            return;
        }
        const list = await res.json();
        renderSearchResults(list);
    } catch (e) {
        console.error('search error', e);
        resultsBox && (resultsBox.innerHTML = `<div class="search-item" style="grid-template-columns:1fr"><span>Error</span></div>`);
        resultsBox && resultsBox.classList.add('open');
    }
}

function renderSearchResults(list) {
    if (!resultsBox) return;
    if (!list || list.length === 0) {
        resultsBox.innerHTML = `<div class="search-item" style="grid-template-columns:1fr"><span>No results</span></div>`;
        resultsBox.classList.add('open');
        return;
    }
    resultsBox.innerHTML = list.map(p => {
        const filename = p.imagePath ? p.imagePath.split('/').pop() : null;
        const imgUrl = filename ? `http://localhost:8080/uploads/${filename}` : 'https://placehold.co/48x48';
        console.log('Image URL', imgUrl);
        const meta = [
            p.plantedDate ? `Planted: ${p.plantedDate}` : null,
            (p.latitude != null && p.longitude != null) ? `Loc: ${Number(p.latitude).toFixed(4)}, ${Number(p.longitude).toFixed(4)}` : null
        ].filter(Boolean).join(' • ');
        const right = [];
        if (p.daysToHarvest != null) right.push(`${p.daysToHarvest} days to harvest`);
        if (p.yieldPredictionKg != null) right.push(`${p.yieldPredictionKg} kg`);
        return `
      <div class="search-item" data-id="${p.plantId}">
        <img class="search-thumb" src="${imgUrl}" alt="thumb">
        <div>
          <div><strong>${p.scientificName || 'Plant'}</strong>${p.commonName ? ` (${p.commonName})` : ''}</div>
          <div class="search-meta">${meta}</div>
        </div>
        <div class="search-badge">${right.join(' • ')}</div>
      </div>`;
    }).join('');
    resultsBox.classList.add('open');
}

// click handler for search results (delegation)
if (resultsBox) {
    resultsBox.addEventListener('click', async (e) => {
        const item = e.target.closest('.search-item');
        if (!item) return;
        selectedPlantId = Number(item.dataset.id);
        plantSearch.value = '';
        resultsBox.classList.remove('open');
        // Request a live snapshot (POST) which the backend persists and returns snapshot DTO
        await loadSnapshot(selectedPlantId);
    });
}

// close results when clicking outside
document.addEventListener('click', (e) => {
    if (!resultsBox) return;
    if (!resultsBox.contains(e.target) && e.target !== plantSearch) {
        resultsBox.classList.remove('open');
    }
});

// ----------------- Snapshot capture & main data load -----------------
async function loadSnapshot(plantId) {
    try {
        const res = await apiFetch(`http://localhost:8080/plants/${plantId}/snapshot`, { method: 'POST' , withCredentials: true,});
        if (!res.ok) {
            console.error('Snapshot request failed', res.status);
            return;
        }
        const data = await res.json();
        // Render the returned snapshot DTO
        renderPlantMonitor(data);
    } catch (e) {
        console.error('snapshot error', e);
    }
}

// Optionally load list of monitored plants on page load (if you maintain such endpoint)
async function loadMonitorData() {
    try {
        const res = await apiFetch('http://localhost:8080/plants/monitor', { method: 'GET' });
        if (!res.ok) {
            console.warn('No monitor list available or failed to fetch list.');
            return;
        }
        monitorCache = await res.json();
        if (monitorCache.length > 0) {
            // render the most recent monitor (or choose first)
            const lastMonitor = monitorCache[monitorCache.length - 1];
            selectedPlantId = lastMonitor.plantId ?? selectedPlantId;
            renderPlantMonitor(lastMonitor);
        } else {
            clearMonitorUI();
        }
    } catch (err) {
        console.error('Monitor load error:', err);
    }
}

function clearMonitorUI() {
    const title = document.getElementById('plant-title-heading');
    if (title) title.textContent = 'Plant Health Analysis';
    const zone = document.getElementById('plant-zone');
    if (zone) zone.textContent = 'No plant selected';
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

// ----------------- Render plant monitor (snapshot DTO) -----------------
function renderPlantMonitor(plant) {
    if (!plant) return;
    const sci = plant.scientificName || 'Plant';
    const common = plant.commonName ? ` (${plant.commonName})` : '';
    const titleEl = document.getElementById('plant-title-heading');
    if (titleEl) titleEl.textContent = `${sci}${common}`;
    const zoneEl = document.getElementById('plant-zone');
    if (zoneEl) zoneEl.textContent = `${sci} • (${plant.latitude?.toFixed?.(4) || '?'}, ${plant.longitude?.toFixed?.(4) || '?'})`;

    const filename = plant.imagePath?.split('/').pop();
    if (filename) {
        const imgEl = document.getElementById('plant-image');
        if (imgEl) imgEl.src = `http://localhost:8080/uploads/${filename}`;
    }

    // Optional stat cells with predictable IDs
    if (plant.plantedDate) {
        const statAge = document.getElementById('stat-age');
        if (statAge) statAge.textContent = computePlantAge(plant.plantedDate);
    }
    if (plant.optimal?.daysToHarvest != null) {
        const sh = document.getElementById('stat-harvest');
        if (sh) sh.textContent = `${plant.optimal.daysToHarvest} days`;
    }
    if (plant.optimal?.yieldPredictionKg != null) {
        const sy = document.getElementById('stat-yield');
        if (sy) sy.textContent = `${plant.optimal.yieldPredictionKg} kg`;
    }

    // Weather & sensors
    const w = plant.weather || {};
    setDetailValue('wind_speed-detail', w.wind != null ? `${formatNumber(w.wind)} km/h` : '--');
    setDetailValue('precipitation-detail', w.precipitation != null ? `${formatNumber(w.precipitation)} mm` : '--');
    setDetailValue('uv_index-detail', w.uvIndex != null ? `${formatNumber(w.uvIndex)}` : '--');
    setDetailValue('cloud_cover-detail', w.cloudCover != null ? `${formatNumber(w.cloudCover)}%` : '--');
    setDetailValue('evapotranspiration-detail', w.evapotranspiration != null ? `${formatNumber(w.evapotranspiration)} mm/day` : '--');
    setDetailValue('pressure-detail', w.pressure != null ? `${formatNumber(w.pressure)} hPa` : '--');

    setMetricValue('temperature-metric', w.airTemperature != null ? `${formatNumber(w.airTemperature)}°C` : '--');
    setMetricValue('humidity-metric', w.airHumidity != null ? `${formatNumber(w.airHumidity)}%` : '--');

    const s = plant.sensor || {};
    setMetricValue('soil_moisture-metric', s.soilMoisture != null ? `${formatNumber(s.soilMoisture)}%` : '--');
    setMetricValue('light_intensity-metric', s.lightIntensity != null ? `${formatNumber(s.lightIntensity)} lux` : '--');

    // Build optimal conditions map for comparison modal
    buildOptimalConditionsMap(plant);
}

// compute plant age in days
function computePlantAge(plantedDateStr) {
    const start = new Date(plantedDateStr);
    const now = new Date();
    if (isNaN(start.getTime())) return '--';
    const diffDays = Math.floor((now - start) / (1000 * 60 * 60 * 24));
    return `${diffDays} days`;
}

// ----------------- Optimal conditions map & helpers -----------------
function buildOptimalConditionsMap(plant) {
    const opt = plant.optimal || {};
    const w = plant.weather || {};
    const s = plant.sensor || {};

    function rangeOrValue(center, unit) {
        if (center == null) return '--';
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
            optimal: opt.idealSoilMoisture ? rangeOrValue(opt.idealSoilMoisture, '%') : '50-70%',
            current: s.soilMoisture != null ? `${formatNumber(s.soilMoisture)}%` : '--',
            status: qualitative(s.soilMoisture, opt.idealSoilMoisture ? opt.idealSoilMoisture - 10 : 50, opt.idealSoilMoisture ? opt.idealSoilMoisture + 10 : 70),
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

// ----------------- Comparison modal -----------------
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
    titleEl && (titleEl.textContent = `${labelEl ? labelEl.textContent : type} Analysis`);

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
    modal && modal.classList.add('visible');
}

function closeComparison() {
    const modal = document.getElementById('comparisonModal');
    modal && modal.classList.remove('visible');
}

// modal close handlers
document.addEventListener('click', (e) => {
    const modal = document.getElementById('comparisonModal');
    if (!modal) return;
    if (e.target === modal) closeComparison();
});
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') closeComparison();
});

// ----------------- Health toggle -----------------
let isDiseaseMode = false;
function toggleHealthMode() {
    const healthContent = document.getElementById('healthContent');
    const healthScore = document.getElementById('healthScore');
    isDiseaseMode = !isDiseaseMode;
    if (!healthScore || !healthContent) return;
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

// ----------------- Refresh button -----------------
async function refreshData() {
    const refreshBtn = document.getElementById('refreshBtn');
    const refreshIcon = refreshBtn ? refreshBtn.querySelector('.fa-sync-alt') : null;
    if (refreshBtn) refreshBtn.disabled = true;
    if (refreshIcon) refreshIcon.style.animation = 'spin 1s linear infinite';
    try {
        if (selectedPlantId) {
            await loadSnapshot(selectedPlantId);
        } else {
            console.warn('Select a plant to refresh its live data.');
            // Optionally load monitor data as fallback:
            await loadMonitorData();
        }
    } finally {
        setTimeout(() => {
            if (refreshBtn) refreshBtn.disabled = false;
            if (refreshIcon) refreshIcon.style.animation = '';
        }, 500);
    }
}

// ----------------- Settings stub -----------------
function showSettings() {
    alert('⚙️ Settings Panel Opened');
}

// ----------------- Startup -----------------
document.addEventListener('DOMContentLoaded', async () => {
    // Theme init
    let currentTheme = 'dark';
    try { currentTheme = localStorage.getItem('theme') || 'dark'; } catch (e) { currentTheme = 'dark'; }
    setTheme(currentTheme);
    const darkBtn = document.getElementById('dark-btn');
    const lightBtn = document.getElementById('light-btn');
    if (darkBtn) darkBtn.addEventListener('click', () => setTheme('dark'));
    if (lightBtn) lightBtn.addEventListener('click', () => setTheme('light'));

    // wire global search Enter (if you also have an Enter-based global search input)
    const globalSearch = document.getElementById('global-search');
    if (globalSearch) {
        globalSearch.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                const term = globalSearch.value.trim().toLowerCase();
                if (!term) return;
                // If monitorCache is populated, search local; otherwise try server search
                const found = monitorCache.find((p) =>
                    (p.scientificName && p.scientificName.toLowerCase().includes(term)) ||
                    (p.commonName && p.commonName.toLowerCase().includes(term))
                );
                if (found) {
                    selectedPlantId = found.plantId;
                    renderPlantMonitor(found);
                    document.querySelector('.main-panel')?.scrollIntoView({ behavior: 'smooth' });
                } else {
                    // fallback: perform server search -> pick first
                    doSearch(term).catch(() => alert('Plant not found'));
                }
            }
        });
    }

    // Add entrance animation
    animateCards();

    // Load monitor data once on start (will call refresh to get token)
    try {
        await ensureAccessToken();
        await loadMonitorData();
    } catch (e) {
        console.warn('Startup token/monitor load may have failed. User likely redirected to login.');
    }
});
