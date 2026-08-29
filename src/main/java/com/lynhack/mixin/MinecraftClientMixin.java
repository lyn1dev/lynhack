package com.lynhack.mixin;

import com.lynhack.LynHack;
import net.minecraft.client.MinecraftClient;
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
}
