package com.lizpostudio.kgoptometrycrm.utils

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.View
import java.io.File
import java.io.FileOutputStream

object BitmapUtils {

    fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun create(view: View, bgColor: Int = Color.WHITE): Bitmap {
        val height = view.height
        val width = view.width
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(bgColor)
        view.draw(canvas)
        return bitmap
    }

    private fun removeIfExist(contentResolver: ContentResolver, filename: String) {
        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null
        )
        if (cursor != null) {
            while (cursor.moveToNext()) {
                val columnIndexDisplayName =
                    cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val displayName = cursor.getString(columnIndexDisplayName)
                if (displayName.contains(filename)) {
//                    val columnIndexSize: Int =
//                        cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
//                    val size: Long = cursor.getLong(columnIndexSize)
                    val columnIndexId: Int =
                        cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val id: Long = cursor.getLong(columnIndexId)
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                    )
                    contentResolver.delete(uri, null, null)
                }
            }
        }
        cursor?.close()
    }

    private fun createMediaUri(c: ContentResolver, file: File, mimetype: String?): Uri? {
        val contentValues = ContentValues()
        val fileName = file.name
        val relativePath = file.path.replace(fileName, "")
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimetype)
        val now = System.currentTimeMillis()
        contentValues.put(MediaStore.Images.Media.DATE_ADDED, now);
        contentValues.put(MediaStore.Images.Media.DATE_MODIFIED, now);
        contentValues.put(MediaStore.Images.Media.DATE_TAKEN, now);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        }
        return c.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    fun saveAsJpeg(c: Context, bitmap: Bitmap, destination: File): Uri? {
        val contentResolver = c.contentResolver ?: return null
        removeIfExist(contentResolver, destination.name)
        val uri = createMediaUri(
            contentResolver, destination, "image/jpeg"
        ) ?: return null

        val parcelFileDescriptor = c.contentResolver.openFileDescriptor(uri, "w")
        val outputStream = FileOutputStream(parcelFileDescriptor?.fileDescriptor)
        val quality = 100
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        outputStream.flush()
        outputStream.close()
        return uri
    }
}