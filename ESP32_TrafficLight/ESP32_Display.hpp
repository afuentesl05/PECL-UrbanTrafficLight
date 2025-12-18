#pragma once
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include "config.h"

/**
 * @file    ESP32_Display.hpp
 * @brief   Encapsulado mínimo para manejo de pantalla OLED SSD1306 (128x64) vía I2C.
 *
 * @details
 *  - Declara una instancia global estática `display` (Adafruit_SSD1306) con el bus I2C
 *    proporcionado por `Wire` y sin pin de reset (se pasa -1).
 *  - Proporciona:
 *      - `displayInit()` para inicializar I2C y el controlador SSD1306.
 *      - `oledCountdown(remainingMs)` para mostrar una cuenta atrás de “PEATONES”.
 *
 * @pre     Librerías Adafruit_GFX y Adafruit_SSD1306 instaladas.
 * @note    Los pines, dirección I2C y dimensiones se toman de `config.h`:
 *          `OLED_SDA`, `OLED_SCL`, `OLED_ADDR`, `OLED_W`, `OLED_H`.
 * @warning Llamar a `displayInit()` una vez en `setup()` antes de usar cualquier función
 *          de dibujo. Si `display.begin(...)` falla, `display` no debe usarse.
 */

// =================== INSTANCIA GLOBAL DEL DISPLAY ===================
// Declaración del display
// - Dimensiones: OLED_W x OLED_H (por defecto 128x64).
// - Bus: Wire (I2C) y sin pin de reset (se pasa -1).
static Adafruit_SSD1306 display(OLED_W, OLED_H, &Wire, -1);

/**
 * @brief  Inicializa bus I2C y controlador SSD1306.
 * @return `true` si la inicialización del display es correcta; `false` en caso contrario.
 *
 * @post   La pantalla se limpia y se envía un primer `display()` para reflejar el estado.
 * @note   `Wire.begin(OLED_SDA, OLED_SCL)` configura los pines I2C definidos en `config.h`.
 */
inline bool displayInit() {
  Wire.begin(OLED_SDA, OLED_SCL);
  bool ok = display.begin(SSD1306_SWITCHCAPVCC, OLED_ADDR);
  display.clearDisplay();
  display.display();
  return ok;
}

/**
 * @brief  Muestra una cuenta atrás grande centrada verticalmente bajo el rótulo "PEATONES".
 * @param  remainingMs  Tiempo restante en milisegundos. Se redondea hacia arriba a segundos.
 *
 * @details
 *  - Limpia completamente la pantalla y pinta:
 *      1) Cabecera “PEATONES” con tamaño de fuente 1.
 *      2) Número de segundos en tamaño de fuente 3, seguido de “ s”.
 *  - Llama a `display.display()` para enviar el buffer interno al panel.
 *
 * @note   El cálculo `s = (remainingMs + 999) / 1000` evita mostrar “0 s” prematuramente.
 */
inline void oledCountdown(uint32_t remainingMs) {
  display.clearDisplay();

  // Cabecera
  display.setTextSize(1);
  display.setTextColor(SSD1306_WHITE);
  display.setCursor(0, 0);
  display.println("PEATONES");

  // Conteo grande 
  display.setTextSize(3);
  display.setCursor(0, 24);
  uint32_t s = (remainingMs + 999) / 1000;
  display.print(s);
  display.println(" s");

  // Envío del buffer a la pantalla
  display.display();
}
