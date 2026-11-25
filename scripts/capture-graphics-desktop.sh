#!/usr/bin/env bash
# Capture real LWJGL frames from independent checkouts on the current X11 display.
set -euo pipefail
original=$(realpath "${1:?official v4.0.2 worktree}")
before=$(realpath "${2:?pre-graphics development worktree}")
out=$(realpath -m "${3:?output directory}")
repo=$(git rev-parse --show-toplevel)
: "${DISPLAY:?a running Cloud X11 display is required}"
test "$(git -C "$original" rev-parse HEAD)" = 57a4e06a4caf162446d1c28caa7983f0493fecf0
test "$(git -C "$before" rev-parse HEAD)" = 011364de12d732fa6eb63e426a3ecec5ea4283a1
mkdir -p "$out"
work=$(mktemp -d)
trap 'rm -rf -- "$work"' EXIT
export ALSOFT_DRIVERS=null LIBGL_ALWAYS_SOFTWARE=1
package=com.shatteredpixel.shatteredpixeldungeon.desktop
jar=desktop/build/libs/desktop-4.0.2.jar
(cd "$original" && bash gradlew :desktop:release --console=plain) >"$out/build-official.log" 2>&1
for tree in "$before" "$repo"; do
    (cd "$tree" && bash gradlew :desktop:compileTestJava :desktop:release --console=plain) >"$out/build-$(git -C "$tree" rev-parse --short HEAD).log" 2>&1
done
mkdir "$work/driver"
javac -cp "$original/$jar" -d "$work/driver" "$repo/desktop/src/test/java/com/shatteredpixel/shatteredpixeldungeon/desktop/UpstreamGraphicsReference.java"
capture() {
    local classes=$1 launcher=$2 folder=$3 mode=$4 lang=$5
    XDG_DATA_HOME=$(mktemp -d "$work/profile-XXXXXXXX")
    export XDG_DATA_HOME
    java -Duser.language="$lang" -cp "$classes" "$package.$launcher" "$out/$folder" "$mode" >"$out/$folder-$mode.log" 2>&1
}
for mode in sewer goo prison tengu-reference; do
    capture "$work/driver:$original/$jar" UpstreamGraphicsReference official "$mode" en
done
for mode in base hazards deep; do
    capture "$before/desktop/build/classes/java/test:$before/$jar" CoopRenderSmoke "before-$mode" "$mode" en
    for lang in en ko; do
        capture "$repo/desktop/build/classes/java/test:$repo/$jar" CoopRenderSmoke "coop-$lang-$mode" "$mode" "$lang"
    done
done
for mode in sewer goo prison tengu-reference; do
    capture "$repo/desktop/build/classes/java/test:$repo/$jar" UpstreamGraphicsReference singleplayer "$mode" en
done
