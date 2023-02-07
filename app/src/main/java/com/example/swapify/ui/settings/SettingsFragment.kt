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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.swapify.BluetoothConnection
import com.example.swapify.DeviceAdapter
import com.example.swapify.DiscoveredAdapter
import com.example.swapify.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null

    private val binding get() = _binding!!

    private lateinit var bluetoothConnection: BluetoothConnection
    private lateinit var bluetoothDevice: BluetoothDevice

    lateinit var deviceAdapter: DeviceAdapter
    lateinit var discoveredAdapter: DiscoveredAdapter

    lateinit var blueMan: BluetoothManager
    lateinit var blueAd: BluetoothAdapter
    lateinit var bonded: Set<BluetoothDevice>

    lateinit var devices: MutableList<BluetoothDevice>
    lateinit var discovered: MutableList<BluetoothDevice>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

    //Inits
        //Activity Ref
        val a = activity

        //Bluetooth
        blueMan = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        blueAd = blueMan.adapter as BluetoothAdapter
        bonded = blueAd.bondedDevices

        bluetoothConnection = BluetoothConnection(a!!)

        //Switches
        val swBlue: Switch = binding.swBlueOnOff
        val swVis: Switch = binding.swVisOnOff
        val swDis: Switch = binding.swDisOnOff
        val rvConn: RecyclerView = binding.rvConn
        val rvScan: RecyclerView = binding.rvScan

        //Adapters
        deviceAdapter = DeviceAdapter(mutableListOf()) {}
        discoveredAdapter = DiscoveredAdapter(mutableListOf()) {
            blueAd.cancelDiscovery()
            val pos = discoveredAdapter.getPos(it)
            discovered[pos].createBond()
            //bluetoothDevice = discovered[pos]
            //Log.d(tag, "BLUETOOTH DEVICE NOW: $bluetoothDevice")
            //bluetoothConnection = BluetoothConnection(a!!)
            swDis.isChecked = false
            discoveredAdapter.deleteList()
            discovered.clear()
        }

        rvConn.adapter = deviceAdapter
        rvConn.layoutManager = LinearLayoutManager(a)

        rvScan.adapter = discoveredAdapter
        rvScan.layoutManager = LinearLayoutManager(a)

        //Lists
        devices = mutableListOf()
        discovered = mutableListOf()

        //Register Recivers
        val intentFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        requireActivity().registerReceiver(reciver, intentFilter)

        val intentFilterBond = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        requireActivity().registerReceiver(bondReciver, intentFilterBond)

    //Start-Up
        //Init Buttons
        swBlue.isChecked = blueAd.isEnabled
        swVis.isChecked = false
        swDis.isChecked = false
        blueAd.cancelDiscovery()

        // Make Discovery possible on old devices
        val REQUEST_CODE = 1
        if (ContextCompat.checkSelfPermission(a, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(a, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_CODE);
        }

        // Set Connected List AND set-up Device and Connection if necessary
        bonded.forEach { BondedDevice ->
            bluetoothDevice = BondedDevice
            devices.add(BondedDevice)
            deviceAdapter.addDevice(BondedDevice.name)
        }

        if (blueAd.isEnabled) {
            startServer()
        }
/*
        if (bonded.isNotEmpty()) {
            startConnection()
        }
*/
        //Turn Bluetooth On/Off
        swBlue.setOnClickListener {
            if (swBlue.isChecked) {
                val intentEnable = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                launcher.launch(intentEnable)
            } else {
                cancelServer()
                blueAd.disable()
                //deviceAdapter.deleteList()
                //devices.clear()
            }
        }

        // Turn Visibility On/Off
        swVis.setOnClickListener {
            if (blueAd.isEnabled) {
                if (swVis.isChecked) {
                    val intentDis = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                    launcher.launch(intentDis)
                }
            } else {
                Toast.makeText(a, "Enable Bluetooth", Toast.LENGTH_SHORT).show()
                swVis.isChecked = false
            }
        }

        //Discovery List
        swDis.setOnClickListener {
            if (blueAd.isEnabled) {
                if (swDis.isChecked) {
                    blueAd.startDiscovery()
                } else {
                    blueAd.cancelDiscovery()
                    discoveredAdapter.deleteList()
                    discovered.clear()
                }
            } else {
                Toast.makeText(a, "Enable Bluetooth", Toast.LENGTH_SHORT).show()
                swDis.isChecked = false
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

    //StartActivity set-up
    private val  launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startServer()
        }
    }

    //Reciver Discovery List
    private val reciver = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                when(action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        if (device!!.name != null) {
                            discovered.add(device)
                            discoveredAdapter.addDevice(device.name)
                        }
                    }
                }
            }
        }
    }

    //Reciver Bonded Change
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
                        bluetoothConnection.startServer()
                        Toast.makeText(activity, "Pairing...", Toast.LENGTH_SHORT).show()
                        Log.d(tag, "BONDING...")
                    }
                    if (device.bondState == BluetoothDevice.BOND_BONDED) {
                        Toast.makeText(activity, "Paired", Toast.LENGTH_SHORT).show()
                        bluetoothDevice = device
                        startConnection()
                        Toast.makeText(activity, "Connected", Toast.LENGTH_SHORT).show()
                        //bluetoothConnection = BluetoothConnection(context)
                        bonded = blueAd.bondedDevices
                        bonded.forEach { BondedDevice ->
                            devices.add(BondedDevice)
                            deviceAdapter.addDevice(BondedDevice.name)
                        }
                        }
                }
            }
        }
    }

    private fun startConnection () {
        bluetoothConnection.startClient(bluetoothDevice)
    }

    private fun startServer () {
        bluetoothConnection.startServer()
    }

    private fun cancelServer () {
        bluetoothConnection.cancelServer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        requireActivity().unregisterReceiver(reciver)
        requireActivity().unregisterReceiver(bondReciver)
    }
}

