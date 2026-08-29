package com.lynhack;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LynHack implements ModInitializer {
    public static final String MOD_ID = "lynhack";
    public static final String MOD_NAME = "LynHack";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[{}] Initializing LynHack core...", MOD_NAME);
    }
}
