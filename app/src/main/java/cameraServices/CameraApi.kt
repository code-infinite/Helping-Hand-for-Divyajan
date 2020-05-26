package cameraServices

import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.graphics.YuvImage
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.view.TextureView
import java.io.ByteArrayOutputStream
import kotlin.math.tan


class CameraApi : CameraController() {

    // Camera-related stuff
    private var camera: Camera? = null
    private var para: Camera.Parameters? = null

    private var stopPicture = false

    private lateinit var textureView: TextureView

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(
            surface: SurfaceTexture?,
            width: Int,
            height: Int
        ) {
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
            return true
        }

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            startPreviewOfCamera()
        }

    }

    private val previewCallback = Camera.PreviewCallback { data, _ ->
        val yuv = YuvImage(data, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuv.compressToJpeg(Rect(0, 0, width, height), 100, out)
        processImage(out.toByteArray())
    }

    override fun initCam() {
        val cameraInfo = CameraInfo()
        for (id in 0 until Camera.getNumberOfCameras()) {
            Camera.getCameraInfo(id, cameraInfo)
            if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
                continue
            } else
                cameraList.add(id.toString())
        }
    }

    private fun findPreviewSizes() {
        val givenSize = camera!!.Size(width, height)
        val previewSizes = para!!.supportedPreviewSizes
        val aspectRatio = width.toFloat() / height.toFloat()
        var index = -1
        if (!previewSizes.contains(givenSize)) {
            previewSizes.reverse()
            for (size in previewSizes) {
                if ((size.width.toFloat() / size.height.toFloat()) == aspectRatio) {
                    if ((givenSize.width - size.width) < 0) {
                        index = previewSizes.indexOf(size)
                        break
                    }
                }
            }
            width = previewSizes[index].width
            height = previewSizes[index].height
        }
    }

    override fun openCam(cameraId: String) {
        camera = Camera.open(cameraId.toInt())
        cameraIsOpen = true
        para = camera!!.parameters
        findPreviewSizes()
        para!!.setPreviewSize(width, height)
        para!!.setPreviewFpsRange(8000, 30000)
        para!!.antibanding = Camera.Parameters.ANTIBANDING_AUTO
        para!!.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
        para!!.sceneMode = Camera.Parameters.SCENE_MODE_AUTO
        para!!.whiteBalance = Camera.Parameters.WHITE_BALANCE_AUTO
        focalLength=para!!.focalLength
        sensorHeight= (tan((para!!.verticalViewAngle/2)*0.0175)*2*focalLength).toFloat()
        camera!!.parameters = para
        createCaptureSession()
        outpt=para!!.focusMode
    }

    override fun createCaptureSession() {
        textureView = TextureView(this)
        textureView.surfaceTexture = SurfaceTexture(10)
        if (textureView.isAvailable) {
            startPreviewOfCamera()
        } else
            textureView.surfaceTextureListener = surfaceTextureListener

    }

    private fun startPreviewOfCamera() {
        camera!!.setPreviewTexture(textureView.surfaceTexture)
        camera!!.setPreviewCallback(previewCallback)
        camera!!.startPreview()
    }

    override fun closeCamera() {
        stopPicture = true
        if (camera != null) {
            camera!!.stopPreview()
            camera!!.setPreviewCallback(null)
            camera!!.release()
            camera = null
            cameraIsOpen = false
        }
    }
}
