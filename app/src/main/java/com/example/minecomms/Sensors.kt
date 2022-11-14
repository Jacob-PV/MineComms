package com.example.minecomms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
//import com.example.minecomms.databinding.ActivityConnectionBinding
import com.example.minecomms.databinding.ActivitySensorsBinding
import java.sql.Timestamp
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.pow
import kotlin.math.sqrt

//class Sensors : AppCompatActivity() {
//    private lateinit var viewBinding: ActivityConnectionBinding
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_sensors)
//
//        viewBinding = ActivityConnectionBinding.inflate(layoutInflater)
//        setContentView(viewBinding.root)
//
//        viewBinding.discoverButton.setOnClickListener { startDiscovery() }
//    }
//
//    private fun displa
//}

class Sensors : AppCompatActivity(), SensorEventListener {

    private val MY_PERMISSIONS_REQUEST_ACTIVITY_RECOGNITION = 90

    private lateinit var viewBinding: ActivitySensorsBinding

    private lateinit var sensorManager: SensorManager
    private val accelerometerReading = FloatArray(3)
    private val linearAccelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var xTot = 0.0
    private var yTot = 0.0
    private var startTime = Timestamp(0)
    private var curTime = Timestamp(0)
    private var timeDiff = 0.0
    private var hyp = 0.0
    private var v0 = 0.0
    private var angle = 0.0

    private var v0x = 0.0
    private var v0y = 0.0
    private var count = 0

    private var step_count = 0

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensors)

        viewBinding = ActivitySensorsBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.readButton.setOnClickListener { displayOrientation() }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        checkPermissions()
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do something here if sensor accuracy changes.
        // You must implement this callback in your code.
    }

    override fun onResume() {
        super.onResume()

        // Get updates from the accelerometer and magnetometer at a constant rate.
        // To make batch operations more efficient and reduce power consumption,
        // provide support for delaying updates to the application.
        //
        // In this example, the sensor reporting delay is small enough such that
        // the application receives an update before the system checks the sensor
        // readings again.
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
            sensorManager.registerListener(
                this,
                magneticField,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)?.also { accelerometer ->
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.also { pedometer ->
            sensorManager.registerListener(
                this,
                pedometer,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }


        displayOrientation()
    }

    override fun onPause() {
        super.onPause()

        // Don't receive any more updates from either sensor.
        sensorManager.unregisterListener(this)
    }

    // Get readings from accelerometer and magnetometer. To simplify calculations,
    // consider storing these readings as unit vectors.
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
        } else if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            System.arraycopy(event.values, 0, linearAccelerometerReading, 0, linearAccelerometerReading.size)
            if (v0 != 0.0 || linearAccelerometerReading[0] != linearAccelerometerReading[1])
                updateAccel()
        } else if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            step_count = event.values[0].toInt()
            Log.d("STEPS", step_count.toString())
        }
//        displayOrientation()
    }

    private fun calibrate() {

    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                MY_PERMISSIONS_REQUEST_ACTIVITY_RECOGNITION)
        }
    }

    private fun updateAccel() {
        calibrate()
        updateOrientationAngles()
        //https://stackoverflow.com/questions/14963190/calculate-acceleration-in-reference-to-true-north
        linearAccelerometerReading[0] *= rotationMatrix[0]
        linearAccelerometerReading[1] *= rotationMatrix[1]
        linearAccelerometerReading[2] *= rotationMatrix[2]

        linearAccelerometerReading[0] = rotationMatrix[0] * linearAccelerometerReading[0] + rotationMatrix[1] * linearAccelerometerReading[1] + rotationMatrix[2] * linearAccelerometerReading[2];
        linearAccelerometerReading[1] = rotationMatrix[3] * linearAccelerometerReading[0] + rotationMatrix[4] * linearAccelerometerReading[1] + rotationMatrix[5] * linearAccelerometerReading[2];
        linearAccelerometerReading[2] = rotationMatrix[6] * linearAccelerometerReading[0] + rotationMatrix[7] * linearAccelerometerReading[1] + rotationMatrix[8] * linearAccelerometerReading[2];

        var tmp = ""
//        if (linearAccelerometerReading[0] > 1)
//            tmp += "N "
//        if (linearAccelerometerReading[0] < -1)
//            tmp += "S "
//        if (linearAccelerometerReading[0] > 1)
//            tmp += "E "
//        if (linearAccelerometerReading[0] < -1)
//            tmp += "W "

        for (item in linearAccelerometerReading) {
            tmp += String.format("%.2f", item) + ", "
        }


        Log.d("RM", tmp)

        xTot += linearAccelerometerReading[0]
        yTot += linearAccelerometerReading[1]

        val orientationDisplay: TextView = findViewById<TextView>(R.id.orientation_display)
        orientationDisplay.text = String.format("%.2f", xTot) + ", " + String.format("%.2f", yTot)

        v0 = calcDistance()
//        if (v0 < 0.000001) {
//            Log.d("IN HERE", "V0")
//            v0 = getv0()
//        }
//        else
//            return
//
//        if (startTime == Timestamp(0)) {
//            startTime = Timestamp(System.currentTimeMillis())
//        }
//        curTime = Timestamp(System.currentTimeMillis())
//        timeDiff = ((curTime.time - startTime.time) / 1000.0)
//        Log.d("TIME", timeDiff.toString())
//
//        xTot += linearAccelerometerReading[0]
//        yTot += linearAccelerometerReading[1]
//
//        hyp = sqrt(xTot.pow(2) + yTot.pow(2))
//        Log.d("hyp", hyp.toString())
    }

    private fun calcDistance(): Double {
        if (startTime == Timestamp(0)) {
            startTime = Timestamp(System.currentTimeMillis())

        }
        curTime = Timestamp(System.currentTimeMillis())
        timeDiff = ((curTime.time - startTime.time) / 1000.0)

        Log.d("AXIS", linearAccelerometerReading[0].toString() + ", " + linearAccelerometerReading[1].toString() + ", " + linearAccelerometerReading[2].toString())
        xTot += linearAccelerometerReading[0].toDouble()
        yTot += linearAccelerometerReading[1].toDouble()
        hyp = sqrt(xTot.pow(2) + yTot.pow(2))
        angle = atan(abs(xTot) / abs(yTot)) * 180 / 3.14
//        Log.d("V01", angle.toString())

        if (xTot > 0 && yTot > 0) {
            Log.d("QUAD", "1")
            angle = angle
        }
        else if (xTot > 0 && yTot < 0) {
            Log.d("QUAD", "2")
            angle = 180 - angle
        }
        else if (xTot > 0 && yTot < 0) {
            Log.d("QUAD", "3")
            angle += 180
        }
        else if (xTot < 0 && yTot > 0) {
            Log.d("QUAD", "4")
            angle = 360 - angle
        }
        Log.d("QUAD", xTot.toString() + ", " + yTot.toString())


        Log.d("V0", angle.toString())
        return hyp
//        if (timeDiff >= 1) {
//
//            var x = 0.0
//            var y = 0.0
//            // not first run
//            if (v0y != v0x) {
//                x = xTot + 0.5 * v0x
//                y = yTot + 0.5 * v0y
//                hyp = sqrt(x.pow(2) + y.pow(2))
//                angle = atan(abs(y) / abs(x)) * 90
////                Log.d("V01", angle.toString() + ", " + x.toString() + ", " + y.toString())
//
//                if (x > 0 && y > 0)
//                    angle = 90 - angle
//                else if (x > 0 && y < 0)
//                    angle += 90
//                else if (x < 0 && y < 0)
//                    angle = 270 - angle
//                else if (x < 0 && y > 0)
//                    angle += 270
//            }
//
//            v0 = hyp
//            Log.d("V0", v0.toString() + " m/s at " + angle.toString() + "°")
//
//            v0x = x
//            v0y = y
//            xTot = 0.0
//            yTot = 0.0
//            startTime = Timestamp(0)
//            return v0
//        }
        return 0.0000001
    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
    fun updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )

        // "rotationMatrix" now has up-to-date information.

        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        // "orientationAngles" now has up-to-date information.
    }

    private fun displayOrientation() {
        updateOrientationAngles()
        val orientationDisplay: TextView = findViewById<TextView>(R.id.orientation_display)
//         looking for Azimuth
        var azimuth = (this.orientationAngles[0] * 180 / 3.14).toInt()
        if (azimuth < 0) {
            azimuth += 360
        }
        orientationDisplay.text = "Orientation: " + azimuth.toString() + "°"
        // acc[0] = east
        // acc[1] = north
        // acc[2] = up
//        Log.d("ACC", accelerometerReading[0].toString() + ", " + accelerometerReading[1].toString() + ", " + accelerometerReading[2].toString())
        Log.d("LINACC", linearAccelerometerReading[0].toString() + ", " + linearAccelerometerReading[1].toString() + ", " + linearAccelerometerReading[2].toString())
    }
}