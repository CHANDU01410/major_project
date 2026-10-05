package com.example.cappnan

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

// --- 1. TABLES (ENTITIES) ---

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey val nodeId: Int,
    val name: String,
    val publicKey: String // We will save the scanned QR key here later
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chatPartnerId: Int, // Who are you chatting with? (Matches FriendEntity.nodeId)
    val text: String,       // The actual message
    val isFromMe: Boolean,  // Did I send it, or did they?
    val timestamp: Long = System.currentTimeMillis()
)

// --- 2. QUERIES (DAOs) ---

@Dao
interface FriendDao {
    @Query("SELECT * FROM friends")
    fun getAllFriends(): Flow<List<FriendEntity>> // Flow updates the UI automatically when data changes

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: FriendEntity)
    @Query("SELECT * FROM friends WHERE nodeId = :id LIMIT 1")
    suspend fun getFriendById(id: Int): FriendEntity?
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatPartnerId = :partnerId ORDER BY timestamp ASC")
    fun getMessagesForFriend(partnerId: Int): Flow<List<MessageEntity>>

    @Insert
    suspend fun insertMessage(message: MessageEntity)
}

// --- 3. DATABASE SETUP ---

@Database(
    entities = [FriendEntity::class, MessageEntity::class, FileTransferEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun friendDao(): FriendDao
    abstract fun messageDao(): MessageDao
    abstract fun fileTransferDao(): FileTransferDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `file_transfers` (
                        `transferId` TEXT NOT NULL,
                        `fileName` TEXT NOT NULL,
                        `fileSize` INTEGER NOT NULL,
                        `senderId` INTEGER NOT NULL,
                        `receiverId` INTEGER NOT NULL,
                        `totalChunks` INTEGER NOT NULL,
                        `transferredChunks` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `localFilePath` TEXT,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`transferId`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mesh_app_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
