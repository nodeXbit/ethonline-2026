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

## Commands

```powershell
npm run ensv2:persistent:inspect
npm run ensv2:persistent:setup
npm run ensv2:access:activate
npm run ensv2:access:deactivate
npm run nfc:bridge
```

Commands that update access state submit Sepolia transactions. Inspect and the NFC bridge read public state.

## Status and security

This is a working Sepolia prototype, not production access-control security. The static NFC UID is clonable and serves only as a demo identifier; it is not cryptographic proof of possession, anti-cloning protection, or Aliro support.

The next direction is a small interactive visual demo that presents the persistent credential as a dynamic digital key whose appearance follows ENSv2 access state. Collectible presentation, dynamic metadata, loyalty, transferability, and programmable benefits are not implemented.
