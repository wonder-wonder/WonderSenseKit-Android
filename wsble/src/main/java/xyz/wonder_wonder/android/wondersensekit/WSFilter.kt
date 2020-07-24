package xyz.wonder_wonder.android.wondersensekit

interface WSFilterResult {
    val id: String
    
    fun equals(other: WSBLEData): Boolean
}

abstract class WSFilter {
    abstract fun filter(data: WSBLEData) : WSFilterResult
}
