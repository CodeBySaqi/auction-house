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
        // POPUP anchored to the emoji button: position:absolute INSIDE the
        // button's .emoji-anchor wrapper (position:relative), so it pops up
        // directly above the icon via bottom:calc(100% + 8px); right:0 and can
        // never detach onto the page/body. No page-level coordinates anywhere.
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

    var PICKER_W = 280;  // popup width (see .emoji-picker-popup cssText)
    var PICKER_H = 250;  // max-height 220 + padding/border/8px gap estimate

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

        // Pop up INSIDE the button's relative wrapper -> structurally anchored
        // to the icon: 8px above it, right edges aligned. It follows the button
        // through any scroll with zero coordinate math.
        var anchorWrap = anchorEl.closest('.emoji-anchor');
        anchorWrap.appendChild(picker);

        // getBoundingClientRect() is used ONLY to pick the open direction/side
        // (never for coordinates), so the popup respects the viewport edges:
        var rect = anchorEl.getBoundingClientRect();
        var fitsAbove = rect.top >= PICKER_H;
        var fitsBelow = (rect.bottom + PICKER_H) <= window.innerHeight;
        var fitsLeft  = rect.right >= (PICKER_W + 8);   // room to extend leftwards
        var fitsRight = (rect.left + PICKER_W + 8) <= window.innerWidth;

        if (!fitsAbove && fitsBelow) {
            // Not enough room above the icon -> pop up below it instead
            picker.style.bottom = 'auto';
            picker.style.top = 'calc(100% + 8px)';
        } else {
            picker.style.bottom = 'calc(100% + 8px)';
            picker.style.top = 'auto';
        }

        if (fitsLeft || !fitsRight) {
            picker.style.right = '0px';
            picker.style.left = 'auto';
        } else {
            // Button too close to the left viewport edge: extend rightwards
            picker.style.right = 'auto';
            picker.style.left = '0px';
        }

        activePicker = picker;
    }

    function createEmojiButton(inputEl) {
        // Wrapper gives the picker a proper positioning context and keeps the
        // button and popup together as one unit (no page-level positioning).
        var wrap = document.createElement('span');
        wrap.className = 'emoji-anchor';
        wrap.style.cssText = 'position:relative;display:inline-flex;flex-shrink:0;';

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

        wrap.appendChild(btn);
        return wrap;
    }

    // Public API
    window.EmojiPicker = {
        attach: function(inputEl) {
            if (!inputEl || inputEl.dataset.emojiAttached) return;
            inputEl.dataset.emojiAttached = 'true';

            // Emoji button in the input row, before the Send button (unchanged)
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

    // Close popup on outside click (clicks on the button/popup/wrapper are ignored)
    document.addEventListener('click', function(e) {
        if (activePicker
                && !activePicker.contains(e.target)
                && !e.target.closest('.emoji-trigger-btn')
                && !e.target.closest('.emoji-anchor')) {
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
