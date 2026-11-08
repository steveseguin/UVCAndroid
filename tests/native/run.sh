#!/bin/sh
set -eu
root=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
build_dir=$(mktemp -d)
trap 'rm -rf "$build_dir"' EXIT HUP INT TERM
source=${FRAME_SOURCE:-"$root/libuvccamera/src/main/jni/libuvc/src/frame.c"}
flags=
if [ "${SANITIZE:-0}" = 1 ]; then
    flags='-fsanitize=address,undefined -fno-omit-frame-pointer'
fi
status=0
for stride in 0 1; do
    "${CC:-cc}" -std=gnu11 -O1 -g $flags -DUSE_STRIDE=$stride \
        -I"$root/tests/native/include" \
        -I"$root/libuvccamera/src/main/jni/libuvc/include" \
        "$source" "$root/tests/native/yuyv_bgr.c" -o "$build_dir/frame-$stride"
    "$build_dir/frame-$stride" || status=$?
done
exit "$status"
