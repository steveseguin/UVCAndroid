# Native frame-conversion regression

Run from the repository root on a host with a GNU-compatible C compiler:

```sh
sh tests/native/run.sh
```

For AddressSanitizer and UndefinedBehaviorSanitizer:

```sh
SANITIZE=1 sh tests/native/run.sh
```

The runner builds the complete production `frame.c` and uses the real public
`uvc_frame_t` header. Test-only include shims replace Android/JNI logging and
libusb implementation headers; JPEG decoding is excluded. No conversion
algorithm is replaced. Both `USE_STRIDE=0` and `USE_STRIDE=1` are checked.

Eighty cases per build combine widths 8, 16, 32, and 640; heights 1, 2, 3,
and 480; and five chroma patterns. The independent scalar oracle uses the
converter's existing full-range fixed-point coefficients with floor rounding.
YUYV-to-RGB and UYVY-to-BGR are controls. Tests also check frame properties,
unchanged input, incorrect-format rejection, and insufficient output capacity.
Uniform-chroma controls intentionally coexist with varying-chroma cases so
reusing the first pair's U/V cannot pass unnoticed.

These are host-only packed-frame tests. They do not validate Android builds,
USB capture, JNI callbacks, JPEG decoding, padded strides, non-multiple-of-eight
tails, or device rendering. Some traced/container runtimes cannot run
LeakSanitizer; in that case use `ASAN_OPTIONS=detect_leaks=0` and record that
leak checking was not run. `CC` selects a compiler, and `FRAME_SOURCE` can select
an unmodified source file for a before/after regression check.
