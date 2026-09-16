#!/usr/bin/env python3
"""Generates data/JSON assets (blockstates, models, loot tables, lang, ...)."""
import json
import os

MAIN = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
A = os.path.join(MAIN, "assets", "backrooms")
D = os.path.join(MAIN, "data", "backrooms")


def w(path, obj):
    full = os.path.join(MAIN, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


SIMPLE = ["wallpaper", "wallpaper_damp", "carpet", "ceiling_tile", "foundation", "pool_tile"]

for name in SIMPLE:
    w(f"assets/backrooms/blockstates/{name}.json", {
        "variants": {"": {"model": f"backrooms:block/{name}"}}
    })
    w(f"assets/backrooms/models/block/{name}.json", {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": f"backrooms:block/{name}"},
    })
    w(f"assets/backrooms/models/item/{name}.json", {"parent": f"backrooms:block/{name}"})

# fluorescent fixture with lit/unlit states
w("assets/backrooms/blockstates/fluorescent.json", {
    "variants": {
        "lit=true": {"model": "backrooms:block/fluorescent"},
        "lit=false": {"model": "backrooms:block/fluorescent_off"},
    }
})
w("assets/backrooms/models/block/fluorescent.json", {
    "parent": "minecraft:block/cube_all",
    "textures": {"all": "backrooms:block/fluorescent"},
})
w("assets/backrooms/models/block/fluorescent_off.json", {
    "parent": "minecraft:block/cube_all",
    "textures": {"all": "backrooms:block/fluorescent_off"},
})
w("assets/backrooms/models/item/fluorescent.json", {"parent": "backrooms:block/fluorescent"})

# pool portal (non-solid in code, full glowing cube model)
w("assets/backrooms/blockstates/pool_portal.json", {
    "variants": {"": {"model": "backrooms:block/pool_portal"}}
})
w("assets/backrooms/models/block/pool_portal.json", {
    "parent": "minecraft:block/cube_all",
    "textures": {"all": "backrooms:block/pool_portal"},
})
w("assets/backrooms/models/item/pool_portal.json", {"parent": "backrooms:block/pool_portal"})

# spawn egg item model
w("assets/backrooms/models/item/still_life_spawn_egg.json", {
    "parent": "minecraft:item/template_spawn_egg"
})

# almond water item model (flat item texture)
w("assets/backrooms/models/item/almond_water.json", {
    "parent": "minecraft:item/generated",
    "textures": {"layer0": "backrooms:item/almond_water"},
})

# sounds.json
w("assets/backrooms/sounds.json", {
    "still_life.ambient": {
        "category": "hostile",
        "subtitle": "subtitles.backrooms.still_life.ambient",
        "sounds": [{"name": "backrooms:still_life/ambient"}],
    },
    "still_life.hurt": {
        "category": "hostile",
        "sounds": [{"name": "backrooms:still_life/hurt"}],
    },
    "still_life.death": {
        "category": "hostile",
        "sounds": [{"name": "backrooms:still_life/death"}],
    },
    "ambient.hum": {
        "category": "ambient",
        "sounds": [{"name": "backrooms:ambient/hum", "stream": True}],
    },
})

# loot tables (simple drop-self), 1.21 uses data/<ns>/loot_table/
for name in SIMPLE + ["fluorescent"]:
    w(f"data/backrooms/loot_table/blocks/{name}.json", {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item", "name": f"backrooms:{name}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    })

# dimension type
w("data/backrooms/dimension_type/backrooms.json", {
    "ultrawarm": False,
    "natural": False,
    "piglin_safe": True,
    "respawn_anchor_works": False,
    "bed_works": False,
    "has_raids": False,
    "has_skylight": False,
    "has_ceiling": True,
    "coordinate_scale": 1.0,
    "ambient_light": 0.12,
    "fixed_time": 18000,
    "logical_height": 256,
    "effects": "backrooms:backrooms",
    "infiniburn": "#minecraft:infiniburn_overworld",
    "min_y": 0,
    "height": 256,
    "monster_spawn_light_level": 0,
    "monster_spawn_block_light_limit": 0,
})

# biome
w("data/backrooms/worldgen/biome/backrooms.json", {
    "has_precipitation": False,
    "temperature": 0.9,
    "downfall": 0.0,
    "effects": {
        "fog_color": 13267064,
        "sky_color": 0,
        "water_color": 9660890,
        "water_fog_color": 6510628,
        "ambient_sound": "backrooms:ambient.hum",
        "mood_sound": {
            "block_search_extent": 8,
            "offset": 2.0,
            "sound": "minecraft:ambient.cave",
            "tick_delay": 6000,
        },
    },
    "spawners": {
        "monster": [{
            "type": "backrooms:still_life", "weight": 6, "minSize": 1, "maxSize": 1
        }],
        "creature": [],
        "ambient": [],
        "axolotls": [],
        "underground_water_creature": [],
        "water_creature": [],
        "water_ambient": [],
        "misc": [],
    },
    "spawn_costs": {},
    "carvers": {},
    "features": [],
})

# dimension (generator type registered by the mod)
w("data/backrooms/dimension/backrooms.json", {
    "type": "backrooms:backrooms",
    "generator": {
        "type": "backrooms:backrooms",
        "biome_source": {
            "type": "minecraft:fixed",
            "biome": "backrooms:backrooms",
        },
    },
})

# ---------------------------------------------------------------- Poolrooms
# supply chest loot (special chests scattered through Level 0)
def item_entry(name, weight, min_c, max_c):
    return {
        "type": "minecraft:item",
        "name": name,
        "weight": weight,
        "functions": [{
            "function": "minecraft:set_count",
            "count": {"type": "minecraft:uniform", "min": min_c, "max": max_c},
        }],
    }


w("data/backrooms/loot_table/chests/supply.json", {
    "type": "minecraft:chest",
    "random_sequence": "backrooms:chests/supply",
    "pools": [{
        "rolls": {"type": "minecraft:uniform", "min": 2, "max": 4},
        "bonus_rolls": 0,
        "entries": [
            item_entry("backrooms:almond_water", 12, 1, 3),
            item_entry("minecraft:glow_berries", 5, 1, 4),
            item_entry("minecraft:lantern", 4, 1, 2),
            item_entry("minecraft:bread", 4, 1, 3),
            item_entry("minecraft:golden_carrot", 2, 1, 2),
        ],
        "conditions": [],
    }],
})

# Poolrooms dimension type: open sky, no ceiling, normal overworld-style sky
w("data/backrooms/dimension_type/poolrooms.json", {
    "ultrawarm": False,
    "natural": True,
    "piglin_safe": False,
    "respawn_anchor_works": False,
    "bed_works": False,
    "has_raids": False,
    "has_skylight": True,
    "has_ceiling": False,
    "coordinate_scale": 1.0,
    "ambient_light": 0.0,
    "logical_height": 256,
    "effects": "minecraft:overworld",
    "infiniburn": "#minecraft:infiniburn_overworld",
    "min_y": 0,
    "height": 256,
    "monster_spawn_light_level": 0,
    "monster_spawn_block_light_limit": 0,
})

# Poolrooms biome: pale tiled rooms, turquoise water, normal sky colours
w("data/backrooms/worldgen/biome/poolrooms.json", {
    "has_precipitation": False,
    "temperature": 0.8,
    "downfall": 0.4,
    "effects": {
        "fog_color": 0xCDE8EE,
        "sky_color": 0x9FD4E8,
        "water_color": 0x5FCFE0,
        "water_fog_color": 0x3FA0BE,
        "mood_sound": {
            "block_search_extent": 8,
            "offset": 2.0,
            "sound": "minecraft:ambient.cave",
            "tick_delay": 6000,
        },
    },
    "spawners": {
        "monster": [],
        "creature": [],
        "ambient": [],
        "axolotls": [],
        "underground_water_creature": [],
        "water_creature": [],
        "water_ambient": [],
        "misc": [],
    },
    "spawn_costs": {},
    "carvers": {},
    "features": [],
})

# Poolrooms dimension
w("data/backrooms/dimension/poolrooms.json", {
    "type": "backrooms:poolrooms",
    "generator": {
        "type": "backrooms:poolrooms",
        "biome_source": {
            "type": "minecraft:fixed",
            "biome": "backrooms:poolrooms",
        },
    },
})

# language file
w("assets/backrooms/lang/en_us.json", {
    "block.backrooms.wallpaper": "Yellow Wallpaper",
    "block.backrooms.wallpaper_damp": "Damp Wallpaper",
    "block.backrooms.carpet": "Old Carpet",
    "block.backrooms.ceiling_tile": "Acoustic Ceiling Tile",
    "block.backrooms.foundation": "Foundation Slab",
    "block.backrooms.fluorescent": "Fluorescent Fixture",
    "block.backrooms.pool_tile": "Pool Tile",
    "block.backrooms.pool_portal": "Sunken Portal",
    "item.backrooms.almond_water": "Almond Water",
    "item.backrooms.still_life_spawn_egg": "Still Life Spawn Egg",
    "entity.backrooms.still_life": "Still Life",
    "subtitles.backrooms.still_life.ambient": "A statue creaks",
    "message.backrooms.enter": "The Backrooms",
    "message.backrooms.enter.subtitle": "You noclip out of reality...",
    "message.backrooms.enter.return.subtitle": "The water spits you back out...",
    "message.backrooms.enter.poolrooms": "The Poolrooms",
    "message.backrooms.enter.poolrooms.subtitle": "Warm tiles and endless water...",
    "message.backrooms.hot_water": "The water is boiling hot!",
    "message.backrooms.equipped": "Sand placed on your head...",
})

print("done")
