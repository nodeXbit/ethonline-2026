# LockENS final demo state

Finalized on Sepolia on 2026-09-13. The original three-pass plan was expanded by explicit operator approval to add four artwork-backed showcase passes to one existing unused wallet.

## Public wallets

| Role | Address |
| --- | --- |
| Issuer | `0xFa90e8301A22833B74378C5fA3a7c120Ac512685` |
| Staff holder | `0xB7Ba9f32c8358a6528E6583f11b83221006b2Cd0` |
| Visitor holder | `0x8Cc9425e3d848Dd105B4311940d19a3A2b722824` |
| Contractor holder | `0x934Af0B974e7023597f1c24C51534E194Ab086dB` |
| Showcase holder | `0xd894da9f20d8c10DE23cd274dD376258b815341D` |

All holders are existing embedded Ethereum wallets. Holder wallets need no Sepolia ETH because they sign holder proofs but do not submit the issuance transactions.

## Final policy matrix

All seven passes are registered through `2027-03-31 23:59 Europe/Madrid` under `keys.demo-access.eth`.

| Pass | Global | Front Door | Lab | Server Room | Transferable | Description |
| --- | --- | --- | --- | --- | --- | --- |
| `staff-demo.keys.demo-access.eth` | Allowed | Yes | Yes | Yes | No | Staff Access Pass |
| `visitor-demo.keys.demo-access.eth` | Allowed | Yes | No | No | Yes | Visitor Pass |
| `contractor-demo.keys.demo-access.eth` | Suspended | No | Yes | No | No | Maintenance Contractor Pass |
| `showcase-all.keys.demo-access.eth` | Allowed | Yes | Yes | Yes | No | Full Access Showcase |
| `showcase-front.keys.demo-access.eth` | Allowed | Yes | No | No | Yes | Transferable Front Door Pass |
| `showcase-suspended.keys.demo-access.eth` | Suspended | No | Yes | No | No | Suspended-Lab-Pass |
| `showcase-server.keys.demo-access.eth` | Allowed | No | No | Yes | No | Server-Room-Only-Pass |

`READY` means the credential is coherently registered and configured. It does not mean the policy is `Allowed`; both suspended passes intentionally reach a ready but denied state.

## Public Sepolia transactions

| Pass | Registration | Configuration |
| --- | --- | --- |
| Staff | `0xb6adf815d86f44012886e8710423055708249309bd430e9d6ee6a3f8ab44d988` | `0xcbdb0034d79a5bba6a8eafbce2b930cdf1e656ed016cd2b2dd95a995e520b989` |
| Visitor | `0xe08b2b8e4acd20bb3bd31472b4bc957e037f88b675fa5a071a1576f10654893f` | `0xb2e14f963a1612f10297b564504a230d032f459e313387e6b2014b8f1f4f2553` |
| Contractor | `0x1357cec835d264aedd090018901f1367b5c461b3994186e885be6831599f2915` | `0x1fe29f63525a9be64befb1aac3235d48aaabf4c771bd8633c5f28e74b0b180c3` |
| Showcase all | `0x1e8f6f218bf2cb2f8fcbe5eedcec682769f9840887359928c91bead4ef919e44` | `0xb64e68194b25a586703b6e1b330774636422860c3d2e211920858aee6e1b3d6e` |
| Showcase front | `0x02f55cb99769a6a7a8ab4dff81eba6f196a96318c793f05b56d06475bb66322d` | `0x8a6b900a678087f5e6920ec9d5f94c3140b7be31bf58fa305c03c0ac24598895` |
| Showcase suspended | `0xd373058c254eaaecb899bc4cbe4e60b873e6c2f05fbfb4f55c234546300c30c7` | `0xc799ac93021a7143d6b05656eb48bb52a392a20fb0263ab49fa5912b7e703eea` |
| Showcase server | `0xcff6956d6b937157a695d302005cfae541dde1d746403f4b08e8ce1a3743c3b8` | `0x4a4f20f1bd7fda02d8fc5f1d752512b6f66317530a148bc11ab1c17a5925f4f4` |

All 14 journaled operations are confirmed. The unused walkthrough label `video-preview-20260913` produced no transaction.

## Artwork

The four showcase cards were refreshed from Sepolia and visually verified together in My Keys. Each rendered its remote HTTPS image rather than relying on the deterministic fallback.

Complete creator, license, modification, and packaged Gate Stand asset provenance is recorded in [ATTRIBUTIONS.md](ATTRIBUTIONS.md).

| Pass | Source and license |
| --- | --- |
| Showcase all | [Standard lock key](https://commons.wikimedia.org/wiki/File:Standard-lock-key.jpg), public domain |
| Showcase front | [Front door, Susan Dennis](https://commons.wikimedia.org/wiki/File:Front_door_(23200352782).jpg), Public Domain Mark |
| Showcase suspended | [Stop sign](https://commons.wikimedia.org/wiki/File:Stop_sign.png), public domain |
| Showcase server | [Server racks, Jemimus](https://commons.wikimedia.org/wiki/File:Server_racks,_one_empty_-_IMG_3636.jpg), CC BY 2.0 |

## Physical validation

The operator confirmed that all final credentials behaved correctly on the physical Android gate readers. The release matrix passed:

| Test | Result | Door behavior |
| --- | --- | --- |
| Staff -> Lab | `ACCESS GRANTED` | Virtual gate animation opened only after authoritative allow |
| Staff -> Server Room | `ACCESS GRANTED` | Virtual gate animation opened only after authoritative allow |
| Visitor -> Lab | `RESOURCE_NOT_ALLOWED` | Remained closed |
| Visitor -> Front Door | `ACCESS GRANTED` | Virtual gate animation opened only after authoritative allow |
| Contractor -> Lab | `ACCESS_SUSPENDED` | Remained closed |

The four showcase passes were also exercised successfully. Wallet switching, pass discovery, stacked-card presentation and HCE selection/invalidation were healthy. No stale pass was reported. A missing local bridge/tunnel was detected and restored before the final all-credential run; no manual recovery was needed during the confirmed run. Interaction time was not formally timed.

`NODE_UNAVAILABLE` is a fail-closed transport error: it cannot grant access and is not presented as a policy decision.

## Walkthrough and validation status

- Create walkthrough: READY. Fresh preview label, recipient, expiry, access, resources, transferability, description, artwork and derived ENS name were shown through Review; Create was not pressed.
- Manage walkthrough: READY. The final Staff pass showed Suspend, access validity, resources, artwork/description and registration-extension surfaces; no management transaction was submitted.
- Unplanned writes: none. Four additional showcase passes and their eight transactions were explicitly approved by the operator.
- Node: 238/238 tests pass with sequential test execution.
- Android: `testDebugUnitTest` passes.
- Android assembly: `assembleDebug` passes.
- Firmware: dynamic and legacy ESP32-S3 sketches compile.
- Physical readers: Front Door, Lab and Server Room all reached `READY - TAP LOCKENS PASS` with the authoritative Node bridge running.

## Feature freeze

**FEATURE FREEZE: ACTIVE**

Until submission, permitted work is limited to submission-blocking bug fixes, security/privacy sanitation, README/documentation, screenshots/diagrams, and video/submission materials.

Do not add product features, sponsor integrations, protocol changes, gate architecture, wallet features, Solidity, standalone Android authorization, backend/cloud services, or changes to PN532 behavior. Preserve historical `staff-001.keys.demo-access.eth` unchanged.
