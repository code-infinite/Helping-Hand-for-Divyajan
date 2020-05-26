package com.helpinghand.pysenses

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_select.*

class SelectType : AppCompatActivity() {

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_select)
        blindbtn.setOnClickListener {
            val intent=Intent(this,MainActivity::class.java)
            startActivity(intent)
        }
        dumbbtn.setOnClickListener {
            val intent=Intent(this,DumbActivity::class.java)
            startActivity(intent)
        }
        deafbtn.setOnClickListener {
            val intent=Intent(this,DeafActivity::class.java)
            startActivity(intent)
        }
    }
}
