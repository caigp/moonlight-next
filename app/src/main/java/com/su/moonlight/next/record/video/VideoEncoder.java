package com.su.moonlight.next.record.video;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
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
import java.util.concurrent.atomic.AtomicBoolean;


public class VideoEncoder implements Runnable {

    private final int INIT_EGL = 0;

    private final int DRAW = 1;

    private final int DESTROY_EGL = 2;

    private Surface surface;

    private HandlerThread handlerThread;

    private Handler handler;

    private EglHelper eglHelper;

    private MediaCodec mediaCodec;

    private AtomicBoolean isStart = new AtomicBoolean(false);

    private Drawer drawer;

    private IMediaRecord mediaRecord;

    private Context context;

    private final Handler.Callback callback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case INIT_EGL:
                    eglHelper = new EglHelper();
                    eglHelper.initEgl(surface, (EGLContext) msg.obj);
                    drawer = new Drawer(context);
                    drawer.create();
                    drawer.setTex_id(msg.arg1, true);
                    break;
                case DRAW:
                    drawer.glDraw();
                    eglHelper.swapBuffers();
                    break;
                case DESTROY_EGL:
                    drawer.destroy();
                    eglHelper.destroyEgl();
                    break;
            }
            return false;
        }
    };

    public VideoEncoder(Context context, IMediaRecord mediaRecord) {
        this.context = context;
        this.mediaRecord = mediaRecord;
    }

    public void start(EGLContext eglContext , int texture) {
        surface = mediaCodec.createInputSurface();
        mediaCodec.start();

        handlerThread = new HandlerThread("VideoEncoderThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper(), callback);
        handler.obtainMessage(INIT_EGL, texture, 0, eglContext).sendToTarget();
        new Thread(this).start();

        isStart.set(true);
    }

    public void draw() {
        if (isStart.get()) {
            handler.sendEmptyMessage(DRAW);
        }
    }

    public void stop() {
        if (isStart.get()) {
            handler.sendEmptyMessage(DESTROY_EGL);
            handlerThread.quitSafely();
            isStart.set(false);
        }
    }

    @Override
    public void run() {
        try {
            while (isStart.get()) {

                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000);
                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat outputFormat = mediaCodec.getOutputFormat();
                    mediaRecord.addTrack(outputFormat, MediaRecord.FLAG_VIDEO);
                } else if (outputBufferIndex >= 0) {
                    ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
                    mediaRecord.writeSample(MediaRecord.FLAG_VIDEO, outputBuffer, bufferInfo);

                    mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
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

    public void configure(int width, int height) {
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc");

            MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 10000000);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
