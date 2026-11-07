package com.rinkynooble.adastraflightexpansion.verify;

import com.rinkynooble.adastraflightexpansion.AdAstraFlightExpansion;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Dev-only, run by {@code gradlew runClient -Pverify}: once the client reaches the title screen, loads
 * {@code LocalPlayer} (so its patch is applied and logged, which otherwise waits for a world) and closes the game.
 * Never shipped.
 */
@Mod.EventBusSubscriber(modid = AdAstraFlightExpansion.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientCheck {
    private static final Logger LOGGER = LogManager.getLogger("Ad Astra Flight Expansion verify");
    private static boolean done;

    private ClientCheck() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (done || event.phase != TickEvent.Phase.END || !Boolean.getBoolean(AdAstraFlightExpansion.MODID + ".verify")) {
            return;
        }
        done = true;
        try {
            Class.forName("net.minecraft.client.player.LocalPlayer");
            LOGGER.info("Client check: reached the title screen and loaded LocalPlayer.");
        } catch (ClassNotFoundException e) {
            LOGGER.error("Client check: couldn't load LocalPlayer", e);
        }
        Minecraft.getInstance().stop();
    }
}
