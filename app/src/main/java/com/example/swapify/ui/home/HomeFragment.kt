package com.example.swapify.ui.home

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.swapify.BluetoothConnection
import com.example.swapify.databinding.FragmentHomeBinding
import kotlinx.android.synthetic.main.fragment_home.*
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.min


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    private val binding get() = _binding!!

    private lateinit var bluetoothConnection: BluetoothConnection
    private lateinit var bluetoothDevice: BluetoothDevice

    lateinit var blueMan: BluetoothManager
    lateinit var blueAd: BluetoothAdapter
    lateinit var bonded: Set<BluetoothDevice>

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {

    // Init:
        //Binding
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

        bluetoothConnection = BluetoothConnection(a!!)

        //register reciver
        LocalBroadcastManager.getInstance(a).registerReceiver(messageReciver, IntentFilter(("DATA_MESSAGE")))

    // Start-Up:
        //Connection Set-Up
        bonded.forEach { BondedDevice ->
            bluetoothDevice = BondedDevice
        }

        if (blueAd.isEnabled) {
            startServer()
        }

    // UI-Elements:
        /** Send-Button-Listener
         *  1. Test if Bluetooth is Connected and a device is connected
         *  2. Start Intent to Open a .jpeg Document
         */
        btSend.setOnClickListener {
            if (blueAd.isEnabled) {
                if (bonded.isNotEmpty()) {
                    startConnection()
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

        return root
    }

// Implementation
    /** Set-Up Reciver
     *  1. if Intent is recived the incoming Data get's turned into a bitmap
     *  2. Bitmap is set in ImageView
     *  3. saveFile is called to save image in storage
     */
    private val messageReciver = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message: ByteArray? = intent.getByteArrayExtra("DATA")
            val bitmap = BitmapFactory.decodeByteArray(message, 0, message!!.size)
            iv_data.setImageBitmap(bitmap)
            Toast.makeText(context, "Transfer successful", Toast.LENGTH_SHORT).show()
            saveFile(bitmap)
        }
    }

    /** Set-Up Start-Activity
     *  1. Takes the data/image and turns it into a bitmap
     *  2. Uses an OutputStream and compresses bitmap and turns it into a ByteArray
     *  3. Then it sends the Size of the ByteArray as a ByteArray to set-up the Buffer in the BluetoothConnection.kt
     *  4. After that it send the Original Array Part by Part
     */
    private val  launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val inputStream: InputStream? = requireActivity().contentResolver.openInputStream(data!!.data!!)
            val bitmap: Bitmap = BitmapFactory.decodeStream(inputStream)
            // This part was made following a Tutorial:
            // https://www.youtube.com/watch?v=EzhWmZjEkrw&list=PLFh8wpMiEi8_I3ujcYY3-OaaYyLudI_qi&index=14
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            val imageBytes: ByteArray = stream.toByteArray()
            val sizeBytes = imageBytes.size.toString().toByteArray()
            sendData(sizeBytes)
            val subArraySize = 900
            var i = 0
            while (i < imageBytes.size) {
                var tempArray: ByteArray?
                tempArray = imageBytes.copyOfRange(i, min(imageBytes.size, i + subArraySize))
                sendData(tempArray)
                i += subArraySize
            }
            Toast.makeText(context, "Data send", Toast.LENGTH_SHORT).show()
        }
    }

    /** Save File to System Storage
     *  1. Sets-Up ContentResolver and takes image URI
     *  2. Sets Name and Datatype for image
     *  3. Saves Image using OutputSteam
     */
    fun saveFile (bitmap: Bitmap) {
        // Method as seen in Tutorial:
        // https://www.youtube.com/watch?v=Ul4hum3y0J8
        val conRes: ContentResolver = requireActivity().contentResolver
        val image = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, System.currentTimeMillis().toString())
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }
        val uri = conRes.insert(image, contentValues)!!

        val stream = conRes.openOutputStream(uri)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)

        Toast.makeText(context, "Image saved", Toast.LENGTH_SHORT).show()
    }

//Bluetooth Connection Calls
    private fun sendData(dataArray: ByteArray) {
        bluetoothConnection.sendData(dataArray)
    }

    private fun startServer () {
        bluetoothConnection.startServer()
    }

    private fun startConnection () {
        bluetoothConnection.startClient(bluetoothDevice)
    }

    // Destorys Fragment
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        LocalBroadcastManager.getInstance(context!!).unregisterReceiver(messageReciver)
    }
}