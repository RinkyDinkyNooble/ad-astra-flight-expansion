package com.rinkynooble.adastraflightexpansion.verify;

import com.mojang.authlib.GameProfile;
import com.rinkynooble.adastraflightexpansion.AdAstraFlightExpansion;
import com.rinkynooble.adastraflightexpansion.Config;
import com.rinkynooble.adastraflightexpansion.JetFlight;
import earth.terrarium.adastra.common.items.armor.JetSuitItem;
import earth.terrarium.adastra.common.utils.KeybindManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Dev-only checks, run by {@code gradlew runServer -Pverify}: when the dev server has started, they wear armour on
 * fake players, set Ad Astra's synced keys, tick the players' inventories and elytra checks by hand, and compare.
 * Vanilla iron armour is in the jet suit tag through this source set's test tag. Results go to the log and to
 * {@code run/server/verify-results.txt}, then the server stops. Never shipped.
 */
@Mod.EventBusSubscriber(modid = AdAstraFlightExpansion.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FlightChecks {
    private static final Logger LOGGER = LogManager.getLogger("Ad Astra Flight Expansion verify");
    private static final long JET_SUIT_ENERGY = 1_000_000L;
    private static final List<String> LINES = new ArrayList<>();
    private static int passed;
    private static int failed;
    private static int rigs;

    private FlightChecks() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!Boolean.getBoolean(AdAstraFlightExpansion.MODID + ".verify")) {
            return;
        }
        MinecraftServer server = event.getServer();
        try {
            run(server.overworld());
        } catch (Throwable t) {
            check("checks ran without an exception: " + t, false);
            LOGGER.error("Verify crashed", t);
        }
        String summary = (failed == 0 ? "ALL PASSED" : "FAILED") + ": " + passed + " passed, " + failed + " failed";
        LINES.add(summary);
        LOGGER.info(summary);
        try {
            Files.write(new File(server.getServerDirectory(), "verify-results.txt").toPath(), LINES, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Couldn't write verify-results.txt", e);
        }
        server.halt(false);
    }

    private static void run(ServerLevel level) throws Exception {
        Method updateFallFlying = ObfuscationReflectionHelper.findMethod(LivingEntity.class, "m_21323_");
        Item[] jetSuit = {item("ad_astra:jet_suit_helmet"), item("ad_astra:jet_suit"),
                item("ad_astra:jet_suit_pants"), item("ad_astra:jet_suit_boots")};
        Item[] iron = {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS};
        int ironMax = Items.IRON_CHESTPLATE.getMaxDamage();

        section("Same physics as Ad Astra's jet suit, tick by tick (30 ticks hover, then 30 ticks full flight)");
        ServerPlayer jet = rig(level, jetSuit);
        ServerPlayer tagged = rig(level, iron);
        fillEnergy(jet);
        boolean same = true;
        int firstDiff = -1;
        for (int t = 0; t < 60; t++) {
            boolean sprint = t >= 30;
            if (t == 30) {
                // Nothing slows the players here (no air drag without a real tick), so the hover has passed the jet
                // suit's speed cap of 2 by now. Start the full flight from rest, the same for both.
                jet.setDeltaMovement(Vec3.ZERO);
                tagged.setDeltaMovement(Vec3.ZERO);
            }
            keys(jet, true, sprint, true);
            keys(tagged, true, sprint, true);
            jet.tickCount = tagged.tickCount = 1000 + t;
            jet.getInventory().tick();
            tagged.getInventory().tick();
            boolean tickSame = jet.getDeltaMovement().equals(tagged.getDeltaMovement())
                    && jet.fallDistance == tagged.fallDistance
                    && jet.isFallFlying() == tagged.isFallFlying();
            if (!tickSame && same) {
                firstDiff = t;
                LOGGER.info("First difference at tick {}: jet suit {} fall {} gliding {}, tagged {} fall {} gliding {}", t,
                        jet.getDeltaMovement(), jet.fallDistance, jet.isFallFlying(),
                        tagged.getDeltaMovement(), tagged.fallDistance, tagged.isFallFlying());
            }
            same &= tickSame;
        }
        check("motion, fall distance and gliding identical every tick" + (same ? "" : " (first difference at tick " + firstDiff + ")"), same);
        check("tagged armour actually moved (" + tagged.getDeltaMovement() + ")", tagged.getDeltaMovement().y > 0);
        check("fall distance reduced while thrusting (" + tagged.fallDistance + " from 20)", tagged.fallDistance < 1);
        check("full flight started gliding", tagged.isFallFlying());
        check("Ad Astra's jet suit used its own energy as before: 30 x 50 + 30 x 100 FE (left " + energy(jet) + ")",
                energy(jet) == JET_SUIT_ENERGY - 4500);
        check("tagged chestplate wore 3 durability in 60 ticks of thrust (damage " + damage(tagged) + ")", damage(tagged) == 3);

        section("Thrust and glide in the same tick wear once");
        ServerPlayer both = rig(level, iron);
        for (int t = 0; t < 40; t++) {
            keys(both, true, true, true);
            both.tickCount = 2000 + t;
            both.getInventory().tick();
            updateFallFlying.invoke(both);
        }
        check("still gliding after 40 ticks", both.isFallFlying());
        check("2 durability in 40 ticks of thrust and glide (damage " + damage(both) + ")", damage(both) == 2);

        section("Glide alone, a tagged chestplate with an untagged helmet (as Ad Astra's jet suit chestplate does)");
        ServerPlayer glider = rig(level, Items.LEATHER_HELMET, iron[1], iron[2], iron[3]);
        keys(glider, true, true, true);
        glider.getInventory().tick();
        check("no thrust without the full tagged set", glider.getDeltaMovement().equals(Vec3.ZERO));
        check("can start gliding (jump pressed in the air, sprint held, toggle on)", glider.tryToStartFallFlying());
        for (int t = 0; t < 40; t++) {
            glider.tickCount = 3000 + t;
            updateFallFlying.invoke(glider);
        }
        check("still gliding after 40 ticks", glider.isFallFlying());
        check("2 durability in 40 ticks of gliding (damage " + damage(glider) + ")", damage(glider) == 2);
        keys(glider, true, false, true);
        updateFallFlying.invoke(glider);
        check("glide ends when sprint is released", !glider.isFallFlying());

        section("Thrust stops at 1 durability, and flying never breaks the chestplate");
        ServerPlayer worn = rig(level, iron);
        worn.getItemBySlot(EquipmentSlot.CHEST).setDamageValue(ironMax - 2);
        for (int t = 0; t < 20; t++) {
            keys(worn, true, false, true);
            worn.tickCount = 4000 + t;
            worn.getInventory().tick();
        }
        check("1 durability left after 20 ticks (left " + left(worn) + ")", left(worn) == 1);
        Vec3 before = worn.getDeltaMovement();
        for (int t = 20; t < 80; t++) {
            worn.tickCount = 4000 + t;
            worn.getInventory().tick();
        }
        check("no thrust at 1 durability", worn.getDeltaMovement().equals(before));
        check("still 1 durability left, not broken", left(worn) == 1 && worn.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE));
        keys(worn, true, true, true);
        check("can't start gliding at 1 durability", !worn.tryToStartFallFlying());

        section("Creative: thrust, no wear");
        ServerPlayer creative = rig(level, iron);
        creative.setGameMode(GameType.CREATIVE);
        for (int t = 0; t < 40; t++) {
            keys(creative, true, t >= 20, true);
            creative.tickCount = 5000 + t;
            creative.getInventory().tick();
        }
        check("thrust in creative", creative.getDeltaMovement().y > 0);
        check("no wear in creative (damage " + damage(creative) + ")", damage(creative) == 0);

        section("Keys and set: no flight without the toggle, without jump, or with an untagged chestplate");
        ServerPlayer off = rig(level, iron);
        keys(off, true, true, false);
        off.getInventory().tick();
        check("toggle off: no thrust", off.getDeltaMovement().equals(Vec3.ZERO));
        check("toggle off: can't start gliding", !off.tryToStartFallFlying());
        keys(off, false, true, true);
        off.getInventory().tick();
        check("jump not held: no thrust", off.getDeltaMovement().equals(Vec3.ZERO));
        check("jump not held: can't start gliding", !off.tryToStartFallFlying());
        keys(off, true, false, true);
        check("sprint not held: can't start gliding", !off.tryToStartFallFlying());
        ServerPlayer diamond = rig(level, iron[0], Items.DIAMOND_CHESTPLATE, iron[2], iron[3]);
        keys(diamond, true, true, true);
        diamond.getInventory().tick();
        check("untagged chestplate: no thrust", diamond.getDeltaMovement().equals(Vec3.ZERO));
        check("untagged chestplate: can't start gliding", !diamond.tryToStartFallFlying());
        ServerPlayer onCooldown = rig(level, iron);
        onCooldown.getCooldowns().addCooldown(Items.IRON_CHESTPLATE, 100);
        keys(onCooldown, true, false, true);
        onCooldown.getInventory().tick();
        check("chestplate on cooldown: no thrust", onCooldown.getDeltaMovement().equals(Vec3.ZERO));
        ServerPlayer creativeFlying = rig(level, iron);
        creativeFlying.getAbilities().flying = true;
        keys(creativeFlying, true, false, true);
        creativeFlying.getInventory().tick();
        check("creative flight on: no thrust", creativeFlying.getDeltaMovement().equals(Vec3.ZERO));

        section("Ad Astra's own jet suit is left to Ad Astra");
        ServerPlayer jet2 = rig(level, jetSuit);
        fillEnergy(jet2);
        keys(jet2, true, true, true);
        check("the mod's glide rule doesn't answer for a jet suit chestplate", !JetFlight.canElytraFly(jet2));
        check("the mod's glide tick doesn't answer for a jet suit chestplate", !JetFlight.elytraFlightTick(jet2));
        check("the jet suit still starts gliding by itself", jet2.tryToStartFallFlying());
        long energyBefore = energy(jet2);
        int jetDamageBefore = damage(jet2);
        for (int t = 0; t < 40; t++) {
            jet2.tickCount = 6000 + t;
            jet2.getInventory().tick();
            updateFallFlying.invoke(jet2);
        }
        check("jet suit used 40 x 100 FE, as before (used " + (energyBefore - energy(jet2)) + ")", energyBefore - energy(jet2) == 4000);
        check("jet suit chestplate's durability untouched by the mod (damage " + damage(jet2) + ", was " + jetDamageBefore + ")",
                damage(jet2) == jetDamageBefore);

        section("Config durabilityPerSecond");
        int configured = Config.DURABILITY_PER_SECOND.get();
        try {
            Config.DURABILITY_PER_SECOND.set(0);
            ServerPlayer free = rig(level, iron);
            for (int t = 0; t < 40; t++) {
                keys(free, true, false, true);
                free.tickCount = 7000 + t;
                free.getInventory().tick();
            }
            check("0: thrust, no wear (damage " + damage(free) + ")", free.getDeltaMovement().y > 0 && damage(free) == 0);
            Config.DURABILITY_PER_SECOND.set(3);
            ServerPlayer costly = rig(level, iron);
            for (int t = 0; t < 40; t++) {
                keys(costly, true, false, true);
                costly.tickCount = 8000 + t;
                costly.getInventory().tick();
            }
            check("3: 6 durability in 40 ticks (damage " + damage(costly) + ")", damage(costly) == 6);
        } finally {
            Config.DURABILITY_PER_SECOND.set(configured);
        }
    }

    private static ServerPlayer rig(ServerLevel level, Item... armour) {
        int n = ++rigs;
        ServerPlayer player = FakePlayerFactory.get(level,
                new GameProfile(UUID.nameUUIDFromBytes(("aafe-verify-" + n).getBytes(StandardCharsets.UTF_8)), "aafe_verify_" + n));
        player.setGameMode(GameType.SURVIVAL);
        player.getAbilities().flying = false;
        BlockPos spawn = level.getSharedSpawnPos();
        player.setPos(spawn.getX() + 0.5, 200.0, spawn.getZ() + 0.5);
        player.setXRot(-30.0F);
        player.setYRot(45.0F);
        player.setDeltaMovement(Vec3.ZERO);
        player.setOnGround(false);
        player.fallDistance = 20.0F;
        player.stopFallFlying();
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < slots.length; i++) {
            player.setItemSlot(slots[i], new ItemStack(armour[i]));
        }
        return player;
    }

    private static void keys(ServerPlayer player, boolean jump, boolean sprint, boolean toggle) {
        KeybindManager.set(player, jump, sprint, toggle);
    }

    private static void fillEnergy(ServerPlayer player) {
        // The jet suit's own container writes into the stack; internal inserts aren't capped at 1000 FE a tick.
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ((JetSuitItem) chest.getItem()).getEnergyStorage(chest).internalInsert(JET_SUIT_ENERGY, false);
    }

    private static long energy(ServerPlayer player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        return ((JetSuitItem) chest.getItem()).getEnergyStorage(chest).getStoredEnergy();
    }

    private static int damage(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).getDamageValue();
    }

    private static int left(ServerPlayer player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        return chest.getMaxDamage() - chest.getDamageValue();
    }

    @SuppressWarnings("removal")
    private static Item item(String id) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        if (item == null || item == Items.AIR) {
            throw new IllegalStateException("Missing item " + id);
        }
        return item;
    }

    private static void section(String title) {
        LINES.add("");
        LINES.add("== " + title);
        LOGGER.info("== {}", title);
    }

    private static void check(String what, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
        }
        String line = (ok ? "PASS " : "FAIL ") + what;
        LINES.add(line);
        if (ok) {
            LOGGER.info(line);
        } else {
            LOGGER.error(line);
        }
    }
}
