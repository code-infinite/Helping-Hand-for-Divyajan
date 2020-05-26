package com.helpinghand.pysenses


import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import cameraServices.Camera2Api
import cameraServices.CameraApi
import cameraServices.CameraController.Companion.flag
import cameraServices.CameraController.Companion.imag
import cameraServices.CameraController.Companion.instance
import cameraServices.CameraController.Companion.recreate
import cameraServices.CameraController.Companion.singleDetect
import cameraServices.isServiceRunning
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.atan2
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {

    companion object {
        const val CODE_PERM_SYSTEM_ALERT_WINDOW = 6112
        var ins: MainActivity? = null
        var cameraRotation: Int = 90
    }

    private lateinit var permissionHandler: PermissionHandler
    private var camera2Support = false
    private var cameraClass: Class<*>? = null
    private lateinit var manager: SensorManager

    private val sensorEvent = object : SensorEventListener {
        private val ORIENTATION_UNKNOWN = -1
        private val X_AXIS_INDEX = 0
        private val Y_AXIS_INDEX = 1
        private val Z_AXIS_AXIS = 2
        private var previousRotation = 0
        private var rotationDeg = 0
        private var rotationRoundedClockwise = 0

        private fun calculateRoundedRotation(newRotationDeg: Int): Int {
            return if (newRotationDeg <= 45 || newRotationDeg > 315) { // round to 0
                0  // portrait
            } else if (newRotationDeg in 46..135) { // round to 90
                90  // clockwise landscape
            } else if (newRotationDeg in 136..225) { // round to 180
                180  // upside down portrait
            } else if (newRotationDeg in 226..315) { // round to 270
                270  // anticlockwise landscape
            } else {
                0
            }
        }


        override fun onAccuracyChanged(sensor: Sensor?, i: Int) {}

        override fun onSensorChanged(sensorEvent: SensorEvent) {
            val calculateNewRotationDegree = calculateNewRotationDegree(sensorEvent)
            if (calculateNewRotationDegree != rotationDeg) {
                rotationDeg = calculateNewRotationDegree
                rotationRoundedClockwise = calculateRoundedRotation(calculateNewRotationDegree)
            }
            val cameraRotation = getCameraRotation()
            if (previousRotation != cameraRotation) {
                previousRotation = cameraRotation
                this@MainActivity.setCameraRotation(cameraRotation)
            }
        }

        private fun calculateNewRotationDegree(event: SensorEvent): Int {
            val values = event.values
            var newRotationDeg = ORIENTATION_UNKNOWN
            val X = -values[X_AXIS_INDEX]
            val Y = -values[Y_AXIS_INDEX]
            val Z = -values[Z_AXIS_AXIS]
            val magnitude = X * X + Y * Y
            // Don't trust the angle if the magnitude is small compared to the y value
            if (magnitude * 4 >= Z * Z) {
                val ONE_EIGHTY_OVER_PI = 57.29577957855f
                val angle = atan2((-Y).toDouble(), X.toDouble()).toFloat() * ONE_EIGHTY_OVER_PI
                newRotationDeg = 90 - angle.roundToInt()
                // normalize to 0 - 359 range
                while (newRotationDeg >= 360) {
                    newRotationDeg -= 360
                }
                while (newRotationDeg < 0) {
                    newRotationDeg += 360
                }
            }
            return newRotationDeg
        }

        private fun getCameraRotation(): Int {
            return when (rotationRoundedClockwise) {
                0 -> 90
                90 -> 180
                180 -> 270
                270 -> 0
                else -> 90
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
        permissionHandler = PermissionHandler(this)
        permissionHandler.requestCameraPermission()
        (getSystemService(SENSOR_SERVICE) as? SensorManager)?.let {
            manager = it
        }
        val manager =
            this.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        if (manager.cameraIdList.isEmpty()) {
            switchCam.isEnabled = false
            switchCam.setTextColor(Color.GRAY)
        }
        for (cameraId in manager.cameraIdList) {
            if (manager.getCameraCharacteristics(cameraId!!)
                    .get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL ||
                manager.getCameraCharacteristics(cameraId)
                    .get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3
            ) {
                camera2Support = true
                break
            }
        }
        camera2Support = false
        cameraClass = if (camera2Support) {
            Camera2Api::class.java
        } else
            CameraApi::class.java
        ins = this
        flag = 0
        previewbutton.isEnabled = false
        singleshot.setTextColor(Color.BLACK)
        previewbutton.setTextColor(Color.GRAY)
        singledetection.visibility = View.GONE
        if (singleDetect) {
            switch1.visibility = View.GONE
            singledetection.visibility = View.VISIBLE
        }
        if (recreate) {
            switch1.toggle(false)
            singleshot.isEnabled = false
            previewbutton.isEnabled = true
            singleshot.setTextColor(Color.GRAY)
            previewbutton.setTextColor(Color.BLACK)
        }
    }

    override fun onStart() {
        super.onStart()
        imag = imageView
        imageView.visibility = View.GONE
        manager.registerListener(
            sensorEvent,
            manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHandler.onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun recreate() {
        super.recreate()
        recreate = true
    }

    private fun setbias(values: Float) {
        val constrain = ConstraintSet()
        constrain.clone(mainlayout)
        constrain.setVerticalBias(R.id.btnpanel, values)
        constrain.applyTo(mainlayout)
    }

    private fun initView() {
        switch1.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) { // The toggle is enabled
                statustxt.text = "Stop"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {

                    // Don't have permission to draw over other apps yet - ask user to give permission
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    startActivityForResult(settingsIntent, CODE_PERM_SYSTEM_ALERT_WINDOW)
                }
                if (!isServiceRunning(this, cameraClass as Class<*>)) {
                    val intent = Intent(this, cameraClass)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else
                        startService(intent)
                    switchCam.isEnabled = true
                    switchCam.setTextColor(Color.WHITE)
                    recreate()
                }
            } else { // The toggle is disabled
                stopService(Intent(this, cameraClass))
                //Toast.makeText(this, outpt,Toast.LENGTH_LONG).show()
                statustxt.text = "Start"
                switchCam.isEnabled = false
                switchCam.setTextColor(Color.GRAY)
                previewbutton.setTextColor(Color.GRAY)
                if (previewbutton.isChecked) {
                    previewbutton.toggle()
                }
                singleshot.isEnabled = true
                singleshot.setTextColor(Color.BLACK)
                previewbutton.isEnabled = false
            }
        }
        cameraControls.setOnCheckedChangeListener { _, checkedId, isChecked ->
            if (isChecked) {
                if (checkedId == previewbutton.id) {
                    imageView.visibility = View.VISIBLE
                    setbias(0.85f)
                }
                if (checkedId == singleshot.id) {
                    switch1.visibility = View.GONE
                    statustxt.visibility = View.GONE
                    singledetection.visibility = View.VISIBLE
                    previewbutton.isEnabled = true
                    previewbutton.setTextColor(Color.BLACK)
                    singleshot.isEnabled = true
                    singleshot.setTextColor(Color.BLACK)
                    singleDetect = true
                }
            } else {
                if (checkedId == previewbutton.id) {
                    imageView.visibility = View.GONE
                    setbias(0.5f)
                }
                if (checkedId == singleshot.id) {
                    singledetection.visibility = View.GONE
                    statustxt.visibility = View.VISIBLE
                    switch1.visibility = View.VISIBLE
                    previewbutton.isEnabled = false
                    previewbutton.setTextColor(Color.GRAY)
                    if (previewbutton.isChecked) {
                        previewbutton.toggle()
                    }
                    singleDetect = false
                    switchCam.isEnabled = false
                    switchCam.setTextColor(Color.GRAY)
                }
            }
        }
        switchCam.setOnClickListener {
            instance!!.switchCamera()
        }

        singledetection.setOnClickListener {
            flag = 1
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {

                // Don't have permission to draw over other apps yet - ask user to give permission
                val settingsIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                startActivityForResult(settingsIntent, CODE_PERM_SYSTEM_ALERT_WINDOW)
            }
            if (!isServiceRunning(this, cameraClass as Class<*>)) {
                val intent = Intent(this, cameraClass)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else
                    startService(intent)
            }
        }
    }

    fun setCameraRotation(i: Int) {
        cameraRotation = i
    }
}
