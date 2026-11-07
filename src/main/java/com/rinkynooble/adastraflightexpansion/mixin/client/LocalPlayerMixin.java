package com.rinkynooble.adastraflightexpansion.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.rinkynooble.adastraflightexpansion.JetFlight;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Lets the client start the glide when jump is pressed in the air (it then tells the server). */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", remap = false,
            target = "Lnet/minecraft/world/item/ItemStack;canElytraFly(Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private boolean adastraflightexpansion$canElytraFly(boolean original) {
        return original || JetFlight.canElytraFly((LocalPlayer) (Object) this);
    }
}
