#!/usr/bin/env bash
# Unzips every decompiled Minecraft sources jar Loom produced (common and
# client-only) into /tmp/mcsrc for the annotation dumper.
set -u
rm -rf /tmp/mcsrc
mkdir -p /tmp/mcsrc
echo "GRADLE_USER_HOME=${GRADLE_USER_HOME:-unset}"
echo "searching for decompiled sources jars..."
JARS=$(find /home/runner /root -name "*.jar" 2>/dev/null | grep -i sources | grep -i -E "minecraft|merged|named|common|client")
echo "$JARS" | sed 's/^/candidate: /'
echo "$JARS" | while read -r j; do
  [ -z "$j" ] && continue
  if unzip -l "$j" 2>/dev/null | grep -qE "FlatLevelSource.java|DimensionSpecialEffects.java|HierarchicalModel.java|ChunkGenerator.java|BlockEntityType.java|BlockEntity.java"; then
    echo "extracting $(basename "$j")"
    unzip -qo "$j" -d /tmp/mcsrc
  fi
done
echo "java files: $(find /tmp/mcsrc -name '*.java' | wc -l)"
for f in \
  net/minecraft/world/level/chunk/ChunkGenerator.java \
  net/minecraft/world/level/levelgen/FlatLevelSource.java \
  net/minecraft/world/level/block/EntityBlock.java \
  net/minecraft/world/level/block/entity/BlockEntity.java \
  net/minecraft/client/renderer/DimensionSpecialEffects.java; do
  [ -f "/tmp/mcsrc/$f" ] && echo "OK  $f" || echo "ABSENT $f"
done
