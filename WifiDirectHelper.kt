package com.rozimbay.wifitransfer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.util.Log

/**
 * Wi-Fi Direct orqali atrofdagi qurilmalarni topish va ulanishni boshqaradi.
 * Router yoki umumiy WiFi tarmog'i shart emas - ikkita telefon to'g'ridan-to'g'ri ulanadi.
 */
class WifiDirectHelper(
    private val context: Context,
    private val onPeersChanged: (List<WifiP2pDevice>) -> Unit,
    private val onConnected: (isGroupOwner: Boolean, groupOwnerAddress: String?) -> Unit,
    private val onDisconnected: () -> Unit
) {
    companion object {
        private const val TAG = "WifiDirectHelper"
    }

    private val manager: WifiP2pManager =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private var channel: Channel = manager.initialize(context, context.mainLooper, null)

    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    private val peerListListener = WifiP2pManager.PeerListListener { peerList: WifiP2pDeviceList ->
        onPeersChanged(peerList.deviceList.toList())
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    manager.requestPeers(channel, peerListListener)
                }

                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val networkInfo = intent.getParcelableExtra<android.net.NetworkInfo>(
                        WifiP2pManager.EXTRA_NETWORK_INFO
                    )
                    if (networkInfo?.isConnected == true) {
                        manager.requestConnectionInfo(channel) { info ->
                            val groupOwnerAddress = info.groupOwnerAddress?.hostAddress
                            onConnected(info.isGroupOwner, groupOwnerAddress)
                        }
                    } else {
                        onDisconnected()
                    }
                }
            }
        }
    }

    /** Activity onResume() ichida chaqiring */
    fun register() {
        context.registerReceiver(receiver, intentFilter)
    }

    /** Activity onPause() ichida chaqiring */
    fun unregister() {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            // allaqachon unregister qilingan bo'lishi mumkin, e'tiborsiz qoldiramiz
        }
    }

    /** Atrofdagi qurilmalarni qidirishni boshlaydi. FINE_LOCATION ruxsati kerak. */
    @Suppress("MissingPermission")
    fun startDiscovery() {
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Qidiruv boshlandi")
            }

            override fun onFailure(reasonCode: Int) {
                Log.e(TAG, "Qidiruv muvaffaqiyatsiz: $reasonCode")
            }
        })
    }

    /** Tanlangan qurilmaga ulanadi */
    @Suppress("MissingPermission")
    fun connectTo(device: WifiP2pDevice, onResult: (Boolean) -> Unit) {
        val config = android.net.wifi.p2p.WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
        }
        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                onResult(true)
            }

            override fun onFailure(reason: Int) {
                onResult(false)
            }
        })
    }

    /** Ulanishni uzadi va guruhni tark etadi */
    fun disconnect() {
        manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Guruhdan chiqildi")
            }

            override fun onFailure(reason: Int) {
                Log.e(TAG, "Guruhdan chiqishda xato: $reason")
            }
        })
    }
}
