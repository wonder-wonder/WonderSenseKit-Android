package xyz.wonder_wonder.android.wondersensekit

import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.nio.ByteBuffer
import kotlin.Error


private const val TAG = "WSBLE"
private const val DeviceName = "WristBand"
private const val NativeLibName = "native"

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class WSBLE(peripheral: BluetoothDevice) : WSBLEDelegate {
    // fields
    // gatt connection
    private var wsblegatt: WSBLEGatt = WSBLEGatt(peripheral)

    private var connectCBFunc: ((status: Int, err: Error?) -> Unit)? = null
    private var dataStreamCBFunc: ((data: Array<WSBLEData>, err: Error?) -> Unit)? = null

    private var isConnecting: Boolean = false
    private var isStreaming: Boolean = false

    val name: String
        get() = wsblegatt.name

    val address: String
        get() = wsblegatt.address

    class DevInfo {
        var ManufacturerName: String = ""
        var HardwareRevision: String = ""
        var FirmwareRevision: String = ""
        var SystemID: String = ""
    }
    class SettingsInfo {
        var Frequency: Int = 0
        var AccMax: Int = 0
        var GyroMax: Int = 0
        var MagXcoef: Float = 0.0F
        var MagYcoef: Float = 0.0F
        var MagZcoef: Float = 0.0F
        var LowPassFilter: Int = 0
        var Mag: ByteArray = ByteArray(0)
    }


    init {
        wsblegatt.delegate = this
    }


    // native code
    private external fun initParser(acc: Int, gyro: Int, magX: Float, magY: Float, magZ: Float): Unit
    private external fun testParser(value: ByteArray)
    private external fun native_parse(value: ByteArray) : Array<WSBLEData>
    companion object {
        var isFinding: Boolean = false
        private var find_cbFunc: ((wsClass: WSBLE, err: Error?) -> Unit)? = null

        private lateinit var bleManager: BluetoothManager
        private lateinit var bleAdapter: BluetoothAdapter
        private var bleScanner: BluetoothLeScanner? = null

        init {
            System.loadLibrary(NativeLibName)
        }

        private val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)

                if (result != null && result.device.name == DeviceName) {
                    val wsble = WSBLE(result.device)
                    find_cbFunc?.invoke(wsble, null)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.d(TAG, "onScanFailed ErrorCode is ${errorCode.toString()}")
                super.onScanFailed(errorCode)
            }
        }

        fun startFind(context: Context, cbFunc: (wsClass: WSBLE, err: Error?) -> Unit) {
            Log.d(TAG, "Start finding...")
            if (bleScanner == null) {
                bleManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                bleAdapter = bleManager.adapter

                if (!checkBLESupport(context)) {
                    throw Error("BLE is not supported.")
                }

                bleScanner = bleAdapter.bluetoothLeScanner
            }

            if (!bleAdapter.isEnabled) {
                bleAdapter.enable()
            }

            isFinding = true
            find_cbFunc = cbFunc
            scanNewDevice()
        }

        fun stopFind() {
            if (bleScanner != null && isFinding) {
                isFinding = false
                find_cbFunc = null
                bleScanner!!.stopScan(scanCallback)
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
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED).build()
            bleScanner?.startScan(null, settings, scanCallback)
        }
    }

    // public functions
    fun connect(context: Context, cbFunc: (status: Int, err: Error?) -> Unit) {
        connectCBFunc = cbFunc

        stopFind()
        isConnecting = true
        isStreaming = false

        wsblegatt.connectToDevice(context)
    }

    fun disconnect() {
        wsblegatt.disconnectToDevice()
        isConnecting = false
        isStreaming = false
    }

    fun startDataStream(cbFunc: (Array<WSBLEData>, Error?) -> Unit) {
        Log.d(TAG, "startDataStream")
        dataStreamCBFunc = cbFunc
        wsblegatt.writeChar(WSBLEProfile.SENSOR_MANAGER_SERVICE_UUID,
            WSBLEProfile.MS_RUNNING_UUID,
            byteArrayOf(1)
        )
    }

    fun stopDataStream() {
        Log.d(TAG, "stopDataStream")
        wsblegatt.writeChar(WSBLEProfile.SENSOR_MANAGER_SERVICE_UUID,
            WSBLEProfile.MS_RUNNING_UUID,
            byteArrayOf(0)
        )

        dataStreamCBFunc = null
    }

    fun sleep() {
        wsblegatt.writeChar(WSBLEProfile.SENSOR_MANAGER_SERVICE_UUID, WSBLEProfile.MS_SLEEP_UUID, byteArrayOf(0x1.toByte()))
    }

    fun reboot() {
        wsblegatt.writeChar(WSBLEProfile.SENSOR_MANAGER_SERVICE_UUID, WSBLEProfile.MS_REBOOT_UUID, byteArrayOf(0x1.toByte()))
    }


    // from this line, the functions are useful.
    fun settings(value: Map<String, Int>) {
        val list = mutableListOf<WSBLEGatt.BLETask>()

        for ((k, v) in value) {
            when (k) {
                "freq" -> {
                    list.add(WSBLEGatt.BLETask(1,
                        WSBLEProfile.SENSOR_MANAGER_SERVICE_UUID,
                        WSBLEProfile.MS_MPU_FREQUENCY_UUID,
                        ByteBuffer.allocate(32).putInt(v).array())
                    )
                }
                "acc" -> {
                    list.add(WSBLEGatt.BLETask(1,
                        WSBLEProfile.SENSOR_MANAGER_SERVICE_UUID,
                        WSBLEProfile.MS_ACC_FSR_UUID,
                        ByteBuffer.allocate(32).putInt(v).array())
                    )
                }
                "gyro" -> {
                    list.add(WSBLEGatt.BLETask(1,
                        WSBLEProfile.SENSOR_MANAGER_SERVICE_UUID,
                        WSBLEProfile.MS_GYRO_FSR_UUID,
                        ByteBuffer.allocate(32).putInt(v).array())
                    )
                }
                "lpf" -> {
                    list.add(WSBLEGatt.BLETask(1,
                        WSBLEProfile.SENSOR_MANAGER_SERVICE_UUID,
                        WSBLEProfile.MS_LOW_PASS_FILTER_UUID,
                        ByteBuffer.allocate(32).putInt(v).array())
                    )
                }
            }

        }
        wsblegatt.readwriteUpdate(list, fun() {
            val tasks = listOf(
                WSBLEGatt.BLETask(0,
                    WSBLEProfile.SENSOR_MANAGER_SERVICE_UUID,
                    WSBLEProfile.MS_SAVE_UUID,
                    byteArrayOf(1)
                ),
                WSBLEGatt.BLETask(0,
                    WSBLEProfile.SENSOR_MANAGER_SERVICE_UUID,
                    WSBLEProfile.MS_REBOOT_UUID,
                    byteArrayOf(1)
                )
            )
            wsblegatt.readwriteUpdate(tasks, null)
        })
    }

    override fun WSBLEGattRowData(byteArray: ByteArray) {
        val sensorData = native_parse(byteArray)
        Log.d(TAG, "Get parsed result")
        for (item in sensorData) {
            Log.d(TAG, "Test result.ax >> ${item.ax}")
        }
        dataStreamCBFunc?.invoke(sensorData, null);

//        val sensorWSBLEData = Array<WSBLEData>(4) {
//            WSBLEData("",
//                0.0,0.0,0.0,
//                0.0,0.0,0.0,
//                0.0,0.0, 0.0,
//                0.0, 0.0, 0.0)
//        }

//        dataStreamCBFunc?.invoke(sensorWSBLEData, null)
    }

    override fun WSBLEGattStatus(status: WSBLEGatt.WSBLEGattStatus) {
        // TODO: change status code.
        when (status) {
            WSBLEGatt.WSBLEGattStatus.CompleteInitialProcess -> {
                connectCBFunc?.invoke(0, null)
            }
        }
    }

    override fun WSBLEGattUpdateDevInfo(devInfo: DevInfo) {
        // TODO: impl
    }

    override fun WSBLEGattUpdateSettingsInfo(settingsInfo: SettingsInfo) {
        initParser(settingsInfo.AccMax, settingsInfo.GyroMax,
            settingsInfo.MagXcoef, settingsInfo.MagYcoef, settingsInfo.MagZcoef)
//        testParser(settingsInfo.Mag)

    }
}
