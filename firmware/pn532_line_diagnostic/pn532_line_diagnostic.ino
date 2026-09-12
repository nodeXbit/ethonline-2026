#include <Wire.h>
#include <driver/gpio.h>

// Standalone line-state test: never transmits an I2C command or enables NFC.
constexpr gpio_num_t kSda = GPIO_NUM_17;
constexpr gpio_num_t kScl = GPIO_NUM_18;

bool releasePins() {
  gpio_config_t config = {};
  config.pin_bit_mask = (1ULL << kSda) | (1ULL << kScl);
  config.mode = GPIO_MODE_INPUT;
  config.pull_up_en = GPIO_PULLUP_ENABLE;
  config.pull_down_en = GPIO_PULLDOWN_DISABLE;
  config.intr_type = GPIO_INTR_DISABLE;
  return gpio_config(&config) == ESP_OK;
}

void sample(const char *stage) {
  // Record before printing so serial output does not space the samples.
  int scl[10], sda[10];
  for (int i = 0; i < 10; ++i) {
    scl[i] = gpio_get_level(kScl);
    sda[i] = gpio_get_level(kSda);
    delay(50);
  }
  for (int i = 0; i < 10; ++i) {
    Serial0.printf("PN532_LINES_V1=STAGE:%s;SAMPLE:%d;SCL:%d;SDA:%d\n", stage, i, scl[i], sda[i]);
  }
}

void setup() {
  Serial0.begin(115200);
  delay(2000);
  Serial0.println("PN532_LINES_START_V1");
  if (!releasePins()) { Serial0.println("PN532_LINES_STOP_V1=INPUT_CONFIG_FAILED"); return; }
  sample("BEFORE_I2C");
  if (Wire.setBufferSize(128) != 128 || !Wire.setPins(kSda, kScl)) {
    Serial0.println("PN532_LINES_STOP_V1=WIRE_CONFIG_FAILED"); return;
  }
  const bool initialized = Wire.begin();
  Serial0.printf("PN532_LINES_BEGIN_V1=%d\n", initialized ? 1 : 0);
  if (initialized) sample("I2C_ENABLED");
  const bool ended = Wire.end();
  Serial0.printf("PN532_LINES_END_V1=%d\n", ended ? 1 : 0);
  if (!ended) { Serial0.println("PN532_LINES_STOP_V1=WIRE_END_FAILED"); return; }
  if (!releasePins()) { Serial0.println("PN532_LINES_STOP_V1=INPUT_CONFIG_FAILED"); return; }
  sample("RELEASED");
  Serial0.println("PN532_LINES_DONE_V1");
}

void loop() { delay(1000); }
