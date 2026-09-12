# LockENS Android Gate Reader fallback

This is a demo/fallback transport. It does not replace or modify the primary
ESP32 + PN532 verifier, and it is not production gate hardware.

## Architecture

- Primary: ESP32 + PN532 + serial transport.
- Fallback: Pixel foreground Reader Mode + NFC-A/ISO-DEP + USB `adb reverse`.
- Shared: Seeker HCE AID/APDUs, 104-byte holder challenge, holder proof, Node
  ENSv2/resource verifier, replay/TTL rules, and ALLOW/DENY meaning.
- Different: reader transport/controller layer only. The Pixel never makes an
  authorization decision and does not represent a physical actuator.

The Node bridge binds each one-shot session to the credential read from HCE and
the selected configured resource. Completion accepts only the session identity
from the URL and a 65-byte proof. Owner, registration, resolver, `access.v1`,
`resources.v1`, and the final decision are read or derived by Node.

The Pixel bridge issues a 55-second challenge, leaving a conservative five-second
clock-skew margin beneath the holder's unchanged 60-second maximum.

## Demo startup

Use the explicitly identified Pixel serial for every command:

```powershell
$adb = 'adb'
& $adb -s <GATE_SERIAL_1> reverse tcp:8792 tcp:8792
npm run pixel-gate:bridge
& $adb -s <GATE_SERIAL_1> shell am start -n io.github.nodexbit.ethonline2026/.gate.GateReaderActivity --ez transport_only true
# After the transport-only tap succeeds, launch the full reader:
& $adb -s <GATE_SERIAL_1> shell am start -n io.github.nodexbit.ethonline2026/.gate.GateReaderActivity
```

The bridge binds only `127.0.0.1:8792`. Choose **Lab**, then perform one tap
against the Seeker. A successful fallback test reports “Verifier transport
confirmed” and “Virtual gate decision”; it must not claim controller or actuator
confirmation.

## Frozen HCE protocol

- AID: `F0454E5356324331`
- SELECT: `00 A4 04 00 08 F0 45 4E 53 56 32 43 31`
- GET_CREDENTIAL: `80 40 01 00`
- SEND_CHALLENGE: `80 10 01 00 68` + 104 bytes
- GET_STATUS: `80 20 01 00`
- GET_SIGNATURE: `80 30 01 00`
- Success status: exact trailing `90 00`

The reader Activity is intentionally absent from the normal holder navigation
and can be launched explicitly for the fallback demo.
