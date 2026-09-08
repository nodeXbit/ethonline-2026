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

## 2026-09-08

- Completed the ENSv2 Sepolia vertical slice for `demo-access.eth` with UserRegistry `0x2d249472B83A453086254Acd8a42913D8e45a2Fd`.
- Attached the parent with `setSubregistry` transaction `0x5c9c54c1873260be56e91a0c8f9adfd37479a92e9c5cf05a864fc2aa0e7f2ab8` (block `11663664`), after proxy deployment transaction `0xb820d86648c1201f59c5f9966adfbee22cb0fee5772a5da3ff39166d88d0657a` (block `11663662`).
- Registered `cred-001.demo-access.eth` with transaction `0xde2447b4146cb1687428e43abf51dac3f748be6dc52f744474a48e1bbbe60dd9` (block `11663688`), read `REGISTERED` with the DEV wallet as owner, and produced `AUTHORIZATION: ALLOW`.
- Unregistered it with transaction `0x65c2d6a6b8ad86d365541d57a26b83d3222ffce2eadb42f84bcdd0f91ac35d91` (block `11663690`), read `AVAILABLE` with zero owner, and produced `AUTHORIZATION: DENY`.
- Key learning: ENSv2 is verified as load-bearing authorization state. The credential tokenId/resource changed after unregister, so the logical ENS name/label is the stable application reference.
