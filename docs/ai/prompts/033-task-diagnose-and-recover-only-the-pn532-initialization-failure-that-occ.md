# Project task packet 033: TASK — Diagnose and recover ONLY the PN532 initialization failure that occurred before

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Diagnose and recover ONLY the PN532 initialization failure that occurred before
> GATE_E_READY.
>
> Do NOT run a Gate E physical signing attempt.
> Do NOT start the Gate E bridge.
> Do NOT present the Seeker.
> Do NOT issue a challenge.
> Do NOT perform blockchain writes.
> Do NOT activate/deactivate guest-001.
> Do NOT edit repository source initially.
> Do NOT commit or push.
>
> CURRENT FAILURE
>
> Latest controlled retry stopped during firmware initialization:
>
> PN532 firmware/version query failed
>
> Therefore:
>
> GATE_E_READY: NOT EMITTED
> PRESENT_SEEKER: NOT EMITTED
> bridge: NOT STARTED
> challenge count: 0
> physical signing attempts: 0
> blockchain writes: 0
>
> The observed AUTHORIZATION: DENY was only the firmware's local initialization
> failure path.
>
> CURRENT EXPECTED REPOSITORY
>
> HEAD == origin/main ==
> b6159981f1e01195df9183ecc9a535a072edf2c7
>
> Expected unstaged modifications only:
>
> scripts/security/gate-e-secure-bridge.mjs
> scripts/security/gate-e-secure-bridge.test.mjs
>
> Preserve them exactly.
>
> PHASE 1 — READ-ONLY REPO CHECK
>
> Run:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
> git diff --check
>
> Require only the two expected modifications.
>
> Then read locally, without editing:
>
> firmware/pn532_secure_access/pn532_secure_access.ino
> firmware/pn532_hce_apdu/pn532_hce_apdu.ino
> firmware/pn532_i2c_tag/pn532_i2c_tag.ino
>
> Compare only the PN532/Wire initialization path.
>
> Confirm the Gate E sketch still uses the established:
>
> SDA GPIO17
> SCL GPIO18
> PN532 I2C address 0x24
> Elechouse-compatible PN532 + PN532_I2C
> Serial0 115200
>
> Identify the exact call that produced:
>
> PN532 firmware/version query failed
>
> Do not change anything.
>
> PHASE 2 — PHYSICAL CONNECTION CHECK
>
> The Seeker must remain AWAY from the PN532.
>
> Before another ESP32 reset, tell the user to perform this physical check.
>
> Give one instruction at a time.
>
> Require the user to:
>
> 1. Disconnect USB/power from the ESP32.
>
> 2. Verify the PN532 wiring is physically secure and still matches the
>    previously successful configuration:
>
>    SDA → ESP32 GPIO17
>    SCL → ESP32 GPIO18
>    GND → ESP32 GND
>
>    For PN532 power:
>    verify it remains connected to the SAME power pin used during the successful
>    Gate D/Gate E runs.
>
>    Do NOT tell the user to change voltage rails unless the actual existing
>    wiring/source establishes that this is required.
>
> 3. Reseat any loose Dupont connection.
>
> 4. Verify any PN532 mode switch/jumper has not been moved from its previously
>    working I2C configuration.
>
> 5. Visually check for:
>    - partially inserted wire
>    - disconnected ground
>    - SDA/SCL swapped
>    - board shifted so a connector is barely contacting.
>
> Do not ask the user to present the phone.
>
> After the physical inspection/reseat is complete, reconnect USB power.
>
> PHASE 3 — BOOT-ONLY INITIALIZATION CHECK
>
> Do NOT start the Node Gate E bridge.
>
> Open serial monitoring/capture for the current ESP32 port.
>
> Perform one clean boot/reset only after Phase 2 is complete.
>
> Do not present any NFC target.
>
> Require:
>
> PN532 firmware/version query succeeds
>
> Expected known firmware version:
> 1.6
>
> Then require:
>
> GATE_E_READY
> PRESENT_SEEKER
>
> If all three are observed:
>
> STOP.
>
> Do NOT present the Seeker.
>
> Classify:
>
> PN532 initialization recovered.
>
> No further diagnostic firmware is needed.
>
> PHASE 4 — ONLY IF INITIALIZATION STILL FAILS
>
> If getFirmwareVersion still fails:
>
> Do NOT repeatedly reset.
>
> Perform a bounded I2C-level diagnostic.
>
> Prefer a temporary diagnostic outside the repository or another method that
> does not modify tracked project files.
>
> Probe the configured I2C bus:
>
> SDA GPIO17
> SCL GPIO18
>
> Determine whether address 0x24 ACKs.
>
> Do not scan/change unrelated hardware unnecessarily.
>
> CLASSIFY:
>
> CASE A
>
> 0x24 does NOT ACK
>
> Boundary:
>
> ESP32 ↔ PN532 I2C connectivity.
>
> Likely categories may include power, ground, SDA/SCL connection, PN532 I2C mode,
> or physical module connection.
>
> Do not claim which one without evidence.
>
> STOP.
>
> CASE B
>
> 0x24 DOES ACK but getFirmwareVersion still fails
>
> Boundary:
>
> PN532 protocol/HAL/init above basic I2C address acknowledgement.
>
> Then inspect read-only:
>
> - exact local installed PN532 / PN532_I2C source
> - Wire initialization order
> - begin()/getFirmwareVersion path
> - differences between the currently successful historical Gate D init and Gate
>   E init
>
> Do NOT patch the library.
> Do NOT change APDU v1.
> Do NOT add retries yet.
>
> Return evidence to Control Tower.
>
> PHASE 5 — RESTORE
>
> If any temporary diagnostic sketch/file outside the repository was used:
>
> - do not add it to Git
> - leave repository files untouched
> - report exactly what was flashed
> - if necessary, restore the unchanged Gate E firmware afterward, but do not run
>   an NFC session.
>
> Final git state must remain:
>
> HEAD == origin/main ==
> b6159981f1e01195df9183ecc9a535a072edf2c7
>
> with only the existing two serial-finalization modifications.
>
> RETURN
>
> # PN532 INITIALIZATION RECOVERY
>
> ## REPO STATE
> ## INITIALIZATION SOURCE CHECK
> ## PHYSICAL CHECK
> ## BOOT-ONLY RESULT
>
> Report:
>
> PN532 firmware query:
> PASS / FAIL
>
> Firmware version:
> <value if obtained>
>
> GATE_E_READY:
> YES / NO
>
> PRESENT_SEEKER:
> YES / NO
>
> ## I2C PROBE
>
> Only if required.
>
> Report:
>
> address 0x24:
> ACK / NO ACK / NOT RUN
>
> ## FAILURE BOUNDARY
> ## FILES / FIRMWARE CHANGED
> ## SECURITY
> ## GIT STATE
> ## NEXT ACTION
>
> If normal boot is recovered, NEXT ACTION must be:
>
> One controlled Gate E INACTIVE physical retry, with the Seeker kept outside
> the NFC field until explicitly requested.
>
> End exactly:
>
> PN532 INIT RECOVERY: PASS
>
> or
>
> PN532 INIT RECOVERY: STOP — <exact reason>
