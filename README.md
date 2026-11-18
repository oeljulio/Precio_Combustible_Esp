# PreCoGas — Precio Combustible España

**Encuentra. Compara. Ahorra.**

Aplicación Android nativa para consultar los precios oficiales de combustibles en España mediante los datos de MITECO.

[![Android](https://img.shields.io/badge/Plataforma-Android-green?logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-Compose-purple?logo=kotlin)](https://kotlinlang.org/)
[![Licencia MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## Estado del proyecto

PreCoGas está evolucionando desde una antigua PWA hacia una aplicación Android nativa independiente.

### Funcionalidades implementadas

- Consulta de estaciones y precios mediante la API oficial MITECO.
- Selección entre las 52 provincias españolas.
- Actualización al abrir la aplicación y actualización manual.
- Almacenamiento local mediante Room.
- Conservación de los últimos datos cuando falla la conexión.
- Catálogo de 23 combustibles; cada estación muestra solo los precios disponibles.
- Temas claro, oscuro y AMOLED.
- Preferencias persistentes mediante DataStore.

- Geolocalización del dispositivo con permisos de ubicación aproximada o precisa.
- Cálculo de distancias por carretera mediante OSRM y estimaciones alternativas cuando no hay respuesta.
- Filtros por radio y provincia.
- Selector de combustible según los productos disponibles en cada provincia.
- Cálculo configurable del coste de repostaje y del desplazamiento de ida o ida y vuelta, con unidades compatibles para productos por litro o kilogramo.

## Tecnologías

- Kotlin y Jetpack Compose.
- Material 3.
- AndroidX ViewModel.
- Room y DataStore.
- KSP.
- API REST de MITECO.
- GitHub Actions.

## API de precios

Servicio oficial:

https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/

Los datos se validan antes de almacenarse. Si falla una actualización, se conservan los datos locales anteriores.

## Compilación y pruebas

Las compilaciones Android completas se realizan mediante GitHub Actions, no localmente en Andronix.

El workflow está configurado para ejecutar los tests instrumentados de Room, compilar el APK Debug y publicar los artefactos.

Acceso a las ejecuciones:

https://github.com/oeljulio/Precio_Combustible_Esp/actions

El APK de pruebas se publica como `PreCoGas-debug`. La compilación y los tests todavía deben verificarse mediante una ejecución satisfactoria del workflow.

## Configuración Android

| Propiedad | Valor |
|---|---|
| Application ID | `es.precogas.app` |
| Versión | `1.0.0` |
| SDK mínimo | 26 |
| SDK objetivo | 36 |
| SDK compilación | 37 |

## Archivos de la antigua PWA

Se conservan en la raíz `index.html`, `manifest.webmanifest`, `sw.js`, `worker.js` e iconos de la versión web. Son archivos heredados y no forman parte de la aplicación Android nativa.

## Licencia

Licencia MIT. Consulta el archivo [LICENSE](LICENSE).
