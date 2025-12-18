#pragma once
#include <Arduino.h>
#include "config.h"

// Estados del semáforo
enum TLState { VEH_GREEN, VEH_AMBER, ALL_RED_TO_PED, PED_GREEN, ALL_RED_TO_CAR };

// Control LEDs vehículos
inline void setVehLights(bool r, bool a, bool v) {
  digitalWrite(PIN_VEH_R, r);
  digitalWrite(PIN_VEH_A, a);
  digitalWrite(PIN_VEH_V, v);
}

// Control LEDs peatones
inline void setPedLights(bool r, bool g) {
  digitalWrite(PIN_PED_R, r);
  digitalWrite(PIN_PED_V, g);
}

// Buzzer simple ON/OFF (usa LEDC canal 0)
inline void buzzerPlay(bool on) {
  if (on) ledcWriteTone(BUZ_CHANNEL, 2000); // 2kHz suave
  else    ledcWriteTone(BUZ_CHANNEL, 0);
}

#include <time.h>

// Llamar una vez tras tener WiFi
inline void timeInitOnceUTC() {
  static bool done = false;
  if (done) return;

  // Desfase de +3600 s → UTC+1 (zona horaria tipo Madrid, sin DST)
  configTime(3600, 0, "pool.ntp.org", "time.google.com", "time.windows.com");

  Serial.print("[TIME] Sincronizando NTP");
  // Esperar a que SNTP rellene el reloj del sistema (máx. ~6 s)
  for (int i = 0; i < 30; ++i) {
    time_t now = time(nullptr);
    if (now > 1700000000) {
      Serial.println(" OK");
      done = true;
      return;
    }
    Serial.print(".");
    delay(200);
  }
  Serial.println(" FALLÓ (se usará 'millis()' como respaldo).");
}

inline String isoNow() {
  time_t now = time(nullptr);
  if (now > 1700000000) {            // reloj válido → formatear hora local (UTC+1)
    struct tm tm;
    // Usamos hora local, que ya está en UTC+1 gracias a configTime(3600, 0, ...)
    localtime_r(&now, &tm);

    char buf[40];
    // ISO-8601 con offset explícito +01:00
    strftime(buf, sizeof(buf), "%Y-%m-%dT%H:%M:%S.000+01:00", &tm);
    return String(buf);
  } else {                           // respaldo si no hubo NTP
    char buf[32];
    uint32_t ms = millis();
    uint32_t h = (ms / 3600000UL) % 24UL;
    uint32_t m = (ms / 60000UL)   % 60UL;
    uint32_t s = (ms / 1000UL)    % 60UL;
    snprintf(buf, sizeof(buf), "1970-01-01T%02u:%02u:%02u.000Z", h, m, s);
    return String(buf);
  }
}
