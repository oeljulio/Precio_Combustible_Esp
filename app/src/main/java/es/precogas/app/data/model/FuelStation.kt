package es.precogas.app.data.model

data class FuelStation(
    val id: String,
    val provinceId: String,
    val postalCode: String,
    val address: String,
    val hours: String,
    val locality: String,
    val latitude: Double?,
    val longitude: Double?,
    val margin: String,
    val municipality: String,
    val provinceName: String,
    val brand: String,
    val saleType: String,
    val prices: Map<FuelProduct, Int>
) {
    fun priceFor(product: FuelProduct): Int? = prices[product]

    val availableProducts: List<FuelProduct>
        get() = prices.keys.toList()
}
