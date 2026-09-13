# Sanitized AI task-packet and prompt record

## Coverage and redaction policy

**Coverage: COMPLETE for the material project phases and asset-generation
prompts needed to understand how AI was directed.**

The local Codex session and attachment record was available for review. This
file preserves exact, relevant excerpts rather than publishing the full private
conversation archive. Excluded portions contain repetitive execution mechanics,
public chain-state tables already documented elsewhere, private operational
context, or unrelated conversation. Redactions are written as
`[REDACTED: reason]`; no text is reconstructed and presented as an original
prompt.

No additional Control Tower prompt artifact is currently required for the
public AI disclosure. The private full packet archive is intentionally not part
of the repository.

## Original / sanitized task-packet excerpts

### Initial ENSv2 vertical slice

Source packet: local attachment `3d3ccbaa-9f86-4d79-8ba6-000725037ebf`.

> TASK
> Implement the first minimal ENSv2 Sepolia vertical slice for the
> nodeXbit/ethonline-2026 repository.
> WHY
> We need observable evidence that ENSv2 is authoritative for access

The remaining packet set exact read/write boundaries, validation, and reporting
requirements. `[REDACTED: private environment and operational values]`

### Fresh holder proof (Gate A)

Source packet: local attachment `1a3b2140-71d8-4967-8762-b32e1f9aa5a1`.

> TASK
> Implement ONLY Gate A of the secure holder-proof vertical slice.
> Gate A is pure Node + viem:
> server-issued EIP-712 challenge
> → deterministic TEST EOA signature

### Privy Android signer (Gate B)

Source packet: local attachment `616e5c99-078d-4fb1-ba18-cb31f502ffbb`.

> TASK
> Implement and validate ONLY Gate B:
> native Android Privy embedded EOA
> → sign the exact ENSv2 Access EIP-712 v1 challenge
> → Node/viem recovers the signer

### Android HCE transport (Gate C1 and C2)

Source packets: local attachments `65499839-e899-43ca-b462-28b18f4bc63c`
and `21f4970d-049f-4c4d-9ad1-845d117f6bb8`.

> TASK
> Implement ONLY Gate C1:
> a deterministic Android Host Card Emulation
> ISO-DEP APDU protocol layer for
> the existing ENSv2 holder-proof architecture.
> Do NOT connect it to Privy signing yet.

> TASK
> Implement and validate ONLY Gate C2:
> connect the already-proven native Privy Android EIP-712 signer to the existing
> Gate C1 HCE ProofProvider abstraction while preserving APDU v1 exactly.
> Do NOT modify PN532 firmware.

### Authoritative secure gate composition (Gate E)

Source packet: local attachment `1c9a446d-09dc-406e-a05a-3163bb484249`.

> TASK
> Implement ONLY the local/code side of Gate E:
> fresh server-issued Gate A challenge
> → serial transport to ESP32
> → physical HCE proof transport

The packet continued through current ENSv2 policy evaluation and controller
confirmation with explicit no-write and fail-closed boundaries.

### Android credential product vertical

Source packet: local attachment `6ba73bd2-6372-4434-b7a3-a159c82c501a`.

> TASK — BUILD FIRST END-TO-END ANDROID CREDENTIAL VERTICAL
> MISSION
> Build ONE complete product vertical:
> Android issuer
> → create staff-001.keys.demo-access.eth

`[REDACTED: wallet-operation details and private runtime context]`

### Wallet foundation and Pass Studio

Source packets: local attachments `b047c247-0472-4874-a00c-d79e5fe24cde`
and `4f79b8ed-9a38-42e1-9168-aeb5002f478f`.

> TASK — BUILD WALLET EXPERIENCE FOUNDATION
> MISSION
> Build one coherent Android wallet-experience foundation on top of the now
> checkpointed first credential vertical.

> TASK — BUILD LOCKENS PASS STUDIO
> MISSION
> Turn the current fixed STAFF issuance demo into a real, capability-driven
> LockENS Studio.

### Dynamic selected pass and resource-aware gates

Source packet: local attachment `3ba16fdf-a228-4bef-82c7-4176cf907148`.

> TASK — BUILD LOCKENS DYNAMIC NFC + RESOURCE-AWARE VIRTUAL GATES
> MISSION
> Build the next complete LockENS vertical:
> SELECTED PASS
> → Android HCE

### Four-device demo rig and feature freeze

Source packets: local attachments `12e80a81-006a-4e4b-aa68-f19911d8479b`
and `413f7465-30d8-4a3a-8b64-650f661bf1cd`.

> TASK — PREPARE COMPLETE LOCKENS FOUR-DEVICE DEMO RIG
> MISSION
> Prepare the complete LockENS demo environment using the four Android devices
> now connected to the PC.
> This task should take the project from the current Gate Stand implementation
> to a reproducible final demo configuration.

> TASK — BUILD FINAL LOCKENS DEMO STATE AND FEATURE FREEZE
> MISSION
> Take the current clean, pushed LockENS project from its validated Gate Stand
> baseline to its final demo-ready state.
> This is the LAST feature/provisioning workstream before release audit.

`[REDACTED: real device identifiers, local paths, and private operational
mapping. Public wallet addresses and transaction hashes are documented in
FINAL_DEMO_STATE.md instead of duplicated here.]`

### Privacy audit and controlled history sanitation

Source packets: local attachments `7cd72375-f3b1-4ee5-a883-3e1617d66a7e`,
`32066ab7-8ece-471d-9622-366ab7a2c35f`, and
`0bd93bf7-6df4-4cdc-8c7e-83a53ab5beec`.

> MISSION — AUD-01 EXHAUSTIVE PRIVACY / GIT HISTORY AUDIT
> This is a READ-ONLY inspection task.
> DO NOT modify any file.
> DO NOT modify Git history.
> DO NOT commit.

> MISSION — EXECUTE PRIV-01 APPROVED HISTORY SANITATION
> CONTROL TOWER AUTHORIZATION:
> Execute the previously reviewed PRIV-01 history sanitation plan, subject to
> the modifications and stop conditions below.

The exact personal path, device identifiers, MAC value, and private evidence
locations are omitted here because publishing them would reverse the completed
privacy remediation. `[REDACTED: privacy-sensitive values and local paths]`

### Release documentation remediation

Source packet: local attachment `180ecd8d-be86-4318-ac57-137ff623fcd8`.

> MISSION — FINAL RELEASE DOCUMENTATION REMEDIATION
> FEATURE FREEZE remains active.
> No application code, blockchain state, credentials, firmware source or
> product behavior may change.

## Original image-generation prompts

These three prompts are reproduced exactly from the Codex session record. They
contain no secret, personal path, device identifier, or account data. The tool
record identifies OpenAI image generation invoked through Codex but does not
identify a specific image model.

### Front Door — ORIGINAL / SANITIZED (no redactions required)

> Create an original portrait 9:16 background asset for a premium Android access-control Gate Stand called Front Door. Polished cinematic architectural concept art, realistic but not photographic. View straight into a refined secure corporate lobby beyond an entrance threshold: elegant reception desk, comfortable lounge sofas, indoor plants, warm pendant lighting, glass and wood details, subtle security architecture, and a clear centered walkway. Composition must work behind a large app-drawn central door: place the most recognizable furniture and depth around and beyond the central opening, keep the top area visually calm for white UI title text, and keep the lower third darker and uncluttered for a translucent security panel. Warm amber and teal palette, believable depth, inviting but controlled. No people, no exterior door slab, no text, no letters, no signs, no logos, no watermark, no border, no UI elements.

### Lab — ORIGINAL / SANITIZED (no redactions required)

> Create an original portrait 9:16 background asset for a premium Android access-control Gate Stand called Lab. Polished cinematic architectural concept art, realistic but not photographic. View straight into a clean advanced research laboratory beyond an access threshold: organized workbenches, microscopes and compact scientific instruments, glass-front cabinets, ergonomic stools, task lighting, sealed sample equipment, and a clear centered aisle with convincing depth. Composition must work behind a large app-drawn central door: distribute recognizable laboratory furniture around and beyond the central opening, keep the top area visually calm for white UI title text, and keep the lower third darker and uncluttered for a translucent security panel. Cool white, cyan and restrained cobalt palette, sterile and high-security but credible. No people, no hazardous incident, no exterior door slab, no text, no letters, no signs, no logos, no watermark, no border, no UI elements.

### Server Room — ORIGINAL / SANITIZED (no redactions required)

> Create an original portrait 9:16 background asset for a premium Android access-control Gate Stand called Server Room. Polished cinematic architectural concept art, realistic but not photographic. View straight down a secure modern data-center aisle beyond an access threshold: symmetrical server racks with restrained blue and green status LEDs, perforated cabinet doors, overhead cable trays, cooling vents, precise floor panels, and deep centered perspective. Composition must work behind a large app-drawn central door: place the recognizable racks and infrastructure around and beyond the central opening, keep the top area visually calm for white UI title text, and keep the lower third darker and uncluttered for a translucent security panel. Dark navy, steel, cyan and subtle green palette, serious restricted infrastructure atmosphere. No people, no smoke or hazard, no exterior door slab, no text, no letters, no signs, no logos, no watermark, no border, no UI elements.
