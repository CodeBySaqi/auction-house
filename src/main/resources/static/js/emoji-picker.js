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
        var box = document.createElement('div');
        box.className = 'emoji-picker-box';
        // SEPARATE BOX in the normal page flow: it is inserted INSIDE the chat
        // component (directly above the message input row). display:none when
        // closed, grid when open. No position:fixed/absolute, no coordinates,
        // no <body> attachment — it physically cannot appear anywhere else.
        box.style.cssText = 'display:none;background:white;border:1px solid #dadce0;border-radius:16px;box-shadow:0 4px 16px rgba(0,0,0,.12);padding:8px;width:100%;max-width:360px;max-height:220px;overflow-y:auto;margin:0 auto 10px;grid-template-columns:repeat(8,1fr);gap:2px;';

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
            box.appendChild(btn);
        });

        return box;
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
            activePicker.style.display = 'none';
            activePicker = null;
        }
    }

    function togglePicker(inputEl) {
        if (activePicker) {
            closePicker();
            return;
        }
        var picker = pickers.get(inputEl);
        if (!picker) return;
        picker.style.display = 'grid'; // open the box (in flow, above the input)
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
            togglePicker(inputEl);
        };

        wrap.appendChild(btn);
        return wrap;
    }

    // One picker box per input, created lazily
    var pickers = new Map();

    function getOrCreatePicker(inputEl) {
        var picker = pickers.get(inputEl);
        if (picker) return picker;
        picker = createPicker(inputEl);
        pickers.set(inputEl, picker);
        return picker;
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

            // The SEPARATE BOX: a normal in-flow block inserted INSIDE the chat
            // component, directly ABOVE the message input row (before the form).
            // It is part of the page layout, so it can only ever appear here —
            // never at the top/right of the page, regardless of scroll,
            // transforms, or ancestor positioning/overflow.
            var box = getOrCreatePicker(inputEl);
            if (form) {
                form.parentNode.insertBefore(box, form);
            } else {
                inputEl.parentNode.insertBefore(box, inputEl);
            }
        }
    };

    // Auto-attach to elements with class "emoji-enabled"
    function autoAttach() {
        document.querySelectorAll('.emoji-enabled').forEach(function(el) {
            EmojiPicker.attach(el);
        });
    }

    // Close the box on outside click (clicks on the emoji button/box are ignored)
    document.addEventListener('click', function(e) {
        if (activePicker
                && !activePicker.contains(e.target)
                && !e.target.closest('.emoji-trigger-btn')) {
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
