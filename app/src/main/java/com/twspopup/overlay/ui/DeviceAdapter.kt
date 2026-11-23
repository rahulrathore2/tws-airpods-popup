package com.twspopup.overlay.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.twspopup.overlay.R
import com.twspopup.overlay.data.DeviceItem

class DeviceAdapter(
    private var devices: List<DeviceItem>,
    private val onDeviceSelected: (DeviceItem) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    private var selectedAddress: String? = null

    fun updateData(newDevices: List<DeviceItem>, currentSelectedMac: String?) {
        this.devices = newDevices
        this.selectedAddress = currentSelectedMac
        notifyDataSetChanged()
    }

    fun setSelected(address: String?) {
        this.selectedAddress = address
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bluetooth_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        val isSelected = device.address.equals(selectedAddress, ignoreCase = true)
        holder.bind(device, isSelected) {
            selectedAddress = device.address
            notifyDataSetChanged()
            onDeviceSelected(device)
        }
    }

    override fun getItemCount(): Int = devices.size

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rootLayout: View = itemView.findViewById(R.id.deviceItemRoot)
        private val txtName: TextView = itemView.findViewById(R.id.txtItemDeviceName)
        private val txtMac: TextView = itemView.findViewById(R.id.txtItemDeviceMac)
        private val imgCheck: ImageView = itemView.findViewById(R.id.imgCheckSelected)

        fun bind(device: DeviceItem, isSelected: Boolean, onClick: () -> Unit) {
            txtName.text = device.name.ifBlank { "Unnamed Bluetooth Device" }
            txtMac.text = device.address
            rootLayout.isSelected = isSelected
            imgCheck.visibility = if (isSelected) View.VISIBLE else View.GONE

            rootLayout.setOnClickListener {
                onClick()
            }
        }
    }
}
