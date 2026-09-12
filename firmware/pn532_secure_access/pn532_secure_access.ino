#include <Wire.h>
#include <PN532_I2C.h>
#include <PN532.h>

#include <cstring>

#ifndef LOCKENS_DYNAMIC_NFC
#define LOCKENS_DYNAMIC_NFC 0
#endif

#if LOCKENS_DYNAMIC_NFC
#include "pn532_bus_recovery.h"
#endif

namespace {
constexpr int kSdaPin = 17;
constexpr int kSclPin = 18;
constexpr uint32_t kSerialBaud = 115200;
constexpr size_t kWireBufferSize = 128;
constexpr uint32_t kStatusPollIntervalMs = 250;
constexpr uint32_t kStatusTimeoutMs = 20000;
constexpr uint32_t kChallengeTimeoutMs = 60000;
constexpr uint32_t kAuthorizationTimeoutMs = 30000;

constexpr uint8_t kStateIdle = 0x00;
constexpr uint8_t kStateProcessing = 0x01;
constexpr uint8_t kStateReady = 0x02;
constexpr uint8_t kStateError = 0x03;

constexpr uint8_t kSelectApdu[] = {
    0x00, 0xA4, 0x04, 0x00, 0x08,
    0xF0, 0x45, 0x4E, 0x53, 0x56, 0x32, 0x43, 0x31,
};
constexpr uint8_t kGetStatusApdu[] = {0x80, 0x20, 0x01, 0x00};
constexpr uint8_t kGetSignatureApdu[] = {0x80, 0x30, 0x01, 0x00};

constexpr size_t kChallengeBodyLength = 104;
constexpr size_t kSendChallengeLength = 109;
constexpr size_t kSignatureLength = 65;
constexpr size_t kSignatureResponseLength = 67;
constexpr size_t kChallengeLineLength = 220;
constexpr size_t kLargestI2cWrite = kSendChallengeLength + 10;
constexpr size_t kLargestI2cRead = kSignatureResponseLength + 11;

static_assert(kLargestI2cWrite <= kWireBufferSize,
              "SEND_CHALLENGE does not fit the Wire TX buffer");
static_assert(kLargestI2cRead <= kWireBufferSize,
              "Signature response does not fit the Wire RX buffer");

#if LOCKENS_DYNAMIC_NFC
// Observe the existing HAL calls without logging payloads or changing their arguments.
class DiagnosticPn532I2C : public PN532_I2C {
 public:
  explicit DiagnosticPn532I2C(TwoWire &wire) : PN532_I2C(wire) {}

  int8_t writeCommand(const uint8_t *header, uint8_t hlen,
                      const uint8_t *body = nullptr, uint8_t blen = 0) override {
    capture_ = hlen >= 2 && header[0] == PN532_COMMAND_INDATAEXCHANGE;
    if (capture_) {
      wireBytes_ = static_cast<unsigned>(hlen) + blen + 8;
      readAttempted_ = false;
    }
    const int8_t result = PN532_I2C::writeCommand(header, hlen, body, blen);
    if (capture_) writeResult_ = result;
    return result;
  }

  int16_t readResponse(uint8_t buffer[], uint8_t capacity,
                       uint16_t timeout = 1000) override {
    const int16_t result = PN532_I2C::readResponse(buffer, capacity, timeout);
    if (capture_) {
      readAttempted_ = true;
      readCapacity_ = capacity;
      readResult_ = result;
      // InDataExchange removes this controller status byte after this call returns.
      statusPresent_ = result > 0 && capacity > 0;
      if (statusPresent_) controllerStatus_ = buffer[0];
    }
    return result;
  }

  void printResult(uint8_t instruction) {
    if (!capture_) return;
    // Print after the complete exchange so serial output cannot delay ACK/read handling.
    Serial0.printf("PN532_DIAG_V1=INS:%02X;WIRE_BYTES:%u;WRITE_ACK_RC:%d;READ_CAP:",
                   static_cast<unsigned>(instruction), wireBytes_, static_cast<int>(writeResult_));
    if (readAttempted_) {
      Serial0.printf("%u;READ_RC:%d;PN532_STATUS:", static_cast<unsigned>(readCapacity_),
                     static_cast<int>(readResult_));
      if (statusPresent_) Serial0.printf("%02X\n", static_cast<unsigned>(controllerStatus_));
      else Serial0.println("NONE");
    } else {
      Serial0.println("NONE;READ_RC:NONE;PN532_STATUS:NONE");
    }
    capture_ = false;
  }

 private:
  bool capture_ = false;
  bool readAttempted_ = false;
  bool statusPresent_ = false;
  unsigned wireBytes_ = 0;
  int8_t writeResult_ = 0;
  uint8_t readCapacity_ = 0;
  int16_t readResult_ = 0;
  uint8_t controllerStatus_ = 0;
};
DiagnosticPn532I2C pn532Transport(Wire);
#else
PN532_I2C pn532Transport(Wire);
#endif
PN532 nfc(pn532Transport);

[[noreturn]] void halt() {
  while (true) delay(1000);
}

[[noreturn]] void stopAt(const char *stage, const char *reason) {
  Serial0.printf("GATE_E: STOP - %s: %s\n", stage, reason);
  Serial0.println("AUTHORIZATION: DENY");
  halt();
}

bool sendCommandWithValidatedResponse(const uint8_t *command,
                                      uint8_t commandLength) {
  const int8_t writeResult = pn532Transport.writeCommand(command, commandLength);
  if (writeResult != 0) {
    Serial0.printf("PN532_BOOT_V1=CMD:%02X;WRITE_RC:%d;READ_RC:NONE\n",
                   command[0], static_cast<int>(writeResult));
    return false;
  }
  uint8_t response[8] = {};
  const int16_t readResult = pn532Transport.readResponse(response, sizeof(response), 1000);
  Serial0.printf("PN532_BOOT_V1=CMD:%02X;WRITE_RC:0;READ_RC:%d\n",
                 command[0], static_cast<int>(readResult));
  // SAMConfiguration and RFConfiguration return no payload after D5/command.
  return readResult == 0;
}

bool hasSuccessStatus(const uint8_t *response, uint8_t responseLength) {
  return responseLength >= 2 && response[responseLength - 2] == 0x90 &&
         response[responseLength - 1] == 0x00;
}

bool exchangeApdu(const uint8_t *apdu, uint8_t apduLength,
                  uint8_t *response, uint8_t *responseLength) {
  const bool exchanged = nfc.inDataExchange(const_cast<uint8_t *>(apdu), apduLength,
                                           response, responseLength);
#if LOCKENS_DYNAMIC_NFC
  pn532Transport.printResult(apduLength >= 2 ? apdu[1] : 0);
#endif
  return exchanged;
}

void requireSuccessOnly(const char *stage, const uint8_t *apdu,
                        uint8_t apduLength) {
  uint8_t response[3] = {};
  uint8_t responseLength = sizeof(response);
  const bool exchanged = exchangeApdu(apdu, apduLength, response, &responseLength);
#if LOCKENS_DYNAMIC_NFC
  if (strcmp(stage, "SEND_CHALLENGE") == 0) {
    // Only transport outcome, response length and trailing status word. No payload/proof.
    Serial0.printf("APDU_RESPONSE_V1=SEND_CHALLENGE;EXCHANGE=%s;LEN=%u;SW=",
                   exchanged ? "OK" : "FAIL",
                   static_cast<unsigned>(exchanged ? responseLength : 0));
    if (exchanged && responseLength >= 2 && responseLength <= sizeof(response)) {
      Serial0.printf("%02X%02X\n",
                     static_cast<unsigned>(response[responseLength - 2]),
                     static_cast<unsigned>(response[responseLength - 1]));
    } else {
      Serial0.println("NONE");
    }
  }
#endif
  if (!exchanged) {
    stopAt(stage, "ISO-DEP exchange failed");
  }
  if (responseLength != 2 || !hasSuccessStatus(response, responseLength)) {
    stopAt(stage, "expected exact 9000 response");
  }
  Serial0.printf("%s: PASS\n", stage);
}

int8_t hexNibble(char value) {
  if (value >= '0' && value <= '9') return value - '0';
  if (value >= 'a' && value <= 'f') return value - 'a' + 10;
  if (value >= 'A' && value <= 'F') return value - 'A' + 10;
  return -1;
}

bool readSerialLine(char *line, size_t capacity, uint32_t timeoutMs) {
  size_t length = 0;
  const uint32_t startedAt = millis();
  while (millis() - startedAt < timeoutMs) {
    while (Serial0.available() > 0) {
      const char value = static_cast<char>(Serial0.read());
      if (value == '\r') continue;
      if (value == '\n') {
        line[length] = '\0';
        return true;
      }
      if (length + 1 >= capacity) return false;
      line[length++] = value;
    }
    delay(5);
  }
  return false;
}

bool decodeChallengeLine(const char *line, uint8_t *challenge) {
  constexpr char kPrefix[] = "CHALLENGE=0x";
  constexpr size_t kPrefixLength = sizeof(kPrefix) - 1;
  if (strlen(line) != kChallengeLineLength ||
      strncmp(line, kPrefix, kPrefixLength) != 0) return false;
  for (size_t index = 0; index < kChallengeBodyLength; ++index) {
    const int8_t high = hexNibble(line[kPrefixLength + index * 2]);
    const int8_t low = hexNibble(line[kPrefixLength + index * 2 + 1]);
    if (high < 0 || low < 0) return false;
    challenge[index] = static_cast<uint8_t>((high << 4) | low);
  }
  return true;
}

uint8_t pollUntilReady() {
  uint8_t previousState = 0xFF;
  const uint32_t startedAt = millis();
  while (millis() - startedAt < kStatusTimeoutMs) {
    uint8_t response[4] = {};
    uint8_t responseLength = sizeof(response);
    if (!exchangeApdu(kGetStatusApdu, sizeof(kGetStatusApdu), response,
                      &responseLength)) stopAt("STATUS", "ISO-DEP exchange failed");
    if (responseLength != 3 || !hasSuccessStatus(response, responseLength)) {
      stopAt("STATUS", "expected state byte followed by 9000");
    }
    const uint8_t state = response[0];
    if (state != previousState) {
      if (state == kStateIdle) Serial0.println("STATUS: IDLE");
      else if (state == kStateProcessing) Serial0.println("STATUS: PROCESSING");
      else if (state == kStateReady) Serial0.println("STATUS: READY");
      else if (state == kStateError) Serial0.println("STATUS: ERROR");
      else stopAt("STATUS", "unknown state byte");
      previousState = state;
    }
    if (state == kStateReady) return state;
    if (state == kStateError) stopAt("STATUS", "Android proof provider reported ERROR");
    if (state != kStateProcessing) stopAt("STATUS", "unexpected post-challenge state");
    delay(kStatusPollIntervalMs);
  }
  stopAt("STATUS", "timed out without resending SEND_CHALLENGE");
}

void printProof(const uint8_t *signature) {
  constexpr char kHex[] = "0123456789abcdef";
  Serial0.print("PROOF=0x");
  for (size_t index = 0; index < kSignatureLength; ++index) {
    Serial0.print(kHex[signature[index] >> 4]);
    Serial0.print(kHex[signature[index] & 0x0F]);
  }
  Serial0.println();
}

void runGateESession() {
  while (!nfc.inListPassiveTarget()) delay(250);
  Serial0.println("TARGET_ACTIVATION: PASS");
  requireSuccessOnly("SELECT", kSelectApdu, sizeof(kSelectApdu));
#if LOCKENS_DYNAMIC_NFC
  constexpr uint8_t getCredential[] = {0x80, 0x40, 0x01, 0x00};
  uint8_t credential[67] = {};
  uint8_t credentialLength = sizeof(credential);
  if (!exchangeApdu(getCredential, sizeof(getCredential), credential, &credentialLength) ||
      credentialLength < 3 || credentialLength > 66 || !hasSuccessStatus(credential, credentialLength)) {
    stopAt("GET_CREDENTIAL", "missing credential or unsupported dynamic APDU");
  }
  // Opaque UTF-8 bytes, hex framed to prevent newline/control injection. Node validates identity.
  Serial0.print("CREDENTIAL_V1=0x");
  for (uint8_t index = 0; index < credentialLength - 2; ++index) Serial0.printf("%02x", credential[index]);
  Serial0.println();
#endif
  Serial0.println("WAITING_CHALLENGE");

  char challengeLine[kChallengeLineLength + 1] = {};
  uint8_t challenge[kChallengeBodyLength] = {};
  if (!readSerialLine(challengeLine, sizeof(challengeLine), kChallengeTimeoutMs) ||
      !decodeChallengeLine(challengeLine, challenge)) {
    stopAt("CHALLENGE", "malformed or timed out");
  }

  uint8_t sendChallenge[kSendChallengeLength] = {
      0x80, 0x10, 0x01, 0x00, 0x68,
  };
  memcpy(sendChallenge + 5, challenge, sizeof(challenge));
  requireSuccessOnly("SEND_CHALLENGE", sendChallenge, sizeof(sendChallenge));
  pollUntilReady();

  uint8_t response[kSignatureResponseLength + 1] = {};
  uint8_t responseLength = sizeof(response);
  if (!exchangeApdu(kGetSignatureApdu, sizeof(kGetSignatureApdu), response,
                    &responseLength)) stopAt("GET_SIGNATURE", "ISO-DEP exchange failed");
  if (responseLength != kSignatureResponseLength) {
    stopAt("SIGNATURE_LENGTH", "expected exactly 67 response bytes");
  }
  if (!hasSuccessStatus(response, responseLength)) {
    stopAt("GET_SIGNATURE", "response status is not 9000");
  }
  Serial0.println("GET_SIGNATURE: PASS");
  printProof(response);

  char authorization[20] = {};
  if (!readSerialLine(authorization, sizeof(authorization), kAuthorizationTimeoutMs)) {
    stopAt("AUTHORIZATION", "malformed or timed out");
  }
  if (strcmp(authorization, "AUTHORIZATION=ALLOW") == 0) {
    Serial0.println("AUTHORIZATION: ALLOW");
    halt();
  }
  if (strcmp(authorization, "AUTHORIZATION=DENY") == 0) {
    Serial0.println("AUTHORIZATION: DENY");
    halt();
  }
  stopAt("AUTHORIZATION", "malformed response");
}
}  // namespace

void setup() {
  Serial0.begin(kSerialBaud);
  delay(2000);
#if LOCKENS_DYNAMIC_NFC
  if (!recoverPn532Bus(kSdaPin, kSclPin)) {
    stopAt("INITIALIZATION", "PN532 bus lines not released");
  }
#endif
  if (Wire.setBufferSize(kWireBufferSize) != kWireBufferSize) {
    stopAt("INITIALIZATION", "Wire buffer allocation failed");
  }
  if (!Wire.setPins(kSdaPin, kSclPin)) stopAt("INITIALIZATION", "Wire.setPins failed");
  nfc.begin();
  // As in Adafruit's I2C startup, do not probe a potentially sleeping PN532.
  // SAMConfiguration wakes it into Normal mode; require its validated reply.
  Serial0.println("PN532_BOOT_START_V1");
  const uint8_t samCommand[] = {PN532_COMMAND_SAMCONFIGURATION, 0x01, 0x14, 0x01};
  if (!sendCommandWithValidatedResponse(samCommand, sizeof(samCommand))) {
    stopAt("INITIALIZATION", "PN532 SAM configuration failed");
  }
  const uint32_t version = nfc.getFirmwareVersion();
  if (version == 0) stopAt("INITIALIZATION", "PN532 firmware/version query failed");
  const uint8_t rfFieldCommand[] = {PN532_COMMAND_RFCONFIGURATION, 0x01, 0x01};
  if (!sendCommandWithValidatedResponse(rfFieldCommand, sizeof(rfFieldCommand))) {
    stopAt("INITIALIZATION", "PN532 RF field configuration failed");
  }
  Serial0.printf("PN532_FIRMWARE=%u.%u\n",
                 static_cast<unsigned>((version >> 16) & 0xFF),
                 static_cast<unsigned>((version >> 8) & 0xFF));
  Serial0.println("GATE_E_READY");
#if LOCKENS_DYNAMIC_NFC
  Serial0.println("LOCKENS_DYNAMIC_V1_READY");
#endif
  Serial0.println("PRESENT_SEEKER");
}

void loop() {
  static bool started = false;
  if (started) halt();
  started = true;
  runGateESession();
  halt();
}
