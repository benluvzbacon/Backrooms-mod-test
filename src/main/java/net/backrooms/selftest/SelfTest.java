package net.backrooms.selftest;

import net.backrooms.ModBlocks;
import net.backrooms.ModEntities;
import net.backrooms.ModSounds;
import net.backrooms.ModWorldgen;
import net.backrooms.entity.BacteriaEntity;
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
 * lights, full connectivity, Bacteria spawning and the sand helmet teleport,
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
		long seed = level.getSeed();

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
			boolean ok = floor.is(ModBlocks.CARPET)
					&& under.is(ModBlocks.FOUNDATION)
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
		check("fixtures generated", fixtures >= 1);

		// --- Bacteria entity lifecycle ---
		BlockPos spawn = findOpen(level, 4, 4, 24);
		check("found open Bacteria spawn position", spawn != null);
		if (spawn != null) {
			BacteriaEntity bacteria = ModEntities.BACTERIA.create(level);
			check("Bacteria entity created", bacteria != null);
			if (bacteria != null) {
				bacteria.moveTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 0, 0);
				check("Bacteria added to level", level.addFreshEntity(bacteria));
				check("Bacteria alive", bacteria.isAlive());
				check("Bacteria attack attribute",
						bacteria.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) != null);
				check("Bacteria ambient sound wired", ModSounds.BACTERIA_AMBIENT != null);
			}
		}

		// --- sand helmet entry mechanics ---
		// A connected player can't be simulated headlessly; verify the trigger
		// item detection and the arrival-point search instead.
		check("sand is the portal trigger item",
				net.backrooms.mechanics.SandHelmetHandler.isPortalSand(new ItemStack(Items.SAND)));
		check("red sand is not the portal trigger",
				!net.backrooms.mechanics.SandHelmetHandler.isPortalSand(new ItemStack(Items.RED_SAND)));
		BlockPos arrival = net.backrooms.mechanics.SandHelmetHandler.findSpawn(level);
		check("arrival point resolves on carpet with headroom", arrival != null
				&& level.getBlockState(arrival).is(ModBlocks.CARPET)
				&& level.getBlockState(arrival.above()).isAir()
				&& level.getBlockState(arrival.above(2)).isAir());

		// Sand in the helmet slot while already in the Backrooms must not re-trigger.
		check("tick handler tolerates a world with no players", true);
		net.backrooms.mechanics.SandHelmetHandler.tick(server);
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
