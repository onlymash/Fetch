@file:JvmName("FetchUtils")

package com.tonyodev.fetch2.util

import android.content.Context
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Downloader
import com.tonyodev.fetch2.Status
import java.io.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.ceil


fun canPauseDownload(download: Download): Boolean {
    return when (download.status) {
        Status.DOWNLOADING,
        Status.QUEUED -> true
        else -> false
    }
}

fun canResumeDownload(download: Download): Boolean {
    return when (download.status) {
        Status.PAUSED -> true
        else -> false
    }
}

fun canRetryDownload(download: Download): Boolean {
    return when (download.status) {
        Status.FAILED,
        Status.CANCELLED -> true
        else -> false
    }
}

fun canCancelDownload(download: Download): Boolean {
    return when (download.status) {
        Status.COMPLETED,
        Status.NONE,
        Status.FAILED -> false
        else -> true
    }
}

fun calculateProgress(downloaded: Long, total: Long): Int {
    return when {
        total < 1 -> -1
        downloaded < 1 -> 0
        downloaded >= total -> 100
        else -> ((downloaded.toDouble() / total.toDouble()) * 100).toInt()
    }
}

fun calculateEstimatedTimeRemainingInMilliseconds(downloadedBytes: Long,
                                                  totalBytes: Long,
                                                  downloadedBytesPerSecond: Long): Long {
    return when {
        totalBytes < 1 -> -1
        downloadedBytes < 1 -> -1
        downloadedBytesPerSecond < 1 -> -1
        else -> {
            val seconds = (totalBytes - downloadedBytes).toDouble() / downloadedBytesPerSecond.toDouble()
            return abs(ceil(seconds)).toLong() * 1000
        }
    }
}

fun hasIntervalTimeElapsed(nanoStartTime: Long, nanoStopTime: Long,
                           progressIntervalMilliseconds: Long): Boolean {
    return TimeUnit.NANOSECONDS
            .toMillis(nanoStopTime - nanoStartTime) >= progressIntervalMilliseconds
}

fun getUniqueId(url: String, file: String): Int {
    return (url.hashCode() * 31) + file.hashCode()
}

fun getIncrementedFileIfOriginalExists(originalPath: String): File {
    var file = File(originalPath)
    var counter = 0
    if (file.exists()) {
        val parentPath = "${file.parent}/"
        val extension = file.extension
        val fileName: String = file.nameWithoutExtension
        while (file.exists()) {
            ++counter
            val newFileName = "$fileName ($counter) "
            file = File("$parentPath$newFileName.$extension")
        }
    }
    return file
}

fun createFileIfPossible(file: File) {
    try {
        if (!file.exists()) {
            if (file.parentFile != null && !file.parentFile.exists()) {
                if (file.parentFile.mkdirs()) {
                    file.createNewFile()
                }
            } else {
                file.createNewFile()
            }
        }
    } catch (e: IOException) {
    }
}

fun getFileTempDir(context: Context): String {
    return "${context.filesDir.absoluteFile}/_fetchData/temp"
}

fun getRequestForDownload(download: Download,
                          rangeStart: Long = -1,
                          rangeEnd: Long = -1): Downloader.Request {
    val start = if (rangeStart == -1L) 0 else rangeStart
    val end = if (rangeEnd == -1L) "" else rangeEnd.toString()
    val headers = download.headers.toMutableMap()
    headers["Range"] = "bytes=$start-$end"
    return Downloader.Request(
            id = download.id,
            url = download.url,
            headers = headers,
            file = download.file,
            tag = download.tag)
}

fun getFile(filePath: String): File {
    val file = File(filePath)
    if (!file.exists()) {
        if (file.parentFile != null && !file.parentFile.exists()) {
            if (file.parentFile.mkdirs()) {
                file.createNewFile()
            }
        } else {
            file.createNewFile()
        }
    }
    return file
}

fun writeTextToFile(filePath: String, text: String) {
    val file = getFile(filePath)
    if (file.exists()) {
        val bufferedWriter = BufferedWriter(FileWriter(file))
        try {
            bufferedWriter.write(text)
        } catch (e: Exception) {
        } finally {
            try {
                bufferedWriter.close()
            } catch (e: Exception) {
            }
        }
    }
}

fun getSingleLineTextFromFile(filePath: String): String? {
    val file = getFile(filePath)
    if (file.exists()) {
        val bufferedReader = BufferedReader(FileReader(file))
        try {
            return bufferedReader.readLine()
        } catch (e: Exception) {
        } finally {
            try {
                bufferedReader.close()
            } catch (e: Exception) {
            }
        }
    }
    return null
}

fun deleteRequestTempFiles(fileTempDir: String,
                           downloader: Downloader,
                           download: Download) {
    try {
        val request = getRequestForDownload(download)
        val tempDirPath = downloader.getDirectoryForFileDownloaderTypeParallel(request)
                ?: fileTempDir
        val tempDir = getFile(tempDirPath)
        if (tempDir.exists()) {
            val tempFiles = tempDir.listFiles()
            for (tempFile in tempFiles) {
                val match = tempFile.name.startsWith("${download.id}.")
                if (match && tempFile.exists()) {
                    try {
                        tempFile.delete()
                    } catch (e: Exception) {
                    }
                }
            }
        }
    } catch (e: Exception) {

    }
}