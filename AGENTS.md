# AGENTS.md — PreCoGas

## 1. Objetivo del proyecto

PreCoGas es una aplicación para consultar, comparar y calcular el coste real de repostar combustible en estaciones de servicio de España.

El proyecto dispone de:
- Aplicación Android nativa.
- Aplicación web.
- Progressive Web App (PWA).

Prioridad: Android nativo, conservando todas las funcionalidades de la web y PWA.

## 2. Entorno de desarrollo

- Andronix con Debian 13 ARM64 mediante PRoot.
- Usuario: oel.
- Directorio personal: /home/oel.
- Proyectos: ~/proyectos/.
- Proyecto: ~/proyectos/PrecioCombustible/.
- Carpetas compartidas: ~/Android/Download y ~/Android/Documents.
- Herramientas: Python 3, Bash, Git, Java 17 Temurin, Gradle Wrapper 9.5.0 y Google Antigravity CLI (agy).

Restricciones:
- No utilizar Termux, QEMU ni máquinas virtuales.
- PRoot no proporciona root real de Android.
- sudo administra exclusivamente Debian.
- Los builds Android completos se realizan exclusivamente mediante GitHub Actions.

## 3. Antes de modificar

Obligatoriamente:

1. Confirmar proyecto, directorio y repositorio.
2. Inspeccionar estructura y documentación.
3. Leer README.md, AGENTS.md y documentos relevantes.
4. Revisar configuración Android, web y PWA.
5. Comprobar rama, remotos y sus URL.
6. Ejecutar git status --short.
7. Revisar cambios pendientes y archivos nuevos.
8. Consultar historial Git cuando sea relevante.
9. Identificar intentos anteriores de solución.
10. Identificar archivos afectados, dependencias e impactos.

La documentación del repositorio es la fuente principal de verdad. Los chats son contexto complementario.

Preservar siempre el trabajo del usuario.

## 4. Método de trabajo

Seguir este ciclo:

1. Detectar el problema.
2. Investigar su causa real.
3. Inspeccionar el código y su contexto.
4. Aplicar la corrección mínima necesaria.
5. Comprobar el resultado.
6. Continuar con el siguiente problema.

Resolver los problemas progresivamente, uno por uno.

Diferenciar errores de código, compilación, configuración, dependencias, pruebas y conectividad.

No confundir síntomas con causas ni declarar que algo funciona sin comprobarlo.

No acumular cambios especulativos.

## 5. Modificaciones seguras

Preferir Python 3 para modificaciones complejas.

Antes de escribir:
- Leer y validar los archivos.
- Comprobar que los patrones esperados existen.
- Verificar el número de coincidencias.
- Abortar si alguna condición no se cumple.
- Preservar codificación, saltos de línea e indentación.

No aplicar dos veces un parche sin comprobar si ya se ejecutó.

Después de modificar, inspeccionar el resultado real.

No sobrescribir cambios existentes ni eliminar archivos sin autorización.

## 6. Desarrollo Android

Tecnologías:
- Kotlin.
- Gradle.
- Jetpack Compose.
- AndroidX.
- Room.
- Coroutines.
- ViewModels.
- Repositorios.

Respetar arquitectura, paquetes, imports, tipos, referencias y dependencias existentes.

### 6.1. Selección de combustible

- No imponer un combustible predeterminado.
- Permitir seleccionar los productos disponibles.
- Adaptar las unidades al combustible seleccionado.
- Los productos en €/L utilizan litros.
- Los productos en €/kg utilizan kilogramos.
- No aplicar consumos en L/100 km a productos en €/kg.
- Evitar mezclar unidades incompatibles.

### 6.2. Configuración inicial

Solicitar:
- Provincia.
- Combustible.
- Consumo.
- Radio de búsqueda.

No imponer combustible predeterminado.

Mostrar discretamente estos valores en el inicio y permitir editarlos desde Ajustes.

En Ajustes deben estar:
- Tema claro, oscuro y AMOLED.
- Información MITECO.
- Fecha y hora de la última consulta.

Implementado mediante `InitialSetupScreen` para la primera apertura y `SettingsScreen` accesible discretamente desde el inicio para editar preferencias, apariencia y consultar metadatos oficiales de MITECO.

### 6.3. Cantidad a repostar

Revisar el campo de cantidad:
- Aceptar decimales.
- Adaptar unidades.
- Evitar pérdida del cursor.
- Actualizar cálculos.
- Validar valores vacíos, negativos e incorrectos.

### 6.4. Modos de cálculo

Deben funcionar:
1. Solo repostaje.
2. Repostaje + ida.
3. Repostaje + ida y vuelta.

Cambiar de modalidad debe actualizar cálculos e interfaz.

### 6.5. Ordenación

Las opciones dependen del modo:

Solo repostaje:
- Precio.
- Distancia por carretera.

Repostaje + ida:
- Coste total.
- Tiempo de viaje.

Repostaje + ida y vuelta:
- Coste total.
- Tiempo de viaje.

No mostrar criterios incompatibles.

Revisar criterio inicial, cambios, ordenación, actualización e invalidación de resultados obsoletos.

La ordenación debe conservarse al abrir y cerrar Ajustes y al rotar o recrear la Activity. Calcular un criterio efectivo a partir del modo actual y aplicarlo de forma coherente a la ordenación, al texto «Orden actual», al menú y a la necesidad de calcular rutas. Al cambiar de modo, adaptar solo los criterios incompatibles: en Solo repostaje, Precio o Distancia por carretera (predeterminado Precio); en modos con desplazamiento, Coste total o Tiempo de viaje (predeterminado Coste total). No reiniciar la ordenación de forma incondicional ni perder las rutas por abrir o cerrar Ajustes.

### 6.6. Rutas OSRM

Inspeccionar especialmente:
- OsrmRoutingClient.kt.
- StationRoute.kt.
- FuelViewModel.kt.
- MainActivity.kt.
- HomeScreen.kt.

Requisitos:
- Utilizar distancias reales por carretera.
- Calcular rutas desde el origen a cada estación.
- Obtener distancia y duración.
- Calcular correctamente el coste.
- Diferenciar ida y vuelta.
- No duplicar distancias ni duraciones.
- Gestionar cancelaciones, concurrencia, caché, caducidad y errores HTTP.
- Gestionar tiempos de espera, reintentos y conectividad.

En FuelViewModel, conservar activeRouteKey mientras el contexto actual tenga una solicitud activa o resultados válidos, incluidos los recuperados de la caché, para deduplicar recomposiciones. Al cambiar el contexto, cancelar la solicitud anterior y actualizar la clave. Si la solicitud falla, poner activeRouteKey a null y registrar la clave y el error en FailedRouteTracker para bloquear reintentos automáticos idénticos. El reintento explícito desde HomeScreen debe limpiar ambos estados mediante clearRoutes().

Una petición de rutas idéntica que ya falló no debe reintentarse automáticamente al recomponer, abrir o cerrar Ajustes ni al rotar. El reintento debe ser explícito; los cambios de origen, estaciones o provincia pueden iniciar una petición nueva.

Gestión del registro de fallos:
- Una clave fallida debe bloquear únicamente los reintentos automáticos de esa misma clave.
- Al solicitar una clave distinta, invalidar el registro del fallo anterior antes de continuar con la nueva solicitud.
- Volver a una clave anteriormente fallida después de un cambio real de contexto debe permitir un nuevo cálculo.
- El reintento explícito debe seguir liberando el bloqueo.
- Conservar la deduplicación de solicitudes activas y la caché de resultados válidos.

Utilizar estimaciones únicamente cuando falle OSRM.

Identificar claramente los resultados estimados y no presentarlos como rutas reales.

### 6.7. Ubicación

Revisar DeviceLocationProvider y MainActivity.

- Solicitar ubicación únicamente cuando sea necesaria.
- No solicitarla al inicio innecesariamente.
- No solicitarla en Solo repostaje si no se necesita.
- Gestionar permisos aproximados y precisos.
- Gestionar permisos denegados.
- Cancelar solicitudes obsoletas.
- Evitar condiciones de carrera.
- Evitar listeners innecesarios.
- Evitar que respuestas antiguas alteren el estado actual.
- Conservar las coordenadas de origen y el estado del proceso de ubicación mediante `rememberSaveable`, nunca en DataStore. El permiso no debe tratarse como un estado persistido: comprobar siempre su concesión directamente en Android antes de recuperar la ubicación o iniciar una solicitud.
- Tras rotar o tras la muerte del proceso, recuperar la ubicación automáticamente solo si el modo la requiere y el permiso ya está concedido; la recuperación automática no debe abrir el diálogo de permisos.
- Mantener la protección por generaciones: aceptar una concesión legítima aunque la generación se haya perdido por una recreación, pero impedir que una respuesta antigua reactive una solicitud incompatible con el modo actual.
- No invalidar incondicionalmente una ubicación restaurada en la primera composición de Solo repostaje; puede seguir siendo necesaria para la ordenación por distancia.
- En Solo repostaje, ofrecer la solicitud de ubicación cuando la ordenación efectiva sea Distancia por carretera y no haya un origen válido.

### 6.8. Interfaz

Inspeccionar todas las pantallas Compose:
- Inicio.
- Ajustes.
- Combustible.
- Resultados.
- Ordenación.
- Filtros.
- Formularios.
- Estados vacíos.
- Carga.
- Errores.
- Navegación.
- Temas.
- Diseño adaptable.

Corregir pérdida del cursor, valores duplicados, problemas de edición, cálculos desactualizados y estados inconsistentes.

Mensaje para la ordenación por carretera:

"La ordenación por carretera requiere compartir tu ubicación y calcular la ruta."

Distinguir los estados vacíos según la modalidad.

Identificar si las estimaciones corresponden a ida o ida y vuelta.

Al cambiar de provincia invalidar rutas que ya no sean válidas.

- Mantener un único propietario de insets en `MainActivity`: `enableEdgeToEdge()` y un contenedor raíz con `safeDrawingPadding()`, que incluye barras del sistema, recortes y teclado. No añadir `imePadding`, `statusBarsPadding` ni `navigationBarsPadding` en pantallas o componentes hijos. Declarar `android:windowSoftInputMode="adjustResize"` en `MainActivity` para que el teclado ajuste el área disponible en API 26-29.
- Usar un `Surface` raíz con `MaterialTheme.colorScheme.background` para que el fondo y el color de contenido sigan el tema, y ajustar la apariencia de las barras del sistema según `ThemeMode` (claro, oscuro y AMOLED).
- Las pantallas con formularios deben poder desplazarse cuando el teclado reduce el espacio disponible; comprobar que la acción principal sigue siendo accesible con el teclado abierto.

## 7. MITECO

Revisar:
- Consulta de estaciones.
- Actualización de precios.
- Interpretación de combustibles.
- Compatibilidad de productos.
- Conversión de unidades.
- Errores.
- Fechas y horas.
- Datos incompletos.
- Estaciones sin coordenadas.
- Persistencia y caché.

Utilizar datos oficiales y no inventar precios.

## 8. Room y persistencia

Auditar entidades, DAO, relaciones, base de datos, migraciones, repositorios y preferencias.

Conservar todos los datos existentes.

Nunca borrar ni reinicializar bases de datos sin autorización expresa.

Toda modificación estructural debe disponer de una migración segura.

## 9. Web y PWA

Revisar:
- Framework.
- package.json.
- Scripts.
- Rutas.
- Dependencias.
- HTML, CSS y JavaScript.
- TypeScript.
- Navegación.
- Formularios.
- Diseño responsive.
- Manifest.
- Iconos.
- Service Worker.
- Caché.
- Instalación.

No cachear recursos inexistentes.

Conservar las funcionalidades y comportamiento de la PWA.

No modificar la PWA para solucionar problemas exclusivos de Android.

## 10. Arquitectura y calidad

Revisar ViewModels, repositorios, clientes HTTP, modelos, dependencias, Coroutines, flujos de estado, ciclo de vida y gestión de errores.

Reutilizar código y evitar duplicaciones demostradas.

No introducir dependencias ni refactorizaciones innecesarias.

Considerar las limitaciones de ARM64 y PRoot.

## 11. Pruebas

Revisar y ampliar las pruebas existentes.

Prestar especial atención a:
- Selección de combustible.
- Compatibilidad de unidades.
- Cálculos de repostaje.
- Coste del desplazamiento.
- Coste total.
- Ida y vuelta.
- Ordenación.
- Distancias reales.
- Tiempo de viaje.
- Respuestas OSRM.
- Errores y reintentos.
- Bloqueo de reintentos automáticos para claves OSRM idénticas que ya fallaron.
- Invalidación del fallo al cambiar de clave y recuperación al volver a una clave anterior.
- Cancelación de una clave nueva sin mantener bloqueada una clave fallida anterior.
- Reintento explícito de rutas fallidas.
- Deduplicación de solicitudes activas y conservación de caché válida.
- Cancelación.
- Caché.
- Cambio de provincia.
- Permisos.
- Persistencia.
- Estados de interfaz.
- Insets, teclado y barras del sistema en API 26-29 y 35+.
- Restauración de ubicación y permisos tras rotación y muerte del proceso.
- Conservación de la ordenación al abrir o cerrar Ajustes y al rotar.

No eliminar pruebas para conseguir que pasen.

Diferenciar pruebas locales y CI.

## 12. Gradle y GitHub Actions

Inspeccionar:
- settings.gradle.kts.
- build.gradle.kts.
- gradle.properties.
- app/build.gradle.kts.
- AndroidManifest.xml.
- .github/workflows/.

Comprobar Gradle, Android Gradle Plugin, Kotlin, Compose, JDK, compileSdk, targetSdk, minSdk, SDK y Build Tools.

Gradle Wrapper:
- Utilizar `./gradlew` como punto de entrada único para ejecutar Gradle, tanto en Debian como en CI.
- La versión fijada es Gradle 9.5.0. Mantener `distributionSha256Sum` para verificar la distribución descargada.
- Mantener versionados `gradlew`, `gradle/wrapper/gradle-wrapper.jar` y `gradle/wrapper/gradle-wrapper.properties`.
- GitHub Actions debe ejecutar los tests unitarios, los tests instrumentados de Room y `assembleDebug` mediante `./gradlew`.
- En Debian ARM64/PRoot, limitar las pruebas a tareas compatibles y verificables en ese entorno. No asumir que el SDK, el emulador o las herramientas nativas x86_64 funcionan en ARM64.

Los builds Android completos se realizan exclusivamente mediante GitHub Actions.

No intentar builds Android completos en Debian ARM64/PRoot.

Verificar los artefactos y APK correspondientes al commit.

No afirmar que un APK funciona sin comprobarlo.

## 13. Git y publicación

PROHIBIDO sin autorización expresa:

- git commit.
- git push.
- Publicar versiones.
- Crear releases.
- Eliminar ramas o tags.
- Reescribir historial.
- Ejecutar operaciones destructivas.

Antes de un commit autorizado, revisar estado, diff, archivos concretos y git diff --cached.

Antes de publicar, comprobar rama, remoto, historial y cambios pendientes.

Una autorización concreta no se extiende a otros proyectos u operaciones.

## 14. Seguridad y datos

- Proteger tokens, contraseñas y claves.
- No publicar secretos.
- No introducir credenciales en el repositorio.
- No exponer datos personales.
- No borrar datos del usuario.
- No sobrescribir trabajo ajeno.
- No cambiar credenciales sin autorización.
- No realizar operaciones irreversibles sin autorización.

Trabajar autónomamente dentro de estas restricciones.

## 15. Documentación

Actualizar cuando corresponda:
- README.md.
- AGENTS.md.
- CHANGELOG.md.
- RELEASE_NOTES.md.

Documentar decisiones técnicas y soluciones importantes.

Evitar documentación duplicada u obsoleta.

Sincronizar versiones entre plataformas cuando corresponda y exista justificación.

## 16. Mantenimiento y evolución de AGENTS.md

Antigravity está autorizado a modificar y ampliar AGENTS.md por iniciativa propia cuando lo considere necesario para mejorar la calidad, continuidad, seguridad y coherencia del proyecto.

Puede:
1. Añadir instrucciones técnicas.
2. Documentar decisiones arquitectónicas.
3. Incorporar soluciones verificadas.
4. Actualizar instrucciones obsoletas.
5. Documentar errores recurrentes.
6. Mejorar procedimientos de pruebas y compilación.
7. Incorporar requisitos confirmados por el usuario.
8. Reorganizar el documento sin perder información importante.

Condiciones:
- Leer el contenido existente antes de modificarlo.
- Conservar todas las instrucciones válidas.
- No eliminar requisitos establecidos por el usuario.
- No convertir suposiciones en reglas confirmadas.
- No documentar como solucionado un problema no verificado.
- Evitar duplicaciones y contradicciones.
- Mantenerlo sincronizado con el estado real del proyecto.
- Inspeccionar su diff después de modificarlo.

Las nuevas instrucciones no pueden anular las restricciones de seguridad ni los requisitos de autorización para commits, pushes y operaciones irreversibles.

Al finalizar cada intervención, indicar si AGENTS.md se ha actualizado y resumir los cambios.

## 17. Informe final obligatorio

Al finalizar cada intervención informar de:
1. Problema detectado.
2. Causa real.
3. Corrección aplicada.
4. Archivos modificados y nuevos.
5. Funcionalidades completadas.
6. Pruebas ejecutadas y resultados.
7. Errores pendientes.
8. Limitaciones del entorno.
9. Cambios en AGENTS.md.
10. Estado final de Git.
11. Próximos pasos.

Ejecutar al finalizar:

git status --short
git diff --check
git --no-pager diff

Inspeccionar también archivos nuevos sin seguimiento.

No declarar terminado el proyecto si quedan problemas funcionales o pruebas importantes sin resolver.

## REGLA FUNDAMENTAL

Detectar, investigar, corregir, comprobar y continuar.

Priorizar estabilidad, conservación del trabajo existente, seguridad y soluciones verificables.

Trabajar autónomamente, pero nunca publicar cambios ni ejecutar operaciones irreversibles sin autorización expresa.
