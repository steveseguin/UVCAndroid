package com.serenegiant.opengl;

// Records Java-level surface operations, not actual EGL behavior.
public final class EGLBase {
    public final TestSurface surface = new TestSurface();
    public IEglSurface createFromSurface(Object ignored) { return surface; }
    public interface IEglSurface {
        void release();
        boolean isValid();
        void makeCurrent();
        void swap();
    }
    public static final class TestSurface implements IEglSurface {
        public int releases, currents, swaps;
        public boolean valid = true;
        public void release() { releases++; valid = false; }
        public boolean isValid() { return valid; }
        public void makeCurrent() { currents++; }
        public void swap() { swaps++; }
    }
}
