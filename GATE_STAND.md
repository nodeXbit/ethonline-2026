# LockENS Gate Stand

Gate Stand is the visual surface layered on the checkpointed Android Gate
Reader. It does not change the HCE protocol or move authorization out of Node.

## Persistent device profile

Each Android device stores exactly one app-private resource profile. The normal
screen has no resource selector. Configure a device explicitly once, then launch
the same Activity normally:

```powershell
$adb = 'adb'

# Choose exactly one per device: front-door, lab, or server-room.
& $adb -s <GATE_SERIAL> shell am start -n io.github.nodexbit.ethonline2026/.gate.GateReaderActivity --es gate_profile lab

# Subsequent normal launches reuse the persisted profile.
& $adb -s <GATE_SERIAL> shell am start -n io.github.nodexbit.ethonline2026/.gate.GateReaderActivity
```

An unknown explicit profile is rejected. Missing or corrupt stored state fails
to the demo-safe Lab default. Node still validates the selected configured
resource, binds it immutably to the session, and performs all holder/ENS/policy
authorization.

## Original background asset contract

The build currently contains lightweight local gradient placeholders. Replace
each placeholder XML with an original WebP using the same resource basename in
`app/src/main/res/drawable-nodpi/`:

- `gate_scene_front_door.webp`
- `gate_scene_lab.webp`
- `gate_scene_server_room.webp`

Recommended canvas: portrait 9:16, at least 1440 × 2560, with a quiet central
area for the rendered door and darker lower third for the security panel. Delete
the same-named XML placeholder when adding each WebP; Android cannot compile two
resources with the same basename.

No remote image URL or runtime image dependency is used.

## Door semantics

The door starts closed and stays closed while reading, signing, on transport
errors, and for every DENY. An authoritative DENY animates a large prohibition
symbol over the closed door; it does not color or move the frame. The door-open
Canvas animation is invoked only after a completed Node response with
`allowed: true`. The revealed interior is decorative and never feeds back into
authorization.

The security panel contains only:

- Credential
- Holder
- Registration
- Global Access
- Resource Access
- Proof
- Final Decision

One localhost Node process serves all three profiles. Each session has its own
opaque ID and immutable credential/resource challenge binding.
