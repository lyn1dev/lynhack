package com.lynhack.client;

import com.lynhack.LynHack;
import com.lynhack.api.event.EventBus;
import com.lynhack.api.input.InputManager;
import com.lynhack.api.module.ModuleManager;
import com.lynhack.api.state.ClientState;
import com.lynhack.modules.movement.SampleModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

@Environment(EnvType.CLIENT)
public class LynHackClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LynHack.LOGGER.info("[{}] Initializing LynHack client subsystems...", LynHack.MOD_NAME);

        // 1. Register InputManager to process monotonic frame queue
        EventBus.getInstance().registerFrameListener(InputManager.getInstance());

        // 2. Hook WorldRenderEvents.START_MAIN for per-frame ClientState caching & dispatch
        WorldRenderEvents.START_MAIN.register(context -> ClientState.update());

        // 3. Register default modules
        ModuleManager.getInstance().register(new SampleModule());

        LynHack.LOGGER.info("[{}] Subsystems initialized successfully! ClickGUI bound to RIGHT SHIFT.", LynHack.MOD_NAME);
    }
}
