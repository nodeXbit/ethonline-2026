# Local demo runbook

Use `guest-001.demo-access.eth`, holder `0x3419148731087b970d2059C53780163B452D5FF7`,
resource `demo-access.eth:door-001`, on Sepolia. Generic `cred-001` commands remain separate.
Run commands from the repository root. Keep `.env.local` and `.env.nfc.local` private;
both require `ENS_PARENT_NAME=demo-access.eth` and matching primary RPC configuration.
DEV signing configuration stays in `.env.local`; the bridge never needs the DEV key.
Optional `SEPOLIA_RPC_FALLBACK_URL` belongs in `.env.nfc.local`; preflight reports presence only.

## Cold boot and startup

1. Keep Seeker away from PN532.
2. Disconnect **all** controller USB and power.
3. Verify the established CH343 disappears from Device Manager (Ports).
4. Wait approximately 10 seconds with all power removed.
5. Reconnect only the established CH343 path. Do not assume COM4; preflight and the demo bridge discover the current port.
6. Observe passive boot output at 115200 baud. If output is absent, use **one short RST/EN press** while observing. Never press BOOT unless an actual upload recovery calls for it.
7. Require I2C `0x24` ACK and PN532 1.6 readiness. The committed secure firmware checks ACK and stops on failure; it does **not** print a separate ACK PASS line. Its ordered success output is:

   ```text
   PN532_FIRMWARE=1.6
   GATE_E_READY
   PRESENT_SEEKER
   ```

   Seeing all three from this boot implies the preceding I2C ACK check passed. Any `GATE_E: STOP` fails readiness.
8. Run `npm run demo:preflight`. It passively opens/closes the detected port, observes for five seconds, reads a fresh primary RPC snapshot and DEV transaction counts, and performs zero blockchain/protocol writes. If boot output already passed, it honestly stops with `MANUAL_COLD_BOOT_REQUIRED`; an old readiness line is insufficient.

   To capture readiness within preflight, use `npm run demo:preflight -- --observe-boot` as the passive observer for steps 6–8. It observes for 30 seconds. After the full power cycle, if natural output was missed, make the single short RST/EN press during that window. Close a competing monitor before this command. Do not reset automatically or keep retrying.
9. Close any competing serial monitor. Require `DEMO PREFLIGHT: PASS` and complete the manual phone check below. A hardware failure means STOP even if onchain checks pass.
10. Start the UI with `npm run demo` and open **http://127.0.0.1:4173**. In a second terminal run `npm run demo:bridge` (or `npm run demo:bridge -- --check-replay` for the ALLOW/replay segment).
11. Present Seeker only after the bridge is listening and the controller explicitly requests `PRESENT_SEEKER`. If that request was captured by preflight, use the readiness from this same boot, with no intervening tap/reset/power change.
12. Hold steady through the controller result. Verifier ALLOW alone is insufficient; require **PHYSICAL CONTROLLER: CONFIRMED ALLOW**. The bridge runs one attempt and closes serial.

The automated healthy target is under 120 seconds (normally roughly 15–50 seconds,
including observation). Primary reads use the Batch A eight-second budget and
60-second block freshness / 15-second future-skew limits. Access validity needs at
least 86,400 seconds remaining. ACTIVE and INACTIVE are both acceptable. Pending
DEV transaction counts must equal latest because demo writes are planned.
`--no-writes-planned` only relaxes that nonce equality check; it never writes.

## Manual phone check

- Seeker unlocked; NFC ON; Internet available.
- ENSv2 Access Demo open; Privy authenticated.
- Read the existing **PRIVY WALLET** display (or Copy wallet address) and compare with
  `0x3419148731087b970d2059C53780163B452D5FF7`.

Preflight cannot inspect the phone session. SYSTEM READINESS shows the timestamped
last automated result; it does not certify the phone or future hardware state.

## STOP and recovery

STOP on wrong owner/resolver/chain, invalid registry/access record, less than 24 hours
access validity, stale block, unavailable primary RPC, pending DEV transaction,
missing/ambiguous CH343, busy port, missing boot evidence, firmware STOP, unexpected
replay result, or missing/mismatched controller confirmation. Do not tap through a failure.

If a physical attempt fails: remove phone, stop bridge, never reuse the proof/challenge,
perform the full cold boot, preflight again, and start a new attempt. No “retry until it works.”
Normal firmware completion also halts the controller: cold boot and preflight before
each fresh physical attempt. Keep the phone away during boot and policy changes.

## Reading the page

- **ENSv2 POLICY** is current registered credential policy, never a physical door result.
- **HOLDER VERIFIER** is the last Gate E attempt, with signer/owner match, reason, block and timestamp.
- **PHYSICAL CONTROLLER** turns green only after matching controller confirmation.
- **REPLAY: DENIED** means the bridge checked the consumed proof in memory. It is not an additional NFC tap or a second physical-controller DENY.
- A new bridge run replaces previous success with IN PROGRESS. A killed process may leave IN PROGRESS; follow recovery. Missing/malformed reports show NOT RUN.

Ignored `.runtime/gate-e-last-attempt.json` and `.runtime/demo-preflight.json` are
local, sanitized, best-effort evidence, never authorization inputs. If reporting is
unavailable, STOP recording and inspect the terminal; the authorization path still runs.
Do not persist proof bytes or manufacture successful runtime evidence.

## Controlled rehearsal — plan only

Do not perform this during Batch B implementation.

1. Cold boot/preflight, inspect the UI, and establish INACTIVE. If currently ACTIVE,
   deactivate once through the local UI and wait for confirmed receipt/readback.
2. Fresh bridge run and **tap 1**: current holder + INACTIVE → verifier DENY and confirmed controller DENY.
3. Remove phone. Activate guest once through the local UI; wait for confirmed receipt/readback.
4. Full cold boot/preflight. Run `npm run demo:bridge -- --check-replay` and **tap 2**:
   fresh holder proof → verifier ALLOW → confirmed controller ALLOW. The bridge also
   checks the same consumed proof in memory → REPLAY DENIED, with no additional tap.
5. Optionally deactivate guest after recording and wait for confirmed receipt/readback.

Exactly **two physical taps**. Core narrative from INACTIVE: **one blockchain write**
(activation). From the expected ACTIVE baseline: **two writes** (initial deactivation
and activation). Optional final deactivation adds **one write**: three total from ACTIVE,
or two total from INACTIVE. Idempotent already-achieved controls require no write.
Do not click again while a submission or readback is pending; follow the existing
read-only recovery messages. Review/checkpoint Batch B before executing this plan.
