package com.lynhack.modules.combat;

import com.lynhack.api.event.FrameUpdateEvent;
import com.lynhack.api.input.InputManager;
import com.lynhack.api.module.Category;
import com.lynhack.api.module.Module;
import com.lynhack.api.setting.BooleanSetting;
import com.lynhack.api.setting.NumberSetting;
import com.lynhack.api.state.ClientState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;

public class TriggerBot extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    private final NumberSetting reach = addSetting(new NumberSetting("Reach", "Maximum attack reach in blocks", 3.5, 2.0, 6.0, 0.1));
    private final NumberSetting cooldownThreshold = addSetting(new NumberSetting("Cooldown", "Attack cooldown progress threshold", 0.95, 0.5, 1.0, 0.05));
    private final BooleanSetting targetPlayers = addSetting(new BooleanSetting("Players", "Attack other player entities", true));
    private final BooleanSetting targetMobs = addSetting(new BooleanSetting("Mobs", "Attack hostile monsters and mobs", true));
    private final BooleanSetting targetAnimals = addSetting(new BooleanSetting("Animals", "Attack passive animal entities", false));
    private final BooleanSetting weaponsOnly = addSetting(new BooleanSetting("WeaponsOnly", "Only trigger when holding a sword or axe", false));

    public TriggerBot() {
        super("TriggerBot", "Automatically attacks targeted entities when attack cooldown is ready", Category.COMBAT, GLFW.GLFW_KEY_R);
    }

    @Override
    public void onFrame(FrameUpdateEvent event) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) {
            return;
        }

        // 1. Check weapon requirement if enabled (uses 1.21+ ItemTags)
        if (weaponsOnly.getValue()) {
            ItemStack stack = mc.player.getMainHandStack();
            boolean isWeapon = stack.isIn(ItemTags.SWORDS) || stack.isIn(ItemTags.AXES);
            if (!isWeapon) {
                return;
            }
        }

        // 2. Check current held item's attack cooldown
        float cooldown = mc.player.getAttackCooldownProgress(0.5f);
        if (cooldown < cooldownThreshold.getValueFloat()) {
            return;
        }

        // 3. Lazy frame-perfect raycast check via ClientState
        EntityHitResult hitResult = ClientState.getTargetedEntity(reach.getValue());
        if (hitResult == null || hitResult.getEntity() == null) {
            return;
        }

        Entity target = hitResult.getEntity();

        // 4. Validate target criteria
        if (!target.isAlive() || target == mc.player) {
            return;
        }

        if (!(target instanceof LivingEntity)) {
            return;
        }

        if (target instanceof PlayerEntity && !targetPlayers.getValue()) {
            return;
        }

        if (target instanceof HostileEntity && !targetMobs.getValue()) {
            return;
        }

        if (target instanceof PassiveEntity && !(target instanceof HostileEntity) && !targetAnimals.getValue()) {
            return;
        }

        // 5. Conditions satisfied! Format telemetry details and simulate attack
        double dist = mc.player.distanceTo(target);
        String details = String.format("Target: %s [%.2fm] (Cooldown: %.0f%%)", target.getName().getString(), dist, cooldown * 100.0f);

        // Command input simulation with Gaussian hold duration and telemetry logging
        InputManager.getInstance().simulateAttack(this, target, details);
    }
}
