package com.rinkynooble.adastraflightexpansion.mixin;

import earth.terrarium.adastra.common.items.armor.JetSuitItem;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Calls Ad Astra's own jet suit movement, so other armour flies with exactly the same physics. */
@Mixin(value = JetSuitItem.class, remap = false)
public interface JetSuitItemInvoker {
    /** Sprint held: push along the look direction, glide. */
    @Invoker("fullFlight")
    void adastraflightexpansion$fullFlight(Player player);

    /** Jump held: push upwards. */
    @Invoker("upwardsFlight")
    void adastraflightexpansion$upwardsFlight(Player player);
}
