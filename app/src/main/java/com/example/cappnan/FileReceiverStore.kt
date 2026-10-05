package com.example.cappnan

import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap

data class InboundTransferState(
    val transferId: String,
    val fileName: String,
    val fileSize: Long,
    val totalChunks: Int,
    val destinationFile: File,
    val receivedChunkIndices: MutableSet<Int> = mutableSetOf()
)

object FileReceiverStore {

    private const val DEFAULT_CHUNK_SIZE = 512
    private val activeInboundTransfers = ConcurrentHashMap<String, InboundTransferState>()

    fun registerInboundTransfer(
        transferId: String,
        fileName: String,
        fileSize: Long,
        totalChunks: Int,
        destDir: File
    ): InboundTransferState {
        if (!destDir.exists()) {
            destDir.mkdirs()
        }
        val destFile = File(destDir, "${transferId}_$fileName")
        val state = InboundTransferState(
            transferId = transferId,
            fileName = fileName,
            fileSize = fileSize,
            totalChunks = totalChunks,
            destinationFile = destFile
        )
        activeInboundTransfers[transferId] = state
        return state
    }

    fun getInboundTransfer(transferId: String): InboundTransferState? {
        return activeInboundTransfers[transferId]
    }

    fun writeChunk(
        chunkPacket: FileChunkPacket,
        configuredChunkSize: Int = DEFAULT_CHUNK_SIZE
    ): Boolean {
        val state = activeInboundTransfers[chunkPacket.transferId] ?: return false

        synchronized(state) {
            if (state.receivedChunkIndices.contains(chunkPacket.chunkIndex)) {
                return false // duplicate ignored
            }

            return try {
                val offset = chunkPacket.chunkIndex.toLong() * configuredChunkSize
                RandomAccessFile(state.destinationFile, "rw").use { raf ->
                    raf.seek(offset)
                    raf.write(chunkPacket.payload)
                }
                state.receivedChunkIndices.add(chunkPacket.chunkIndex)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    fun isTransferComplete(transferId: String): Boolean {
        val state = activeInboundTransfers[transferId] ?: return false
        synchronized(state) {
            return state.receivedChunkIndices.size >= state.totalChunks
        }
    }

    fun clearTransfer(transferId: String) {
        activeInboundTransfers.remove(transferId)
    }
}
