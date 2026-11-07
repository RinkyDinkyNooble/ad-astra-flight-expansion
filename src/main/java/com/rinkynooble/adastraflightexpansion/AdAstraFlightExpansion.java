package com.rinkynooble.adastraflightexpansion;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(AdAstraFlightExpansion.MODID)
public class AdAstraFlightExpansion {
    public static final String MODID = "adastraflightexpansion";

    // The older calls, deprecated in late Forge 47 builds, keep the mod working on every Forge 47 build.
    @SuppressWarnings("removal")
    public AdAstraFlightExpansion() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        // Items are registered by now; look up Ad Astra's jet suit once.
        event.enqueueWork(JetFlight::init);
    }
}
