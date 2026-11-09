package android.opengl;

// Test-only no-op: no OpenGL context or graphics driver is exercised.
public final class GLES20 {
    public static final int GL_COLOR_BUFFER_BIT = 0x4000;
    public static void glClear(int mask) {}
    public static void glClearColor(float r, float g, float b, float a) {}
}
