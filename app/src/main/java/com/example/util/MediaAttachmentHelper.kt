package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import com.example.data.remote.gemini.GeminiInlineData
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.Locale

data class AttachedMediaItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uriString: String,
    val mimeType: String,
    val isAudio: Boolean,
    val displayName: String,
    val base64Data: String,
    val durationSeconds: Int? = null,
    val transcription: String? = null,
    val isTranscribing: Boolean = false
) {
    fun toGeminiInlineData(): GeminiInlineData {
        return GeminiInlineData(
            mimeType = mimeType,
            data = base64Data
        )
    }
}

object MediaAttachmentHelper {
    private const val TAG = "MediaAttachmentHelper"

    fun processImageUri(context: Context, uri: Uri): AttachedMediaItem? {
        return try {
            val contentResolver = context.contentResolver
            val displayName = queryFileName(context, uri) ?: "Attached_Image.jpg"
            
            // Read bitmap and compress to standard JPEG for optimal multimodal token efficiency
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from $uri")
                return null
            }

            // Scale down if larger than 1600px dimension
            val maxDimension = 1600
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scaledBitmap = if (width > maxDimension || height > maxDimension) {
                val ratio = width.toFloat() / height.toFloat()
                val targetW: Int
                val targetH: Int
                if (ratio > 1) {
                    targetW = maxDimension
                    targetH = (maxDimension / ratio).toInt()
                } else {
                    targetH = maxDimension
                    targetW = (maxDimension * ratio).toInt()
                }
                Bitmap.createScaledBitmap(originalBitmap, targetW, targetH, true)
            } else {
                originalBitmap
            }

            val baos = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            val bytes = baos.toByteArray()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

            AttachedMediaItem(
                uriString = uri.toString(),
                mimeType = "image/jpeg",
                isAudio = false,
                displayName = displayName,
                base64Data = base64
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image URI: ${e.message}", e)
            null
        }
    }

    fun processAudioFile(file: File, durationSeconds: Int = 0): AttachedMediaItem? {
        return try {
            val bytes = file.readBytes()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            AttachedMediaItem(
                uriString = Uri.fromFile(file).toString(),
                mimeType = "audio/mp4",
                isAudio = true,
                displayName = "Voice Note (${formatDuration(durationSeconds)})",
                base64Data = base64,
                durationSeconds = durationSeconds
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error processing recorded audio file: ${e.message}", e)
            null
        }
    }

    fun processAudioUri(context: Context, uri: Uri): AttachedMediaItem? {
        return try {
            val displayName = queryFileName(context, uri) ?: "Audio_Reflection.m4a"
            val mimeType = context.contentResolver.getType(uri) ?: when {
                displayName.endsWith(".mp3", true) -> "audio/mp3"
                displayName.endsWith(".wav", true) -> "audio/wav"
                displayName.endsWith(".m4a", true) || displayName.endsWith(".mp4", true) -> "audio/mp4"
                displayName.endsWith(".aac", true) -> "audio/aac"
                displayName.endsWith(".ogg", true) -> "audio/ogg"
                else -> "audio/mp4"
            }

            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

            AttachedMediaItem(
                uriString = uri.toString(),
                mimeType = mimeType,
                isAudio = true,
                displayName = displayName,
                base64Data = base64
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error processing audio URI: ${e.message}", e)
            null
        }
    }

    private fun queryFileName(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            return cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore and fallback
            }
        }
        return uri.lastPathSegment
    }

    fun formatDuration(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    fun formatDurationMillis(millis: Long): String {
        val totalSecs = (millis / 1000).toInt()
        return formatDuration(totalSecs)
    }
}
