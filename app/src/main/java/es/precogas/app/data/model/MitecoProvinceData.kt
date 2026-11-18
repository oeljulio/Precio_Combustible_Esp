package es.precogas.app.data.model

data class MitecoProvinceData(
    val provinceId: String,
    val officialDate: String,
    val downloadedAtMillis: Long,
    val stations: List<FuelStation>,
    val note: String
)
