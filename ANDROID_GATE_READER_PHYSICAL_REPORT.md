# LockENS Android Gate Reader — physical validation

Date: 2026-09-12 (Europe/Madrid)

Primary verifier baseline: `ec82f3f578b610ff3978de10d197f1032e449809`

## Devices

- Pixel gate: `<GATE_SERIAL_1>`, Pixel 9 Pro XL, GrapheneOS Android 17/API 37.
- Seeker holder: `<GATE_SERIAL_2>`, Android 16/API 36.
- Pixel USB reverse: `tcp:8792 -> tcp:8792`.

## Transport-only checkpoint

- SELECT LockENS AID: `9000`.
- GET_CREDENTIAL: `9000`.
- Credential: `staff-001.keys.demo-access.eth`.
- No challenge or holder signature was requested during this checkpoint.

## End-to-end checkpoint

- Virtual resource: Lab.
- SELECT: `9000`.
- GET_CREDENTIAL: 30-byte bounded UTF-8 credential + `9000`.
- SEND_CHALLENGE: exact 104-byte credential/resource-bound challenge + `9000`.
- Holder status: `PROCESSING -> READY`, each response ending `9000`.
- GET_SIGNATURE: 65-byte proof + `9000`.
- Node holder result: `Verified`.
- Node registration result: `Valid`.
- Node global access result: `Allowed`.
- Node resource result: `RESOURCE_POLICY_MISSING`.
- Virtual gate decision: `ACCESS DENIED`.

The Pixel displayed `VERIFIER TRANSPORT CONFIRMED — VIRTUAL GATE DECISION`.
No ESP32 controller, physical actuator, or production-gate claim applies to this
fallback path.

## Security/accounting

- Firmware changes: none.
- Seeker HCE protocol changes: none.
- Blockchain writes: 0.
- Existing STAFF credential changes: none.
- Secrets captured: 0.
- Commit/push: none.

The Pixel bridge uses a 55-second expiry to tolerate the observed two-second
device clock offset while remaining strictly below the holder's unchanged
60-second maximum.
