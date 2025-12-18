#pragma once
#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include "config.h"
#include "ESP32_Utils.hpp"

/**
 * @file    MQTT.hpp
 * @brief   Utilidades WiFi/MQTT y publicación de telemetría del semáforo.
 *
 * @details
 *  - Proporciona funciones para asegurar la conexión WiFi (`ensureWiFi`) y MQTT (`ensureMqtt`),
 *    identificando el cliente por MAC y suscribiéndose al tópico de comandos `TL_CMD_TOPIC`.
 *  - Expone `publishTrafficLight(...)` para construir y publicar el JSON de estado en
 *    `TL_STATE_TOPIC`, con parpadeo del LED de TX a través de `blinkPubLed()` (declarada extern).
 *  - Incluye utilidades de depuración (`mqttConnStateName`) y mapeo de estados (`tlStateToString`).
 *
 * @note    `isoNow()` y otras utilidades temporales/procedurales se definen en `ESP32_Utils.hpp`.
 * @warning Este módulo asume tópicos y metadatos definidos en `config.h`.
 */

// ===================== Helpers de depuración =====================
/**
 * @brief  Devuelve una etiqueta legible para el estado de `PubSubClient::state()`.
 * @param  s Código de estado devuelto por `mqtt.state()`.
 * @return Cadena constante descriptiva del estado.
 */
inline const char* mqttConnStateName(int s) {
  switch (s) {
    case -4: return "MQTT_CONNECTION_TIMEOUT";
    case -3: return "MQTT_CONNECTION_LOST";
    case -2: return "MQTT_CONNECT_FAILED";
    case -1: return "MQTT_DISCONNECTED";
    case  0: return "MQTT_CONNECTED";
    case  1: return "MQTT_CONNECT_BAD_PROTOCOL";
    case  2: return "MQTT_CONNECT_BAD_CLIENT_ID";
    case  3: return "MQTT_CONNECT_UNAVAILABLE";
    case  4: return "MQTT_CONNECT_BAD_CREDENTIALS";
    case  5: return "MQTT_CONNECT_UNAUTHORIZED";
    default: return "MQTT_UNKNOWN";
  }
}



// ============================== WIFI ==============================
/**
 * @brief  Garantiza que el ESP32 esté conectado a la red WiFi.
 * @post   Bloquea hasta conseguir `WL_CONNECTED`. Imprime trazas por Serial.
 * @note   Usa `WIFI_SSID`/`WIFI_PASSWORD` definidos en `config.h`.
 */
inline void ensureWiFi() {
  if (WiFi.status() == WL_CONNECTED) return;
  Serial.printf("[WIFI] Conectando a SSID '%s'...\n", WIFI_SSID);
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  uint32_t t0 = millis();
  while (WiFi.status() != WL_CONNECTED) {
    delay(300);
    Serial.print(".");
    if (millis() - t0 > 20000) {
      Serial.println("\n[WIFI] Timeout al conectar, reintentando...");
      t0 = millis();
    }
  }
  Serial.printf("\n[WIFI] Conectado. IP: %s  RSSI: %d dBm\n",
                WiFi.localIP().toString().c_str(), WiFi.RSSI());
}

// ============================== MQTT ==============================
/**
 * @brief  Garantiza conexión al broker MQTT y suscripción al tópico de comandos.
 * @param  mqtt Referencia al cliente `PubSubClient` ya creado con `WiFiClient`.
 * @post   Conecta (si no lo estaba), ajusta `setBufferSize(1024)` y se suscribe a `TL_CMD_TOPIC`.
 * @note   El `clientId` incluye la MAC para evitar colisiones en brokers públicos.
 * @warning Si el payload JSON creciera, considere aumentar `setBufferSize(...)`.
 */
inline void ensureMqtt(PubSubClient &mqtt) {
  if (mqtt.connected()) return;

  mqtt.setServer(MQTT_HOST, MQTT_PORT);

  // Buffer grande para JSON (por defecto ~256B es insuficiente para tu payload)
  mqtt.setBufferSize(1024);
  Serial.printf("[MQTT] Broker: %s:%u  Buffer: %u bytes\n",
                MQTT_HOST, MQTT_PORT, (unsigned)mqtt.getBufferSize());

  // ID único (evita colisiones en broker público) usando la MAC
  String mac = WiFi.macAddress(); mac.replace(":", "");
  String clientId = String("ESP32_TL_001_") + mac;

  while (!mqtt.connected()) {
    Serial.printf("[MQTT] Conectando como '%s'...\n", clientId.c_str());
    bool ok = mqtt.connect(clientId.c_str());
    if (ok) {
      Serial.println("[MQTT] Conectado.");
      // Suscripción a comandos del conjunto de semáforos
      if (mqtt.subscribe(TL_CMD_TOPIC)) {
        Serial.printf("[MQTT] SUB OK → %s\n", TL_CMD_TOPIC);
      } else {
        Serial.printf("[MQTT] SUB FAIL → %s (state=%d %s)\n",
                      TL_CMD_TOPIC, mqtt.state(), mqttConnStateName(mqtt.state()));
      }
    } else {
      int st = mqtt.state();
      Serial.printf("[MQTT] Conexión FALLIDA (state=%d %s). Reintento en 1s...\n",
                    st, mqttConnStateName(st));
      delay(1000);
    }
  }
}

// ==================== Marca de cambio de estado ====================
// Última marca temporal ISO en la que se detectó un cambio de estado del semáforo.
// @note Se actualiza en `publishTrafficLight` cuando `stateChanged == true`.
static String g_lastStateChangeIso = "";

// ==================== Mapeo de estado (vehicular) ====================
/**
 * @brief  Mapea el estado del semáforo a la cadena usada en telemetría para el bloque de vehículos.
 * @param  s Estado de la máquina (`TLState`).
 * @return Cadena: "green", "amber" o "red".
 * @note   Durante fases de peatones y todo en rojo, el estado vehicular se reporta "red".
 */
inline const char* tlStateToString(TLState s) {
  switch (s) {
    case VEH_GREEN:      return "green";
    case VEH_AMBER:      return "amber";
    case PED_GREEN:      return "red";  
    case ALL_RED_TO_PED: return "red";
    case ALL_RED_TO_CAR: return "red";
  }
  return "red";
}

// Declarada en el .ino: parpadeo del LED de publicación (TX).
extern void blinkPubLed();

// ==================== Publicación: traffic_light ====================
/**
 * @brief  Construye y publica el JSON de telemetría del semáforo en `TL_STATE_TOPIC`.
 *
 * @param  mqtt              Cliente MQTT conectado.
 * @param  state             Estado actual de la FSM del semáforo.
 * @param  stateStartMs      `millis()` en el que se entró al estado actual.
 * @param  pedWaiting        Indica si hay solicitud/espera de peatones (pulsador o lógica).
 * @param  lastCarSeenMs     `millis()` del último vehículo detectado en banda (ventana 8–12 cm).
 * @param  cycleCount        Contador de ciclos completos realizados.
 * @param  stateChanged      Si `true`, actualiza `g_lastStateChangeIso` con `isoNow()`.
 *
 * @post   Publica `doc` serializado. Si `publish` devuelve true, parpadea el LED de TX.
 * @note   El JSON incluye metadatos (sensor, calle, ubicación) y bloque `data{...}` con tiempos
 *         relativos y estado vehicular. El timestamp se obtiene de `isoNow()`.
 * @warning Mantener `mqtt.setBufferSize(...)` acorde al tamaño del JSON para evitar `publish=false`.
 */
inline void publishTrafficLight(PubSubClient &mqtt,
                                TLState state,
                                uint32_t stateStartMs,
                                bool pedWaiting,
                                uint32_t lastCarSeenMs,
                                uint32_t cycleCount,
                                bool stateChanged = false)
{
  if (stateChanged || g_lastStateChangeIso.isEmpty()) {
    g_lastStateChangeIso = isoNow();
  }

  // ---------- Construcción del documento JSON ----------
  StaticJsonDocument<512> doc;
  doc["sensor_id"]   = SENSOR_ID_TL;
  doc["sensor_type"] = "traffic_light";
  doc["street_id"]   = STREET_ID;
  doc["timestamp"]   = isoNow();   // ISO-8601 (ver implementación en ESP32_Utils.hpp)

  JsonObject loc = doc.createNestedObject("location");
  loc["latitude"]     = LOC_LAT;
  loc["longitude"]    = LOC_LON;
  loc["district"]     = LOC_DISTRICT;
  loc["neighborhood"] = LOC_BARRIO;

  // Tiempos de ciclo y restantes según estado actual
  const uint32_t now = millis();
  const uint32_t elapsed = now - stateStartMs;
  uint32_t remain = 0, cycle = 0;

  if (state == VEH_GREEN) {
    remain = (now > lastCarSeenMs && (now - lastCarSeenMs < T_NO_CAR_TIMEOUT))
             ? (T_NO_CAR_TIMEOUT - (now - lastCarSeenMs)) : 0;
    cycle = 0;
  } else if (state == VEH_AMBER) {
    remain = (elapsed < T_AMBAR) ? (T_AMBAR - elapsed) : 0;
    cycle  = T_AMBAR;
  } else if (state == ALL_RED_TO_PED) {
    remain = (elapsed < T_ALL_RED_BEFORE) ? (T_ALL_RED_BEFORE - elapsed) : 0;
    cycle  = T_ALL_RED_BEFORE;
  } else if (state == PED_GREEN) {
    remain = (elapsed < T_PED_GREEN) ? (T_PED_GREEN - elapsed) : 0;
    cycle  = T_PED_GREEN;
  } else if (state == ALL_RED_TO_CAR) {
    const uint32_t T_ALL_RED_BACK = 300;
    remain = (elapsed < T_ALL_RED_BACK) ? (T_ALL_RED_BACK - elapsed) : 0;
    cycle  = T_ALL_RED_BACK;
  }

  // Bloque de datos de telemetría
  JsonObject data = doc.createNestedObject("data");
  data["current_state"]             = tlStateToString(state);
  data["cycle_position_seconds"]    = (int)(elapsed / 1000);
  data["time_remaining_seconds"]    = (int)(remain  / 1000);
  data["cycle_duration_seconds"]    = (int)(cycle   / 1000);
  data["traffic_light_type"]        = "mixed_vehicle_pedestrian";
  data["circulation_direction"]     = "bidirectional";
  data["pedestrian_waiting"]        = pedWaiting;
  data["pedestrian_button_pressed"] = pedWaiting;   // mismo significado en esta práctica
  data["malfunction_detected"]      = false;
  data["cycle_count"]               = cycleCount;
  data["state_changed"]             = stateChanged;
  data["last_state_change"]         = g_lastStateChangeIso;

  // ---------- Serialización y publicación ----------
  static char out[700];
  size_t n = serializeJson(doc, out);

  Serial.printf("[MQTT/PUB] Topic: %s  Bytes:%u  Conectado:%d\n",
                TL_STATE_TOPIC, (unsigned)n, mqtt.connected());

  if (!mqtt.connected()) {
    Serial.printf("[MQTT/PUB] Cliente no conectado (state=%d %s). Se reintentará en loop.\n",
                  mqtt.state(), mqttConnStateName(mqtt.state()));
    return;
  }

  // Publicación con longitud explícita para evitar resolver el overload (retained)
  bool ok = mqtt.publish(TL_STATE_TOPIC, out, n);
  if (ok) {
    Serial.println("[MQTT/PUB] OK");
    // Parpadeo del LED de publicación (TX)
    blinkPubLed();
  } else {
    Serial.printf("[MQTT/PUB] FAIL (state=%d)\n", mqtt.state());
  }

}
