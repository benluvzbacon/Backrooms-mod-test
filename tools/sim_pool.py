#!/usr/bin/env python3
"""
Standalone simulation of net.backrooms.worldgen.PoolroomsLayout, used to
validate the Poolrooms floor plan without launching Minecraft:

  - full connectivity flood fill (every room reachable through archways)
  - water basin / pillar / exit-pad distribution

Java 64-bit signed-overflow semantics are reproduced exactly.
Usage: python3 tools/sim_pool.py [radius]
"""
import collections
import sys

MASK = (1 << 64) - 1
R = int(sys.argv[1]) if len(sys.argv) > 1 else 150
SEEDS = (42, 1, 123, 9999, -50)


def s64(x):
    x &= MASK
    return x - (1 << 64) if x >= (1 << 63) else x


def u64(x):
    return x & MASK


def mix(z):
    z = s64(u64(z + 0x9E3779B97F4A7C15))
    z = s64(u64(z ^ (u64(z) >> 30)) * 0xBF58476D1CE4E5B9)
    z = s64(u64(z ^ (u64(z) >> 27)) * 0x94D049BB133111EB)
    return s64(u64(z) ^ (u64(z) >> 31))


class Layout:
    SPACING = 24

    def __init__(self, seed):
        self.seed = seed

    def hash(self, a, b, c):
        return mix(s64(self.seed ^ mix(s64(a * 31 + b * 37 + c * 131 + 0x85A308D313198A2E))))

    def rand(self, a, b, c):
        return (u64(self.hash(a, b, c)) >> 11) * (2.0 ** -53)

    @staticmethod
    def fmod(a, n):
        return a % n

    @staticmethod
    def fdiv(a, n):
        return int(a // n)

    def in_arch(self, h, local):
        c1 = 5 + (h % 5)
        c2 = 17 + ((h >> 16) % 4)
        return abs(local - c1) <= 2 or abs(local - c2) <= 2

    def is_wall(self, x, z):
        lx, lz = self.fmod(x, 24), self.fmod(z, 24)
        on_x, on_z = lx == 0, lz == 0
        if not on_x and not on_z:
            return False
        if on_x and on_z:
            return True
        cx, cz = self.fdiv(x, 24), self.fdiv(z, 24)
        if on_x:
            if lz <= 1 or lz >= 22:
                return True
            return not self.in_arch(self.hash(90, cx, cz), lz) \
                and not self.in_arch(self.hash(91, cx, cz), lz)
        if lx <= 1 or lx >= 22:
            return True
        return not self.in_arch(self.hash(92, cx, cz), lx) \
            and not self.in_arch(self.hash(93, cx, cz), lx)

    def is_pool_cell(self, cx, cz):
        return self.rand(85, cx, cz) < 0.45

    def pad_center(self, cx, cz):
        if self.hash(94, cx, cz) % 16 != 0:
            return None
        lx = 3 + (self.hash(95, cx, cz) % 10)
        lz = 3 + (self.hash(96, cx, cz) % 10)
        wx, wz = cx * 16 + lx, cz * 16 + lz
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                if self.is_wall(wx + dx, wz + dz):
                    return None
        return wx, wz

    def pad_role(self, x, z):
        cx, cz = self.fdiv(x, 16), self.fdiv(z, 16)
        for dcx in (-1, 0, 1):
            for dcz in (-1, 0, 1):
                c = self.pad_center(cx + dcx, cz + dcz)
                if c is None:
                    continue
                if abs(x - c[0]) <= 1 and abs(z - c[1]) <= 1:
                    return 2 if (x == c[0] and z == c[1]) else 1
        return 0

    def is_water(self, x, z):
        if self.is_wall(x, z) or self.pad_role(x, z) != 0:
            return False
        cx, cz = self.fdiv(x, 24), self.fdiv(z, 24)
        if not self.is_pool_cell(cx, cz):
            return False
        lx, lz = self.fmod(x, 24), self.fmod(z, 24)
        return 3 <= lx <= 20 and 3 <= lz <= 20

    def is_pillar(self, x, z):
        if self.is_wall(x, z) or self.pad_role(x, z) != 0:
            return False
        cx, cz = self.fdiv(x, 24), self.fdiv(z, 24)
        if self.is_pool_cell(cx, cz):
            return False
        lx, lz = self.fmod(x, 24), self.fmod(z, 24)
        sx = 0 if lx == 5 else 1 if lx == 18 else -1
        sz = 0 if lz == 5 else 1 if lz == 18 else -1
        if sx < 0 or sz < 0:
            return False
        return (self.hash(86, cx, cz) >> ((sx * 2 + sz) * 8)) % 3 != 0

    def is_dry_floor(self, x, z):
        return not self.is_wall(x, z) and not self.is_water(x, z) \
            and not self.is_pillar(x, z) and self.pad_role(x, z) == 0


def main():
    failures = 0
    for seed in SEEDS:
        layout = Layout(seed)
        walk = set()
        for x in range(-R, R + 1):
            for z in range(-R, R + 1):
                if not layout.is_wall(x, z):
                    walk.add((x, z))
        # (0,0) is a wall lattice corner; start at the nearest dry floor.
        start = next((x, z) for x in range(1, 9) for z in range(1, 9)
                     if layout.is_dry_floor(x, z))
        queue = collections.deque([start])
        reached = {start}
        while queue:
            x, z = queue.popleft()
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = (x + dx, z + dz)
                if n in walk and n not in reached:
                    reached.add(n)
                    queue.append(n)
        enclosed = sum(1 for (x, z) in walk
                       if abs(x) <= R - 50 and abs(z) <= R - 50
                       and (x, z) not in reached)
        water = sum(1 for x in range(-120, 121) for z in range(-120, 121)
                    if layout.is_water(x, z))
        pads = sum(1 for x in range(-120, 121) for z in range(-120, 121)
                   if layout.pad_role(x, z) == 2)
        walls = sum(1 for x in range(-R, R + 1) for z in range(-R, R + 1)
                    if layout.is_wall(x, z))
        print(f"seed={seed}: walkable={len(walk)} reached={len(reached)} "
              f"enclosed={enclosed} wall%={walls / (2 * R + 1) ** 2 * 100:.1f} "
              f"water={water} exitPads={pads}")
        if enclosed != 0 or water == 0 or pads == 0:
            failures += 1
    sys.exit(1 if failures else 0)


if __name__ == "__main__":
    main()
