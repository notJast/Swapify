package com.example.swapify

import android.bluetooth.*
import android.content.Context
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.NonCancellable.start
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

    private lateinit var accThr: AcceptThread
    private lateinit var conThr: ConnectThread
    private lateinit var nedThr: ConnectedThread

    init {
        Log.d(tag, "STARTING CONNECTION")
        this.blueMan = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        this.blueAd = blueMan.adapter as BluetoothAdapter
        startServer()

    }

    //private fun randomUUID() = UUID.randomUUID()

    private inner class AcceptThread : Thread() {

        init {
            Log.d(tag, "STARTING ACCEPT THREAD")
        }

        private val blueServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            blueAd.listenUsingInsecureRfcommWithServiceRecord(APP, MY_UUID)
        }

        override fun run() {
            Log.d(tag, "AcceptThread: run.")
            // Keep listening until exception occurs or a socket is returned.
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    blueServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e(tag, "Socket's accept() method failed", e)
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


        public override fun run() {
            Log.d(tag, "ConnectThread: run.")
            // Cancel discovery because it otherwise slows down the connection.
            blueAd.cancelDiscovery()

            blueClientSocket?.let { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                socket.connect()
                Log.d(tag, "run: ConnectThread connected.")

                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
                //manageMyConnectedSocket(socket)
                connected(socket)
            }

        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
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
        private val blueBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

        override fun run() {
            var numBytes: Int // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                // Read from the InputStream.
                numBytes = try {
                    blueInStream.read(blueBuffer)
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
            } catch (e: IOException) {
                Log.e(tag, "Error occurred when sending data", e)
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

    fun write(out: ByteArray) {
        nedThr.write(out)
    }

    fun cancelServer () {
        accThr.cancel()
    }



}