package com.rinkynooble.adastraflightexpansion.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.rinkynooble.adastraflightexpansion.JetFlight;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Lets the glide start (the elytra check when jumping in the air), on the server and in single player. */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @ModifyExpressionValue(method = "tryToStartFallFlying", at = @At(value = "INVOKE", remap = false,
            target = "Lnet/minecraft/world/item/ItemStack;canElytraFly(Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private boolean adastraflightexpansion$canElytraFly(boolean original) {
        return original || JetFlight.canElytraFly((Player) (Object) this);
    }
}
