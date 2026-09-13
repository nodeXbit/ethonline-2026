# LockENS Android gate demo runbook

This is the public, sanitized runbook for the final ETHOnline demo. The live
roles are `<HOLDER>`, `<FRONT_DOOR>`, `<LAB>`, and `<SERVER_ROOM>`. Resolve those
roles from the private local mapping; never copy real device identifiers into
commands, screenshots, logs, or tracked files.

## Architecture and prerequisites

```text
<HOLDER>
  -> NFC / Android HCE
  -> <FRONT_DOOR> / <LAB> / <SERVER_ROOM>
  -> per-gate ADB reverse
  -> local Node pixel-gate bridge
  -> Sepolia RPC
  -> current ENSv2 registration, holder, access, and resource policy
  -> ALLOW / DENY
  -> virtual gate UI
```

Opening the four Android apps does not reconstruct the PC runtime. Exactly one
Node bridge must be running, and every gate needs its own ADB reverse to that
local verifier. Missing Node, tunnel, RPC, holder proof, or valid current policy
fails closed. `npm run gate:monitor` belongs to a historical desktop/PN532
workflow and is not live telemetry for the current Android-gate flow.

## Verified restart sequence

1. Connect the holder and all three gate devices.
2. Use the ignored private mapping to identify `<HOLDER>`, `<FRONT_DOOR>`,
   `<LAB>`, and `<SERVER_ROOM>`.
3. From the repository root, start exactly one verifier:

   ```powershell
   npm run pixel-gate:bridge
   ```

4. Confirm exactly one local verifier listener is ready on port `8792`.
5. Configure the tunnel once for each gate, substituting the device assigned to
   the shown role:

   ```powershell
   adb -s <FRONT_DOOR> reverse tcp:8792 tcp:8792
   adb -s <LAB> reverse tcp:8792 tcp:8792
   adb -s <SERVER_ROOM> reverse tcp:8792 tcp:8792
   ```

6. Launch the Gate Reader on each mapped device with the persistent profile
   `front-door`, `lab`, or `server-room`, respectively.
7. Confirm all three gates show `READY - TAP LOCKENS PASS` and a closed virtual
   door.
8. On `<HOLDER>`, open LockENS and go to **My Keys**.
9. Select the intended embedded wallet and its matching pass.
10. Press **Refresh onchain**.
11. Wait for **Ready to tap**. Switching wallet or pass requires a fresh
    selection and fresh HCE state; never reuse a stale selection.
12. Present `<HOLDER>` to the intended gate and observe the physical NFC result.

The Android gate never authorizes independently. Node verifies the fresh holder
proof offchain, reads current ENSv2 state from Sepolia, evaluates the selected
resource, and returns the authoritative `ALLOW` or `DENY`. The virtual gate
animation opens only after `ALLOW`; every error and denial leaves it closed.

If a launch helper stops after the harmless ADB message that an activity is
already in the foreground, repeat the affected role's `adb reverse`, launch that
Gate Reader Activity manually, and visually reconfirm its profile and READY
state. Do not change credentials or onchain state as a startup repair.

## Final demonstration matrix

| Holder/pass | Gate | Expected result |
| --- | --- | --- |
| Staff | Lab | `ACCESS GRANTED` |
| Staff | Server Room | `ACCESS GRANTED` |
| Visitor | Lab | `ACCESS DENIED / RESOURCE_NOT_ALLOWED` |
| Visitor | Front Door | `ACCESS GRANTED` |
| Contractor | Lab | `ACCESS DENIED / ACCESS_SUSPENDED` |
