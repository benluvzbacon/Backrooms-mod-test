#!/usr/bin/env python3
"""Emits the Minecraft signatures we need as GitHub workflow annotations.
Annotation messages are capped at 4 KiB and 10 per step, so this tool makes
compact targeted extracts (method declarations + small files) and shards them
into bundles selected by the SHARD argument (one workflow step per shard)."""
import os
import re
import sys

SRC = "/tmp/mcsrc"
MAX = 3800
PER_SHARD = 8
SHARD = int(sys.argv[1]) if len(sys.argv) > 1 else 0
BUNDLES = []


def read(rel):
    full = os.path.join(SRC, rel)
    if not os.path.exists(full):
        return None
    with open(full, errors="replace") as f:
        return f.read()


def add(title, text):
    if text is None:
        text = "MISSING"
    BUNDLES.append((title, text[:MAX]))


def full(rel, rename=None):
    add(rename or rel.split("/")[-1], read(rel))


def grep(rel, patterns, before=2, after=6, rename=None, head=None):
    content = read(rel)
    if content is None:
        add(rename or rel.split("/")[-1], None)
        return
    lines = content.splitlines()
    out, picked = [], set()
    for i, line in enumerate(lines):
        if any(re.search(p, line) for p in patterns):
            for j in range(max(0, i - before), min(len(lines), i + after + 1)):
                picked.add(j)
    for i in sorted(picked):
        out.append(lines[i])
    text = "\n".join(out)
    if head:
        text = text[:head]
    add(rename or rel.split("/")[-1], text)


def section(rel, start, end, rename=None):
    content = read(rel)
    if content is None:
        add(rename or rel.split("/")[-1], None)
        return
    text = "\n".join(content.splitlines()[start:end])
    add(rename or rel.split("/")[-1], text)


CG = "net/minecraft/world/level/chunk/ChunkGenerator.java"

if SHARD == 0:
    # ChunkGenerator structure: abstract/overridable surface (split in two)
    grep(CG, [r"^\s*(public|protected)\s"], before=0, after=4, rename="ChunkGenerator-A", head=MAX)
    full("net/minecraft/world/level/block/EntityBlock.java")
    full("net/minecraft/world/level/block/entity/BlockEntityTicker.java")
    grep("net/minecraft/world/level/chunk/ChunkAccess.java",
         [r"setBlockState|setBlockEntity|getOrCreateSection|getSectionIndex|getSections|getOrCreateHeightmapUnprimed"],
         rename="ChunkAccess-api")
    grep("net/minecraft/world/level/chunk/LevelChunkSection.java",
         [r"public BlockState setBlockState|hasOnlyAir|getBlockState"], rename="ChunkSection")
    grep("net/minecraft/core/registries/BuiltInRegistries.java", [r"CHUNK_GENERATOR"], after=1)
    grep("net/minecraft/world/level/biome/FixedBiomeSource.java", [r"CODEC|public FixedBiomeSource"], rename="FixedBiomeSource")
    grep("net/minecraft/world/level/biome/BiomeSource.java",
         [r"CODEC|abstract|public .*getNoiseBiome"], before=0, after=3, rename="BiomeSource-api")
    full("net/minecraft/world/level/NoiseColumn.java", rename="NoiseColumn")

if SHARD == 1:
    full("net/minecraft/world/level/block/entity/BlockEntity.java")
    grep("net/minecraft/world/level/block/entity/BlockEntityType.java",
         [r"public static.*of\(|BlockEntityType<.*> build|class Builder|interface BlockEntitySupplier"],
         rename="BlockEntityType-api")
    grep("net/minecraft/world/entity/EntityType.java",
         [r"public static .*Builder.*of\(|EntityType\.Builder.*sized|public Builder.*sized|clientTrackingRange|updateInterval|public EntityType.*build\("],
         rename="EntityType-api")
    full("net/minecraft/world/entity/EntityDimensions.java")
    grep("net/minecraft/world/item/SpawnEggItem.java", [r"public SpawnEggItem"], after=4)
    full("net/minecraft/sounds/SoundEvent.java")
    full("net/minecraft/world/InteractionResult.java")
    grep("net/minecraft/server/level/ServerPlayer.java",
         [r"public void teleportTo\("], before=1, after=8, rename="ServerPlayer-teleport")

if SHARD == 2:
    full("net/minecraft/client/renderer/DimensionSpecialEffects.java")
    full("net/minecraft/client/model/HierarchicalModel.java")
    grep("net/minecraft/client/model/EntityModel.java",
         [r"public|protected"], before=0, after=2, rename="EntityModel-api")
    full("net/minecraft/client/model/ModelPart.java")
    for f in ["LayerDefinition", "MeshDefinition", "PartDefinition", "CubeListBuilder", "PartPose"]:
        grep(f"net/minecraft/client/model/geom/builders/{f}.java",
             [r"public |public static "], before=0, after=3, rename=f)
    grep("net/minecraft/client/renderer/entity/MobRenderer.java",
         [r"public MobRenderer"], after=8)
    grep("net/minecraft/client/renderer/entity/LivingEntityRenderer.java",
         [r"public LivingEntityRenderer"], after=8)

if SHARD == 3:
    full("net/minecraft/world/entity/monster/Monster.java")
    grep("net/minecraft/world/entity/Mob.java",
         [r"setPathfindingMalus|getAmbientSoundInterval|protected SoundEvent|public Mob\(|finalizeSpawn"],
         rename="Mob-api")
    goals = [
        ("MeleeAttackGoal", r"public MeleeAttackGoal"),
        ("LeapAtTargetGoal", r"public LeapAtTargetGoal"),
        ("RandomStrollGoal", r"public RandomStrollGoal|protected int|public boolean canUse"),
        ("LookAtPlayerGoal", r"public LookAtPlayerGoal"),
        ("RandomLookAroundGoal", r"public RandomLookAroundGoal"),
    ]
    for name, pat in goals:
        grep(f"net/minecraft/world/entity/ai/goal/{name}.java", [pat], after=5, rename=name)
    grep("net/minecraft/world/entity/ai/goal/target/NearestAttackableTargetGoal.java",
         [r"public NearestAttackableTargetGoal"], after=6, rename="NearestAttackableTargetGoal")
    grep("net/minecraft/world/entity/LivingEntity.java",
         [r"getItemBySlot|setItemSlot|public boolean hurt\("], after=2, rename="LivingEntity-api")

if SHARD == 5:
    grep(CG, [r"withSeed|getBaseColumn|abstract int getGenDepth|abstract CompletableFuture"], rename="ChunkGenerator-C")
    grep("net/minecraft/world/level/levelgen/RandomState.java",
         [r"public|seed"], before=0, after=1, rename="RandomState-api")
    grep("net/minecraft/world/level/chunk/ChunkAccess.java",
         [r"public BlockState setBlockState|public void setBlockEntity|public LevelChunkSection|getOrCreateSection|getSectionIndex|getSections\(|getOrCreateHeightmapUnprimed|getPos\("],
         rename="ChunkAccess-B")
    full("net/minecraft/world/level/block/EntityBlock.java")
    grep("net/minecraft/world/level/chunk/LevelChunkSection.java",
         [r"public BlockState setBlockState|public boolean hasOnlyAir|public BlockState getBlockState"],
         rename="ChunkSection-B")
    grep("net/minecraft/world/entity/monster/Monster.java",
         [r"createMonsterAttributes"], before=0, after=14)
    grep("net/minecraft/world/level/block/entity/BlockEntity.java",
         [r"createTickerHelper|public BlockEntity\(|protected Level level|public Level getLevel"],
         rename="BlockEntity-api")
    grep("net/minecraft/world/level/Level.java",
         [r"getMaxLocalRawBrightness|public boolean setBlockAndUpdate|playSound"],
         rename="Level-api")

if SHARD == 6:
    full("net/minecraft/client/renderer/DimensionSpecialEffects.java")
    full("net/minecraft/client/model/HierarchicalModel.java")
    full("net/minecraft/client/model/geom/builders/TexturedModelData.java", rename="TexturedModelData-builders")
    full("net/minecraft/client/model/geom/TexturedModelData.java", rename="TexturedModelData-geom")
    for f in ["LayerDefinition", "MeshDefinition", "PartDefinition", "CubeListBuilder", "PartPose"]:
        grep(f"net/minecraft/client/model/geom/builders/{f}.java",
             [r"public |public static "], before=0, after=3, rename=f)

if SHARD == 7:
    grep("net/minecraft/client/renderer/entity/MobRenderer.java",
         [r"public MobRenderer|getTextureLocation"], after=6)
    grep("net/minecraft/client/renderer/entity/LivingEntityRenderer.java",
         [r"public LivingEntityRenderer"], after=8)
    grep("net/minecraft/client/renderer/entity/EntityRenderer.java",
         [r"public EntityRenderer|protected EntityRenderer|render\("], after=4, rename="EntityRenderer-api")
    full("net/minecraft/client/model/geom/ModelLayerLocation.java")
    grep("net/minecraft/client/renderer/entity/EntityRendererProvider.java",
         [r"interface|ModelPart bakeLayer"], rename="EntityRendererProvider")

if SHARD == 4:
    # FlatLevelSource tail (first ~95 lines already inspected previously)
    section("net/minecraft/world/level/levelgen/FlatLevelSource.java", 90, 200,
            rename="FlatLevelSource-tail")
    grep("net/minecraft/world/level/levelgen/NoiseBasedChunkGenerator.java",
         [r"public CompletableFuture|doCreateBiomes|fillFromNoise|protected.*codec|createBiomes"],
         before=0, after=6, rename="NoiseGen-api")
    grep("net/minecraft/world/level/chunk/ChunkGenerator.java",
         [r"fillBiomes|createBiomes|biomeSource|public ChunkGenerator|getSeaLevel|getGenDepth|getMinY|withSeed|applyBiomeDecoration|createReferences|buildSurface|applyCarvers|getBaseColumn|addDebugScreenInfo|spawnOriginalMobs"],
         before=0, after=5, rename="ChunkGenerator-B")

for title, text in BUNDLES[:PER_SHARD]:
    body = text.replace("%", "%25").replace("\r", "").replace("\n", "%0A")
    if len(body) > MAX:
        body = body[:MAX]
    print(f"::warning title={title}::{body}")
