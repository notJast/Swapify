package com.example.swapify.ui.recived

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.swapify.BluetoothConnection
import com.example.swapify.databinding.FragmentRecivedBinding

class RecivedFragment : Fragment() {

    private var _binding: FragmentRecivedBinding? = null

    private val binding get() = _binding!!

    private lateinit var bluetoothConnection: BluetoothConnection

    lateinit var blueMan: BluetoothManager
    lateinit var blueAd: BluetoothAdapter

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {

        _binding = FragmentRecivedBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val a = activity

        blueMan = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        blueAd = blueMan.adapter as BluetoothAdapter

        bluetoothConnection = BluetoothConnection(a!!)

        if (blueAd.isEnabled) {
            startServer()
        }

        return root
    }

    private fun startServer () {
        bluetoothConnection.startServer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}