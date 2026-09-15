#!/usr/bin/env python3
"""3D validation of PoolroomsChunkGenerator.columnState (v1.0.5).

Mirrors the Java column builder and verifies, over many cells/seed:
  * maze interior columns are sealed at/above y70 (no air path to the sky);
  * great-hall interiors have air y65..85 and GLASS at y86;
  * tall wall planes (incl. arch openings above their lintel) are sealed;
  * doorways/arches still leave a passable 2-wide air gap at foot level;
  * every maze sub-room is reachable in 3D walk (2-block headroom) and
    great halls are reachable through perimeter arches.
"""
import collections
import sys

sys.path.insert(0, ".")
from sim_pool105 import Layout, SPACING  # noqa: E402

BOTTOM, TOP = 58, 87
TILE, FOUND, LAMP, GLASS, WATER, AIR, PORTAL = range(7)


def column_state(L, x, z, y):
    wall = L.is_wall_column(x, z)
    tall = L.is_tall_wall(x, z)
    water = L.is_water(x, z)
    pillar = L.is_pillar(x, z)
    hall = (x % SPACING != 0 and z % SPACING != 0
            and L.is_great_hall(x // SPACING, z // SPACING))
    opening = L.is_perimeter_opening(x, z)
    if y <= 62:
        return FOUND
    if y == 63:
        return TILE if (wall or water) else FOUND
    if y == 64:
        if water:
            return WATER
        return TILE
    if tall:
        if y <= 76:
            return AIR if opening else TILE
        if y == 77:
            return TILE
        if 78 <= y <= 81:
            return GLASS
        return TILE
    if hall:
        if y == 86:
            return TILE if pillar else GLASS
        if pillar and y < 85:
            return TILE
        if pillar and y == 85:
            return LAMP
        lx, lz = x % SPACING, z % SPACING
        lantern = (lx in (4, 12, 20) and lz in (4, 12, 20))
        if lantern and y == 85:
            return LAMP
        return AIR
    # maze
    if wall or (pillar and y <= 69):
        return TILE
    if y <= 69:
        return AIR
    if y == 70:
        return LAMP if L.ceiling_lamp_at(x, z) else TILE
    if y == 71:
        return FOUND
    return AIR


# helper mirrors
def ceiling_lamp_at(self, x, z):
    if self.is_wall_column(x, z):
        return False
    lx, lz = x % SPACING, z % SPACING
    if lx == 0 or lz == 0:
        return False
    if self.is_great_hall(x // SPACING, z // SPACING):
        return False
    return x % 6 == 2 and z % 6 == 2


Layout.ceiling_lamp_at = ceiling_lamp_at


def is_solid(state):
    return state in (TILE, FOUND, LAMP, GLASS, WATER)


def walkable_feet(state):
    return state in (AIR, WATER)


def check_seed(seed):
    L = Layout(seed)
    cells = 6  # -6..5 cell range, fully contained
    x0, z0 = -cells * SPACING, -cells * SPACING
    W = cells * 2 * SPACING

    def get(x, z, y):
        return column_state(L, x, z, y)

    # 1) sky-seal. The shell only needs to be closed at its roof layer:
    #    maze columns solid at y70+y71; hall columns glass y86; wall planes
    #    closed above their openings (no air at y >= 77 for tall walls).
    seal_bad = []
    hall_bad = []
    hall_count = 0
    maze_count = 0
    for x in range(x0, x0 + W):
        for z in range(z0, z0 + W):
            lx, lz = x % SPACING, z % SPACING
            on_line = lx == 0 or lz == 0
            if not on_line and L.is_great_hall(x // SPACING, z // SPACING):
                hall_count += 1
                if not L.is_pillar(x, z) and get(x, z, 86) != GLASS:
                    hall_bad.append((x, z, "roof"))
                if not L.is_pillar(x, z):
                    for y in range(65, 86):
                        if get(x, z, y) not in (AIR, LAMP):
                            hall_bad.append((x, z, f"blocked@{y}"))
                            break
            elif on_line:
                if L.is_tall_wall(x, z):
                    for y in range(77, TOP + 1):
                        if get(x, z, y) == AIR:
                            seal_bad.append((x, z, y))
                            break
                else:
                    for y in (70, 71):
                        if get(x, z, y) == AIR:
                            seal_bad.append((x, z, y))
                            break
            else:
                # maze interior: tile/foundation roof pair
                maze_count += 1
                for y in (70, 71):
                    if get(x, z, y) == AIR:
                        seal_bad.append((x, z, y))
                        break

    # 2) 3D walk: feet y65 air/water, head y66 air; flood from a maze point.
    visited = set()
    start = None
    for x in range(2, SPACING):
        for z in range(2, SPACING):
            if (walkable_feet(get(x, z, 65)) and get(x, z, 66) == AIR
                    and get(x, z, 64) in (TILE, WATER)):
                start = (x, z)
                break
        if start:
            break
    q = collections.deque([start])
    visited.add(start)
    while q:
        x, z = q.popleft()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, nz = x + dx, z + dz
            if not (x0 <= nx < x0 + W and z0 <= nz < z0 + W):
                continue
            if (nx, nz) in visited:
                continue
            if walkable_feet(get(nx, nz, 65)) and get(nx, nz, 66) == AIR:
                visited.add((nx, nz))
                q.append((nx, nz))

    # every dry interior column of non-pool cells must be reached (margin)
    unreach = 0
    halls_reached = set()
    for x in range(x0 + 3, x0 + W - 3):
        for z in range(z0 + 3, z0 + W - 3):
            lx, lz = x % SPACING, z % SPACING
            if lx == 0 or lz == 0:
                continue
            cx, cz = x // SPACING, z // SPACING
            if get(x, z, 65) == AIR and get(x, z, 64) == TILE:
                if (x, z) not in visited:
                    unreach += 1
            if L.is_great_hall(cx, cz) and get(x, z, 65) == AIR and (x, z) in visited:
                halls_reached.add((cx, cz))
    total_halls = {(cx, cz)
                   for cx in range(-cells + 1, cells - 1)
                   for cz in range(-cells + 1, cells - 1)
                   if L.is_great_hall(cx, cz)}
    missed_halls = total_halls - halls_reached

    print(f"seed={seed} sealGaps={len(seal_bad)} hallBad={len(hall_bad)} "
          f"walkReached={len(visited)} dryUnreachable={unreach} "
          f"halls {len(halls_reached)}/{len(total_halls)} reached "
          f"(missed={sorted(missed_halls)[:3]})")
    if seal_bad[:3]:
        print("  seal examples:", seal_bad[:3])
    if hall_bad[:3]:
        print("  hall examples:", hall_bad[:3])
    return not seal_bad and not hall_bad and unreach == 0 and not missed_halls


def main():
    ok = True
    for s in (1, 42, 12345, -777, 555):
        ok &= check_seed(s)
    print("3D ALL OK" if ok else "3D FAILURES")
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
