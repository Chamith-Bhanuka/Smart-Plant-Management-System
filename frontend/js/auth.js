document.addEventListener('DOMContentLoaded', () => {
    // theme toggle
    const themeToggle = document.getElementById('theme-toggle');
    const themeIcon = document.getElementById('theme-icon');
    const htmlEl = document.documentElement;
    const savedTheme = localStorage.getItem('theme') || 'dark';
    htmlEl.setAttribute('data-theme', savedTheme);

    const updateThemeIcon = (theme) => {
        themeIcon.className = theme === 'dark' ? 'fas fa-sun' : 'fas fa-moon';
    };
    updateThemeIcon(savedTheme);

    themeToggle.addEventListener('click', () => {
        const currentTheme = htmlEl.getAttribute('data-theme');
        const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
        htmlEl.setAttribute('data-theme', newTheme);
        localStorage.setItem('theme', newTheme);
        updateThemeIcon(newTheme);
    });

    // form toggle
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

    // sign up
    const signupForm = document.querySelector('#signup-form form');

    signupForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const email = document.getElementById('signup-email').value.trim();
        const password = document.getElementById('signup-password').value.trim();
        const confirmPassword = document.getElementById('signup-confirm-password').value.trim();
        const role = document.querySelector('input[name="role"]:checked')?.value;

        if (!email || !password || !confirmPassword || !role) {
            alert("Please fill in all fields.");
            return;
        }

        if (password !== confirmPassword) {
            alert("Passwords do not match.");
            return;
        }

        const payload = {
            email,
            password,
            role: role.toUpperCase()
        };

        try {
            const response = await fetch('http://localhost:8080/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const result = await response.json();

            if (response.ok) {
                alert(result.message || "Registration successful!");
                signupForm.reset();
                showSignInLink.click();
            } else {
                alert(result.message || "Registration failed.");
            }
        } catch (error) {
            console.error("Signup error:", error);
            alert("Something went wrong. Please try again.");
        }
    });

    // auto scrolling
    const roleInputs = document.querySelectorAll('input[name="role"]');
    const formWrapper = document.querySelector('.form-wrapper');

    roleInputs.forEach(input => {
        input.addEventListener('change', () => {
            if (input.checked) {
                setTimeout(() => {
                    const submitBtn = document.querySelector('#signup-form .submit-btn');
                    if (submitBtn) {
                        const submitBtnRect = submitBtn.getBoundingClientRect();
                        const formWrapperRect = formWrapper.getBoundingClientRect();

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
});
