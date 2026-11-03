package com.limelight.binding.input;

import android.accessibilityservice.AccessibilityService;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import com.limelight.Game;

/**
 * TVML: Android (and especially Android TV) handles the Windows/Meta key in the
 * system window manager (e.g. Meta → Home / All apps), so a normal Activity never
 * receives it. Accessibility services with key event filtering see keys *before*
 * that system handling, so this service grabs the Meta key (and any key pressed
 * while Meta is held, e.g. Win+D) during an active stream and forwards it to the
 * Game activity, which sends it to the host PC.
 *
 * It must be enabled by the user in Settings → Accessibility. It does nothing
 * outside of an active, input-grabbing stream.
 */
public class MetaKeyInterceptService extends AccessibilityService {
    private static volatile Game target;

    /** Called by Game when it gains/loses focus while streaming. */
    public static void setTarget(Game game) {
        target = game;
    }

    public static void clearTarget(Game game) {
        if (target == game) {
            target = null;
        }
    }

    private static boolean isMetaKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_META_LEFT || keyCode == KeyEvent.KEYCODE_META_RIGHT;
    }

    /**
     * Some TV builds/key layouts deliver the keyboard's Windows key as KEYCODE_HOME
     * instead of META. Treat HOME coming from a full (alphabetic) keyboard as the
     * Windows key, but never touch the HOME button of the TV remote.
     */
    private static boolean isKeyboardHome(KeyEvent event) {
        if (event.getKeyCode() != KeyEvent.KEYCODE_HOME) {
            return false;
        }
        InputDevice dev = event.getDevice();
        return dev != null && !dev.isVirtual() &&
                dev.getKeyboardType() == InputDevice.KEYBOARD_TYPE_ALPHABETIC;
    }

    @Override
    protected boolean onKeyEvent(KeyEvent original) {
        KeyEvent ev = original;
        if (isKeyboardHome(original)) {
            ev = new KeyEvent(original.getDownTime(), original.getEventTime(), original.getAction(),
                    KeyEvent.KEYCODE_META_LEFT, original.getRepeatCount(), original.getMetaState(),
                    original.getDeviceId(), original.getScanCode(), original.getFlags(), original.getSource());
        }
        final KeyEvent event = ev;

        final Game game = target;
        if (game == null || !game.isCapturingMetaKey()) {
            return false;
        }

        if (!isMetaKey(event.getKeyCode()) && (event.getMetaState() & KeyEvent.META_META_ON) == 0) {
            // Not a Meta-related key: let it follow the normal path to the activity
            return false;
        }

        game.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    game.handleKeyDown(event);
                } else if (event.getAction() == KeyEvent.ACTION_UP) {
                    game.handleKeyUp(event);
                }
            }
        });
        return true;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        target = null;
        super.onDestroy();
    }
}
