package com.example.swapify

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DiscoveredAdapter (val devices : MutableList<String>,
                         val clickListener: (String) -> Unit)
    : RecyclerView.Adapter<DiscoveredAdapter.DeviceViewHolder>() {

    class DeviceViewHolder(deviceView: View, clickPosition: (Int) -> Unit) : RecyclerView.ViewHolder(deviceView) {
        val tvDm: TextView = deviceView.findViewById(R.id.tv_device_name)
        init {
            itemView.setOnClickListener {
                clickPosition(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        return DeviceViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.device_list_item,
                parent,
                false)) {
            clickListener(devices[it])
        }
    }

    override fun onBindViewHolder(viewHolder: DeviceViewHolder, position: Int) {
        val curDevice = devices[position]
        val device = viewHolder.tvDm
        device.text = curDevice
    }

    override fun getItemCount(): Int {
        return devices.size
    }

    fun addDevice(device: String) {
        devices.add(device)
        notifyItemInserted(devices.size - 1)
    }

    fun deleteList() {
        val size = devices.size
        devices.clear()
        notifyItemRangeChanged(0, size)
    }

    fun getPos(device: String): Int {
        return devices.indexOf(device)
    }


}