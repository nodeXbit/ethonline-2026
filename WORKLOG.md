# Worklog

## 2026-09-05

Preflight development environment completed.
No product code has been written yet.

## 2026-09-07

- Validated ESP32-S3 serial output and programming with a minimal serial test sketch.
- Validated PN532 communication over I2C and detected a physical ISO14443A tag, including successful UID reading.
- Confirmed wiring and address: GPIO17 is SDA, GPIO18 is SCL, and the PN532 responds at I2C address `0x24`.
- Identified the initial failure as an HSU/I2C interface mismatch: the PN532 hardware/interface selection and sketch transport must both use I2C.
- Confirmed that custom ESP32-S3 I2C pins require `Wire.setPins(17, 18)` before `nfc.begin()`, because the Elechouse I2C transport calls `Wire.begin()` without pin arguments.
- Identified an Elechouse library behavior in which `PN532::SAMConfig()` treats the valid zero-payload SAMConfiguration response as failure. Kept the library unchanged and added a sketch-level command/response workaround that accepts a validated zero-length payload.
- Hardware feasibility result: PASS for PC -> ESP32-S3 -> PN532 over I2C -> physical ISO14443A tag.
- Decided that static NFC UID can serve only as a clonable prototype/demo identifier. Cryptographic NFC challenge-response and Android HCE remain outside the MVP critical path and may be revisited as security hardening if time permits.
