# Project task packet 042: TASK — Flash ONLY the committed Batch A Gate E firmware to the ESP32-S3 and perform a

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Flash ONLY the committed Batch A Gate E firmware to the ESP32-S3 and perform a
> boot/readiness check.
>
> The user explicitly authorizes:
>
> - compile the committed Gate E firmware
> - flash it to the established ESP32-S3 on CH343 / COM4
> - one normal boot/readiness verification
>
> NO blockchain write is authorized.
>
> DO NOT:
>
> - activate/deactivate/renew any ENS credential
> - run the Gate E bridge
> - present the Seeker
> - issue a challenge
> - perform NFC signing
> - edit source
> - commit
> - push
>
> CURRENT REPOSITORY
>
> Expected:
>
> HEAD == origin/main ==
> 21041a26586bf615130708b5215c477d5c757aa5
>
> Working tree:
> clean
>
> Verify first:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> If not clean/synchronized:
> STOP.
>
> CURRENT ONCHAIN STATE — READ-ONLY CONTEXT
>
> guest-001.demo-access.eth is already expected to be:
>
> REGISTERED
> owner =
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> access.active =
> true
>
> access.validUntil =
> 1793487599
>
> ENS policy =
> ALLOW
>
> Latest known DEV nonce:
> 22 / 22
>
> Do not modify or needlessly re-read ENS unless needed only for final safety.
>
> ============================================================
> PHASE 1 — VERIFY FIRMWARE SOURCE
> ============================================================
>
> Use exactly the committed:
>
> firmware/pn532_secure_access/pn532_secure_access.ino
>
> Confirm from source that the Batch A terminal hardening is present:
>
> GATE_E: STOP - <STAGE>: <PUBLIC_REASON>
>
> and is emitted before the firmware-local human-readable:
>
> AUTHORIZATION: DENY
>
> on local terminal failure.
>
> Also confirm unchanged:
>
> - Elechouse-compatible PN532 / PN532_I2C
> - SDA GPIO17
> - SCL GPIO18
> - I2C address 0x24
> - Serial0 115200
> - APDU v1 unchanged
>
> Do not edit.
>
> ============================================================
> PHASE 2 — COMPILE
> ============================================================
>
> Compile with the established toolchain:
>
> esp32:esp32:esp32s3
> ESP32 core 3.3.11
> existing Elechouse-compatible PN532 libraries
>
> Require:
>
> COMPILE: PASS
>
> Record safe build evidence:
>
> - program size
> - RAM usage
> - firmware/source SHA if straightforward to obtain
>
> Do not install/update anything.
>
> ============================================================
> PHASE 3 — HARDWARE PREPARATION
> ============================================================
>
> Tell the user:
>
> KEEP THE SEEKER AWAY FROM THE PN532.
>
> Use the established USB-Enhanced-SERIAL CH343 interface.
>
> Expected current port:
> COM4
>
> Confirm the CH343 device is present before upload.
>
> If Windows assigned another COM number, identify it by CH343 identity rather
> than blindly forcing COM4.
>
> Do not investigate COM3 or the second USB unless the established CH343 path is
> actually unavailable.
>
> ============================================================
> PHASE 4 — FLASH
> ============================================================
>
> Upload the exact compiled Batch A Gate E firmware.
>
> Require:
>
> UPLOAD: PASS
>
> Use the established ESP32-S3 upload workflow.
>
> Do not alter board settings unless required to match the already-established
> working configuration.
>
> If manual reset is necessary, tell the user exactly when to press RST/EN once.
>
> Do NOT instruct BOOT unless the actual uploader specifically requires download
> mode after a demonstrated upload failure.
>
> ============================================================
> PHASE 5 — BOOT / READINESS CHECK
> ============================================================
>
> After successful upload:
>
> keep the Seeker AWAY.
>
> Open/capture Serial0 at 115200.
>
> Perform one clean normal boot if required.
>
> Require:
>
> I2C 0x24:
> ACK
>
> PN532 firmware query:
> PASS
>
> PN532 firmware:
> 1.6
>
> GATE_E_READY:
> YES
>
> PRESENT_SEEKER:
> YES
>
> Then STOP.
>
> Do NOT present the phone.
>
> Do NOT start Node Gate E.
>
> No challenge must be issued.
>
> ============================================================
> PHASE 6 — FINAL SAFETY
> ============================================================
>
> Require:
>
> - NFC presentations: 0
> - challenges: 0
> - signatures: 0
> - blockchain writes: 0
> - source edits: 0
> - git still clean
> - HEAD == origin/main
>
> If doing a read-only nonce check is straightforward, confirm DEV remains with
> no pending transaction.
>
> Do not change ENS state.
>
> RETURN
>
> # BATCH A FIRMWARE FLASH
>
> ## REPO STATE
>
> ## SOURCE VERIFICATION
>
> Confirm Batch A STOP telemetry present.
>
> ## BUILD
>
> Include:
> - compile PASS/FAIL
> - program size
> - RAM
>
> ## FLASH
>
> Include:
> - CH343 device
> - actual COM
> - upload PASS/FAIL
>
> ## BOOT READINESS
>
> Report exactly:
>
> I2C 0x24:
> ACK / NO ACK
>
> PN532 firmware query:
> PASS / FAIL
>
> PN532 firmware:
> 1.6 / unavailable
>
> GATE_E_READY:
> YES / NO
>
> PRESENT_SEEKER:
> YES / NO
>
> ## SECURITY / STATE
>
> Confirm:
>
> - zero NFC operations
> - zero challenges
> - zero signatures
> - zero blockchain writes
> - zero source changes
>
> ## GIT STATE
>
> ## NEXT ACTION
>
> If PASS state exactly:
>
> Perform one controlled fresh Gate E ACTIVE physical proof expecting end-to-end
> ALLOW and same-proof replay DENY.
>
> End exactly:
>
> BATCH A FIRMWARE: PASS
>
> or
>
> BATCH A FIRMWARE: STOP — <exact reason>
