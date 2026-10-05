package com.example.cappnan

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.InputStream

data class FileChunk(
    val transferId: String,
    val chunkIndex: Int,
    val totalChunks: Int,
    val chunkSize: Int,
    val payload: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FileChunk
        if (transferId != other.transferId) return false
        if (chunkIndex != other.chunkIndex) return false
        if (totalChunks != other.totalChunks) return false
        if (chunkSize != other.chunkSize) return false
        if (!payload.contentEquals(other.payload)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = transferId.hashCode()
        result = 31 * result + chunkIndex
        result = 31 * result + totalChunks
        result = 31 * result + chunkSize
        result = 31 * result + payload.contentHashCode()
        return result
    }
}

data class FileMetadata(
    val fileName: String,
    val fileSize: Long,
    val contentUri: String,
    val mimeType: String?,
    val totalChunks: Int
)

object FileMetadataUtils {
    const val DEFAULT_CHUNK_SIZE = 512

    fun calculateTotalChunks(fileSize: Long, chunkSize: Int = DEFAULT_CHUNK_SIZE): Int {
        if (fileSize <= 0) return 0
        return ((fileSize + chunkSize - 1) / chunkSize).toInt()
    }

    fun extractMetadata(context: Context, uri: Uri, chunkSize: Int = DEFAULT_CHUNK_SIZE): FileMetadata? {
        val contentResolver = context.contentResolver
        var fileName: String? = null
        var fileSize: Long = 0L
        val mimeType: String? = contentResolver.getType(uri)

        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1 && !cursor.isNull(nameIndex)) {
                        fileName = cursor.getString(nameIndex)
                    }
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback query fail
        }

        if (fileName.isNull_or_empty()) {
            fileName = uri.lastPathSegment ?: "unknown_file"
        }

        if (fileSize == 0L) {
            try {
                contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    fileSize = pfd.statSize
                }
            } catch (e: Exception) {
                // Fallback
            }
        }

        val totalChunks = calculateTotalChunks(fileSize, chunkSize)

        return FileMetadata(
            fileName = fileName ?: "unknown_file",
            fileSize = fileSize,
            contentUri = uri.toString(),
            mimeType = mimeType,
            totalChunks = totalChunks
        )
    }

    private fun String?.isNull_or_empty(): Boolean = this == null || this.isEmpty()
}

object FileChunker {
    const val DEFAULT_CHUNK_SIZE = 512

    fun readChunks(
        inputStream: InputStream,
        transferId: String,
        fileSize: Long,
        chunkSize: Int = DEFAULT_CHUNK_SIZE
    ): Sequence<FileChunk> = sequence {
        if (fileSize <= 0) return@sequence

        val totalChunks = FileMetadataUtils.calculateTotalChunks(fileSize, chunkSize)
        val buffer = ByteArray(chunkSize)
        var chunkIndex = 0
        var bytesRead: Int

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            if (bytesRead > 0) {
                val chunkPayload = if (bytesRead == chunkSize) {
                    buffer.copyOf()
                } else {
                    buffer.copyOf(bytesRead)
                }
                yield(
                    FileChunk(
                        transferId = transferId,
                        chunkIndex = chunkIndex,
                        totalChunks = totalChunks,
                        chunkSize = bytesRead,
                        payload = chunkPayload
                    )
                )
                chunkIndex++
            }
        }
    }

    fun reassembleChunks(chunks: List<FileChunk>): ByteArray {
        val sorted = chunks.sortedBy { it.chunkIndex }
        val totalSize = sorted.sumOf { it.chunkSize }
        val result = ByteArray(totalSize)
        var offset = 0
        for (chunk in sorted) {
            System.arraycopy(chunk.payload, 0, result, offset, chunk.chunkSize)
            offset += chunk.chunkSize
        }
        return result
    }
}
