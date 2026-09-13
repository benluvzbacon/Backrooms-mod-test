#!/usr/bin/env python3
"""
Standalone simulation of net.backrooms.worldgen.Level0Layout, used to validate
the procedural floor plan without launching Minecraft:

  - full connectivity flood fill (no sealed pockets)
  - corridor bands stay open
  - no obvious repeating 16x16 chunk pattern
  - wall-density sanity (avoid huge empty areas)

Java 64-bit signed-overflow semantics are reproduced exactly.
Usage: python3 tools/sim_layout.py [seed] [radius]
"""
import collections
import statistics
import sys

MASK = (1 << 64) - 1
SEED = int(sys.argv[1]) if len(sys.argv) > 1 else 42
R = int(sys.argv[2]) if len(sys.argv) > 2 else 300


def s64(x):
    x &= MASK
    return x - (1 << 64) if x >= (1 << 63) else x


def u64(x):
    return x & MASK


K1 = 0x9E3779B97F4A7C15
A = 0xBF58476D1CE4E5B9
B = 0x94D049BB133111EB


def mix(z):
    z = s64(u64(z + K1))
    z = s64(u64(z ^ (u64(z) >> 30)) * A)
    z = s64(u64(z ^ (u64(z) >> 27)) * B)
    return s64(u64(z) ^ (u64(z) >> 31))


def h(a, b, c):
    return mix(s64(SEED ^ mix(s64(a * 31 + b * 37 + c * 131 + 0x243F6A8885A308D3))))


def r01(a, b, c):
    return (u64(h(a, b, c)) >> 11) * (2.0 ** -53)


SPACING = 24
HALL = 3
BAND = 1
JITTER = 5


def fdiv(a, n):
    return -((-a) // n) if a < 0 else a // n


def line(pos_axis, i):
    if i % HALL == 0:
        return i * SPACING
    return i * SPACING + int(r01(pos_axis, i, 0) * (JITTER * 2 + 1)) - JITTER


def line_x(i):
    return line(1, i)


def line_z(k):
    return line(2, k)


def line_index(coord, xaxis):
    q = fdiv(coord, SPACING)
    for i in range(q - 1, q + 2):
        if (line_x(i) if xaxis else line_z(i)) == coord:
            return i
    return None


def cell_index(coord, xaxis):
    q = fdiv(coord, SPACING)
    for c in range(q - 2, q + 3):
        lo = line_x(c) if xaxis else line_z(c)
        hi = line_x(c + 1) if xaxis else line_z(c + 1)
        if lo < coord < hi:
            return c
    return q


def in_band(coord, xaxis):
    step = HALL * SPACING
    nearest = round(coord / step) * step
    if abs(coord - nearest) > BAND:
        return False
    idx = nearest // SPACING
    pos = line_x(idx) if xaxis else line_z(idx)
    return abs(coord - pos) <= BAND and idx % HALL == 0


SEG_MISSING, SEG_DOOR, SEG_SOLID = 0, 1, 2


def raw(axis, wall, cell):
    r = r01(axis, wall, cell)
    return SEG_MISSING if r < 0.30 else SEG_DOOR if r < 0.78 else SEG_SOLID


def enclosed(cx, cz):
    return (cx % HALL == 1 and cz % HALL == 1
            and raw(1, cx, cz) == SEG_SOLID
            and raw(1, cx + 1, cz) == SEG_SOLID
            and raw(2, cz, cx) == SEG_SOLID
            and raw(2, cz + 1, cx) == SEG_SOLID)


def escape(cx, cz):
    return int(r01(9, cx, cz) * 4)


def xseg(w, k):
    rs = raw(1, w, k)
    if rs != SEG_SOLID:
        return rs
    for cx in (w - 1, w):
        if cx % HALL == 1 and k % HALL == 1 and enclosed(cx, k) and escape(cx, k) == (0 if cx == w else 1):
            return SEG_DOOR
    return SEG_SOLID


def zseg(w, i):
    rs = raw(2, w, i)
    if rs != SEG_SOLID:
        return rs
    for cz in (w - 1, w):
        if i % HALL == 1 and cz % HALL == 1 and enclosed(i, cz) and escape(i, cz) == (2 if cz == w else 3):
            return SEG_DOOR
    return SEG_SOLID


def xwallblocks(i, z):
    k = cell_index(z, True)
    z0, z1 = line_z(k), line_z(k + 1)
    if z <= z0 + 1 or z >= z1 - 1:
        return True
    st = xseg(i, k)
    if st == SEG_MISSING:
        return False
    if st == SEG_SOLID:
        return True
    mid = (z0 + z1) // 2 + int(r01(11, i, k) * 5) - 2
    return z not in (mid - 1, mid)


def zwallblocks(i, x):
    k = cell_index(x, False)
    x0, x1 = line_x(k), line_x(k + 1)
    if x <= x0 + 1 or x >= x1 - 1:
        return True
    st = zseg(i, k)
    if st == SEG_MISSING:
        return False
    if st == SEG_SOLID:
        return True
    mid = (x0 + x1) // 2 + int(r01(12, i, k) * 5) - 2
    return x not in (mid - 1, mid)


def is_wall(x, z):
    if in_band(x, True) or in_band(z, False):
        return False
    xi = line_index(x, True)
    ki = line_index(z, False)
    if xi is not None and xi % HALL != 0 and xwallblocks(xi, z):
        return True
    if ki is not None and ki % HALL != 0 and zwallblocks(ki, x):
        return True
    return False


def main():
    walk = set()
    walls = 0
    for x in range(-R, R + 1):
        for z in range(-R, R + 1):
            (walls := walls + 1) if is_wall(x, z) else walk.add((x, z))
    seen = {(0, 0)}
    dq = collections.deque([(0, 0)])
    while dq:
        x, z = dq.popleft()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + dx, z + dz)
            if n in walk and n not in seen:
                seen.add(n)
                dq.append(n)
    interior_unreached = sum(
        1 for p in walk if p not in seen and abs(p[0]) <= R - 100 and abs(p[1]) <= R - 100)
    total = (2 * R + 1) ** 2
    print(f"seed={SEED} radius={R}: walls {walls} ({walls/total:.1%}), walkable {len(walk)}")
    print(f"unreached interior columns: {interior_unreached}")

    blocked_center = blocked_band = 0
    for i in range(-5, 6):
        cx = i * HALL * SPACING
        for z in range(-R, R + 1):
            blocked_center += 1 if is_wall(cx, z) else 0
            blocked_band += 1 if is_wall(cx - 1, z) or is_wall(cx + 1, z) else 0
        cz = cx
        for x in range(-R, R + 1):
            blocked_center += 1 if is_wall(x, cz) else 0
            blocked_band += 1 if is_wall(x, cz - 1) or is_wall(x, cz + 1) else 0
    print(f"blocked corridor centers: {blocked_center}, blocked band edges: {blocked_band}")

    chunks = {}
    for cx in range(-6, 7):
        for cz in range(-6, 7):
            chunks[(cx, cz)] = tuple(
                is_wall(cx * 16 + dx, cz * 16 + dz) for dx in range(16) for dz in range(16))
    print(f"unique 16x16 chunk patterns: {len(set(chunks.values()))}/{len(chunks)}")

    dens = []
    for qx in range(-4, 5):
        for qz in range(-4, 5):
            dens.append(sum(1 for x in range(qx * 60, qx * 60 + 60)
                            for z in range(qz * 60, qz * 60 + 60) if is_wall(x, z)) / 3600)
    print(f"wall density/60x60: min {min(dens):.2%} max {max(dens):.2%} mean {statistics.mean(dens):.2%}")

    ok = interior_unreached == 0 and blocked_center == 0 and blocked_band == 0
    print("RESULT:", "OK" if ok else "FAIL")
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
