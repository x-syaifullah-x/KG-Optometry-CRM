package com.lizpostudio.kgoptometrycrm.utils

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.os.Build
import java.io.FileDescriptor
import kotlin.math.roundToInt

fun screenWidthPx(): Int {
    return Resources.getSystem().displayMetrics.widthPixels
}

fun getScaledBitmap(path: String, destWidth: Int, destHeight: Int): Bitmap? {
    // Read in the dimensions of the image on disk
    var options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    BitmapFactory.decodeFile(path, options)

    val srcWidth = options.outWidth.toFloat()
    val srcHeight = options.outHeight.toFloat()

    // Figure out how much to scale down by
    var inSampleSize = 1
    if (srcHeight > destHeight || srcWidth > destWidth) {
        val heightScale = srcHeight / destHeight
        val widthScale = srcWidth / destWidth

        val sampleScale = if (heightScale > widthScale) {
            heightScale
        } else {
            widthScale
        }
        inSampleSize = sampleScale.roundToInt()
    }

    options = BitmapFactory.Options()
    options.inSampleSize = inSampleSize

    // Read in and create final bitmap
    return BitmapFactory.decodeFile(path, options)
}

fun getScaledBitmap(
    fileDescriptor: FileDescriptor,
    destWidth: Int,
    destHeight: Int
): Bitmap {
    // Read in the dimensions of the image on disk
    var options = BitmapFactory.Options()
    options.inJustDecodeBounds = true

    BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options)
    val srcWidth = options.outWidth.toFloat()
    val srcHeight = options.outHeight.toFloat()

    // Figure out how much to scale down by
    var inSampleSize = 1
    if (srcHeight > destHeight || srcWidth > destWidth) {
        val heightScale = srcHeight / destHeight
        val widthScale = srcWidth / destWidth

        val sampleScale =
            if (heightScale > widthScale) {
                heightScale
            } else {
                widthScale
            }
        inSampleSize = sampleScale.roundToInt()
    }

    options = BitmapFactory.Options()
    options.inSampleSize = inSampleSize
    options.inJustDecodeBounds = false

    // Read in and create final bitmap
    val result = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options)

    val exifAttributeInt =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val exif = ExifInterface(fileDescriptor)
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)
        } else {
            0
        }
    val newBitmap =
        if (exifAttributeInt == ExifInterface.ORIENTATION_ROTATE_90) {
            BitmapUtils.rotate(result, 90F)
        } else {
            result
        }
    return newBitmap
}
