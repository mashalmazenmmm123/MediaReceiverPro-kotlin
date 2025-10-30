package com.mediareceiver.pro

import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FileUtils {
    
    fun getMediaStorageDir(): File {
        val mediaDir = File(Environment.getExternalStorageDirectory(), "MediaReceiverPro")
        if (!mediaDir.exists()) {
            mediaDir.mkdirs()
        }
        return mediaDir
    }
    
    fun createMediaSubfolder(type: String): File {
        val mainDir = getMediaStorageDir()
        val subDir = File(mainDir, type)
        if (!subDir.exists()) {
            subDir.mkdirs()
        }
        return subDir
    }
    
    fun generateSafeFilename(originalName: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val safeName = originalName.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
        return "${timestamp}_$safeName"
    }
    
    fun getFileType(filename: String): String {
        if (filename.isBlank()) return "other"
        
        val extension = getFileExtension(filename).lowercase()
        return when (extension) {
            "jpg", "jpeg", "png", "gif", "bmp", "webp" -> "images"
            "mp4", "avi", "mov", "wmv", "flv", "mkv", "3gp" -> "videos"
            "mp3", "wav", "ogg", "m4a", "aac" -> "audio"
            "pdf", "doc", "docx", "txt", "xls", "xlsx" -> "documents"
            else -> "other"
        }
    }
    
    private fun getFileExtension(filename: String): String {
        val lastDot = filename.lastIndexOf('.')
        return if (lastDot == -1) "" else filename.substring(lastDot + 1)
    }
    
    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return "%.1f %s".format(Locale.getDefault(), size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
