package com.rinkynooble.adastraflightexpansion;

import net.minecraftforge.common.ForgeConfigSpec;

/** Server config: {@code serverconfig/adastraflightexpansion-server.toml} in each world. */
public final class Config {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue DURABILITY_PER_SECOND;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        DURABILITY_PER_SECOND = builder
                .comment("Durability the chestplate loses for each second of jet flight (hovering, flying or gliding).",
                        "Flight stops when 1 durability is left, so flying never breaks it. 0 makes flight free.",
                        "Ad Astra's own jet suit runs on energy and isn't affected.")
                .defineInRange("durabilityPerSecond", 1, 0, 1000);
        SPEC = builder.build();
    }

    private Config() {
    }

    static int durabilityPerSecond() {
        // Read on the server only. Before a world loads the config there's nothing to read.
        return SPEC.isLoaded() ? DURABILITY_PER_SECOND.get() : 1;
    }
}
