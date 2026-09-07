#include <Wire.h>
#include <PN532_I2C.h>
#include <PN532.h>

namespace {
constexpr int kSdaPin = 17;
constexpr int kSclPin = 18;
constexpr uint8_t kPn532Address = 0x24;
constexpr uint32_t kSerialBaud = 115200;

PN532_I2C pn532Transport(Wire);
PN532 nfc(pn532Transport);

void stopWithError(const char *message) {
  Serial0.print("FATAL: ");
  Serial0.println(message);
  while (true) {
    delay(1000);
  }
}

void printUid(const uint8_t *uid, uint8_t uidLength) {
  for (uint8_t i = 0; i < uidLength; ++i) {
    if (uid[i] < 0x10) {
      Serial0.print('0');
    }
    Serial0.print(uid[i], HEX);
    if (i + 1 < uidLength) {
      Serial0.print(':');
    }
  }
}

bool sendCommandWithValidatedResponse(const char *label,
                                      const uint8_t *command,
                                      uint8_t commandLength) {
  const int8_t writeStatus =
      pn532Transport.writeCommand(command, commandLength);
  if (writeStatus != 0) {
    Serial0.printf("%s write failed (status %d)\n", label, writeStatus);
    return false;
  }

  uint8_t response[8] = {};
  const int16_t payloadLength =
      pn532Transport.readResponse(response, sizeof(response), 1000);
  if (payloadLength < 0) {
    Serial0.printf("%s response failed (status %d)\n", label, payloadLength);
    return false;
  }

  Serial0.printf("%s accepted (response payload=%d)\n", label,
                 payloadLength);
  return true;
}
}  // namespace

void setup() {
  Serial0.begin(kSerialBaud);
  delay(2000);

  Serial0.println();
  Serial0.println("=== ESP32-S3 + PN532 I2C NFC TAG TEST ===");
  Serial0.printf("I2C pins: SDA=GPIO%d SCL=GPIO%d\n", kSdaPin, kSclPin);

  // PN532_I2C::begin() calls Wire.begin() without pin arguments. The ESP32
  // Wire implementation requires custom pins to be selected before that call.
  if (!Wire.setPins(kSdaPin, kSclPin)) {
    stopWithError("Wire.setPins failed");
  }
  nfc.begin();

  Wire.beginTransmission(kPn532Address);
  const uint8_t i2cError = Wire.endTransmission();
  if (i2cError != 0) {
    Serial0.printf("I2C preflight failed at 0x%02X (error %u)\n",
                   kPn532Address, i2cError);
    stopWithError("raw I2C address 0x24 not detected");
  }
  Serial0.println("I2C preflight: PN532 ACK at 0x24");

  const uint32_t version = nfc.getFirmwareVersion();
  if (version == 0) {
    stopWithError("PN532 getFirmwareVersion failed");
  }

  Serial0.printf("PN532 detected: chip=PN5%02X firmware=%u.%u support=0x%02X\n",
                 static_cast<unsigned>((version >> 24) & 0xFF),
                 static_cast<unsigned>((version >> 16) & 0xFF),
                 static_cast<unsigned>((version >> 8) & 0xFF),
                 static_cast<unsigned>(version & 0xFF));

  // PN532::SAMConfig() in this Elechouse library incorrectly requires a
  // positive payload length. SAMConfiguration's valid response has no
  // payload, so PN532_I2C::readResponse() correctly returns zero.
  const uint8_t samCommand[] = {
      PN532_COMMAND_SAMCONFIGURATION,
      0x01,  // normal mode
      0x14,  // 1 second timeout (20 * 50 ms)
      0x01,  // enable IRQ signaling
  };
  if (!sendCommandWithValidatedResponse("SAM normal mode", samCommand,
                                        sizeof(samCommand))) {
    stopWithError("PN532 SAM configuration failed");
  }

  const uint8_t rfFieldCommand[] = {
      PN532_COMMAND_RFCONFIGURATION,
      0x01,  // RF field configuration
      0x01,  // RF field on
  };
  if (!sendCommandWithValidatedResponse("RF field on", rfFieldCommand,
                                        sizeof(rfFieldCommand))) {
    stopWithError("PN532 RF field configuration failed");
  }

  const uint8_t retryCommand[] = {
      PN532_COMMAND_RFCONFIGURATION,
      0x05,  // MaxRetries configuration
      0xFF,  // MxRtyATR
      0x01,  // MxRtyPSL
      0x02,  // bounded passive activation attempts
  };
  if (!sendCommandWithValidatedResponse("Passive retries", retryCommand,
                                        sizeof(retryCommand))) {
    stopWithError("PN532 passive retry configuration failed");
  }

  Serial0.println("NFC reader initialized (SAM normal mode, RF field on)");
  Serial0.println("Present one ISO14443A tag...");
}

void loop() {
  static uint32_t attempts = 0;
  uint8_t uid[7] = {};
  uint8_t uidLength = 0;

  if (nfc.readPassiveTargetID(PN532_MIFARE_ISO14443A, uid, &uidLength,
                              2000)) {
    Serial0.print("ISO14443A tag detected; UID=");
    printUid(uid, uidLength);
    Serial0.printf(" (%u bytes)\n", uidLength);
    delay(1500);
  } else {
    ++attempts;
    if (attempts % 5 == 0) {
      Serial0.printf("No ISO14443A tag yet (%lu attempts)\n",
                     static_cast<unsigned long>(attempts));
    }
    delay(250);
  }
}
