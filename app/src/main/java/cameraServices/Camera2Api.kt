package cameraServices

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.view.Surface
import androidx.core.content.ContextCompat
import java.nio.ByteBuffer

open class Camera2Api : CameraController() {

    // Camera2-related stuff
    private var cameraManager: CameraManager? = null
    private var cameraDevice: CameraDevice? = null
    private var captureRequest: CaptureRequest? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var camId: String = "0"

    //Image Listener  to get images form camera
    private val imageListener = ImageReader.OnImageAvailableListener { reader ->
        val image: Image? = reader?.acquireLatestImage()
        // Process image here..ideally async so that you don't block the callback
        // ..
        val planes = image!!.planes
        val bBuffer: ByteBuffer = planes[0].buffer
        bBuffer.rewind()
        val bytes = ByteArray(bBuffer.remaining())
        planes[0].buffer.get(bytes)
        processImage(bytes)
        image.close()
    }

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {

        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
        }
    }

    private val stateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(currentCameraDevice: CameraDevice) {
            cameraDevice = currentCameraDevice
            createCaptureSession()
        }

        override fun onDisconnected(currentCameraDevice: CameraDevice) {
            currentCameraDevice.close()
            cameraDevice = null
        }

        override fun onError(currentCameraDevice: CameraDevice, error: Int) {
            currentCameraDevice.close()
            cameraDevice = null
        }
    }

    override fun initCam() {
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        for (id in cameraManager!!.cameraIdList) {
            val characteristics = cameraManager!!.getCameraCharacteristics(id)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                continue
            } else
                cameraList.add(id)
        }
    }

    override fun openCam(cameraId: String) {
        camId = cameraId
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        )
            cameraManager!!.openCamera(cameraId, stateCallback, null)
        cameraIsOpen = true
    }

    override fun createCaptureSession() {
        try {

            val targetSurfaces = ArrayList<Surface>()
            val fps = cameraManager!!.getCameraCharacteristics(camId)
                .get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            sensorHeight =
                cameraManager!!.getCameraCharacteristics(camId)
                    .get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)!!.height
            // Prepare CaptureRequest that can be used with CameraCaptureSession
            val requestBuilder =
                cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {

                    // Configure target surface for background processing (ImageReader)
                    imageReader = ImageReader.newInstance(
                        width, height,
                        ImageFormat.JPEG, 1
                    )
                    imageReader!!.setOnImageAvailableListener(imageListener, null)

                    targetSurfaces.add(imageReader!!.surface)
                    addTarget(imageReader!!.surface)

                    // Set some additional parameters for the request
                    set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                    )
                    set(
                        CaptureRequest.CONTROL_AE_ANTIBANDING_MODE,
                        CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_AUTO
                    )
                    set(
                        CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fps!![1]
                    )
                    set(
                        CaptureRequest.CONTROL_AWB_MODE,
                        CameraMetadata.CONTROL_AWB_MODE_AUTO
                    )
                }

            // Prepare CameraCaptureSession
            cameraDevice!!.createCaptureSession(
                targetSurfaces,
                object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        // The camera is already closed
                        if (null == cameraDevice) {
                            return
                        }
                        captureSession = cameraCaptureSession
                        try {
                            // Now we can start capturing
                            captureRequest = requestBuilder.build()
                            captureSession!!.setRepeatingRequest(
                                captureRequest!!,
                                captureCallback,
                                null
                            )
                            focalLength = captureRequest?.get(CaptureRequest.LENS_FOCAL_LENGTH)!!

                        } catch (e: CameraAccessException) {
                        }
                    }

                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                    }
                }, null
            )
        } catch (e: CameraAccessException) {
        }
    }

    override fun closeCamera() {
        try {
            captureSession?.close()
            captureSession = null

            cameraDevice?.close()
            cameraDevice = null

            imageReader?.close()
            imageReader = null

            cameraIsOpen = false

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}

