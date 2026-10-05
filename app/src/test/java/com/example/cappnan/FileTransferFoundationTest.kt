package com.example.cappnan

import org.junit.Assert.*
import org.junit.Test

class FileTransferFoundationTest {

    @Test
    fun packetTypeConstants_areCorrect() {
        assertEquals(1.toByte(), TYPE_RREQ)
        assertEquals(2.toByte(), TYPE_RREP)
        assertEquals(3.toByte(), TYPE_DATA)
        assertEquals(4.toByte(), TYPE_ACK)
        assertEquals(5.toByte(), TYPE_FILE_META)
        assertEquals(6.toByte(), TYPE_FILE_CHUNK)
        assertEquals(7.toByte(), TYPE_FILE_ACK)
    }

    @Test
    fun fileTransferEntity_instantiation() {
        val transfer = FileTransferEntity(
            transferId = "test-uuid-1234",
            fileName = "document.pdf",
            fileSize = 1048576L,
            senderId = 1001,
            receiverId = 1002,
            totalChunks = 100,
            transferredChunks = 0,
            status = "PENDING",
            localFilePath = "/sdcard/document.pdf"
        )

        assertEquals("test-uuid-1234", transfer.transferId)
        assertEquals("document.pdf", transfer.fileName)
        assertEquals(1048576L, transfer.fileSize)
        assertEquals(1001, transfer.senderId)
        assertEquals(1002, transfer.receiverId)
        assertEquals(100, transfer.totalChunks)
        assertEquals(0, transfer.transferredChunks)
        assertEquals("PENDING", transfer.status)
        assertEquals("/sdcard/document.pdf", transfer.localFilePath)
    }
}
