package com.rinkynooble.adastraflightexpansion.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.rinkynooble.adastraflightexpansion.JetFlight;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Keeps the glide going (the elytra check each tick), as Ad Astra's jet suit does through its own item hooks. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @ModifyExpressionValue(method = "updateFallFlying", at = @At(value = "INVOKE", remap = false,
            target = "Lnet/minecraft/world/item/ItemStack;canElytraFly(Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private boolean adastraflightexpansion$canElytraFly(boolean original) {
        return original || JetFlight.canElytraFly((LivingEntity) (Object) this);
    }

    @ModifyExpressionValue(method = "updateFallFlying", at = @At(value = "INVOKE", remap = false,
            target = "Lnet/minecraft/world/item/ItemStack;elytraFlightTick(Lnet/minecraft/world/entity/LivingEntity;I)Z"))
    private boolean adastraflightexpansion$elytraFlightTick(boolean original) {
        return original || JetFlight.elytraFlightTick((LivingEntity) (Object) this);
    }
}
