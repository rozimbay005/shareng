package com.rozimbay.wifitransfer

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.p2p.WifiP2pDevice
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.rozimbay.wifitransfer.databinding.ActivityMainBinding
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var wifiDirectHelper: WifiDirectHelper
    private lateinit var peerAdapter: PeerAdapter

    private var groupOwnerAddress: String? = null
    private var isGroupOwner: Boolean = false
    private var transferServer: TransferServer? = null

    private val filePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { sendFile(it) }
    }

    private val requiredPermissions = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            wifiDirectHelper.startDiscovery()
        } else {
            Toast.makeText(this, "Qurilmalarni topish uchun ruxsat kerak", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        peerAdapter = PeerAdapter { device -> connectToDevice(device) }
        binding.peerList.layoutManager = LinearLayoutManager(this)
        binding.peerList.adapter = peerAdapter

        wifiDirectHelper = WifiDirectHelper(
            context = this,
            onPeersChanged = { peers ->
                runOnUiThread {
                    peerAdapter.submitList(peers)
                    binding.statusText.text = if (peers.isEmpty())
                        "Qurilmalar qidirilmoqda..." else "${peers.size} ta qurilma topildi"
                }
            },
            onConnected = { owner, address ->
                runOnUiThread {
                    isGroupOwner = owner
                    groupOwnerAddress = address
                    binding.statusText.text = if (owner)
                        "Ulandi! Boshqa qurilma fayl yuborishini kutyapmiz..."
                    else
                        "Ulandi! Endi fayl yuborishingiz mumkin."
                    binding.sendButton.isEnabled = !owner

                    if (owner) {
                        startReceivingServer()
                    }
                }
            },
            onDisconnected = {
                runOnUiThread {
                    binding.statusText.text = "Ulanish uzildi"
                    binding.sendButton.isEnabled = false
                    transferServer?.stop()
                }
            }
        )

        binding.scanButton.setOnClickListener { checkPermissionsAndScan() }
        binding.sendButton.setOnClickListener { filePicker.launch("*/*") }
    }

    override fun onResume() {
        super.onResume()
        wifiDirectHelper.register()
    }

    override fun onPause() {
        super.onPause()
        wifiDirectHelper.unregister()
    }

    override fun onDestroy() {
        super.onDestroy()
        transferServer?.stop()
    }

    private fun checkPermissionsAndScan() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            wifiDirectHelper.startDiscovery()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun connectToDevice(device: WifiP2pDevice) {
        binding.statusText.text = "${device.deviceName} ga ulanmoqda..."
        wifiDirectHelper.connectTo(device) { success ->
            runOnUiThread {
                if (!success) {
                    Toast.makeText(this, "Ulanib bo'lmadi, qayta urinib ko'ring", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startReceivingServer() {
        transferServer = TransferServer(
            onProgress = { fileName, received, total ->
                runOnUiThread {
                    val percent = if (total > 0) (received * 100 / total) else 0
                    binding.statusText.text = "Qabul qilinmoqda: $fileName ($percent%)"
                }
            },
            onComplete = { path ->
                runOnUiThread {
                    binding.statusText.text = "Fayl saqlandi: $path"
                    Toast.makeText(this, "Fayl qabul qilindi!", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { message ->
                runOnUiThread {
                    binding.statusText.text = "Xato: $message"
                }
            }
        )
        thread { transferServer?.start() }
    }

    private fun sendFile(uri: Uri) {
        val address = groupOwnerAddress
        if (address == null) {
            Toast.makeText(this, "Avval qurilmaga ulaning", Toast.LENGTH_SHORT).show()
            return
        }

        val (name, size) = queryFileInfo(uri)

        val client = TransferClient(
            onProgress = { sent, total ->
                runOnUiThread {
                    val percent = if (total > 0) (sent * 100 / total) else 0
                    binding.statusText.text = "Yuborilmoqda: $name ($percent%)"
                }
            },
            onComplete = {
                runOnUiThread {
                    binding.statusText.text = "Fayl yuborildi!"
                    Toast.makeText(this, "Muvaffaqiyatli yuborildi", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { message ->
                runOnUiThread {
                    binding.statusText.text = "Xato: $message"
                }
            }
        )

        thread {
            client.sendFile(contentResolver, uri, name, size, address)
        }
    }

    /** Tanlangan fayl (Uri) dan nomi va hajmini oladi */
    private fun queryFileInfo(uri: Uri): Pair<String, Long> {
        var name = "fayl"
        var size = 0L
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
                if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
            }
        }
        return name to size
    }
}
