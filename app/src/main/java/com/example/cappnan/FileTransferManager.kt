package com.example.cappnan

import kotlinx.coroutines.flow.Flow
import java.util.UUID

class FileTransferManager(private val fileTransferDao: FileTransferDao) {

    suspend fun createOutboundTransfer(
        fileName: String,
        fileSize: Long,
        senderId: Int,
        receiverId: Int,
        totalChunks: Int,
        localFilePath: String? = null
    ): FileTransferEntity {
        val transfer = FileTransferEntity(
            transferId = UUID.randomUUID().toString(),
            fileName = fileName,
            fileSize = fileSize,
            senderId = senderId,
            receiverId = receiverId,
            totalChunks = totalChunks,
            transferredChunks = 0,
            status = "PENDING",
            localFilePath = localFilePath,
            timestamp = System.currentTimeMillis()
        )
        fileTransferDao.insertTransfer(transfer)
        return transfer
    }

    suspend fun createInboundTransfer(
        transferId: String,
        fileName: String,
        fileSize: Long,
        senderId: Int,
        receiverId: Int,
        totalChunks: Int,
        localFilePath: String? = null
    ): FileTransferEntity {
        val transfer = FileTransferEntity(
            transferId = transferId,
            fileName = fileName,
            fileSize = fileSize,
            senderId = senderId,
            receiverId = receiverId,
            totalChunks = totalChunks,
            transferredChunks = 0,
            status = "PENDING",
            localFilePath = localFilePath,
            timestamp = System.currentTimeMillis()
        )
        fileTransferDao.insertTransfer(transfer)
        return transfer
    }

    suspend fun updateProgress(transferId: String, transferredChunks: Int, status: String = "IN_PROGRESS") {
        val existing = fileTransferDao.getTransferById(transferId) ?: return
        val updated = existing.copy(
            transferredChunks = transferredChunks,
            status = if (transferredChunks >= existing.totalChunks) "COMPLETED" else status
        )
        fileTransferDao.updateTransfer(updated)
    }

    suspend fun updateStatus(transferId: String, status: String) {
        val existing = fileTransferDao.getTransferById(transferId) ?: return
        val updated = existing.copy(status = status)
        fileTransferDao.updateTransfer(updated)
    }

    suspend fun getTransfer(transferId: String): FileTransferEntity? {
        return fileTransferDao.getTransferById(transferId)
    }

    fun observeTransfer(transferId: String): Flow<FileTransferEntity?> {
        return fileTransferDao.observeTransfer(transferId)
    }

    fun getAllTransfers(): Flow<List<FileTransferEntity>> {
        return fileTransferDao.getAllTransfers()
    }

    suspend fun getPendingOrIncompleteTransfers(): List<FileTransferEntity> {
        return fileTransferDao.getIncompleteTransfers()
    }
}
