# SkyNCar · Spark GT rojo velvet

Primera versión de un launcher Android para Isias, con interfaz oscura inspirada en iOS y acentos rojo velvet. Código original; CleanLauncher se consultó como referencia funcional y no se incorporó su código.

## Descargar APK

[Descargar SkyNCar.apk](https://github.com/isiasromero/SkyNCar/raw/refs/heads/main/downloads/SkyNCar.apk)

Versión inicial de desarrollo 0.2.1. El APK publicado está en `downloads/`; su checksum SHA-256 está en `downloads/SHA256SUMS`. Antes de instalar, revisar la compatibilidad indicada abajo. No incluye la clave privada de firma.

## Actualización 0.2.1

- Las apps marcadas con ☆ aparecen como accesos directos en **Mis aplicaciones favoritas** del inicio. **Ver todas las apps** mantiene disponible la lista completa.
- Botón fijo **Mi perfil** en la barra izquierda, accesible aunque la lista de secciones tenga desplazamiento. Abre el campo **Tu nombre para el saludo** y el botón **Guardar preferencias**; el nombre persiste entre reinicios.
- Botón fijo **Android** en la cabecera para abrir directamente los ajustes completos que ofrece el dispositivo, además del acceso en Ajustes de SkyNCar.
- Continúan disponibles los accesos a aplicaciones originales y el botón para elegir otro launcher. No hay modo kiosco ni políticas que restrinjan Android; las restricciones y permisos propios del fabricante siguen dependiendo del equipo.

## Actualización 0.2.0

- Radio FM y Todas las apps visibles desde el inicio; Radio FM y Apps suben en la barra lateral.
- Botones independientes Waze / Google Maps y elección de navegador preferido.
- Lista de actividades LAUNCHER, INFO y HOME que Android permite abrir: iconos reales, buscador por nombre/paquete, filtros Todas/Del equipo/Favoritas y favoritos persistentes. Se conservan diferentes actividades del mismo paquete (por ejemplo, teléfono y música Bluetooth).
- Selector de aplicaciones originales para FM, navegación, música, Bluetooth, teléfono e inicio de fábrica desde Ajustes. La selección persiste.
- Actualización de la lista al regresar al launcher y botón de recarga manual.
- No se desinstalan, deshabilitan ni borran apps, servicios, archivos ni datos del fabricante. Las actividades no exportadas y los servicios sin interfaz no pueden abrirse; no se inventan accesos a ellos.
- Sigue sin incrustarse Waze/Google Maps dentro de la tarjeta: cada uno abre su aplicación.

### Cómo configurar las aplicaciones del equipo

1. Instalar esta actualización sobre la anterior, sin desinstalar SkyNCar.
2. Abrir **Apps → Todas** o **Del equipo**, comprobar la lista y usar Actualizar lista si se acaba de instalar otra app.
3. Abrir **Radio FM → Elegir aplicación FM** y seleccionar la app original. Se sugieren posibles coincidencias por nombre, pero la elección es manual.
4. En **Ajustes → Aplicaciones preferidas del radio**, vincular Bluetooth, teléfono, música, navegador e inicio original. Solo se crean asociaciones en SkyNCar.
5. El acceso **Inicio original** permite volver a la interfaz del fabricante una vez elegido. También se puede cambiar la app de inicio predeterminada desde **Ajustes de SkyNCar → Dejar SkyNCar y elegir otro inicio**. Este botón abre los ajustes HOME de Android; el usuario elige el launcher anterior. No desinstala SkyNCar ni cambia la elección sin confirmación del usuario.

No se garantiza que todas las funciones propietarias del equipo tengan actividades exportadas. Si falta alguna, hace falta inspeccionar el modelo, paquetes y actividades reales del radio; las pruebas de navegador usan una simulación explícita del puente nativo y no prueban el hardware.

## Actualización 0.1.1

Icono propio de SkyNCar, con monograma SN blanco y rojo velvet, e icono adaptativo para Android 8+. La actualización mantiene las preferencias y usa la misma clave local de firma.

## Estado de esta versión

- Nueve secciones: inicio, navegación, música, teléfono, vehículo, vista dividida, aplicaciones, radio FM y ajustes.
- Saludo basado en la hora local **del radio**: mañana 05:00–11:59, tarde 12:00–18:59, noche 19:00–04:59. Nombre predeterminado: Isias; editable.
- Tema oscuro, claro o automático; preferencias persistentes.
- Accesos a Waze/Maps, música, marcador y ajustes Bluetooth. Cajón de aplicaciones reales en Android.
- Selección persistente de la aplicación FM original. No implementa el protocolo propietario de sintonización.
- Lectura de título/artista y reproducción/pausa/anterior/siguiente mediante MediaSession, después de conceder acceso a notificaciones. Requiere una app compatible reproduciendo música.
- Mantenimiento con kilometraje e intervalo manuales y validación de entradas. No lee el odómetro ni acumula GPS.
- Modo reposo con pantalla negra y regreso al tocar. **No apaga la retroiluminación física.**

Navegación abre la app externa. El fondo de mapa es ilustrativo; no contiene rutas ni tráfico en vivo. La vista dividida combina accesos y música, no incrusta Waze. Combustible, presión, temperatura y batería aparecen como **No conectado**. No se implementaron clima en vivo, Android Auto/CarPlay real, llamadas Bluetooth propietarias, sincronización con luces, controles de volante, ecualizador del fabricante ni voz. Esas integraciones requieren conocer y probar el radio; no se modifica firmware ni se deshabilitan apps del sistema.

## Compatibilidad e instalación

- Android 7.0/API 24 o superior. Android System WebView/Chrome **87 o superior** para la interfaz web (CSS Grid, clamp y JavaScript moderno). Confirmar modelo, versión Android y WebView del radio antes de instalar.
- APK de desarrollo, firmado con una clave local en `.local/` (ignorada por Git). Conservar esa clave para instalar actualizaciones sobre la misma aplicación. No es una firma de producción.
- Copiar `build/SkyNCar.apk` al equipo e instalarlo desde su administrador de archivos, o ejecutar `adb install -r build/SkyNCar.apk` si existe un dispositivo autorizado.
- Probar primero desde el icono de aplicaciones. Seleccionarlo como inicio es opcional desde Ajustes → Elegir launcher de inicio; conservar el launcher original.
- En Música, conceder el acceso a notificaciones de SkyNCar solo si se desean los controles. No se solicitan permisos de contactos, teléfono ni ubicación.
- En Radio FM, elegir la app original del fabricante desde la lista de aplicaciones.

La instalación, reproducción real, llamadas y comportamiento físico del radio **no se han probado en un dispositivo Android**. La validación realizada es compilación/firma del APK y pruebas de la interfaz en Chromium.

## Compilar en Linux

Requisitos: Java 17+ con módulo `jdk.compiler` y `keytool`, Python 3, `unzip`, herramientas nativas requeridas por Android Build Tools. Sin Gradle ni dependencias Java de terceros.

```sh
cd /workspace/SkyNCar
# Predeterminado: /workspace/tools/android-sdk; cambiar ANDROID_HOME si corresponde.
python3 scripts/setup_sdk.py
sh build.sh
node --test tests/core.test.cjs
```

El instalador descarga de `dl.google.com` Platform 33 y Build Tools 35.0.0, verifica el checksum publicado por Google y conserva los permisos de los archivos. El compilador usa Java 8 bytecode y d8 para Android API 24. `build.sh` solo borra el directorio generado `build/`; conserva la clave en `.local/`. El APK final es `build/SkyNCar.apk` y su firma se verifica al terminar.

## Pruebas funcionales de interfaz

```sh
npm install --prefix /workspace/tools/ui-check --cache /workspace/tools/npm-cache playwright@1.64.1
NODE_PATH=/workspace/tools/ui-check/node_modules CHROMIUM_PATH=/usr/bin/chromium node tests/ui.smoke.cjs
```

Chromium debe estar instalado. La prueba levanta un servidor interno temporal, comprueba navegación por las nueve secciones, persistencia de nombre/tema/mantenimiento, modo reposo y tres tamaños de pantalla. Además comprueba con un puente nativo simulado los filtros de apps del sistema, actividades distintas de un mismo paquete, búsqueda, favoritas, elección persistente de FM, navegador e inicio original y botones separados para Waze/Maps. Cierra servidor y navegador al terminar y escribe una captura en `build/home.png`. No valida el puente nativo de Android ni hardware.

## Estructura

- `src/`: Activity, puente nativo limitado a acciones locales y listener multimedia.
- `assets/`: interfaz, estilos, lógica de saludo y mantenimiento; imagen del vehículo generada como recurso visual conceptual.
- `scripts/setup_sdk.py`: instalación del SDK con comprobación de integridad.
- `tests/`: pruebas de lógica y funcionales de la interfaz.

El puente JavaScript se expone únicamente a los assets incluidos; el WebView bloquea la navegación externa y la política CSP impide conexiones remotas. Los mapas y otras apps se abren mediante intents. Los datos personales se guardan localmente, sin backend.
