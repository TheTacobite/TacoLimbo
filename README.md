# TacoLimbo

Our own fork of [LOOHP/Limbo](https://github.com/LOOHP/Limbo) - a standalone, from-scratch
Minecraft protocol server implementation (not Paper/Bukkit) that serves a static world schematic
instead of a real simulated one. Used as the NetCore network's AFK-holding server, via the
companion plugin [`NetCoreLimbo`](https://github.com/TheTacobite/NetCoreLimbo).

## Why a fork exists

Supply-chain independence - "let's not rely on anyone." Building our own jar from this fork's
source means the network's AFK-holding server keeps working even if upstream LOOHP ever deletes
or changes their repo, and means we never depend on `repo.loohpjames.com` (their own Maven host)
or GitHub release binaries at build time.

## Branches

- **`master`** - tracks upstream LOOHP's latest game-version bumps. Not built from directly.
- **`production`** - pinned to whatever exact Minecraft version this network's real Paper servers
  actually run (currently `26.3`, from upstream commit `cadac37`). **Always build `NetCoreLimbo`'s vendored
  jar from this branch, never `master`.**

Confirmed live: a real Minecraft client silently rejects Limbo's dynamic registry data
(`dimension_type`, biomes, etc.) if it's built for a *different* game version than the client
itself - even one point release off. The failure looks exactly like corrupted JSON
("Failed to parse value...") even though the data is perfectly valid for its own target version.
`master` had already moved on to "Minecraft 26.2" while this network runs `26.1.2`, which cost a
long, misleading debugging cycle before the real cause (a version mismatch, not bad data) was
found. Re-point `production` at a newer commit only when this network's own Paper version
actually moves to that Minecraft version too - never just to pick up upstream fixes.

## Building

```bash
git checkout production
mvn clean package
# -> target/Limbo-<version>.jar
```

**Always use `mvn clean package`, never plain `mvn package`** - Maven's resource-copy step only
adds/updates files in `target/classes`, it never prunes files deleted from `src/`. A build without
`clean` can silently repackage stale, deleted resources from a previous build (confirmed live -
cost a full debugging cycle before this was caught).

## Fixes applied on top of upstream

- **`PacketLoginOutLoginSuccess` wire-format bug** (fixed on `master`'s lineage, commit `3fd1af4`
  - not needed on `production`, which was already correct at the `27aae24` commit it's pinned to):
  the packet wrote an extra, unused internal `sessionId` UUID (16 bytes) onto the wire after the
  properties-count field - not part of the real `login_finished` packet format at all. Velocity's
  decoder rejected it outright as a corrupted frame. `sessionId` had no other consumer anywhere in
  the codebase; it was only ever passed into this one packet and written straight onto the wire.

## What's NOT changed

Everything else - `production`'s bundled `dimension_type`/biome/jukebox_song registry data
(including a couple of non-vanilla entries LOOHP's own repo ships, e.g. a `sulfur_caves` biome and
a `bounce` jukebox song with no real Minecraft counterpart) is left exactly as upstream shipped
it. An earlier attempt to hand-patch this data against real Paper-generated reference values was
fully reverted once the real root cause (the `production`/`master` version mismatch above) was
found - the stock data works fine once the game version actually matches.

## Consumer

See [`NetCoreLimbo`](https://github.com/TheTacobite/NetCoreLimbo) for the companion plugin that
actually runs on top of this (heartbeats into the network's shared server registry, returns idle
players to the hub) and for the vendored-jar refresh workflow.
