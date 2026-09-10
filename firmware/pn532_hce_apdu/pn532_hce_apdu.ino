#include <Wire.h>
#include <PN532_I2C.h>
#include <PN532.h>

#include <cstring>

namespace {
constexpr int kSdaPin = 17;
constexpr int kSclPin = 18;
constexpr uint8_t kPn532Address = 0x24;
constexpr uint32_t kSerialBaud = 115200;
constexpr size_t kWireBufferSize = 128;
constexpr uint32_t kStatusPollIntervalMs = 250;
constexpr uint32_t kStatusTimeoutMs = 20000;

constexpr uint8_t kStateIdle = 0x00;
constexpr uint8_t kStateProcessing = 0x01;
constexpr uint8_t kStateReady = 0x02;
constexpr uint8_t kStateError = 0x03;

constexpr uint8_t kSelectApdu[] = {
    0x00, 0xA4, 0x04, 0x00, 0x08,
    0xF0, 0x45, 0x4E, 0x53, 0x56, 0x32, 0x43, 0x31,
};

// GATE D TEST VECTOR ONLY. This is the exact deterministic Gate C2 vector:
// credential[32] || resource[32] || nonce[32] || expiresAt uint64 big-endian.
constexpr uint8_t kGateDTestVector[] = {
    0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11,
    0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11,
    0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11,
    0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11,
    0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22,
    0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22,
    0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22,
    0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22,
    0x33, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33,
    0x33, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33,
    0x33, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33,
    0x33, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33,
    0x00, 0x00, 0x00, 0x00, 0x65, 0x53, 0xF1, 0x3C,
};

constexpr uint8_t kGetStatusApdu[] = {0x80, 0x20, 0x01, 0x00};
constexpr uint8_t kGetSignatureApdu[] = {0x80, 0x30, 0x01, 0x00};

constexpr size_t kChallengeBodyLength = 104;
constexpr size_t kSendChallengeLength = 109;
constexpr size_t kSignatureLength = 65;
constexpr size_t kSignatureResponseLength = 67;
// InDataExchange adds two command bytes to TX. PN532 framing adds eight more.
constexpr size_t kLargestI2cWrite = kSendChallengeLength + 10;
// The 67-byte APDU response gains status, TFI/command, framing, and RDY bytes.
constexpr size_t kLargestI2cRead = kSignatureResponseLength + 11;

static_assert(sizeof(kGateDTestVector) == kChallengeBodyLength,
              "Gate D test vector must remain exactly 104 bytes");
static_assert(kLargestI2cWrite <= kWireBufferSize,
              "Frozen SEND_CHALLENGE does not fit the Wire TX buffer");
static_assert(kLargestI2cRead <= kWireBufferSize,
              "Frozen signature response does not fit the Wire RX buffer");

PN532_I2C pn532Transport(Wire);
PN532 nfc(pn532Transport);

[[noreturn]] void halt() {
  while (true) {
    delay(1000);
  }
}

[[noreturn]] void stopAt(const char *stage, const char *reason) {
  Serial0.printf("GATE_D: STOP - %s: %s\n", stage, reason);
  halt();
}

bool sendCommandWithValidatedResponse(const uint8_t *command,
                                      uint8_t commandLength) {
  if (pn532Transport.writeCommand(command, commandLength) != 0) {
    return false;
  }

  uint8_t response[8] = {};
  return pn532Transport.readResponse(response, sizeof(response), 1000) >= 0;
}

bool hasSuccessStatus(const uint8_t *response, uint8_t responseLength) {
  return responseLength >= 2 && response[responseLength - 2] == 0x90 &&
         response[responseLength - 1] == 0x00;
}

bool exchangeApdu(const uint8_t *apdu, uint8_t apduLength,
                  uint8_t *response, uint8_t *responseLength) {
  return nfc.inDataExchange(const_cast<uint8_t *>(apdu), apduLength,
                            response, responseLength);
}

void requireSuccessOnly(const char *stage, const uint8_t *apdu,
                        uint8_t apduLength) {
  // One extra byte is required because PN532::inDataExchange initially stores
  // its own status byte before removing it from the returned APDU response.
  uint8_t response[3] = {};
  uint8_t responseLength = sizeof(response);
  if (!exchangeApdu(apdu, apduLength, response, &responseLength)) {
    stopAt(stage, "ISO-DEP exchange failed");
  }
  if (responseLength != 2 || !hasSuccessStatus(response, responseLength)) {
    stopAt(stage, "expected exact 9000 response");
  }
  Serial0.printf("%s: PASS\n", stage);
}

uint8_t pollUntilReady() {
  uint8_t previousState = 0xFF;
  const uint32_t startedAt = millis();

  while (millis() - startedAt < kStatusTimeoutMs) {
    uint8_t response[4] = {};
    uint8_t responseLength = sizeof(response);
    if (!exchangeApdu(kGetStatusApdu, sizeof(kGetStatusApdu), response,
                      &responseLength)) {
      stopAt("STATUS", "ISO-DEP exchange failed");
    }
    if (responseLength != 3 || !hasSuccessStatus(response, responseLength)) {
      stopAt("STATUS", "expected state byte followed by 9000");
    }

    const uint8_t state = response[0];
    if (state != previousState) {
      switch (state) {
        case kStateIdle:
          Serial0.println("STATUS: IDLE");
          break;
        case kStateProcessing:
          Serial0.println("STATUS: PROCESSING");
          break;
        case kStateReady:
          Serial0.println("STATUS: READY");
          break;
        case kStateError:
          Serial0.println("STATUS: ERROR");
          break;
        default:
          stopAt("STATUS", "unknown state byte");
      }
      previousState = state;
    }

    if (state == kStateReady) {
      return state;
    }
    if (state == kStateError) {
      stopAt("STATUS", "Android proof provider reported ERROR");
    }
    if (state != kStateProcessing) {
      stopAt("STATUS", "unexpected state after SEND_CHALLENGE");
    }
    delay(kStatusPollIntervalMs);
  }

  stopAt("STATUS", "timed out without resending SEND_CHALLENGE");
}

void printSignature(const uint8_t *signature) {
  constexpr char kHex[] = "0123456789abcdef";
  Serial0.print("GATE_D_SIGNATURE=0x");
  for (size_t i = 0; i < kSignatureLength; ++i) {
    Serial0.print(kHex[signature[i] >> 4]);
    Serial0.print(kHex[signature[i] & 0x0F]);
  }
  Serial0.println();
}

void runGateDSession() {
  while (!nfc.inListPassiveTarget()) {
    delay(250);
  }
  Serial0.println("TARGET_ACTIVATION: PASS");

  requireSuccessOnly("SELECT", kSelectApdu, sizeof(kSelectApdu));

  uint8_t sendChallenge[kSendChallengeLength] = {
      0x80, 0x10, 0x01, 0x00, 0x68,
  };
  memcpy(sendChallenge + 5, kGateDTestVector, sizeof(kGateDTestVector));
  requireSuccessOnly("SEND_CHALLENGE", sendChallenge,
                     sizeof(sendChallenge));
  // SEND_CHALLENGE is deliberately never retried in this selected session.

  pollUntilReady();

  // Allocate one extra byte for PN532::inDataExchange's leading status byte.
  uint8_t response[kSignatureResponseLength + 1] = {};
  uint8_t responseLength = sizeof(response);
  if (!exchangeApdu(kGetSignatureApdu, sizeof(kGetSignatureApdu), response,
                    &responseLength)) {
    stopAt("GET_SIGNATURE", "ISO-DEP exchange failed");
  }
  if (responseLength != kSignatureResponseLength) {
    stopAt("SIGNATURE_LENGTH", "expected exactly 67 response bytes");
  }
  if (!hasSuccessStatus(response, responseLength)) {
    stopAt("GET_SIGNATURE", "response status is not 9000");
  }

  Serial0.println("GET_SIGNATURE: PASS");
  Serial0.println("SIGNATURE_LEN=65");
  printSignature(response);
  Serial0.println("GATE_D: PASS");
}
}  // namespace

void setup() {
  Serial0.begin(kSerialBaud);
  delay(2000);

  if (Wire.setBufferSize(kWireBufferSize) != kWireBufferSize) {
    stopAt("INITIALIZATION", "Wire buffer allocation failed");
  }
  if (!Wire.setPins(kSdaPin, kSclPin)) {
    stopAt("INITIALIZATION", "Wire.setPins failed");
  }
  nfc.begin();

  Wire.beginTransmission(kPn532Address);
  if (Wire.endTransmission() != 0) {
    stopAt("INITIALIZATION", "PN532 did not ACK at I2C address 0x24");
  }

  const uint32_t version = nfc.getFirmwareVersion();
  if (version == 0) {
    stopAt("INITIALIZATION", "PN532 firmware/version query failed");
  }

  // This installed PN532::SAMConfig() treats the valid zero-length response as
  // failure, so retain the validated direct command used by the UID sketch.
  const uint8_t samCommand[] = {
      PN532_COMMAND_SAMCONFIGURATION, 0x01, 0x14, 0x01,
  };
  if (!sendCommandWithValidatedResponse(samCommand, sizeof(samCommand))) {
    stopAt("INITIALIZATION", "PN532 SAM configuration failed");
  }

  const uint8_t rfFieldCommand[] = {
      PN532_COMMAND_RFCONFIGURATION, 0x01, 0x01,
  };
  if (!sendCommandWithValidatedResponse(rfFieldCommand,
                                        sizeof(rfFieldCommand))) {
    stopAt("INITIALIZATION", "PN532 RF field configuration failed");
  }

  Serial0.printf("PN532_FIRMWARE=%u.%u\n",
                 static_cast<unsigned>((version >> 16) & 0xFF),
                 static_cast<unsigned>((version >> 8) & 0xFF));
  Serial0.println("GATE_D_READY");
  Serial0.println("PRESENT_SEEKER");
}

void loop() {
  static bool started = false;
  if (started) {
    halt();
  }
  started = true;
  runGateDSession();
  halt();
}
