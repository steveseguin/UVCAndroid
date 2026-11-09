package com.serenegiant.opengl.renderer;

import com.serenegiant.opengl.EGLBase;
import com.serenegiant.opengl.GLDrawer2D;
import com.serenegiant.utils.Time;
import java.util.Arrays;

public final class RendererSurfaceTest {
    private static int passed, failed;
    private static final long INTERVAL = 100000000L;
    private interface Case { void run(); }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static void test(String name, Case action) {
        try { action.run(); passed++; System.out.println("PASS " + name); }
        catch (Throwable e) { failed++; System.out.println("FAIL " + name + ": " + e); }
    }
    private static final class Fixture {
        final EGLBase egl = new EGLBase();
        final GLDrawer2D drawer = new GLDrawer2D();
        final RendererSurface renderer;
        final float[] texture = new float[16];
        final float[] mvp = new float[16];
        Fixture(int fps) {
            Time.now = 0;
            renderer = RendererSurface.newInstance(egl, new Object(), fps);
            for (int i = 0; i < 16; i++) mvp[i] = i + 1;
        }
        void draw(boolean explicitMatrix) {
            if (explicitMatrix) renderer.draw(drawer, 42, texture, mvp);
            else renderer.draw(drawer, 42, texture);
        }
    }
    private static void cadence(boolean explicitMatrix) {
        Fixture f = new Fixture(10);
        check(!f.renderer.canDraw(), "initial interval must be respected");
        Time.now = INTERVAL;
        check(!f.renderer.canDraw(), "existing strict deadline comparison is preserved");
        Time.now++;
        check(f.renderer.canDraw(), "first deadline passed");
        f.draw(explicitMatrix);
        check(!f.renderer.canDraw(), "draw must advance next deadline");
        Time.now += INTERVAL;
        check(!f.renderer.canDraw(), "exact next deadline remains gated");
        Time.now++;
        check(f.renderer.canDraw(), "second deadline passed");
        f.draw(explicitMatrix);
        check(!f.renderer.canDraw(), "second draw must also advance deadline");
        check(f.drawer.draws == 2 && f.egl.surface.swaps == 2, "draw/swap delegation");
        check(f.drawer.textureId == 42 && f.drawer.texture == f.texture, "texture forwarding");
        check(Arrays.equals(f.drawer.mvp, explicitMatrix ? f.mvp : f.renderer.mMvpMatrix), "MVP forwarding");
    }
    private static void sixtyInputFrames(boolean explicitMatrix) {
        Fixture f = new Fixture(10);
        for (int i = 1; i <= 60; i++) {
            Time.now = i * 16666667L;
            if (f.renderer.canDraw()) f.draw(explicitMatrix);
        }
        check(f.drawer.draws == 10, "60 incoming frames at ~60 FPS with 10 FPS cap: got " + f.drawer.draws);
    }
    public static void main(String[] args) {
        test("four-argument cadence and forwarding", () -> cadence(true));
        test("three-argument cadence and forwarding", () -> cadence(false));
        test("four-argument 60-to-10 FPS cap", () -> sixtyInputFrames(true));
        test("three-argument 60-to-10 FPS cap", () -> sixtyInputFrames(false));
        test("mixed overloads share a deadline", () -> {
            Fixture f = new Fixture(10);
            Time.now = INTERVAL + 1;
            f.draw(false);
            Time.now += INTERVAL + 1;
            check(f.renderer.canDraw(), "three-argument deadline expired");
            f.draw(true);
            check(!f.renderer.canDraw(), "four-argument draw resets shared deadline");
            Time.now += INTERVAL + 1;
            check(f.renderer.canDraw(), "four-argument deadline expired");
            f.draw(false);
            check(!f.renderer.canDraw(), "three-argument draw resets shared deadline");
        });
        for (final int fps : new int[] {0, -1}) {
            test("unlimited FPS " + fps, () -> {
                Fixture f = new Fixture(fps);
                for (int i = 0; i < 5; i++) {
                    check(f.renderer.canDraw(), "unlimited surface must remain ready");
                    f.draw((i % 2) == 0);
                }
                check(f.drawer.draws == 5 && f.egl.surface.swaps == 5, "all unlimited frames delegated");
            });
        }
        test("disable/re-enable retains limiter", () -> {
            Fixture f = new Fixture(10);
            Time.now = INTERVAL + 1;
            f.renderer.setEnable(false);
            check(!f.renderer.canDraw() && !f.renderer.isEnable(), "disabled is gated");
            f.renderer.setEnable(true);
            check(f.renderer.canDraw(), "re-enabled after deadline");
            f.draw(true);
            f.renderer.setEnable(false);
            f.renderer.setEnable(true);
            check(!f.renderer.canDraw(), "toggle does not bypass fresh deadline");
        });
        test("per-surface deadlines remain independent", () -> {
            Fixture a = new Fixture(10), b = new Fixture(10);
            Time.now = INTERVAL + 1;
            a.draw(true);
            check(!a.renderer.canDraw() && b.renderer.canDraw(), "only drawn surface is gated");
        });
        test("release is idempotent and prevents EGL delegation", () -> {
            Fixture f = new Fixture(10);
            check(f.renderer.isValid(), "new surface valid");
            f.renderer.release(); f.renderer.release();
            check(!f.renderer.isValid() && f.egl.surface.releases == 1, "release once");
            Time.now = INTERVAL + 1;
            f.draw(true); f.draw(false); f.renderer.clear(0);
            check(f.drawer.draws == 0 && f.egl.surface.swaps == 0, "released target is not used");
        });
        System.out.println("RESULT passed=" + passed + " failed=" + failed);
        if (failed != 0) throw new AssertionError(failed + " regression case(s) failed");
    }
}
