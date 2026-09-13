#!/usr/bin/env bash
# Runs inside CI. Logs to ci-out/build.log, builds, decompiles and extracts
# Minecraft sources into /tmp/mcsrc for the annotation dumper.
set -u
cd "$GITHUB_WORKSPACE"
mkdir -p ci-out
LOG=ci-out/build.log
: > "$LOG"

run() {
  echo "===== STEP: $* =====" | tee -a "$LOG"
  "$@" >>"$LOG" 2>&1
  local rc=$?
  if [ $rc -ne 0 ]; then
    echo "STEP FAILED ($rc): $*" | tee -a "$LOG"
    return $rc
  fi
  return 0
}

chmod +x ./gradlew
run ./gradlew build --stacktrace --no-daemon --console=plain || BUILD_RC=$?

# List built jars for the record
echo "===== build/libs =====" | tee -a "$LOG"
ls -la build/libs/ >>"$LOG" 2>&1 || true

echo "BUILD_RC=${BUILD_RC:-0}" | tee -a "$LOG"
[ "${BUILD_RC:-0}" -eq 0 ] || exit "$BUILD_RC"
exit 0
