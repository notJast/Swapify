package com.example.swapify

import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class BluetoothConnection(context: Context) {

    private var tag = "BLUETHOOTH CONNECTION TESTING"

    private var APP = "Swapify"
    private var MY_UUID: UUID = UUID.fromString("da443036-a419-11ed-a8fc-0242ac120002")

    private var blueMan: BluetoothManager
    private var blueAd: BluetoothAdapter

    private var  blueContext: Context

    private lateinit var accThr: AcceptThread
    private lateinit var conThr: ConnectThread
    private lateinit var nedThr: ConnectedThread

    init {
        this.blueMan = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        this.blueAd = blueMan.adapter as BluetoothAdapter
        this.blueContext = context
    }

    /** Concept as shown in the Android Developer Docs:
     *  https://developer.android.com/guide/topics/connectivity/bluetooth/connect-bluetooth-devices
     *  https://developer.android.com/guide/topics/connectivity/bluetooth/transfer-data
     */

    //Server Thread
    private inner class AcceptThread : Thread() {

        /** Server
         *  runs and waits for client to connect
         */

        private val blueServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            blueAd.listenUsingInsecureRfcommWithServiceRecord(APP, MY_UUID)
        }

        override fun run() {
            Log.d(tag, "START SERVER")
            // Listen for Client
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    blueServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e(tag, "Server .accept() failed", e)
                    shouldLoop = false
                    null
                }
                socket?.also {
                    connected(it)
                    blueServerSocket?.close()
                    shouldLoop = false
                }
            }
        }

        // Close Server Socket
        fun cancel() {
            Log.d(tag, "CANCEL SERVER")
            try {
                blueServerSocket?.close()
            } catch (e: IOException) {
                Log.e(tag, "Could not close server", e)
            }
        }
    }

    //Client Thread
    private inner class ConnectThread(device: BluetoothDevice) : Thread() {

        /** Client
         *  tries to find and connect to running Server
         */

        private val blueClientSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(MY_UUID)
        }


        override fun run() {
            Log.d(tag, "START CLIENT")
            // Cancel discovery because it slows down connection
            blueAd.cancelDiscovery()

            blueClientSocket?.let { socket ->
                // Connect to Server
                try {
                    socket.connect()
                    connected(socket)
                    Log.d(tag, "Connected.")
                } catch (e: IOException) {
                    Log.e(tag, "Could find Server", e)
                }
            }
        }

        // Closes Client Socket
        fun cancel() {
            Log.d(tag, "CANCEL CLIENT")
            try {
                blueClientSocket?.close()
            } catch (e: IOException) {
                Log.e(tag, "Could not close Client", e)
            }
        }
    }

    //Conection Thread
    private inner class ConnectedThread(private val blueSocket: BluetoothSocket) : Thread() {

        /** Connection
         *  runs after server and client connected
         *  waits for input as long as connection is up
         *  send input from on device to another
         */

        private val blueInStream: InputStream = blueSocket.inputStream
        private val blueOutStream: OutputStream = blueSocket.outputStream
        lateinit var blueBuffer: ByteArray

        override fun run() {
            Log.d(tag, "START CONNECTION")

            var tempArray: ByteArray
            var parts: ByteArray
            var tempNumBytes: Int
            var numBytes = 0
            var counter = 0
            var sendSize = true

            //runs for as long as Connection is up and waits for Input
            while (true) {
                try {
                    // Following Tutorial:
                    // https://www.youtube.com/watch?v=EzhWmZjEkrw&list=PLFh8wpMiEi8_I3ujcYY3-OaaYyLudI_qi&index=14
                    if (sendSize) {
                        // takes size of data and sets buffer
                        Log.d(tag, "SENDING...")
                        tempArray = ByteArray(blueInStream.available())
                        if (blueInStream.read(tempArray) > 0) {
                            numBytes = String(tempArray).toInt()
                            blueBuffer = ByteArray(numBytes)
                            sendSize = false
                        }
                    } else {
                        // puts data part by part into buffer
                        parts = ByteArray(blueInStream.available())
                        tempNumBytes = blueInStream.read(parts)

                        System.arraycopy(parts, 0, blueBuffer, counter, tempNumBytes)
                        counter += tempNumBytes

                        if (counter == numBytes) {
                            Log.d(tag, "DATA SEND")
                            //when all data is in the buffer: send data
                            val messageIntent = Intent("DATA_MESSAGE")
                            messageIntent.putExtra("DATA", blueBuffer)
                            LocalBroadcastManager.getInstance(blueContext).sendBroadcast(messageIntent)
                            counter = 0
                            sendSize = true
                        }
                    }
                } catch (e: IOException) {
                    Log.d(tag, "Input disconnected", e)
                    break
                }
            }
        }

        // Sends data
        fun write(bytes: ByteArray) {
            Log.d(tag, "START SENDING")
            try {
                blueOutStream.write(bytes)
                blueOutStream.flush()
            } catch (e: IOException) {
                Log.e(tag, "Error while sending data", e)
            }
        }

        // Cancel Connection
        fun cancel() {
            Log.d(tag, "CANCEL CONNECTION")
            try {
                blueSocket.close()
            } catch (e: IOException) {
                Log.e(tag, "Could not close Connection", e)
            }
        }
    }

    /**
     *  Functions to Initialize and Manage the connection
     */

    @Synchronized
    fun startServer() {
        accThr = AcceptThread()
        accThr.start()
    }

    fun startClient(device: BluetoothDevice) {
        conThr = ConnectThread(device)
        conThr.start()
    }

    private fun connected (blueSocket: BluetoothSocket) {
        nedThr = ConnectedThread(blueSocket)
        nedThr.start()
    }

    fun sendData (out: ByteArray) {
        nedThr.write(out)
    }

    fun cancelServer () {
        accThr.cancel()
    }

}