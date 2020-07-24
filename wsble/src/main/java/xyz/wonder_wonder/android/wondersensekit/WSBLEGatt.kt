package xyz.wonder_wonder.android.wondersensekit

import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.nio.ByteBuffer
import java.util.*
import kotlin.experimental.and

private const val TAG = "WSBLEGatt"

interface WSBLEDelegate {
    fun WSBLEGattStatus (status: WSBLEGatt.WSBLEGattStatus)
    fun WSBLEGattUpdateDevInfo(devInfo: WSBLE.DevInfo)
    fun WSBLEGattUpdateSettingsInfo(settingsInfo: WSBLE.SettingsInfo)
    fun WSBLEGattRowData(byteArray: ByteArray)
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class WSBLEGatt(peripheral: BluetoothDevice) {
    // field
    var delegate: WSBLEDelegate? = null

    // inner
    private var bleGatt: BluetoothGatt? = null
    private var peripheral: BluetoothDevice = peripheral

    var devInfo = WSBLE.DevInfo()
    var settingsInfo = WSBLE.SettingsInfo()

    val name: String
        get() = peripheral.name

    val address: String
        get() = peripheral.address


    enum class WSBLEGattStatus {
        Disconnected,
        Connected,
        Connecting,
        CompleteInitialProcess
    }


    // function
    fun connectToDevice(context: Context) {
        bleGatt = peripheral.connectGatt(context.applicationContext, false, gattCallback)
    }

    fun disconnectToDevice() {
        bleGatt?.disconnect()
    }



    private val gattCallback = object : BluetoothGattCallback() {
        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "success")
                Log.d(TAG, "mtu is ${mtu}")

                if (gatt != null) {
                    bleGatt = gatt
                    gatt.discoverServices()
                }
            }
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    if (gatt != null) {
                        val flag = gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                        Log.d(TAG, "onConnectionStateChange, I request Connection Priority. The result is ${flag}")
                        if (gatt.requestMtu(100)) {
                            Log.d(TAG, "Requested MTU successfully")

                        } else {
                            Log.d(TAG, "failed to request MTU")
                        }
                    }
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "BLE Disconnected.")
                    bleGatt?.close()
                    bleGatt = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                val services = gatt.services
                if (services != null) {
                    var hasDevInfoService = false
                    var hasManagerService = false
                    var hasDataService = false

                    for (service in services) {
                        when (service.uuid) {
                            WSBLEProfile.SENSOR_DEVICE_INFO_SERVICE_UUID -> {
                                hasDevInfoService = true
                            }
                            WSBLEProfile.SENSOR_DATA_SERVICE_UUID -> {
                                hasDataService = true
                            }

                            WSBLEProfile.SENSOR_MANAGER_SERVICE_UUID -> {
                                hasManagerService = true
                            }

                            else -> {
                                Log.d(TAG, "Found unknown service.")
                            }
                        }
                    }

                    if (hasDataService && hasDevInfoService && hasManagerService) {
                        Log.d(TAG, "Found three services.")
                        initialProcess()
                    }
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            if (characteristic != null) {
                val value = characteristic.value
                delegate?.WSBLEGattRowData(value)
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            Log.d(TAG, "char read")
            if (characteristic != null)
                readChar(characteristic)

            readwriteUpdate()
        }
    }

    // function
    private fun notifyChar(service_uuid: UUID, char_uuid: UUID) {
        if (bleGatt != null) {
            val char = bleGatt!!.getService(service_uuid).getCharacteristic(char_uuid)
            if (char != null) {
                bleGatt!!.setCharacteristicNotification(char, true)

                val desc = char.getDescriptor(WSBLEProfile.CCCD)
                if (desc != null) {
                    Log.d(TAG, "Found desc")
                    desc.apply {
                        value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    }
                    bleGatt!!.writeDescriptor(desc)
                }
            }
        }
    }

    data class BLETask(var readwrite: Int, var service_uuid: UUID, var char_uuid: UUID, var value: ByteArray?)
    var bleTasks: MutableList<BLETask> = mutableListOf()
    var bleTaskFinally: (() -> Unit)? = null
    private fun readwriteUpdate() {
        if (bleTasks.size > 0) {
            val (readwrite, s_uuid, c_uuid, value) = bleTasks.removeAt(0)
            if (readwrite == 0) {
                readChar(s_uuid, c_uuid)

            } else {
                if (value != null) {
                    writeChar(s_uuid, c_uuid, value)
                }
            }
        } else if (bleTaskFinally != null) {
            bleTaskFinally!!.invoke()
        }
    }


    fun readwriteUpdate(bleTasks: List<BLETask>, finally: (() -> Unit)?) {
        this.bleTasks.addAll(bleTasks)
        this.bleTaskFinally = finally
        readwriteUpdate()
    }


    // MARK: read function for characters
    private fun readChar(service_uuid: UUID, char_uuid: UUID) {
        if (bleGatt != null) {
            val char = bleGatt!!.getService(service_uuid).getCharacteristic(char_uuid)
            bleGatt!!.readCharacteristic(char)
        }
    }
    private fun readChar(characteristic: BluetoothGattCharacteristic) {
        when (characteristic.uuid) {
            WSBLEProfile.DI_HARDWARE_REVISION_UUID -> {
                val value = characteristic.getStringValue(0)
                devInfo.HardwareRevision = value

                Log.d(TAG, "hardware revision: ${value}")
            }
            WSBLEProfile.DI_FIRMWARE_REVISION_UUID -> {
                val value = characteristic.getStringValue(0)
                devInfo.FirmwareRevision = value
                Log.d(TAG, "firmware revision: ${value}")
            }
            WSBLEProfile.DI_MANUFACTURER_NAME_UUID -> {
                devInfo.ManufacturerName = characteristic.getStringValue(0)
            }
            WSBLEProfile.DI_SYSTEM_ID_UUID -> {
                val values = characteristic.value.reversed()
                var address: String = ""
                for (value in values) {
                    address += value.toString(16)
                }
                address = address.removeRange(6, 10)
                devInfo.SystemID = address

                Log.d(TAG, "system id: ${address}")
            }
            WSBLEProfile.MS_MPU_FREQUENCY_UUID -> {
                val value = characteristic.value
                val result = (((value[0] and 0xFF.toByte()).toInt() shl 8) or (value[1] and 0xFF.toByte()).toInt())
                settingsInfo.Frequency = result
                Log.d(TAG, "freq: ${result}")
            }
            WSBLEProfile.MS_ACC_FSR_UUID -> {
                val value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                settingsInfo.AccMax = value
                Log.d(TAG, "acc: ${value}")
            }
            WSBLEProfile.MS_GYRO_FSR_UUID -> {
                val v1 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                val v2 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1)
                val result = (v1.toInt() shl 8) or v2
                settingsInfo.GyroMax = result
                Log.d(TAG, "gyro: ${result}")
            }
            WSBLEProfile.MS_MAGCOEF_UUID -> {
                val value = characteristic.value
                Log.d(TAG, "mag coef value size is ${value.size}, value is ${value}")
                for (b in value) {
                    Log.d(TAG, "> ${b}")
                }

                Log.d(TAG, "test value[0] is ${value[0]}")
                Log.d(TAG, "test value[1] is ${value[1]}")
                Log.d(TAG, "test value[2] is ${value[2]}")
                Log.d(TAG, "test value[3] is ${value[3]}")

                val magX = ByteBuffer.wrap(value, 0, 4).getFloat()
                val magY = ByteBuffer.wrap(value, 4, 4).getFloat()
                val magZ = ByteBuffer.wrap(value, 8, 4).getFloat()

                Log.d(TAG, "mag is ${magX.toString()}, ${magY}, ${magZ}")
//                settingsInfo.MagXcoef = magX
//                settingsInfo.MagYcoef = magY
//                settingsInfo.MagZcoef = magZ
                settingsInfo.MagXcoef = 1.0f
                settingsInfo.MagYcoef = 1.0f
                settingsInfo.MagZcoef = 1.0f
                settingsInfo.Mag = value
            }

            WSBLEProfile.MS_LOW_PASS_FILTER_UUID -> {
                val value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                Log.d(TAG, "lpf: ${value}")
                settingsInfo.LowPassFilter = value
            }
        }
    }

    fun writeChar(service_uuid: UUID, char_uuid: UUID, writeValue: ByteArray) {
        if (bleGatt != null) {
            val char = bleGatt!!.getService(service_uuid).getCharacteristic(char_uuid)
            char.apply {
                value = writeValue
            }

            bleGatt!!.writeCharacteristic(char)
        }
    }

    private fun initialProcess() {
        Log.d(TAG, "initial process")
        val list = listOf(
            BLETask(0,
                WSBLEProfile.SENSOR_DEVICE_INFO_SERVICE_UUID, WSBLEProfile.DI_MANUFACTURER_NAME_UUID,
                null
            ),
            BLETask(0,
                WSBLEProfile.SENSOR_DEVICE_INFO_SERVICE_UUID, WSBLEProfile.DI_HARDWARE_REVISION_UUID,
                null
            ),
            BLETask(0,
                WSBLEProfile.SENSOR_DEVICE_INFO_SERVICE_UUID, WSBLEProfile.DI_FIRMWARE_REVISION_UUID,
                null
            ),
            BLETask(0,
                WSBLEProfile.SENSOR_DEVICE_INFO_SERVICE_UUID, WSBLEProfile.DI_SYSTEM_ID_UUID,
                null
            ),
            BLETask(0,
                WSBLEProfile.SENSOR_MANAGER_SERVICE_UUID, WSBLEProfile.MS_MPU_FREQUENCY_UUID,
                null
            ),
            BLETask(0,
                WSBLEProfile.SENSOR_MANAGER_SERVICE_UUID, WSBLEProfile.MS_ACC_FSR_UUID,
                null
            ),
            BLETask(0,
                WSBLEProfile.SENSOR_MANAGER_SERVICE_UUID, WSBLEProfile.MS_GYRO_FSR_UUID,
                null
            ),
            BLETask(0,
                WSBLEProfile.SENSOR_MANAGER_SERVICE_UUID, WSBLEProfile.MS_LOW_PASS_FILTER_UUID,
                null
            ),
            BLETask(0,
                WSBLEProfile.SENSOR_MANAGER_SERVICE_UUID, WSBLEProfile.MS_MAGCOEF_UUID,
                null
            )
        )
        readwriteUpdate(list, fun () {
            delegate?.WSBLEGattUpdateSettingsInfo(settingsInfo)
            notifyChar(WSBLEProfile.SENSOR_DATA_SERVICE_UUID, WSBLEProfile.DS_MOTIONDATA_UUID)
            delegate?.WSBLEGattStatus(WSBLEGattStatus.CompleteInitialProcess)
        })
    }
}