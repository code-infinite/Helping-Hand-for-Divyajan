package com.helpinghand.pysenses

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.AsyncTask
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_deaf.*
import org.kaldi.Assets
import org.kaldi.Model
import org.kaldi.RecognitionListener
import org.kaldi.SpeechRecognizer
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference


class DeafActivity : AppCompatActivity(), RecognitionListener{

    private val startState = 0
    private val readyState = 1
    private val micState = 3

    private var model: Model? = null
    private var recognizer: SpeechRecognizer? = null

    private lateinit var permissionHandler: PermissionHandler


    init {
        System.loadLibrary("kaldi_jni")
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_deaf)
        permissionHandler = PermissionHandler(this)
        permissionHandler.requestAudioPermission()
        setUiState(startState)
        recognize_mic.setOnClickListener { recognizeMicrophone(); }
        SetupTask(this).execute()
    }

    private class SetupTask internal constructor(activity: DeafActivity) :
        AsyncTask<Void?, Void?, java.lang.Exception?>() {
        var activityReference: WeakReference<DeafActivity>? = WeakReference(activity)
        override fun doInBackground(vararg params: Void?): java.lang.Exception? {
            try {
                val assets = Assets(activityReference!!.get())
                val assetDir: File? = assets.syncAssets()
                activityReference!!.get()!!.model =
                    Model(assetDir.toString() + "/modelandroid")
            } catch (e: IOException) {
                return e
            }
            return null
        }

        override fun onPostExecute(result: java.lang.Exception?) {
            if (result != null) {
                activityReference!!.get()!!.setErrorState(
                    java.lang.String.format(
                        activityReference!!.get()!!.getString(R.string.failed), result
                    )
                )
            } else {
                activityReference!!.get()!!.setUiState(1)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (recognizer != null) {
            recognizer!!.cancel()
            recognizer!!.shutdown()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHandler.onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onResult(hypothesis: String?) {
        val aa: Array<String>
        val bb: Array<String>
        val cc: Array<String>
        if (hypothesis!!.contains("]")) {
            aa = hypothesis.split("]").toTypedArray()
            if (aa[1].contains(":")) {
                bb = aa[1].split(":").toTypedArray()
                if (bb[1].contains("\"")) {
                    cc = bb[1].split("\"").toTypedArray()
                    result_text.append(
                        """
                            ${cc[1]}
                            
                            """.trimIndent()
                    )
                }
            }
        }
    }

    override fun onPartialResult(hypothesis: String?) {
    }

    override fun onTimeout() {
        recognizer!!.cancel()
        recognizer = null
        setUiState(readyState)
    }

    override fun onError(e: Exception?) {
        setErrorState(e!!.message!!)
    }

    private fun setUiState(state: Int) {
        when (state) {
            startState -> {
                result_text.setText(R.string.preparing)
                recognize_mic.isEnabled = false
            }
            readyState -> {
                result_text.setText(R.string.ready)
                recognize_mic.setText(R.string.recognize_microphone)
                recognize_mic.isEnabled = true
            }
            micState -> {
                recognize_mic.setText(R.string.stop_microphone)
                recognize_mic.isEnabled = true
            }
        }
    }

    private fun setErrorState(message: String) {
        result_text.text = message
        recognize_mic.setText(R.string.recognize_microphone)
        recognize_mic.isEnabled = false
    }

    private fun recognizeMicrophone() {
        if (recognizer != null) {
            setUiState(readyState)
            recognizer!!.cancel()
            recognizer = null
        } else {
            setUiState(micState)
            try {
                recognizer = SpeechRecognizer(model)
                recognizer!!.addListener(this)
                recognizer!!.startListening()
            } catch (e: IOException) {
                setErrorState(e.message!!)
            }
        }
    }
}
