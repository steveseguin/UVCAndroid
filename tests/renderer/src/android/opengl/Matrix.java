package android.opengl;

// Only the identity operation required by RendererSurface is represented.
public final class Matrix {
    public static void setIdentityM(float[] matrix, int offset) {
        for (int i = 0; i < 16; i++) matrix[offset + i] = (i % 5 == 0) ? 1 : 0;
    }
}
