# ETHOnline 2026 — Persistent Physical Credential

A Sepolia prototype connecting a persistent ENSv2 credential to a physical NFC interaction. The credential remains owned and REGISTERED while its independent access state changes, so revoking entry does not burn or unregister the credential.

## Architecture

```text
Physical NFC tag (UID 91:2D:E3:06)
              │
              ▼
      ESP32-S3 + PN532
              │ serial UID
              ▼
        Node.js bridge
              │ read-only Sepolia lookup
              ▼
demo-access.eth → UserRegistry → cred-001.demo-access.eth
                       │                    │
                       │ ownership          └─ PermissionedResolver access.v1
                       ▼                                  │
                   REGISTERED                      ALLOW / DENY
```

UserRegistry provides persistent credential ownership. PermissionedResolver `access.v1` independently controls physical authorization.

The validated interactive path is:

```text
Browser digital key -> local Node server -> ENSv2 PermissionedResolver access.v1
                                              |
                              persistent UserRegistry credential
```

The separate physical NFC bridge reads the same authoritative ENSv2 state and produces the corresponding `ALLOW` or `DENY` decision.

## Interactive demo

Start the local browser UI:

```powershell
npm run demo
```

Open `http://127.0.0.1:4173`. The single credential card remains visible while one-click activation and deactivation move it between `INACTIVE / DENY` and `ACTIVE / ALLOW`. Pending/recovery feedback prevents repeated actions, and each completed transition refreshes from real ENSv2 reads rather than browser-local state.

Run `npm run nfc:bridge` separately in a terminal and present the physical tag to prove the same lifecycle physically. The NFC bridge remains separate from the browser.

For demo reliability, a dedicated Sepolia endpoint may be supplied locally through `SEPOLIA_RPC_URL`. Never commit or publish the endpoint or its API key; the public fallback remains supported.

## Verified physical flow

The same physical tag was presented throughout:

```text
INACTIVE → DENY
ACTIVE   → ALLOW
INACTIVE → DENY
```

The credential remained REGISTERED with the same owner, tokenId, resolver, and registry expiry. Rerunning persistent setup while ACTIVE was also verified to preserve the active state without another transaction.

Sepolia deployment:

- Parent: `demo-access.eth`
- Credential: `cred-001.demo-access.eth`
- UserRegistry: `0x2d249472B83A453086254Acd8a42913D8e45a2Fd`
- PermissionedResolver: `0x0B723c0C2170F2ea508F2f4e3e122C6c0782744C`
- Access record: `data(namehash("cred-001.demo-access.eth"), "access.v1")`
- Access schema: `(bool active, uint64 validUntil)`

## Commands

```powershell
npm run demo
npm run ensv2:persistent:inspect
npm run ensv2:persistent:setup
npm run ensv2:access:activate
npm run ensv2:access:deactivate
npm run nfc:bridge
```

The browser activate/deactivate actions and corresponding CLI commands submit Sepolia transactions. Inspect and the NFC bridge read public state.

## Status and security

This is a working Sepolia prototype, not production access-control security. The static NFC UID is clonable and serves only as a demo identifier; it is not cryptographic proof of possession, anti-cloning protection, or Aliro support.

The interactive browser and physical NFC lifecycle are validated on Sepolia. This remains a prototype, not production physical security. Aliro, account abstraction, collectible presentation, NFT/dynamic metadata, loyalty, transferability, and programmable benefits are not implemented.
