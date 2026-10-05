package com.example.cappnan

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

data class FileMetaPacket(
    val transferId: String,
    val fileName: String,
    val fileSize: Long,
    val totalChunks: Int,
    val mimeType: String
)

data class FileChunkPacket(
    val transferId: String,
    val chunkIndex: Int,
    val totalChunks: Int,
    val chunkSize: Int,
    val payload: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FileChunkPacket
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

object FilePacketManager {

    // --- TYPE_FILE_META SERIALIZATION ---
    fun serializeFileMeta(meta: FileMetaPacket): ByteArray {
        val transferIdBytes = meta.transferId.toByteArray(StandardCharsets.UTF_8)
        val fileNameBytes = meta.fileName.toByteArray(StandardCharsets.UTF_8)
        val mimeTypeBytes = meta.mimeType.toByteArray(StandardCharsets.UTF_8)

        val totalSize = 2 + transferIdBytes.size +
                        2 + fileNameBytes.size +
                        8 + // fileSize (Long)
                        4 + // totalChunks (Int)
                        2 + mimeTypeBytes.size

        val buffer = ByteBuffer.allocate(totalSize)
        buffer.putShort(transferIdBytes.size.toShort())
        buffer.put(transferIdBytes)

        buffer.putShort(fileNameBytes.size.toShort())
        buffer.put(fileNameBytes)

        buffer.putLong(meta.fileSize)
        buffer.putInt(meta.totalChunks)

        buffer.putShort(mimeTypeBytes.size.toShort())
        buffer.put(mimeTypeBytes)

        return buffer.array()
    }

    fun deserializeFileMeta(bytes: ByteArray): FileMetaPacket? {
        if (bytes.size < 18) return null
        return try {
            val buffer = ByteBuffer.wrap(bytes)

            val transferIdLen = buffer.getShort().toInt() and 0xFFFF
            if (buffer.remaining() < transferIdLen + 2) return null
            val transferIdBytes = ByteArray(transferIdLen)
            buffer.get(transferIdBytes)
            val transferId = String(transferIdBytes, StandardCharsets.UTF_8)

            val fileNameLen = buffer.getShort().toInt() and 0xFFFF
            if (buffer.remaining() < fileNameLen + 12) return null
            val fileNameBytes = ByteArray(fileNameLen)
            buffer.get(fileNameBytes)
            val fileName = String(fileNameBytes, StandardCharsets.UTF_8)

            val fileSize = buffer.getLong()
            val totalChunks = buffer.getInt()

            if (buffer.remaining() < 2) return null
            val mimeTypeLen = buffer.getShort().toInt() and 0xFFFF
            if (buffer.remaining() < mimeTypeLen) return null
            val mimeTypeBytes = ByteArray(mimeTypeLen)
            buffer.get(mimeTypeBytes)
            val mimeType = String(mimeTypeBytes, StandardCharsets.UTF_8)

            FileMetaPacket(
                transferId = transferId,
                fileName = fileName,
                fileSize = fileSize,
                totalChunks = totalChunks,
                mimeType = mimeType
            )
        } catch (e: Exception) {
            null
        }
    }

    // --- TYPE_FILE_CHUNK SERIALIZATION ---
    fun serializeFileChunk(chunk: FileChunkPacket): ByteArray {
        val transferIdBytes = chunk.transferId.toByteArray(StandardCharsets.UTF_8)
        val totalSize = 2 + transferIdBytes.size +
                        4 + // chunkIndex (Int)
                        4 + // totalChunks (Int)
                        4 + // chunkSize (Int)
                        chunk.payload.size

        val buffer = ByteBuffer.allocate(totalSize)
        buffer.putShort(transferIdBytes.size.toShort())
        buffer.put(transferIdBytes)

        buffer.putInt(chunk.chunkIndex)
        buffer.putInt(chunk.totalChunks)
        buffer.putInt(chunk.payload.size)
        buffer.put(chunk.payload)

        return buffer.array()
    }

    fun deserializeFileChunk(bytes: ByteArray): FileChunkPacket? {
        if (bytes.size < 14) return null
        return try {
            val buffer = ByteBuffer.wrap(bytes)

            val transferIdLen = buffer.getShort().toInt() and 0xFFFF
            if (buffer.remaining() < transferIdLen + 12) return null
            val transferIdBytes = ByteArray(transferIdLen)
            buffer.get(transferIdBytes)
            val transferId = String(transferIdBytes, StandardCharsets.UTF_8)

            val chunkIndex = buffer.getInt()
            val totalChunks = buffer.getInt()
            val chunkSize = buffer.getInt()

            if (buffer.remaining() < chunkSize || chunkSize < 0) return null
            val payload = ByteArray(chunkSize)
            buffer.get(payload)

            FileChunkPacket(
                transferId = transferId,
                chunkIndex = chunkIndex,
                totalChunks = totalChunks,
                chunkSize = chunkSize,
                payload = payload
            )
        } catch (e: Exception) {
            null
        }
    }
}
