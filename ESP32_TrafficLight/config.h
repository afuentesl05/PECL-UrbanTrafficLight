#pragma once
#include <Arduino.h>

/**
 * @file    config.h
 * @brief   Configuración central (constantes y mapeo de pines) del proyecto ESP32_TrafficLight.
 *
 * @details
 *  - Contiene credenciales WiFi, datos del broker MQTT, tópicos, metadatos del sensor,
 *    ubicación representativa y asignación de pines del ESP32 (LEDs de semáforo/peatones,
 *    botón, buzzer, ultrasonidos y OLED).
 *
 * @note    Este archivo se incluye desde el `.ino` y los distintos helpers (`MQTT.hpp`,
 *          `ESP32_Utils.hpp`, `ESP32_Display.hpp`, `ESP32_Ultrasonic.hpp`).
 * @warning Las credenciales WiFi están en texto claro (válido para prácticas). 
 */

// =================== WIFI & MQTT ===================
// SSID/clave de la red a la que se conectará el ESP32 (modo estación STA).
static const char* WIFI_SSID     = "TU_WIFI";
static const char* WIFI_PASSWORD = "TU_PASS";

// Dirección del broker MQTT y puerto TCP. En prácticas, suele ser un broker público o uno local.
static const char* MQTT_HOST = "IP_BROKER";
static const uint16_t MQTT_PORT = 1883;

// Topics MQTT
// - Topic de estado donde el nodo publica su telemetría del semáforo.
// - Topic de comandos donde el nodo recibe órdenes (p. ej., force/ped_green, buzzer, etc.).
static const char* TL_STATE_TOPIC = "sensors/ST_2245/traffic_light/TL_001/state";
static const char* TL_CMD_TOPIC   = "sensors/ST_2245/traffic_light/TL_001/cmd";

// IDs y metadatos
// - Identificador lógico del conjunto de semáforos (sensor-id) y la calle asociada.
static const char* SENSOR_ID_TL = "1";
static const char* STREET_ID    = "ST_2245";

// Ubicación (punto representativo de la vía)
// - Lat/Lon de referencia (no se utiliza para navegación; solo metadato informativo).
// - Distrito/barrio/postal para enriquecer la telemetría.
static const float  LOC_LAT      = 40.4477116f;
static const float  LOC_LON      = -3.7589143f;
static const char*  LOC_DISTRICT = "Moncloa-Aravaca";
static const char*  LOC_BARRIO   = "Moncloa-Aravaca";
static const char*  POSTAL_CODE  = "28050";

// =================== PINES ESP32 ===================
// Asignación de pines para los LEDs del semáforo de vehículos (R/A/V) y peatones (R/V).
#define PIN_VEH_R 12
#define PIN_VEH_A 14
#define PIN_VEH_V 27
#define PIN_PED_R 33
#define PIN_PED_V 32
#define PIN_BTN   26          // Botón de peatones (entrada con PULLUP en el código principal)
#define PIN_BUZ   25          // Buzzer
#define BUZ_CHANNEL 0         // Canal LEDC para generación de tono PWM (si se usa variante con tono)

// LEDs de comunicaciones
// - LED de publicación (TX): parpadea cuando se publica por MQTT.
// - LED de suscripción (RX): parpadea cuando se recibe un mensaje por el topic de comandos.
#define PIN_LED_PUB 16   // parpadea al publicar (azul)
#define PIN_LED_SUB 17   // parpadea al recibir (subscribe) (blanco)

// Sensor ultrasónico HC-SR04
// - TRIG: salida del ESP32
// - ECHO: entrada con divisor resistivo a 3.3V (el HC-SR04 da 5V en ECHO)
#define PIN_TRIG  5
#define PIN_ECHO  18  // con divisor a 3.3V

// I2C OLED (SSD1306 típico)
#define OLED_SDA  21
#define OLED_SCL  22
#define OLED_ADDR 0x3C
#define OLED_W    128
#define OLED_H    64

// =================== TIEMPOS (ms) ===================
// Constantes temporales que controlan las fases del semáforo y señales auxiliares.
// - T_AMBAR: duración del ámbar para vehículos antes de ceder paso.
// - T_ALL_RED_BEFORE: todo en rojo previo a abrir peatones (interbloqueo de seguridad).
// - T_PED_GREEN: tiempo de paso para peatones (con buzzer activo si está habilitado).
// - T_NO_CAR_TIMEOUT: ausencia continua de coches tras la cual se cede a peatones.
// - BEEP_PERIOD: periodo de parpadeo/alternancia del buzzer durante el verde de peatones.
static const uint32_t T_AMBAR            = 2000;
static const uint32_t T_ALL_RED_BEFORE   = 1000;
static const uint32_t T_PED_GREEN        = 12000;
static const uint32_t T_NO_CAR_TIMEOUT   = 25000;
static const uint32_t BEEP_PERIOD        = 500
;
