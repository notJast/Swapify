package com.example.swapify

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Switch
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.swapify.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        var view = binding.root
        setContentView(view)

        val navView: BottomNavigationView = binding.navView


        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(setOf(
                R.id.navigation_home, R.id.navigation_recived, R.id.navigation_send, R.id.navigation_settings, R.id.navigation_search))
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navView.isSelected = true

        //var launcher: ActivityResultLauncher<Intent?>
        //start-activity set-up
        val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data
            }
        }

/*
        //Bluetooth
        val blueMan = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val blueAd = blueMan.adapter as BluetoothAdapter



        //init
        if (blueAd == null) {
            swBlue.isClickable = false
        }
        else swBlue.isChecked = blueAd.isEnabled


        //Turn On/Off
        swBlue.setOnClickListener {
            if (blueAd.isEnabled) {
                swBlue.isChecked = true
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                launcher.launch(intent)
            } else {
                blueAd.disable()
            }
        }

        swVis.setOnClickListener {
            if (!blueAd.isDiscovering) {
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                launcher.launch(intent)
            }
        }

         */



    }
}