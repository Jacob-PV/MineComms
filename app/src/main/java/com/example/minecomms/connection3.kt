package com.example.minecomms

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.collection.SimpleArrayMap
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.example.minecomms.databinding.ActivityConnectionBinding
import com.example.minecomms.db.AppDatabase
//import com.google.android.gms.common.util.IOUtils.copyStream
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import java.io.*
import java.nio.charset.StandardCharsets


// RENAME TO CONNECTION IF USING AGAIN
class Connection3 : AppCompatActivity() {
    private val TAG = "Connection"
    private val SERVICE_ID = "Nearby"
    private val STRATEGY: Strategy = Strategy.P2P_CLUSTER
    private val context: Context = this

    private var isAdvertising = false;
    private var eid : String = ""


    private val incomingFilePayloads = SimpleArrayMap<Long, Payload>()
    private val completedFilePayloads = SimpleArrayMap<Long, Payload>()

    private lateinit var viewBinding: ActivityConnectionBinding

//    private var rcvdFilename: String? = null
//    private var policyMsg: String? = null
//    private var receiverRLindex: Int = -1
//    private var imageItem: ImageListItem? = null

    private val READ_REQUEST_CODE = 42
    private val ENDPOINT_ID_EXTRA = "com.foo.myapp.EndpointId"



    companion object {
        private const val LOCATION_PERMISSION_CODE = 100
        private const val READ_PERMISSION_CODE = 101

        private const val OWN_IMAGE_FOLDER = "own_images"
        private const val COLLECTED_IMAGE_FOLDER = "collected_images"
        private const val ENCRYPTED_PREFIX = "encrypted"
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
                this.isAdvertising = true
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

    /**
     * Fires an intent to spin up the file chooser UI and select an image for sending to endpointId.
     */
    private fun showImageChooser(endpointId: String) {
        this.eid = endpointId
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        intent.putExtra(ENDPOINT_ID_EXTRA, endpointId)
        startActivityForResult(intent, READ_REQUEST_CODE)
        Log.d(TAG, "end img")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == READ_REQUEST_CODE && resultCode == RESULT_OK && resultData != null) {
//            val endpointId = resultData.getStringExtra(ENDPOINT_ID_EXTRA)
            val endpointId = this.eid
            Log.d("EID", endpointId.toString())

            // The URI of the file selected by the user.
            val uri = resultData.data
            val filePayload: Payload
            filePayload = try {
                // Open the ParcelFileDescriptor for this URI with read access.
                val pfd = contentResolver.openFileDescriptor(uri!!, "r")
                Payload.fromFile(pfd!!)
            } catch (e: FileNotFoundException) {
                Log.e("MyApp", "File not found", e)
                return
            }

            // Construct a simple message mapping the ID of the file payload to the desired filename.
            val filenameMessage = filePayload.id.toString() + ":" + uri.lastPathSegment

            // Send the filename message as a bytes payload.
            val filenameBytesPayload =
                Payload.fromBytes(filenameMessage.toByteArray(StandardCharsets.UTF_8))
            Nearby.getConnectionsClient(context).sendPayload(endpointId!!, filenameBytesPayload)

            // Finally, send the file payload.



            if(endpointId != null) {
                Log.d(TAG, "in result")

                Nearby.getConnectionsClient(context).sendPayload(endpointId, filePayload).addOnSuccessListener {
                    Log.d(TAG, "successful send?")
                }
            }
        }
    }
    private fun sendPayLoad(endPointId: String, filePayload: Payload) {
        Log.d(TAG, context.filesDir.toString())
//        val fileToSend = File(context.filesDir, "C:/Users/jacob/Code/MineComms/app/src/main/java/com/example/minecomms/img.png")
        try {
            Log.d(TAG, "sending file?")
//            val filePayload = Payload.fromFile(fileToSend)
            Nearby.getConnectionsClient(context).sendPayload(endPointId, filePayload)
        } catch (e: FileNotFoundException) {
            Log.e("MyApp", "File not found", e)
        }

    }

    internal class ReceiveFilePayloadCallback(private val context: Context) :
        PayloadCallback() {
        private val incomingFilePayloads = SimpleArrayMap<Long, Payload>()
        private val completedFilePayloads = SimpleArrayMap<Long, Payload?>()
        private val filePayloadFilenames = SimpleArrayMap<Long, String>()
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val payloadFilenameMessage = String(payload.asBytes()!!, StandardCharsets.UTF_8)
                val payloadId = addPayloadFilename(payloadFilenameMessage)
                processFilePayload(payloadId)
            } else if (payload.type == Payload.Type.FILE) {
                Log.d("CON", "IN PAY REC")
                // Add this to our tracking map, so that we can retrieve the payload later.
                incomingFilePayloads.put(payload.id, payload)
            }
        }

        /**
         * Extracts the payloadId and filename from the message and stores it in the
         * filePayloadFilenames map. The format is payloadId:filename.
         */
        private fun addPayloadFilename(payloadFilenameMessage: String): Long {
            val parts = payloadFilenameMessage.split(":").toTypedArray()
            val payloadId = parts[0].toLong()
            val filename = parts[1]
            filePayloadFilenames.put(payloadId, filename)
            return payloadId
        }

        private fun processFilePayload(payloadId: Long) {
            // BYTES and FILE could be received in any order, so we call when either the BYTES or the FILE
            // payload is completely received. The file payload is considered complete only when both have
            // been received.
            val filePayload = completedFilePayloads[payloadId]
            val filename = filePayloadFilenames[payloadId]
            if (filePayload != null && filename != null) {
                completedFilePayloads.remove(payloadId)
                filePayloadFilenames.remove(payloadId)

                // Get the received file (which will be in the Downloads folder)
                // Because of https://developer.android.com/preview/privacy/scoped-storage, we are not
                // allowed to access filepaths from another process directly. Instead, we must open the
                // uri using our ContentResolver.
                val uri = filePayload.asFile()!!.asUri()
                try {
                    // Copy the file to a new location.
                    val `in`: InputStream? = context.contentResolver.openInputStream(uri!!)
                    copyStream(
                        `in`, FileOutputStream(
                            File(
                                context.cacheDir, filename
                            )
                        )
                    )
                } catch (e: IOException) {
                    // Log the error.
                } finally {
                    // Delete the original file.
                    context.contentResolver.delete(uri!!, null, null)
                }
            }
        }

        // add removed tag back to fix b/183037922
        private fun processFilePayload2(payloadId: Long) {
            // BYTES and FILE could be received in any order, so we call when either the BYTES or the FILE
            // payload is completely received. The file payload is considered complete only when both have
            // been received.
            val filePayload = completedFilePayloads[payloadId]
            val filename = filePayloadFilenames[payloadId]
            if (filePayload != null && filename != null) {
                completedFilePayloads.remove(payloadId)
                filePayloadFilenames.remove(payloadId)

                // Get the received file (which will be in the Downloads folder)
                if (VERSION.SDK_INT >= VERSION_CODES.Q) {
                    // Because of https://developer.android.com/preview/privacy/scoped-storage, we are not
                    // allowed to access filepaths from another process directly. Instead, we must open the
                    // uri using our ContentResolver.
                    val uri = filePayload.asFile()!!.asUri()
                    try {
                        // Copy the file to a new location.
                        val `in`: InputStream? = context.contentResolver.openInputStream(uri!!)
                        copyStream(
                            `in`, FileOutputStream(
                                File(
                                    context.cacheDir, filename
                                )
                            )
                        )
                    } catch (e: IOException) {
                        // Log the error.
                    } finally {
                        // Delete the original file.
                        context.contentResolver.delete(uri!!, null, null)
                    }
                } else {
                    val payloadFile = filePayload.asFile()!!.asJavaFile()

                    // Rename the file.
                    payloadFile!!.renameTo(File(payloadFile.parentFile, filename))
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
                val payloadId = update.payloadId
                val payload = incomingFilePayloads.remove(payloadId)
                completedFilePayloads.put(payloadId, payload)
                if (payload!!.type == Payload.Type.FILE) {
                    processFilePayload(payloadId)
                }
            }
        }

        companion object {
            /** Copies a stream from one location to another.  */
            @Throws(IOException::class)
            private fun copyStream(`in`: InputStream?, out: OutputStream) {
                try {
                    val buffer = ByteArray(1024)
                    var read: Int
                    while (`in`!!.read(buffer).also { read = it } != -1) {
                        out.write(buffer, 0, read)
                    }
                    out.flush()
                } finally {
                    `in`!!.close()
                    out.close()
                }
            }
        }
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
                        if (isAdvertising) {
//                            sendPayLoad(endpointId)
                            showImageChooser(endpointId)
                        }
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
        private val context: Context? = null
        private var incomingFilePayloads = SimpleArrayMap<Long, Payload>()
        private var completedFilePayloads = SimpleArrayMap<Long, Payload>()
        private var filePayloadFilenames = SimpleArrayMap<Long, String>()
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            // A new payload is being sent over.
            Log.d(TAG, "Payload Received")
            when (payload.type) {
                Payload.Type.BYTES -> {
                    val payloadFilenameMessage = String(payload.asBytes()!!, StandardCharsets.UTF_8)
                    val payloadId: Long = addPayloadFilename(payloadFilenameMessage)
                    processFilePayload(payloadId)
//                    var rcvdFilename = String(payload.asBytes()!!, StandardCharsets.UTF_8)
                    Log.d(TAG, payload.asBytes().toString())
                    val dataDisplay: TextView = findViewById<TextView>(R.id.data_received)
                    dataDisplay.text = payload.asBytes().toString()
                }
                Payload.Type.FILE -> {
                    Log.d(TAG, "receiving file?")
                    // Add this to our tracking map, so that we can retrieve the payload later.
                    incomingFilePayloads.put(payload.id, payload);
                }
//                Payload.Type.STREAM -> {
//                    Log.d(TAG, "Inside file mode")
//                }
            }
        }

        /**
         * Extracts the payloadId and filename from the message and stores it in the
         * filePayloadFilenames map. The format is payloadId:filename.
         */
        private fun addPayloadFilename(payloadFilenameMessage: String): Long {
            val parts = payloadFilenameMessage.split(":").toTypedArray()
            val payloadId = parts[0].toLong()
            val filename = parts[1]
            filePayloadFilenames.put(payloadId, filename)
            return payloadId
        }

        private fun copyStream(`in`: InputStream?, out: OutputStream) {
            try {
                val buffer = ByteArray(1024)
                var read: Int
                while (`in`!!.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                }
                out.flush()
            } finally {
                `in`!!.close()
                out.close()
            }
        }

        private fun processFilePayload(payloadId: Long) {
            // BYTES and FILE could be received in any order, so we call when either the BYTES or the FILE
            // payload is completely received. The file payload is considered complete only when both have
            // been received.
            val filePayload = completedFilePayloads[payloadId]
            val filename: String? = filePayloadFilenames.get(payloadId)
            if(filename != null)
                Log.d("PFP", filename)
            if (filePayload != null && filename != null) {
                completedFilePayloads.remove(payloadId)
                filePayloadFilenames.remove(payloadId)

                // Get the received file (which will be in the Downloads folder)
                // Because of https://developer.android.com/preview/privacy/scoped-storage, we are not
                // allowed to access filepaths from another process directly. Instead, we must open the
                // uri using our ContentResolver.
                val uri: Uri? = filePayload.asFile()!!.asUri()
                try {
                    // Copy the file to a new location.
                    val `in` = uri?.let { context?.contentResolver?.openInputStream(it) }
                    copyStream(`in`, FileOutputStream(File(context?.cacheDir, filename)))
                } catch (e: IOException) {
                    // Log the error.
                } finally {
                    // Delete the original file.
                    if (uri != null) {
                        context?.contentResolver?.delete(uri, null, null)
                    }
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
                val payloadId = update.payloadId
                val payload = incomingFilePayloads.remove(payloadId)
                completedFilePayloads.put(payloadId, payload)
//                if (payload!!.type == Payload.Type.FILE) {
//                    processFilePayload(payloadId)
//                }
                processFilePayload(payloadId)

            }
        }
    }

//        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
//            val dataDisplay: TextView = findViewById<TextView>(R.id.data_received)
//
//
////            if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
////                val payloadId = update.payloadId
////                val payload = incomingFilePayloads.remove(payloadId)
////                completedFilePayloads.put(payloadId, payload)
////                if (payload != null && payload.type == Payload.Type.FILE) {
//////                    val isDone = processFilePayload(payloadId, endpointId)
////                    val isDone = true // REPLACE JUST A TEST
////                    if (isDone) {
////                        Log.d(TAG, "above")
////                        Log.d(TAG, payload.toString())
////                        Log.d(TAG, "below")
////                        Nearby.getConnectionsClient(context).disconnectFromEndpoint(endpointId) //test
////                    }
////                }
////            }
//
//            if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
//                val payloadId = update.payloadId
//                val payload = incomingFilePayloads.remove(payloadId)
//                completedFilePayloads.put(payloadId, payload)
//                Log.d(TAG, "successful send")
////                count++
//                dataDisplay.text = payload?.asBytes().toString()
//
//                if (payload != null && payload.type == Payload.Type.FILE) {
////                    val isDone = processFilePayload(payloadId, endpointId)
//                    val isDone = false
//
//                    if (isDone) {
//                        Nearby.getConnectionsClient(context!!)
//                            .disconnectFromEndpoint(endpointId) //test
//                    }
//                }
//            }
////            if (update.status === PayloadTransferUpdate.Status.SUCCESS) {
////                val payloadId = update.payloadId
////                val payload = incomingFilePayloads.remove(payloadId)
////                completedFilePayloads.put(payloadId, payload)
////                if (payload!!.type == Payload.Type.FILE) {
////                    processFilePayload(payloadId)
////                }
////            }
//        }
//    }

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