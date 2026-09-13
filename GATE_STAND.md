# LockENS Gate Stand

Gate Stand is the visual surface layered on the checkpointed Android Gate
Reader. It does not change the HCE protocol or move authorization out of Node.

## Persistent device profile

Each Android device stores exactly one app-private resource profile. The normal
screen has no resource selector. Configure a device explicitly once, then launch
the same Activity normally:

```powershell
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'

# Choose exactly one per device: front-door, lab, or server-room.
& $adb -s <GATE_SERIAL> shell am start -n io.github.nodexbit.ethonline2026/.gate.GateReaderActivity --es gate_profile lab

# Subsequent normal launches reuse the persisted profile.
& $adb -s <GATE_SERIAL> shell am start -n io.github.nodexbit.ethonline2026/.gate.GateReaderActivity
```

An unknown explicit profile is rejected. Missing or corrupt stored state fails
to the demo-safe Lab default. Node still validates the selected configured
resource, binds it immutably to the session, and performs all holder/ENS/policy
authorization.

## Cable-free demo transport

The three gate phones can run without USB by using ADB over a trusted private
Wi-Fi network plus one independent `adb reverse` tunnel per device. The Pixel
holder does not need wireless debugging. A machine-local, gitignored launcher
at `.runtime/start-wireless-gates.ps1` reconnects the current three endpoints,
restores all tunnels, and launches each persisted profile.

This removes the visible cables but does not remove the authoritative Node
machine: the local Node bridge and the PC must remain powered on. Moving Node to
a dedicated mini-PC is the next option for a fully self-contained installation,
but requires a separately secured LAN transport and is outside this visual-only
Gate Stand change. Legacy ADB TCP mode is for the private demo LAN only and may
need to be enabled again after a phone reboot.

## Original background assets

The build contains three original local portrait PNG assets in
`app/src/main/res/drawable-nodpi/`:

- `gate_scene_front_door.png`
- `gate_scene_lab.png`
- `gate_scene_server_room.png`

Each asset is 941 x 1672 (approximately 9:16), with a quiet central approach
and darker lower third for the security panel. The same resource is center-crop
rendered inside the gate opening, so resource-specific furniture appears as the
authoritative ALLOW animation opens the door.

No remote image URL or runtime image dependency is used.

## Door semantics

The door starts closed and stays closed while reading, signing, on transport
errors, and for every DENY. An authoritative DENY animates a large prohibition
symbol over the closed door; it does not color or move the frame. The door-open
Canvas animation is invoked only after a completed Node response with
`allowed: true`. The revealed interior is decorative and never feeds back into
authorization.

Every authoritative result or technical failure remains visible with a
ten-second countdown and then returns to a clean READY state. A new NFC attempt
during the countdown cancels the pending reset and immediately takes over the
visual surface.

While the holder app remains visible with a selected pass, it refreshes the
authoritative credential snapshot every 45 seconds. The HCE publication still
expires after 60 seconds; the refresh maintains a safety margin without
weakening the existing freshness bound.

The security panel contains only:

- Credential
- Holder
- Global Access
- Resource Access
- Proof
- Final Decision

One localhost Node process serves all three profiles. Each session has its own
opaque ID and immutable credential/resource challenge binding.
