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

## 2026-09-09

- Completed the physical NFC → ENSv2 authorization demo with physical UID `91:2D:E3:06`: ISO14443A tag → ESP32-S3 + PN532 over I2C → 115200-baud serial UID → Node PC bridge → Sepolia ENSv2 read.
- The bridge resolved `demo-access.eth` to UserRegistry `0x2d249472B83A453086254Acd8a42913D8e45a2Fd` and mapped the UID to `cred-001.demo-access.eth`.
- Revoked read at block `11663898`: status `AVAILABLE` (0), zero owner, and result `AUTHORIZATION: DENY`.
- Issued with transaction `0x965260d7a766e0d0bbaa0abe110487723a229cd06d81b88acf770b42a5a98d35` in block `11663902`; result `CREDENTIAL STATE: ACTIVE`. The same physical tag then read at block `11663904` as `REGISTERED` (2), owner `0x4C60a5AD311510543B56d0408872A52e4AEEe19C`, and `AUTHORIZATION: ALLOW`.
- Revoked with transaction `0xd538c72e6ec5a59e4c7722db0c4ce63a98731f832a76d351fa68c5c7b9803c69` in block `11663907`; result `CREDENTIAL STATE: REVOKED`. The same physical tag then read at block `11663909` as `AVAILABLE` (0), zero owner, and `AUTHORIZATION: DENY`.
- The logical ENS label remained `cred-001.demo-access.eth`, while the current tokenId changed after unregister (`...167041` → `...167042`); tokenId must therefore be rediscovered and is not a permanent application identifier.
- The static NFC UID is only a clonable prototype/demo identifier, not proof of possession or production security.
- Conclusion: ENSv2 is verified as load-bearing authorization state in the physical demo. The next product question is a persistent credential lifecycle that can survive after physical access expires rather than treating unregister/burn as the final model.

### Persistent physical credential milestone

- Chose a two-layer ENSv2 model: UserRegistry provides persistent ownership of `cred-001.demo-access.eth`, while PermissionedResolver `access.v1` independently controls whether the physical credential authorizes entry.
- Reused the verified PermissionedResolver at `0x0B723c0C2170F2ea508F2f4e3e122C6c0782744C` with UserRegistry `0x2d249472B83A453086254Acd8a42913D8e45a2Fd` under `demo-access.eth`.
- Initialized inactive access with `0xfdee98ad2bb5d646ae1a5ce940a858970759b2cb6bab0615cae57b89c00a4f69`, then registered the persistent credential with `0x60f6125afe15d6383728cbc4cf019926afe8e62927d41fb91e1fef54543d029a`.
- Activated access with `0xe2385b730a8ee48c7d9fbc7ecf862d5022ea8883997a56375f1d05738a066335` in block `11664354`, and deactivated it with `0x0e3ab168faa2fffe67bb0d0cda2222726fb58f8890bd90c1f82108bd0fce712b` in block `11664363`.
- Presented the same physical NFC tag (`91:2D:E3:06`) for the complete lifecycle and observed `DENY → ALLOW → DENY`, driven by `access.active: false → true → false`.
- Preserved REGISTERED status, owner `0x4C60a5AD311510543B56d0408872A52e4AEEe19C`, tokenId `111633085976721986886445685281703791217854403259346662013299173065803998167042`, resolver, and registry expiry `1820447664` throughout.
- Confirmed that rerunning persistent setup while ACTIVE was idempotent: it sent no transaction, did not re-register the credential, and did not reset access.
- Recovery/debugging lesson: reconstruct public nonces, receipts, resolver provenance, roles, and pinned-block state before retrying. Safe errors must retain stage, operation, custom error/RPC code, and any public transaction hash without exposing RPC credentials.
- Conclusion: ENSv2 is load-bearing for physical authorization, while the credential survives access revocation. Ownership lifetime and access lifetime are now separate.
