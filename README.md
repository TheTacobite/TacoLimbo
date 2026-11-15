# TacoLimbo

Our own fork of [LOOHP/Limbo](https://github.com/LOOHP/Limbo) - a standalone, from-scratch
Minecraft protocol server implementation (not Paper/Bukkit) that serves a static world schematic
instead of a real simulated one. Used as the NetCore network's AFK-holding server, via the
companion plugin [`NetCoreLimbo`](https://github.com/TheTacobite/NetCoreLimbo).

## Why a fork exists

Supply-chain independence - "let's not rely on anyone." Building our own jar from this fork's
source means the network's AFK-holding server keeps working even if upstream LOOHP ever deletes
or changes their repo, and means we never depend on `repo.loohpjames.com` (their own Maven host)
or GitHub release binaries at build time. The only remote the build touches is Maven Central:
the one library not published there (`com.github.Querz:NBT:6.1`, previously pulled from JitPack)
is committed under `libs/` as a file-based Maven repository, so `mvn -o clean package` builds
fully offline once Central's artifacts are cached.

**We maintain this fork ourselves, including Minecraft version updates** (decided 2026-11-15).
Upstream's last change we took is `cadac37` (Minecraft 26.3, 2026-09-15); from here on nothing is
merged from LOOHP/Limbo. New Minecraft versions are ported by hand - see "Updating to a new
Minecraft version" below. Keep `LICENSE` (Apache 2.0) and LOOHP's copyright headers in place; that
is the licence's only condition.

## Branches

- **`master`** - where the next Minecraft version is ported. Not built from for deployment.
- **`production`** - pinned to whatever exact Minecraft version this network's real Paper servers
  actually run (currently `26.3`). **Always build `NetCoreLimbo`'s vendored jar and the GitHub
  releases from this branch, never `master`.**

Confirmed live: a real Minecraft client silently rejects Limbo's dynamic registry data
(`dimension_type`, biomes, etc.) if it's built for a *different* game version than the client
itself - even one point release off. The failure looks exactly like corrupted JSON
("Failed to parse value...") even though the data is perfectly valid for its own target version.
`master` had already moved on to "Minecraft 26.2" while this network runs `26.1.2`, which cost a
long, misleading debugging cycle before the real cause (a version mismatch, not bad data) was
found. Move `production` to a newer Minecraft version only when this network's own Paper version
actually moves to that Minecraft version too.

## Updating to a new Minecraft version

Every past version bump (26.1, 26.1.1, 26.1.2, 26.2, 26.3 - e.g. upstream commit `d651aee` for
26.3) touched the same places. Do the work on `master`, then fast-forward `production` once the
network's Paper servers move to that version.

1. **Version constants** - `Limbo.java`: `SERVER_IMPLEMENTATION_VERSION` (e.g. `"26.3"`) and
   `SERVER_IMPLEMENTATION_PROTOCOL` (the new protocol number, from the Minecraft Wiki's protocol
   version table). `pom.xml`: bump `<version>` and the Minecraft suffix in `<finalName>`.
2. **Regenerate the bundled game data from Mojang's own server jar** - download the official
   `server.jar` for the new version, then run its built-in data generator:
   ```bash
   java -DbundlerMainClass=net.minecraft.data.Main -jar server.jar --reports --server
   ```
   Copy `generated/reports/{blocks,packets,registries}.json` over `src/main/resources/reports/`.
   The registry JSONs under `src/main/resources/data/minecraft/` (dimension_type, worldgen/biome,
   chat_type, damage_type, the `*_variant` folders, tags, and so on) are the vanilla data pack:
   take them from the generator's `generated/data/minecraft/` if it produced them, otherwise
   from `data/minecraft/` inside the real server jar that `server.jar` bundles
   (`META-INF/versions/<version>/server-<version>.jar` - it's a zip). Not yet done by us end to
   end: on the first port, diff the result against upstream's `d651aee` layout to confirm the
   source, then fix this step. Add any brand-new registry folder the client now expects, and
   delete ones Mojang removed. This is most of the diff by line count, but it's all generated.
3. **Packet format changes** - read the release's protocol changes (the Minecraft Wiki's
   "Java Edition protocol" page history, or the `packets.json` diff from step 2) and update the
   matching classes under `network/protocol/packets/`. Usually only a handful change: for 26.3
   that was the chunk packet, registry data, login, and respawn packets, plus small helpers in
   `DataTypeIO` / `CustomNBTUtils` / `RegistryCustom`.
4. **Build and test with a real client** - `mvn clean package`, run the jar behind a local
   Velocity, and join with the new Minecraft version. A version mismatch or bad registry data
   fails at login with "Failed to parse value", so a successful join plus moving around is the
   real check.
5. **Release** - fast-forward `production`, tag `v<pom version>-<mc version>`, attach the jar,
   copy it into `NetCoreLimbo/libs/` (updating that pom's Limbo version), pin the new tag as
   `TACOLIMBO` in [NetCoreDeploy](https://github.com/TheTacobite/NetCoreDeploy)'s `versions.env`,
   push, and run `sudo netcore update` on Box 2.

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

- **bStats removed.** Upstream ships bStats usage reporting turned on by default (it creates
  `plugins/bStats/config.yml` with `enabled: true` and posts server stats to bStats.org). The
  `metrics` package and its two call sites are deleted, so the server makes no outbound calls of
  its own beyond Mojang's skin lookup for players who connect without proxy forwarding.
- **`PacketLoginOutLoginSuccess` wire-format bug** (commit `3fd1af4`, on both branches since
  `production` moved to 26.3):
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
