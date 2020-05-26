package com.helpinghand.pysenses

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat


class PermissionHandler internal constructor(private val activity: Activity) {

    /** Show a "rationale" to the user for needing a particular permission, then request that permission again
     * once they close the dialog.
     */

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private fun showRequestPermissionRationale(permission_code: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }
        var permissions: Array<String?>? = null
        var messageId = 0
        when (permission_code) {
            MY_PERMISSIONS_REQUEST_CAMERA -> {
                permissions = arrayOf(Manifest.permission.CAMERA)
                messageId = R.string.permission_rationale_camera
            }
            MY_PERMISSIONS_REQUEST_AUDIO -> {
                permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
                messageId = R.string.permission_rationale_Audio
            }
        }
        val permissionsF = permissions
        AlertDialog.Builder(activity)
            .setTitle(R.string.permission_rationale_title)
            .setMessage(messageId)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.ok, null)
            .setOnDismissListener {
                ActivityCompat.requestPermissions(
                    activity,
                    permissionsF!!,
                    permission_code
                )
            }.show()
    }

    fun requestCameraPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.CAMERA
            )
        ) {
            showRequestPermissionRationale(MY_PERMISSIONS_REQUEST_CAMERA)
        } else { // Can go ahead and request the permission
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE),
                MY_PERMISSIONS_REQUEST_CAMERA
            )
        }
    }

    fun requestAudioPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.RECORD_AUDIO
            )
        ) {
            showRequestPermissionRationale(MY_PERMISSIONS_REQUEST_AUDIO)
        } else { // Can go ahead and request the permission
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                MY_PERMISSIONS_REQUEST_AUDIO
            )
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_CAMERA -> {
                if (grantResults.firstOrNull() != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(activity, "No Camera Permission Granted", Toast.LENGTH_LONG).show()
                    activity.finish()
                    activity.overridePendingTransition(0, 0)
                }
            }
            MY_PERMISSIONS_REQUEST_AUDIO->{
                if (grantResults.firstOrNull() != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(activity, "No Audio Permission Granted", Toast.LENGTH_LONG).show()
                    activity.finish()
                    activity.overridePendingTransition(0, 0)
                }
            }
        }
        }

    companion object {
        private const val MY_PERMISSIONS_REQUEST_CAMERA = 0
        private const val MY_PERMISSIONS_REQUEST_AUDIO = 1
    }

}
