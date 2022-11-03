//package com.example.minecomms
//
////import com.google.android.gms.common.util.IOUtils.copyStream
//
//import android.Manifest
//import android.content.Context
//import android.content.pm.PackageManager
//import android.os.Bundle
//import android.util.Log
//import android.widget.TextView
//import androidx.appcompat.app.AppCompatActivity
//import androidx.collection.SimpleArrayMap
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import androidx.room.Room
//import com.example.minecomms.Connection.SerializationHelper.serialize
//import com.example.minecomms.databinding.ActivityConnectionBinding
//import com.example.minecomms.db.AppDatabase
//import com.google.android.gms.nearby.Nearby
//import com.google.android.gms.nearby.connection.*
//import java.io.*
//import java.sql.Timestamp
//
//
//// RENAME TO CONNECTION IF USING AGAIN
//class ConnectionString : AppCompatActivity() {
//    private val TAG = "Connection"
//    private val SERVICE_ID = "Nearby"
//    private val STRATEGY: Strategy = Strategy.P2P_CLUSTER
//    private val context: Context = this
//
//    private var isAdvertising = false;
//    private var eid : String = ""
//
//
//    private val incomingFilePayloads = SimpleArrayMap<Long, Payload>()
//    private val completedFilePayloads = SimpleArrayMap<Long, Payload>()
//
//    private lateinit var viewBinding: ActivityConnectionBinding
//
////    private var rcvdFilename: String? = null
////    private var policyMsg: String? = null
////    private var receiverRLindex: Int = -1
////    private var imageItem: ImageListItem? = null
//
//    private val READ_REQUEST_CODE = 42
//    private val ENDPOINT_ID_EXTRA = "com.foo.myapp.EndpointId"
//
//
//
//    companion object {
//        private const val LOCATION_PERMISSION_CODE = 100
//        private const val READ_PERMISSION_CODE = 101
//
//        private const val OWN_IMAGE_FOLDER = "own_images"
//        private const val COLLECTED_IMAGE_FOLDER = "collected_images"
//        private const val ENCRYPTED_PREFIX = "encrypted"
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_connection)
//
//        checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, LOCATION_PERMISSION_CODE)
//
//        viewBinding = ActivityConnectionBinding.inflate(layoutInflater)
//        setContentView(viewBinding.root)
//
//        viewBinding.discoverButton.setOnClickListener { startDiscovery() }
//        viewBinding.advertiseButton.setOnClickListener { startAdvertising() }
//    }
//
//    private fun checkPermission(permission: String, requestCode: Int) {
//        if (ContextCompat.checkSelfPermission(this,permission) == PackageManager.PERMISSION_DENIED) {
//            // Requesting the permission
//            ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
//        }
//        else {
//            Log.d(TAG, "Permissions not denied")
////            if (requestCode == LOCATION_PERMISSION_CODE) {
////                createFragment()
////            }
////
////            if (requestCode == READ_PERMISSION_CODE) {
////                checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, LOCATION_PERMISSION_CODE)
////            }
//        }
//    }
//
//    private fun getLocalUserName(): String {
//        val db : AppDatabase = Room.databaseBuilder(
//            applicationContext,
//            AppDatabase::class.java, "database-name"
//        ).allowMainThreadQueries().fallbackToDestructiveMigration().build()
//
//        val user = db.userDao().findActive()
//
//        if (user != null) {
//            return user.username.toString()
//        }
//
//        return ""
//    }
//
//    private fun startAdvertising() {
//        val advertisingOptions: AdvertisingOptions = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
//        val connectionReport: TextView = findViewById<TextView>(R.id.connection_report)
//
//        Nearby.getConnectionsClient(context)
//            .startAdvertising(
//                getLocalUserName(), SERVICE_ID, connectionLifecycleCallback, advertisingOptions
//            )
//            .addOnSuccessListener { unused: Void? ->
//                connectionReport.text = "Advertising..." + getLocalUserName()
//                this.isAdvertising = true
//            }
//            .addOnFailureListener { e: Exception? -> }
//    }
//
//    private fun startDiscovery() {
//        val discoveryOptions: DiscoveryOptions = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
//        val connectionReport: TextView = findViewById<TextView>(R.id.connection_report)
//        this.isAdvertising = false
//
//        Nearby.getConnectionsClient(context)
//            .startDiscovery(SERVICE_ID, endpointDiscoveryCallback, discoveryOptions)
//            .addOnSuccessListener { unused: Void? ->
//                connectionReport.text = "Discovering..."
//            }
//            .addOnFailureListener { e: java.lang.Exception? -> }
//    }
//
//    private val endpointDiscoveryCallback: EndpointDiscoveryCallback =
//        object : EndpointDiscoveryCallback() {
//            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
//                // An endpoint was found. We request a connection to it.
//                Nearby.getConnectionsClient(context)
//                    .requestConnection(getLocalUserName(), endpointId, connectionLifecycleCallback)
//                    .addOnSuccessListener { unused: Void? -> }
//                    .addOnFailureListener { e: java.lang.Exception? -> }
//            }
//
//            override fun onEndpointLost(endpointId: String) {
//                // A previously discovered endpoint has gone away.
//            }
//        }
//
//    private val connectionLifecycleCallback: ConnectionLifecycleCallback =
//        object : ConnectionLifecycleCallback() {
//            override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
//                // Automatically accept the connection on both sides.
//                Nearby.getConnectionsClient(context).acceptConnection(endpointId, payloadCallback)
//            }
//
//            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
//                val connectionReport: TextView = findViewById<TextView>(R.id.connection_report)
//
//                when (result.status.statusCode) {
//                    ConnectionsStatusCodes.STATUS_OK -> {
//                        connectionReport.text = "Connection Made!"
//
//                        val timestamp = Timestamp(System.currentTimeMillis())
//                        val bytesPayload = Payload.fromBytes(serialize(timestamp))
//                        Log.d("MESSAGE", bytesPayload.toString())
//                        if(isAdvertising)
//                            Nearby.getConnectionsClient(context).sendPayload(endpointId, bytesPayload)
//                    }
//                    ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {}
//                    ConnectionsStatusCodes.STATUS_ERROR -> {}
//                    else -> {}
//                }
//            }
//
//            override fun onDisconnected(endpointId: String) {
//                // We've been disconnected from this endpoint. No more data can be
//                // sent or received.
//            }
//        }
//
//    private val payloadCallback: PayloadCallback = object : PayloadCallback() {
//        override fun onPayloadReceived(endpointId: String, payload: Payload) {
//            // This always gets the full data of the payload. Is null if it's not a BYTES payload.
//            if (payload.type == Payload.Type.BYTES) {
//                val receivedBytes = SerializationHelper.deserialize(payload.asBytes())
//                Log.d("MESSAGE", receivedBytes.toString())
//
//                val dataDisplay: TextView = findViewById<TextView>(R.id.data_received)
//                dataDisplay.text = "Message: $receivedBytes"
//            }
//        }
//
//        override fun onPayloadTransferUpdate(
//            endpointId: String,
//            update: PayloadTransferUpdate
//        ) {
//            // Bytes payloads are sent as a single chunk, so you'll receive a SUCCESS update immediately
//            // after the call to onPayloadReceived().
//        }
//    }
//
//    /** Helper class to serialize and deserialize an Object to byte[] and vice-versa  */
//    object SerializationHelper {
//        @Throws(IOException::class)
//        fun serialize(`object`: Any?): ByteArray {
//            val byteArrayOutputStream = ByteArrayOutputStream()
//            val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
//            // transform object to stream and then to a byte array
//            objectOutputStream.writeObject(`object`)
//            objectOutputStream.flush()
//            objectOutputStream.close()
//            return byteArrayOutputStream.toByteArray()
//        }
//
//        @Throws(IOException::class, ClassNotFoundException::class)
//        fun deserialize(bytes: ByteArray?): Any {
//            val byteArrayInputStream = ByteArrayInputStream(bytes)
//            val objectInputStream = ObjectInputStream(byteArrayInputStream)
//            return objectInputStream.readObject()
//        }
//    }
//}
//
////    private fun showImageChooser(endpointId: String) {
////        this.eid = endpointId
////        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
////        intent.addCategory(Intent.CATEGORY_OPENABLE)
////        intent.type = "image/*"
////        intent.putExtra(ENDPOINT_ID_EXTRA, endpointId)
////        startActivityForResult(intent, READ_REQUEST_CODE)
////        Log.d(TAG, "end img")
////    }
////
////    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
////        super.onActivityResult(requestCode, resultCode, resultData)
////        if (requestCode == READ_REQUEST_CODE && resultCode == RESULT_OK && resultData != null) {
////            val endpointId = resultData.getStringExtra(ENDPOINT_ID_EXTRA)
////
////            // The URI of the file selected by the user.
////            val uri: Uri? = resultData.data
////            val filePayload: Payload = try {
////                // Open the ParcelFileDescriptor for this URI with read access.
////                val pfd = uri?.let { contentResolver.openFileDescriptor(it, "r") }
////                Payload.fromFile(pfd!!)
////            } catch (e: FileNotFoundException) {
////                Log.e("MyApp", "File not found", e)
////                return
////            }
////
////            // Construct a simple message mapping the ID of the file payload to the desired filename.
////            val filenameMessage = filePayload.id.toString() + ":" + uri.lastPathSegment
////
////            // Send the filename message as a bytes payload.
////            val filenameBytesPayload =
////                Payload.fromBytes(filenameMessage.toByteArray(StandardCharsets.UTF_8))
////            Nearby.getConnectionsClient(context).sendPayload(endpointId!!, filenameBytesPayload)
////
////            // Finally, send the file payload.
////            Nearby.getConnectionsClient(context).sendPayload(endpointId, filePayload)
////        }
////    }
