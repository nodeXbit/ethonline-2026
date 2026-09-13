# Project task packet 021: TASK — Resume Gate D from the now-proven TARGET_ACTIVATION boundary and perform the

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Resume Gate D from the now-proven TARGET_ACTIVATION boundary and perform the
> full physical PN532 ↔ Seeker HCE transport validation.
>
> Do not redesign anything unless a concrete later-stage failure requires a
> small scoped fix.
>
> CURRENT VERIFIED RESULT
>
> TARGET ACTIVATION IS NOW PASS.
>
> Control tests proved:
>
> - known physical tag 91:2D:E3:06 is still detected
> - PN532 RF field/antenna/wiring/I2C/ESP32-S3 are healthy
> - Seeker NFC enabled
> - FEATURE_NFC_HOST_CARD_EMULATION = YES
> - HCE service enabled
> - isDefaultServiceForAid = YES
> - activation-only sketch successfully produced:
>
> TARGET_ACTIVATION: PASS
>
> The normal Gate D firmware has been restored.
>
> CURRENT REPOSITORY STATE
>
> Expected:
>
> HEAD == origin/main ==
> 5cb7d3561492afeb5b9477b32ab9dd34688df7d6
>
> Expected uncommitted scope:
>
> - firmware/pn532_hce_apdu/
> - temporary Android HCE diagnostic edit in MainActivity
>
> No commit/push.
>
> The temporary Android diagnostic label concerning dynamically registered AIDs
> is known to be misleading and must NOT be treated as evidence of static AID
> failure.
>
> Do not clean it up yet unless required for the physical Gate D run.
>
> FIRST
>
> Confirm read-only:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> Require pushed baseline unchanged.
>
> Do NOT discard current Gate D work.
>
> PHONE PRECONDITION
>
> Before starting the physical session, instruct the user to:
>
> 1. open ENSv2 Access Demo on the Seeker;
> 2. keep NFC enabled;
> 3. keep Internet available;
> 4. keep the device unlocked;
> 5. ensure Privy session is authenticated;
> 6. press Create/reuse Ethereum wallet if needed;
> 7. confirm the existing public wallet is available.
>
> Expected wallet:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> Do not request OTP/config/secrets.
>
> Do not ask the user to run the manual Gate C2 harness.
>
> PHYSICAL SESSION 1
>
> Use the already-restored normal Gate D firmware.
>
> Capture serial output directly if possible.
>
> Require boot:
>
> GATE_D_READY
> PRESENT_SEEKER
>
> Then tell the user exactly:
>
> PRESENT THE SEEKER TO THE PN532 NOW AND HOLD IT STEADY
>
> The user should keep the NFC antenna area in position until PASS/STOP.
>
> Run the complete frozen APDU v1 flow.
>
> STAGE A — TARGET ACTIVATION
>
> Require:
> PASS
>
> If activation fails once because of positioning:
>
> allow one bounded positioning retry.
>
> Do not modify code.
>
> STAGE B — SELECT
>
> Send the frozen SELECT AID:
>
> AID:
> F0454E5356324331
>
> Require:
> 9000
>
> Capture:
>
> SELECT: PASS
>
> If SELECT fails:
>
> STOP the session.
>
> Report:
> - exact response bytes/status if available
> - whether target remained activated
>
> Do not proceed to SEND_CHALLENGE.
>
> STAGE C — SEND_CHALLENGE
>
> Send the existing deterministic Gate C2 transport vector as the frozen
> 104-byte binary challenge.
>
> Require:
>
> SEND_CHALLENGE: PASS
>
> After this point:
>
> NEVER resend SEND_CHALLENGE during the current session.
>
> If communication fails after this point:
> STOP the session rather than creating a second signing request.
>
> STAGE D — GET_STATUS
>
> Poll only GET_STATUS at the existing bounded cadence.
>
> Expected:
>
> STATUS: PROCESSING
> then:
> STATUS: READY
>
> A direct READY without an observed PROCESSING poll is acceptable if signing
> completed between polls.
>
> ERROR:
> STOP.
>
> Timeout:
> STOP without resending challenge.
>
> Record approximate time from SEND_CHALLENGE success to READY if it can be
> measured from existing tooling without adding complex instrumentation.
>
> STAGE E — GET_SIGNATURE
>
> Only after READY.
>
> Require exact response:
>
> 65 signature bytes
> +
> 90 00
>
> Require:
>
> SIGNATURE_LEN=65
>
> Capture the strict serial line:
>
> GATE_D_SIGNATURE=0x...
>
> Do not route the signature through ChatGPT.
>
> Validate locally:
>
> - ASCII-only hex
> - exactly 130 hex chars after 0x
> - exactly 65 decoded bytes
>
> STAGE F — NODE RECOVERY
>
> Use:
>
> node scripts/security/gate-c2-verify.mjs <wallet> <transported-signature>
>
> Expected wallet:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> Require:
>
> RECOVERED SIGNER ==
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> MATCH:
> PASS
>
> No ENS reads.
> No blockchain writes.
>
> SESSION 1 PASS CRITERIA
>
> Require all:
>
> TARGET ACTIVATION PASS
> SELECT PASS
> SEND_CHALLENGE PASS
> READY
> GET_SIGNATURE PASS
> 65 bytes
> NODE MATCH PASS
>
> If any later stage fails, identify EXACT stage and STOP random debugging.
>
> REPEATABILITY — SESSION 2
>
> Only after Session 1 fully passes:
>
> 1. user removes Seeker from PN532;
> 2. reset/reboot ESP32 Gate D sketch;
> 3. wait for PRESENT_SEEKER;
> 4. tell user to present Seeker again;
> 5. execute a completely fresh Gate D session;
> 6. capture a fresh signature;
> 7. verify it with Node.
>
> Require:
>
> 2/2 complete physical sessions PASS
>
> A second signature may differ from the first; that is fine.
>
> Do not compare signature bytes for equality.
>
> Compare recovered signer to the same Privy wallet.
>
> If Session 2 has one obvious transient positioning failure before
> SEND_CHALLENGE, allow one bounded retry.
>
> If failure occurs after SEND_CHALLENGE:
> STOP rather than re-triggering within the same session.
>
> IMPORTANT DEBUGGING BOUNDARY
>
> If failure occurs at:
>
> SELECT
> SEND_CHALLENGE
> STATUS
> GET_SIGNATURE
> SIGNATURE_LENGTH
> NODE_RECOVERY
>
> do not immediately edit firmware.
>
> First return:
>
> - exact stage
> - actual request/response length
> - available response/status bytes
> - whether NFC link remained alive
> - whether Android app showed any visible status/error
>
> Then make at most ONE small evidence-driven fix if the cause is obvious and
> does not alter APDU v1.
>
> If the fix would require:
> - changing AID
> - changing APDU command bytes
> - chunking
> - changing Privy signing semantics
> - changing Gate A/B/C semantics
> - switching PN532 libraries
>
> STOP and report instead.
>
> POST-PASS CLEANUP — DO NOT COMMIT YET
>
> If 2/2 physical sessions pass:
>
> 1. inspect the temporary Android diagnostic edit;
> 2. since the "CATEGORY_OTHER AID REGISTERED" label is semantically misleading
>    for statically registered AIDs, either:
>    - revert the temporary diagnostic UI entirely, preferred if no longer
>      needed;
>    OR
>    - rename only the misleading label to accurately state that it reports
>      dynamically registered AIDs.
>
> Prefer reverting the diagnostic-only Android change if it has no continuing
> demo/product value.
>
> Do NOT alter Gate C HCE behavior.
>
> After cleanup rerun:
>
> Android:
> - :app:testDebugUnitTest
> - :app:assembleDebug
>
> Node:
> - node --test --test-isolation=none
>
> Require:
>
> Android 21/21 PASS
> Android build PASS
> Node 83/83 PASS
>
> Also:
>
> - git diff --check
> - secret scan
> - tracked status scoped appropriately
> - no local Privy config staged
> - no transported real signature stored in repository
>
> Do NOT commit.
> Do NOT push.
>
> RETURN
>
> # GATE D PHYSICAL PN532 HCE TRANSPORT
>
> ## BASELINE
> ## PHONE READINESS
> ## SESSION 1
>
> Report:
> TARGET ACTIVATION
> SELECT
> SEND_CHALLENGE
> STATUS
> GET_SIGNATURE
> SIGNATURE LENGTH
> NODE RECOVERY
>
> ## SESSION 2
>
> Same structure.
>
> ## SIGNING LATENCY
>
> Approximate observed SEND_CHALLENGE → READY if available.
>
> ## REPEATABILITY
>
> Require:
> 2/2 PASS
>
> ## ANDROID DIAGNOSTIC CLEANUP
>
> State whether temporary diagnostics were reverted or retained/renamed and why.
>
> ## FINAL TESTS
> ## SECURITY
> ## FILES CHANGED
> ## GIT STATE
> ## WHAT THIS PROVES
> ## NEXT TASK
>
> WHAT THIS PROVES must state:
>
> Gate D proves real physical ISO-DEP/APDU interoperability between the
> ESP32-S3 + Elechouse PN532 and the Seeker HostApduService, including transport
> of a real Privy-produced 65-byte Gate A-compatible EIP-712 signature and Node
> recovery of the same embedded wallet.
>
> It still does NOT prove ENS-based physical authorization.
>
> NEXT TASK must recommend only Gate E.
>
> End exactly:
>
> GATE D: PASS
>
> or
>
> GATE D: STOP — <exact stage/reason>
