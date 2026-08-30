package com.lynhack.mixin;

import com.lynhack.LynHack;
import com.lynhack.api.input.InputManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onClientInit(CallbackInfo ci) {
        LynHack.LOGGER.info("[{}] Injected into MinecraftClient successfully!", LynHack.MOD_NAME);
    }

    /**
     * Bulletproof key flush: runs whenever any screen is opened, closed, switched, or closed by server.
     */
    @Inject(method = "setScreen", at = @At("HEAD"))
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        InputManager.getInstance().flushAll();
    }
}
