# Project task packet 098: TASK — BUILD AND VALIDATE ANDROID GATE READER FALLBACK

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — BUILD AND VALIDATE ANDROID GATE READER FALLBACK
>
> WHY
>
> The primary LockENS physical verifier is already checkpointed and physically
> validated at:
>
> PHYSICAL_NFC_BASELINE_SHA
> ec82f3f578b610ff3978de10d197f1032e449809
>
> It successfully completed:
>
> staff-001
> → Lab
> → holder proof valid
> → registration valid
> → access.v1 Allowed
> → controller confirmed
> → DENY RESOURCE_POLICY_MISSING
>
> The ESP32 + PN532 path remains PRIMARY and must not be modified.
>
> This task adds a SECOND transport:
>
> Android Pixel Reader Mode
> → Seeker HCE
> → existing Node verifier
>
> TIMEBOX
>
> 60 MINUTES MAXIMUM.
>
> If a minimal end-to-end reader cannot be achieved inside that timebox, STOP
> without broad debugging or architectural expansion.
>
> ============================================================
> SCOPE
> ============================================================
>
> Build the smallest Android Gate Reader capable of replacing ONLY:
>
> PN532
> +
> ESP32
> +
> serial transport
>
> for a demo/fallback.
>
> Preserve:
>
> existing Seeker HCE
> existing AID
> existing APDUs
> existing 104-byte challenge
> existing holder-proof scheme
> existing Node credential validation
> existing ENS verification
> existing resources.v1 policy
> existing replay/TTL protections
> existing Gate Monitor semantics
>
> DO NOT alter the primary PN532 path.
>
> ============================================================
> NO-TOUCH
> ============================================================
>
> Do NOT:
>
> - modify firmware;
> - flash firmware;
> - alter PN532 code;
> - change HCE protocol;
> - change challenge bytes;
> - weaken TTL/replay/TOCTOU protections;
> - change Studio;
> - perform blockchain writes;
> - add resources.v1;
> - create credentials;
> - add new Solidity;
> - move authorization decisions to Android;
> - add backend/cloud infrastructure;
> - commit/push until physical validation succeeds.
>
> ============================================================
> PHASE 1 — DEVICE PREFLIGHT
> ============================================================
>
> READ-ONLY FIRST.
>
> Run:
>
> adb devices -l
>
> There may be BOTH:
>
> - Seeker holder phone
> - Pixel GrapheneOS gate phone
>
> Identify each device unambiguously by serial/model.
>
> From this point NEVER use an ambiguous adb command that could target the wrong
> phone.
>
> Use:
>
> adb -s <SERIAL>
>
> for every device-specific operation.
>
> For the PIXEL verify:
>
> - android.hardware.nfc present;
> - NFC enabled;
> - Android/API version;
> - Reader Mode APIs available;
> - IsoDep available;
> - USB debugging stable.
>
> Useful inspections may include:
>
> adb -s <PIXEL> shell pm list features
> adb -s <PIXEL> shell dumpsys nfc
> adb -s <PIXEL> shell getprop ro.product.model
> adb -s <PIXEL> shell getprop ro.build.version.sdk
>
> For SEEКER verify only:
>
> - device present;
> - existing LockENS APK present;
> - NFC enabled.
>
> Do not clear app data.
>
> If Pixel lacks the required NFC/ISO-DEP capability:
>
> STOP immediately.
>
> ============================================================
> PHASE 2 — INSPECT EXISTING PROTOCOL
> ============================================================
>
> Inspect source, not summaries.
>
> Pin exact existing:
>
> AID
>
> SELECT APDU
>
> GET_CREDENTIAL
> 80 40 01 00
>
> SEND_CHALLENGE
> 80 10 01 00
>
> GET_STATUS
> 80 20 01 00
>
> GET_SIGNATURE
> 80 30 01 00
>
> response/status conventions
>
> challenge encoding
>
> proof response encoding
>
> timing bounds
>
> Do not create a second protocol implementation with hand-copied constants if
> the existing Android code can safely share them.
>
> Prefer reuse/shared code where it does NOT risk the holder path.
>
> ============================================================
> PHASE 3 — MINIMAL PIXEL GATE READER
> ============================================================
>
> Implement the smallest isolated Gate Reader surface.
>
> Prefer an isolated Activity/component within the existing Android project
> unless repository structure strongly favors a tiny separate module.
>
> Do NOT expose it in the normal holder navigation unless necessary.
>
> It may be launchable explicitly through adb for this sprint.
>
> Reader must use Android foreground NFC Reader Mode.
>
> Use NFC-A / ISO-DEP appropriate to the current HCE implementation.
>
> Disable unnecessary NDEF handling when supported.
>
> On tag discovery:
>
> 1. require IsoDep;
> 2. connect;
> 3. use bounded timeout;
> 4. SELECT LockENS AID;
> 5. require exact successful status;
> 6. GET_CREDENTIAL;
> 7. parse bounded UTF-8 full ENS name;
> 8. request resource-bound challenge from Node;
> 9. SEND_CHALLENGE;
> 10. poll/use existing semantics for status;
> 11. GET_SIGNATURE;
> 12. submit proof to Node;
> 13. render Node's authoritative result.
>
> Close IsoDep safely on:
> success
> failure
> Activity pause
> tag removal / I/O exception.
>
> No automatic repeated submissions from the same tap.
>
> ============================================================
> PHASE 4 — PIXEL UI
> ============================================================
>
> Minimal but demo-usable.
>
> Screen:
>
> LOCKENS GATE READER
>
> Virtual Gate
> [ Front Door | Lab | Server Room ]
>
> For first test:
> Lab
>
> State:
>
> READY — TAP LOCKENS PASS
>
> Then:
>
> Credential
> staff-001.keys.demo-access.eth
>
> Holder
> Verifying...
>
> Global access
> ...
>
> Resource policy
> ...
>
> Final:
>
> ACCESS GRANTED
>
> or
>
> ACCESS DENIED
> RESOURCE_POLICY_MISSING
>
> Do not invent authorization details locally.
>
> All policy result fields come from Node's authoritative result.
>
> ============================================================
> PHASE 5 — NODE BRIDGE
> ============================================================
>
> Reuse existing Node verifier.
>
> Do not duplicate policy verification.
>
> Add the smallest local transport/API needed for Pixel.
>
> Prefer localhost-only binding.
>
> Connect Pixel to the PC using USB:
>
> adb reverse
>
> Select an unused stable localhost port according to current repo conventions.
>
> Example only:
>
> adb -s <PIXEL> reverse tcp:8792 tcp:8792
>
> Verify actual chosen port before use.
>
> The Pixel should be able to call:
>
> 127.0.0.1:<PORT>
>
> through adb reverse.
>
> Required conceptual API:
>
> START GATE SESSION
> input:
> resourceId
> credential
>
> output:
> bounded holder challenge/session identity
>
> COMPLETE GATE SESSION
> input:
> session identity
> holder proof
>
> output:
> existing authoritative verifier result
>
> Do not return:
> secrets
> RPC URLs containing credentials
> raw internal exceptions.
>
> ============================================================
> SECURITY BOUNDARY
> ============================================================
>
> PIXEL IS UNTRUSTED TRANSPORT.
>
> Never trust Pixel claims for:
>
> owner
> registration
> resolver
> access.v1
> resources.v1
> final ALLOW
>
> Node validates all of them.
>
> Node chooses/validates the resource against the configured virtual gate session.
>
> A malicious Pixel must not be able to turn:
>
> Lab
>
> into:
>
> Front Door
>
> after a challenge is issued.
>
> Session resource is immutable.
>
> ============================================================
> REPLAY / CONCURRENCY
> ============================================================
>
> A Gate Reader tap must preserve current protections.
>
> Require:
>
> fresh challenge
>
> credential-bound challenge
>
> resource-bound challenge
>
> one-shot nonce
>
> proof expiry
>
> same proof cannot be reused
>
> same proof cannot be replayed against another resource
>
> Pixel reconnect/retry must not silently submit a consumed proof.
>
> ============================================================
> PHASE 6 — TESTS
> ============================================================
>
> Add focused tests.
>
> Android:
>
> ReaderMode lifecycle
> non-IsoDep tag rejection
> SELECT status failure
> GET_CREDENTIAL success
> bounded credential
> SEND_CHALLENGE
> signature response parsing
> tag removed
> I/O exception
> Activity pause cleanup
> duplicate tag callback / one logical session
> Node unavailable
> resource selection latch
>
> Node:
>
> Pixel session creation
> resource immutability
> credential binding
> proof completion
> replay
> expired session
> unknown session
> malformed Pixel input
> RPC failure
> same existing ENS/policy verifier remains authoritative
>
> Do not weaken existing suites.
>
> ============================================================
> PHASE 7 — BUILD / INSTALL
> ============================================================
>
> Run:
>
> Android tests
> assembleDebug
> Node tests
> git diff --check
>
> Use actual counts.
>
> Install ONLY on the identified Pixel using explicit serial:
>
> adb -s <PIXEL_SERIAL> install -r <APK>
>
> Do not accidentally reinstall or clear the Seeker.
>
> If the same APK contains holder state, do not copy holder data from Seeker.
>
> Pixel Gate Reader must not require the holder's Privy account.
>
> ============================================================
> PHASE 8 — TRANSPORT-ONLY PHYSICAL TEST FIRST
> ============================================================
>
> Before involving a real holder signature, prove:
>
> Pixel Reader
> ↕
> Seeker HCE
>
> SELECT:
> 9000
>
> GET_CREDENTIAL:
> staff-001.keys.demo-access.eth
> 9000
>
> Capture sanitized evidence.
>
> If this fails, debug ONLY Android ReaderMode/IsoDep within the timebox.
>
> ============================================================
> PHASE 9 — END-TO-END PHYSICAL TEST
> ============================================================
>
> Only after transport works:
>
> Node Gate session:
> Lab
>
> Expected existing chain state:
>
> staff-001
> owner valid
> registration valid
> access.v1 Allowed
> resources.v1 MISSING
>
> User performs ONE physical tap.
>
> Expected:
>
> Pixel discovers STAFF
>
> Node issues Lab-bound challenge
>
> Pixel sends challenge
>
> Seeker holder signs
>
> Pixel obtains proof
>
> Node verifies holder
>
> Node rereads ENS policy
>
> final:
>
> ACCESS DENIED
> RESOURCE_POLICY_MISSING
>
> This must be the same authorization meaning as the previously validated PN532
> result.
>
> No controller hardware exists in the Pixel path.
>
> Therefore DO NOT falsely label an ESP32 controller as confirmed.
>
> For the Android verifier fallback, report truthfully:
>
> Verifier transport confirmed
>
> and:
>
> Virtual gate decision
>
> Do not claim a physical actuator.
>
> ============================================================
> PHASE 10 — COMPARISON
> ============================================================
>
> Document:
>
> PRIMARY:
> ESP32 + PN532
>
> FALLBACK:
> Android Reader Mode Pixel
>
> Shared:
> HCE protocol
> holder proof
> Node verifier
> ENSv2 policy
> virtual resources
> ALLOW/DENY semantics
>
> Different:
> transport/controller layer
>
> No claim that Pixel is production gate hardware.
>
> ============================================================
> STOP IF
> ============================================================
>
> STOP if:
>
> - Pixel does not support ReaderMode/IsoDep;
> - GrapheneOS blocks required standard NFC behavior;
> - implementation requires changing Seeker HCE protocol;
> - implementation requires weakening security;
> - Node authorization would have to move into Pixel;
> - scope exceeds the 60-minute timebox;
> - existing PN532 path would need modification;
> - a real blockchain write becomes necessary.
>
> Do NOT respond by redesigning the system.
>
> ============================================================
> IF SUCCESSFUL
> ============================================================
>
> After physical:
>
> staff-001
> → Pixel Lab Gate
> → RESOURCE_POLICY_MISSING
>
> run final validation.
>
> Do NOT commit/push yet.
>
> Return first for Control Tower review.
>
> ============================================================
> RETURN
> ============================================================
>
> # LOCKENS ANDROID GATE READER — RESULT
>
> ## TIMEBOX
>
> Elapsed:
>
> ## PIXEL PREFLIGHT
>
> Model:
>
> Android:
>
> NFC:
>
> ReaderMode:
>
> IsoDep:
>
> ## ARCHITECTURE
>
> ## ANDROID READER
>
> AID:
>
> APDUs reused:
>
> ## NODE BRIDGE
>
> Port:
>
> adb reverse:
>
> Authorization boundary:
>
> ## TESTS
>
> Android:
>
> Node:
>
> assemble:
>
> diff check:
>
> ## PHYSICAL TRANSPORT
>
> SELECT:
>
> GET_CREDENTIAL:
>
> Credential:
>
> ## PHYSICAL END TO END
>
> Resource:
>
> Holder proof:
>
> Registration:
>
> Global access:
>
> Resource policy:
>
> Decision:
>
> ## PRIMARY VERIFIER REGRESSION
>
> PN532/ESP32 code modified:
> NO
>
> ## SECURITY
>
> Blockchain writes:
> 0
>
> Existing STAFF modified:
> NO
>
> Secrets:
> 0
>
> ## GIT
>
> Changes:
> UNCOMMITTED
>
> ## VERDICT
>
> ANDROID GATE READER:
>
> WORKING
> NOT WORKING — STOPPED WITHIN TIMEBOX
>
> If working end exactly:
>
> ANDROID GATE READER FALLBACK PHYSICALLY VALIDATED
