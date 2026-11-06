# ⛽ Precios de Combustible en España (PWA)

[![GitHub Pages](https://img.shields.io/badge/GitHub%20Pages-Live%20Demo-brightgreen?style=flat-square&logo=github)](https://oeljulio.github.io/Precio_Combustible_Esp/)
[![PWA Ready](https://img.shields.io/badge/PWA-Installable-blue?style=flat-square&logo=pwa)](https://oeljulio.github.io/Precio_Combustible_Esp/)
[![API](https://img.shields.io/badge/Data-MITECO%20REST%20API-orange?style=flat-square)](https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)

> Aplicación web progresiva (PWA) rápida, ligera y sin publicidad para consultar los precios oficiales de gasolineras en España en tiempo real y calcular el **coste real del viaje** (precio del combustible + gasto de desvío por distancia).

👉 **[Abrir Aplicación en Vivo](https://oeljulio.github.io/Precio_Combustible_Esp/)**

---

## 💡 El Problema que Resuelve

A menudo la gasolinera más barata no es la más rentable si está a varios kilómetros de distancia. **Precio Combustible España** resuelve esto calculando:

$$\text{Coste Total} = (\text{Litros a repostar} \times \text{Precio/L}) + (\text{Distancia ida y vuelta} \times \frac{\text{Consumo}}{100} \times \text{Precio/L})$$

Así sabes exactamente si te compensa desplazarte a otra estación o repostar en la más cercana.

---

## 🚀 Características Principales

* 📡 **Datos Oficiales en Tiempo Real:** Conectada a la API pública del **Ministerio para la Transición Ecológica y el Reto Demográfico (MITECO)**.
* 🚗 **Cálculo de Rutas y Desvío Real:** Integración con **OSRM (Open Source Routing Machine)** para medir la distancia real por carretera (no en línea recta).
* 📍 **Búsqueda Flexible:**
  * Búsqueda por **geolocalización GPS** o introduciendo un pueblo/ciudad de salida (**Nominatim / OpenStreetMap**).
  * Navegación por **Provincias** y **Municipios** de toda España.
  * Ajuste de **radio de búsqueda** (de 5 km a 100 km).
* ⛽ **Múltiples Carburantes:** Gasolina 95 E5, Gasolina 98 E5, Gasóleo A habitual, Gasóleo Premium, Gasóleo B, GLP, GNC, GNL e Hidrógeno.
* 📱 **PWA Instalable:** Funciona como aplicación nativa en Android, iOS, Windows y macOS.
* 📴 **Soporte Offline (Service Worker):** Permite cargar la interfaz y consultar los últimos datos en caché sin conexión activa.
* 🌗 **Modo Oscuro / Claro:** Detección automática según las preferencias del sistema.
* ⚡ **Ultra Rápida y Privada:** Construida en **Vanilla JavaScript** puro, sin frameworks pesados ni rastreadores de terceros. Tu ubicación nunca sale de tu dispositivo.

---

## 🧱 Arquitectura y Servicios Utilizados

| Servicio / API | Proveedor | Función en la App |
| :--- | :--- | :--- |
| **API Precios Carburantes** | [MITECO Gobierno de España](https://sedeaplicaciones.minetur.gob.es/) | Datos oficiales actualizados de estaciones de servicio y precios. |
| **OSRM Routing** | [Project OSRM](https://project-osrm.org/) | Matriz de distancias por carretera en tiempo real. |
| **Nominatim Geocoding** | [OpenStreetMap](https://nominatim.openstreetmap.org/) | Búsqueda y geocodificación de nombres de ciudades/pueblos. |
| **Service Worker (`sw.js`)** | Estándar W3C / Cache API | Caché de recursos estáticos y respaldo para uso sin conexión. |
| **Cloudflare Worker (`worker.js`)** | Cloudflare *(Opcional)* | Proxy de caché con compresión y cabeceras CORS para alta concurrencia. |

---

## 📂 Estructura del Proyecto

```
Precio_Combustible_Esp/
├── index.html              # Interfaz completa, lógica reactiva y estilos CSS
├── manifest.webmanifest    # Manifiesto PWA para instalación en móviles y PC
├── sw.js                   # Service Worker para caché y funcionamiento offline
├── worker.js               # Proxy opcional para Cloudflare Workers con caché
├── icon.svg                # Icono vectorial escalable de la app
├── icon-192.png            # Icono PWA resolución estándar
├── icon-512.png            # Icono PWA alta resolución
├── .gitignore              # Archivos ignorados por Git
├── LICENSE                 # Licencia de código abierto MIT
└── README.md               # Documentación del proyecto
```

---

## 💻 Ejecución en Local

Al ser una aplicación web estática pura, no requiere `npm install` ni compiladores. Puedes ejecutarla con cualquier servidor local:

**Usando Python:**
```bash
python3 -m http.server 8080
```

**Usando Node.js:**
```bash
npx serve .
```

Luego abre en tu navegador: `http://localhost:8080`

---

## 📄 Licencia

Este proyecto está bajo la Licencia **MIT**. Consulta el archivo [LICENSE](LICENSE) para más detalles.
