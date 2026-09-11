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

    // Bid form validation
    const bidForm = document.getElementById('bidForm');
    if (bidForm) {
        bidForm.addEventListener('submit', function(e) {
            const amountInput = bidForm.querySelector('input[name="amount"]');
            const minBid = parseFloat(amountInput.min);
            const amount = parseFloat(amountInput.value);

            if (isNaN(amount) || amount < minBid) {
                e.preventDefault();
                alert('Please enter a bid of at least $' + minBid.toFixed(2));
                return false;
            }

            // Confirm bid
            const confirmed = confirm('Place a bid of $' + amount.toFixed(2) + '?');
            if (!confirmed) {
                e.preventDefault();
                return false;
            }
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
