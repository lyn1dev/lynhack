package com.lynhack.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    @Invoker("doAttack")
    boolean invokeDoAttack();

    @Invoker("doItemUse")
    void invokeDoItemUse();

    @Invoker("handleBlockBreaking")
    void invokeHandleBlockBreaking(boolean breaking);

    @Accessor("attackCooldown")
    int getAttackCooldown();

    @Accessor("attackCooldown")
    void setAttackCooldown(int cooldown);
}
