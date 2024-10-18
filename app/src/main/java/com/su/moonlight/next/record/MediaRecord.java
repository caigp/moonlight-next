package com.su.moonlight.next.record;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.EGLContext;
import android.util.Log;

import com.su.moonlight.next.record.audio.AudioEncoder;
import com.su.moonlight.next.record.video.VideoEncoder;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class MediaRecord implements IMediaRecord, Runnable {
    private static final String TAG = "MediaRecord";
    private MediaMuxer muxer;

    private AudioEncoder audioEncoder;
    private VideoEncoder videoEncoder;
    public static final int FLAG_AUDIO = 0X01;
    public static final int FLAG_VIDEO = 0X10;

    public static final int CONFIG = 0X11;
    private int currentConfig = 0;

    private int audioTrackIndex;
    private int videoTrackIndex;
    private File file;
    private Thread muxerThread;

    private final ArrayBlockingQueue<Sample> sampleQueue = new ArrayBlockingQueue<>(1024);

    private static final AtomicBoolean isRunning = new AtomicBoolean(false);

    public MediaRecord() {
    }

    public void start(Context context, int width, int height, EGLContext eglContext, int texture) {
        Log.d(TAG, "开始录像...");
        if (muxer == null) {
            file = new File(context.getExternalCacheDir(), "record_" + System.currentTimeMillis());
            try {
                muxer = new MediaMuxer(file.getPath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if ((CONFIG & FLAG_AUDIO) == FLAG_AUDIO) {

            if (audioEncoder == null) {
                audioEncoder = new AudioEncoder(this);
                audioEncoder.configure();
                audioEncoder.start();
            }
        }

        if ((CONFIG & FLAG_VIDEO) == FLAG_VIDEO) {
            if (videoEncoder == null) {
                videoEncoder = new VideoEncoder(context, this);
                videoEncoder.configure(width, height);
                videoEncoder.start(eglContext, texture);
            }
        }

        isRunning.set(true);
    }

    @Override
    public void addTrack(MediaFormat format, int flag) {
        currentConfig = currentConfig | flag;
        Log.i(TAG, "addTrack: 0x" + Integer.toHexString(flag));
        if (flag == FLAG_AUDIO) {
            audioTrackIndex = muxer.addTrack(format);
        } else if (flag == FLAG_VIDEO) {
            videoTrackIndex = muxer.addTrack(format);
        }
        if (currentConfig == CONFIG) {
            muxer.start();
            muxerThread = new Thread(this);
            muxerThread.start();
        }
    }

    @Override
    public void writeSample(int flag, ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {
        Log.d(TAG, "writeSample: " + String.format("%02x", flag));
        bufferInfo.presentationTimeUs = getPTSUs();
        sampleQueue.offer(new Sample(flag, byteBuf, bufferInfo));
    }

    public boolean stop() {
        isRunning.set(false);
        currentConfig = 0;
        boolean ret = false;

        if (audioEncoder != null) {
            audioEncoder.stop();
            audioEncoder = null;
        }
        if (videoEncoder != null) {
            videoEncoder.stop();
            videoEncoder = null;
        }

        sampleQueue.clear();
        if (muxerThread != null) {
            muxerThread.interrupt();
            try {
                muxerThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            muxerThread = null;
        }
        if (muxer != null) {
            try {
                muxer.release();
                ret = true;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                muxer = null;
            }
        }
        return ret;
    }

    protected long getPTSUs() {
        return System.nanoTime() / 1000L;
    }

    public void writeAudio(short[] ss) {
        if (isRunning.get()) {
            if (audioEncoder != null) {
                audioEncoder.write(ss);
            }
        }
    }

    public void updateVideo() {
        if (isRunning.get()) {
            if (videoEncoder != null) {
                videoEncoder.draw();
            }
        }
    }

    public File getFile() {
        return file;
    }

    public static MediaCodecInfo getEncoderInfo(String mimeType) {
        MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        MediaCodecInfo[] codecInfos = codecList.getCodecInfos();

        for (MediaCodecInfo codecInfo : codecInfos) {
            if (!codecInfo.isEncoder()) {
                continue; // 只关心编码器
            }

            String codecName = codecInfo.getName();
            Log.i(TAG, "Encoder: " + codecName);

            if (!codecName.startsWith("OMX")) {
                continue;
            }

            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    public static boolean getIsRunning() {
        return isRunning.get();
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {

            Sample sample = null;
            try {
                sample = sampleQueue.take();
            } catch (InterruptedException e) {
                break;
            }

            try {
                muxer.writeSampleData(sample.getFlag() == FLAG_AUDIO ? audioTrackIndex : videoTrackIndex, sample.getByteBuf(), sample.getBufferInfo());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "muxer stop...");
    }
}
