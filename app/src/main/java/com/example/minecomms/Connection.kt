package com.example.minecomms

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.collection.SimpleArrayMap
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener

import com.example.minecomms.Account
import com.example.minecomms.databinding.ActivityAccountBinding
import com.example.minecomms.databinding.ActivityConnectionBinding
import com.example.minecomms.db.AppDatabase


class Connection : AppCompatActivity() {
    private val TAG = "Connection"
    private val SERVICE_ID = "Nearby"
    private val STRATEGY: Strategy = Strategy.P2P_CLUSTER
    private val context: Context = this

    private val incomingFilePayloads = SimpleArrayMap<Long, Payload>()
    private val completedFilePayloads = SimpleArrayMap<Long, Payload>()

    private lateinit var viewBinding: ActivityConnectionBinding

    companion object {
        private const val LOCATION_PERMISSION_CODE = 100
        private const val READ_PERMISSION_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection)

        checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, LOCATION_PERMISSION_CODE)


        viewBinding = ActivityConnectionBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)


        viewBinding.discoverButton.setOnClickListener { startDiscovery() }
        viewBinding.advertiseButton.setOnClickListener { startAdvertising() }
    }

    private fun checkPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(this,permission) == PackageManager.PERMISSION_DENIED) {
            // Requesting the permission
            ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
        }
        else {
            Log.d(TAG, "Permissions not denied")
//            if (requestCode == LOCATION_PERMISSION_CODE) {
//                createFragment()
//            }
//
//            if (requestCode == READ_PERMISSION_CODE) {
//                checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, LOCATION_PERMISSION_CODE)
//            }
        }
    }

    private fun getLocalUserName(): String {
        val db : AppDatabase = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "database-name"
        ).allowMainThreadQueries().fallbackToDestructiveMigration().build()

        val user = db.userDao().findActive()

        if (user != null) {
            return user.username.toString()
        }

        return ""
    }

    private fun startAdvertising() {
        val advertisingOptions: AdvertisingOptions = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        val connectionReport: TextView = findViewById<TextView>(R.id.connection_report)

        Nearby.getConnectionsClient(context)
            .startAdvertising(
                getLocalUserName(), SERVICE_ID, connectionLifecycleCallback, advertisingOptions
            )
            .addOnSuccessListener { unused: Void? ->
                connectionReport.text = "Advertising..." + getLocalUserName()
            }
            .addOnFailureListener { e: Exception? -> }
    }

    private fun startDiscovery() {
        val discoveryOptions: DiscoveryOptions = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        val connectionReport: TextView = findViewById<TextView>(R.id.connection_report)

        Nearby.getConnectionsClient(context)
            .startDiscovery(SERVICE_ID, endpointDiscoveryCallback, discoveryOptions)
            .addOnSuccessListener { unused: Void? ->
                connectionReport.text = "Discovering..."
            }
            .addOnFailureListener { e: java.lang.Exception? -> }
    }

    private val endpointDiscoveryCallback: EndpointDiscoveryCallback =
        object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                // An endpoint was found. We request a connection to it.
                Nearby.getConnectionsClient(context)
                    .requestConnection(getLocalUserName(), endpointId, connectionLifecycleCallback)
                    .addOnSuccessListener(
                        OnSuccessListener { unused: Void? -> })
                    .addOnFailureListener(
                        OnFailureListener { e: java.lang.Exception? -> })
            }

            override fun onEndpointLost(endpointId: String) {
                // A previously discovered endpoint has gone away.
            }
        }



    private val connectionLifecycleCallback: ConnectionLifecycleCallback =
        object : ConnectionLifecycleCallback() {

            override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
                // Automatically accept the connection on both sides.
                Nearby.getConnectionsClient(context).acceptConnection(endpointId, payloadCallback)
            }

            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                val connectionReport: TextView = findViewById<TextView>(R.id.connection_report)

                when (result.status.statusCode) {
                    ConnectionsStatusCodes.STATUS_OK -> {
                        connectionReport.text = "Connection Made!"
//                        sendPayload(endpointId, -1)
                    }
                    ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {}
                    ConnectionsStatusCodes.STATUS_ERROR -> {}
                    else -> {}
                }
            }

            override fun onDisconnected(endpointId: String) {
                // We've been disconnected from this endpoint. No more data can be
                // sent or received.
            }
        }

    private val payloadCallback: PayloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            // A new payload is being sent over.
            Log.d(TAG, "Payload Received")
            when (payload.type) {
                Payload.Type.BYTES -> {
//                    var rcvdFilename = String(payload.asBytes()!!, StandardCharsets.UTF_8)
                    Log.d(TAG, payload.asBytes().toString())
                    val dataDisplay: TextView = findViewById<TextView>(R.id.data_received)
                    dataDisplay.text = payload.asBytes().toString()
                }
//                Payload.Type.FILE -> {
//                    // Add this to our tracking map, so that we can retrieve the payload later.
//                    incomingFilePayloads.put(payload.id, payload);
//                }
//                Payload.Type.STREAM -> {
//                    Log.d(TAG, "Inside file mode")
//                }
            }
        }


        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            val dataDisplay: TextView = findViewById<TextView>(R.id.data_received)


//            if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
//                val payloadId = update.payloadId
//                val payload = incomingFilePayloads.remove(payloadId)
//                completedFilePayloads.put(payloadId, payload)
//                if (payload != null && payload.type == Payload.Type.FILE) {
////                    val isDone = processFilePayload(payloadId, endpointId)
//                    val isDone = true // REPLACE JUST A TEST
//                    if (isDone) {
//                        Log.d(TAG, "above")
//                        Log.d(TAG, payload.toString())
//                        Log.d(TAG, "below")
//                        Nearby.getConnectionsClient(context).disconnectFromEndpoint(endpointId) //test
//                    }
//                }
//            }
            if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
                val payloadId = update.payloadId
                val payload = incomingFilePayloads.remove(payloadId)
                completedFilePayloads.put(payloadId, payload)
                Log.d(TAG, "successful send")
//                count++
                dataDisplay.text = payload?.asBytes().toString()

                if (payload != null && payload.type == Payload.Type.FILE) {
//                    val isDone = processFilePayload(payloadId, endpointId)
                    val isDone = false

                    if (isDone) {
                        Nearby.getConnectionsClient(context)
                            .disconnectFromEndpoint(endpointId) //test
                    }
                }
            }
//            if (update.status === PayloadTransferUpdate.Status.SUCCESS) {
//                val payloadId = update.payloadId
//                val payload = incomingFilePayloads.remove(payloadId)
//                completedFilePayloads.put(payloadId, payload)
//                if (payload!!.type == Payload.Type.FILE) {
//                    processFilePayload(payloadId)
//                }
//            }
        }
    }

//    fun onConnectionInitiated(endpointId: String?, info: ConnectionInfo) {
//        Builder(context)
//            .setTitle("Accept connection to " + info.endpointName)
//            .setMessage("Confirm the code matches on both devices: " + info.authenticationDigits)
//            .setPositiveButton(
//                "Accept"
//            ) { dialog: DialogInterface?, which: Int ->  // The user confirmed, so we can accept the connection.
//                Nearby.getConnectionsClient(context)
//                    .acceptConnection(endpointId!!, payloadCallback)
//            }
//            .setNegativeButton(
//                android.R.string.cancel
//            ) { dialog: DialogInterface?, which: Int ->  // The user canceled, so we should reject the connection.
//                Nearby.getConnectionsClient(context).rejectConnection(endpointId!!)
//            }
//            .setIcon(android.R.drawable.ic_dialog_alert)
//            .show()
//    }

//    Payload bytesPayload = Payload.fromBytes(new byte[] {0xa, 0xb, 0xc, 0xd});
//    Nearby.getConnectionsClient(context).sendPayload(toEndpointId, bytesPayload);

//    internal class ReceiveBytesPayloadListener : PayloadCallback() {
//        override fun onPayloadReceived(endpointId: String, payload: Payload) {
//            // This always gets the full data of the payload. Is null if it's not a BYTES payload.
//            if (payload.type == Payload.Type.BYTES) {
//                val receivedBytes = payload.asBytes()
//            }
//        }
//
//        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
//            // Bytes payloads are sent as a single chunk, so you'll receive a SUCCESS update immediately
//            // after the call to onPayloadReceived().
//        }
//    }
}