package com.rozimbay.wifitransfer

import android.content.ContentResolver
import android.net.Uri
import java.io.DataOutputStream
import java.net.Socket

/**
 * Guruh a'zosi (client) tomonida ishlaydi.
 * Guruh egasining IP manziliga ulanib, tanlangan faylni yuboradi.
 * TransferServer bilan bir xil protokoldan foydalanadi.
 */
class TransferClient(
    private val onProgress: (sentBytes: Long, totalBytes: Long) -> Unit,
    private val onComplete: () -> Unit,
    private val onError: (String) -> Unit
) {
    /** Fon oqimida (thread) chaqiring - bloklovchi funksiya */
    fun sendFile(
        contentResolver: ContentResolver,
        fileUri: Uri,
        fileName: String,
        fileSize: Long,
        hostAddress: String,
        port: Int = 8988
    ) {
        try {
            Socket(hostAddress, port).use { socket ->
                val output = DataOutputStream(socket.getOutputStream())

                val nameBytes = fileName.toByteArray(Charsets.UTF_8)
                output.writeLong(fileSize)
                output.writeShort(nameBytes.size)
                output.write(nameBytes)

                contentResolver.openInputStream(fileUri)?.use { input ->
                    val buffer = ByteArray(8192)
                    var sent = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        sent += read
                        onProgress(sent, fileSize)
                    }
                }

                output.flush()
                onComplete()
            }
        } catch (e: Exception) {
            onError("Yuborishda xato: ${e.message}")
        }
    }
}
