document.addEventListener('DOMContentLoaded', () => {
    // --- SIMPLE THEME TOGGLE FUNCTIONALITY ---
    const themeToggle = document.getElementById('theme-toggle');
    const themeIcon = document.getElementById('theme-icon');
    const htmlEl = document.documentElement;

    // Get saved theme or default to dark
    const savedTheme = localStorage.getItem('theme') || 'dark';
    htmlEl.setAttribute('data-theme', savedTheme);

    // Update icon based on current theme
    const updateThemeIcon = (theme) => {
        themeIcon.className = theme === 'dark' ? 'fas fa-sun' : 'fas fa-moon';
    };
    updateThemeIcon(savedTheme);

    // Theme toggle event
    themeToggle.addEventListener('click', () => {
        const currentTheme = htmlEl.getAttribute('data-theme');
        const newTheme = currentTheme === 'dark' ? 'light' : 'dark';

        htmlEl.setAttribute('data-theme', newTheme);
        localStorage.setItem('theme', newTheme);
        updateThemeIcon(newTheme);
    });

    // --- FORM TOGGLE FUNCTIONALITY ---
    const signInForm = document.getElementById('signin-form');
    const signUpForm = document.getElementById('signup-form');
    const showSignUpLink = document.getElementById('show-signup');
    const showSignInLink = document.getElementById('show-signin');
    const authContainer = document.getElementById('auth-container');

    showSignUpLink.addEventListener('click', (e) => {
        e.preventDefault();
        signInForm.classList.add('hidden');
        signUpForm.classList.remove('hidden');
        authContainer.classList.add('signup-active');
    });

    showSignInLink.addEventListener('click', (e) => {
        e.preventDefault();
        signUpForm.classList.add('hidden');
        signInForm.classList.remove('hidden');
        authContainer.classList.remove('signup-active');
    });

    // Prevent form submission for this demo
    document.querySelectorAll('form').forEach(form => {
        form.addEventListener('submit', e => e.preventDefault());
    });
});

// --- AUTO-SCROLL AFTER ROLE SELECTION ---
const roleInputs = document.querySelectorAll('input[name="role"]');
const formWrapper = document.querySelector('.form-wrapper');

roleInputs.forEach(input => {
    input.addEventListener('change', () => {
        if (input.checked) {
            // Small delay to ensure the UI has updated
            setTimeout(() => {
                // Scroll the form wrapper to show the submit button
                const submitBtn = document.querySelector('#signup-form .submit-btn');
                if (submitBtn) {
                    // Calculate the position to scroll to
                    const submitBtnRect = submitBtn.getBoundingClientRect();
                    const formWrapperRect = formWrapper.getBoundingClientRect();

                    // If submit button is not fully visible, scroll to it
                    if (submitBtnRect.bottom > formWrapperRect.bottom) {
                        formWrapper.scrollTo({
                            top: formWrapper.scrollTop + (submitBtnRect.bottom - formWrapperRect.bottom) + 20,
                            behavior: 'smooth'
                        });
                    }
                }
            }, 150);
        }
    });
});