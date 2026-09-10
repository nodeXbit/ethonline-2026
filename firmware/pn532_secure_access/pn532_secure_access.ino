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

PN532_I2C pn532Transport(Wire);
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
  if (pn532Transport.writeCommand(command, commandLength) != 0) return false;
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
  if (Wire.setBufferSize(kWireBufferSize) != kWireBufferSize) {
    stopAt("INITIALIZATION", "Wire buffer allocation failed");
  }
  if (!Wire.setPins(kSdaPin, kSclPin)) stopAt("INITIALIZATION", "Wire.setPins failed");
  nfc.begin();
  Wire.beginTransmission(kPn532Address);
  if (Wire.endTransmission() != 0) {
    stopAt("INITIALIZATION", "PN532 did not ACK at I2C address 0x24");
  }
  const uint32_t version = nfc.getFirmwareVersion();
  if (version == 0) stopAt("INITIALIZATION", "PN532 firmware/version query failed");
  const uint8_t samCommand[] = {PN532_COMMAND_SAMCONFIGURATION, 0x01, 0x14, 0x01};
  if (!sendCommandWithValidatedResponse(samCommand, sizeof(samCommand))) {
    stopAt("INITIALIZATION", "PN532 SAM configuration failed");
  }
  const uint8_t rfFieldCommand[] = {PN532_COMMAND_RFCONFIGURATION, 0x01, 0x01};
  if (!sendCommandWithValidatedResponse(rfFieldCommand, sizeof(rfFieldCommand))) {
    stopAt("INITIALIZATION", "PN532 RF field configuration failed");
  }
  Serial0.printf("PN532_FIRMWARE=%u.%u\n",
                 static_cast<unsigned>((version >> 16) & 0xFF),
                 static_cast<unsigned>((version >> 8) & 0xFF));
  Serial0.println("GATE_E_READY");
  Serial0.println("PRESENT_SEEKER");
}

void loop() {
  static bool started = false;
  if (started) halt();
  started = true;
  runGateESession();
  halt();
}
