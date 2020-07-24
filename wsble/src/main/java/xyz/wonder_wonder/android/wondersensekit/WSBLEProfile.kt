package xyz.wonder_wonder.android.wondersensekit

import java.util.*

object WSBLEProfile {
    // generic
    val CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // data service
    val SENSOR_DATA_SERVICE_UUID: UUID = UUID.fromString("F0001110-0451-4000-B000-000000000000")
    val DS_MOTIONDATA_UUID: UUID = UUID.fromString("F0002EA1-0451-4000-B000-000000000000")

    // device information service
    val SENSOR_DEVICE_INFO_SERVICE_UUID: UUID = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB")
    val DI_MANUFACTURER_NAME_UUID: UUID = UUID.fromString("00002A29-0000-1000-8000-00805F9B34FB")
    val DI_HARDWARE_REVISION_UUID: UUID = UUID.fromString("00002A27-0000-1000-8000-00805F9B34FB")
    val DI_FIRMWARE_REVISION_UUID: UUID = UUID.fromString("00002A26-0000-1000-8000-00805F9B34FB")
    val DI_SYSTEM_ID_UUID: UUID = UUID.fromString("00002A23-0000-1000-8000-00805F9B34FB")

    // manager service
    val SENSOR_MANAGER_SERVICE_UUID: UUID = UUID.fromString("F000EA8E-0451-4000-B000-000000000000");
    var MS_SAVE_UUID: UUID = UUID.fromString("F0002FAF-0451-4000-B000-000000000000");
    var MS_RUNNING_UUID: UUID = UUID.fromString("F0006EF5-0451-4000-B000-000000000000");
    var MS_MPU_FREQUENCY_UUID: UUID = UUID.fromString("F0002698-0451-4000-B000-000000000000");
    var MS_COMPASS_FREQUENCY_UUID: UUID = UUID.fromString("F000B39A-0451-4000-B000-000000000000");
    var MS_SELF_TEST_UUID: UUID = UUID.fromString("F0001E89-0451-4000-B000-000000000000");
    var MS_LOW_PASS_FILTER_UUID: UUID = UUID.fromString("F0006C54-0451-4000-B000-000000000000");
    var MS_ACC_FSR_UUID: UUID = UUID.fromString("F0004C4A-0451-4000-B000-000000000000");
    var MS_GYRO_FSR_UUID: UUID = UUID.fromString("F000E4FF-0451-4000-B000-000000000000");
    var MS_REBOOT_UUID: UUID = UUID.fromString("F000C048-0451-4000-B000-000000000000");
    var MS_SLEEP_UUID: UUID = UUID.fromString("F0000F73-0451-4000-B000-000000000000");
    var MS_MAGCOEF_UUID: UUID = UUID.fromString("F00007F3-0451-4000-B000-000000000000");
}

class WSBLEData(
    var id: String,
    var ax: Double, var ay: Double, var az: Double,
    var gx: Double, var gy: Double, var gz: Double,
    var mx: Double, var my: Double, var mz: Double,
    var temperature:Double, var battery: Double, var airPressure:Double) {
}


