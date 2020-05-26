package cameraServices

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.*
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.helpinghand.pysenses.Detector
import com.helpinghand.pysenses.MainActivity
import com.helpinghand.pysenses.R
import com.helpinghand.pysenses.Speak


abstract class CameraController : Service() {

    companion object {

        //Preview Surface
        var imag: ImageView? = null

        //Testing
        var count = 0
        var outpt: String = ""

        //instance
        var instance: CameraController? = null

       var flag = 0
        var recreate=false
        var singleDetect=false
    }

    //Camera Parameter
    protected var cameraList = mutableListOf<String>()
    private var cameraId: MutableListIterator<String>? = null

    //Width and Height of image
    var width = 640
    var height = 480

    //Wake Lock for continues running
    private var wakeLock: PowerManager.WakeLock? = null

    //Flags
    var cameraIsOpen: Boolean = false

    private var handler: Handler? = null
    private var handlerThread: HandlerThread? = null

    private var computingDetection = false

    private var pythonInstance: Python? = null
    private var mainObject: PyObject? = null
    private var detectorObject: PyObject? = null

    //Camera parameters
    var focalLength = 0.0f
    var sensorHeight = 0.0f

    //Speach
    private var tts: Speak? = null

    //Singleshot
    private var firstRun = true

    //Initialize Camera
    protected abstract fun initCam()

    //Open Camera
    protected abstract fun openCam(cameraId: String)

    //Create  capture Session
    protected abstract fun createCaptureSession()

    //Close camera
    protected abstract fun closeCamera()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        count = 0
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Camera::lock").apply {
                acquire()
            }
        }
        handlerThread = HandlerThread("detector")
        handlerThread!!.priority = Thread.MAX_PRIORITY
        handlerThread!!.start()
        handler = Handler(handlerThread!!.looper)
        tts = Speak(this)
        initOverlay()
        initCam()
        cameraId = cameraList.listIterator()
        pythonInstance = Python.getInstance()
        mainObject = pythonInstance!!.getModule("main")
        detectorObject = mainObject!!.callAttr("Detect")
        switchCamera()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startMyOwnForeground() else startForeground(
            1,
            Notification()
        )
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        closeCamera()
        wakeLock!!.release()
        wakeLock = null
        pythonInstance = null
        mainObject = null
        tts!!.close()
        tts = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startMyOwnForeground() {
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }
        val notificationChannelID = "com.example.pysenses"

        val channelName = "Pysenses Service"

        val chan = NotificationChannel(
            notificationChannelID,
            channelName,
            NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE

        val manager: NotificationManager =
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        manager.createNotificationChannel(chan)

        val notificationBuilder = NotificationCompat.Builder(this, notificationChannelID)

        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.drawable.ic_camera_black_24dp)
            .setContentTitle("App is running in background")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(2, notification)
    }

    private fun initOverlay() {
        val li = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rootView = li.inflate(R.layout.overlay, null)


        val type = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        val params = WindowManager.LayoutParams(
            type,
            (WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE),
            PixelFormat.TRANSLUCENT
        )

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        outpt=wm.defaultDisplay.rotation.toString()
        wm.addView(rootView, params)
    }

    protected fun processImage(bytes: ByteArray) {
        if (flag == 1 && !firstRun) {
            closeCamera()
            if (!tts!!.status()) {
                this.stopSelf()
            } else
                return
        } else {
            if (computingDetection) {
                count++
                return
            }
            computingDetection = true
            runInBackground(
                Runnable {
                    Detector(bytes, detectorObject!!, focalLength, sensorHeight, tts)
                    computingDetection = false
                    firstRun = false
                }
            )

        }
    }

    fun switchCamera() {
        closeCamera()
        if (cameraId!!.hasNext())
            openCam(cameraId!!.next())
        else {
            cameraId = cameraList.listIterator()
            switchCamera()
        }
    }

    protected open fun runInBackground(r: Runnable) {
        if (handler != null) {
            handler!!.post(r)
        }
    }
}
