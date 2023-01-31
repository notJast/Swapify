package com.example.swapify.ui.settings


import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.swapify.DeviceAdapter
import com.example.swapify.databinding.FragmentSettingsBinding


class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    //var launcher: ActivityResultLauncher<Intent?>
    //start-activity set-up
    private val  launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
        }
    }



    lateinit var deviceAdapter: DeviceAdapter

    private val devices = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val settingsViewModel =
            ViewModelProvider(this).get(SettingsViewModel::class.java)

        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root


        //Switches
        val swBlue: Switch = binding.swBlueOnOff
        val swVis: Switch = binding.swVisOnOff
        val rvConn: RecyclerView = binding.rvConn
        val rvScan: RecyclerView = binding.rvScan

        //TODO Warum geht hier das "this" nicht beim LinearLayoutManager?
        rvConn.layoutManager = LinearLayoutManager(null)

        //Bluetooth
        val blueMan = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val blueAd = blueMan.adapter as BluetoothAdapter

        swBlue.isChecked = blueAd.isEnabled

        //Turn Bluetooth On/Off
        swBlue.setOnClickListener {
            if (swBlue.isChecked) {
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                launcher.launch(intent)
            } else {
                blueAd.disable()
            }
        }


        /*
        swBlue.setOnClickListener {
            if (blueAd.isEnabled) {
                swBlue.isChecked = true
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                launcher.launch(intent)
        } else {
               swBlue.isChecked = false
               blueAd.disable()
           }
        }
        */

        // Turn Visibility On/Off
        swVis.setOnClickListener {
            if (!blueAd.isDiscovering) {
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                launcher.launch(intent)
            }
        }

        // Set Connected List
        val bonded = blueAd.bondedDevices
        if (bonded.size  > 0) {
            for(device in bonded) {
                devices.add(device.name)
            }
            //
            rvConn.adapter = DeviceAdapter(devices)
            //adapter = ListAdapter(devices, activity, String)
            //val arrayAdapter: ArrayAdapter<String> = ArrayAdapter<String>(ApplicationProvider.getApplicationContext(), rvConn, devices)
        }



/*
        val textView: TextView = binding.textSettings
        settingsViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

 */
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

