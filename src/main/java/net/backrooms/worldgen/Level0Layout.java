package net.backrooms.worldgen;

/**
 * Deterministic procedural description of the Level-0 floor plan.
 *
 * <p>The world is built on a jittered lattice of wall lines 24 blocks apart:
 * every third line is a perfectly straight, 3-block-wide <b>corridor</b> with
 * open intersections (the guaranteed navigation backbone). The strips between
 * corridors are split into <em>cells</em> (rooms). Each cell is either:</p>
 * <ul>
 *   <li>a completely <b>open room</b> (~17%) - these preserve the spacious feel;
 *       adjacent open rooms sometimes merge through missing perimeter walls;</li>
 *   <li>a <b>maze room</b>: a 2-4 by 2-4 grid of sub-rooms partitioned by walls
 *       whose openings form a spanning tree (randomized depth-first search),
 *       so every sub-room is reachable but the interior genuinely winds.</li>
 * </ul>
 *
 * <p>Every block column is a pure function of (seed, x, z), so chunk generation
 * asks the same function for its 16x16 columns and walls/openings always line
 * up across chunk borders with no stored adjacency state. No region is ever
 * sealed off: the eight cells around each corridor intersection open directly
 * onto the corridor bands, and the single fully-enclosed middle cell gets a
 * deterministic forced escape door.</p>
 */
public final class Level0Layout {
	/** Nominal distance between lattice lines. */
	public static final int SPACING = 24;
	/** Every Nth lattice line is a corridor. */
	public static final int HALL_EVERY = 3;
	/** Corridors are 2*HALF_BAND + 1 blocks wide. */
	public static final int HALL_HALF_BAND = 1;
	/** How far non-corridor lines may wander from their nominal position. */
	public static final int JITTER = 5;

	private static final int AXIS_X = 1;
	private static final int AXIS_Z = 2;

	// Vertical plan (shared with the chunk generator).
	public static final int FLOOR_Y = 64;
	public static final int WALL_HEIGHT = 4;
	public static final int CEILING_Y = FLOOR_Y + WALL_HEIGHT + 1; // 69

	// Fixture results.
	public static final int FIXTURE_NONE = 0;
	public static final int FIXTURE_LIT = 1;
	public static final int FIXTURE_FLICKER = 2;
	public static final int FIXTURE_DEAD = 3;

	// Perimeter wall segment decisions.
	private static final int SEG_MISSING = 0;
	private static final int SEG_DOOR = 1;
	private static final int SEG_SOLID = 2;

	/**
	 * Per-cell cached maze data. One {@link Level0Layout} is used per chunk,
	 * which only intersects a handful of cells, so this stays tiny and is
	 * discarded with the chunk generation call.
	 */
	private static final class CellMaze {
		final boolean open;
		final int gx;
		final int gz;
		final boolean[] edges;
		final int[] vx;
		final int[] hz;

		CellMaze(boolean open, int gx, int gz, boolean[] edges, int[] vx, int[] hz) {
			this.open = open;
			this.gx = gx;
			this.gz = gz;
			this.edges = edges;
			this.vx = vx;
			this.hz = hz;
		}
	}

	private final java.util.HashMap<Long, CellMaze> cellCache = new java.util.HashMap<>();

	private CellMaze cellMaze(int i, int k) {
		long key = ((long) i << 32) ^ (k & 0xffffffffL);
		CellMaze cached = cellCache.get(key);
		if (cached != null) {
			return cached;
		}
		int x0 = lineX(i);
		int x1 = lineX(i + 1);
		int z0 = lineZ(k);
		int z1 = lineZ(k + 1);
		boolean open = cellIsOpenRoom(i, k);
		int[] grid = mazeGrid(i, k, x1 - x0, z1 - z0);
		int gx = grid[0];
		int gz = grid[1];
		boolean[] edges = open ? null : mazeEdges(i, k, gx, gz);
		int[] vx = new int[gx - 1];
		for (int s = 1; s < gx; s++) {
			vx[s - 1] = x0 + Math.round(s * (float) (x1 - x0) / gx);
		}
		int[] hz = new int[gz - 1];
		for (int s = 1; s < gz; s++) {
			hz[s - 1] = z0 + Math.round(s * (float) (z1 - z0) / gz);
		}
		CellMaze maze = new CellMaze(open, gx, gz, edges, vx, hz);
		cellCache.put(key, maze);
		return maze;
	}

	/** Fraction of cells left completely open (open rooms). */
	private static final float OPEN_CELL_CHANCE = 0.20F;
	/** Extra loop openings carved through maze walls (reduces dead-end density). */
	private static final float MAZE_LOOP_CHANCE = 0.12F;

	private final long seed;

	public Level0Layout(long seed) {
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
		return mix(this.seed ^ mix(a * 31 + b * 37 + c * 131 + 0x243F6A8885A308D3L));
	}

	private float rand01(long a, long b, long c) {
		return (hash(a, b, c) >>> 11) * 0x1.0p-53f;
	}

	private float rand01(long a, long b, long c, long d) {
		return (mix(hash(a, b, c) ^ mix(d * 0x9E3779B1L + 17L)) >>> 11) * 0x1.0p-53f;
	}

	private static int floorMod(int a, int n) {
		int r = a % n;
		return r < 0 ? r + n : r;
	}

	private static int floorDiv(int a, int n) {
		int q = a / n;
		if (a < 0 && a % n != 0) {
			q--;
		}
		return q;
	}

	private static int clampInt(int v, int lo, int hi) {
		return v < lo ? lo : Math.min(v, hi);
	}

	// ------------------------------------------------------------- geometry
	/** World coordinate of wall/corridor line {@code i} on the X axis. */
	public int lineX(int i) {
		if (floorMod(i, HALL_EVERY) == 0) {
			return i * SPACING; // perfectly straight corridors
		}
		return i * SPACING + (int) (rand01(AXIS_X, i, 0) * (JITTER * 2 + 1)) - JITTER;
	}

	/** World coordinate of wall/corridor line {@code k} on the Z axis. */
	public int lineZ(int k) {
		if (floorMod(k, HALL_EVERY) == 0) {
			return k * SPACING;
		}
		return k * SPACING + (int) (rand01(AXIS_Z, k, 0) * (JITTER * 2 + 1)) - JITTER;
	}

	private Integer lineIndexAt(int coord, boolean xAxis) {
		int q = floorDiv(coord, SPACING);
		for (int i = q - 1; i <= q + 1; i++) {
			int pos = xAxis ? lineX(i) : lineZ(i);
			if (pos == coord) {
				return i;
			}
		}
		return null;
	}

	/** Index of the cell (interval between consecutive lines) containing coord. */
	private int cellIndexAt(int coord, boolean xAxis) {
		int q = floorDiv(coord, SPACING);
		for (int c = q - 2; c <= q + 2; c++) {
			int lo = xAxis ? lineX(c) : lineZ(c);
			int hi = xAxis ? lineX(c + 1) : lineZ(c + 1);
			if (coord > lo && coord < hi) {
				return c;
			}
		}
		return q;
	}

	private boolean inHallBand(int coord, boolean xAxis) {
		int step = HALL_EVERY * SPACING;
		int nearest = Math.round((float) coord / step) * step;
		if (Math.abs(coord - nearest) > HALL_HALF_BAND) {
			return false;
		}
		int idx = nearest / SPACING;
		int pos = xAxis ? lineX(idx) : lineZ(idx);
		return Math.abs(coord - pos) <= HALL_HALF_BAND && floorMod(idx, HALL_EVERY) == 0;
	}

	// ---------------------------------------------------------- perimeter
	private int rawSegmentStatus(int axisSalt, int wallIndex, int cellIndex) {
		float r = rand01(axisSalt, wallIndex, cellIndex);
		if (r < 0.13F) {
			return SEG_MISSING; // occasional merged, larger rooms
		} else if (r < 0.57F) {
			return SEG_DOOR;
		}
		return SEG_SOLID;
	}

	private boolean middleCellEnclosed(int cx, int cz) {
		return floorMod(cx, HALL_EVERY) == 1
				&& floorMod(cz, HALL_EVERY) == 1
				&& rawSegmentStatus(AXIS_X, cx, cz) == SEG_SOLID
				&& rawSegmentStatus(AXIS_X, cx + 1, cz) == SEG_SOLID
				&& rawSegmentStatus(AXIS_Z, cz, cx) == SEG_SOLID
				&& rawSegmentStatus(AXIS_Z, cz + 1, cx) == SEG_SOLID;
	}

	private int escapeSide(int cx, int cz) {
		return (int) (rand01(9, cx, cz) * 4);
	}

	private int xWallSegmentStatus(int wallIndex, int k) {
		int raw = rawSegmentStatus(AXIS_X, wallIndex, k);
		if (raw != SEG_SOLID) {
			return raw;
		}
		for (int cx = wallIndex - 1; cx <= wallIndex; cx++) {
			if (floorMod(cx, HALL_EVERY) == 1 && floorMod(k, HALL_EVERY) == 1 && middleCellEnclosed(cx, k)) {
				int roleForThisWall = (cx == wallIndex) ? 0 : 1; // 0=WEST, 1=EAST
				if (escapeSide(cx, k) == roleForThisWall) {
					return SEG_DOOR;
				}
			}
		}
		return SEG_SOLID;
	}

	private int zWallSegmentStatus(int wallIndex, int i) {
		int raw = rawSegmentStatus(AXIS_Z, wallIndex, i);
		if (raw != SEG_SOLID) {
			return raw;
		}
		for (int cz = wallIndex - 1; cz <= wallIndex; cz++) {
			if (floorMod(i, HALL_EVERY) == 1 && floorMod(cz, HALL_EVERY) == 1 && middleCellEnclosed(i, cz)) {
				int roleForThisWall = (cz == wallIndex) ? 2 : 3; // 2=SOUTH, 3=NORTH
				if (escapeSide(i, cz) == roleForThisWall) {
					return SEG_DOOR;
				}
			}
		}
		return SEG_SOLID;
	}

	private boolean perimeterXWallBlocks(int wallIndex, int z) {
		int k = cellIndexAt(z, true);
		int z0 = lineZ(k);
		int z1 = lineZ(k + 1);
		// Keep 2-block returns/pillars where perpendicular walls meet.
		if (z <= z0 + 1 || z >= z1 - 1) {
			return true;
		}
		int status = xWallSegmentStatus(wallIndex, k);
		if (status == SEG_MISSING) {
			return false;
		}
		if (status == SEG_SOLID) {
			return true;
		}
		int mid = (z0 + z1) / 2 + (int) (rand01(11, wallIndex, k) * 5) - 2;
		return z != mid - 1 && z != mid;
	}

	private boolean perimeterZWallBlocks(int wallIndex, int x) {
		int i = cellIndexAt(x, false);
		int x0 = lineX(i);
		int x1 = lineX(i + 1);
		if (x <= x0 + 1 || x >= x1 - 1) {
			return true;
		}
		int status = zWallSegmentStatus(wallIndex, i);
		if (status == SEG_MISSING) {
			return false;
		}
		if (status == SEG_SOLID) {
			return true;
		}
		int mid = (x0 + x1) / 2 + (int) (rand01(12, wallIndex, i) * 5) - 2;
		return x != mid - 1 && x != mid;
	}

	// --------------------------------------------------------- maze cells

	/** Sub-room grid dimensions for the maze inside cell (i,k). */
	private int[] mazeGrid(int i, int k, int spanX, int spanZ) {
		int gx = clampInt(Math.round(spanX / 8.0F), 2, 3);
		int gz = clampInt(Math.round(spanZ / 8.0F), 2, 3);
		return new int[]{gx, gz};
	}

	private boolean cellIsOpenRoom(int i, int k) {
		return rand01(40, i, k) < OPEN_CELL_CHANCE;
	}

	/**
	 * Deterministic iterative DFS spanning-tree maze.
	 *
	 * @param edge encoded openings: index {@code edgeId(r, c, dir)} bit set means
	 *             the wall between the two neighbouring sub-rooms is open.
	 */
	private boolean[] mazeEdges(int i, int k, int gx, int gz) {
		int dirs = 4;
		boolean[] edges = new boolean[gx * gz * dirs];
		boolean[] visited = new boolean[gx * gz];
		int[] stack = new int[gx * gz];
		int sp = 0;
		stack[sp++] = 0;
		visited[0] = true;
		// Deterministic per-cell RNG stream.
		long rng = hash(51, i, k) | 1L;
		while (sp > 0) {
			int node = stack[sp - 1];
			int c = node % gx;
			int r = node / gx;
			// Gather unvisited neighbours in a deterministic, shuffled order.
			int[] nb = new int[4];
			int count = 0;
			int[] candidates = {node - gx, node + gx, node - 1, node + 1};
			int[] dirIds = {0, 1, 2, 3}; // N,S,W,E
			// simple Fisher-Yates with the per-cell stream
			for (int q = 0; q < 4; q++) {
				rng = rng * 0x5851F42D4C957F2DL + 0x14057B7EF767814FL;
				int pick = q + (int) ((rng >>> 33) % (4 - q));
				int tmpNode = candidates[q];
				candidates[q] = candidates[pick];
				candidates[pick] = tmpNode;
				int tmpDir = dirIds[q];
				dirIds[q] = dirIds[pick];
				dirIds[pick] = tmpDir;
			}
			for (int q = 0; q < 4; q++) {
				int nn = candidates[q];
				int nc = nn < 0 ? -1 : nn % gx;
				int nr = nn < 0 ? -1 : nn / gx;
				boolean valid = nn >= 0 && nr >= 0 && nr < gz && nc >= 0 && nc < gx && !visited[nn];
				// Reject horizontal wrap-around (c changes by +-1 only when on the same row).
				if (dirIds[q] == 2 && (c == 0)) {
					valid = false;
				}
				if (dirIds[q] == 3 && (c == gx - 1)) {
					valid = false;
				}
				if (valid) {
					nb[count++] = q;
				}
			}
			if (count == 0) {
				sp--;
				continue;
			}
			rng = rng * 0x5851F42D4C957F2DL + 0x14057B7EF767814FL;
			int choice = nb[(int) ((rng >>> 33) % count)];
			int nn = candidates[choice];
			int dir = dirIds[choice];
			visited[nn] = true;
			edges[node * 4 + dir] = true;
			int opposite = dir ^ 1; // N<->S, W<->E by id order
			edges[nn * 4 + opposite] = true;
			stack[sp++] = nn;
		}
		// Knock a few extra walls out to add loops (less of a pure tree maze).
		for (int r = 0; r < gz; r++) {
			for (int c = 0; c < gx; c++) {
				int node = r * gx + c;
				if (c + 1 < gx && !edges[node * 4 + 3] && rand01(52, i, k, node) < MAZE_LOOP_CHANCE) {
					edges[node * 4 + 3] = true;
					edges[(node + 1) * 4 + 2] = true;
				}
				if (r + 1 < gz && !edges[node * 4 + 1] && rand01(53, i, k, node) < MAZE_LOOP_CHANCE) {
					edges[node * 4 + 1] = true;
					edges[(node + gx) * 4 + 0] = true;
				}
			}
		}
		return edges;
	}

	/**
	 * Maze partition check for a column known to be strictly inside cell (i,k)
	 * (not on a perimeter line and not in a corridor band).
	 */
	private boolean interiorPartitionBlocks(int i, int k, int x, int z) {
		int x0 = lineX(i);
		int x1 = lineX(i + 1);
		int z0 = lineZ(k);
		int z1 = lineZ(k + 1);
		CellMaze maze = cellMaze(i, k);
		if (maze.open) {
			return false;
		}
		int gx = maze.gx;
		int gz = maze.gz;
		boolean[] edges = maze.edges;
		int[] vx = maze.vx;
		int[] hz = maze.hz;

		int partitionX = indexOf(vx, x);
		int partitionZ = indexOf(hz, z);

		// Junctions where two partitions cross are always solid pillars.
		if (partitionX >= 0 && partitionZ >= 0) {
			return true;
		}

		if (partitionX >= 0) {
			// Wall between maze columns (partitionX) and (partitionX+1).
			int c = partitionX; // left sub-room column
			int r = subRow(hz, z0, z1, gz, z);
			if (r < 0) {
				return false;
			}
			// Junction pillars at horizontal partition rows.
			for (int h : hz) {
				if (z == h) {
					return true;
				}
			}
			if (edges[(r * gx + c) * 4 + 3]) { // edge to the eastern neighbour open
				int lo = r == 0 ? z0 + 2 : hz[r - 1];
				int hi = r == gz - 1 ? z1 - 2 : hz[r];
				return !inVerticalGap(i, k, c, r, lo, hi, z);
			}
			return true;
		}

		if (partitionZ >= 0) {
			// Wall between maze rows (partitionZ) and (partitionZ+1).
			int r = partitionZ;
			int c = subCol(vx, x0, x1, gx, x);
			if (c < 0) {
				return false;
			}
			for (int v : vx) {
				if (x == v) {
					return true;
				}
			}
			if (edges[(r * gx + c) * 4 + 1]) { // edge to the southern neighbour open
				int lo = c == 0 ? x0 + 2 : vx[c - 1];
				int hi = c == gx - 1 ? x1 - 2 : vx[c];
				return !inHorizontalGap(i, k, c, r, lo, hi, x);
			}
			return true;
		}
		return false;
	}

	private boolean inVerticalGap(int i, int k, int c, int r, int lo, int hi, int z) {
		int width = rand01(60, i, k, r * 8 + c) < 0.22F ? 3 : 2;
		int mid = lo + 1 + (int) (rand01(61, i, k, r * 8 + c) * Math.max(1, hi - lo - width - 1));
		for (int g = 0; g < width; g++) {
			if (z == mid + g) {
				return true;
			}
		}
		return false;
	}

	private boolean inHorizontalGap(int i, int k, int c, int r, int lo, int hi, int x) {
		int width = rand01(62, i, k, r * 8 + c) < 0.22F ? 3 : 2;
		int mid = lo + 1 + (int) (rand01(63, i, k, r * 8 + c) * Math.max(1, hi - lo - width - 1));
		for (int g = 0; g < width; g++) {
			if (x == mid + g) {
				return true;
			}
		}
		return false;
	}

	private static int indexOf(int[] arr, int v) {
		for (int i = 0; i < arr.length; i++) {
			if (arr[i] == v) {
				return i;
			}
		}
		return -1;
	}

	/** Maze row containing z, or -1 if it sits on a perimeter-adjacent seam. */
	private int subRow(int[] hz, int z0, int z1, int gz, int z) {
		if (z <= z0 + 1 || z >= z1 - 1) {
			return -1;
		}
		for (int s = 0; s < hz.length; s++) {
			if (z < hz[s]) {
				return s;
			}
		}
		return gz - 1;
	}

	private int subCol(int[] vx, int x0, int x1, int gx, int x) {
		if (x <= x0 + 1 || x >= x1 - 1) {
			return -1;
		}
		for (int s = 0; s < vx.length; s++) {
			if (x < vx[s]) {
				return s;
			}
		}
		return gx - 1;
	}

	/**
	 * @return true if the column at (x,z) is a solid wallpaper wall column.
	 */
	public boolean isWallColumn(int x, int z) {
		// Corridor bands suppress every wall (giving open halls + junctions).
		if (inHallBand(x, true) || inHallBand(z, false)) {
			return false;
		}
		Integer xi = lineIndexAt(x, true);
		Integer ki = lineIndexAt(z, false);
		if (xi != null) {
			if (floorMod(xi, HALL_EVERY) == 0) {
				return false;
			}
			if (perimeterXWallBlocks(xi, z)) {
				return true;
			}
		}
		if (ki != null) {
			if (floorMod(ki, HALL_EVERY) == 0) {
				return false;
			}
			if (perimeterZWallBlocks(ki, x)) {
				return true;
			}
		}
		if (xi == null && ki == null) {
			int i = cellIndexAt(x, false);
			int k = cellIndexAt(z, true);
			return interiorPartitionBlocks(i, k, x, z);
		}
		return false;
	}

	public boolean isDampWall(int x, int z) {
		return rand01(3, x, z) < 0.15F;
	}

	// ------------------------------------------------------------- lighting
	/**
	 * Deterministic ceiling fixture plan. Corridors get a bright regular row;
	 * rooms use a 6-spaced grid with occasional permanently dark patches.
	 */
	public int fixtureAt(int x, int z) {
		boolean hallX = inHallBand(x, true);
		boolean hallZ = inHallBand(z, false);

		if (hallX && !hallZ) {
			int idx = Math.round((float) x / (HALL_EVERY * SPACING)) * HALL_EVERY;
			if (x == lineX(idx) && floorMod(z + 2, 4) == 0) {
				float r = rand01(7, x, z);
				if (r < 0.20F) {
					return FIXTURE_FLICKER;
				} else if (r < 0.26F) {
					return FIXTURE_DEAD;
				}
				return FIXTURE_LIT;
			}
			return FIXTURE_NONE;
		}
		if (hallZ) {
			int idx = Math.round((float) z / (HALL_EVERY * SPACING)) * HALL_EVERY;
			if (z == lineZ(idx) && floorMod(x + 2, 4) == 0) {
				float r = rand01(8, x, z);
				if (r < 0.20F) {
					return FIXTURE_FLICKER;
				} else if (r < 0.26F) {
					return FIXTURE_DEAD;
				}
				return FIXTURE_LIT;
			}
			return FIXTURE_NONE;
		}
		if (floorMod(x, 6) == 2 && floorMod(z, 6) == 2) {
			int rx = floorDiv(x, HALL_EVERY * SPACING);
			int rz = floorDiv(z, HALL_EVERY * SPACING);
			if (rand01(5, rx, rz) < 0.14F) {
				return FIXTURE_DEAD;
			}
			float r = rand01(6, x, z);
			if (r < 0.18F) {
				return FIXTURE_FLICKER;
			} else if (r < 0.24F) {
				return FIXTURE_DEAD;
			}
			return FIXTURE_LIT;
		}
		return FIXTURE_NONE;
	}
}
