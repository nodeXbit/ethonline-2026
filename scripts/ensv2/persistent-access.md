# Persistent credential access demo

Sepolia-only application schema v1: `access.v1` contains ABI-encoded
`(bool active, uint64 validUntil)` at `namehash("cred-001.demo-access.eth")`.
Ownership and registration expiry remain separate from access state and its deadline.

## Resolver discovery

No resolver environment variable or address copying is required. For a REGISTERED
credential, its UserRegistry resolver pointer is authoritative. Every nonzero resolver
used for access must have bytecode and pass `VerifiableFactory.verifyContract(address)`;
the returned current implementation must equal PermissionedResolverImpl.

For an AVAILABLE credential, setup verifies and reuses a nonzero parent resolver,
including a DEV `setData` simulation. An unproven parent resolver stops the operation.
If the parent resolver is zero, setup reads `factory.proxyLogic()` and predicts the
canonical OwnedResolver using the pinned factory's exact clone creation bytecode:

- `userSalt = keccak256(abi.encode(keccak256(bytes("OwnedResolver")), DEV, uint256(0)))`
- `outerSalt = keccak256(abi.encode(DEV, uint256(userSalt)))`
- Creation bytes: `0x3d604d80600a3d3981f3363d3d373d3d3d363d73` + proxyLogic (20 bytes)
  + `0x5af43d82803e903d91602b57fd5bf3` + outerSalt (32 bytes), totaling 87 bytes.
- CREATE2 uses the official factory address, outerSalt, and the hash of those bytes.

If candidate code exists, setup verifies provenance/implementation and DEV control,
then reuses it without calling deployProxy. If absent, the exact deployment simulation
must return the predicted address before its request can be submitted. Initialization
is `initialize(DEV, (1 << 36) | (1 << 164), [])`; no other roles are granted. A successful
receipt, matching ProxyDeployed event, bytecode, and factory provenance are required.

The bytecode and ABI were checked against the [pinned official factory artifact and
embedded source](https://github.com/ensdomains/contracts-v2/blob/97a57293f3b4279d94b571e678edb53ce62638f4/contracts/deployments/sepolia/VerifiableFactory.json).
The pinned contracts-v2 commit is `97a57293f3b4279d94b571e678edb53ce62638f4`.

## Setup and recovery

Setup reconstructs progress from confirmed on-chain state; it stores no local checkpoint.
Before registration it requires the expected UserRegistry, AVAILABLE credential, and
DEV parent ownership. Registry authority is simulated before deployment/record writes.

The order remains: verified resolver -> explicitly INACTIVE record -> registration.
This avoids a stale ACTIVE record granting temporary access when registration succeeds.
Missing records receive INACTIVE with the snapshot timestamp +24 hours. Existing future
INACTIVE records are preserved. ACTIVE records are forced INACTIVE before registration;
a future deadline is retained, while stale deadlines are renewed to +24 hours. Malformed
records stop execution, rather than being silently overwritten.

Registration sets DEV owner, zero subregistry, verified resolver, and credential roles zero.
Expiry targets one year capped by parent expiry. It must exceed both the current snapshot
+24-hour initial deadline and the preserved access deadline, while remaining <= parent
expiry. No arbitrary multi-day parent-expiry minimum is imposed.

For a REGISTERED credential, setup requires DEV ownership, future registry expiry, and
verified nonzero resolver. Any valid existing access record is preserved exactly,
including ACTIVE or expired access. Only a missing record is initialized INACTIVE.
A successful rerun does not deactivate an activated credential.

| Confirmed interruption | What the next setup does | Classification |
| --- | --- | --- |
| Before deployment | Derives candidate, verifies preflight, deploys if absent | SAFE IDEMPOTENT |
| After deployment | Rediscovers and verifies proxy, initializes missing access | SAFE IDEMPOTENT |
| After inactive initialization | Preserves future inactive record and registers | SAFE IDEMPOTENT |
| After registration, before validation | Discovers credential resolver and returns current state | SAFE IDEMPOTENT |
| After final validation | Validates and returns without writes, preserving ACTIVE | SAFE IDEMPOTENT |

Before every setup write, latest/pending DEV transaction counts must agree, including
another check after simulation. Otherwise setup tells the user to wait for the pending
result before rerunning. It never replaces transactions. For a process interruption,
wait for submitted transactions to resolve, then run the same setup command again.
Confirmed failures roll back that transaction; setup resumes from preceding confirmed state.

## Manual demo

Use the existing DEV and NFC configuration. No new environment variables are needed.
Setup/activate/deactivate use the existing signing configuration; inspect and bridge
use the existing NFC configuration and public clients only.

```powershell
npm run ensv2:persistent:setup
npm run ensv2:persistent:inspect
npm run nfc:bridge
# Scan UID 91:2D:E3:06: DENY after fresh setup.
npm run ensv2:access:activate
npm run nfc:bridge
# Scan: ALLOW.
npm run ensv2:persistent:setup
# Idempotency check: must preserve ACTIVE.
npm run ensv2:access:deactivate
npm run nfc:bridge
# Scan: DENY.
npm run ensv2:persistent:inspect
```

Compare final REGISTERED owner, tokenId, resource, resolver, and registry expiry with
initial inspection. The bridge runs once per scan. If resuming an already ACTIVE demo,
setup intentionally preserves ACTIVE. Do not use the legacy revoke command for this demo.

## Authorization and inspect

Inspect requires no signing connection or prior resolver knowledge. It displays credential
state and discovered resolver/access information; AVAILABLE means not currently registered.

Authorization reads use one pinned block. Registry validity is checked first: status must
be REGISTERED, owner nonzero, and expiry after the block timestamp. Otherwise return DENY
without reading the resolver. For a valid registration, zero resolver or empty access record
is DENY. Nonzero resolver without code, failed provenance, wrong implementation, malformed
data, and RPC failures are ERROR. Valid access allows only when active and its deadline is
strictly future. Credential owner need not equal parent owner.

## Local validation and limits

```powershell
node --test --test-isolation=none scripts/ensv2/access-record.test.mjs
```

Tests execute the setup state machine against a local mocked chain, including interruption
and retry after each confirmed write, prediction mismatch, pending guards, state preservation,
bridge errors/expiry, and safe failure reporting. They send no transactions and use no
environment files or keys. Failures report their stage, operation, public custom error/RPC
code, and any submitted transaction hash while redacting RPC URLs.

Setup is not atomic. Concurrent external changes, reorgs, or permissions/expiry changes can
stop verification; no automatic rollback is attempted. Run one setup process at a time.
The pending guard depends on the RPC's mempool visibility and cannot prevent a simultaneous
submission elsewhere. Unexpected proxies, malformed data, changed ownership, or insufficient
expiry deliberately stop; these are outside normal confirmed interruption recovery.
Real Sepolia and physical NFC validation remain manual.
