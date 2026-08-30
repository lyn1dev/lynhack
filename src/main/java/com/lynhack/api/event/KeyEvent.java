package com.lynhack.api.event;

public class KeyEvent extends Event {
    private final int key;
    private final int scancode;
    private final int action;
    private final int modifiers;

    public KeyEvent(int key, int scancode, int action, int modifiers) {
        this.key = key;
        this.scancode = scancode;
        this.action = action;
        this.modifiers = modifiers;
    }

    public int getKey() {
        return key;
    }

    public int getScancode() {
        return scancode;
    }

    public int getAction() {
        return action;
    }

    public int getModifiers() {
        return modifiers;
    }

    public boolean isPress() {
        return action == 1; // GLFW.GLFW_PRESS
    }

    public boolean isRelease() {
        return action == 0; // GLFW.GLFW_RELEASE
    }

    public boolean isRepeat() {
        return action == 2; // GLFW.GLFW_REPEAT
    }
}
