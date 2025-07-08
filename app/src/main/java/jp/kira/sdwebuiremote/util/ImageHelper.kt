package jp.kira.sdwebuiremote.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageHelper {

    fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    fun getBase64FromUri(context: Context, uri: Uri): String? {
        return try {
            val bitmap = getBitmapFromUri(context, uri)
            bitmap?.let { bitmapToBase64(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveImageToInternalStorage(context: Context, bitmap: Bitmap, directoryName: String): String? {
        val directory = context.getDir(directoryName, Context.MODE_PRIVATE)
        val fileName = "img_${System.currentTimeMillis()}.png"
        val file = File(directory, fileName)
        return try {
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
