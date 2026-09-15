#!/usr/bin/env python3
"""
Standalone simulation of net.backrooms.worldgen.Level0Layout, used to validate
the procedural floor plan without launching Minecraft:

  - full connectivity flood fill (no sealed pockets)
  - corridor bands stay open
  - no obvious repeating 16x16 chunk pattern
  - wall-density sanity (maze-like but with open rooms)

Java 64-bit signed-overflow semantics are reproduced exactly.
Usage: python3 tools/sim_layout.py [seed] [radius] [--preview]
"""
import collections
import statistics
import sys

MASK = (1 << 64) - 1
ARGS = [a for a in sys.argv[1:] if not a.startswith("--")]
SEED = int(ARGS[0]) if len(ARGS) > 0 else 42
R = int(ARGS[1]) if len(ARGS) > 1 else 300
PREVIEW = "--preview" in sys.argv


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


def h3(a, b, c):
    return mix(s64(SEED ^ mix(s64(a * 31 + b * 37 + c * 131 + 0x243F6A8885A308D3))))


def h4(a, b, c, d):
    return mix(h3(a, b, c) ^ mix(s64(d * 0x9E3779B1 + 17)))


def r3(a, b, c):
    return (u64(h3(a, b, c)) >> 11) * (2.0 ** -53)


def r4(a, b, c, d):
    return (u64(h4(a, b, c, d)) >> 11) * (2.0 ** -53)


SPACING = 24
HALL = 3
BAND = 1
JITTER = 5
OPEN_CELL_CHANCE = 0.20
MAZE_LOOP_CHANCE = 0.12

SEG_MISSING, SEG_DOOR, SEG_SOLID = 0, 1, 2


def fdiv(a, n):
    return -((-a) // n) if a < 0 else a // n


def line(pos_axis, i):
    if i % HALL == 0:
        return i * SPACING
    return i * SPACING + int(r3(pos_axis, i, 0) * (JITTER * 2 + 1)) - JITTER


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


def raw(axis, wall, cell):
    r = r3(axis, wall, cell)
    if r < 0.13:
        return SEG_MISSING
    return SEG_DOOR if r < 0.57 else SEG_SOLID


def enclosed(cx, cz):
    return (cx % HALL == 1 and cz % HALL == 1
            and raw(1, cx, cz) == SEG_SOLID
            and raw(1, cx + 1, cz) == SEG_SOLID
            and raw(2, cz, cx) == SEG_SOLID
            and raw(2, cz + 1, cx) == SEG_SOLID)


def escape(cx, cz):
    return int(r3(9, cx, cz) * 4)


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
    mid = (z0 + z1) // 2 + int(r3(11, i, k) * 5) - 2
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
    mid = (x0 + x1) // 2 + int(r3(12, i, k) * 5) - 2
    return x not in (mid - 1, mid)


# ------------------------------------------------------------- maze cells
def jround(f):
    # java Math.round: floor(x + 0.5)
    import math
    return int(math.floor(f + 0.5))


def maze_grid(span_x, span_z):
    gx = min(3, max(2, jround(span_x / 8.0)))
    gz = min(3, max(2, jround(span_z / 8.0)))
    return gx, gz


def cell_open(i, k):
    return r3(40, i, k) < OPEN_CELL_CHANCE


def maze_edges(i, k, gx, gz):
    """Spanning-tree DFS maze + a few loop openings. Returns dict (node,dir)->open."""
    edges = {}
    visited = [False] * (gx * gz)
    stack = [0]
    visited[0] = True
    rng = h3(51, i, k) | 1
    while stack:
        node = stack[-1]
        c, r = node % gx, node // gx
        cands = [node - gx, node + gx, node - 1, node + 1]
        dirs = [0, 1, 2, 3]
        for q in range(4):
            rng = (rng * 0x5851F42D4C957F2D + 0x14057B7EF767814F) & MASK
            s = rng & MASK
            pick = q + ((s >> 33) % (4 - q))
            cands[q], cands[pick] = cands[pick], cands[q]
            dirs[q], dirs[pick] = dirs[pick], dirs[q]
        valid_neighbours = []
        for q in range(4):
            nn, d = cands[q], dirs[q]
            if nn < 0 or nn >= gx * gz:
                continue
            nc, nr = nn % gx, nn // gx
            if d == 2 and c == 0:
                continue
            if d == 3 and c == gx - 1:
                continue
            if not visited[nn]:
                valid_neighbours.append(q)
        if not valid_neighbours:
            stack.pop()
            continue
        rng = (rng * 0x5851F42D4C957F2D + 0x14057B7EF767814F) & MASK
        q = valid_neighbours[(rng >> 33) % len(valid_neighbours)]
        nn, d = cands[q], dirs[q]
        visited[nn] = True
        edges[(node, d)] = True
        edges[(nn, d ^ 1)] = True
        stack.append(nn)
    for r in range(gz):
        for c in range(gx):
            node = r * gx + c
            if c + 1 < gx and (node, 3) not in edges and r4(52, i, k, node) < MAZE_LOOP_CHANCE:
                edges[(node, 3)] = True
                edges[(node + 1, 2)] = True
            if r + 1 < gz and (node, 1) not in edges and r4(53, i, k, node) < MAZE_LOOP_CHANCE:
                edges[(node, 1)] = True
                edges[(node + gx, 0)] = True
    return edges


def interior_partition(i, k, x, z):
    x0, x1 = line_x(i), line_x(i + 1)
    z0, z1 = line_z(k), line_z(k + 1)
    span_x, span_z = x1 - x0, z1 - z0
    if cell_open(i, k):
        return False
    gx, gz = maze_grid(span_x, span_z)
    edges = maze_edges(i, k, gx, gz)
    vx = [x0 + jround(s * span_x / gx) for s in range(1, gx)]
    hz = [z0 + jround(s * span_z / gz) for s in range(1, gz)]
    px = vx.index(x) + 1 if x in vx else -1
    pz = hz.index(z) + 1 if z in hz else -1
    if px >= 0 and pz >= 0:
        return True
    if px >= 0:
        c = px - 1
        if z <= z0 + 1 or z >= z1 - 1:
            return False
        if z in hz:
            return True
        r = next((s for s in range(len(hz)) if z < hz[s]), gz - 1)
        if edges.get((r * gx + c, 3)):
            lo = z0 + 2 if r == 0 else hz[r - 1]
            hi = z1 - 2 if r == gz - 1 else hz[r]
            width = 3 if r4(60, i, k, r * 8 + c) < 0.22 else 2
            span = max(1, hi - lo - width - 1)
            mid = lo + 1 + int(r4(61, i, k, r * 8 + c) * span)
            return not any(z == mid + g for g in range(width))
        return True
    if pz >= 0:
        r = pz - 1
        if x <= x0 + 1 or x >= x1 - 1:
            return False
        if x in vx:
            return True
        c = next((s for s in range(len(vx)) if x < vx[s]), gx - 1)
        if edges.get((r * gx + c, 1)):
            lo = x0 + 2 if c == 0 else vx[c - 1]
            hi = x1 - 2 if c == gx - 1 else vx[c]
            width = 3 if r4(62, i, k, r * 8 + c) < 0.22 else 2
            span = max(1, hi - lo - width - 1)
            mid = lo + 1 + int(r4(63, i, k, r * 8 + c) * span)
            return not any(x == mid + g for g in range(width))
        return True
    return False


def is_wall(x, z):
    if in_band(x, True) or in_band(z, False):
        return False
    xi = line_index(x, True)
    ki = line_index(z, False)
    if xi is not None and xi % HALL != 0 and xwallblocks(xi, z):
        return True
    if ki is not None and ki % HALL != 0 and zwallblocks(ki, x):
        return True
    if xi is None and ki is None:
        i = cell_index(x, False)
        k = cell_index(z, True)
        return interior_partition(i, k, x, z)
    return False


def main():
    walk = set()
    walls = 0
    for x in range(-R, R + 1):
        for z in range(-R, R + 1):
            if is_wall(x, z):
                walls += 1
            else:
                walk.add((x, z))
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
    for i in range(-6, 7):
        cx = i * HALL * SPACING
        for z in range(-R, R + 1):
            blocked_center += 1 if is_wall(cx, z) else 0
            blocked_band += 1 if is_wall(cx - 1, z) or is_wall(cx + 1, z) else 0
        for x in range(-R, R + 1):
            blocked_center += 1 if is_wall(x, cx) else 0
            blocked_band += 1 if is_wall(x, cx - 1) or is_wall(x, cx + 1) else 0
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

    if PREVIEW:
        from PIL import Image
        scale = 3
        img = Image.new("RGB", ((2 * R + 1) * scale, (2 * R + 1) * scale), (196, 178, 84))
        px = img.load()
        for x in range(-R, R + 1):
            for z in range(-R, R + 1):
                if is_wall(x, z):
                    col = (116, 100, 34)
                elif in_band(x, True) or in_band(z, False):
                    col = (214, 200, 120)
                else:
                    continue
                for a in range(scale):
                    for b in range(scale):
                        px[(x + R) * scale + a, (z + R) * scale + b] = col
        out = "tools/layout_preview.png"
        img.save(out)
        print("wrote", out)

    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
