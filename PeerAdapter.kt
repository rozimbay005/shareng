package com.rozimbay.wifitransfer

import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rozimbay.wifitransfer.databinding.ItemPeerBinding

class PeerAdapter(
    private val onClick: (WifiP2pDevice) -> Unit
) : RecyclerView.Adapter<PeerAdapter.PeerViewHolder>() {

    private val peers = mutableListOf<WifiP2pDevice>()

    fun submitList(newPeers: List<WifiP2pDevice>) {
        peers.clear()
        peers.addAll(newPeers)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeerViewHolder {
        val binding = ItemPeerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PeerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PeerViewHolder, position: Int) {
        holder.bind(peers[position])
    }

    override fun getItemCount() = peers.size

    inner class PeerViewHolder(private val binding: ItemPeerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(device: WifiP2pDevice) {
            binding.peerName.text = device.deviceName
            binding.root.setOnClickListener { onClick(device) }
        }
    }
}
