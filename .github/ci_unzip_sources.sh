#!/usr/bin/env bash
# Unzips every decompiled Minecraft sources jar Loom produced (the common jar
# and the client-only jar) into /tmp/mcsrc for the annotation dumper.
set -u
rm -rf /tmp/mcsrc
mkdir -p /tmp/mcsrc
find "$HOME/.gradle" -name "*sources*.jar" 2>/dev/null | while read -r j; do
  if unzip -l "$j" 2>/dev/null | grep -qE "FlatLevelSource.java|DimensionSpecialEffects.java|HierarchicalModel.java|ChunkGenerator.java|BlockEntityType.java"; then
    echo "extracting $(basename "$j")"
    unzip -qo "$j" -d /tmp/mcsrc
  fi
done
echo "java files: $(find /tmp/mcsrc -name '*.java' | wc -l)"
