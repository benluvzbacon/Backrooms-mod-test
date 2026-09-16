package net.backrooms.selftest;

import net.backrooms.ModBlocks;
import net.backrooms.ModEntities;
import net.backrooms.ModSounds;
import net.backrooms.ModWorldgen;
import net.backrooms.entity.StillLifeEntity;
import net.backrooms.worldgen.Level0Layout;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;


import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Headless in-engine smoke test, activated with {@code -Dbackrooms.selftest=true}
 * (the CI "selftest" Gradle task). Loads the Backrooms dimension on a dedicated
 * server, generates chunks thousands of blocks out, verifies the floor plan,
 * lights, full connectivity, Still Life spawning and the sand helmet teleport,
 * then stops the process (exit code 1 on any failure).
 */
public final class SelfTest {
	private static final java.util.List<String> LINES = new java.util.ArrayList<>();
	private static int failures;
	public static volatile boolean DONE;

	private SelfTest() {
	}

	public static synchronized void run(MinecraftServer server) {
		if (DONE) {
			return;
		}
		DONE = true;
		try {
			runThrowing(server);
		} catch (Throwable t) {
			fail("unexpected exception: " + t);
			t.printStackTrace();
		}
		report(server);
	}

	private static void runThrowing(MinecraftServer server) throws Exception {
		ServerLevel level = server.getLevel(ModWorldgen.BACKROOMS_LEVEL);
		check("backrooms dimension exists", level != null);
		if (level == null) {
			return;
		}
		long seed = net.backrooms.worldgen.LevelSeeds.of(level);
		log("level0 worldgen seed=" + seed + " getSeed()=" + level.getSeed()
				+ " equal=" + (seed == level.getSeed()));

		// --- pure layout: connectivity flood fill over a large region ---
		Level0Layout layout = new Level0Layout(seed);
		int r = 260;
		int margin = 90; // only assert enclosure for cells fully inside the region
		Set<Long> walkable = new HashSet<>();
		Set<Long> reached = new HashSet<>();
		for (int x = -r; x <= r; x++) {
			for (int z = -r; z <= r; z++) {
				if (!layout.isWallColumn(x, z)) {
					walkable.add(pack(x, z));
				}
			}
		}
		ArrayDeque<int[]> queue = new ArrayDeque<>();
		queue.add(new int[]{0, 0});
		reached.add(pack(0, 0));
		while (!queue.isEmpty()) {
			int[] cur = queue.poll();
			int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
			for (int[] d : dirs) {
				int nx = cur[0] + d[0];
				int nz = cur[1] + d[1];
				long key = pack(nx, nz);
				if (walkable.contains(key) && reached.add(key)) {
					queue.add(new int[]{nx, nz});
				}
			}
		}
		int enclosed = 0;
		for (Long key : walkable) {
			int x = (int) (key >> 32);
			int z = key.intValue();
			if (Math.abs(x) <= r - margin && Math.abs(z) <= r - margin && !reached.contains(key)) {
				enclosed++;
			}
		}
		log("walkable=" + walkable.size() + " reached=" + reached.size() + " enclosed(interior)=" + enclosed);
		check("interior floor plan is fully connected", enclosed == 0);

		// --- determinism / seed sensitivity ---
		Level0Layout same = new Level0Layout(seed);
		int mismatches = 0;
		for (int x = -97; x < 97; x++) {
			for (int z = -97; z < 97; z++) {
				if (layout.isWallColumn(x, z) != same.isWallColumn(x, z)) {
					mismatches++;
				}
			}
		}
		check("layout deterministic for same seed", mismatches == 0);
		Level0Layout other = new Level0Layout(seed ^ 0xDEADBEEFL);
		boolean differs = false;
		for (int x = 0; x < 200 && !differs; x++) {
			for (int z = 0; z < 200; z++) {
				if (layout.isWallColumn(x, z) != other.isWallColumn(x, z)) {
					differs = true;
					break;
				}
			}
		}
		check("different seeds produce different layouts", differs);

		// --- real chunk generation, thousands of blocks away ---
		List<int[]> probes = List.of(
				new int[]{0, 0}, new int[]{1, 1}, new int[]{-17, 33},
				new int[]{1000, -2400}, new int[]{-5000, 4200}, new int[]{12345, 12345});
		int fixtures = 0;
		for (int[] p : probes) {
			int x = p[0];
			int z = p[1];
			level.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);
			BlockState floor = level.getBlockState(BlockPos.containing(x, Level0Layout.FLOOR_Y, z));
			BlockState under = level.getBlockState(BlockPos.containing(x, Level0Layout.FLOOR_Y - 1, z));
			BlockState ceil = level.getBlockState(BlockPos.containing(x, Level0Layout.CEILING_Y, z));
			BlockState interior = level.getBlockState(BlockPos.containing(x, Level0Layout.FLOOR_Y + 2, z));
			boolean inBasin = floor.is(net.minecraft.world.level.block.Blocks.WATER);
			boolean onDeck = floor.is(ModBlocks.POOL_TILE);
			boolean ok = (floor.is(ModBlocks.CARPET) || onDeck || inBasin)
					&& (under.is(ModBlocks.FOUNDATION) || inBasin)
					&& (ceil.is(ModBlocks.CEILING_TILE) || ceil.is(ModBlocks.FLUORESCENT))
					&& (interior.isAir() || interior.is(ModBlocks.WALLPAPER) || interior.is(ModBlocks.WALLPAPER_DAMP));
			check("structure at (" + x + "," + z + ")", ok);
			if (ceil.is(ModBlocks.FLUORESCENT)) {
				fixtures++;
				if (ceil.getValue(net.backrooms.worldgen.block.FluorescentBlock.LIT)) {
					int light = level.getMaxLocalRawBrightness(BlockPos.containing(x, Level0Layout.FLOOR_Y + 2, z));
					check("bright under lit fixture (" + x + "," + z + ") light=" + light, light >= 12);
				}
			}
			if (layout.isWallColumn(x, z)) {
				BlockState wall = level.getBlockState(BlockPos.containing(x, Level0Layout.FLOOR_Y + 1, z));
				check("wall material at (" + x + "," + z + ")",
						wall.is(ModBlocks.WALLPAPER) || wall.is(ModBlocks.WALLPAPER_DAMP));
			} else {
				BlockState interior1 = level.getBlockState(BlockPos.containing(x, Level0Layout.FLOOR_Y + 1, z));
				check("open column stays clear at (" + x + "," + z + ")", interior1.isAir());
			}
		}
		log("fixtures across probes: " + fixtures);
		// Explicit, broad fixture scan around spawn (the plan grid is sparse).
		for (int cx = -3; cx <= 2; cx++) {
			for (int cz = -3; cz <= 2; cz++) {
				level.getChunk(cx, cz, ChunkStatus.FULL, true);
			}
		}
		int lit = 0;
		int dead = 0;
		for (int x = -40; x < 40; x++) {
			for (int z = -40; z < 40; z++) {
				BlockState ceil2 = level.getBlockState(BlockPos.containing(x, Level0Layout.CEILING_Y, z));
				if (ceil2.is(ModBlocks.FLUORESCENT)) {
					if (ceil2.getValue(net.backrooms.worldgen.block.FluorescentBlock.LIT)) {
						lit++;
					} else {
						dead++;
					}
				}
			}
		}
		log("spawn-area fixtures lit=" + lit + " dark=" + dead);
		check("lit fluorescent fixtures generate near spawn", lit > 0);

		// Brightness check under one placed lit fixture.
		BlockPos litPos = findFixture(level, true);
		if (litPos != null) {
			int light = level.getMaxLocalRawBrightness(litPos.below(2));
			check("bright under a lit fixture, light=" + light, light >= 12);
		} else {
			check("found a lit fixture to measure light", false);
		}

		// Deterministic plan-level checks for flickering and dead fixtures
		// somewhere nearby (exact, not probabilistic - the plan is seed fixed).
		boolean planDead = false;
		boolean planFlicker = false;
		int planLit = 0;
		for (int x = -240; x < 240 && !(planDead && planFlicker); x++) {
			for (int z = -240; z < 240; z++) {
				int f = layout.fixtureAt(x, z);
				if (f == Level0Layout.FIXTURE_DEAD) {
					planDead = true;
				} else if (f == Level0Layout.FIXTURE_FLICKER) {
					planFlicker = true;
				} else if (f == Level0Layout.FIXTURE_LIT) {
					planLit++;
				}
			}
		}
		log("plan fixtures in 480x480: lit=" + planLit + " dead=" + planDead + " flicker=" + planFlicker);
		check("plan contains bright fixtures", planLit > 0);
		check("plan contains permanently dead fixtures (dark sections)", planDead);
		check("plan contains flickering fixtures", planFlicker);

		// --- Still Life entity lifecycle ---
		BlockPos spawn = findOpen(level, 4, 4, 24);
		check("found open Still Life spawn position", spawn != null);
		if (spawn != null) {
			StillLifeEntity still = ModEntities.STILL_LIFE.create(level);
			check("Still Life entity created", still != null);
			if (still != null) {
				still.moveTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 0, 0);
				check("Still Life added to level", level.addFreshEntity(still));
				check("Still Life alive", still.isAlive());
				check("Still Life attack attribute",
						still.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) != null);
				check("Still Life ambient sound wired", ModSounds.STILL_LIFE_AMBIENT != null);
				check("Still Life starts unfrozen before observation", !still.isFrozenPose());
			}
		}

		// --- sand helmet entry mechanics ---
		// A connected player's cross-dimension teleport can't be driven
		// headlessly (FakePlayer is untracked), so exercise the equip routes
		// directly and verify the arrival-point search instead.
		check("sand is the portal trigger item",
				net.backrooms.mechanics.SandHelmetHandler.isPortalSand(new ItemStack(Items.SAND)));
		check("red sand is not the portal trigger",
				!net.backrooms.mechanics.SandHelmetHandler.isPortalSand(new ItemStack(Items.RED_SAND)));
		BlockPos arrival = net.backrooms.mechanics.SandHelmetHandler.findSpawn(level);
		check("arrival point resolves on carpet with headroom", arrival != null
				&& level.getBlockState(arrival).is(ModBlocks.CARPET)
				&& level.getBlockState(arrival.above()).isAir()
				&& level.getBlockState(arrival.above(2)).isAir());

		// Route A: right-click in the air with sand (no sneak required).
		var airFake = net.fabricmc.fabric.api.entity.FakePlayer.get(server.overworld(),
				new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "br-air"));
		airFake.getAbilities().instabuild = false;
		airFake.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.SAND, 3));
		var airResult = net.backrooms.mechanics.SandHelmetHandler.onUseItem(
				airFake, server.overworld(), net.minecraft.world.InteractionHand.MAIN_HAND);
		check("air-click with sand succeeds",
				airResult.getResult() == net.minecraft.world.InteractionResult.SUCCESS);
		check("air-click puts sand on the head",
				net.backrooms.mechanics.SandHelmetHandler.isPortalSand(
						airFake.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD)));
		check("air-click consumes one sand",
				airFake.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).getCount() == 2);

		// Route B: sneak + right-click on a block equips; without sneak it passes (normal placement).
		var blockFake = net.fabricmc.fabric.api.entity.FakePlayer.get(server.overworld(),
				new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "br-block"));
		blockFake.getAbilities().instabuild = false;
		blockFake.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.SAND, 3));
		var sneakResult = net.backrooms.mechanics.SandHelmetHandler.useBlock(
				blockFake, server.overworld(), net.minecraft.world.InteractionHand.MAIN_HAND, true);
		check("sneak+block-click with sand succeeds (no placement)",
				sneakResult == net.minecraft.world.InteractionResult.SUCCESS);
		check("sneak+block-click puts sand on the head",
				net.backrooms.mechanics.SandHelmetHandler.isPortalSand(
						blockFake.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD)));
		var normalFake = net.fabricmc.fabric.api.entity.FakePlayer.get(server.overworld(),
				new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "br-normal"));
		normalFake.getAbilities().instabuild = false;
		normalFake.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.SAND, 3));
		var normalResult = net.backrooms.mechanics.SandHelmetHandler.useBlock(
				normalFake, server.overworld(), net.minecraft.world.InteractionHand.MAIN_HAND, false);
		check("non-sneak block-click still allows placing sand (pass)",
				normalResult == net.minecraft.world.InteractionResult.PASS
						&& normalFake.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty()
						&& normalFake.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).getCount() == 3);

		// The tick watcher must not trip on players in other worlds without sand.
		net.backrooms.mechanics.SandHelmetHandler.tick(server);
		check("tick handler runs cleanly", true);

		// Regression: sand on the head must only trigger from OUTSIDE the
		// Backrooms - never from Level 0 or the Poolrooms (otherwise stepping
		// through a Pool Portal immediately yanks creative players back).
		check("sand entry armed in the Overworld",
				net.backrooms.mechanics.SandHelmetHandler.isOutsideBackrooms(
						net.minecraft.world.level.Level.OVERWORLD));
		check("sand entry inert inside Level 0",
				!net.backrooms.mechanics.SandHelmetHandler.isOutsideBackrooms(ModWorldgen.BACKROOMS_LEVEL));
		check("sand entry inert inside the Poolrooms",
				!net.backrooms.mechanics.SandHelmetHandler.isOutsideBackrooms(ModWorldgen.POOLROOMS_LEVEL));

		runV103Checks(server);
	}

	// ------------------------------------------------------------ v1.0.3
	private static void runV103Checks(MinecraftServer server) {
		ServerLevel level = server.getLevel(ModWorldgen.BACKROOMS_LEVEL);
		if (level == null) {
			check("backrooms dimension available for v1.0.3 checks", false);
			return;
		}
		// The generators must have captured the same seed LevelSeeds hands out.
		check("level0 generator captured the worldgen seed",
				level.getChunkSource().getGenerator() instanceof net.backrooms.worldgen.BackroomsChunkGenerator bg
						&& bg.getLayoutSeed() == net.backrooms.worldgen.LevelSeeds.of(level));
		// ---- Almond Water item registration and drinking behaviour ----
		check("almond water item registered",
				net.minecraft.core.registries.BuiltInRegistries.ITEM
						.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("backrooms", "almond_water"))
						== net.backrooms.ModItems.ALMOND_WATER);
		var drinkFake = net.fabricmc.fabric.api.entity.FakePlayer.get(server.overworld(),
				new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "br-drink"));
		drinkFake.getAbilities().instabuild = false;
		net.backrooms.sanity.SanityHandler.set(drinkFake, 0.2F);
		net.minecraft.world.item.ItemStack drinkStack =
				new net.minecraft.world.item.ItemStack(net.backrooms.ModItems.ALMOND_WATER, 1);
		net.minecraft.world.item.ItemStack remainder =
				((net.backrooms.item.AlmondWaterItem) net.backrooms.ModItems.ALMOND_WATER)
						.finishUsingItem(drinkStack, server.overworld(), drinkFake);
		check("drinking almond water restores sanity",
				Math.abs(net.backrooms.sanity.SanityHandler.get(drinkFake) - 0.8F) < 0.001F);
		check("drinking almond water grants regeneration",
				drinkFake.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION));
		check("drinking almond water returns the glass bottle",
				remainder.is(net.minecraft.world.item.Items.GLASS_BOTTLE));

		// ---- Sanity attachment default/clamping ----
		var sanityFake = net.fabricmc.fabric.api.entity.FakePlayer.get(server.overworld(),
				new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "br-sanity"));
		check("sanity defaults to full",
				Math.abs(net.backrooms.sanity.SanityHandler.get(sanityFake) - 1.0F) < 0.0001F);
		net.backrooms.sanity.SanityHandler.set(sanityFake, 5.0F);
		check("sanity clamps at maximum", net.backrooms.sanity.SanityHandler.get(sanityFake) == 1.0F);
		net.backrooms.sanity.SanityHandler.set(sanityFake, -1.0F);
		check("sanity clamps at zero", net.backrooms.sanity.SanityHandler.get(sanityFake) == 0.0F);
		net.backrooms.sanity.SanityHandler.restore(sanityFake, 0.4F);
		check("sanity restore adds up",
				Math.abs(net.backrooms.sanity.SanityHandler.get(sanityFake) - 0.4F) < 0.0001F);

		// ---- Supply chest loot table exists ----
		net.minecraft.world.level.storage.loot.LootTable supplyTable =
				server.reloadableRegistries().getLootTable(net.backrooms.ModLoot.SUPPLY_CHEST);
		check("supply chest loot table loaded", supplyTable != null && supplyTable != net.minecraft.world.level.storage.loot.LootTable.EMPTY);

		// ---- Level 0: supply chests and pool pads actually generate ----
		for (int cx = -8; cx < 12; cx++) {
			for (int cz = -8; cz < 12; cz++) {
				level.getChunk(cx, cz, ChunkStatus.FULL, true);
			}
		}
		Level0Layout planLayout = new Level0Layout(net.backrooms.worldgen.LevelSeeds.of(level));
		int planPads = 0;
		for (int cx = -8; cx < 12; cx++) {
			for (int cz = -8; cz < 12; cz++) {
				if (planLayout.poolPadCenter(cx, cz) != Level0Layout.NO_POS) {
					planPads++;
				}
			}
		}
		int planRingColumns = 0;
		for (int x = -8 * 16; x < 12 * 16; x++) {
			for (int z = -8 * 16; z < 12 * 16; z++) {
				if (planLayout.poolPadRoleAt(x, z) != 0) {
					planRingColumns++;
				}
			}
		}
		log("level0 plan pads=" + planPads + " padColumns=" + planRingColumns);
		int chests = 0;
		boolean chestWithAlmondWater = false;
		int portalBlocks = 0;
		int ringBlocks = 0;
		boolean ringOk = false;
		boolean basinWetOk = true;
		boolean deckOk = true;
		var chestVisitor = net.fabricmc.fabric.api.entity.FakePlayer.get(server.overworld(),
				new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "br-chest"));
		for (int x = -8 * 16; x < 12 * 16; x++) {
			for (int z = -8 * 16; z < 12 * 16; z++) {
				BlockPos p = BlockPos.containing(x, Level0Layout.FLOOR_Y + 1, z);
				if (level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.CHEST)) {
					chests++;
					check("chest sits on floor with headroom (" + x + "," + z + ")",
							level.getBlockState(p.below()).is(ModBlocks.CARPET)
									&& level.getBlockState(p.above()).isAir());
					if (level.getBlockEntity(p) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
						chest.unpackLootTable(chestVisitor);
						for (int slot = 0; slot < chest.getContainerSize(); slot++) {
							if (chest.getItem(slot).is(net.backrooms.ModItems.ALMOND_WATER)) {
								chestWithAlmondWater = true;
							}
						}
					}
				}
				if (level.getBlockState(BlockPos.containing(x, Level0Layout.FLOOR_Y - 2, z))
						.is(ModBlocks.POOL_PORTAL)) {
					portalBlocks++;
					boolean ring = true;
					for (int dx = -1; dx <= 1 && ring; dx++) {
						for (int dz = -1; dz <= 1; dz++) {
							if (dx == 0 && dz == 0) {
								continue;
							}
							if (!level.getBlockState(BlockPos.containing(x + dx, Level0Layout.FLOOR_Y - 2, z + dz))
									.is(net.minecraft.world.level.block.Blocks.SEA_LANTERN)) {
								ring = false;
								break;
							}
						}
					}
					if (ring) {
						ringOk = true;
					}
					// Two-deep water above the drain, surface flush with the floor.
					if (!level.getBlockState(BlockPos.containing(x, Level0Layout.FLOOR_Y - 1, z))
							.is(net.minecraft.world.level.block.Blocks.WATER)
							|| !level.getBlockState(BlockPos.containing(x, Level0Layout.FLOOR_Y, z))
							.is(net.minecraft.world.level.block.Blocks.WATER)) {
						basinWetOk = false;
					}
					// Tiled deck ring wherever it doesn't hit a wall.
					for (int dx = -2; dx <= 2; dx++) {
						for (int dz = -2; dz <= 2; dz++) {
							if (Math.max(Math.abs(dx), Math.abs(dz)) != 2) {
								continue;
							}
							boolean wallColumn = planLayout.isWallColumn(x + dx, z + dz);
							boolean tiled = level.getBlockState(
									BlockPos.containing(x + dx, Level0Layout.FLOOR_Y, z + dz))
									.is(ModBlocks.POOL_TILE);
							if (!wallColumn && !tiled) {
								deckOk = false;
							}
						}
					}
				}
				if (level.getBlockState(BlockPos.containing(x, Level0Layout.FLOOR_Y - 2, z))
						.is(net.minecraft.world.level.block.Blocks.SEA_LANTERN)) {
					ringBlocks++;
				}
			}
		}
		log("level0 generated chests=" + chests + " planPads=" + planPads
				+ " portalBlocks=" + portalBlocks + " ringBlocks=" + ringBlocks);
		check("supply chests generate in Level 0", chests > 0);
		check("a supply chest contained almond water", chestWithAlmondWater);
		check("portal basins generate in Level 0", portalBlocks > 0);
		// Pads stay inside their owning chunk (basin at local 2..13), so every
		// accepted plan pad must correspond 1:1 to a drain block in world.
		check("every planned Level 0 pad built a drain (" + planPads + " vs " + portalBlocks + ")",
				portalBlocks == planPads);
		check("basin floors have a sea-lantern ring (" + ringBlocks + " ring blocks)", ringOk && ringBlocks >= 8);
		check("basin water is two deep with a flush surface", basinWetOk && portalBlocks > 0);
		check("basins have a tiled deck wherever it misses walls", deckOk && portalBlocks > 0);

		// ---- Poolrooms dimension: existence, plan and real generation ----
		ServerLevel pool = server.getLevel(net.backrooms.ModWorldgen.POOLROOMS_LEVEL);
		check("poolrooms dimension exists", pool != null);
		if (pool == null) {
			return;
		}
		check("poolrooms generator captured the worldgen seed",
				pool.getChunkSource().getGenerator() instanceof net.backrooms.worldgen.PoolroomsChunkGenerator pg
						&& pg.getLayoutSeed() == net.backrooms.worldgen.LevelSeeds.of(pool));
		net.backrooms.worldgen.PoolroomsLayout pl =
				new net.backrooms.worldgen.PoolroomsLayout(net.backrooms.worldgen.LevelSeeds.of(pool));

		// pure plan: connectivity flood fill (water basins are only 1 deep - passable)
		int pr = 150;
		Set<Long> pWalk = new HashSet<>();
		Set<Long> pReach = new HashSet<>();
		for (int x = -pr; x <= pr; x++) {
			for (int z = -pr; z <= pr; z++) {
				if (!pl.isWallColumn(x, z)) {
					pWalk.add(pack(x, z));
				}
			}
		}
		// (0,0) is a wall lattice corner; find a guaranteed dry point nearby.
		int[] pStart = null;
		outer:
		for (int x = 1; x <= 8; x++) {
			for (int z = 1; z <= 8; z++) {
				if (pl.isDryFloor(x, z)) {
					pStart = new int[]{x, z};
					break outer;
				}
			}
		}
		check("found a dry poolrooms flood-fill start", pStart != null);
		if (pStart == null) {
			pStart = new int[]{2, 2};
		}
		ArrayDeque<int[]> pq = new ArrayDeque<>();
		pq.add(pStart);
		pReach.add(pack(pStart[0], pStart[1]));
		while (!pq.isEmpty()) {
			int[] cur = pq.poll();
			for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
				int nx = cur[0] + d[0];
				int nz = cur[1] + d[1];
				long key = pack(nx, nz);
				if (pWalk.contains(key) && pReach.add(key)) {
					pq.add(new int[]{nx, nz});
				}
			}
		}
		int pEnclosed = 0;
		for (Long key : pWalk) {
			int x = (int) (key >> 32);
			int z = key.intValue();
			if (Math.abs(x) <= pr - 50 && Math.abs(z) <= pr - 50 && !pReach.contains(key)) {
				pEnclosed++;
			}
		}
		log("poolrooms walkable=" + pWalk.size() + " reached=" + pReach.size()
				+ " enclosed(interior)=" + pEnclosed);
		check("poolrooms plan is fully connected", pEnclosed == 0);

		// plan variety
		int planWater = 0;
		int poolPlanPads = 0;
		for (int x = -120; x <= 120; x++) {
			for (int z = -120; z <= 120; z++) {
				if (pl.isWaterColumn(x, z)) {
					planWater++;
				}
				if (pl.exitPadRoleAt(x, z) == 2) {
					poolPlanPads++;
				}
			}
		}
		log("poolrooms plan waterColumns=" + planWater + " exitPads=" + poolPlanPads);
		check("poolrooms plan contains water pools", planWater > 0);
		check("poolrooms plan contains exit pads", poolPlanPads > 0);

		// real chunk structure at scattered points
		for (int[] pp : new int[][]{{0, 0}, {48, -48}, {-400, 300}, {2000, -1800}}) {
			int x = pp[0];
			int z = pp[1];
			pool.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);
			BlockState floor = pool.getBlockState(
					BlockPos.containing(x, net.backrooms.worldgen.PoolroomsLayout.FLOOR_Y, z));
			BlockState under = pool.getBlockState(
					BlockPos.containing(x, net.backrooms.worldgen.PoolroomsLayout.BASIN_FLOOR_Y, z));
			boolean floorOk = floor.is(ModBlocks.POOL_TILE)
					|| floor.is(net.minecraft.world.level.block.Blocks.WATER)
					|| floor.is(net.minecraft.world.level.block.Blocks.SEA_LANTERN);
			boolean underOk = under.is(ModBlocks.POOL_TILE) || under.is(ModBlocks.FOUNDATION);
			check("poolrooms structure at (" + x + "," + z + ")", floorOk && underOk);
		}

		// real water and real portal blocks generated near spawn
		for (int cx = -6; cx <= 6; cx++) {
			for (int cz = -6; cz <= 6; cz++) {
				pool.getChunk(cx, cz, ChunkStatus.FULL, true);
			}
		}
		int realWater = 0;
		int realPortal = 0;
		int planPortal = 0;
		for (int x = -100; x <= 100; x++) {
			for (int z = -100; z <= 100; z++) {
				if (pool.getBlockState(BlockPos.containing(x, net.backrooms.worldgen.PoolroomsLayout.FLOOR_Y, z))
						.is(net.minecraft.world.level.block.Blocks.WATER)) {
					realWater++;
				}
				if (pl.exitPadRoleAt(x, z) == 2) {
					planPortal++;
				}
				if (pool.getBlockState(BlockPos.containing(x, net.backrooms.worldgen.PoolroomsLayout.FLOOR_Y - 2, z))
						.is(ModBlocks.POOL_PORTAL)) {
					realPortal++;
				}
			}
		}
		log("poolrooms generated water=" + realWater + " drains=" + realPortal + " plan=" + planPortal);
		check("poolrooms generate real pool water", realWater > 0);
		check("every planned Poolrooms pad built a drain (" + planPortal + " vs " + realPortal + ")",
				realPortal == planPortal);

		// arrival search lands on dry tiled floor with two air blocks above
		BlockPos poolSpawn = net.backrooms.mechanics.PoolPortalHandler.findPoolroomsSpawn(pool);
		check("poolrooms arrival point is safe", poolSpawn != null
				&& pool.getBlockState(poolSpawn).is(ModBlocks.POOL_TILE)
				&& pool.getBlockState(poolSpawn.above()).isAir()
				&& pool.getBlockState(poolSpawn.above(2)).isAir());

		// ---- 5 minute day/night cycle and hot-water phase ----
		check("cycle length is 6000 ticks (5 minutes)",
				net.backrooms.mechanics.PoolroomsEnvironmentHandler.FULL_CYCLE_TICKS == 6000L);
		check("water hot at dawn", net.backrooms.mechanics.PoolroomsEnvironmentHandler.isHotDaytime(0L));
		check("water hot before noon", net.backrooms.mechanics.PoolroomsEnvironmentHandler.isHotDaytime(11999L));
		check("water cool at dusk", !net.backrooms.mechanics.PoolroomsEnvironmentHandler.isHotDaytime(12000L));
		check("water cool at midnight", !net.backrooms.mechanics.PoolroomsEnvironmentHandler.isHotDaytime(23999L));
		check("water hot again next dawn", net.backrooms.mechanics.PoolroomsEnvironmentHandler.isHotDaytime(24000L));
		net.backrooms.mechanics.PoolroomsEnvironmentHandler.resetPhaseForTest(0L);
		net.backrooms.mechanics.PoolroomsEnvironmentHandler.tick(server);
		check("poolrooms independent clock runs 4x and is read by the level",
				net.backrooms.mechanics.PoolroomsEnvironmentHandler.currentPhase() == 4L
						&& pool.getDayTime() == 4L);
		net.backrooms.mechanics.PoolroomsEnvironmentHandler.resetPhaseForTest(12000L);
		check("pool level reports night through the clock mixin", pool.getDayTime() == 12000L);

		// the portal/environment/sanity tick handlers run with players present
		net.backrooms.mechanics.PoolPortalHandler.clearCooldowns();
		net.backrooms.mechanics.PoolPortalHandler.tick(server);
		net.backrooms.sanity.SanityHandler.tick(server);
		check("v1.0.3 tick handlers run cleanly", true);

		runV105Checks(server, pl, pool);
	}

	// ------------------------------------------------------------ v1.0.5
	private static void runV105Checks(MinecraftServer server,
			net.backrooms.worldgen.PoolroomsLayout pl, ServerLevel pool) {
		check("sanity regenerates inside the Poolrooms",
				net.backrooms.sanity.SanityHandler.poolroomsRegenPerTick() > 0.0F);

		int S = net.backrooms.worldgen.PoolroomsLayout.SPACING;

		// ---- every ordinary maze room is fully roofed ----
		int roofChecked = 0;
		outerRoof:
		for (int cx = -6; cx <= 6 && roofChecked < 24; cx++) {
			for (int cz = -6; cz <= 6; cz++) {
				if (pl.isGreatHall(cx, cz) || pl.isPoolCell(cx, cz)) {
					continue;
				}
				// local (4,4) is an interior sub-room column away from partitions
				int wx = cx * S + 4;
				int wz = cz * S + 4;
				if (pl.blocksColumn(wx, wz) || pl.isWaterColumn(wx, wz)) {
					continue;
				}
				pool.getChunk(wx >> 4, wz >> 4, ChunkStatus.FULL, true);
				BlockState ceil = pool.getBlockState(BlockPos.containing(
						wx, net.backrooms.worldgen.PoolroomsLayout.CEILING_Y, wz));
				BlockState roof = pool.getBlockState(BlockPos.containing(
						wx, net.backrooms.worldgen.PoolroomsLayout.ROOF_Y, wz));
				BlockState gap = pool.getBlockState(BlockPos.containing(
						wx, net.backrooms.worldgen.PoolroomsLayout.CEILING_Y - 1, wz));
				check("maze room roofed at cell (" + cx + "," + cz + ")",
						(ceil.is(ModBlocks.POOL_TILE) || ceil.is(net.minecraft.world.level.block.Blocks.SEA_LANTERN))
								&& roof.is(ModBlocks.FOUNDATION) && gap.isAir());
				roofChecked++;
				if (roofChecked >= 24) {
					break outerRoof;
				}
			}
		}
		log("v1.0.5 roofed maze cells checked=" + roofChecked);
		check("roofed maze cells were found to check", roofChecked >= 10);

		// ---- the plan contains rare great halls ----
		int halls = 0;
		int[] hall = null;
		for (int cx = -15; cx <= 15; cx++) {
			for (int cz = -15; cz <= 15; cz++) {
				if (pl.isGreatHall(cx, cz)) {
					halls++;
					if (hall == null) {
						hall = new int[]{cx, cz};
					}
				}
			}
		}
		log("v1.0.5 great halls in 31x31 cells=" + halls + " first="
				+ (hall == null ? "none" : hall[0] + "," + hall[1]));
		// 3% of 961 cells expects ~29 (std ~5); generous bounds keep CI deterministic.
		check("great halls generate but stay rare", halls >= 5 && halls <= 75);

		if (hall != null) {
			int hx = hall[0] * S;
			int hz = hall[1] * S;
			for (int x = hx; x < hx + S; x++) {
				for (int z = hz; z < hz + S; z++) {
					pool.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);
				}
			}
			// interior point kept off the pillar (5/18) and lantern (4/12/20) grids
			int ix = hx + 10;
			int iz = hz + 10;
			check("great hall has tiled floor",
					pool.getBlockState(BlockPos.containing(ix,
							net.backrooms.worldgen.PoolroomsLayout.FLOOR_Y, iz)).is(ModBlocks.POOL_TILE));
			check("great hall interior is open at roof height",
					pool.getBlockState(BlockPos.containing(ix,
							net.backrooms.worldgen.PoolroomsLayout.HALL_GLASS_ROOF_Y - 1, iz)).isAir());
			check("great hall has a glass roof",
					pool.getBlockState(BlockPos.containing(ix,
							net.backrooms.worldgen.PoolroomsLayout.HALL_GLASS_ROOF_Y, iz))
							.is(net.minecraft.world.level.block.Blocks.GLASS));
			// west perimeter wall: tall, with a glass window band, capped solid
			boolean foundWindow = false;
			boolean wallTall = false;
			for (int lz = 2; lz < S - 2; lz++) {
				int wx = hx;
				int wz = hz + lz;
				if (pl.isPerimeterOpening(wx, wz)) {
					continue;
				}
				BlockState band = pool.getBlockState(BlockPos.containing(wx,
						net.backrooms.worldgen.PoolroomsLayout.HALL_WINDOW_MIN_Y, wz));
				BlockState cap = pool.getBlockState(BlockPos.containing(wx,
						net.backrooms.worldgen.PoolroomsLayout.HALL_WALL_TOP_Y, wz));
				if (band.is(net.minecraft.world.level.block.Blocks.GLASS) && cap.is(ModBlocks.POOL_TILE)) {
					foundWindow = true;
				}
				if (cap.is(ModBlocks.POOL_TILE)) {
					wallTall = true;
				}
			}
			check("great hall walls carry a window band of glass", foundWindow);
			check("great hall walls rise above maze ceiling height", wallTall);
			// Wall lines are owned by the cell on their + side, so the hall's
			// east wall is the lx=0 line of the neighbouring cell - it must be
			// tall there too (symmetry across the cell border).
			int nz = hz + 12;
			check("tall west wall resolves on the hall's own line",
					pl.isTallWallColumn(hx, nz));
			check("tall wall also resolves from the neighbour-owned east line",
					pl.isTallWallColumn(hx + S, nz));
			// Exhaustive border rule near spawn: an x-wall is tall iff either
			// adjoining cell is a hall.
			int tallRuleMismatches = 0;
			for (int wx = -8 * S; wx <= 8 * S; wx += S) {
				for (int wz = -8 * S + 3; wz < 8 * S - 3; wz += 5) {
					if (Math.floorMod(wz, S) == 0) {
						continue; // lattice corners use a four-cell rule
					}
					int ccx = Math.floorDiv(wx, S);
					int ccz = Math.floorDiv(wz, S);
					boolean expectTall = pl.isGreatHall(ccx, ccz) || pl.isGreatHall(ccx - 1, ccz);
					if (pl.isTallWallColumn(wx, wz) != expectTall) {
						tallRuleMismatches++;
					}
				}
			}
			check("tall-wall rule agrees on every sampled border", tallRuleMismatches == 0);
		}
	}


	private static BlockPos findOpen(ServerLevel level, int cx, int cz, int radius) {
		for (int dx = 0; dx < radius; dx++) {
			for (int dz = 0; dz < radius; dz++) {
				BlockPos feet = BlockPos.containing(cx + dx, Level0Layout.FLOOR_Y + 1, cz + dz);
				if (level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir()
						&& level.getBlockState(feet.below()).is(ModBlocks.CARPET)) {
					return feet;
				}
			}
		}
		return null;
	}

	/** Finds an actually placed fluorescent fixture block (optionally lit) near spawn. */
	private static BlockPos findFixture(ServerLevel level, boolean lit) {
		for (int x = -48; x < 48; x++) {
			for (int z = -48; z < 48; z++) {
				BlockPos c = BlockPos.containing(x, Level0Layout.CEILING_Y, z);
				BlockState s = level.getBlockState(c);
				if (s.is(ModBlocks.FLUORESCENT)
						&& s.getValue(net.backrooms.worldgen.block.FluorescentBlock.LIT) == lit) {
					return c;
				}
			}
		}
		return null;
	}

	private static long pack(int x, int z) {
		return ((long) x << 32) ^ (z & 0xffffffffL);
	}

	private static void check(String name, boolean ok) {
		log((ok ? "PASS " : "FAIL ") + name);
		if (!ok) {
			failures++;
		}
	}

	private static void log(String s) {
		LINES.add(s);
		System.out.println("[Backrooms-SelfTest] " + s);
	}

	private static void fail(String s) {
		LINES.add("FAIL " + s);
		failures++;
	}

	private static void report(MinecraftServer server) {
		StringBuilder sb = new StringBuilder(failures == 0 ? "SELFTEST OK" : "SELFTEST FAILED (" + failures + ")")
				.append('\n');
		for (String s : LINES) {
			sb.append(s).append('\n');
		}
		String report = sb.toString();
		try {
			java.nio.file.Files.writeString(java.nio.file.Path.of("/tmp/backrooms-selftest.txt"), report);
		} catch (Exception ignored) {
		}
		try {
			// Also write next to the server working dir so CI can pick it up reliably.
			java.nio.file.Files.writeString(java.nio.file.Path.of("selftest-report.txt"), report);
		} catch (Exception ignored) {
		}
		boolean ok = failures == 0;
		new Thread(() -> {
			try {
				Thread.sleep(1500);
			} catch (InterruptedException ignored) {
			}
			System.exit(ok ? 0 : 1);
		}, "selftest-exit").start();
		server.halt(false);
	}
}
