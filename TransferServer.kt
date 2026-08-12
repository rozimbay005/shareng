package com.rozimbay.wifitransfer

import android.os.Environment
import java.io.DataInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.ServerSocket
import java.net.Socket

/**
 * Wi-Fi Direct guruh egasi (group owner) tomonida ishlaydi.
 * Kiruvchi ulanishni kutadi va faylni qabul qilib, telefon xotirasiga saqlaydi.
 *
 * Protokol juda sodda:
 *   1) 8 bayt (Long) - fayl hajmi (bayt hisobida)
 *   2) 2 bayt (Short) - fayl nomi uzunligi
 *   3) fayl nomi (UTF-8)
 *   4) faylning o'zi (ketma-ket bayt oqimi)
 */
class TransferServer(
    private val port: Int = 8988,
    private val onProgress: (fileName: String, receivedBytes: Long, totalBytes: Long) -> Unit,
    private val onComplete: (savedFilePath: String) -> Unit,
    private val onError: (String) -> Unit
) {
    @Volatile
    private var running = false
    private var serverSocket: ServerSocket? = null

    /** Fon oqimida (thread) ishga tushiring - bloklovchi funksiya */
    fun start() {
        running = true
        try {
            serverSocket = ServerSocket(port)
            while (running) {
                val client = serverSocket?.accept() ?: break
                handleClient(client)
            }
        } catch (e: Exception) {
            if (running) {
                onError("Server xatosi: ${e.message}")
            }
        }
    }

    fun stop() {
        running = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // e'tiborsiz qoldiramiz
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            val input = DataInputStream(socket.getInputStream())

            val totalBytes = input.readLong()
            val nameLength = input.readShort().toInt()
            val nameBytes = ByteArray(nameLength)
            input.readFully(nameBytes)
            val fileName = String(nameBytes, Charsets.UTF_8)

            val downloadsDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "WifiTransfer"
            )
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val outFile = File(downloadsDir, fileName)
            val output = FileOutputStream(outFile)

            val buffer = ByteArray(8192)
            var received = 0L
            var read: Int
            while (received < totalBytes) {
                read = input.read(buffer, 0, minOf(buffer.size, (totalBytes - received).toInt()))
                if (read == -1) break
                output.write(buffer, 0, read)
                received += read
                onProgress(fileName, received, totalBytes)
            }

            output.flush()
            output.close()
            onComplete(outFile.absolutePath)
        } catch (e: Exception) {
            onError("Qabul qilishda xato: ${e.message}")
        } finally {
            socket.close()
        }
    }
}
