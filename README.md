# The Backrooms — Minecraft 1.21.1 (Fabric)

Put **sand on your head** and noclip into an endless, procedurally generated
**Level 0** — yellow rooms, damp carpet, humming fluorescent lights, long
hallways, junctions, dead ends, and the **Bacteria** hunting you through it
all. Built for **Minecraft Java Edition 1.21.1** with the **Fabric** mod
loader.

> Core loop (v1): **sand helmet → enter the Backrooms → explore the infinite
> procedural Level 0 → encounter the Bacteria → survive.**

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
5. Wander. Watch the lights. Listen for the growl.

> **Tip:** dragging sand into the helmet slot with the mouse does **not** work
> in a survival inventory screen — vanilla refuses non-armor items there. Use
> one of the click routes above.

There is no escape mechanic in v1 — dying uses normal Minecraft respawn
rules (you wake up back in the Overworld at your spawn point).

### The Bacteria

- A tall, emaciated humanoid that wanders the halls, senses players (even
  through walls), and sprints into melee range.
- It is **uncommon by design**. A custom, configurable spawner places at most
  a couple near each player, mostly in dim areas. It respects
  `doMobSpawning` and disappears on **Peaceful** difficulty.
- In a pinch you can spawn one for testing with the **Bacteria Spawn Egg**
  (creative menu) or `/summon backrooms:bacteria`.

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

- **`build/libs/backrooms-1.0.1.jar`** ← the file to put in your `mods/` folder
- `backrooms-1.0.1-sources.jar` — sources only, not needed to play

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
3. Put **`backrooms-1.0.1.jar`** in `mods/`.
4. Launch the **fabric-loader-1.21.1** profile.

---

## Configuration

On first server start the mod writes `config/backrooms.properties`:

| Property | Default | Meaning |
| --- | --- | --- |
| `bacteriaEnabled` | `true` | Master switch for the custom spawner |
| `bacteriaSpawnIntervalTicks` | `200` | Attempt a spawn for each player every N ticks (20 ticks = 1 s) |
| `bacteriaSpawnChance` | `0.10` | Probability an attempt actually spawns one |
| `bacteriaMaxNearPlayer` | `2` | Hard cap on Bacteria within 64 blocks of a player |
| `bacteriaMinSpawnDistance` | `22` | Never spawn closer than this |
| `bacteriaMaxSpawnDistance` | `46` | Search out to this distance |
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

### Bacteria spawning

Vanilla monster spawning requires darkness; Level 0 is mostly lit, so
`BacteriaSpawner` runs sparse per-player attempts, finds a real carpet-floor
position with headroom 22–46 blocks away (preferring dim spots), enforces a
near-player cap, and spawns a normal despawnable monster that uses vanilla
goal-based AI and navigation through the generated doorways.

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
│   └── BacteriaSpawner.java       # custom natural spawner
├── worldgen/
│   ├── BackroomsChunkGenerator.java
│   ├── Level0Layout.java          # seed-deterministic floor plan ("Level N" seam)
│   └── block/                     # fluorescent block + flicker block entity
├── entity/BacteriaEntity.java     # goals/attributes/sounds
└── selftest/SelfTest.java         # CI headless smoke test
src/client/java/net/backrooms/client/  # model, renderer, dimension effects
tools/gen_textures.py, gen_sounds.py, gen_assets.py   # regenerate all assets
```

Adding new Backrooms *levels* later means adding another layout + dimension
(level JSON + chunk generator), with `Level0Layout` as the template; items,
entities, sounds, dimensions and game rules are all independently registered
so new systems (sanity, objectives, loot, escape routes, multiplayer events)
slot in without touching the existing code paths.

All textures and sounds in this mod are generated procedurally by the scripts
in `tools/` (Python + Pillow + imageio-ffmpeg). **No external/copyrighted
assets are used.**

---

## GitHub Actions

Two workflows live in `.github/workflows/`:

- **`build.yml`** — builds on every push/PR with JDK 21, fails on compile
  errors, and uploads the actual remapped jars as a workflow artifact named
  **`backrooms-mod`**. After a run, open its **Summary → Artifacts** section to
  download `backrooms-1.0.1.jar`.
- **`release.yml`** — pushing a version tag such as **`v1.0.0`** builds the
  jar and attaches it to a GitHub Release automatically (no secrets beyond the
  default `GITHUB_TOKEN`).

A headless in-engine self-test also runs in CI, generating chunks thousands of
blocks out, asserting full connectivity, correct block structure, lighting and
Bacteria lifecycle, and the sand-helmet teleport.

---

## Implemented features (v1)

- [x] Sand-in-helmet-slot transport into a dedicated `backrooms:backrooms` dimension
- [x] Sneak + right-click equip route for survival
- [x] One-shot teleport guard (dimension based) + title/sound feedback
- [x] Seed-deterministic, effectively infinite procedural **Level 0**
- [x] Corridors, intersections, rooms, large merged rooms, dead ends, jittered walls
- [x] Guaranteed connectivity (no sealed/unreachable pockets)
- [x] Yellow/damp wallpaper, old carpet, acoustic ceiling, foundation, fixtures
- [x] Bright lights, flickering lights, long blackouts, dark patches, ambient biome hum
- [x] **Bacteria** entity with custom model, texture, sounds and melee AI
- [x] Uncommon, capped, configurable natural spawner (peaceful/gamerule aware)
- [x] Custom dimension type, biome, fog colour and empty sky/weather
- [x] Spawn egg + creative tabs, loot tables, English localisation
- [x] Normal death/respawn behaviour
- [x] Build artifact + release workflows, headless CI self-test, Gradle wrapper

## Known limitations / roadmap

- No escape mechanic yet (death returns you to the Overworld).
- Bacteria AI relies on vanilla navigation (robust in the 2-wide doorways); it
  cannot open doors (there are none) or break walls.
- Mining through the floor drops you toward the void; mining through the roof
  reveals the dark shell above the ceiling.
- Planned later: more levels and level transitions, more entities, items and
  weapons, sanity system, rare/secret rooms, loot, objectives, random events,
  richer pathfinding and multiplayer-tuned events.
