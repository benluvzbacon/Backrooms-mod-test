package net.backrooms.worldgen;

/**
 * Deterministic layout of the Poolrooms.
 *
 * <p>The level is an infinite indoor pool complex on a 24-block lattice. Normal
 * cells are roofed <b>maze rooms</b>: tiled interior partitions laid out with a
 * depth-first spanning tree (every sub-room reachable), shallow still-water
 * pools, pillars and ceiling lamps. Perimeter walls have wide archways into
 * neighbouring cells.</p>
 *
 * <p>Roughly three cells in a hundred are instead <b>great halls</b>: no partitions,
 * walls twice the height ringed with a continuous band of windows, a glass
 * roof open to the real sky (so the fast day/night cycle is visible inside),
 * tall lamp pillars and dry tiled floor. They are the only rooms where the
 * outside can be seen.</p>
 *
 * <p>Every column is a pure function of (seed, x, z) so chunk borders agree
 * without stored state.</p>
 */
public final class PoolroomsLayout {
	public static final int SPACING = 24;

	public static final int FLOOR_Y = 64;
	public static final int BASIN_FLOOR_Y = 63;
	/** Roofed maze rooms. */
	public static final int CEILING_Y = 70;
	public static final int ROOF_Y = 71;
	/** Great halls. */
	public static final int HALL_WALL_TOP_Y = 85;
	public static final int HALL_GLASS_ROOF_Y = 86;
	public static final int HALL_ARCH_TOP_Y = 76;
	public static final int HALL_LINTEL_Y = 77;
	public static final int HALL_WINDOW_MIN_Y = 78;
	public static final int HALL_WINDOW_MAX_Y = 81;
	public static final int HALL_PILLAR_LAMP_Y = 85;

	public static final int EXIT_PAD_CHUNK_MOD = 16;
	public static final int PAD_RADIUS = 1;
	public static final long NO_POS = Long.MIN_VALUE;

	private static final float POOL_CELL_CHANCE = 0.45F;
	/** Chance a cell is a great hall: 3%, inside the intended 1-5% band. */
	private static final float GREAT_HALL_CHANCE = 0.03F;
	private static final float MAZE_LOOP_CHANCE = 0.12F;
	private static final int ARCH_WIDTH = 5;
	private static final int GRID = 3;

	private final long seed;
	private final java.util.HashMap<Long, boolean[]> edgeCache = new java.util.HashMap<>();

	public PoolroomsLayout(long seed) {
		this.seed = seed;
	}

	// ---------------------------------------------------------------- hashes
	private static long mix(long z) {
		z += 0x9E3779B97F4A7C15L;
		z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
		z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
		return z ^ (z >>> 31);
	}

	private long hash(long a, long b, long c) {
		return mix(this.seed ^ mix(a * 31 + b * 37 + c * 131 + 0x85A308D313198A2EL));
	}

	private float rand01(long a, long b, long c) {
		return (hash(a, b, c) >>> 11) * 0x1.0p-53f;
	}

	private float rand01(long a, long b, long c, long d) {
		return (mix(hash(a, b, c) ^ mix(d * 0x9E3779B1L + 17L)) >>> 11) * 0x1.0p-53f;
	}

	private static int fmod(int a, int n) {
		return Math.floorMod(a, n);
	}

	private static int fdiv(int a, int n) {
		return Math.floorDiv(a, n);
	}

	// -------------------------------------------------------------- cell data
	public boolean isGreatHall(int cellX, int cellZ) {
		return rand01(88, cellX, cellZ) < GREAT_HALL_CHANCE;
	}

	public boolean isPoolCell(int cellX, int cellZ) {
		return !isGreatHall(cellX, cellZ) && rand01(85, cellX, cellZ) < POOL_CELL_CHANCE;
	}

	/** Two 5-wide archways per perimeter segment, one per half. */
	private boolean inArch(long h, int local) {
		int c1 = 5 + (int) Math.floorMod(h, 5);
		int c2 = 17 + (int) Math.floorMod(h >>> 16, 4);
		return Math.abs(local - c1) <= ARCH_WIDTH / 2
				|| Math.abs(local - c2) <= ARCH_WIDTH / 2;
	}

	// --------------------------------------------------------------- walls
	/** Solid column at all walkable heights (perimeter or maze partition). */
	public boolean isWallColumn(int x, int z) {
		int lx = fmod(x, SPACING);
		int lz = fmod(z, SPACING);
		int cellX = fdiv(x, SPACING);
		int cellZ = fdiv(z, SPACING);
		boolean onX = lx == 0;
		boolean onZ = lz == 0;
		if (onX || onZ) {
			// Wall corners are always solid pillars.
			return onX && onZ || perimeterBlocks(x, z, lx, lz, cellX, cellZ, onX, onZ);
		}
		if (isGreatHall(cellX, cellZ)) {
			return false; // great halls are completely open inside
		}
		return interiorPartitionBlocks(cellX, cellZ, lx, lz);
	}

	private boolean perimeterBlocks(int x, int z, int lx, int lz, int cellX, int cellZ,
			boolean onX, boolean onZ) {
		if (onX && onZ) {
			return true;
		}
		if (onX) {
			// The x=lattice-line wall is owned by the cell on its +X side, so
			// both sides resolve the same arch hash.
			if (lz <= 1 || lz >= SPACING - 2) {
				return true;
			}
			return !inArch(hash(90, cellX, cellZ), lz) && !inArch(hash(91, cellX, cellZ), lz);
		}
		if (lx <= 1 || lx >= SPACING - 2) {
			return true;
		}
		return !inArch(hash(92, cellX, cellZ), lx) && !inArch(hash(93, cellX, cellZ), lx);
	}

	/** Whether a perimeter-wall column sits inside an archway opening. */
	public boolean isPerimeterOpening(int x, int z) {
		int lx = fmod(x, SPACING);
		int lz = fmod(z, SPACING);
		int cellX = fdiv(x, SPACING);
		int cellZ = fdiv(z, SPACING);
		if (lx == 0 && lz != 0 && lz > 1 && lz < SPACING - 2) {
			return inArch(hash(90, cellX, cellZ), lz) || inArch(hash(91, cellX, cellZ), lz);
		}
		if (lz == 0 && lx != 0 && lx > 1 && lx < SPACING - 2) {
			return inArch(hash(92, cellX, cellZ), lx) || inArch(hash(93, cellX, cellZ), lx);
		}
		return false;
	}

	/**
	 * A perimeter wall column is TALL (great-hall height with windows) when
	 * either adjoining cell is a great hall.
	 */
	public boolean isTallWallColumn(int x, int z) {
		int lx = fmod(x, SPACING);
		int lz = fmod(z, SPACING);
		int cellX = fdiv(x, SPACING);
		int cellZ = fdiv(z, SPACING);
		if (lx != 0 && lz != 0) {
			return false;
		}
		if (lx == 0 && lz == 0) {
			// Lattice corner shared by four cells: a single short column would
			// leave a 1x1 sky hole beside a diagonal great hall, so check all four.
			return isGreatHall(cellX, cellZ) || isGreatHall(cellX - 1, cellZ)
					|| isGreatHall(cellX, cellZ - 1) || isGreatHall(cellX - 1, cellZ - 1);
		}
		if (lx == 0) {
			// x-wall separates this cell from its -X neighbour.
			return isGreatHall(cellX, cellZ) || isGreatHall(cellX - 1, cellZ);
		}
		// z-wall separates this cell from its -Z neighbour.
		return isGreatHall(cellX, cellZ) || isGreatHall(cellX, cellZ - 1);
	}

	// --------------------------------------------------------- maze partitions
	private long edgeKey(int cx, int cz) {
		return ((long) cx << 32) ^ (cz & 0xffffffffL);
	}

	/** Deterministic per-cell DFS spanning tree, index node*4 + dir (N,S,W,E). */
	private boolean[] mazeEdges(int cx, int cz) {
		boolean[] cached = edgeCache.get(edgeKey(cx, cz));
		if (cached != null) {
			return cached;
		}
		int n = GRID * GRID;
		boolean[] edges = new boolean[n * 4];
		boolean[] visited = new boolean[n];
		int[] stack = new int[n];
		int sp = 0;
		stack[sp++] = 0;
		visited[0] = true;
		long rng = hash(71, cx, cz) | 1L;
		while (sp > 0) {
			int node = stack[sp - 1];
			int c = node % GRID;
			int r = node / GRID;
			int[] candidates = {node - GRID, node + GRID, node - 1, node + 1};
			int[] dirIds = {0, 1, 2, 3};
			for (int q = 0; q < 4; q++) {
				rng = rng * 0x5851F42D4C957F2DL + 0x14057B7EF767814FL;
				int pick = q + (int) ((rng >>> 33) % (4 - q));
				int t1 = candidates[q];
				candidates[q] = candidates[pick];
				candidates[pick] = t1;
				int t2 = dirIds[q];
				dirIds[q] = dirIds[pick];
				dirIds[pick] = t2;
			}
			boolean deadEnd = true;
			for (int q = 0; q < 4; q++) {
				int nn = candidates[q];
				if (nn < 0 || nn >= n || visited[nn]) {
					continue;
				}
				// Reject horizontal wrap between rows.
				if ((dirIds[q] == 2 && c == 0) || (dirIds[q] == 3 && c == GRID - 1)) {
					continue;
				}
				deadEnd = false;
				int dir = dirIds[q];
				visited[nn] = true;
				edges[node * 4 + dir] = true;
				edges[nn * 4 + (dir ^ 1)] = true;
				stack[sp++] = nn;
				break;
			}
			if (deadEnd) {
				sp--;
			}
		}
		// A few extra openings so it isn't a pure tree maze.
		for (int r = 0; r < GRID; r++) {
			for (int c = 0; c < GRID; c++) {
				int node = r * GRID + c;
				if (c + 1 < GRID && !edges[node * 4 + 3] && rand01(72, cx, cz, node) < MAZE_LOOP_CHANCE) {
					edges[node * 4 + 3] = true;
					edges[(node + 1) * 4 + 2] = true;
				}
				if (r + 1 < GRID && !edges[node * 4 + 1] && rand01(73, cx, cz, node) < MAZE_LOOP_CHANCE) {
					edges[node * 4 + 1] = true;
					edges[(node + GRID) * 4 + 0] = true;
				}
			}
		}
		edgeCache.put(edgeKey(cx, cz), edges);
		return edges;
	}

	private boolean inGapV(int cx, int cz, int r, int c, int lz) {
		int lo = r * 8 + 1;
		int hi = (r + 1) * 8;
		int width = rand01(74, cx, cz, r * 8 + c) < 0.25F ? 3 : 2;
		int mid = lo + 1 + (int) (rand01(75, cx, cz, r * 8 + c) * Math.max(1, hi - lo - width - 1));
		return lz >= mid && lz < mid + width;
	}

	private boolean inGapH(int cx, int cz, int r, int c, int lx) {
		int lo = c * 8 + 1;
		int hi = (c + 1) * 8;
		int width = rand01(76, cx, cz, r * 8 + c) < 0.25F ? 3 : 2;
		int mid = lo + 1 + (int) (rand01(77, cx, cz, r * 8 + c) * Math.max(1, hi - lo - width - 1));
		return lx >= mid && lx < mid + width;
	}

	private boolean interiorPartitionBlocks(int cx, int cz, int lx, int lz) {
		boolean onV = lx == 8 || lx == 16;
		boolean onH = lz == 8 || lz == 16;
		if (!onV && !onH) {
			return false;
		}
		if (lz <= 1 || lz >= SPACING - 2 || lx <= 1 || lx >= SPACING - 2) {
			return true; // solid returns against perimeter walls
		}
		boolean[] edges = mazeEdges(cx, cz);
		if (onV && onH) {
			return true; // junction pillar
		}
		if (onV) {
			int c = lx == 8 ? 0 : 1; // sub-room column west of the partition
			int r;
			if (lz < 8) {
				r = 0;
			} else if (lz < 16) {
				r = 1;
			} else {
				r = 2;
			}
			// Junction pillars where the perpendicular partition crosses.
			if (lz == 8 || lz == 16) {
				return true;
			}
			if (edges[(r * GRID + c) * 4 + 3]) {
				return !inGapV(cx, cz, r, c, lz);
			}
			return true;
		}
		int r = lz == 8 ? 0 : 1; // sub-room row north of the partition
		int c;
		if (lx < 8) {
			c = 0;
		} else if (lx < 16) {
			c = 1;
		} else {
			c = 2;
		}
		if (lx == 8 || lx == 16) {
			return true;
		}
		if (edges[(r * GRID + c) * 4 + 1]) {
			return !inGapH(cx, cz, r, c, lx);
		}
		return true;
	}

	// --------------------------------------------------------------- features
	/** Pool basins occupy the inside of pool cells, leaving a tiled deck. */
	public boolean isWaterColumn(int x, int z) {
		if (isWallColumn(x, z) || isPillarColumn(x, z) || exitPadRoleAt(x, z) != 0) {
			return false;
		}
		int cellX = fdiv(x, SPACING);
		int cellZ = fdiv(z, SPACING);
		if (!isPoolCell(cellX, cellZ)) {
			return false;
		}
		int lx = fmod(x, SPACING);
		int lz = fmod(z, SPACING);
		return lx >= 3 && lx <= SPACING - 4 && lz >= 3 && lz <= SPACING - 4;
	}

	/** Lamp pillars: short in maze rooms, tall in great halls. */
	public boolean isPillarColumn(int x, int z) {
		// The pad lookup MUST stay out of the geometry tested while planning a
		// pad (exitPadCenter) or the two methods recurse into a stack overflow.
		if (exitPadRoleAt(x, z) != 0) {
			return false;
		}
		return isPillarPlan(x, z);
	}

	/** Pillar geometry without consulting pad roles (also used to plan pads). */
	private boolean isPillarPlan(int x, int z) {
		if (isWallColumn(x, z)) {
			return false;
		}
		int cellX = fdiv(x, SPACING);
		int cellZ = fdiv(z, SPACING);
		if (isPoolCell(cellX, cellZ)) {
			return false;
		}
		int lx = fmod(x, SPACING);
		int lz = fmod(z, SPACING);
		int sx = lx == 5 ? 0 : lx == 18 ? 1 : -1;
		int sz = lz == 5 ? 0 : lz == 18 ? 1 : -1;
		if (sx < 0 || sz < 0) {
			return false;
		}
		// Great halls always get their four pillars; maze rooms only sometimes.
		if (isGreatHall(cellX, cellZ)) {
			return true;
		}
		int slot = sx * 2 + sz;
		return fmod((int) (hash(86, cellX, cellZ) >>> (slot * 8)), 3) != 0;
	}

	public boolean isInGreatHall(int x, int z) {
		int lx = fmod(x, SPACING);
		int lz = fmod(z, SPACING);
		if (lx == 0 || lz == 0) {
			return false;
		}
		return isGreatHall(fdiv(x, SPACING), fdiv(z, SPACING));
	}

	/** Sea-lantern grid embedded in the maze-room ceilings. */
	public boolean ceilingLampAt(int x, int z) {
		if (isWallColumn(x, z) || isInGreatHall(x, z)) {
			return false;
		}
		return fmod(x, 6) == 2 && fmod(z, 6) == 2;
	}

	/** Lanterns hanging under the glass of a great hall. */
	public boolean hallLanternAt(int x, int z) {
		if (!isInGreatHall(x, z) || isPillarColumn(x, z)) {
			return false;
		}
		int lx = fmod(x, SPACING);
		int lz = fmod(z, SPACING);
		return (lx == 4 || lx == 12 || lx == 20) && (lz == 4 || lz == 12 || lz == 20);
	}

	// ------------------------------------------------------------- exit pads
	private long exitPadCenter(int chunkX, int chunkZ) {
		if (Math.floorMod(hash(94, chunkX, chunkZ), EXIT_PAD_CHUNK_MOD) != 0) {
			return NO_POS;
		}
		long stream = hash(96, chunkX, chunkZ) | 1L;
		for (int attempt = 0; attempt < 8; attempt++) {
			stream = stream * 0x5851F42D4C957F2DL + 0x14057B7EF767814FL;
			int lx = 3 + (int) ((stream >>> 33) % 10);
			stream = stream * 0x5851F42D4C957F2DL + 0x14057B7EF767814FL;
			int lz = 3 + (int) ((stream >>> 33) % 10);
			int wx = chunkX * 16 + lx;
			int wz = chunkZ * 16 + lz;
			boolean clear = true;
			for (int dx = -PAD_RADIUS; dx <= PAD_RADIUS && clear; dx++) {
				for (int dz = -PAD_RADIUS; dz <= PAD_RADIUS; dz++) {
					// No pad lookup here: that would re-enter this method.
					if (isWallColumn(wx + dx, wz + dz) || isPillarPlan(wx + dx, wz + dz)) {
						clear = false;
						break;
					}
				}
			}
			if (clear) {
				return net.minecraft.core.BlockPos.asLong(wx, FLOOR_Y, wz);
			}
		}
		return NO_POS;
	}

	/** 0 = not on a pad, 1 = glowing basin floor ring, 2 = centre (the drain: portal back to Level 0). */
	public int exitPadRoleAt(int x, int z) {
		int cx = fdiv(x, 16);
		int cz = fdiv(z, 16);
		for (int dcx = -1; dcx <= 1; dcx++) {
			for (int dcz = -1; dcz <= 1; dcz++) {
				long center = exitPadCenter(cx + dcx, cz + dcz);
				if (center == NO_POS) {
					continue;
				}
				net.minecraft.core.BlockPos cp = net.minecraft.core.BlockPos.of(center);
				int dx = x - cp.getX();
				int dz = z - cp.getZ();
				if (Math.abs(dx) <= PAD_RADIUS && Math.abs(dz) <= PAD_RADIUS) {
					return dx == 0 && dz == 0 ? 2 : 1;
				}
			}
		}
		return 0;
	}

	/** Any solid obstruction at walk level (walls, partitions, pillars). */
	public boolean blocksColumn(int x, int z) {
		return isWallColumn(x, z) || isPillarColumn(x, z);
	}

	/** Dry, open floor suitable for arrival/spawn. */
	public boolean isDryFloor(int x, int z) {
		return !blocksColumn(x, z) && !isWaterColumn(x, z) && exitPadRoleAt(x, z) == 0;
	}
}
