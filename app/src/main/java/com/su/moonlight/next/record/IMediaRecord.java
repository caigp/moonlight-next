package com.su.moonlight.next.record;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

public interface IMediaRecord {
    void writeSample(int flag, ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo);
    void addTrack(MediaFormat format, int flag);
}
