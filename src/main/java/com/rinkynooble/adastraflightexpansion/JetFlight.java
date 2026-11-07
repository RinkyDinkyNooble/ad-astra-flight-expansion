package com.rinkynooble.adastraflightexpansion;

import com.rinkynooble.adastraflightexpansion.mixin.JetSuitItemInvoker;
import earth.terrarium.adastra.common.items.armor.JetSuitItem;
import earth.terrarium.adastra.common.items.armor.SpaceSuitItem;
import earth.terrarium.adastra.common.tags.ModItemTags;
import earth.terrarium.adastra.common.utils.KeybindManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Jet suit flight for armour in Ad Astra's {@code ad_astra:jet_suit_items} tag whose chestplate isn't Ad Astra's own
 * jet suit. The conditions follow {@code JetSuitItem}; the movement itself is Ad Astra's, called on its jet suit item.
 * Fuel is the chestplate's durability instead of energy.
 */
public final class JetFlight {
    private static final Logger LOGGER = LogManager.getLogger("Ad Astra Flight Expansion");
    @SuppressWarnings("removal") // Its replacement is missing from early Forge 47 builds.
    private static final ResourceLocation JET_SUIT_ID = new ResourceLocation("ad_astra", "jet_suit");
    /** Ticks of flight per wear; the jet suit's glide wears its chestplate once every 20 ticks too. */
    private static final int TICKS_PER_WEAR = 20;

    /** Ad Astra's jet suit chestplate, through which its flight is called. Null: the mod does nothing. */
    private static volatile JetSuitItemInvoker jetSuit;
    /** Server side only: flight ticks counted towards the next wear, per player. */
    private static final Map<Player, FlightTicks> FLIGHT_TICKS = new WeakHashMap<>();

    private JetFlight() {
    }

    static void init() {
        Item item = ForgeRegistries.ITEMS.getValue(JET_SUIT_ID);
        if (!(item instanceof JetSuitItem)) {
            LOGGER.warn("Not applied: Ad Astra's jet suit ({}) isn't registered. Nothing is changed.", JET_SUIT_ID);
        } else if (!(item instanceof JetSuitItemInvoker invoker)) {
            LOGGER.warn("Not applied: this Ad Astra version's jet suit flight couldn't be reached. Nothing is changed.");
        } else {
            jetSuit = invoker;
            LOGGER.info("Ready: armour in ad_astra:jet_suit_items flies like Ad Astra's jet suit.");
        }
    }

    /**
     * Called right after the player's inventory ticks, which is where Ad Astra's jet suit chestplate runs its own
     * flight ({@code inventoryTick}). Runs for the local player on the client and for each player on the server.
     */
    public static void tick(Player player) {
        JetSuitItemInvoker suit = jetSuit;
        if (suit == null) {
            return;
        }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!isOtherJetChest(chest)
                || player.getAbilities().flying
                || player.getCooldowns().isOnCooldown(chest.getItem())
                || !SpaceSuitItem.hasFullJetSuitSet(player)
                || !KeybindManager.suitFlightEnabled(player)
                || !KeybindManager.jumpDown(player)
                || !canFly(player, chest)) {
            return;
        }

        if (KeybindManager.sprintDown(player)) {
            suit.adastraflightexpansion$fullFlight(player);
        } else {
            suit.adastraflightexpansion$upwardsFlight(player);
        }

        if (player.level().isClientSide) {
            spawnFlames(player);
        } else {
            countFlightTick(player, chest);
        }
    }

    /**
     * Whether the chestplate may glide like an elytra: the jet suit's rule (can fly, and the suit-flight toggle,
     * jump and sprint all on). Called where Forge asks the chestplate {@code canElytraFly}, when it said no.
     */
    public static boolean canElytraFly(LivingEntity entity) {
        if (jetSuit == null || !(entity instanceof Player player)) {
            return false;
        }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        return isOtherJetChest(chest)
                && canFly(player, chest)
                && KeybindManager.suitFlightEnabled(player)
                && KeybindManager.jumpDown(player)
                && KeybindManager.sprintDown(player);
    }

    /**
     * One tick of gliding, where Forge asks the chestplate {@code elytraFlightTick} and it said no. Keeps the glide
     * going, and on the server wears the chestplate (counted once with thrust in the same tick) and makes the glide
     * sound event every 10 ticks, like the jet suit and the elytra.
     */
    public static boolean elytraFlightTick(LivingEntity entity) {
        if (!canElytraFly(entity)) {
            return false;
        }
        Player player = (Player) entity;
        if (!player.level().isClientSide) {
            if ((player.getFallFlyingTicks() + 1) % 10 == 0) {
                player.gameEvent(GameEvent.ELYTRA_GLIDE);
            }
            countFlightTick(player, player.getItemBySlot(EquipmentSlot.CHEST));
        }
        return true;
    }

    /** In Ad Astra's jet suit tag, but not one of Ad Astra's own jet suit chestplates (those fly by themselves). */
    private static boolean isOtherJetChest(ItemStack chest) {
        return !chest.isEmpty() && chest.is(ModItemTags.JET_SUITS) && !(chest.getItem() instanceof JetSuitItem);
    }

    /** Creative, or more than 1 durability left (like the elytra), or a chestplate that can't be damaged. */
    private static boolean canFly(Player player, ItemStack chest) {
        return player.isCreative() || !chest.isDamageableItem() || chest.getMaxDamage() - chest.getDamageValue() > 1;
    }

    /** Server side: counts one tick of flight, and wears the chestplate every 20 of them. */
    private static void countFlightTick(Player player, ItemStack chest) {
        int perSecond = Config.durabilityPerSecond();
        if (perSecond <= 0 || player.isCreative() || player.isSpectator() || !chest.isDamageableItem()) {
            return;
        }
        FlightTicks ticks = FLIGHT_TICKS.computeIfAbsent(player, p -> new FlightTicks());
        if (ticks.lastTick == player.tickCount) {
            return; // Thrust and glide in the same tick count once.
        }
        ticks.lastTick = player.tickCount;
        if (++ticks.count < TICKS_PER_WEAR) {
            return;
        }
        ticks.count = 0;
        // Never below 1 durability left, so flying never breaks the chestplate.
        int amount = Math.min(perSecond, chest.getMaxDamage() - chest.getDamageValue() - 1);
        if (amount > 0) {
            chest.hurtAndBreak(amount, player, p -> p.broadcastBreakEvent(EquipmentSlot.CHEST));
        }
    }

    /** Client side: a flame under each foot while thrusting (the jet suit draws its own from its suit model). */
    private static void spawnFlames(Player player) {
        Level level = player.level();
        double yaw = Math.toRadians(player.yBodyRot);
        double sideX = Math.cos(yaw) * 0.15;
        double sideZ = Math.sin(yaw) * 0.15;
        double y = player.getY() + (player.isFallFlying() ? 0.1 : 0.0);
        level.addParticle(ParticleTypes.FLAME, player.getX() + sideX, y, player.getZ() + sideZ, 0.0, 0.0, 0.0);
        level.addParticle(ParticleTypes.FLAME, player.getX() - sideX, y, player.getZ() - sideZ, 0.0, 0.0, 0.0);
    }

    private static final class FlightTicks {
        int lastTick = Integer.MIN_VALUE;
        int count;
    }
}
