package com.example.bletest

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import xyz.wonder_wonder.android.wondersensekit.*

private const val TAG = "MainActivity"

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class MainActivity : AppCompatActivity() {

    private lateinit var connectedBLE: WSBLE

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "this is test....")

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            Log.d(TAG, "Manifest.permission.ACCESS_FINE_LOCATION.")
            this.requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                123
            )
            Log.d(TAG, "Scanning after request permission")
        }

        val scanner = WSBLEScanner
        scanner.startFind(this) { _: WSBLE, _: Error? ->

        }
    }

    private fun connectBLE() {
        WSBLE.startFind(this, fun(wsClass: WSBLE, err: Error?) {
            connectedBLE = wsClass

            wsClass.connect(this, fun(status: Int, err: Error?) {
                Log.d(TAG, "WSBLE::connect status is ${status.toString()}")
                if (status == 100) {
                    Log.d(TAG, "connected")
                }
            })
        })
    }

}
