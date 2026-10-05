package com.example.cappnan

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FilePacketSerializationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun fileMetaPacket_serializationAndDeserialization() {
        val originalMeta = FileMetaPacket(
            transferId = "uuid-meta-123456789",
            fileName = "sample_document.pdf",
            fileSize = 204800L,
            totalChunks = 400,
            mimeType = "application/pdf"
        )

        val serialized = FilePacketManager.serializeFileMeta(originalMeta)
        assertNotNull(serialized)
        assertTrue(serialized.size > 0)

        val deserialized = FilePacketManager.deserializeFileMeta(serialized)
        assertNotNull(deserialized)
        assertEquals(originalMeta.transferId, deserialized!!.transferId)
        assertEquals(originalMeta.fileName, deserialized.fileName)
        assertEquals(originalMeta.fileSize, deserialized.fileSize)
        assertEquals(originalMeta.totalChunks, deserialized.totalChunks)
        assertEquals(originalMeta.mimeType, deserialized.mimeType)
    }

    @Test
    fun fileChunkPacket_binaryPayloadPreservation() {
        // Arbitrary binary bytes including 0x00, 0x01, 0x7F, 0x80, 0xFF
        val binaryPayload = byteArrayOf(
            0x00.toByte(), 0x01.toByte(), 0x7F.toByte(), 0x80.toByte(), 0xFF.toByte(),
            0xAA.toByte(), 0x55.toByte(), 0x12.toByte(), 0x34.toByte(), 0xFE.toByte()
        )

        val originalChunk = FileChunkPacket(
            transferId = "chunk-transfer-uuid-99",
            chunkIndex = 42,
            totalChunks = 100,
            chunkSize = binaryPayload.size,
            payload = binaryPayload
        )

        val serialized = FilePacketManager.serializeFileChunk(originalChunk)
        assertNotNull(serialized)

        val deserialized = FilePacketManager.deserializeFileChunk(serialized)
        assertNotNull(deserialized)
        assertEquals(originalChunk.transferId, deserialized!!.transferId)
        assertEquals(originalChunk.chunkIndex, deserialized.chunkIndex)
        assertEquals(originalChunk.totalChunks, deserialized.totalChunks)
        assertEquals(originalChunk.chunkSize, deserialized.chunkSize)
        assertArrayEquals(binaryPayload, deserialized.payload)
    }

    @Test
    fun malformedPackets_handledSafely() {
        val truncatedBytes = byteArrayOf(0x00, 0x01, 0x02)

        val badMeta = FilePacketManager.deserializeFileMeta(truncatedBytes)
        assertNull(badMeta)

        val badChunk = FilePacketManager.deserializeFileChunk(truncatedBytes)
        assertNull(badChunk)
    }

    @Test
    fun fileReceiverStore_duplicateAndReconstructionTest() {
        val transferId = "test-store-id-777"
        val fileName = "test_binary.bin"
        val chunkSize = 10

        // Create 2.5 chunks of binary data (25 bytes total)
        val originalData = ByteArray(25) { ((it * 13 + 7) % 256).toByte() }
        val totalChunks = 3

        val destDir = tempFolder.newFolder("receiver_test")
        FileReceiverStore.registerInboundTransfer(
            transferId = transferId,
            fileName = fileName,
            fileSize = originalData.size.toLong(),
            totalChunks = totalChunks,
            destDir = destDir
        )

        // Chunk 0 (bytes 0..9)
        val chunk0 = FileChunkPacket(transferId, 0, totalChunks, 10, originalData.copyOfRange(0, 10))
        val chunk0First = FileReceiverStore.writeChunk(chunk0, configuredChunkSize = chunkSize)
        assertTrue(chunk0First)

        // Duplicate Chunk 0 -> Should be ignored
        val chunk0Duplicate = FileReceiverStore.writeChunk(chunk0, configuredChunkSize = chunkSize)
        assertFalse(chunk0Duplicate)

        // Chunk 2 out of order (bytes 20..24)
        val chunk2 = FileChunkPacket(transferId, 2, totalChunks, 5, originalData.copyOfRange(20, 25))
        val chunk2First = FileReceiverStore.writeChunk(chunk2, configuredChunkSize = chunkSize)
        assertTrue(chunk2First)

        assertFalse(FileReceiverStore.isTransferComplete(transferId))

        // Chunk 1 (bytes 10..19)
        val chunk1 = FileChunkPacket(transferId, 1, totalChunks, 10, originalData.copyOfRange(10, 20))
        val chunk1First = FileReceiverStore.writeChunk(chunk1, configuredChunkSize = chunkSize)
        assertTrue(chunk1First)

        // Now transfer should be complete
        assertTrue(FileReceiverStore.isTransferComplete(transferId))

        // Verify file bytes on disk match originalData exactly
        val state = FileReceiverStore.getInboundTransfer(transferId)
        assertNotNull(state)
        val reconstructedBytes = state!!.destinationFile.readBytes()
        assertArrayEquals(originalData, reconstructedBytes)

        FileReceiverStore.clearTransfer(transferId)
    }
}
