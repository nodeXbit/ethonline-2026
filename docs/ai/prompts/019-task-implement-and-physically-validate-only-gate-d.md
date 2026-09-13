# Project task packet 019: TASK — Implement and physically validate ONLY Gate D:

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Implement and physically validate ONLY Gate D:
>
> ESP32-S3 + the exact installed Elechouse-compatible PN532 / PN532_I2C stack
> → real ISO14443A / ISO-DEP communication
> → Android Seeker HostApduService
> → frozen Gate C APDU v1
> → real Privy HCE signature
> → Node/viem signer recovery.
>
> Gate D is TRANSPORT ONLY.
>
> Do NOT add ENS authorization yet.
> Do NOT perform blockchain transactions.
> Do NOT change Gate A/B/C cryptographic semantics.
>
> WHY
>
> Already proven:
>
> Gate A:
> server-issued EIP-712 challenge
> → signature verification
> → CURRENT ENS owner
> → replay protection
> → ENS policy.
>
> Gate B:
> real Privy Android EOA
> → eth_signTypedData_v4
> → Node recovered same wallet.
>
> Gate C1:
> Android HCE APDU v1.
>
> Gate C2:
> the real PrivyProofProvider asynchronously signs through the same
> application-scoped HceApduProcessor used by HostApduService.
>
> Gate D must prove only that the real PN532 can drive that exact HCE state
> machine over NFC.
>
> CURRENT CANONICAL STATE
>
> Expected pushed checkpoint:
>
> 5cb7d3561492afeb5b9477b32ab9dd34688df7d6
>
> Expected:
>
> HEAD == origin/main
> tracked working tree clean
>
> Ignored local files/build outputs may exist.
>
> Before editing run:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> Require:
>
> HEAD == origin/main == 5cb7d3561492afeb5b9477b32ab9dd34688df7d6
>
> If tracked state is dirty/ahead/behind/divergent:
> STOP.
>
> BASELINE
>
> Before editing require:
>
> Node:
>
> node --test --test-isolation=none
>
> Expected:
> 83 passed
> 0 failed
>
> Android:
>
> :app:testDebugUnitTest
> :app:assembleDebug
>
> Expected:
> 21/21 JVM tests PASS
> build PASS
>
> Do not change Android code during Gate D unless a concrete physical
> interoperability defect proves the already-frozen implementation incorrect.
>
> If that happens:
> STOP and report rather than silently changing APDU v1.
>
> CURRENT HARDWARE
>
> Validated existing hardware:
>
> - ESP32-S3
> - PN532 over I2C
> - SDA GPIO17
> - SCL GPIO18
> - PN532 I2C address 0x24
> - Serial0 115200
> - existing hardware spike successfully reads ISO14443A tags
> - Windows device previously identified on COM4
>
> Do not change wiring.
>
> IMPORTANT LIBRARY REQUIREMENT
>
> The validated firmware uses the installed Elechouse-compatible:
>
> - PN532
> - PN532_I2C
>
> Do NOT use Adafruit_PN532.
>
> Before implementation inspect locally:
>
> - current successful PN532 I2C sketch
> - exact installed PN532.h / PN532.cpp
> - exact PN532_I2C.h / PN532_I2C.cpp
> - installed AndroidHCE or relevant Elechouse example if present
> - actual inListPassiveTarget / inDataExchange signatures and behavior
>
> Use the exact locally installed source.
>
> Do not install, upgrade or replace the PN532 library.
>
> Reconfirm from source:
>
> - APDU TX capacity
> - APDU RX capacity
> - actual response-length handling
> - Wire/I2C constraints relevant to the exact installed ESP32 core
>
> Do not rely on Adafruit buffer assumptions.
>
> If the frozen APDU payloads do NOT fit the actual current transport:
> STOP.
>
> Do not add chunking during Gate D.
>
> APDU V1 IS FROZEN
>
> AID:
>
> F0454E5356324331
>
> SELECT:
>
> ISO7816 SELECT AID
> CLA 00
> INS A4
> P1 04
> P2 00
> AID exactly as above
>
> Application protocol:
>
> SEND_CHALLENGE:
> CLA 80
> INS 10
> P1 01
> P2 00
> Lc = 0x68
> Data = exactly 104 bytes
>
> GET_STATUS:
> CLA 80
> INS 20
> P1 01
> P2 00
> No data
>
> GET_SIGNATURE:
> CLA 80
> INS 30
> P1 01
> P2 00
> No data
>
> State response byte:
>
> 00 IDLE
> 01 PROCESSING
> 02 READY
> 03 ERROR
>
> Success status word:
> 9000
>
> Expected signature response:
> 65 signature bytes + 9000
>
> Do NOT:
> - change command bytes
> - change AID
> - add JSON
> - add wallet address
> - add signature chunking
> - change proof length.
>
> TRANSPORT TEST VECTOR
>
> Reuse EXACTLY the deterministic Gate C2 manual challenge vector already present
> in the repository.
>
> Do not invent a second vector.
>
> It remains TEST/TRANSPORT evidence only.
>
> It is not a Gate A server-issued authorization attempt.
>
> The firmware must send the exact 104-byte binary encoding of that vector:
>
> credential[32]
> || resource[32]
> || nonce[32]
> || expiresAt[8 unsigned big-endian]
>
> If constants must exist in firmware, derive/copy them exactly from the existing
> Gate C2 canonical vector after inspection and label them clearly:
>
> GATE D TEST VECTOR ONLY
>
> Do not change Gate C2's vector.
>
> FIRMWARE SCOPE
>
> Preserve the existing working UID firmware.
>
> Create a NEW scoped sketch, preferably:
>
> firmware/pn532_hce_apdu/pn532_hce_apdu.ino
>
> Do not turn the old UID sketch into the secure firmware.
>
> Gate D firmware should be deliberately one-shot and diagnostic.
>
> On boot:
>
> 1. initialize Serial0 115200
> 2. initialize Wire on GPIO17/GPIO18
> 3. initialize PN532
> 4. verify PN532 firmware/version as current library supports
> 5. configure PN532/SAM as required
> 6. print:
>
> GATE_D_READY
> PRESENT_SEEKER
>
> 7. wait for one Android ISO14443A/ISO-DEP target
> 8. perform one full Gate D session
> 9. print final evidence
> 10. do NOT automatically start another signing session
>
> Require device reset/reboot for a second physical run.
>
> This prevents accidentally triggering repeated Privy signatures while the
> phone remains on the reader.
>
> APDU EXCHANGE
>
> Implement small explicit helpers around the actual Elechouse APIs.
>
> A. TARGET ACTIVATION
>
> Use the library's initiator/passive-target path appropriate for Android HCE.
>
> Require a target before APDU exchange.
>
> Do not use NFC UID as authorization or identity.
>
> The UID, if the PN532/library exposes one during activation, must play NO role
> in the result and does not need to be printed.
>
> B. SELECT
>
> Send the frozen SELECT AID APDU.
>
> Require response status:
> 9000
>
> If not:
> STOP this physical session.
>
> Print:
>
> SELECT: PASS
>
> C. SEND_CHALLENGE
>
> Send the exact APDU:
>
> 80 10 01 00 68 <104 bytes>
>
> Require:
> 9000
>
> Print:
>
> SEND_CHALLENGE: PASS
>
> After SEND_CHALLENGE succeeds:
>
> NEVER automatically resend SEND_CHALLENGE during that selected session.
>
> D. POLL GET_STATUS
>
> Poll only GET_STATUS.
>
> Use a conservative bounded cadence appropriate for Privy network signing,
> for example approximately 200–300 ms between polls.
>
> Use a bounded overall timeout around 15–20 seconds.
>
> Do not busy-loop.
>
> Expected responses:
>
> 01 90 00
> → PROCESSING
>
> 02 90 00
> → READY
>
> 03 90 00
> → ERROR
>
> Print state changes only, not every identical poll, for readable serial output:
>
> STATUS: PROCESSING
> STATUS: READY
>
> If ERROR:
> fail the session.
>
> If timeout:
> fail the session without resending the challenge.
>
> E. GET_SIGNATURE
>
> Only after READY.
>
> Send GET_SIGNATURE once.
>
> Require exactly:
>
> 67 response bytes total
>
> where:
>
> first 65 bytes = signature
> last 2 bytes = 90 00
>
> Validate locally before printing:
>
> - exact length
> - final SW=9000
>
> Print:
>
> SIGNATURE_LEN=65
>
> and one strict ASCII lowercase/uppercase hex line:
>
> GATE_D_SIGNATURE=0x<130 ASCII hex characters>
>
> No spaces.
> No Unicode.
> No labels inside the value.
>
> Then:
>
> GATE_D: PASS
>
> Do not print:
> - Privy config
> - auth tokens
> - OTP
> - private material.
>
> LINK INTERRUPTION
>
> If NFC/ISO-DEP communication fails after SEND_CHALLENGE:
>
> - do not blindly resend APDUs inside an invalid session;
> - mark the session failed;
> - require a fresh firmware reset + fresh phone presentation for retry.
>
> The Android HCE deactivation/reset semantics already invalidate the old
> session.
>
> Do not add complex reconnect logic during Gate D.
>
> PHONE PRECONDITIONS
>
> For physical validation, instruct the user to prepare the Seeker:
>
> - latest Gate C2 APK already installed
> - NFC enabled
> - Internet available
> - Privy user authenticated
> - existing Ethereum wallet available
> - app opened and wallet readiness confirmed
> - screen unlocked for the first transport validation
>
> Do not require Seed Vault.
> Do not use any Solana functionality.
>
> The user should hold the Seeker steadily against the PN532 antenna from
> PRESENT_SEEKER until GATE_D PASS/STOP.
>
> FIRMWARE BUILD / UPLOAD
>
> Reuse the already-established local Arduino/ESP32 workflow.
>
> Inspect what tooling is actually available before choosing commands.
>
> Do NOT install:
> - PlatformIO
> - another Arduino core
> - another PN532 library
> - unrelated CLI tooling
>
> If an existing CLI/build path can compile the new sketch safely:
> use it.
>
> If firmware upload cannot be performed from the available local command-line
> tooling:
>
> 1. finish source review/build validation as far as possible;
> 2. give the user exact minimal Arduino IDE steps using the existing board/core
>    and library configuration;
> 3. stop at the manual flash checkpoint;
> 4. continue after the user confirms upload.
>
> Do not make the user choose libraries or board settings that can be discovered
> from the previous validated firmware/environment.
>
> COM PORT
>
> Previously observed:
> COM4
>
> Do not assume blindly.
>
> Before serial validation, inspect the current available serial device
> read-only and confirm the ESP32 port.
>
> Do not expose unrelated devices.
>
> SERIAL CAPTURE
>
> After firmware is flashed and any Arduino Serial Monitor is closed, prefer
> Codex to capture the serial output directly rather than making the user relay
> the signature manually.
>
> The repository already has the serialport dependency.
>
> A temporary one-off local command may read the ESP32 serial stream without
> creating a committed file.
>
> Do not modify the existing NFC bridge merely to capture Gate D evidence.
>
> Codex should tell the user exactly:
>
> PRESENT THE SEEKER TO THE PN532 NOW
>
> when serial output reaches:
>
> PRESENT_SEEKER
>
> The user is responsible only for the physical presentation.
>
> Capture the strict:
>
> GATE_D_SIGNATURE=0x...
>
> line directly from serial.
>
> Do not route the signature through ChatGPT.
>
> NODE CRYPTOGRAPHIC CHECK
>
> After physical transport succeeds, use the already-existing Gate C2 verifier:
>
> node scripts/security/gate-c2-verify.mjs <wallet> <signature>
>
> Expected public Privy wallet:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> The signature must come from the real PN532/HCE transport capture.
>
> Require:
>
> PRIVY WALLET:
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> RECOVERED SIGNER:
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> MATCH:
> PASS
>
> This remains cryptographic interoperability/transport validation only.
>
> Do not consult ENS.
>
> REPEATABILITY
>
> After one complete PASS:
>
> perform one second clean physical run if it can be done without changing code:
>
> 1. remove phone
> 2. reset/reboot ESP32 test sketch
> 3. wait for PRESENT_SEEKER
> 4. present phone again
> 5. repeat complete exchange
> 6. verify the second transported signature with Node
>
> Target:
>
> 2/2 complete physical sessions PASS
>
> If first run passes but second fails due to a clear transient positioning/NFC
> interruption, one bounded retry is acceptable.
>
> Do not keep repeating indefinitely.
>
> TESTS / VALIDATION
>
> Before physical testing:
>
> Node:
> node --test --test-isolation=none
> Require:
> 83/83 PASS
>
> Android:
> existing Gate C1/C2 tests remain:
> 21/21 PASS
>
> Android build:
> PASS
>
> Firmware:
> - source/build validation PASS using current installed stack
>
> After implementation:
>
> - git diff --check
> - scoped status
> - safe secret scan
> - firmware change limited to the new Gate D sketch unless a tiny shared
>   compile-only helper is concretely necessary
> - no Android protocol change
> - no Gate A/B/C changes
> - no blockchain write.
>
> NO-TOUCH
>
> Do not modify:
>
> mobile/android Gate C APDU semantics
> scripts/security holder-proof semantics
> scripts/ensv2/
> scripts/nfc/
> demo/
> existing UID firmware behavior
> project documentation during implementation
>
> Do not implement:
>
> - ENS owner check
> - access.v1 check
> - ALLOW/DENY
> - server-issued live nonce store
> - blockchain transaction
> - credential issuance
> - payment/USDC
> - World
> - ERC-4337
> - Aliro
> - relay/door actuator
> - signature chunking
> - Wi-Fi/RPC on ESP32
>
> The ESP32 remains NFC/APDU transport for Gate D.
>
> STOP IF
>
> Stop and report before redesigning if:
>
> - repo baseline differs;
> - exact Elechouse library cannot support the frozen APDU request/response sizes;
> - SEND_CHALLENGE 109-byte APDU cannot be transmitted intact;
> - 67-byte GET_SIGNATURE response cannot be received intact;
> - Android does not route SELECT to the registered HCE service;
> - reliable physical exchange requires changing APDU v1;
> - the only proposed fix is changing PN532 libraries;
> - Privy/HCE path that passed Gate C2 stops working independently;
> - firmware upload requires installing a new toolchain;
> - scope starts moving into ENS authorization.
>
> DEBUGGING RULE
>
> If the physical test fails:
>
> identify exact stage first:
>
> TARGET_ACTIVATION
> SELECT
> SEND_CHALLENGE
> STATUS
> GET_SIGNATURE
> SIGNATURE_LENGTH
> NODE_RECOVERY
>
> Do not make random firmware/protocol changes.
>
> Use:
> A) small fix
> B) bounded workaround
> C) STOP
>
> before considering architectural change.
>
> DO NOT COMMIT.
> DO NOT PUSH.
>
> EVIDENCE REQUIRED
>
> Return:
>
> # GATE D PHYSICAL PN532 HCE TRANSPORT
>
> ## BASELINE
>
> - HEAD
> - origin/main
> - status
> - Node tests
> - Android tests/build
>
> ## ELECHOUSE LIBRARY
>
> State:
> - exact local implementation inspected
> - relevant inDataExchange API
> - confirmed APDU TX/RX limits
> - whether frozen 109-byte request and 67-byte response fit
>
> Explicitly:
>
> CHUNKING:
> NOT REQUIRED / REQUIRED
>
> If REQUIRED:
> Gate D must STOP because APDU v1 is frozen.
>
> ## FILES CHANGED
>
> ## FIRMWARE
>
> Explain:
> - initialization
> - target activation
> - one-shot behavior
> - serial evidence format
>
> ## PHYSICAL APDU EXCHANGE
>
> Report every stage:
>
> TARGET ACTIVATION:
> PASS/FAIL
>
> SELECT:
> PASS/FAIL
>
> SEND_CHALLENGE:
> PASS/FAIL
>
> STATUS PROCESSING:
> PASS/FAIL
>
> STATUS READY:
> PASS/FAIL
>
> GET_SIGNATURE:
> PASS/FAIL
>
> SIGNATURE LENGTH:
> 65 / other
>
> ## NODE RECOVERY
>
> For each successful physical run:
>
> PRIVY WALLET:
> 0x...
>
> TRANSPORTED SIGNATURE LENGTH:
> 65
>
> RECOVERED SIGNER:
> 0x...
>
> MATCH:
> PASS/FAIL
>
> ## REPEATABILITY
>
> Report:
> 1/1, 2/2, etc.
>
> ## SECURITY
>
> Confirm:
> - NFC UID unused
> - no private key
> - no Privy secret
> - no OTP/token exposure
> - no blockchain write
> - no ENS authorization yet
> - APDU v1 unchanged
>
> ## WHAT THIS PROVES
>
> State exactly:
>
> Gate D proves real physical ISO-DEP/APDU interoperability between the
> ESP32-S3 + Elechouse PN532 reader and the Seeker Android HostApduService,
> including transport of a real Privy-produced 65-byte Gate A-compatible
> signature.
>
> It does NOT yet prove ENS-based physical authorization.
>
> ## GIT STATE
>
> Do not commit.
>
> ## NEXT TASK
>
> Recommend ONLY Gate E:
>
> replace the deterministic transport vector with a fresh verifier-issued
> Gate A challenge and compose the physically transported holder proof with
> CURRENT ENSv2 owner/access authorization to produce physical ALLOW/DENY.
>
> End exactly:
>
> GATE D: PASS
>
> or
>
> GATE D: STOP — <exact stage/reason>
