package com.su.moonlight.next.record.video;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.Surface;

import com.su.moonlight.next.gl.Drawer;
import com.su.moonlight.next.gl.EglHelper;
import com.su.moonlight.next.record.IMediaRecord;
import com.su.moonlight.next.record.MediaRecord;

import java.io.IOException;
import java.nio.ByteBuffer;


public class VideoEncoder implements Runnable {

    private final int INIT_EGL = 0;

    private final int DRAW = 1;

    private final int DESTROY_EGL = 2;

    private EGLContext eglContext;

    private Surface surface;

    private HandlerThread handlerThread;

    private Handler handler;

    private EglHelper eglHelper;

    private MediaCodec mediaCodec;

    private boolean isStart = false;

    private Drawer drawer;

    private IMediaRecord mediaRecord;

    private int width, height;

    private final Handler.Callback callback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case INIT_EGL:
                    eglHelper.initEgl(surface, eglContext);
                    drawer.create();
                    break;
                case DRAW:
                    drawer.glDraw();
                    eglHelper.swapBuffers();
                    break;
                case DESTROY_EGL:
                    drawer.destroy();
                    eglHelper.destroyEgl();
                    eglContext = EGL14.EGL_NO_CONTEXT;
                    break;
            }
            return false;
        }
    };

    public VideoEncoder(Context context, EGLContext eglContext, IMediaRecord mediaRecord, int texture, int width, int height) {
        this.eglContext = eglContext;
        this.mediaRecord = mediaRecord;
        drawer = new Drawer(context);
        drawer.setTex_id(texture, true);
        this.width = width;
        this.height = height;
    }

    public void start() throws IOException {
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc");

            MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 10000000);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            surface = mediaCodec.createInputSurface();

            mediaCodec.start();
        } catch (IOException e) {
            throw new IOException(e);
        }

        eglHelper = new EglHelper();
        handlerThread = new HandlerThread("VideoEncoderThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper(), callback);
        handler.sendEmptyMessage(INIT_EGL);
        isStart = true;

        new Thread(this).start();
    }

    public void draw() {
        if (isStart) {
            handler.sendEmptyMessage(DRAW);
        }
    }

    public void stop() {
        if (isStart) {
            handler.sendEmptyMessage(DESTROY_EGL);
            handlerThread.quitSafely();
            isStart = false;
        }
    }

    public Drawer getDrawer() {
        return drawer;
    }

    @Override
    public void run() {
        try {

            while (isStart) {
                ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();

                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000);
                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat outputFormat = mediaCodec.getOutputFormat();
                    mediaRecord.addTrack(MediaRecord.FLAG_VIDEO, outputFormat);
                }
                while (outputBufferIndex >= 0) {
                    ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                    mediaRecord.writeSample(MediaRecord.FLAG_VIDEO, outputBuffer, bufferInfo);

                    mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                    outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            mediaCodec.stop();
            mediaCodec.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
