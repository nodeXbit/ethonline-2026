#pragma once

#include <Arduino.h>
#include <driver/gpio.h>

// Run before Wire owns these pins. HIGH always releases an open-drain line.
// NXP UM10204 section 3.1.16: at most nine clocks for SDA stuck LOW.
inline bool recoverPn532Bus(int sdaPin, int sclPin) {
  const auto sda = static_cast<gpio_num_t>(sdaPin);
  const auto scl = static_cast<gpio_num_t>(sclPin);
  gpio_config_t config = {};
  config.pin_bit_mask = (1ULL << sdaPin) | (1ULL << sclPin);
  config.mode = GPIO_MODE_INPUT;
  config.pull_up_en = GPIO_PULLUP_ENABLE;
  config.pull_down_en = GPIO_PULLDOWN_DISABLE;
  config.intr_type = GPIO_INTR_DISABLE;
  if (gpio_config(&config) != ESP_OK) return false;
  auto waitHigh = [](gpio_num_t pin) {
    const uint32_t started = micros();
    while (!gpio_get_level(pin)) {
      if (micros() - started >= 10000) return false;
      delayMicroseconds(5);
    }
    return true;
  };
  const int initialScl = gpio_get_level(scl);
  const int initialSda = gpio_get_level(sda);
  unsigned pulses = 0;
  bool ok = waitHigh(scl);
  if (ok && !gpio_get_level(sda)) {
    // Preload release levels before enabling open-drain outputs.
    ok = gpio_set_level(sda, 1) == ESP_OK && gpio_set_level(scl, 1) == ESP_OK;
    config.mode = GPIO_MODE_INPUT_OUTPUT_OD;
    ok = ok && gpio_config(&config) == ESP_OK;
    while (ok && pulses < 9 && !gpio_get_level(sda)) {
      ok = gpio_set_level(scl, 0) == ESP_OK;
      delayMicroseconds(5);
      ok = ok && gpio_set_level(scl, 1) == ESP_OK && waitHigh(scl);
      if (ok) ++pulses;
      delayMicroseconds(5);
    }
    ok = ok && gpio_get_level(sda);
    if (ok) {
      // STOP: pull SDA low only while SCL is low, then release SCL and SDA.
      ok = gpio_set_level(scl, 0) == ESP_OK && gpio_set_level(sda, 0) == ESP_OK;
      delayMicroseconds(5);
      ok = ok && gpio_set_level(scl, 1) == ESP_OK && waitHigh(scl);
      delayMicroseconds(5);
      const bool released = gpio_set_level(sda, 1) == ESP_OK;
      ok = ok && released;
      delayMicroseconds(5);
    }
  }
  // Release outputs on every outcome, including a clock-stretch timeout.
  config.mode = GPIO_MODE_INPUT;
  const bool released = gpio_config(&config) == ESP_OK;
  ok = ok && released && gpio_get_level(scl) && gpio_get_level(sda);
  Serial0.printf("PN532_BUS_CLEAR_V1=INITIAL_SCL:%d;INITIAL_SDA:%d;PULSES:%u;SCL:%d;SDA:%d;OK:%d\n",
                 initialScl, initialSda, pulses, gpio_get_level(scl), gpio_get_level(sda), ok ? 1 : 0);
  return ok;
}
