package com.lynhack.mixin;

import com.lynhack.api.event.EventBus;
import com.lynhack.api.event.KeyEvent;
import com.lynhack.api.input.InputManager;
import com.lynhack.api.module.ModuleManager;
import com.lynhack.gui.ClickGuiScreen;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void onKey(long window, int action, KeyInput keyInput, CallbackInfo ci) {
        int key = keyInput.key();
        int scancode = keyInput.scancode();
        int modifiers = keyInput.modifiers();

        if (key == GLFW.GLFW_KEY_UNKNOWN) {
            return;
        }

        // 1. Check if physical key is authoritatively blocked by a macro
        if (InputManager.getInstance().shouldBlockPhysicalKey(key)) {
            ci.cancel();
            return;
        }

        // 2. Dispatch KeyEvent across EventBus
        KeyEvent event = new KeyEvent(key, scancode, action, modifiers);
        EventBus.getInstance().post(event);

        if (event.isCancelled()) {
            ci.cancel();
            return;
        }

        // 3. Handle global in-game hotkeys when not typing in a screen
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen == null && action == GLFW.GLFW_PRESS) {
            if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
                mc.setScreen(ClickGuiScreen.getInstance());
                ci.cancel();
                return;
            }
            ModuleManager.getInstance().onKey(key, action);
        }
    }
}
