package com.example.bletest

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
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


        val filter: WSFKF = WSFKF()
        filter.initFilter()
        val dataStream = WSDataStream()
        dataStream.setFilteredDataCB(filter, {

        })

        dataStream.setRawDataCB {
            Log.d(TAG, "mag data: ${it.mx}, ${it.my}, ${it.mz}")
        }

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


        connectSwitch.setOnCheckedChangeListener { compoundButton, b ->
            if (b) {
                connectBLE()
            } else {
                connectedBLE.disconnect()
            }
        }

        streamSwitch.setOnCheckedChangeListener { compoundButton, b ->

            if (b) {
                Log.d(TAG, "start data stream")
                connectedBLE.startDataStream { arrayOfWSBLEDatas, error ->
                    Log.d(TAG, "Get parsed result")
                    dataStream.receiveData(arrayOfWSBLEDatas)
                }

            } else {
                Log.d(TAG, "stop data stream")
                connectedBLE.stopDataStream()
            }

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
