# PECL1 + PECL2 + PECL3 — Urban Traffic Light (ESP32 + Docker + Android)

Repositorio completo del sistema desarrollado en las prácticas PECL:
- **PECL1 (Dispositivo)**: semáforo en **ESP32** que publica estado y recibe comandos por **MQTT**.
- **PECL2 (Infraestructura)**: servicios en **Docker** (Tomcat + MariaDB + Mosquitto).
- **PECL3 (App Android)**: aplicación **Android** para monitorizar y controlar semáforos (histórico + tiempo real + acciones).

---

## Estructura del repositorio

- `arduino/`  
  Código del ESP32 (`.ino`, `.hpp`, configuración WiFi/MQTT).

- `docker/`  
  Infraestructura PECL2 con Docker Compose.
  - `docker-compose.yml`
  - `tomcat/` → contiene el WAR desplegable en Tomcat (**`serverubicua.war`**)
  - `mariadb/` → base de datos (volumen/configuración)
  - `mosquitto/` → configuración del broker MQTT

- `server/` (NetBeans)  
  Código Java (servlets + lógica) que se compila y genera el WAR.

- `android/` (Android Studio)  
  Proyecto Android de la app **AppPECL3**.

> Nota: si en tu repositorio las carpetas tienen otro nombre, basta con renombrar este apartado para que coincida.

---

## Arquitectura general

### Comunicaciones

**HTTP (Android → Tomcat)**  
La app Android consume endpoints del servidor desplegado en Tomcat y utiliza **Retrofit** para realizar las llamadas.

**MQTT (Android ↔ Mosquitto ↔ ESP32)**  
El ESP32 publica estados en topics `.../state`.  
Las acciones se envían desde Android por HTTP al servlet `/SetData`, y el servidor publica comandos por MQTT a `.../cmd`.  
La app también se suscribe a MQTT para recibir el estado en tiempo real.

---

## Configuración de red utilizada

El proyecto está configurado para ejecutarse en red local con:

- **Base URL Retrofit (Tomcat):** `http://192.168.0.73:8080/`
- **MQTT Broker URI (Mosquitto):** `tcp://192.168.0.73:1883`

Si cambias IP/puertos, revisa:
- `RetrofitClient` (Android)
- `MqttConsoleActivity` y `StreetMonitoring` (Android)
- Configuración del ESP32 (PECL1) si aplica

---

## Endpoints HTTP (Tomcat)

### Obtener calles
`GET /GetStreets`

Devuelve un listado de `streetId` en formato JSON.

### Obtener dispositivos por calle
`GET /GetDevicesByStreet?streetId=<streetId>`

Parámetros:
- `streetId` (String)

Devuelve una lista JSON de enteros con los `deviceId` disponibles para esa calle.

### Obtener histórico filtrado
`GET /GetDataFiltered?streetId=<streetId>&device=<deviceId>&startDate=<...>&endDate=<...>`

Parámetros:
- `streetId` (String)
- `device` (String)
- `startDate` (String, opcional)
- `endDate` (String, opcional)

Devuelve una lista JSON de mediciones (`MeasurementDto`).  
En la app los timestamps se convierten a horario local (**Europe/Madrid**) antes de mostrarse.

### Enviar acciones al dispositivo (vía servidor)
`GET /SetData?action=<action>&streetId=<streetId>&deviceId=<deviceId>&enabled=<true|false>`

Acciones soportadas:
- `action=force` → fuerza paso de peatones
- `action=buzzer` + `enabled=true|false` → activa/desactiva buzzer

Ejemplos:
- Forzar peatones:  
  `GET /SetData?action=force&streetId=ST_2245&deviceId=1`
- Buzzer ON:  
  `GET /SetData?action=buzzer&enabled=true&streetId=ST_2245&deviceId=1`
- Buzzer OFF:  
  `GET /SetData?action=buzzer&enabled=false&streetId=ST_2245&deviceId=1`

---

## Topics MQTT

### Estado (publicado por el ESP32)
`sensors/<streetId>/traffic_light/TL_%03d/state`

Ejemplo para `streetId=ST_2245` y `deviceId=1`:
- `sensors/ST_2245/traffic_light/TL_001/state`

### Comandos (publicados por el servidor)
`sensors/<streetId>/traffic_light/TL_%03d/cmd`

Ejemplo:
- `sensors/ST_2245/traffic_light/TL_001/cmd`

### Payloads de comandos (ejemplos)

Forzar peatones:
```json
{ "force": "ped_green" }
```

Buzzer:
```json
{ "buzzer": true }
{ "buzzer": true }
```

---

## Cómo ejecutar la infraestructura (PECL2) - Docker

### Requisitos
- Docker Desktop Instalado y abierto

### Pasos
1. Abrir una terminal en la carpeta docker/
2. Ejecutar

docker compose up --build

Esto levanta:
- *Tomcat* (servidor HTTP con servlets)
- *MariaDB* (persistencia)
- *Mosquito* (broker MQTT)

## Actualizar el servidor (WAR) — NetBeans → Docker/Tomcat

El servidor se despliega como `serverubicua.war` dentro de `docker/tomcat/`.

**Flujo de actualización:**

1. Abrir el proyecto del servidor en NetBeans (`server/`)
2. Ejecutar **Clean and Build**
3. NetBeans genera el fichero `.war`
4. Copiar el WAR generado a `docker/tomcat/`
5. Reemplazar el WAR antiguo
6. Asegurarse de que el archivo se llama exactamente `serverubicua.war`
7. Reconstruir contenedores:
   ```bash
   cd docker
   docker compose up --build
   ```

---

## Cómo ejecutar la App Android (PECL3)

### Requisitos
- Android Studio
- Dispositivo físico o emulador
- Conectividad desde el móvil/emulador hacia la IP `192.168.0.73`

### Pasos
1. Abrir la carpeta `android/` en Android Studio
2. Ejecutar la app

### Funcionalidades implementadas

La app **AppPECL3** permite:

- **Selección dinámica** de calle y dispositivo (HTTP: `GetStreets`, `GetDevicesByStreet`)
- **Consulta de histórico** desde BD con filtros por fecha (HTTP: `GetDataFiltered`)
- **Monitorización en tiempo real** por MQTT (suscripción a `.../state`)
- **Acciones de control:**
  - Forzar paso de peatones (HTTP: `SetData` → MQTT `.../cmd`)
  - Activar/Desactivar buzzer (HTTP: `SetData` → MQTT `.../cmd`)
- **Consola MQTT** para pruebas:
  - Suscripción manual a topics
  - Publicación manual de payloads
  - Log de mensajes recibidos/enviados
- **Gestión de tema** (light/dark/cyber) con modo especial asociado a determinadas pistas
- **Reproducción de música** global y selección de pista desde ajustes

---

## Cómo ejecutar el dispositivo (PECL1) — ESP32

### Requisitos
- Arduino IDE o PlatformIO

### Pasos
1. Compilar y flashear el proyecto en el ESP32 desde Arduino IDE / PlatformIO
2. Configurar:
   - WiFi (SSID/PASS)
   - Broker MQTT (IP y puerto)
   - `streetId` / `deviceId` (si aplica)
3. Verificar que:
   - Publica en el topic `.../state`
   - Escucha comandos en `.../cmd`

---

## Entrega — Vídeo de demostración

Se recomienda incluir en el vídeo:

- **Infraestructura Docker**: Docker Compose levantando servicios (Tomcat/MariaDB/Mosquitto)
- **App Android:**
  - Selección de calle y dispositivo
  - Carga del histórico filtrado
  - Recepción en vivo por MQTT
  - Acciones de control (force + buzzer)
- **Conexión con el dispositivo PECL1** en tiempo real (si es posible):
  - Cambios del semáforo tras enviar acciones desde la app
