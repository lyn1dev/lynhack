package com.lynhack.api.setting;

import java.util.List;

public class ModeSetting extends Setting<String> {
    private final List<String> modes;
    private int index;

    public ModeSetting(String name, String description, String defaultMode, String... modes) {
        super(name, description, defaultMode);
        this.modes = List.of(modes);
        this.index = this.modes.indexOf(defaultMode);
        if (this.index < 0) {
            this.index = 0;
            if (!this.modes.isEmpty()) {
                setValue(this.modes.get(0));
            }
        }
    }

    public List<String> getModes() {
        return modes;
    }

    public void cycle() {
        if (modes.isEmpty()) return;
        index = (index + 1) % modes.size();
        setValue(modes.get(index));
    }

    public boolean is(String mode) {
        return getValue() != null && getValue().equalsIgnoreCase(mode);
    }
}
