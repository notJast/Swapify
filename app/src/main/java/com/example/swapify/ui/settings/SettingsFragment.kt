package com.example.swapify.ui.settings


import android.Manifest
import android.app.Activity
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.swapify.DeviceAdapter
import com.example.swapify.DiscoveredAdapter
import com.example.swapify.databinding.FragmentSettingsBinding
import java.io.IOException


class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    lateinit var deviceAdapter: DeviceAdapter
    lateinit var discoveredAdapter: DiscoveredAdapter

    lateinit var devices: MutableList<BluetoothDevice>
    lateinit var discovered: MutableList<BluetoothDevice>


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val settingsViewModel =
            ViewModelProvider(this).get(SettingsViewModel::class.java)

        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //Activity Ref
        val a = activity

        //Bluetooth
        val blueMan = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val blueAd = blueMan.adapter as BluetoothAdapter
        val bonded: Set<BluetoothDevice>? = blueAd.bondedDevices

        //Switches
        val swBlue: Switch = binding.swBlueOnOff
        val swVis: Switch = binding.swVisOnOff
        val swDis: Switch = binding.swDisOnOff
        val rvConn: RecyclerView = binding.rvConn
        val rvScan: RecyclerView = binding.rvScan

        //Adapters
        deviceAdapter = DeviceAdapter(mutableListOf()) {Log.d(tag, "Clicked on: $it")}
        discoveredAdapter = DiscoveredAdapter(mutableListOf()) {
            blueAd.cancelDiscovery()
            swDis.isChecked = false
            val pos = discoveredAdapter.getPos(it)
            Log.d(tag, "Chosen Device${discovered[pos]}")
            discovered[pos].createBond()}

        rvConn.adapter = deviceAdapter
        rvConn.layoutManager = LinearLayoutManager(a)

        rvScan.adapter = discoveredAdapter
        rvScan.layoutManager = LinearLayoutManager(a)

        //Init
        devices = mutableListOf()
        discovered = mutableListOf()


        //Bond State Change
        val intentFilterBond = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        requireActivity().registerReceiver(bondReciver, intentFilterBond)
        //TODO maybe try and share bonded?

        //Start-Up
        swBlue.isChecked = blueAd.isEnabled
        swDis.isChecked = blueAd.isDiscovering

        val REQUEST_CODE = 1

        if (ContextCompat.checkSelfPermission(a!!, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(a, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_CODE);
        }

        // Set Connected List
        bonded?.forEach { device ->
            Log.d(tag, "Name: ${device.name}")
            devices.add(device)
            deviceAdapter.addDevice(device.name)
        }

        //Turn Bluetooth On/Off
        swBlue.setOnClickListener {
            if (swBlue.isChecked) {
                val intentEnable = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                launcher.launch(intentEnable)
                /*
                bonded?.forEach { device ->
                    Log.d(tag, "Name: ${device.name}")
                    devices.add(device)
                    deviceAdapter.addDevice(device.name)
                }
                */
            } else {
                blueAd.disable()
                //deviceAdapter.deleteList()
            }
        }

        // Turn Visibility On/Off
        swVis.setOnClickListener {
            if (swVis.isChecked) {
                val intentDis = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                launcher.launch(intentDis)
            }
        }


        //Discovery List
        swDis.setOnClickListener {
            if (swDis.isChecked) {
                Log.d(tag, "STARTING DISCOVERY")
                blueAd.startDiscovery()
                Log.d(tag, "DISCOVERY STARTED")
                val intentFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
                val test = requireActivity()
                Log.d(tag, "ACTIVITY: $test")
                requireActivity().registerReceiver(reciver, intentFilter)
            } else {
                blueAd.cancelDiscovery()
                discoveredAdapter.deleteList()
                discovered.clear()
            }
        }





/*
        val textView: TextView = binding.textSettings
        settingsViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

 */
        return root
    }

    //start-activity set-up
    private val  launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
        }
    }

    private val reciver = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(tag, "ACCESSED RECEIVER")
            val action = intent.action
            Log.d(tag, "ACTION FOUND")
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                when(action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        Log.d(tag, "DEVICE IS: ${device!!.name}")
                        if (device.name != null) {
                            discovered.add(device)
                            discoveredAdapter.addDevice(device.name)
                        }
                    }
                }
            }
        }
    }

    private val bondReciver = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                if (device != null) {
                    if (device.bondState == BluetoothDevice.BOND_NONE) {
                        deviceAdapter.deleteList()
                        devices.clear()
                    }
                    if (device.bondState == BluetoothDevice.BOND_BONDING) {
                        Log.d(tag, "BONDING...")
                    }
                    if (device.bondState == BluetoothDevice.BOND_BONDED) {

                        }

                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        requireActivity().unregisterReceiver(reciver)
        requireActivity().unregisterReceiver(bondReciver)
    }
}

