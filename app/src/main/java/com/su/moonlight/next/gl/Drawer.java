package com.su.moonlight.next.gl;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.su.moonlight.next.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Drawer {

    private int program;

    private int aPosition;

    private int aCoordinate;

    private FloatBuffer vertexBuffer;

    private FloatBuffer textureBuffer;

    private Context context;

    private int tex_id;

    private boolean sharedTexture;

    public Drawer(Context context) {
        this.context = context;
    }

    public int getTex_id() {
        return tex_id;
    }

    public void create() {
        float[] vertex = {
                -1, -1,
                1,  1,
                -1,  1,
                1,  1,
                -1, -1,
                1, -1,
        };
        vertexBuffer = ByteBuffer.allocateDirect(vertex.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertex);
        vertexBuffer.position(0);

        float[] coordinate = {
                0,  1,
                1,  0,
                0,  0,
                1,  0,
                0,  1,
                1,  1
        };
        textureBuffer = ByteBuffer.allocateDirect(coordinate.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(coordinate);
        textureBuffer.position(0);

        GLES20.glClearColor(1, 0, 0, 1);

        program = ShaderHelper.buildProgram(TextResourceReader.readTextFileFromResource(context, R.raw.vertex_shader),
                TextResourceReader.readTextFileFromResource(context, R.raw.fragment_shader));

        aPosition = GLES20.glGetAttribLocation(program, "aPosition");
        aCoordinate = GLES20.glGetAttribLocation(program, "aCoordinate");
    }

    public void destroy() {
        GLES20.glDeleteProgram(program);
        if (!sharedTexture) {
            GLES20.glDeleteTextures(1, new int[]{tex_id}, 0);
        }
    }

    public void glDraw() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glUseProgram(program);

        GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(aPosition);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex_id);

        GLES20.glVertexAttribPointer(aCoordinate, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLES20.glEnableVertexAttribArray(aCoordinate);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

        GLES20.glDisableVertexAttribArray(aPosition);
        GLES20.glDisableVertexAttribArray(aCoordinate);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
    }

    public void setTex_id(int tex_id, boolean sharedTexture) {
        this.tex_id = tex_id;
        this.sharedTexture = sharedTexture;
    }

}
