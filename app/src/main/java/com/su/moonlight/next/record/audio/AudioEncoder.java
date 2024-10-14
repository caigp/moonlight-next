package com.su.moonlight.next.record.audio;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import com.su.moonlight.next.record.IMediaRecord;
import com.su.moonlight.next.record.MediaRecord;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;


public class AudioEncoder implements Runnable {

    private MediaCodec mediaCodec;

    private AtomicBoolean isStart = new AtomicBoolean(false);

    // 采样率 44.1kHz，所有设备都支持
    private final static int SAMPLE_RATE = 44100;

    // 通道数
    private static final int CHANNEL_COUNT = 2;
    // 比特率
    private static final int BIT_RATE = 128000;

    private IMediaRecord mediaRecord;

    private boolean hasAudio = false;

    public AudioEncoder(IMediaRecord mediaRecord) {
        this.mediaRecord = mediaRecord;
    }

    public void start() {
        mediaCodec.start();

        isStart.set(true);
        new Thread(this).start();
    }

    /**
     * short转byte
     * @param data
     * @return
     */
    public static byte[] shortToByte(short[] data) {
        byte[] byteValue = new byte[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            byteValue[i * 2] = (byte) (data[i] & 0xff);
            byteValue[i * 2 + 1] = (byte) ((data[i] & 0xff00) >> 8);
        }
        return byteValue;
    }

    public void write(short[] ss) {
        if (isStart.get()) {
            try {
                int dequeueInputBuffer = mediaCodec.dequeueInputBuffer(100000);
                if (dequeueInputBuffer >= 0) {
                    ByteBuffer inputBuffer = mediaCodec.getInputBuffer(dequeueInputBuffer);
                    inputBuffer.put(shortToByte(ss));

                    mediaCodec.queueInputBuffer(dequeueInputBuffer, 0, inputBuffer.position(), 0, MediaCodec.BUFFER_FLAG_KEY_FRAME);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        isStart.set(false);
    }

    @Override
    public void run() {
        try {
            while (isStart.get()) {

                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000);
                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat outputFormat = mediaCodec.getOutputFormat();
                    mediaRecord.addTrack(outputFormat, MediaRecord.FLAG_AUDIO);
                } else if (outputBufferIndex >= 0) {
                    ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
                    mediaRecord.writeSample(MediaRecord.FLAG_AUDIO, outputBuffer, bufferInfo);
                    if (bufferInfo.flags == 0) {
                        hasAudio = true;
                    }
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

    public boolean isHasAudio() {
        return hasAudio;
    }

    public void configure() {
        try {
            MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, CHANNEL_COUNT);
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);

            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
