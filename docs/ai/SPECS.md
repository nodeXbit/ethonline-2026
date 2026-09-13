# LockENS specification and planning index

LockENS used living specifications and checkpoint documents rather than one
static requirements file. This index points to the genuine tracked artifacts
that directed implementation and bounded claims.

## Product and control

- [PROJECT.md](../../PROJECT.md) — current product goal, architecture, scope,
  security boundary, and limitations.
- [AGENTS.md](../../AGENTS.md) — repository-level rules for inspecting, editing,
  validation, privacy, commits, and scope control.
- [DECISIONS.md](../../DECISIONS.md) — accepted architecture/security decisions
  and historical rationale; the newest final-demo decision supersedes older
  hardware-primary wording for the release narrative.
- [STATUS.md](../../STATUS.md) — authoritative current release state and remaining
  submission work.
- [WORKLOG.md](../../WORKLOG.md) — chronological implementation, validation,
  audit, sanitation, and recovery evidence.
- [FINAL_DEMO_STATE.md](../../FINAL_DEMO_STATE.md) — final public Sepolia state,
  pass policy matrix, public transactions, physical validation, and freeze.

## Architecture and security specifications

- [ANDROID_GATE_READER.md](../../ANDROID_GATE_READER.md) — Android reader and
  local Node gate boundary.
- [GATE_STAND.md](../../GATE_STAND.md) — persistent gate profiles and virtual-door
  semantics.
- [DEMO_RUNBOOK.md](../../DEMO_RUNBOOK.md) — generic public demo workflow and
  fail-closed operational expectations.
- [DYNAMIC_NFC_RUNBOOK.md](../../DYNAMIC_NFC_RUNBOOK.md) — historical dynamic
  NFC/PN532 procedure; retained for the alternate hardware route.
- [scripts/ensv2/persistent-access.md](../../scripts/ensv2/persistent-access.md) —
  persistent credential/access lifecycle commands and boundaries.

## Test plans, audits, and physical evidence

- [PHYSICAL_NFC_CHECKPOINT.md](../../PHYSICAL_NFC_CHECKPOINT.md) — checkpoint
  audit and physical NFC evidence.
- [NFC_TEST_COORDINATION.md](../../NFC_TEST_COORDINATION.md) — coordinated NFC
  validation plan.
- [NFC_AB_TEST_PLAN.md](../../NFC_AB_TEST_PLAN.md) — bounded A/B diagnostic plan.
- [NFC_READ_ONLY_AUDIT.md](../../NFC_READ_ONLY_AUDIT.md) — read-only NFC audit.
- [FOUR_SMARTPHONE_GATE_STAND_REPORT.md](../../FOUR_SMARTPHONE_GATE_STAND_REPORT.md)
  — three-gate Android Stand implementation and physical regression evidence.
- [ANDROID_GATE_READER_PHYSICAL_REPORT.md](../../ANDROID_GATE_READER_PHYSICAL_REPORT.md)
  — Android reader physical validation.

The root `NFC_*` reports are historical diagnostic evidence. They document
specific checkpoints and should not be read as the primary current architecture.
They remain useful for understanding how fail-closed behavior and the alternate
ESP32-S3/PN532 route were tested.

## AI-direction evidence

[PROMPTS.md](PROMPTS.md) preserves sanitized original excerpts from the material
Control Tower task packets and the exact image-generation prompts. The excerpts
show the implementation goals, authority boundaries, stop conditions, and
validation requirements without publishing private operational identifiers or
unrelated conversation.
