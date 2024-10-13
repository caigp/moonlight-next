package com.su.moonlight.next.gl

import android.graphics.Bitmap
import android.graphics.Matrix
import android.opengl.GLES20
import java.nio.ByteBuffer

object GLUtils {

    fun readPixels(width: Int, height: Int, initWidth: Int, initHeight: Int): Bitmap {
        val pixels = ByteArray(width * height * 4)
        val byteBuffer = ByteBuffer.wrap(pixels)
        GLES20.glReadPixels(
            0,
            0,
            width,
            height,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            byteBuffer
        )
        val dest = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )
        dest.copyPixelsFromBuffer(byteBuffer)

        val m = Matrix()
        m.setScale(initWidth / width.toFloat(), -(initHeight / height.toFloat()), width.toFloat() / 2, height.toFloat() / 2)
        return Bitmap.createBitmap(dest, 0, 0, width, height, m, false)
    }
}