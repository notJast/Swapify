package com.example.swapify

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter (val devices : MutableList<String>)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class DeviceViewHolder(deviceView: View) : RecyclerView.ViewHolder(deviceView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return DeviceViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.device_list_item,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // TODO: WICHTIG!!!!
        // TODO: Warum hab ich mit holder.tvDeviceName keinen zugriff auf die Text View meiner "device_list_item.xml"
        // TODO  bzw. warum hab ich allgemein keinen Zugriff auf meine .xml id's?
        // TODO: Warum geht hier "holder: DeviceViewHolder" statt "holder: DeviceViewHolder" nicht?

    }

    override fun getItemCount(): Int {
        return devices.size
    }

}