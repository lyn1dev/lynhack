package com.lynhack.client;

import com.lynhack.LynHack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class LynHackClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LynHack.LOGGER.info("[{}] Initializing client utility subsystems...", LynHack.MOD_NAME);
    }
}
