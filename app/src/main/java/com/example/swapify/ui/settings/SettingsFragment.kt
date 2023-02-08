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

    //Local Arrays / Copys of both RecyclerViews
    lateinit var devices: MutableList<BluetoothDevice>
    lateinit var discovered: MutableList<BluetoothDevice>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

    //Inits
        //Binding
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

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
        // I saw this on Stackoverflow, but can't find it :(
        // I think newer devices need another check but I couldn't test it and didn't wanna put code in here where I didn't know what it did
        val REQUEST_CODE = 1
        if (ContextCompat.checkSelfPermission(a, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(a, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_CODE);
        }

    // UI-Elements
        /** Set Connected List AND Set-Up Device and Connection (if necessary)
         *  1. Check if there are bonded devices
         *  2. Add all found devices to RecyclerView
         */
        bonded.forEach { BondedDevice ->
            bluetoothDevice = BondedDevice
            devices.add(BondedDevice)
            deviceAdapter.addDevice(BondedDevice.name)
        }
        if (blueAd.isEnabled) {
            startServer()
        }


        /** Turn Bluetooth On/Off
         *  1. if Off: Send Intent to turn Bluetooth on
         *  2. if ON: Disable Bluetooth
         */
        swBlue.setOnClickListener {
            if (swBlue.isChecked) {
                val intentEnable = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                launcher.launch(intentEnable)
            } else {
                cancelServer()
                blueAd.disable()
                // Does not delect conected Devices List because for some reason the phones we used still stayed connected
            }
        }

        /** Turn Visibility On/Off
         *  1. if On: send intent to turn Visibility on
         *  2. make sure Bluetooth is on first
         *  3. probably would've been better to implement as button since you can't really turn it off
         *     except if you turn off Bluetooth itself
         */
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

        /** Discovery List On/Off
         *  1. if On: Starts Discovery so Bluetooth devices can be found
         *  2. if Off: Clears RecyclerView and local array for Found devices
         */
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

        return root
    }

//Implementaions
    /** StartActivity set-up
     *  Also start's up a Server Socket just in case, since Bluetooth is always on when it's called
     */
    private val  launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startServer()
        }
    }

    /** Reciver Discovery List
     *  If a Bluetooth devices is found it get's added to RecyclerView and local array
     */
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

    /** Reciver Bonded Change
     *  Listens for a Connection State Change:
     *  No Bond: delete Recycler View and local array
     *  Bonding: Start a Server in preperation of client
     *  New Bond: Start Client Request and Add to RecyclerView and local array
     */
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

//Bluetooth Connection Calls
    private fun startConnection () {
        bluetoothConnection.startClient(bluetoothDevice)
    }

    private fun startServer () {
        bluetoothConnection.startServer()
    }

    private fun cancelServer () {
        bluetoothConnection.cancelServer()
    }

    // Destorys Fragment
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        requireActivity().unregisterReceiver(reciver)
        requireActivity().unregisterReceiver(bondReciver)
    }
}

