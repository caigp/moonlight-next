package com.su.moonlight.next.record;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.EGLContext;
import android.util.Log;

import com.su.moonlight.next.record.audio.AudioEncoder;
import com.su.moonlight.next.record.video.VideoEncoder;
import com.su.moonlight.next.utils.MediaUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

public class MediaRecord implements IMediaRecord, Runnable {
    private static final String TAG = "MediaRecord";
    private MediaMuxer muxer;

    private AudioEncoder audioEncoder;
    private VideoEncoder videoEncoder;

    private Context context;
    public static final int FLAG_AUDIO = 0X01;
    public static final int FLAG_VIDEO = 0X10;

    public static final int CONFIG = 0X11;
    private int currentConfig = 0;

    private int audioTrackIndex;
    private int videoTrackIndex;
    private File file;
    private Thread muxerThread;

    private ArrayBlockingQueue<Sample> sampleQueue = new ArrayBlockingQueue<>(1024);

    public MediaRecord(Context context) {
        this.context = context;
    }

    public void start(int width, int height, EGLContext eglContext, int texture) throws Exception {
        if (muxer == null) {
            file = new File(context.getExternalCacheDir(), System.currentTimeMillis() + ".mp4");
            try {
                muxer = new MediaMuxer(file.getPath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            } catch (IOException e) {
                throw new IOException(e);
            }
        }

        if (audioEncoder == null) {
            audioEncoder = new AudioEncoder(this);
            audioEncoder.configure();
            audioEncoder.start();
        }
        if (videoEncoder == null) {
            videoEncoder = new VideoEncoder(context, this);
            videoEncoder.configure(width, height);
            videoEncoder.start(eglContext, texture);
        }
    }

    @Override
    public void addTrack(MediaFormat format, int flag) {
        currentConfig = currentConfig | flag;
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
        bufferInfo.presentationTimeUs = getPTSUs();
        sampleQueue.add(new Sample(flag, byteBuf, bufferInfo));
    }

    public String stop() throws Exception {
        String path = null;
        boolean hasAudio = audioEncoder.isHasAudio();

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
            muxerThread = null;
        }
        if (muxer != null) {
            checkNeedAudio(hasAudio);
            try {
                muxer.release();
                path = MediaUtils.INSTANCE.insertVideo(context, file, "record_" + System.currentTimeMillis(), "record");
            } catch (Exception e) {
                throw e;
            } finally {
                muxer = null;
                file.delete();
            }
        }

        return path;
    }

    /**
     * 这里很重要，如果添加了音频轨道却没有任何音频输入会无法正常结束
     */
    private void checkNeedAudio(boolean hasAudio) {
        if ((CONFIG & FLAG_AUDIO) == FLAG_AUDIO) {
            if (!hasAudio) {
                //随便写点东西
                ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[1]);
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                bufferInfo.size = 1;
                bufferInfo.presentationTimeUs = getPTSUs();
                try {
                    muxer.writeSampleData(audioTrackIndex, byteBuffer, bufferInfo);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected long getPTSUs() {
        return System.nanoTime() / 1000L;
    }

    public void writeAudio(short[] ss) {
        if (audioEncoder != null) {
            audioEncoder.write(ss);
        }
    }

    public void updateVideo() {
        if (videoEncoder != null) {
            videoEncoder.draw();
        }
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            Sample sample;
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
