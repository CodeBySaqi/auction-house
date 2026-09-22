/**
 * Lightweight Emoji Picker for chat inputs.
 * Auto-attaches to any input/textarea with class "emoji-enabled"
 * or by calling EmojiPicker.attach(inputElement).
 */
(function() {
    'use strict';

    var EMOJIS = [
        // Smileys
        '😀','😃','😄','😁','😆','😅','🤣','😂','🙂','😊',
        '😇','🥰','😍','🤩','😘','😗','😚','😙','🥲','😋',
        '😛','😜','🤪','😝','🤑','🤗','🤭','🤫','🤔','🫡',
        '🤐','🤨','😐','😑','😶','🫥','😏','😒','🙄','😬',
        '🤥','😌','😔','😪','🤤','😴','😷','🤒','🤕','🤢',
        '🤮','🥵','🥶','🥴','😵','🤯','🤠','🥳','🥸','😎',
        // Gestures
        '👍','👎','👏','🙌','🤝','🙏','✌️','🤞','🤟','🤘',
        '👌','🤌','👈','👉','👆','👇','☝️','✋','🤚','🖐️',
        // Hearts
        '❤️','🧡','💛','💚','💙','💜','🖤','🤍','🤎','💔',
        '❣️','💕','💞','💓','💗','💖','💘','💝','💟','♥️',
        // Objects
        '🔥','⭐','🌟','✨','💫','🎉','🎊','🏆','🥇','🎯',
        '💰','💵','💎','🎁','🛒','📦','📱','💻','⌚','📷',
        // Nature
        '🌹','🌺','🌸','🌻','🌼','🍀','🌈','☀️','🌙','⚡',
        // Animals
        '🐶','🐱','🦊','🐻','🐼','🐨','🦁','🐯','🐸','🐵',
        // Food
        '🍕','🍔','🍟','🌮','🍣','🍰','🎂','🍩','☕','🍺',
        // Symbols
        '✅','❌','⚠️','💯','🔴','🟢','🔵','⬆️','⬇️','➡️',
        '💬','💭','🗯️','👀','🫶','💪','🤝','🫡','🎶','🎵'
    ];

    var activePicker = null;

    function createPicker(inputEl) {
        var wrapper = document.createElement('div');
        wrapper.className = 'emoji-picker-popup';
        wrapper.style.cssText = 'position:absolute;z-index:9999;background:white;border:1px solid #dadce0;border-radius:16px;box-shadow:0 4px 16px rgba(0,0,0,.15);padding:8px;width:280px;max-height:220px;overflow-y:auto;display:grid;grid-template-columns:repeat(8,1fr);gap:2px;';

        EMOJIS.forEach(function(emoji) {
            var btn = document.createElement('button');
            btn.type = 'button';
            btn.textContent = emoji;
            btn.style.cssText = 'border:none;background:none;font-size:20px;padding:4px;cursor:pointer;border-radius:8px;transition:background .15s;line-height:1;';
            btn.onmouseenter = function() { this.style.background = '#f1f3f4'; };
            btn.onmouseleave = function() { this.style.background = 'none'; };
            btn.onclick = function(e) {
                e.preventDefault();
                e.stopPropagation();
                insertEmoji(inputEl, emoji);
                closePicker();
            };
            wrapper.appendChild(btn);
        });

        return wrapper;
    }

    function insertEmoji(inputEl, emoji) {
        inputEl.focus();
        var start = inputEl.selectionStart || 0;
        var end = inputEl.selectionEnd || 0;
        var val = inputEl.value;
        inputEl.value = val.substring(0, start) + emoji + val.substring(end);
        var newPos = start + emoji.length;
        inputEl.selectionStart = newPos;
        inputEl.selectionEnd = newPos;
        // Trigger input event for any listeners
        inputEl.dispatchEvent(new Event('input', { bubbles: true }));
    }

    function closePicker() {
        if (activePicker) {
            activePicker.remove();
            activePicker = null;
        }
    }

    function togglePicker(inputEl, anchorEl) {
        if (activePicker) {
            closePicker();
            return;
        }

        var picker = createPicker(inputEl);
        document.body.appendChild(picker);

        // Position above the anchor button
        var rect = anchorEl.getBoundingClientRect();
        picker.style.left = Math.max(8, rect.left - 120) + 'px';
        picker.style.bottom = (window.innerHeight - rect.top + 8) + 'px';
        picker.style.top = 'auto';

        activePicker = picker;
    }

    function createEmojiButton(inputEl) {
        var btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'emoji-trigger-btn';
        btn.title = 'Add emoji';
        btn.innerHTML = '<span style="font-size:20px;line-height:1;">😊</span>';
        btn.style.cssText = 'border:none;background:none;cursor:pointer;padding:6px;border-radius:50%;transition:background .15s;flex-shrink:0;display:flex;align-items:center;justify-content:center;';
        btn.onmouseenter = function() { this.style.background = '#f1f3f4'; };
        btn.onmouseleave = function() { this.style.background = 'none'; };
        btn.onclick = function(e) {
            e.preventDefault();
            e.stopPropagation();
            togglePicker(inputEl, btn);
        };
        return btn;
    }

    // Public API
    window.EmojiPicker = {
        attach: function(inputEl) {
            if (!inputEl || inputEl.dataset.emojiAttached) return;
            inputEl.dataset.emojiAttached = 'true';

            // Insert emoji button before the input's parent form submit button
            var form = inputEl.closest('form');
            if (form) {
                var submitBtn = form.querySelector('button[type="submit"]');
                if (submitBtn) {
                    submitBtn.parentNode.insertBefore(createEmojiButton(inputEl), submitBtn);
                } else {
                    inputEl.parentNode.appendChild(createEmojiButton(inputEl));
                }
            } else {
                inputEl.parentNode.appendChild(createEmojiButton(inputEl));
            }
        }
    };

    // Auto-attach to elements with class "emoji-enabled"
    function autoAttach() {
        document.querySelectorAll('.emoji-enabled').forEach(function(el) {
            EmojiPicker.attach(el);
        });
    }

    // Close picker on outside click
    document.addEventListener('click', function(e) {
        if (activePicker && !activePicker.contains(e.target) && !e.target.closest('.emoji-trigger-btn')) {
            closePicker();
        }
    });

    // Run on DOM ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', autoAttach);
    } else {
        autoAttach();
    }

})();
