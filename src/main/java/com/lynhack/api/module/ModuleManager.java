package com.lynhack.api.module;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModuleManager {
    private static final ModuleManager INSTANCE = new ModuleManager();

    public static ModuleManager getInstance() {
        return INSTANCE;
    }

    private final List<Module> modules = new ArrayList<>();
    private final Map<Class<? extends Module>, Module> moduleClassMap = new ConcurrentHashMap<>();
    private final Map<String, Module> moduleNameMap = new ConcurrentHashMap<>();

    private ModuleManager() {}

    public void register(Module module) {
        if (module != null && !modules.contains(module)) {
            modules.add(module);
            moduleClassMap.put(module.getClass(), module);
            moduleNameMap.put(module.getName().toLowerCase(), module);
        }
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Module> getModulesByCategory(Category category) {
        List<Module> categoryModules = new ArrayList<>();
        for (Module m : modules) {
            if (m.getCategory() == category) {
                categoryModules.add(m);
            }
        }
        return categoryModules;
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(Class<T> clazz) {
        return (T) moduleClassMap.get(clazz);
    }

    public Module getModule(String name) {
        if (name == null) return null;
        return moduleNameMap.get(name.toLowerCase());
    }

    /**
     * Dispatches key press to registered module keybinds.
     */
    public void onKey(int key, int action) {
        if (action != GLFW.GLFW_PRESS || key == GLFW.GLFW_KEY_UNKNOWN) {
            return;
        }

        for (Module module : modules) {
            if (module.getKeyBind().getValue() == key) {
                module.toggle();
            }
        }
    }
}
