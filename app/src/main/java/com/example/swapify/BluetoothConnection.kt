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

    private inner class AcceptThread : Thread() {

        private val blueServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            blueAd.listenUsingInsecureRfcommWithServiceRecord(APP, MY_UUID)
        }

        override fun run() {
            Log.d(tag, "START SERVER")
            // Keep listening until exception occurs or a socket is returned.
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    blueServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e(tag, "Socket's accept() method failed", e)
                    //Toast.makeText(blueContext, "ERROR: ${"/n"}Unable to create Bluetooth Server", Toast.LENGTH_LONG).show()
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

        // Closes the connect socket and causes the thread to finish.
        fun cancel() {
            Log.d(tag, "CANCEL SERVER")
            try {
                blueServerSocket?.close()
            } catch (e: IOException) {
                Log.e(tag, "Could not close the connect socket", e)
            }
        }
    }

    private inner class ConnectThread(device: BluetoothDevice) : Thread() {

        private val blueClientSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(MY_UUID)
        }


        override fun run() {
            Log.d(tag, "START CLIENT")
            // Cancel discovery because it otherwise slows down the connection.
            blueAd.cancelDiscovery()

            blueClientSocket?.let { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                try {
                    socket.connect()
                    connected(socket)
                    Log.d(tag, "ConnectThread connected.")
                } catch (e: IOException) {
                    Log.e(tag, "Could find Server to connect to.", e)
                    //Toast.makeText(blueContext, "ERROR: ${"/n"}Unable to connect to Bluetooth Server", Toast.LENGTH_LONG).show()
                }


                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
                //manageMyConnectedSocket(socket)

            }

        }

        // Closes the client socket and causes the thread to finish.
        // EXCEPT IT DOESN'T!!!!!! FOR SOME F-ING REASON?!?
        fun cancel() {
            Log.d(tag, "CANCEL CLIENT")
            try {
                blueClientSocket?.close()
            } catch (e: IOException) {
                Log.e(tag, "Could not close the client socket", e)
            }
        }
    }

    private inner class ConnectedThread(private val blueSocket: BluetoothSocket) : Thread() {

        private val blueInStream: InputStream = blueSocket.inputStream
        private val blueOutStream: OutputStream = blueSocket.outputStream
        lateinit var blueBuffer: ByteArray // mmBuffer store for the stream

        override fun run() {
            Log.d(tag, "START CONNECTION")

            var tempArray: ByteArray
            var parts: ByteArray
            var tempNumBytes: Int
            var numBytes = 0
            var counter = 0
            var sendSize = true

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                // Read from the InputStream.
                try {
                    if (sendSize) {
                        tempArray = ByteArray(blueInStream.available())
                        if (blueInStream.read(tempArray) > 0) {
                            numBytes = String(tempArray).toInt()
                            blueBuffer = ByteArray(numBytes)
                            sendSize = false
                        }
                    } else {
                        parts = ByteArray(blueInStream.available())
                        tempNumBytes = blueInStream.read(parts)

                        System.arraycopy(parts, 0, blueBuffer, counter, tempNumBytes)
                        counter += tempNumBytes

                        if (counter == numBytes) {
                            val messageIntent = Intent("DATA_MESSAGE")
                            messageIntent.putExtra("DATA", blueBuffer)
                            LocalBroadcastManager.getInstance(blueContext).sendBroadcast(messageIntent)
                            counter = 0
                            sendSize = true
                        }
                    }
                } catch (e: IOException) {
                    Log.d(tag, "Input stream was disconnected", e)
                    break
                }
                /*
                // Send the obtained bytes to the UI activity.
                val readMsg = handler.obtainMessage(
                    MESSAGE_READ, numBytes, -1,
                    BlueBuffer)
                readMsg.sendToTarget()
                 */
            }
        }

        // Call this from the main activity to send data to the remote device.
        fun write(bytes: ByteArray) {
            try {
                blueOutStream.write(bytes)
                blueOutStream.flush()
            } catch (e: IOException) {
                Log.e(tag, "Error occurred when sending data", e)
                //Toast.makeText(blueContext, "ERROR: ${"/n"}Data couldn't be send.", Toast.LENGTH_LONG).show()
            /*
                // Send a failure message back to the activity.
                val writeErrorMsg = handler.obtainMessage(MESSAGE_TOAST)
                val bundle = Bundle().apply {
                    putString("toast", "Couldn't send data to the other device")
                }
                writeErrorMsg.data = bundle
                handler.sendMessage(writeErrorMsg)
                return
             */
            }

            // Share the sent message with the UI activity.
            /*
            val writtenMsg = handler.obtainMessage(
                MESSAGE_WRITE, -1, -1, BlueBuffer)
            writtenMsg.sendToTarget()
             */
        }


        // Call this method from the main activity to shut down the connection.
        fun cancel() {
            Log.d(tag, "CANCEL CONNECTION")
            try {
                blueSocket.close()
            } catch (e: IOException) {
                Log.e(tag, "Could not close the connect socket", e)
            }
        }
    }

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