/*
  ESP32_TrafficLight (main)
  - Vehículos en VERDE por defecto
  - Paso a peatones SIEMPRE: por botón inmediato o por 25 s sin coches (8–12 cm)
  - Ámbar 2 s; peatones 12 s con buzzer ACTIVO (HIGH = suena)

  ===========================================================================
  @file   ESP32_TrafficLight.ino
  @brief  Bucle principal y FSM del conjunto de semáforos + integración MQTT.
  @details
    • Gestiona estados del semáforo (vehículos/peatones), sensor HC-SR04,
      OLED, buzzer y telemetría MQTT con LEDs de TX/RX.
    • Incluye callback MQTT para recibir órdenes (buzzer/force).
    • Publica telemetría periódica y en cambios de estado.
  ===========================================================================
*/
#define DEBUG_TIMEOUT 1  // Activa trazas de cuenta atrás (25 s sin coches)

#include <Arduino.h>
#include <PubSubClient.h>
#include "config.h"
#include "ESP32_Utils.hpp"
#include "ESP32_Display.hpp"
#include "ESP32_Ultrasonic.hpp"
#include "MQTT.hpp"

// =================== GLOBALES ===================
// Conectividad
WiFiClient espClient;
PubSubClient mqtt(espClient);

// FSM del semáforo
TLState   state             = VEH_GREEN; // Estado actual
uint32_t  stateStart        = 0;         // millis() de entrada al estado
uint32_t  lastVehGreenStart = 0;         // millis() del último VEH_GREEN
uint32_t  lastCarSeen       = 0;         // millis() de última detección de coche

// Buzzer (parpadeo durante peatones en verde)
bool      buzzerEnabled = true;
bool      beepOn        = false;
uint32_t  lastBeep      = 0;

// Conteo de vehículos por ciclo de peatones
uint32_t     vehicleCountCycle = 0;
CounterState cntState;

// Telemetría y ciclos completos
uint32_t  cycleCount     = 0;
uint32_t  lastTelemetry  = 0;

// Control de botón de peatones (petición con retardo no bloqueante)
bool      pedDelayPending = false;        // hay petición de peatones en espera
uint32_t  pedDelayStart   = 0;            // millis() de la pulsación
const uint32_t T_BUTTON_DELAY = 2000;     // 2 s de gracia antes de pasar a ámbar

// =================== Helpers ===================
/**
 * @brief  Activa/Desactiva el buzzer (ON/OFF simple).
 * @param  on true → HIGH (suena), false → LOW (silencio).
 */
inline void buzzerSet(bool on) { digitalWrite(PIN_BUZ, on ? HIGH : LOW); }

// =================== MQTT callback ===================
/**
 * @brief  Gestión de mensajes entrantes por el topic de comandos (TL_CMD_TOPIC).
 * @param  topic   Tópico recibido.
 * @param  payload Datos (JSON esperado).
 * @param  len     Longitud del payload.
 *
 * @details
 *  - Traza por serie el topic/payload crudos.
 *  - Parpadea LED de RX.
 *  - Soporta claves: { "buzzer": bool }, { "force": "veh_green"|"ped_green" }.
 *  - Si hay cambio de estado, publica telemetría inmediata.
 */
void onMqttMessage(char* topic, byte* payload, unsigned int len) {
  // ====== Traza RX: topic + payload crudo ======
  Serial.printf("[MQTT RX] topic='%s' len=%u payload='", topic, len);
  for (unsigned int i = 0; i < len; ++i) Serial.write(payload[i]);
  Serial.println("'");

  // ====== Parpadeo LED de recepción ======
  blinkSubLed();

  // Parseo JSON
  StaticJsonDocument<256> doc;
  if (deserializeJson(doc, payload, len)) {
    Serial.println("[MQTT RX] JSON inválido");
    return;
  }
  bool stateChanged = false;

  // ---- Control de buzzer ----
  if (doc.containsKey("buzzer")) {
    buzzerEnabled = doc["buzzer"];
    Serial.printf("[MQTT RX] buzzerEnabled = %s\n",
                  buzzerEnabled ? "true" : "false");
  }

  // ---- Fuerza de estado / peticiones remotas ----
  if (doc.containsKey("force")) {
    String f = doc["force"].as<String>();
    Serial.printf("[MQTT RX] force='%s'\n", f.c_str());

    // Forzar vehículos en verde (esto sí lo seguimos haciendo inmediato)
    if (f == "veh_green" && state != VEH_GREEN) {
      state             = VEH_GREEN;
      stateStart        = millis();
      lastVehGreenStart = stateStart;
      setVehLights(false, false, true);
      setPedLights(true, false);
      buzzerSet(false);

      // limpiamos cualquier petición pendiente de peatones
      pedDelayPending = false;
      stateChanged = true;
    }

    // "ped_green" ahora se interpreta como "simular pulsación de botón"
    if (f == "ped_green") {
      if (state == VEH_GREEN) {
        if (!pedDelayPending) {
          pedDelayPending = true;
          pedDelayStart   = millis();
          Serial.println(
            "[MQTT RX] ped_green => simulando pulsación de botón peatones"
          );
        } else {
          Serial.println(
            "[MQTT RX] ped_green recibido pero ya había petición pendiente"
          );
        }
      } else {
        Serial.println(
          "[MQTT RX] ped_green recibido pero semáforo no está en VEH_GREEN, se ignora"
        );
      }
    }
  }

  // Solo publicamos telemetría inmediata si realmente hemos cambiado
  // de estado dentro de este callback (veh_green forzado).
  if (stateChanged) {
    publishTrafficLight(mqtt, state, stateStart, false, lastCarSeen, cycleCount);
  }
}



// ====== LEDs de comunicaciones (blink no bloqueante) ======
// Gestión de parpadeo (TX/RX) sin bloquear el loop principal.
static uint32_t lastPubBlink = 0, lastSubBlink = 0;
static const uint32_t LED_BLINK_MS = 120;  // Duración del destello

/** @brief Solicita destello del LED de publicación (TX). */
inline void blinkPubLed() {
  digitalWrite(PIN_LED_PUB, HIGH);
  lastPubBlink = millis();
}
/** @brief Solicita destello del LED de suscripción (RX). */
inline void blinkSubLed() {
  digitalWrite(PIN_LED_SUB, HIGH);
  lastSubBlink = millis();
}
/**
 * @brief  Servicio de apagado de LEDs de comunicaciones cuando expira el tiempo de blink.
 * @note   Debe llamarse en cada iteración del loop.
 */
inline void serviceCommLeds() {
  const uint32_t now = millis();
  if (digitalRead(PIN_LED_PUB) == HIGH && now - lastPubBlink >= LED_BLINK_MS) {
    digitalWrite(PIN_LED_PUB, LOW);
  }
  if (digitalRead(PIN_LED_SUB) == HIGH && now - lastSubBlink >= LED_BLINK_MS) {
    digitalWrite(PIN_LED_SUB, LOW);
  }
}

/**
 * @brief  Test de cableado de LEDs de comunicaciones (enciende ambos de forma fija).
 * @note   Útil para verificación rápida de hardware; no se usa en la versión final.
 */
void testCommLedsWiring() {
  pinMode(PIN_LED_PUB, OUTPUT);
  pinMode(PIN_LED_SUB, OUTPUT);
  digitalWrite(PIN_LED_PUB, HIGH);
  digitalWrite(PIN_LED_SUB, HIGH);
}


// =================== SETUP ===================
/**
 * @brief  Inicialización de pines, periféricos, WiFi/MQTT y estado inicial de la FSM.
 * @post   Publica primer mensaje de telemetría.
 */
void setup() {
  Serial.begin(115200);

  // Pines de salida/entrada
  pinMode(PIN_VEH_R, OUTPUT);
  pinMode(PIN_VEH_A, OUTPUT);
  pinMode(PIN_VEH_V, OUTPUT);
  pinMode(PIN_PED_R, OUTPUT);
  pinMode(PIN_PED_V, OUTPUT);
  pinMode(PIN_TRIG,  OUTPUT);
  pinMode(PIN_ECHO,  INPUT);
  pinMode(PIN_BTN,   INPUT_PULLUP);
  pinMode(PIN_BUZ,   OUTPUT);

  pinMode(PIN_LED_PUB, OUTPUT);
  pinMode(PIN_LED_SUB, OUTPUT);
  digitalWrite(PIN_LED_PUB, LOW);
  digitalWrite(PIN_LED_SUB, LOW);

  // Buzzer: beep de saludo (feedback de encendido)
  buzzerSet(false);
  buzzerSet(true); delay(120); buzzerSet(false);

  // OLED
  if (!displayInit()) Serial.println("[OLED] ERROR al iniciar pantalla");

  // Estado inicial (vehículos en verde, peatones rojo)
  setVehLights(false,false,true);
  setPedLights(true,false);

  state             = VEH_GREEN;
  stateStart        = millis();
  lastVehGreenStart = stateStart;
  lastCarSeen       = millis(); // evita disparo inmediato por "no hay coches"

  // Conectividad y reloj
  ensureWiFi();
  timeInitOnceUTC();                 // Inicializa NTP 
  mqtt.setServer(MQTT_HOST, MQTT_PORT);
  mqtt.setCallback(onMqttMessage);
  ensureMqtt(mqtt);

  // Telemetría inicial
  publishTrafficLight(mqtt, state, stateStart, false, lastCarSeen, cycleCount);
}

// ================ BOTÓN (antirrebote simple) =================
// Antirrebote minimalista por comparación con memoria de estado y retardo.
static bool     btnLast = true;           // PULLUP → reposo HIGH
static uint32_t btnLastChangeMs = 0;
const  uint32_t BTN_DEBOUNCE_MS = 35;

/**
 * @brief  Detecta flanco de pulsado del botón (LOW) con antirrebote por tiempo.
 * @return true solo en el instante del flanco (pulsación nueva).
 */
inline bool buttonPressedEdge() {
  bool nowLevel = (digitalRead(PIN_BTN) == LOW);
  uint32_t now = millis();
  if (nowLevel != btnLast && (now - btnLastChangeMs) >= BTN_DEBOUNCE_MS) {
    btnLast = nowLevel; btnLastChangeMs = now;
    return (nowLevel == LOW); // flanco a pulsado
  }
  return false;
}

// =================== LOOP ===================
/**
 * @brief  Bucle principal: servicio de conectividad, lectura de sensores,
 *         FSM del semáforo, actualización de OLED/buzzer y telemetría periódica.
 */
void loop() {
  // Servicio de conectividad MQTT
  ensureWiFi();
  ensureMqtt(mqtt);
  mqtt.loop();

  // Servicio de apagado no bloqueante de LEDs de comunicaciones
  serviceCommLeds();

  const uint32_t now = millis();

  // -------- Ultrasonidos: presencia y conteo --------
  float cm = readDistanceCm(PIN_TRIG, PIN_ECHO);
  bool  valid   = (cm > 0.5f);
  bool  inBand  = valid && (cm > CAR_MIN_CM && cm < CAR_MAX_CM); // 8–12 cm

  // Guardamos el valor previo para detectar reinicios de la cuenta
  uint32_t lastCarSeen_before = lastCarSeen;

  if (inBand) {
    lastCarSeen = now;   // presencia real de coche → reinicia timeout
  }
  counterUpdate(cntState, cm, vehicleCountCycle); // conteo robusto (flanco de salida)

  // -------- DEBUG: cuenta atrás de los 25 s sin coches --------
  #if DEBUG_TIMEOUT
  {
    // Tiempo desde el último coche y tiempo restante hasta el timeout
    uint32_t since     = now - lastCarSeen;
    uint32_t remaining = (since >= T_NO_CAR_TIMEOUT) ? 0 : (T_NO_CAR_TIMEOUT - since);

    // Imprimir solo cuando cambia el segundo mostrado o cuando se reinicia
    static uint32_t lastShownSec = 0xFFFFFFFF;
    uint32_t secRem = remaining / 1000U;

    // Si acabamos de ver coche, anunciar reinicio
    if (lastCarSeen != lastCarSeen_before) {
      Serial.printf("[TIMEOUT] Reinicio por coche. Cuenta=25s\n");
      lastShownSec = 0xFFFFFFFF; // fuerza impresión del nuevo segundo
    }

    if (secRem != lastShownSec) {
      lastShownSec = secRem;
      // Formato mm:ss 
      uint32_t mm = secRem / 60U;
      uint32_t ss = secRem % 60U;
      Serial.printf("[TIMEOUT] Restante: %02lu:%02lu (%lus)\n",
                    (unsigned long)mm, (unsigned long)ss, (unsigned long)secRem);
    }

    // Mensaje específico cuando llega a 0
    static bool announcedZero = false;
    if (remaining == 0 && !announcedZero) {
      announcedZero = true;
      Serial.println("[TIMEOUT] 25s sin coches → ceder a peatones");
    }
    if (remaining > 0 && announcedZero) {
      announcedZero = false; // se reinició, permitimos anunciar de nuevo en el futuro
    }
  }
  #endif

  // -------- Disparo por BOTÓN con retardo no bloqueante (2 s) --------
  if (state == VEH_GREEN && buttonPressedEdge()) {   // flanco de pulsado
    if (!pedDelayPending) {
      pedDelayPending = true;
      pedDelayStart   = now;
      Serial.println("[BTN] Pulsado: iniciando espera 2 s antes de pasar a ámbar");
      
    }
  }

  // -------- Máquina de estados --------
  switch (state) {

    case VEH_GREEN: {
      setVehLights(false,false,true);
      setPedLights(true,false);
      buzzerSet(false);

      // Si 25 s seguidos sin coches (en ventana 8–12 cm) → ceder a peatones
      if (now - lastCarSeen >= T_NO_CAR_TIMEOUT) {
        pedDelayPending = false;               // limpiar petición (si la hubiera)
        state = VEH_AMBER; stateStart = now;   // paso a ámbar
        publishTrafficLight(mqtt, state, stateStart, false, lastCarSeen, cycleCount);
      }
      // Si hay petición pendiente y ya pasaron 2 s → pasar a ámbar
      if (pedDelayPending && (now - pedDelayStart >= T_BUTTON_DELAY)) {
        pedDelayPending = false;           // consumir la petición
        state = VEH_AMBER;
        stateStart = now;
        publishTrafficLight(mqtt, state, stateStart, true, lastCarSeen, cycleCount);
      }

    } break;

    case VEH_AMBER: {
      setVehLights(false,true,false);
      setPedLights(true,false);
      if (now - stateStart >= T_AMBAR) {
        state = ALL_RED_TO_PED; stateStart = now;
        publishTrafficLight(mqtt, state, stateStart, false, lastCarSeen, cycleCount);
      }
    } break;

    case ALL_RED_TO_PED: {
      setVehLights(true,false,false);
      setPedLights(true,false);
      if (now - stateStart >= T_ALL_RED_BEFORE) {
        state = PED_GREEN; stateStart = now;
        publishTrafficLight(mqtt, state, stateStart, false, lastCarSeen, cycleCount);
      }
    } break;

    case PED_GREEN: {
      setVehLights(true,false,false);
      setPedLights(false,true);

      // Buzzer activo (parpadeo cada BEEP_PERIOD)
      if (buzzerEnabled) {
        if (now - lastBeep >= BEEP_PERIOD) { beepOn = !beepOn; lastBeep = now; }
        buzzerSet(beepOn);
      } else {
        buzzerSet(false);
      }

      // Cuenta atrás en OLED (12 s)
      const uint32_t elapsed   = now - stateStart;
      const uint32_t remaining = (elapsed >= T_PED_GREEN) ? 0 : (T_PED_GREEN - elapsed);
      oledCountdown(remaining);

      if (elapsed >= T_PED_GREEN) {
        state = ALL_RED_TO_CAR; stateStart = now;
        publishTrafficLight(mqtt, state, stateStart, false, lastCarSeen, cycleCount);
      }
    } break;

    case ALL_RED_TO_CAR: {
      setVehLights(true,false,false);
      setPedLights(true,false);
      buzzerSet(false);
      display.clearDisplay(); display.display();

      if (now - stateStart >= 300) {
        // Vuelta a vehículos en verde
        pedDelayPending = false;

        state              = VEH_GREEN;
        stateStart         = now;
        lastVehGreenStart  = now;

        // Reseteos críticos
        lastCarSeen        = now;     // evita disparo inmediato por no-coches
        beepOn             = false;
        vehicleCountCycle  = 0;
        cntState           = CounterState(); // reset ventana HC-SR04

        cycleCount++;
        publishTrafficLight(mqtt, state, stateStart, false, lastCarSeen, cycleCount);
      }
    } break;
  }

  // Telemetría periódica (cada 5 s)
  if (now - lastTelemetry >= 5000) {
    publishTrafficLight(mqtt, state, stateStart, false, lastCarSeen, cycleCount);
    lastTelemetry = now;
  }
}
