## 1.0.0 (2026-11-07)

First version, for Minecraft 1.20.1, Forge 47 and Ad Astra 1.15.21.

- Added: armour in Ad Astra's jet suit tag (`ad_astra:jet_suit_items`) flies like the jet suit: hover on jump, fly and glide on jump and sprint, with Ad Astra's own controls and flight.
- Added: the chestplate's durability is the fuel, 1 a second by default (`durabilityPerSecond` in the server config, 0 for free flight); flight stops at 1 durability left, so it never breaks the chestplate. No wear in creative.
- Added: flames under your feet while you thrust.
- Ad Astra's own jet suit is unchanged.
- Needed on both the client and the server.
