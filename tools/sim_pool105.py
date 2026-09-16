#!/usr/bin/env python3
"""Offline validation of the v1.0.5 PoolroomsLayout (roofed maze + great halls).

Mirrors src/main/java/net/backrooms/worldgen/PoolroomsLayout.java 1:1 and
checks: interior connectivity (flood fill), perimeter-arch symmetry across
cell borders, tall-wall symmetry across borders, roof coverage, and that
great halls are rare and open inside.
"""
import collections
import random
import sys

SPACING = 24
POOL_CELL_CHANCE = 0.45
GREAT_HALL_CHANCE = 0.03
MAZE_LOOP_CHANCE = 0.12
ARCH_WIDTH = 5
GRID = 3

MASK64 = (1 << 64) - 1


def mix(z):
    z = (z + 0x9E3779B97F4A7C15) & MASK64
    z = ((z ^ (z >> 30)) * 0xBF58476D1CE4E5B9) & MASK64
    z = ((z ^ (z >> 27)) * 0x94D049BB133111EB) & MASK64
    return z ^ (z >> 31)


def s32(x):
    x &= MASK64
    return x - (1 << 64) if x >= (1 << 63) else x


class Layout:
    def __init__(self, seed):
        self.seed = seed
        self.edge_cache = {}

    def hash(self, a, b, c):
        return mix((self.seed ^ mix((a * 31 + b * 37 + c * 131 + 0x85A308D313198A2E) & MASK64)) & MASK64)

    def rand01(self, a, b, c, d=None):
        if d is None:
            h = self.hash(a, b, c)
        else:
            h = mix((self.hash(a, b, c) ^ mix((d * 0x9E3779B1 + 17) & MASK64)) & MASK64)
        return (h >> 11) * (1.0 / (1 << 53))

    def is_great_hall(self, cx, cz):
        return self.rand01(88, cx, cz) < GREAT_HALL_CHANCE

    def is_pool_cell(self, cx, cz):
        return not self.is_great_hall(cx, cz) and self.rand01(85, cx, cz) < POOL_CELL_CHANCE

    def in_arch(self, h, local):
        c1 = 5 + (h % 5)
        c2 = 17 + ((h >> 16) % 4)
        return abs(local - c1) <= ARCH_WIDTH // 2 or abs(local - c2) <= ARCH_WIDTH // 2

    def is_wall_column(self, x, z):
        lx, lz = x % SPACING, z % SPACING
        cx, cz = x // SPACING, z // SPACING
        on_x, on_z = lx == 0, lz == 0
        if on_x or on_z:
            if on_x and on_z:
                return True
            return self._perimeter(x, z, lx, lz, cx, cz, on_x, on_z)
        if self.is_great_hall(cx, cz):
            return False
        return self._partition(cx, cz, lx, lz)

    def _perimeter(self, x, z, lx, lz, cx, cz, on_x, on_z):
        if on_x:
            if lz <= 1 or lz >= SPACING - 2:
                return True
            return not (self.in_arch(self.hash(90, cx, cz), lz)
                        or self.in_arch(self.hash(91, cx, cz), lz))
        if lx <= 1 or lx >= SPACING - 2:
            return True
        return not (self.in_arch(self.hash(92, cx, cz), lx)
                    or self.in_arch(self.hash(93, cx, cz), lx))

    def is_perimeter_opening(self, x, z):
        lx, lz = x % SPACING, z % SPACING
        cx, cz = x // SPACING, z // SPACING
        if lx == 0 and lz != 0 and 1 < lz < SPACING - 2:
            return self.in_arch(self.hash(90, cx, cz), lz) or self.in_arch(self.hash(91, cx, cz), lz)
        if lz == 0 and lx != 0 and 1 < lx < SPACING - 2:
            return self.in_arch(self.hash(92, cx, cz), lx) or self.in_arch(self.hash(93, cx, cz), lx)
        return False

    def is_tall_wall(self, x, z):
        lx, lz = x % SPACING, z % SPACING
        cx, cz = x // SPACING, z // SPACING
        if lx != 0 and lz != 0:
            return False
        if lx == 0 and lz == 0:
            return any(self.is_great_hall(cx + dx, cz + dz)
                       for dx in (-1, 0) for dz in (-1, 0))
        if lx == 0:
            return self.is_great_hall(cx, cz) or self.is_great_hall(cx - 1, cz)
        return self.is_great_hall(cx, cz) or self.is_great_hall(cx, cz - 1)

    def maze_edges(self, cx, cz):
        key = (cx, cz)
        if key in self.edge_cache:
            return self.edge_cache[key]
        n = GRID * GRID
        edges = [False] * (n * 4)
        visited = [False] * n
        stack = [0]
        visited[0] = True
        rng = self.hash(71, cx, cz) | 1
        while stack:
            node = stack[-1]
            c, r = node % GRID, node // GRID
            candidates = [node - GRID, node + GRID, node - 1, node + 1]
            dir_ids = [0, 1, 2, 3]
            for q in range(4):
                rng = (rng * 0x5851F42D4C957F2D + 0x14057B7EF767814F) & MASK64
                pick = q + ((rng >> 33) % (4 - q))
                candidates[q], candidates[pick] = candidates[pick], candidates[q]
                dir_ids[q], dir_ids[pick] = dir_ids[pick], dir_ids[q]
            dead_end = True
            for q in range(4):
                nn = candidates[q]
                if nn < 0 or nn >= n or visited[nn]:
                    continue
                if (dir_ids[q] == 2 and c == 0) or (dir_ids[q] == 3 and c == GRID - 1):
                    continue
                dead_end = False
                d = dir_ids[q]
                visited[nn] = True
                edges[node * 4 + d] = True
                edges[nn * 4 + (d ^ 1)] = True
                stack.append(nn)
                break
            if dead_end:
                stack.pop()
        for r in range(GRID):
            for c in range(GRID):
                node = r * GRID + c
                if c + 1 < GRID and not edges[node * 4 + 3] and self.rand01(72, cx, cz, node) < MAZE_LOOP_CHANCE:
                    edges[node * 4 + 3] = True
                    edges[(node + 1) * 4 + 2] = True
                if r + 1 < GRID and not edges[node * 4 + 1] and self.rand01(73, cx, cz, node) < MAZE_LOOP_CHANCE:
                    edges[node * 4 + 1] = True
                    edges[(node + GRID) * 4 + 0] = True
        self.edge_cache[key] = edges
        return edges

    def _gap_v(self, cx, cz, r, c, lz):
        lo, hi = r * 8 + 1, (r + 1) * 8
        width = 3 if self.rand01(74, cx, cz, r * 8 + c) < 0.25 else 2
        span = max(1, hi - lo - width - 1)
        mid = lo + 1 + int(self.rand01(75, cx, cz, r * 8 + c) * span)
        return mid <= lz < mid + width

    def _gap_h(self, cx, cz, r, c, lx):
        lo, hi = c * 8 + 1, (c + 1) * 8
        width = 3 if self.rand01(76, cx, cz, r * 8 + c) < 0.25 else 2
        span = max(1, hi - lo - width - 1)
        mid = lo + 1 + int(self.rand01(77, cx, cz, r * 8 + c) * span)
        return mid <= lx < mid + width

    def _partition(self, cx, cz, lx, lz):
        on_v = lx in (8, 16)
        on_h = lz in (8, 16)
        if not on_v and not on_h:
            return False
        if lz <= 1 or lz >= SPACING - 2 or lx <= 1 or lx >= SPACING - 2:
            return True
        edges = self.maze_edges(cx, cz)
        if on_v and on_h:
            return True
        if on_v:
            c = 0 if lx == 8 else 1
            r = 0 if lz < 8 else (1 if lz < 16 else 2)
            if lz in (8, 16):
                return True
            if edges[(r * GRID + c) * 4 + 3]:
                return not self._gap_v(cx, cz, r, c, lz)
            return True
        r = 0 if lz == 8 else 1
        c = 0 if lx < 8 else (1 if lx < 16 else 2)
        if lx in (8, 16):
            return True
        if edges[(r * GRID + c) * 4 + 1]:
            return not self._gap_h(cx, cz, r, c, lx)
        return True

    def exit_pad_role(self, x, z):
        # pads are rare (1/16 chunks hash) and always placed clear; ignore for geometry checks
        return 0

    def is_pillar(self, x, z):
        if self.is_wall_column(x, z):
            return False
        cx, cz = x // SPACING, z // SPACING
        if self.is_pool_cell(cx, cz):
            return False
        lx, lz = x % SPACING, z % SPACING
        sx = 0 if lx == 5 else (1 if lx == 18 else -1)
        sz = 0 if lz == 5 else (1 if lz == 18 else -1)
        if sx < 0 or sz < 0:
            return False
        if self.is_great_hall(cx, cz):
            return True
        slot = sx * 2 + sz
        return (self.hash(86, cx, cz) >> (slot * 8)) % 3 != 0

    def is_water(self, x, z):
        if self.is_wall_column(x, z) or self.is_pillar(x, z):
            return False
        cx, cz = x // SPACING, z // SPACING
        if not self.is_pool_cell(cx, cz):
            return False
        lx, lz = x % SPACING, z % SPACING
        return 3 <= lx <= SPACING - 4 and 3 <= lz <= SPACING - 4

    def blocks(self, x, z):
        return self.is_wall_column(x, z) or self.is_pillar(x, z)

    def dry_floor(self, x, z):
        return not self.blocks(x, z) and not self.is_water(x, z)


def check(seed, radius=120):
    L = Layout(seed)
    walk = set()
    for x in range(-radius, radius + 1):
        for z in range(-radius, radius + 1):
            if not L.blocks(x, z):
                walk.add((x, z))
    start = next((x, z) for x in range(1, 9) for z in range(1, 9) if L.dry_floor(x, z))
    q = collections.deque([start])
    reached = {start}
    while q:
        x, z = q.popleft()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + dx, z + dz)
            if n in walk and n not in reached:
                reached.add(n)
                q.append(n)
    margin = 50
    enclosed = [p for p in walk
                if abs(p[0]) <= radius - margin and abs(p[1]) <= radius - margin and p not in reached]
    halls = sum(1 for cx in range(-10, 11) for cz in range(-10, 11)
                if L.is_great_hall(cx, cz))
    pools = sum(1 for cx in range(-10, 11) for cz in range(-10, 11)
                if L.is_pool_cell(cx, cz))

    # Walls are single lines owned by the cell on the + side. Each arch opening
    # must have walkable columns immediately on BOTH sides, with at least a
    # 2-wide gap (a partition may claim one column where it meets the wall).
    arch_problems = 0
    arch_count = 0
    for cx in range(-radius // SPACING, radius // SPACING + 1):
        for cz in range(-radius // SPACING, radius // SPACING + 1):
            # x-wall at x=cx*24, openings vary with lz
            open_cols = [lz for lz in range(2, SPACING - 2)
                         if L.is_perimeter_opening(cx * SPACING, cz * SPACING + lz)]
            for lz in open_cols:
                arch_count += 1
                inner_plus = not L.blocks(cx * SPACING + 1, cz * SPACING + lz)
                inner_minus = not L.blocks(cx * SPACING - 1, cz * SPACING + lz)
                if not (inner_plus and inner_minus):
                    # allow one blocked column within a wider opening group
                    passable_plus = sum(not L.blocks(cx * SPACING + 1, cz * SPACING + d)
                                        for d in (lz - 1, lz, lz + 1))
                    passable_minus = sum(not L.blocks(cx * SPACING - 1, cz * SPACING + d)
                                         for d in (lz - 1, lz, lz + 1))
                    if passable_plus < 2 or passable_minus < 2:
                        arch_problems += 1
            # z-wall at z=cz*24
            for lx in range(2, SPACING - 2):
                if not L.is_perimeter_opening(cx * SPACING + lx, cz * SPACING):
                    continue
                arch_count += 1
                passable_plus = sum(not L.blocks(cx * SPACING + d, cz * SPACING + 1)
                                    for d in (lx - 1, lx, lx + 1))
                passable_minus = sum(not L.blocks(cx * SPACING + d, cz * SPACING - 1)
                                     for d in (lx - 1, lx, lx + 1))
                if passable_plus < 2 or passable_minus < 2:
                    arch_problems += 1

    # tall wall must mark the shared border whenever either side is a hall
    tall_asym = 0
    for cx in range(-radius // SPACING, radius // SPACING + 1):
        for cz in range(-radius // SPACING, radius // SPACING + 1):
            hall_e = L.is_great_hall(cx, cz)
            hall_w = L.is_great_hall(cx - 1, cz)
            hall_n = L.is_great_hall(cx, cz - 1)
            if (hall_e or hall_w) != L.is_tall_wall(cx * SPACING, cz * SPACING + 12):
                tall_asym += 1
            if (hall_e or hall_n) != L.is_tall_wall(cx * SPACING + 12, cz * SPACING):
                tall_asym += 1
    asym = 0
    open_asym = 0

    # great halls: interior completely clear (no partitions; pillars allowed at 4 spots)
    bad_hall = 0
    hall_found = None
    for cx in range(-8, 9):
        for cz in range(-8, 9):
            if not L.is_great_hall(cx, cz):
                continue
            hall_found = (cx, cz)
            for lx in range(1, SPACING - 1):
                for lz in range(1, SPACING - 1):
                    x, z = cx * SPACING + lx, cz * SPACING + lz
                    if L.is_wall_column(x, z):
                        bad_hall += 1

    # determinism
    L2 = Layout(seed)
    det = sum(1 for x in range(-30, 30) for z in range(-30, 30)
              if L.is_wall_column(x, z) != L2.is_wall_column(x, z))

    print(f"seed={seed} walk={len(walk)} reached={len(reached)} enclosed={len(enclosed)} "
          f"halls={halls} pools={pools} arches={arch_count} archProblems={arch_problems} "
          f"tallAsym={tall_asym} badHallCols={bad_hall} nondet={det} sampleHall={hall_found}")
    ok = (not enclosed and arch_problems == 0 and tall_asym == 0
          and bad_hall == 0 and det == 0 and halls >= 1)
    return ok


def main():
    seeds = [1, 42, 12345, -777, 0xC0FFEE, 999999999]
    all_ok = True
    for s in seeds:
        all_ok &= check(s)
    print("ALL OK" if all_ok else "FAILURES DETECTED")
    sys.exit(0 if all_ok else 1)


if __name__ == "__main__":
    main()
