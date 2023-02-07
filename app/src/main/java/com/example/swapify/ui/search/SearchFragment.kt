package com.example.swapify.ui.search

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.swapify.BluetoothConnection
import com.example.swapify.databinding.FragmentSearchBinding

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null

    private val binding get() = _binding!!

    private lateinit var bluetoothConnection: BluetoothConnection

    lateinit var blueMan: BluetoothManager
    lateinit var blueAd: BluetoothAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        val root: View = binding.root

        blueMan = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        blueAd = blueMan.adapter as BluetoothAdapter

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