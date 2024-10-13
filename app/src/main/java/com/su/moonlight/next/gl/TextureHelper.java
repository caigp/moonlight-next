package com.su.moonlight.next.gl;

import android.opengl.GLES20;

public class TextureHelper {

    public static int loadTexture() {
        int[] textureObjectId = new int[1];
        GLES20.glGenTextures(1, textureObjectId, 0);
        return textureObjectId[0];
    }

}
