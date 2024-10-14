package com.su.moonlight.next.record

import android.media.MediaCodec
import java.nio.ByteBuffer

data class Sample(val flag: Int, val byteBuf: ByteBuffer, val bufferInfo: MediaCodec.BufferInfo)
