/**
 * Auction House Simulator - Main JavaScript
 */

document.addEventListener('DOMContentLoaded', function() {

    // Auto-dismiss alerts after 5 seconds
    const alerts = document.querySelectorAll('.alert-dismissible');
    alerts.forEach(function(alert) {
        setTimeout(function() {
            const bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
            if (bsAlert) bsAlert.close();
        }, 5000);
    });

    // Bid form validation with double-click prevention
    const bidForm = document.getElementById('bidForm');
    if (bidForm) {
        let isSubmitting = false;
        
        bidForm.addEventListener('submit', function(e) {
            // Prevent double-click submissions
            if (isSubmitting) {
                e.preventDefault();
                return false;
            }
            
            const amountInput = bidForm.querySelector('input[name="amount"]');
            const submitButton = bidForm.querySelector('button[type="submit"]');
            const minBid = parseFloat(amountInput.min);
            let amount = parseFloat(amountInput.value);

            // Validation: Must be a valid number
            if (isNaN(amount)) {
                e.preventDefault();
                alert('Please enter a valid bid amount.');
                return false;
            }

            // Validation: Must be positive
            if (amount <= 0) {
                e.preventDefault();
                alert('Bid amount must be greater than zero.');
                return false;
            }

            // Validation: Must meet minimum bid
            if (amount < minBid) {
                e.preventDefault();
                alert('Please enter a bid of at least $' + minBid.toFixed(2));
                return false;
            }

            // Round to 2 decimal places to prevent floating point issues
            amount = Math.round(amount * 100) / 100;
            amountInput.value = amount;

            // Confirm bid
            const confirmed = confirm('Place a bid of $' + amount.toFixed(2) + '?');
            if (!confirmed) {
                e.preventDefault();
                return false;
            }

            // Disable button to prevent double-clicks
            isSubmitting = true;
            submitButton.disabled = true;
            submitButton.innerHTML = '<span class="material-icons-outlined text-lg animate-spin">hourglass_empty</span> Placing Bid...';
            
            // Re-enable after 3 seconds in case of network issues
            setTimeout(() => {
                isSubmitting = false;
                submitButton.disabled = false;
                submitButton.innerHTML = '<span class="material-icons-outlined text-lg">gavel</span> Place Bid';
            }, 3000);
        });
    }

    // Profile picture preview
    const picInput = document.querySelector('input[name="profilePic"]');
    if (picInput) {
        picInput.addEventListener('change', function() {
            if (this.files && this.files[0]) {
                const file = this.files[0];
                if (file.size > 5 * 1024 * 1024) {
                    alert('File size must be less than 5MB');
                    this.value = '';
                    return;
                }
                if (!file.type.match('image/(jpeg|png|gif|webp)')) {
                    alert('Only JPEG, PNG, GIF, and WebP images are allowed');
                    this.value = '';
                    return;
                }
            }
        });
    }

    // Countdown timers on listing page
    const countdowns = document.querySelectorAll('[data-countdown]');
    countdowns.forEach(function(el) {
        const seconds = parseInt(el.dataset.countdown);
        if (seconds > 0) {
            startCountdown(el, seconds);
        }
    });

    // Search form enhancement
    const searchInputs = document.querySelectorAll('input[name="search"]');
    searchInputs.forEach(function(input) {
        input.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                this.closest('form').submit();
            }
        });
    });

    // Notification badge update
    updateNotificationBadge();
});

/**
 * Start a countdown timer on an element.
 */
function startCountdown(el, totalSeconds) {
    let remaining = totalSeconds;

    function update() {
        if (remaining <= 0) {
            el.textContent = 'Ended';
            el.classList.add('text-danger');
            return;
        }

        const days = Math.floor(remaining / 86400);
        const hours = Math.floor((remaining % 86400) / 3600);
        const minutes = Math.floor((remaining % 3600) / 60);
        const secs = remaining % 60;

        let display = '';
        if (days > 0) display = days + 'd ' + hours + 'h';
        else if (hours > 0) display = hours + 'h ' + minutes + 'm';
        else display = minutes + 'm ' + secs + 's';

        el.textContent = display;
        if (remaining < 3600) el.classList.add('text-danger');
        remaining--;
    }

    update();
    setInterval(update, 1000);
}

/**
 * Update notification badge via simple check.
 */
function updateNotificationBadge() {
    // Could be enhanced with AJAX polling in the future
}
