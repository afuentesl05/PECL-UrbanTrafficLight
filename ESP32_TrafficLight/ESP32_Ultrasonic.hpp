#pragma once
#include <Arduino.h>

/**
 * @file    ESP32_Ultrasonic.hpp
 * @brief   Utilidades de medición/filtrado con HC-SR04 y conteo robusto de “vehículos” en ventana 8–12 cm.
 *
 * @details
 *  - Lectura: `readDistanceCmSingle()` efectúa un disparo único (pulso TRIG + `pulseIn` con *timeout*).
 *    `readDistanceCm()` devuelve la media de hasta 3 lecturas válidas para suavizar ruido.
 *  - Conteo: `counterUpdate()` implementa lógica de flanco de salida con:
 *      - Ventana de presencia (CAR_MIN_CM, CAR_MAX_CM).
 *      - Permanencia mínima `MIN_INBAND_MS`.
 *      - Refractario `REFRACTORY_MS` para evitar rebotes/multipases.
 *  - Header-only: todas las funciones son `inline` para uso directo desde el `.ino`.
 * @warning El pin ECHO del HC-SR04 entrega 5 V → obligatorio divisor a 3.3 V al pin del ESP32.
 */

/*
  ESP32_Ultrasonic.hpp (header-only)
  - readDistanceCm(trig, echo): medición con HC-SR04 (timeout y media simple)
  - Conteo de vehículos con ventana (8–12 cm), antirrebotes y refractario
  - Evita redefiniciones de umbrales con #ifndef
*/

// ================== Parámetros de presencia/ventana ==================
#ifndef CAR_MIN_CM
#define CAR_MIN_CM 8.0f   // límite inferior EXCLUSIVO de la ventana de presencia
#endif
#ifndef CAR_MAX_CM
#define CAR_MAX_CM 12.0f  // límite superior EXCLUSIVO de la ventana de presencia
#endif

// ================== Parámetros de robustez para conteo ===============
static const uint32_t MIN_INBAND_MS = 120;   // Permanencia mínima dentro de la ventana para validar presencia
static const uint32_t REFRACTORY_MS = 600;   // Tiempo mínimo entre conteos para evitar múltiples incrementos por un mismo paso
static const float    MIN_VALID_CM  = 0.5f;  // Umbral para descartar lecturas inválidas (0/negativas/ruido)

// ================== Lectura ultrasónica ==============================
/**
 * @brief  Disparo único del HC-SR04 y cálculo de distancia (cm) con timeout.
 * @param  trigPin   Pin TRIG (salida).
 * @param  echoPin   Pin ECHO (entrada, con divisor a 3.3 V).
 * @param  timeout_us Tiempo máx. de espera para HIGH en ECHO (μs). Por defecto 30000 μs.
 * @return Distancia en centímetros (cm) o -1.0 si timeout/lectura inválida.
 *
 * @note   Distancia ≈ (tiempo_ECHO_high * 0.0343) / 2, asumiendo v_sonido ≈ 343 m/s.
 */
inline float readDistanceCmSingle(int trigPin, int echoPin, uint32_t timeout_us = 30000UL) {
  // Pulso de disparo: LOW 2 us -> HIGH 10 us -> LOW
  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);

  // Espera eco en HIGH con timeout
  unsigned long duration = pulseIn((uint8_t)echoPin, HIGH, timeout_us);
  if (duration == 0) {
    // timeout → lectura inválida
    return -1.0f;
  }
  const float distance = (duration * 0.0343f) * 0.5f; // cm
  return distance;
}

/**
 * @brief  Lectura “suave”: media de hasta 3 lecturas válidas consecutivas.
 * @param  trigPin Pin TRIG (salida).
 * @param  echoPin Pin ECHO (entrada con divisor).
 * @return Distancia promedio (cm) o -1.0 si ninguna lectura fue válida.
 *
 * @note   Inserta una breve separación (5 ms) entre disparos para reducir acoples/eco residual.
 */
inline float readDistanceCm(int trigPin, int echoPin) {
  float acc = 0.0f;
  int   n   = 0;
  for (int i = 0; i < 3; ++i) {
    float d = readDistanceCmSingle(trigPin, echoPin);
    if (d > 0.0f) { acc += d; n++; }
    delay(5); // pequeña separación entre disparos
  }
  if (n == 0) return -1.0f;
  return acc / n;
}

// ================== Estado y lógica de conteo ========================
/**
 * @struct CounterState
 * @brief  Estado interno del algoritmo de conteo por ventana con flanco de salida.
 *
 * @var CounterState::inBand        Flag: actualmente dentro de la ventana (CAR_MIN_CM, CAR_MAX_CM).
 * @var CounterState::firstInBandAt Instante `millis()` al entrar en ventana (para medir permanencia).
 * @var CounterState::lastCountAt   Instante del último incremento (para respetar `REFRACTORY_MS`).
 */
struct CounterState {
  bool      inBand         = false;   // estamos dentro de (CAR_MIN_CM, CAR_MAX_CM)
  uint32_t  firstInBandAt  = 0;       // instante de entrada en ventana
  uint32_t  lastCountAt    = 0;       // último instante de incremento
};

/**
 * @brief  Actualiza el estado de conteo con la lectura actual.
 *
 * @param  st            Estado interno (persistente entre llamadas).
 * @param  cm            Distancia medida (cm). Usualmente salida de `readDistanceCm(...)`.
 * @param  vehicleCount  Contador acumulado de vehículos (se incrementa en flanco de salida).
 *
 * @details
 *  - Define presencia cuando la distancia está exclusivamente en (CAR_MIN_CM, CAR_MAX_CM).
 *  - Solo incrementa al salir de la ventana si:
 *      1) se ha permanecido ≥ `MIN_INBAND_MS`, y
 *      2) han pasado ≥ `REFRACTORY_MS` desde el último conteo.
 *  - Lecturas inválidas (≤ `MIN_VALID_CM`) se tratan como “fuera de ventana”.
 */
inline void counterUpdate(CounterState& st, float cm, uint32_t& vehicleCount) {
  const uint32_t now = millis();

  const bool valid     = (cm > MIN_VALID_CM);
  const bool inBandNow = (valid && (cm > CAR_MIN_CM) && (cm < CAR_MAX_CM));

  if (inBandNow) {
    if (!st.inBand) {
      st.inBand = true;
      st.firstInBandAt = now;
    }
    return; // seguimos dentro; el conteo se verifica al salir
  }

  // Estamos fuera de ventana
  if (st.inBand) {
    const uint32_t inBandDur = now - st.firstInBandAt;
    const bool longEnough    = (inBandDur >= MIN_INBAND_MS);
    const bool refractoryOK  = (now - st.lastCountAt >= REFRACTORY_MS);

    if (longEnough && refractoryOK) {
      vehicleCount++;
      st.lastCountAt = now;
    }
    st.inBand = false;
  }
}

/**
 * @brief  Restablece explícitamente el estado interno del conteo.
 * @param  st Estado a limpiar.
 * @post   `inBand=false`, `firstInBandAt=0`, `lastCountAt=0`.
 */
inline void counterReset(CounterState& st) {
  st.inBand = false;
  st.firstInBandAt = 0;
  st.lastCountAt = 0;
}
