package es.precogas.app.data.model

enum class FuelCategory {
    FUEL,
    ADDITIVE,
    ALTERNATIVE_ENERGY
}

enum class FuelProduct(
    val apiKey: String,
    val displayName: String,
    val category: FuelCategory,
    val priceUnit: String = "€/l"
) {
    ADBLUE("Precio Adblue", "AdBlue", FuelCategory.ADDITIVE),
    AMMONIA("Precio Amoniaco", "Amoniaco", FuelCategory.ALTERNATIVE_ENERGY, priceUnit = "€/kg"),
    BIODIESEL("Precio Biodiesel", "Biodiésel", FuelCategory.ALTERNATIVE_ENERGY),
    BIOETHANOL("Precio Bioetanol", "Bioetanol", FuelCategory.ALTERNATIVE_ENERGY),
    BIOGAS_CNG("Precio Biogas Natural Comprimido", "Biogás natural comprimido", FuelCategory.ALTERNATIVE_ENERGY, priceUnit = "€/kg"),
    BIOGAS_LNG("Precio Biogas Natural Licuado", "Biogás natural licuado", FuelCategory.ALTERNATIVE_ENERGY, priceUnit = "€/kg"),
    RENEWABLE_DIESEL("Precio Diésel Renovable", "Diésel renovable", FuelCategory.ALTERNATIVE_ENERGY),
    CNG("Precio Gas Natural Comprimido", "Gas natural comprimido", FuelCategory.ALTERNATIVE_ENERGY, priceUnit = "€/kg"),
    LNG("Precio Gas Natural Licuado", "Gas natural licuado", FuelCategory.ALTERNATIVE_ENERGY, priceUnit = "€/kg"),
    LPG("Precio Gases licuados del petróleo", "GLP (autogás)", FuelCategory.FUEL),
    DIESEL_A("Precio Gasoleo A", "Diésel A", FuelCategory.FUEL),
    DIESEL_B("Precio Gasoleo B", "Gasóleo B (agrícola)", FuelCategory.FUEL),
    DIESEL_PREMIUM("Precio Gasoleo Premium", "Diésel premium", FuelCategory.FUEL),
    GASOLINE_95_E10("Precio Gasolina 95 E10", "Gasolina 95 (E10)", FuelCategory.FUEL),
    GASOLINE_95_E25("Precio Gasolina 95 E25", "Gasolina 95 (E25)", FuelCategory.FUEL),
    GASOLINE_95_E5("Precio Gasolina 95 E5", "Gasolina 95 (E5)", FuelCategory.FUEL),
    GASOLINE_95_E5_PREMIUM("Precio Gasolina 95 E5 Premium", "Gasolina 95 (E5 premium)", FuelCategory.FUEL),
    GASOLINE_95_E85("Precio Gasolina 95 E85", "Gasolina 95 (E85)", FuelCategory.FUEL),
    GASOLINE_98_E10("Precio Gasolina 98 E10", "Gasolina 98 (E10)", FuelCategory.FUEL),
    GASOLINE_98_E5("Precio Gasolina 98 E5", "Gasolina 98 (E5)", FuelCategory.FUEL),
    RENEWABLE_GASOLINE("Precio Gasolina Renovable", "Gasolina renovable", FuelCategory.ALTERNATIVE_ENERGY),
    HYDROGEN("Precio Hidrogeno", "Hidrógeno", FuelCategory.ALTERNATIVE_ENERGY, priceUnit = "€/kg"),
    METHANOL("Precio Metanol", "Metanol", FuelCategory.ALTERNATIVE_ENERGY, priceUnit = "€/kg");

    companion object {
        fun fromApiKey(key: String): FuelProduct? =
            entries.firstOrNull { it.apiKey == key }
    }
}
