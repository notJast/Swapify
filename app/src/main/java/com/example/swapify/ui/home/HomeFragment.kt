package com.example.swapify.ui.home

import android.R.attr
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.swapify.databinding.FragmentHomeBinding
import kotlinx.android.synthetic.main.fragment_home.*
import java.io.InputStream


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    lateinit var blueMan: BluetoothManager
    lateinit var blueAd: BluetoothAdapter
    lateinit var bonded: Set<BluetoothDevice>

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
                ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //Activity Ref
        val a = activity

        //Button
        val btSend: Button = binding.btSend

        //Bluetooth
        blueMan = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        blueAd = blueMan.adapter as BluetoothAdapter
        bonded = blueAd.bondedDevices

        Log.d(tag, "BONDED: $bonded")


        btSend.setOnClickListener {
            if (blueAd.isEnabled) {
                if (bonded.isNotEmpty()) {
                        var intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                        intent.addCategory(Intent.CATEGORY_OPENABLE)
                        intent.type = "image/jpeg"
                        intent = Intent.createChooser(intent, "Choose files to send")
                        launcher.launch(intent)
                    } else {
                    Toast.makeText(a, "Connect to a device", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(a, "Enable Bluetooth", Toast.LENGTH_SHORT).show()
            }
        }

/*
        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

 */
        return root
    }

    //start-activity set-up
    private val  launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            //val imageView: ImageView = binding.imageView
            val data: Intent? = result.data
            val inputStream: InputStream? = requireActivity().contentResolver.openInputStream(data!!.data!!)
            val bitmap: Bitmap = BitmapFactory.decodeStream(inputStream)
            //imageView.setImageBitmap(bitmap)

        }
    }

    fun sendFile (fileUri: Uri) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_STREAM, fileUri)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}