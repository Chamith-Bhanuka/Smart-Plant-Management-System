document.addEventListener('DOMContentLoaded', () => {
    const interfaceContainer = document.getElementById('leaf-interface');
    const secondaryVeins = document.querySelectorAll('.secondary-vein');

    const featureData = [
        {
            id: 'monitor',
            icon: 'fa-chart-line',
            title: 'Real-Time Monitoring',
            description: 'Access comprehensive live sensor data for soil conditions, climate parameters, and plant vitality metrics with instant alerts and custom dashboards.',
            position: { x: '31%', y: '31%' },
            label: 'Monitor'
        },
        {
            id: 'chat',
            icon: 'fa-robot',
            title: 'AI Agronomist',
            description: 'Interact with our advanced AI system for instant agricultural insights, predictive analytics, and personalized crop management recommendations.',
            position: { x: '28%', y: '48%' },
            label: 'AI Assistant'
        },
        {
            id: 'disease',
            icon: 'fa-microscope',
            title: 'Disease Detection',
            description: 'Upload plant imagery for AI-powered disease identification, pest analysis, and automated health assessment with treatment suggestions.',
            position: { x: '25%', y: '67%' },
            label: 'Diagnostics'
        },
        {
            id: 'expert',
            icon: 'fa-user-tie',
            title: 'Expert Network',
            description: 'Connect with certified agricultural specialists, schedule video consultations, and access expert knowledge for complex farming challenges.',
            position: { x: '69%', y: '31%' },
            label: 'Experts'
        },
        {
            id: 'articles',
            icon: 'fa-graduation-cap',
            title: 'Knowledge Hub',
            description: 'Explore our extensive library of farming guides, research papers, best practices, and cutting-edge agricultural innovations and techniques.',
            position: { x: '72%', y: '48%' },
            label: 'Learning'
        },
        {
            id: 'reports',
            icon: 'fa-chart-area',
            title: 'Analytics Suite',
            description: 'Generate comprehensive reports on crop performance, yield predictions, and sustainability metrics for data-driven farming decisions.',
            position: { x: '75%', y: '67%' },
            label: 'Analytics'
        }
    ];

    // leaf nodes
    featureData.forEach((feature, index) => {
        const node = document.createElement('div');
        node.className = 'node';
        node.innerHTML = `<span class="node-label">${feature.label}</span><i class="fas ${feature.icon}"></i>`;
        node.style.left = feature.position.x;
        node.style.top = feature.position.y;

        // Create hover card
        const hoverCard = document.createElement('div');
        hoverCard.className = 'hover-card';
        hoverCard.innerHTML = `
        <h3><i class="fas ${feature.icon}"></i> ${feature.title}</h3>
        <p>${feature.description}</p>
      `;

        interfaceContainer.appendChild(node);
        interfaceContainer.appendChild(hoverCard);

        const veinIndex = index >= 3 ? index + 1 : index;
        const correspondingVein = secondaryVeins[veinIndex];

        // Calculate card position to avoid conflicts
        const nodeRect = {
            x: parseFloat(feature.position.x),
            y: parseFloat(feature.position.y)
        };

        // Position card based on node location
        if (nodeRect.x < 50) { // Left side nodes
            hoverCard.style.left = `${nodeRect.x + 15}%`;
            hoverCard.style.top = `${Math.max(5, nodeRect.y - 15)}%`;
        } else { // Right side nodes
            hoverCard.style.right = `${100 - nodeRect.x + 15}%`;
            hoverCard.style.top = `${Math.max(5, nodeRect.y - 15)}%`;
        }

        node.addEventListener('mouseenter', () => {
            interfaceContainer.classList.add('focused');
            node.classList.add('active');
            if (correspondingVein) correspondingVein.classList.add('active');
            hoverCard.classList.add('visible');
        });

        node.addEventListener('mouseleave', () => {
            interfaceContainer.classList.remove('focused');
            node.classList.remove('active');
            if (correspondingVein) correspondingVein.classList.remove('active');
            hoverCard.classList.remove('visible');
        });

        node.addEventListener('click', () => {
            showModal(`Launching Module`, `🌱 The ${feature.title} module is now loading...`);
        });

        node.addEventListener('click', () => {
            switch (feature.id) {
                case 'monitor':
                    handleMonitorClick();
                    break;
                case 'chat':
                    handleAIClick();
                    break;
                case 'disease':
                    handleDiagnosticsClick();
                    break;
                case 'expert':
                    handleExpertClick();
                    break;
                case 'articles':
                    handleLearningClick();
                    break;
                case 'reports':
                    handleAnalyticsClick();
                    break;
            }
        });
    });

    // setup wizard button
    const setupWizardBtn = document.getElementById('setup-wizard-btn');
    setupWizardBtn.addEventListener('click', () => {
        showModal('Setup Wizard', '🚀 Welcome to SmartFarm Setup! We\'ll guide you through configuring your smart farming system step by step.');
    });

    // theme toggle
    const themeToggle = document.getElementById('theme-toggle');
    const htmlEl = document.documentElement;

    // Try to get saved theme, but don't rely on localStorage
    let savedTheme = 'dark';
    try {
        savedTheme = localStorage.getItem('theme') || 'dark';
    } catch (e) {
        // Fallback if localStorage is not available
        savedTheme = 'dark';
    }

    htmlEl.setAttribute('data-theme', savedTheme);
    themeToggle.checked = savedTheme === 'light';

    themeToggle.addEventListener('change', () => {
        const newTheme = themeToggle.checked ? 'light' : 'dark';
        htmlEl.setAttribute('data-theme', newTheme);

        // Try to save theme, but continue if localStorage is not available
        try {
            localStorage.setItem('theme', newTheme);
        } catch (e) {
            // Continue without saving if localStorage is not available
            console.log('Theme preference could not be saved');
        }
    });

    // CUSTOM MODAL FUNCTIONALITY
    const modalOverlay = document.getElementById('custom-modal-overlay');
    const modalTitle = document.getElementById('modal-title');
    const modalMessage = document.getElementById('modal-message');
    const modalCloseBtn = document.getElementById('modal-close-btn');

    function showModal(title, message) {
        modalTitle.textContent = title;
        modalMessage.textContent = message;
        modalOverlay.classList.add('visible');
    }

    function hideModal() {
        modalOverlay.classList.remove('visible');
    }

    modalCloseBtn.addEventListener('click', hideModal);
    modalOverlay.addEventListener('click', (event) => {
        if (event.target === modalOverlay) hideModal();
    });


    // bee animation
    const beeContainer = document.getElementById('bee-container');
    const messages = [
        "New soil data available",
        "Pest alert in Zone B",
        "Irrigation cycle complete",
        "Rain expected",
        "Harvest time approaching",
        "Low moisture detected",
        "Temperature rising",
        "Crop health optimal"
    ];

    const createRealisticBee = () => {
        const bee = document.createElement('div');
        bee.className = 'bee';

        const beeBody = document.createElement('div');
        beeBody.className = 'bee-body';

        const wings = document.createElement('div');
        wings.className = 'bee-wings';
        wings.innerHTML = `
        <div class="wing left"></div>
        <div class="wing right"></div>
      `;

        const notice = document.createElement('div');
        notice.className = 'bee-notice';
        notice.textContent = messages[Math.floor(Math.random() * messages.length)];

        bee.appendChild(beeBody);
        bee.appendChild(wings);
        bee.appendChild(notice);

        // Random flight path
        const startX = Math.random() > 0.5 ? -50 : window.innerWidth + 50;
        const endX = startX < 0 ? window.innerWidth + 50 : -50;
        const startY = Math.random() * (window.innerHeight * 0.6) + window.innerHeight * 0.2;
        const endY = Math.random() * (window.innerHeight * 0.6) + window.innerHeight * 0.2;
        const duration = Math.random() * 8 + 12; // 12-20 seconds

        // Set CSS custom properties for flight path
        bee.style.setProperty('--start-x', `${startX}px`);
        bee.style.setProperty('--end-x', `${endX}px`);
        bee.style.setProperty('--start-y', `${startY}px`);
        bee.style.setProperty('--end-y', `${endY}px`);
        bee.style.animationDuration = `${duration}s`;

        // Add subtle flight wobble
        bee.style.transform = `rotate(${Math.random() * 10 - 5}deg)`;

        beeContainer.appendChild(bee);

        // Show notice after bee flies for a bit
        const noticeDelay = duration * 0.3 + Math.random() * 2;
        setTimeout(() => {
            notice.classList.add('visible');
            // Hide notice after 3 seconds
            setTimeout(() => {
                notice.classList.remove('visible');
            }, 3000);
        }, noticeDelay * 1000);

        // Remove bee after animation
        setTimeout(() => {
            bee.remove();
        }, duration * 1000);
    };

    // Create initial bees and continuous spawning
    for(let i = 0; i < 2; i++) {
        setTimeout(() => createRealisticBee(), i * 3000);
    }

    // Spawn new bee every 8-15 seconds
    setInterval(() => {
        createRealisticBee();
    }, Math.random() * 7000 + 8000);

    // Additional random bee spawning for more natural feel
    const randomSpawn = () => {
        if (Math.random() < 0.3) { // 30% chance
            createRealisticBee();
        }
        setTimeout(randomSpawn, Math.random() * 10000 + 5000);
    };
    setTimeout(randomSpawn, 10000);

    function handleMonitorClick() {
        console.log("Monitor clicked!");
    }

    function handleAIClick() {
        console.log("AI Assistant clicked!");
    }

    function handleDiagnosticsClick() {
        console.log("Diagnostics clicked!");
    }

    function handleExpertClick() {
        console.log("Expert clicked!");
    }

    function handleLearningClick() {
        console.log("Learning clicked!");
    }

    function handleAnalyticsClick() {
        console.log("Analytics clicked!");
    }
});