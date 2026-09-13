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


SIMPLE = ["wallpaper", "wallpaper_damp", "carpet", "ceiling_tile", "foundation"]

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

# spawn egg item model
w("assets/backrooms/models/item/bacteria_spawn_egg.json", {
    "parent": "minecraft:item/template_spawn_egg"
})

# sounds.json
w("assets/backrooms/sounds.json", {
    "ambient.hum": {
        "category": "ambient",
        "sounds": [{"name": "backrooms:ambient/hum", "stream": True}],
    },
    "bacteria.ambient": {
        "category": "hostile",
        "sounds": [{"name": "backrooms:bacteria/ambient"}],
    },
    "bacteria.hurt": {
        "category": "hostile",
        "sounds": [{"name": "backrooms:bacteria/hurt"}],
    },
    "bacteria.death": {
        "category": "hostile",
        "sounds": [{"name": "backrooms:bacteria/death"}],
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
            "type": "backrooms:bacteria", "weight": 6, "minSize": 1, "maxSize": 1
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

# language file
w("assets/backrooms/lang/en_us.json", {
    "block.backrooms.wallpaper": "Yellow Wallpaper",
    "block.backrooms.wallpaper_damp": "Damp Wallpaper",
    "block.backrooms.carpet": "Old Carpet",
    "block.backrooms.ceiling_tile": "Acoustic Ceiling Tile",
    "block.backrooms.foundation": "Foundation Slab",
    "block.backrooms.fluorescent": "Fluorescent Fixture",
    "item.backrooms.bacteria_spawn_egg": "Bacteria Spawn Egg",
    "entity.backrooms.bacteria": "Bacteria",
    "message.backrooms.enter": "The Backrooms",
    "message.backrooms.enter.subtitle": "You noclip out of reality...",
    "message.backrooms.equipped": "Sand placed on your head...",
})

print("done")
