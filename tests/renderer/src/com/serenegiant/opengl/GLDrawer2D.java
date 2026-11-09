package com.serenegiant.opengl;

// Captures delegation only. It does not load shaders or render pixels.
public final class GLDrawer2D {
    public int draws, textureId;
    public float[] mvp, texture;
    public void setMvpMatrix(float[] matrix, int offset) {
        mvp = new float[16];
        System.arraycopy(matrix, offset, mvp, 0, 16);
    }
    public void draw(int id, float[] matrix, int offset) {
        draws++;
        textureId = id;
        texture = matrix;
    }
}
