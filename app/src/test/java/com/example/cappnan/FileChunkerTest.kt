package com.example.cappnan

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

class FileChunkerTest {

    private val transferId = "test-transfer-id-001"
    private val chunkSize = 512

    @Test
    fun smallFile_singleChunk() {
        val originalData = "Hello World P2Pigeon".toByteArray(Charsets.UTF_8)
        val inputStream = ByteArrayInputStream(originalData)

        val chunks = FileChunker.readChunks(
            inputStream = inputStream,
            transferId = transferId,
            fileSize = originalData.size.toLong(),
            chunkSize = chunkSize
        ).toList()

        assertEquals(1, chunks.size)
        val firstChunk = chunks[0]
        assertEquals(0, firstChunk.chunkIndex)
        assertEquals(1, firstChunk.totalChunks)
        assertEquals(originalData.size, firstChunk.chunkSize)
        assertArrayEquals(originalData, firstChunk.payload)

        val reassembled = FileChunker.reassembleChunks(chunks)
        assertArrayEquals(originalData, reassembled)
    }

    @Test
    fun fileExactlyEqualToChunkSize() {
        val originalData = ByteArray(chunkSize) { (it % 256).toByte() }
        val inputStream = ByteArrayInputStream(originalData)

        val chunks = FileChunker.readChunks(
            inputStream = inputStream,
            transferId = transferId,
            fileSize = originalData.size.toLong(),
            chunkSize = chunkSize
        ).toList()

        assertEquals(1, chunks.size)
        assertEquals(0, chunks[0].chunkIndex)
        assertEquals(1, chunks[0].totalChunks)
        assertEquals(chunkSize, chunks[0].chunkSize)
        assertArrayEquals(originalData, chunks[0].payload)

        val reassembled = FileChunker.reassembleChunks(chunks)
        assertArrayEquals(originalData, reassembled)
    }

    @Test
    fun fileSlightlyLargerThanChunkSize_twoChunks() {
        val size = chunkSize + 128
        val originalData = ByteArray(size) { (it % 256).toByte() }
        val inputStream = ByteArrayInputStream(originalData)

        val chunks = FileChunker.readChunks(
            inputStream = inputStream,
            transferId = transferId,
            fileSize = originalData.size.toLong(),
            chunkSize = chunkSize
        ).toList()

        assertEquals(2, chunks.size)

        // Chunk 0
        assertEquals(0, chunks[0].chunkIndex)
        assertEquals(2, chunks[0].totalChunks)
        assertEquals(chunkSize, chunks[0].chunkSize)

        // Chunk 1 (final partial chunk)
        assertEquals(1, chunks[1].chunkIndex)
        assertEquals(2, chunks[1].totalChunks)
        assertEquals(128, chunks[1].chunkSize)

        val reassembled = FileChunker.reassembleChunks(chunks)
        assertArrayEquals(originalData, reassembled)
    }

    @Test
    fun multipleChunks_exactReconstruction() {
        val size = chunkSize * 5 + 317 // 5 full chunks + 1 partial chunk
        val originalData = ByteArray(size) { ((it * 7) % 256).toByte() }
        val inputStream = ByteArrayInputStream(originalData)

        val chunks = FileChunker.readChunks(
            inputStream = inputStream,
            transferId = transferId,
            fileSize = originalData.size.toLong(),
            chunkSize = chunkSize
        ).toList()

        assertEquals(6, chunks.size)

        for (i in 0..4) {
            assertEquals(i, chunks[i].chunkIndex)
            assertEquals(6, chunks[i].totalChunks)
            assertEquals(chunkSize, chunks[i].chunkSize)
        }

        assertEquals(5, chunks[5].chunkIndex)
        assertEquals(6, chunks[5].totalChunks)
        assertEquals(317, chunks[5].chunkSize)

        val reassembled = FileChunker.reassembleChunks(chunks)
        assertArrayEquals(originalData, reassembled)
    }

    @Test
    fun emptyFile_zeroChunks() {
        val originalData = ByteArray(0)
        val inputStream = ByteArrayInputStream(originalData)

        val chunks = FileChunker.readChunks(
            inputStream = inputStream,
            transferId = transferId,
            fileSize = 0L,
            chunkSize = chunkSize
        ).toList()

        assertTrue(chunks.isEmpty())

        val reassembled = FileChunker.reassembleChunks(chunks)
        assertEquals(0, reassembled.size)
    }

    @Test
    fun calculateTotalChunks_boundaryTest() {
        assertEquals(0, FileMetadataUtils.calculateTotalChunks(0, chunkSize))
        assertEquals(1, FileMetadataUtils.calculateTotalChunks(1, chunkSize))
        assertEquals(1, FileMetadataUtils.calculateTotalChunks(512, chunkSize))
        assertEquals(2, FileMetadataUtils.calculateTotalChunks(513, chunkSize))
        assertEquals(10, FileMetadataUtils.calculateTotalChunks(5120, chunkSize))
    }
}
