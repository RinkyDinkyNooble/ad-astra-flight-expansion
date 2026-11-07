# Ad Astra Flight Expansion

An unofficial add-on for [Ad Astra](https://www.curseforge.com/minecraft/mc-mods/ad-astra) by Terrarium (Minecraft 1.20.1, Forge 47, Ad Astra 1.15.21 and later). Not affiliated with Ad Astra.

Ad Astra has an item tag for its jet suit, `ad_astra:jet_suit_items`, but only the jet suit's own chestplate can fly: the flight is code in that item. This mod makes the tag mean what it says. **Any armour in `ad_astra:jet_suit_items` flies like the jet suit**, with the chestplate's durability as fuel instead of energy.

## Using it

1. Put the armour in Ad Astra's tag with a datapack (or KubeJS). Add to Ad Astra's own list, don't replace it:

   `data/ad_astra/tags/items/jet_suit_items.json`
   ```json
   {
     "replace": false,
     "values": ["mymod:helmet", "mymod:chestplate", "mymod:leggings", "mymod:boots"]
   }
   ```
2. Wear the full set. The controls are Ad Astra's own:
   - **Toggle Suit Flight** (V by default, under Ad Astra in Controls) turns flight on and off, with Ad Astra's "Suit flight enabled/disabled" message.
   - **Hold jump** to hover upwards.
   - **Hold jump and sprint** to fly where you look; you glide like an elytra.

Install it on **both the client and the server**: the client moves the player, and the server keeps fall damage off, keeps the glide going and wears the chestplate.

## Fuel: the chestplate's durability

- 1 durability for every second (20 ticks) of flight, hovering or gliding, counted once when both happen in the same tick. The jet suit's own glide wears its chestplate at that rate too.
- Flight stops when **1 durability** is left, like the elytra, so flying never breaks the chestplate. Repair it to fly again.
- No wear in creative. A chestplate that can't take damage (unbreakable, or no durability) flies for free.
- Unbreaking works on it, as on the elytra.
- Server config `serverconfig/adastraflightexpansion-server.toml` in each world (copy it to `defaultconfigs/` for new worlds):
  - `durabilityPerSecond` (default 1, 0 to 1000): durability lost per second of flight. 0 makes flight free.

For scale: a chestplate with 560 durability gives about 9 minutes of flight between repairs. Ad Astra's jet suit (1,000,000 FE) gives about 17 minutes of hover or 8 of full flight.

## How it behaves, exactly

The rules are the jet suit's (`JetSuitItem` in Ad Astra 1.15.21); the movement is the jet suit's own code, called on Ad Astra's jet suit item, so the physics are the same to the last decimal (checked tick by tick, see Verifying).

- **Thrust** needs: the chestplate in the tag, every armour slot in the tag (the full set), Ad Astra's suit flight toggled on, jump held, no creative flight, the chestplate not on cooldown, and more than 1 durability left (or creative). Sprint held: fly along the look direction (capped at Ad Astra's top speed) and start gliding; otherwise push upwards. Both cut fall distance, so a landing under thrust doesn't hurt.
- **Glide** (the elytra state) needs: the chestplate in the tag, toggle on, jump and sprint held, and more than 1 durability left. Like the jet suit's chestplate, this doesn't check the other slots: a tagged chestplate worn with other armour can still glide like an elytra when you jump off something holding jump and sprint, but it gives no thrust.
- **Flames** under the feet while you thrust, seen by you (Ad Astra draws the jet suit's flames from its own suit model). Ad Astra's jet suit flames show the same way on servers: only for your own player.
- **Ad Astra's own jet suit is untouched.** A jet suit chestplate (`JetSuitItem`) is left to Ad Astra: energy, wear, flames and all. A real jet suit chestplate worn with other tagged pieces already flew before this mod (Ad Astra's own code).
- If Ad Astra's jet suit isn't registered, or a future Ad Astra changes its flight methods, the mod logs it and does nothing.

## What the tag also does in Ad Astra (side effects)

Putting armour in `ad_astra:jet_suit_items` also puts it in `ad_astra:space_suit_items` (Ad Astra's tag includes the jet suit tag). In Ad Astra 1.15.21 that means:

- **No kick for flying:** Ad Astra resets the server's "flying is not enabled" counter for players off the ground in a full space suit set, so flying with `allow-flight=false` is fine. This mod relies on it.
- Ad Astra's own temperature system treats the set as freeze resistant (`freeze_resistant_armor` includes the space suits).
- The Sulfur Creeper's oxygen drain and other full-set checks count the set. Oxygen itself only lives in Ad Astra's own suits, so other armour holds none.
- Ad Astra's suit textures and energy overlay check its own item classes, so other armour keeps its look and shows no energy bar.
- Ad Astra's Toggle Suit Flight key and key syncing work for any chestplate in the tag; this mod adds no keys and no packets.

## How it works

Mixins, all optional (`required: false`, `defaultRequire: 0`), each logged at startup: `Patched <Class>: N of M changes applied.`

| Class | Change |
|---|---|
| `Inventory.tick` | At the end (where the jet suit's chestplate runs its flight from `inventoryTick`), applies the jet suit's rules and calls Ad Astra's `fullFlight` or `upwardsFlight` on its jet suit item, through an `@Invoker` on `JetSuitItem`. Server side it counts the tick towards wear; client side it draws the flames. |
| `LivingEntity.updateFallFlying` | `@ModifyExpressionValue` on Forge's `ItemStack.canElytraFly` and `elytraFlightTick`: when the chestplate says no, the jet suit's glide rule can say yes, and the glide tick wears it on the server. |
| `Player.tryToStartFallFlying` | The same on `canElytraFly`, so the server and single player can start the glide. |
| `LocalPlayer.aiStep` (client) | The same on `canElytraFly`, so the client starts the glide when jump is pressed in the air. |

The elytra hooks only add a "yes" when the item said "no", so they chain with other mods' changes at the same spots (in ABZA: Fabric API's elytra events through Sinytra Connector, Lithium's elytra check, ParCool and Expandability). Forge 47 doesn't ship MixinExtras, so MixinExtras 0.4.1 (MIT, by LlamaLad7) is packed inside the jar with Forge's Jar-in-Jar; Forge loads the newest copy any mod brings.

## Building

Needs JDK 17 and the jars this is compiled against in `libs/` (not in the repo): `ad_astra-forge-1.20.1-1.15.21.jar`, `botarium-forge-1.20.1-2.3.4.jar`, `resourcefullib-forge-1.20.1-2.1.29.jar`, `resourcefulconfig-forge-1.20.1-2.1.3.jar`.

```bash
./gradlew build
```

The mod is `build/libs/adastraflightexpansion-1.0.0.jar` (the `-slim` jar lacks MixinExtras; don't ship it).

## Verifying

`src/verify` is dev-only and never shipped. It puts vanilla iron armour in the jet suit tag for the dev runs, so `./gradlew runClient` gives a client with flying iron armour, and `./gradlew runServer` a server in `run/server` (connect the dev client to `localhost`).

```bash
./gradlew runServer -Pverify
```

starts the dev server, runs 35 checks on fake players and writes them to `run/server/verify-results.txt`, then stops:

- tagged iron armour and Ad Astra's own jet suit, side by side for 30 ticks of hover and 30 of full flight: motion, fall distance and glide identical every tick;
- the jet suit's energy use unchanged (50 FE a tick hovering, 100 flying), its durability untouched by the mod, its own glide still starting;
- wear: 1 durability per 20 ticks, once when thrust and glide share a tick, thrust and glide stopping at 1 left, never breaking, none in creative, `durabilityPerSecond` 0 and 3;
- no thrust without the full set, the toggle or jump, with an untagged chestplate, on cooldown or in creative flight; no glide start without sprint;
- a tagged chestplate with an untagged helmet: no thrust, glide like the jet suit's chestplate.

`./gradlew runClient -Pverify` opens the dev client, checks the client patch applies at the title screen, and closes.

## License

0BSD (see `LICENSE`), for this mod's own code. Ad Astra is Terrarium's and isn't included; MixinExtras is MIT and ships inside the jar with its own license.
