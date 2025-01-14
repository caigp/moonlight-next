package com.su.moonlight.next.gl;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.view.Surface;


public class EglHelper {

    private EGLSurface eglSurface = EGL14.EGL_NO_SURFACE;
    private EGLContext eglCtx = EGL14.EGL_NO_CONTEXT;
    private EGLDisplay eglDis = EGL14.EGL_NO_DISPLAY;

    private int width, height;

    public void initEgl(Surface surface, EGLContext eglContext) {
        eglDis = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);

        int[] version = new int[2];
        if (!EGL14.eglInitialize(eglDis, version, 0, version, 1)) {
            throw new RuntimeException("eglInitialize failed");
        }
        int confAttr[] = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_SURFACE_TYPE,
                EGL14.EGL_WINDOW_BIT,
                EGL14.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL14.eglChooseConfig(eglDis, confAttr, 0, configs, 0, 1, numConfigs, 0)) {
            throw new IllegalArgumentException("eglChooseConfig failed");
        }
        int ctxAttr[] = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,// 0x3098
                EGL14.EGL_NONE
        };
        if (eglContext == null) {
            eglCtx = EGL14.eglCreateContext(eglDis, configs[0], EGL14.EGL_NO_CONTEXT, ctxAttr, 0);
        } else {
            eglCtx = EGL14.eglCreateContext(eglDis, configs[0], eglContext, ctxAttr, 0);
        }

        int[] surfaceAttr = {
                EGL14.EGL_NONE
        };
        eglSurface = EGL14.eglCreateWindowSurface(eglDis, configs[0], surface, surfaceAttr, 0);
        EGL14.eglMakeCurrent(eglDis, eglSurface, eglSurface, eglCtx);

        int[] value = new int[1];
        EGL14.eglQuerySurface(eglDis, eglSurface, EGL14.EGL_WIDTH, value, 0);
        width = value[0];
        value[0] = 0;
        EGL14.eglQuerySurface(eglDis, eglSurface, EGL14.EGL_HEIGHT, value, 0);
        height = value[0];
    }

    //9.刷新数据，显示渲染场景
    public boolean swapBuffers() {
        return EGL14.eglSwapBuffers(eglDis, eglSurface);
    }

    //回收数据
    public void destroyEgl(){

        if (eglSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglMakeCurrent(eglDis, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroySurface(eglDis, eglSurface);
            eglSurface = EGL14.EGL_NO_SURFACE;
        }
        if (eglCtx != EGL14.EGL_NO_CONTEXT) {
            EGL14.eglDestroyContext(eglDis, eglCtx);
            eglCtx = EGL14.EGL_NO_CONTEXT;
        }
        if (eglDis != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglTerminate(eglDis);
            eglDis = EGL14.EGL_NO_DISPLAY;
        }
    }

    public EGLContext getEglCtx() {
        return eglCtx;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
