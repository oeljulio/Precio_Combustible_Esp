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
- Herramientas: Python 3, Bash, Git y Google Antigravity CLI (agy).

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

Revisar especialmente activeRouteKey después de errores para evitar que bloquee solicitudes idénticas posteriores (debe restablecerse a null ante excepciones en FuelViewModel y proveer acción de reintento en HomeScreen).

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
- Cancelación.
- Caché.
- Cambio de provincia.
- Permisos.
- Persistencia.
- Estados de interfaz.

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
