package xyz.wonder_wonder.android.wondersensekit

import xyz.wonder_wonder.android.wondersensekit.WSFilter

private const val TAG = "WSFilter"
private const val NativeLibName = "native"

class WSFKF: WSFilter() {
    data class StaticCorrectionResult (val sigma_a: Double, val sigma_g: Double, val sigma_m: Double,
                                        val gx: Double,      val gy: Double,      val gz: Double
    )

    data class DynamicCorrectionResult (val u: Array<Double>,
                                         val mx: Double, val my: Double, val mz: Double
    )

    data class FKFResult (
        override val id: String,
        val q0: Double, val q1: Double, val q2: Double, val q3: Double
    ) : WSFilterResult {
        override fun equals(other: WSBLEData): Boolean {
            return other.id == this.id
        }
    }

    var staticCorrection: StaticCorrectionResult? = null
    var dynamicCorrection: DynamicCorrectionResult? = null

    fun initFilter() {
        native_init_fkf(staticCorrection, dynamicCorrection)
    }

    override fun filter(data: WSBLEData): WSFilterResult {
        // this process. if this do the process, the app happen the seg11.
        //        val result = FKFResult(data.id, 0.0,0.0,0.0,0.0)
        return native_fkf(data)
    }

    fun dynamicCorrection(data: Array<WSBLEData>) {
        val result = native_dynamic_correction(data)
        dynamicCorrection = result
    }

    fun staticCorrection(data: Array<WSBLEData>) {
        val result = native_static_correction(data)
        staticCorrection = result
    }


    private external fun native_init_fkf(sc: StaticCorrectionResult?, dc: DynamicCorrectionResult?)
    private external fun native_fkf(data: WSBLEData) : FKFResult
    private external fun native_static_correction(data: Array<WSBLEData>) : StaticCorrectionResult
    private external fun native_dynamic_correction(data: Array<WSBLEData>) : DynamicCorrectionResult
    companion object {
        init {
            System.loadLibrary(NativeLibName)
        }
    }
}
