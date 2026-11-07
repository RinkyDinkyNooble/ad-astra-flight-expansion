package com.rinkynooble.adastraflightexpansion.mixin;

import com.rinkynooble.adastraflightexpansion.JetFlight;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Thrust, right where Ad Astra's jet suit thrusts: when the player's inventory (armour included) has ticked. */
@Mixin(Inventory.class)
public abstract class InventoryMixin {
    @Shadow
    @Final
    public Player player;

    @Inject(method = "tick", at = @At("TAIL"))
    private void adastraflightexpansion$jetFlight(CallbackInfo ci) {
        JetFlight.tick(this.player);
    }
}
