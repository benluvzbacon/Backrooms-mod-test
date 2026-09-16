# The Backrooms — Minecraft 1.21.1 (Fabric)

Put **sand on your head** and noclip into an endless, procedurally generated
**Level 0** — yellow rooms, damp carpet, humming fluorescent lights, long
hallways, junctions, dead ends, and the **Still Life** — a tricorn-hatted
mannequin that only moves when you aren't looking at it — hunting you through
it all. Find a glowing **Sunken Portal** basin and dive into the **Poolrooms**, a
roofed indoor tile maze of shallow water on a fast 5-minute day/night cycle
where your **Sanity slowly returns**, and seek out **Almond Water** in rare
supply chests. Rare, towering **great halls** break the maze: ringed with
windows under a glass roof, they are the only rooms open to the sky. Built for
**Minecraft Java Edition 1.21.1** with the **Fabric** mod loader.

> Core loop: **sand helmet → enter the Backrooms → explore the infinite
> procedural Level 0 → loot Almond Water → dive a Sunken Portal basin → recover your
> Sanity in the roofed Poolrooms, and don't take your eyes off the Still
> Life.**

---

## How to play

1. Install **Fabric Loader 0.16+ for 1.21.1** and **Fabric API**
   (`0.116.x+1.21.1` or newer).
2. Drop the mod `.jar` into your `mods/` folder (Java 21 is required by 1.21.1).
3. Get some **sand** and put it on your head — any of these works:
   - **Look at the sky/open air and right-click** while holding sand (the
     easiest: vanilla air-use of sand does nothing, so the mod uses it to
     place one block on your head).
   - **Sneak + right-click while looking at a block** while holding sand (this
     equips instead of placing the block; without sneaking, sand places
     normally).
   - **Creative / commands:** put sand directly into the helmet slot, or run
     `/item replace entity @s armor.head from minecraft:sand`.

   Your previous helmet is returned to your inventory. In survival one sand
   block is consumed; in creative it isn't.
4. The moment sand occupies the helmet slot (normally the very next tick) you
   are pulled into `backrooms:backrooms`. The trigger checks your dimension, so
   it fires exactly once and never spams teleports. If you somehow end up with
   sand on your head in the Overworld again (e.g. `/item`), it teleports you
   once more.
5. Wander. Watch the lights. If you see the Still Life, don't blink.

### Sanity

A **Sanity** readout appears above your hotbar while you're in the Backrooms.
It drains in Level 0 — **twice as fast in darkness** and several times faster
when the Still Life is near (the HUD flashes *DON'T BLINK*). Below one third
you start seeing darkness pulse at the edges of your vision; below that come
nausea, weakness and slowness. Sanity **regenerates in the Overworld** and —
gently — **while you stay in the Poolrooms**, and is restored instantly by
drinking **Almond Water**. Sanity is saved with the player and kept across
death.

### Supply chests and Almond Water

Rare **supply chests** generate in Level 0 (about one in every handful of
chunks, always with headroom and loot attached). They usually contain one or
more bottles of **Almond Water**, plus occasional food, lanterns and glow
berries. Drinking Almond Water restores 60% sanity, grants a short regeneration
burst, and returns the empty glass bottle. Bottles also show up in the
**Food & Drinks** creative tab.

### The Poolrooms and the exit

Some Level 0 corridors hide a sunken **Sunken Portal** basin: a 3×3 tiled
pool with a glowing sea-lantern floor and a shimmering turquoise drain at
the centre, ringed by a tiled deck. Dive in and **submerge** — ducking under
the dark water pulls you through into `backrooms:poolrooms` — a second
infinite procedural dimension:

- An **indoor, fully roofed maze**: tiled partitions with doorways laid out by
  a depth-first spanning tree (every room reachable), shallow still-water
  pools a single block deep, ceiling lamps and lamp pillars. Wide archways
  connect every cell.
- Rare **great halls** — roughly three rooms in a hundred — have no partitions and
  walls twice as tall, ringed by a band of high **windows** under a full
  **glass roof**. They are the only places in the Poolrooms where you can see
  the sky and the fast sun.
- The Poolrooms run on a **5-minute full day/night cycle** (2½ minutes of day,
  2½ minutes of night), visible through the glass of the great halls and
  driven independently of the Overworld clock.
- **During the day the pool water runs hot** — swimming while the sun is up
  scalds you (2 fire damage per second) and warns you in the hotbar. The water
  is safe after dark. Plan your crossings, or move at night.
- Matching portal basins lead **back to Level 0**. A short cooldown stops
  instant bounce-back, and arrival always places you on dry tile after a
  splash of bubbles.

> **Tip:** dragging sand into the helmet slot with the mouse does **not** work
> in a survival inventory screen — vanilla refuses non-armor items there. Use
> one of the click routes above.

The Poolrooms are an exit onward from Level 0, not from the Backrooms — dying
still uses normal Minecraft respawn rules (you wake up back in the Overworld
at your spawn point, and your Sanity comes with you).

### The Still Life

- A tall, gaunt mannequin in a **tricorn hat**, black beard, blood-streaked
  yellow vest and teal sleeves, standing posed in the halls.
- **It cannot move while anyone is looking at it** — not a twitch; it simply
  stands there, head slowly turning to meet your eyes. The instant line of
  sight is broken (you turn away, blink around a corner, look at a friend),
  it **sprints the gap** and attacks in a flurry of creaking timber.
- It is **uncommon by design**. A custom, configurable spawner places at most
  a couple near each player, mostly in dim areas. It respects
  `doMobSpawning` and disappears on **Peaceful** difficulty.
- In a pinch you can spawn one for testing with the **Still Life Spawn Egg**
  (creative menu) or `/summon backrooms:still_life`.

---

## Building locally

Requirements: **JDK 21** (Temurin/Adoptium recommended). Internet access is
needed on the first build so Gradle can download Fabric, Minecraft and
mappings.

### Windows

```bat
gradlew.bat build
```

### macOS / Linux

```bash
./gradlew build
```

The finished, remapped mod jars appear in:

```
build/libs/
```

- **`build/libs/backrooms-1.0.8.jar`** ← the file to put in your `mods/` folder
- `backrooms-1.0.8-sources.jar` — sources only, not needed to play

To run a dev client/server: `./gradlew runClient` / `./gradlew runServer`.

### Other Gradle tasks

| Task | Purpose |
| --- | --- |
| `gradlew build` | Compile, remap and package the mod jar |
| `gradlew runClient` | Launch a dev Minecraft client |
| `gradlew runServer` | Launch a headless dev server |
| `gradlew runServer -Pselftest` | Headless in-engine smoke test (CI) |

---

## Installation for players

1. Install Fabric Loader for **1.21.1** (<https://fabricmc.net/use/installer/>).
2. Download **Fabric API** for 1.21.1 and put it in `mods/`.
3. Put **`backrooms-1.0.8.jar`** in `mods/`.
4. Launch the **fabric-loader-1.21.1** profile.

---

## Configuration

On first server start the mod writes `config/backrooms.properties`:

| Property | Default | Meaning |
| --- | --- | --- |
| `stillLifeEnabled` | `true` | Master switch for the custom spawner (old `bacteria*` keys still read) |
| `stillLifeSpawnIntervalTicks` | `200` | Attempt a spawn for each player every N ticks (20 ticks = 1 s) |
| `stillLifeSpawnChance` | `0.10` | Probability an attempt actually spawns one |
| `stillLifeMaxNearPlayer` | `2` | Hard cap on Still Lives within 64 blocks of a player |
| `stillLifeMinSpawnDistance` | `22` | Never spawn closer than this |
| `stillLifeMaxSpawnDistance` | `46` | Search out to this distance |
| `normalSandOnly` | `true` | Reserved for later (red sand option) |

---

## How the procedural generation works

Everything lives in `src/main/java/net/backrooms/worldgen/`.

The level is the intersection of two families of wall lines on a
**deterministic jittered lattice** (`Level0Layout`):

- Lattice lines are nominally **24 blocks** apart; room lines jitter by ±5
  blocks, every third line is perfectly straight and is carved into a
  **3-block-wide corridor** with open intersections.
- Each wall segment between junctions is independently **solid**,
  **door-punched** (2-wide gap), or **missing entirely** (merging adjacent
  rooms into larger ones). Rare larger rooms emerge from consecutive missing
  segments and from occasional permanently dark fixture patches.
- The strips between the corridor bands are split into **cells**. About 20 %
  of cells are left as wide-open rooms (sometimes merging through a missing
  perimeter wall). The rest are **maze rooms**: a 2-3 x 2-3 grid of sub-rooms
  partitioned by walls whose openings are laid out with a deterministic
  depth-first spanning-tree search (every sub-room reachable, genuine winding
  passages), plus a few extra loop openings so it doesn't degenerate into a
  pure tree maze. Cell maze data is cached per chunk, so the extra walls cost
  essentially no generation time.
- Doors are decided by hashing the world seed together with the segment
  coordinates, so the layout is a **pure function of `(seed, x, z)`**. Because
  chunk generation asks that same function for its 16×16 columns, walls and
  openings line up perfectly across chunk borders with no stored adjacency
  state.
- To guarantee the entire floor plan is explorable (no sealed rooms), the
  one cell between corridors that doesn't open directly onto a hallway is
  tested for enclosure; if all four of its walls rolled solid, one side is
  deterministically forced open.

The vertical slice is constant and cheap: foundation below, carpet floor at
Y=64, four blocks of interior, an acoustic-tile ceiling at Y=69 with
fluorescent fixtures, and a roof slab above. `BackroomsChunkGenerator.fillFromNoise`
writes only those ~9 block rows per column (~2,300 block writes per chunk);
there are no features, carvers, structure starts, caves or terrain noise, so
generation stays fast and world saves stay small. Lighting uses the vanilla
light engine — no per-block custom calculations.

### Lights

- Corridors get a bright, regular row of fixtures; rooms use a 6-block grid.
- Some fixtures are **dead** (dark), including occasional larger dark patches.
- Fixtures marked "flicker" own a tiny block entity that schedules long calm
  periods, short flicker bursts and rare multi-second blackouts. Only the
  block state changes, so relighting is incremental. About 1 in 4 room
  fixtures has an entity, keeping block-entity counts negligible.

### Still Life spawning and the freeze rule

Vanilla monster spawning requires darkness; Level 0 is mostly lit, so
`StillLifeSpawner` runs sparse per-player attempts, finds a real carpet-floor
position with headroom 22–46 blocks away (preferring dim spots), enforces a
near-player cap, and spawns a normal despawnable monster using vanilla
navigation through the generated doorways. Its single custom goal
(`StillLifeHuntGoal`) scans nearby players every tick: while any player faces
it within 48 blocks with line of sight, it stops dead and only turns its
head; unseen, it sprints at the target and strikes. The check is a view-vector
dot product plus vanilla `hasLineOfSight`, so pillars, corners and genuine
blinks all release it.

### The Poolrooms generator

`PoolroomsLayout` is the same pure-function technique applied a second way:

- A 24-block lattice of pool-tile walls with **two wide archways per segment**
  guarantees every cell connects to its neighbours (flood-fill verified for
  zero enclosed pockets across many seeds in `tools/sim_pool105.py` and the
  in-game self-test).
- Every ordinary cell is a **roofed maze room**: two partition lines divide
  it into a 3×3 of sub-rooms, and a deterministic per-cell depth-first
  spanning tree (plus ~12% loop openings) cuts the doorways, so every room is
  reachable and the maze never repeats a visible 16×16 pattern. 45% of cells
  hold a shallow pool; the rest are dry decks. A tile ceiling with embedded
  sea-lantern lamps and a foundation roof close every room at y70/71.
- About 3% of cells are **great halls**: no partitions, walls up to y85 with a
  continuous glass **window band** (y78–81), four tall lamp pillars, hanging
  lanterns and a full **glass roof at y86** — the only Poolrooms columns open
  to the sky. Walls bordering a hall are tall on both sides, so the feature
  agrees across chunk and cell borders.
- Rare sunken portal basins (glowing lantern floor + `pool_portal` drain under
  two-deep water) return the player to Level 0; Level 0's basins enter it.
  The transfer fires while the player's head is underwater inside a basin,
  wrapped in bubbles and a splash.
- The fast day/night phase is owned by `PoolroomsEnvironmentHandler`, because
  vanilla gives non-Overworld dimensions a read-only clock derived from the
  Overworld. Small mixins into `Level`/`ClientLevel` expose the independent
  phase so sky light, the sun/moon and the time packet all follow the 5-minute
  cycle; the client advances its own copy 4× between server syncs.

### Scale / infinity

The dimension uses a normal (finite-seed) world border at the engine's
±~30 million block limit — effectively infinite for any play session; chunks
generate lazily on demand and the same seed always yields the same floor plan
at any distance. No region is pre-generated or held in memory beyond vanilla
chunk loading/unloading.

---

## Project layout (future-proofing)

```
src/main/java/net/backrooms/
├── Backrooms.java                 # entrypoint / event wiring
├── ModBlocks / ModItems / ...     # registries, one class per content kind
├── config/BackroomsConfig         # tunables (spawn rates today, more later)
├── mechanics/
│   ├── SandHelmetHandler.java     # entry trigger + survival equip action
│   ├── StillLifeSpawner.java      # custom natural spawner
│   ├── PoolPortalHandler.java     # Level 0 <-> Poolrooms teleport pads
│   └── PoolroomsEnvironmentHandler.java  # 5-minute cycle + hot daytime water
├── sanity/
│   ├── ModAttachments.java        # persistent, death-copied Sanity attachment
│   └── SanityHandler.java         # drain, low-sanity effects, action-bar HUD
├── item/AlmondWaterItem.java      # drinkable sanity restore
├── worldgen/
│   ├── BackroomsChunkGenerator.java + Level0Layout.java   # Level 0
│   ├── PoolroomsChunkGenerator.java + PoolroomsLayout.java
│   └── block/                     # fluorescent/flicker + non-solid portal drain
├── mixin/LevelMixin.java          # independent Poolrooms clock (server)
├── entity/StillLifeEntity.java    # attributes/sounds + StillLifeHuntGoal (freeze rule)
└── selftest/SelfTest.java         # CI headless smoke test
src/client/java/net/backrooms/client/  # model, renderer, dimension effects
tools/gen_textures.py, gen_sounds.py, gen_assets.py   # regenerate all assets
tools/sim_layout.py, sim_pool.py, sim_pool105.py   # headless layout validators
```

Adding new Backrooms *levels* means adding another layout + dimension
(dimension-type / dimension / biome JSON + chunk generator, registered in
`ModWorldgen`), with `Level0Layout` / `PoolroomsLayout` as the two templates;
items, entities, sounds, dimensions, attachments, loot tables and game rules
are all independently registered so new systems (objectives, more loot,
multiplayer events) slot in without touching the existing code paths.

All textures and sounds in this mod are generated procedurally by the scripts
in `tools/` (Python + Pillow + imageio-ffmpeg). **No external/copyrighted
assets are used.**

---

## GitHub Actions

Two workflows live in `.github/workflows/`:

- **`build.yml`** — builds on every push/PR with JDK 21, fails on compile
  errors, and uploads the actual remapped jars as a workflow artifact named
  **`backrooms-mod`**. After a run, open its **Summary → Artifacts** section to
  download `backrooms-1.0.8.jar`.
- **`release.yml`** — pushing a version tag such as **`v1.0.0`** builds the
  jar and attaches it to a GitHub Release automatically (no secrets beyond the
  default `GITHUB_TOKEN`).

A headless in-engine self-test also runs in CI, generating chunks thousands of
blocks out, asserting full connectivity in both dimensions, correct block
structure, lighting and Still Life lifecycle, the sand-helmet teleport, supply
chest loot and Almond Water, the Poolrooms water/pad generation, and the
independent 5-minute clock (60+ assertions in total).

---

## Implemented features

- [x] Sand-in-helmet-slot transport into a dedicated `backrooms:backrooms` dimension
- [x] Sneak + right-click equip route for survival
- [x] One-shot teleport guard (dimension based) + title/sound feedback
- [x] Seed-deterministic, effectively infinite procedural **Level 0**
- [x] Corridors, intersections, rooms, large merged rooms, dead ends, jittered walls
- [x] Guaranteed connectivity (no sealed/unreachable pockets)
- [x] Yellow/damp wallpaper, old carpet, acoustic ceiling, foundation, fixtures
- [x] Bright lights, flickering lights, long blackouts, dark patches, ambient biome hum
- [x] **Still Life** entity (freezes under observation) with custom model, texture and sounds
- [x] Uncommon, capped, configurable natural spawner (peaceful/gamerule aware)
- [x] **Sanity system** — drains in the Backrooms (drains in darkness / near the Still Life; regenerates in the Poolrooms),
      low-sanity effects, action-bar HUD, Overworld regen, saved + kept on death
- [x] **Almond Water** drink (60% sanity + regeneration), glass bottle returned,
      creative tab, custom texture
- [x] **Supply chests** placed procedurally in Level 0 with a custom loot table
      (Almond Water + supporting supplies)
- [x] **Poolrooms dimension** (`backrooms:poolrooms`): fully roofed indoor
      DFS maze of tile rooms and archways, shallow pools, ceiling/pillar
      lamps, and rare tall **great halls** ringed with windows under a glass
      roof (the only rooms open to the fast sky); dedicated biome and generator
- [x] **Sanity regenerates while staying in the Poolrooms**
- [x] **Sunken Portal basins** in both dimensions — dive, submerge, get pulled
      through (teleport, titles, sounds, bubbles, cooldown)
- [x] **5-minute day/night cycle** independent of the Overworld (mixin-served clock,
      smooth client sun) and **hot water that scalds swimmers during the day**
- [x] Custom dimension types, biomes, fog/sky colours, empty sky for Level 0
- [x] Spawn egg + creative tabs, loot tables, English localisation
- [x] Normal death/respawn behaviour
- [x] Build artifact + release workflows, headless CI self-test, Gradle wrapper

## Known limitations / roadmap

- Portals travel between Level 0 and the Poolrooms; there is no way back to the
  Overworld except dying (normal respawn) — more exits/levels come later.
- The hot-water effect uses fire damage ticks; fire itself is extinguished by
  the water (you take damage without visually igniting).
- Still Life AI relies on vanilla navigation (robust in the 2-wide doorways); it
  cannot open doors (there are none) or break walls.
- Mining through the floor drops you toward the void; mining through the roof
  reveals the dark shell above the ceiling.
- Planned later: more levels and level transitions, more entities, items and
  weapons, rare/secret rooms, more loot, objectives, random events, richer
  pathfinding and multiplayer-tuned events.

---

---

## Changelog

### v1.0.8 — Sunken Portal basins

- Portal pads remade as immersive dive-through basins: sunken 3x3 tiled
  pools with a glowing lantern floor, a shimmering drain, and (in Level 0)
  a tiled deck ring. Ducking under the dark water pulls you through.
- The dive is wrapped in feedback: bubble bursts, a splash, a fresh breath
  of air, and a splashy arrival on dry tile. The old walk-through
  head-height portal is gone (`pool_portal` is now the basin drain, with a
  bubble column rising from it).
- Self-test checks the basin structure: glowing ring, two-deep water with
  a flush surface, deck tiles, and every planned pad building a drain.

### v1.0.7 — Still Life remodel

- Remodelled the Still Life after the reference photo: a bigger beard
  wrapping the jaw, a larger tricorn crown, flared coat-skirt panels with a
  painted sash knot and hanging ends, brass waistcoat buttons, a repositioned
  belt and buckle, socketed staring eyes, weathered skin and grimy hands.
- Poolrooms great halls (the big windowed rooms) are rarer: 3% of cells
  instead of 6%.
- Fixed missing foundation texture (magenta-black bands visible through
  great-hall arches); every model texture now resolves.

### v1.0.6 — bug-fix pass over v1.0.5

- Still Life model pivots fixed — the figure rendered sunk into the floor;
  feet now land on the ground and the 2.8-block figure matches its hitbox.
- Overworld sanity regen fixed (was 20x/tick; now ~4 min for a full bar,
  only slightly faster than the Poolrooms as documented).
- `normalSandOnly=false` now actually allows red sand as the entry trigger.
- Still Life ignores creative/spectator players, swings when it attacks, and
  no longer gets stuck in its frozen pose.
- Registered the Still Life's natural-spawn placement (the biome spawner
  entry previously had no defined placement).
- Level 0 arrival search loads chunks before reading spawn columns.
- `tools/gen_assets.py` finished the bacteria -> Still Life migration and is
  byte-identical with the checked-in assets again; ambient subtitle wired.
- Config typos now warn and fall back to defaults instead of crashing startup.
