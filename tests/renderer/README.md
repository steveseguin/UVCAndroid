# RendererSurface frame-limit regression

Run from the repository root with a full JDK 11 or later (no Android SDK or external dependencies):

```
java tests/renderer/RunTests.java .
```

To test another exact `RendererSurface.java`, supply its path as the second argument:

```
java tests/renderer/RunTests.java . /path/to/original/RendererSurface.java
```

The runner compiles the production source together with isolated test-only stand-ins,
using Java 8 source/bytecode levels, runs the regression cases, and removes its temporary
class directory. This does not validate the Java 8 API surface or Android API compatibility.
The stand-ins are only on this standalone test classpath, not in the Android source set.
The deterministic clock tests overload dispatch and frame admission without sleeping.
EGL and GL drawer spies check forwarding and release bookkeeping but do not exercise
Android, a camera, a graphics context, rendering, real-time scheduling, or driver behavior.
This is not a substitute for the Android/NDK build and device integration tests.

The four-argument draw used by `RendererHolder.onDrawSlaveSurface` must advance the
same deadline as the three-argument overload. At a requested 10 FPS cap, 60 incoming
frames spaced 16,666,667 ns apart should yield 10 draw calls, not 55. Controls cover
both overloads, shared and independent deadlines, unlimited and disabled surfaces,
matrix forwarding, and release. Existing initial delay and strict deadline comparison
are intentionally preserved.
