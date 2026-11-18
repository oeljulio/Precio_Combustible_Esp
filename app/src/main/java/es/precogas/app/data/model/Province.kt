package es.precogas.app.data.model

data class Province(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double
)

object ProvinceCatalog {
    val all: List<Province> = listOf(
        Province("01", "Álava/Araba", 42.85, -2.67),
        Province("02", "Albacete", 38.99, -1.86),
        Province("03", "Alicante", 38.35, -0.49),
        Province("04", "Almería", 36.84, -2.46),
        Province("05", "Ávila", 40.66, -4.70),
        Province("06", "Badajoz", 38.88, -6.97),
        Province("07", "Illes Balears", 39.6, 2.9),
        Province("08", "Barcelona", 41.5, 2.0),
        Province("09", "Burgos", 42.34, -3.70),
        Province("10", "Cáceres", 39.47, -6.37),
        Province("11", "Cádiz", 36.53, -5.90),
        Province("12", "Castellón", 40.2, -0.05),
        Province("13", "Ciudad Real", 38.99, -3.93),
        Province("14", "Córdoba", 37.89, -4.78),
        Province("15", "A Coruña", 43.1, -8.4),
        Province("16", "Cuenca", 40.07, -2.14),
        Province("17", "Girona", 42.0, 2.8),
        Province("18", "Granada", 37.18, -3.6),
        Province("19", "Guadalajara", 40.63, -3.16),
        Province("20", "Gipuzkoa", 43.2, -2.2),
        Province("21", "Huelva", 37.4, -6.9),
        Province("22", "Huesca", 42.14, -0.41),
        Province("23", "Jaén", 37.77, -3.79),
        Province("24", "León", 42.6, -5.57),
        Province("25", "Lleida", 41.6, 1.0),
        Province("26", "La Rioja", 42.3, -2.5),
        Province("27", "Lugo", 43.0, -7.55),
        Province("28", "Madrid", 40.42, -3.7),
        Province("29", "Málaga", 36.72, -4.42),
        Province("30", "Murcia", 38.0, -1.5),
        Province("31", "Navarra", 42.7, -1.65),
        Province("32", "Ourense", 42.3, -7.86),
        Province("33", "Asturias", 43.36, -5.85),
        Province("34", "Palencia", 42.0, -4.53),
        Province("35", "Las Palmas", 28.3, -14.9),
        Province("36", "Pontevedra", 42.43, -8.64),
        Province("37", "Salamanca", 40.97, -5.66),
        Province("38", "Santa Cruz de Tenerife", 28.4, -16.9),
        Province("39", "Cantabria", 43.18, -3.99),
        Province("40", "Segovia", 40.95, -4.12),
        Province("41", "Sevilla", 37.39, -5.98),
        Province("42", "Soria", 41.76, -2.47),
        Province("43", "Tarragona", 41.12, 1.25),
        Province("44", "Teruel", 40.35, -1.1),
        Province("45", "Toledo", 39.86, -4.02),
        Province("46", "Valencia", 39.47, -0.38),
        Province("47", "Valladolid", 41.65, -4.72),
        Province("48", "Bizkaia", 43.26, -2.93),
        Province("49", "Zamora", 41.5, -5.75),
        Province("50", "Zaragoza", 41.65, -0.88),
        Province("51", "Ceuta", 35.89, -5.32),
        Province("52", "Melilla", 35.29, -2.94)
    )

    fun findById(id: String): Province? =
        all.firstOrNull { it.id == id }
}
