# Project task packet 096: TASK — PREPARE AND ORCHESTRATE FIRST CONTROLLED DYNAMIC NFC PHYSICAL TEST

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — PREPARE AND ORCHESTRATE FIRST CONTROLLED DYNAMIC NFC PHYSICAL TEST
>
> MISSION
>
> The Dynamic NFC + Virtual Gates implementation is complete but physically
> unvalidated.
>
> Prepare the exact first physical test.
>
> Do NOT perform blockchain writes.
>
> Do NOT modify code unless a physical failure proves it necessary.
>
> Do NOT flash automatically.
>
> The USER will authorize/perform the actual firmware flash.
>
> ============================================================
> OBJECTIVE
> ============================================================
>
> First physical dynamic test must use:
>
> Active holder wallet:
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> Selected pass:
> staff-001.keys.demo-access.eth
>
> Virtual gate:
> Lab
>
> Existing staff currently has:
> access.v1 = Allowed
> resources.v1 = MISSING
>
> Therefore expected final result:
>
> ACCESS DENIED
> RESOURCE_POLICY_MISSING
>
> This is an intentional fail-closed test.
>
> Do NOT add resources.v1 before this test.
>
> ============================================================
> PHASE 1 — PREFLIGHT
> ============================================================
>
> Verify read-only/local:
>
> - current worktree contains only expected uncommitted NFC/gate work;
> - NFC_BASELINE_SHA is:
>   20c2dd9866f8c48bc0611dee34b9b272906a3429
> - Android APK is the final tested build;
> - adb sees the expected Seeker;
> - current active wallet is the holder;
> - STAFF is selected;
> - STAFF remains currently owned by holder;
> - STAFF access.v1 is still Allowed;
> - STAFF resources.v1 is still missing;
> - COM4 exists;
> - ESP32-S3 target matches expected hardware;
> - dynamic firmware artifact exists;
> - dynamic firmware hash;
> - legacy firmware build/artifact exists;
> - provide exact ROLLBACK flash command before any flash.
>
> Do not expose secrets.
>
> ============================================================
> PHASE 2 — GATE MONITOR / NODE PREFLIGHT
> ============================================================
>
> Start/validate the required local Node verifier / serial bridge / Gate Monitor
> without performing a tap.
>
> Verify:
>
> - monitor loads;
> - Front Door / Lab / Server Room selector works;
> - select Lab;
> - selected resource latches correctly for a future session;
> - serial COM4 can be opened when appropriate;
> - RPC read path works;
> - existing STAFF read returns RESOURCE_POLICY_MISSING.
>
> Do not invent an ALLOW.
>
> Return exact commands/processes that need to remain running during the physical
> test.
>
> ============================================================
> PHASE 3 — FLASH PLAN
> ============================================================
>
> Provide the exact user command for the dynamic firmware.
>
> Expected current command from prior implementation:
>
> arduino-cli upload --fqbn esp32:esp32:esp32s3 --port COM4 --input-dir .runtime/firmware-dynamic firmware/pn532_dynamic_access
>
> Verify it against the actual artifact/CLI usage before returning it.
>
> Also provide exact rollback command to legacy firmware.
>
> Do not execute either upload yourself.
>
> ============================================================
> PHASE 4 — FIRST TAP EXPECTATION
> ============================================================
>
> After user flashes dynamic firmware:
>
> 1. restart/reconnect serial components cleanly;
> 2. Gate Monitor = Lab;
> 3. Seeker:
>    - active holder wallet;
>    - STAFF selected;
>    - Ready to tap;
> 4. user taps phone on PN532 exactly once.
>
> Capture sanitized evidence for:
>
> GET_CREDENTIAL:
> staff-001.keys.demo-access.eth
>
> resource:
> Lab
>
> credential structural/provenance validation:
> PASS
>
> challenge:
> fresh and resource-bound
>
> holder proof:
> valid
>
> current owner:
> holder
>
> registration:
> valid
>
> access.v1:
> Allowed
>
> resources.v1:
> missing
>
> final:
> DENY
> RESOURCE_POLICY_MISSING
>
> Controller must NOT produce an ALLOW decision.
>
> No fallback to guest-001.
>
> ============================================================
> STOP CONDITIONS
> ============================================================
>
> STOP immediately if:
>
> - GET_CREDENTIAL returns guest-001;
> - wrong selected credential;
> - wrong active wallet;
> - resource != Lab;
> - HCE signs credential mismatch;
> - serial protocol desynchronizes;
> - controller reports ALLOW despite missing resources.v1;
> - Gate silently falls back to legacy;
> - replay/TTL/freshness protection is bypassed;
> - RPC/provider secret appears in monitor/logs.
>
> Do not patch around a physical failure before recording exact evidence.
>
> ============================================================
> RETURN FIRST
> ============================================================
>
> Before the user flashes anything, return:
>
> # DYNAMIC NFC FIRST PHYSICAL TEST — PREFLIGHT
>
> ## DEVICE
>
> ## ANDROID
>
> ## STAFF ONCHAIN
>
> ## NODE / MONITOR
>
> ## DYNAMIC FIRMWARE
>
> Artifact:
> SHA-256:
>
> ## LEGACY ROLLBACK
>
> Artifact:
> SHA-256:
> Exact rollback command:
>
> ## FLASH COMMAND
>
> ## RUN COMMANDS
>
> Exact terminals/processes required.
>
> ## EXPECTED FIRST TAP
>
> Credential:
> Resource:
> Global access:
> Resource policy:
> Final expected decision:
>
> ## STOP CONDITIONS
>
> End exactly:
>
> READY FOR USER-AUTHORIZED DYNAMIC FIRMWARE FLASH
