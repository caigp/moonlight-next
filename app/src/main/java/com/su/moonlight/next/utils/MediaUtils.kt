package com.su.moonlight.next.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.io.IOException

object MediaUtils {

    fun insertImage(
        context: Context,
        source: Bitmap,
        title: String,
        description: String?
    ): String? {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, title)
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "$title.jpg")
        values.put(MediaStore.Images.Media.DESCRIPTION, description)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(
                MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/MyAppImages"
            )
            val resolver = context.contentResolver
            val externalContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val uri = resolver.insert(externalContentUri, values)
            if (uri != null) {
                try {
                    val out = resolver.openOutputStream(uri)
                    source.compress(Bitmap.CompressFormat.JPEG, 100, out!!)
                } catch (e: IOException) {
                    e.printStackTrace()
                    return null
                } finally {

                }
            }
            uri.toString()
        } else {
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                try {
                    val out = resolver.openOutputStream(uri)
                    source.compress(Bitmap.CompressFormat.JPEG, 100, out!!)
                } catch (e: IOException) {
                    e.printStackTrace()
                    return null
                } finally {

                }
                // For backward compatibility, we also need to call the notify method
                context.contentResolver.notifyChange(uri, null)
            }
            uri.toString()
        }
    }

    fun insertVideo(
        context: Context,
        source: File,
        title: String,
        description: String?
    ): String? {
        val fis = FileInputStream(source)

        val values = ContentValues()
        values.put(MediaStore.Video.Media.TITLE, title)
        values.put(MediaStore.Video.Media.DISPLAY_NAME, "$title.mp4")
        values.put(MediaStore.Video.Media.DESCRIPTION, description)
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        values.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
        values.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(
                MediaStore.Video.Media.RELATIVE_PATH,
                Environment.DIRECTORY_MOVIES + "/MyAppVideos"
            )
            val resolver = context.contentResolver
            val externalContentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val uri = resolver.insert(externalContentUri, values)
            if (uri != null) {
                try {
                    fis.use {
                        it.copyTo(resolver.openOutputStream(uri)!!)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    return null
                }
            }
            uri.toString()
        } else {
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                try {
                    fis.use {
                        it.copyTo(resolver.openOutputStream(uri)!!)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    return null
                }
                // For backward compatibility, we also need to call the notify method
                context.contentResolver.notifyChange(uri, null)
            }
            uri.toString()
        }
    }
}