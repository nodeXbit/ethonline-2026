# Status

## Final LockENS release state — 2026-09-13

**DEMO READY. FEATURE FREEZE ACTIVE.**

Current product: one Android holder and three Android NFC gate stands, backed by
a local Node authoritative verifier and current ENSv2 state on Sepolia. The
virtual gate opens only after Node verifies holder proof offchain and evaluates
current UserRegistry/PermissionedResolver resource policy.

Final evidence is recorded in [FINAL_DEMO_STATE.md](FINAL_DEMO_STATE.md): seven
credentials, fourteen authorized confirmed Sepolia operations, the final
resource-policy matrix, three ready Android gate profiles, physical grant/deny
cases, multi-wallet/pass selection, and HCE freshness/invalidation.

## Validation and release controls

- Final runtime preflight and restored demo flow: PASS.
- Node: 238/238 tests PASS.
- Android: 261 unit tests PASS; `assembleDebug` PASS.
- Firmware: three approved ESP32-S3 sketches compile; no flash was part of the
  release-documentation validation.
- Privacy audit: AUD-01 PASS.
- Controlled main-history sanitation: PRIV-01 PASS.
- Sanitized release base before this documentation increment:
  `e7ce904c7ff4156ed7b7aecd115e54c86fa4a6df`.
- The current SHA after the documentation commit is the commit reported by Git;
  this file intentionally does not predict a commit hash before it exists.
- Repository visibility remains private until the final compliance decision.

## Remaining work

1. Perform the final documentation/compliance audit.
2. Change repository visibility to public only after that audit passes.
3. Record and finalize the demo video.
4. Complete the ETHOnline submission and final narration.

No product feature, blockchain write, credential change, firmware change, or
new physical architecture is part of this remaining work.

## Historical evidence

Earlier root reports preserve engineering checkpoints and may mention
`RESOURCE_POLICY_MISSING`, pending Android fallback, or the ESP32-S3/PN532 path
as the primary verifier. Those statements accurately describe their dates but
are superseded for the final release narrative by this status,
[README.md](README.md), [PROJECT.md](PROJECT.md), and
[FINAL_DEMO_STATE.md](FINAL_DEMO_STATE.md). Historical reports were retained for
traceability rather than rewritten as if their outcomes occurred later.
