package xyz.wonder_wonder.android.wondersensekit

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
object WSBLEScanner {
    private val TAG = "WSBLEScanner"

    private lateinit var find_cbFunc: ((wsClass: WSBLE, err: Error?) -> Unit)
    private lateinit var bleManager: BluetoothManager
    private lateinit var bleAdapter: BluetoothAdapter
    private lateinit var bleScanner: BluetoothLeScanner

    var isFinding: Boolean = false

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)

            if (result != null && result.device.name == WSBLEConfig.DeviceName) {
                val wsble = WSBLE(result.device)
                find_cbFunc.invoke(wsble, null)
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)

            if (results != null) {
                for (it in results) {
                    find_cbFunc.invoke(WSBLE(it.device), null)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.d(TAG, "onScanFailed ErrorCode is ${errorCode.toString()}")
            super.onScanFailed(errorCode)
        }
    }

    fun startFind(context: Context, cbFunc: (wsClass: WSBLE, err: Error?) -> Unit) {
        Log.d(TAG, "Start finding...")

        bleManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bleAdapter = bleManager.adapter

        if (!checkBLESupport(context)) {
            throw Error("BLE is not supported.")
        }

        bleScanner = bleAdapter.bluetoothLeScanner
        if (!bleAdapter.isEnabled) {
            bleAdapter.enable()
        }

        isFinding = true
        find_cbFunc = cbFunc
        scanNewDevice()
    }

    fun stopFind() {
        if (isFinding) {
            isFinding = false
            find_cbFunc = fun (_, _) {}
            bleScanner.stopScan(scanCallback)
        }
    }

    private fun checkBLESupport(context: Context) : Boolean {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.d(TAG, "bluetooth le is not supported")
            return false
        }

        return true
    }

    private fun scanNewDevice() {
        val filter = ScanFilter.Builder()
            .setDeviceName(WSBLEConfig.DeviceName)
            .build()
        val settings = ScanSettings.Builder()
            .setReportDelay(2500)
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build()
        bleScanner.startScan(listOf(filter), settings, scanCallback)
    }
}