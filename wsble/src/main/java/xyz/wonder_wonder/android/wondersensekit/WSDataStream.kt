package xyz.wonder_wonder.android.wondersensekit

//import xyz.wonder_wonder.android.wondersensekit.WSFilter
//import xyz.wonder_wonder.android.wondersensekit.WSFilterResult

class WSDataStream {
    private var rawData_cbFunc: ((data: WSBLEData) -> Unit)? = null
//    private var filteredData_cbFunc: ((result: WSFilterResult) -> Unit)? = null
//    private var filter: WSFilter? = null

    fun setRawDataCB(cbFunc: (data: WSBLEData) -> Unit) {
        rawData_cbFunc = cbFunc
    }

//    fun setFilteredDataCB(filter: WSFilter, cbFunc: (WSFilterResult)-> Unit) {
//        this.filter = filter
//        filteredData_cbFunc = cbFunc
//    }

    fun receiveData(data: Array<WSBLEData>) {
        for (item in data) {
            rawData_cbFunc?.invoke(item)
        }

//        for (item in data) {
//            filter?.filter(item).let {
//                it?.let { it1 -> filteredData_cbFunc?.invoke(it1) }
//            }
//        }
    }
}
