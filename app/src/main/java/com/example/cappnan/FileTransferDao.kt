package com.example.cappnan

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FileTransferDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(transfer: FileTransferEntity)

    @Update
    suspend fun updateTransfer(transfer: FileTransferEntity)

    @Query("SELECT * FROM file_transfers WHERE transferId = :transferId LIMIT 1")
    suspend fun getTransferById(transferId: String): FileTransferEntity?

    @Query("SELECT * FROM file_transfers WHERE transferId = :transferId")
    fun observeTransfer(transferId: String): Flow<FileTransferEntity?>

    @Query("SELECT * FROM file_transfers ORDER BY timestamp DESC")
    fun getAllTransfers(): Flow<List<FileTransferEntity>>

    @Query("SELECT * FROM file_transfers WHERE status IN ('PENDING', 'IN_PROGRESS') ORDER BY timestamp ASC")
    suspend fun getIncompleteTransfers(): List<FileTransferEntity>
}
