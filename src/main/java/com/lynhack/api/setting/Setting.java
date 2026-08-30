package com.lynhack.api.setting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class Setting<T> {
    private final String name;
    private final String description;
    private T value;
    private final T defaultValue;
    private final List<Consumer<T>> changeListeners = new ArrayList<>();

    public Setting(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
        for (Consumer<T> listener : changeListeners) {
            listener.accept(value);
        }
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public void reset() {
        setValue(defaultValue);
    }

    public void addListener(Consumer<T> listener) {
        changeListeners.add(listener);
    }
}
