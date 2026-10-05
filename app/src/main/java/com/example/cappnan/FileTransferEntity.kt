package com.example.cappnan

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "file_transfers")
data class FileTransferEntity(
    @PrimaryKey val transferId: String,
    val fileName: String,
    val fileSize: Long,
    val senderId: Int,
    val receiverId: Int,
    val totalChunks: Int,
    val transferredChunks: Int = 0,
    val status: String,
    val localFilePath: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
