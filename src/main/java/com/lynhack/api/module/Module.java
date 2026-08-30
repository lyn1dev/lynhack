package com.lynhack.api.module;

import com.lynhack.api.event.EventBus;
import com.lynhack.api.event.FrameListener;
import com.lynhack.api.event.FrameUpdateEvent;
import com.lynhack.api.input.InputManager;
import com.lynhack.api.setting.KeybindSetting;
import com.lynhack.api.setting.Setting;

import java.util.ArrayList;
import java.util.List;

public abstract class Module implements FrameListener {
    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled = false;
    private final KeybindSetting keyBind;
    private final List<Setting<?>> settings = new ArrayList<>();

    public Module(String name, String description, Category category, int defaultKey) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.keyBind = new KeybindSetting("Keybind", "Key to toggle " + name, defaultKey);
        this.settings.add(this.keyBind);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) {
                EventBus.getInstance().registerFrameListener(this);
                onEnable();
            } else {
                EventBus.getInstance().unregisterFrameListener(this);
                onDisable();
                InputManager.getInstance().releaseAll(this);
            }
        }
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public KeybindSetting getKeyBind() {
        return keyBind;
    }

    public List<Setting<?>> getSettings() {
        return settings;
    }

    protected <T extends Setting<?>> T addSetting(T setting) {
        this.settings.add(setting);
        return setting;
    }

    protected void onEnable() {}
    protected void onDisable() {}

    @Override
    public void onFrame(FrameUpdateEvent event) {}
}
