package com.su.moonlight.next.record;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaScannerConnection;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.os.Environment;

import com.su.moonlight.next.record.audio.AudioEncoder;
import com.su.moonlight.next.record.video.VideoEncoder;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class MediaRecord implements IMediaRecord {

    private MediaMuxer muxer;

    private AtomicBoolean isStarted = new AtomicBoolean(false);

    private AudioEncoder audioEncoder;
    private VideoEncoder videoEncoder;

    private Context context;
    private EGLContext eglContext;
    private int texture;
    public static final int FLAG_AUDIO = 0X01;
    public static final int FLAG_VIDEO = 0X10;

    public static final int CONFIG = 0X11;

    private int currentConfig;

    private int audioTrackIndex;
    private int videoTrackIndex;
    private File file;

    public MediaRecord(Context context, EGLContext eglContext, int texture) {
        this.context = context;
        this.eglContext = eglContext;
        this.texture = texture;
    }

    public void start() throws Exception {
        if (muxer == null) {
            file = new File(createRecordPath());
            try {
                muxer = new MediaMuxer(file.getPath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            } catch (IOException e) {
                throw new IOException(e);
            }
        }
        if (audioEncoder == null) {
            audioEncoder = new AudioEncoder(this);
            audioEncoder.start();
        }
        if (videoEncoder == null) {
            videoEncoder = new VideoEncoder(context, eglContext, this, texture);
            videoEncoder.start();
        }
    }

    @Override
    public void writeSample(int flag, ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {
        if (muxer == null) {
            return;
        }
        if (!isStarted.get()) {
            return;
        }
        bufferInfo.presentationTimeUs = getPTSUs();
        if (flag == FLAG_AUDIO) {
            muxer.writeSampleData(audioTrackIndex, byteBuf, bufferInfo);
        } else if (flag == FLAG_VIDEO) {
            muxer.writeSampleData(videoTrackIndex, byteBuf, bufferInfo);
        }
    }

    @Override
    public void addTrack(int flag, MediaFormat format) {
        if (muxer == null) {
            return;
        }
        int track = muxer.addTrack(format);
        if (flag == FLAG_AUDIO) {
            audioTrackIndex = track;
        } else if (flag == FLAG_VIDEO) {
            videoTrackIndex = track;
        }
        currentConfig |= flag;
        if (currentConfig == CONFIG) {
            muxer.start();
            isStarted.set(true);
        }
    }

    public String stop() {
        String path = null;
        if (muxer != null) {
            if (isStarted.get()) {
                isStarted.set(false);
                try {
                    checkNeedAudio();
                    muxer.release();

                    MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, new String[]{"video/*"}, null);
                    path = file.getAbsolutePath();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            muxer = null;
        }
        eglContext = EGL14.EGL_NO_CONTEXT;

        if (audioEncoder != null) {
            audioEncoder.stop();
            audioEncoder = null;
        }
        if (videoEncoder != null) {
            videoEncoder.stop();
            videoEncoder = null;
        }
        return path;
    }

    /**
     * 这里很重要，如果添加了音频轨道却没有任何音频输入会无法正常结束
     */
    private void checkNeedAudio() {
        if ((CONFIG & FLAG_AUDIO) == FLAG_AUDIO) {
            if (!audioEncoder.isHasAudio()) {
                //随便写点东西
                ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[1]);
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                bufferInfo.size = 1;
                bufferInfo.presentationTimeUs = getPTSUs();
                muxer.writeSampleData(audioTrackIndex, byteBuffer, bufferInfo);
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

    private String createRecordPath() {
        File sdDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        if (sdDir != null) {
            File dir = new File(sdDir.getAbsolutePath());//新建子目录
            if (!dir.exists()) {
                dir.mkdirs();
            }
            //视频文件的路径
            String path = dir.getAbsolutePath() + "/" + System.currentTimeMillis() + ".mp4";
            return path;
        }
        return null;
    }
}
