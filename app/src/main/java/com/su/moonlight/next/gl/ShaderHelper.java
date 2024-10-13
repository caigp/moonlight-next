package com.su.moonlight.next.gl;

import android.opengl.GLES20;
import android.util.Log;

public class ShaderHelper {
    private static final String TAG = "ShaderHelper";

    private static int compileShader(int type, String source) {
        int shaderObjectId = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shaderObjectId, source);
        GLES20.glCompileShader(shaderObjectId);
        int[] params = new int[1];
        GLES20.glGetShaderiv(shaderObjectId, GLES20.GL_COMPILE_STATUS, params, 0);
        Log.d(TAG, "compileShader: " + GLES20.glGetShaderInfoLog(shaderObjectId));
        return shaderObjectId;
    }

    public static int compileVertexShader(String shaderCode) {
        return compileShader(GLES20.GL_VERTEX_SHADER, shaderCode);
    }

    public static int compileFragmentShader(String shaderCode) {
        return compileShader(GLES20.GL_FRAGMENT_SHADER, shaderCode);
    }

    public static int linkProgram(int vertexShaderId, int fragmentShaderId) {
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShaderId);
        GLES20.glAttachShader(program, fragmentShaderId);
        GLES20.glLinkProgram(program);
        int[] params = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, params, 0);

        GLES20.glValidateProgram(program);

        int[] status = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_VALIDATE_STATUS, status, 0);
        Log.d(TAG, "validate shader program: " + GLES20.glGetProgramInfoLog(program));
        return program;
    }

    public static int buildProgram(String vertexShaderCode, String fragmentShaderCode) {
        int vertexShader = compileVertexShader(vertexShaderCode);
        int fragmentShader = compileFragmentShader(fragmentShaderCode);
        return linkProgram(vertexShader, fragmentShader);
    }
}
