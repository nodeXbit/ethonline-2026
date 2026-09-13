# Project task packet 004: READ-ONLY FINAL AUDIT.

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> READ-ONLY FINAL AUDIT.
>
> Do not edit files.
> Do not install anything.
> Do not access .env.local or .env.nfc.local.
> Do not send transactions.
> Do not commit or push.
>
> We need to decide whether the persistent-credential implementation is safe
> and operationally ready for the first real Sepolia execution.
>
> Inspect the exact current local workspace.
>
> REPORT:
>
> 1. EXACT FILE SCOPE
>
> List every created or modified file from this task.
>
> For each added file explain in one sentence why it is necessary.
>
> Explicitly identify:
>
> * shared access-record helper files
> * all seven test files
> * persistent-access.md
>
> Mark any file that is not necessary for the minimal slice as:
> OPTIONAL / REMOVE BEFORE COMMIT
>
> Do not remove anything.
>
> 2. OFFICIAL ABI / SEMANTICS
>
> Confirm against the pinned ENSv2 deployment/source:
>
> contracts-v2 commit:
> 97a57293f3b4279d94b571e678edb53ce62638f4
>
> Verify exact usage of:
>
> * PermissionedResolver.initialize(address,uint256,bytes[])
> * setData(bytes32,string,bytes)
> * data(bytes32,string)
> * UserRegistry register/getResolver/getState/getOwner/getExpiry
> * VerifiableFactory.deployProxy
> * VerifiableFactory.verifyContract(address)
>
> Confirm verifyContract returns the current implementation address and that
> the code explicitly compares that return value with:
>
> 0x9eae5c2730a7dd16bdd1dee6421a1b91e3b0365e
>
> 3. ACCESS RECORD
>
> Confirm the resolver node is exactly:
>
> namehash("cred-001.demo-access.eth")
>
> not:
>
> * tokenId
> * registry resource
> * parent namehash
> * labelhash alone.
>
> Confirm access.v1 is exactly ABI encoded:
>
> (bool active, uint64 validUntil)
>
> and malformed/non-empty unexpected byte lengths cannot silently decode to an
> authorization result.
>
> 4. ROLE BITMAP
>
> Show the exact integer/hex bitmap passed to resolver initialize.
>
> It must contain only:
>
> ROLE_SET_DATA       = 1n << 36n
> ROLE_SET_DATA_ADMIN = 1n << 164n
>
> Confirm no unrelated resolver roles are granted.
>
> 5. FIRST-WRITE PREFLIGHT
>
> Show the exact sequence before the FIRST possible write.
>
> Verify before any deployment/write:
>
> * Sepolia chainId
> * required official contract bytecode
> * parent ownership/control needed by this flow
> * current parent resolver state/provenance where applicable
> * expected UserRegistry pointer
> * credential current state
> * parent expiry constraints
> * deterministic resolver collision/provenance checks
>
> Identify the exact first possible write.
>
> 6. MULTI-TRANSACTION RECOVERY
>
> This is critical.
>
> Enumerate every interruption point in setup, for example:
>
> A. before resolver deployment
> B. after resolver deployment
> C. after access.v1 initialization
> D. after credential registration
> E. before final validation
>
> For EACH point state what exists onchain and what happens if the user simply
> runs:
>
> npm run ensv2:persistent:setup
>
> again.
>
> Classify each as:
>
> SAFE IDEMPOTENT
> SAFE BUT MANUAL RECOVERY
> UNSAFE / BLOCKER
>
> If a normal interruption can leave us requiring undocumented manual recovery,
> return REVIEW: STOP.
>
> We prefer setup to be safely rerunnable where practical before the first real
> execution.
>
> 7. ORDERING REVIEW
>
> The implementation summary says access.v1 is initialized INACTIVE before the
> credential is registered.
>
> Explain why that order was chosen.
>
> Compare it with:
>
> register credential with resolver
> → setData(INACTIVE)
>
> State which ordering is safer/recoverable for this prototype.
>
> Do not change it yet.
>
> 8. ENS_ACCESS_RESOLVER
>
> Explain exactly where ENS_ACCESS_RESOLVER is stored/read.
>
> Confirm:
>
> * it is public, not a secret
> * .env.nfc.example documents it if required by nfc:bridge
> * bridge rejects a different resolver
> * persistent inspect/setup can establish the authoritative resolver value
>
> If the user would need to manually edit an undocumented variable:
> REVIEW: STOP.
>
> 9. PARENT EXPIRY RULE
>
> Explain why the implementation requires more than 30 days remaining.
>
> Distinguish:
>
> * requirement imposed by ENSv2
> * project safety policy
> * arbitrary implementation choice.
>
> If 30 days is arbitrary, recommend the smallest justified threshold but do
> not edit.
>
> 10. BRIDGE SECURITY
>
> Confirm bridge.mjs:
>
> * never reads DEV_PRIVATE_KEY
> * never constructs walletClient
> * never calls simulateContract/writeContract
> * uses one pinned block for authorization
> * requires REGISTERED
> * requires nonzero owner
> * requires registry expiry > block timestamp
> * requires access.active == true
> * requires access.validUntil > block timestamp
> * does NOT require owner == issuer/parent owner
> * missing access.v1 => DENY
> * malformed resolver data/RPC/provenance errors => ERROR, never DENY
>
> 11. VALIDATION
>
> Run only safe/read-only validation:
>
> * syntax checks
> * local tests
> * git diff --check
> * git status --short
>
> Do not run any command requiring secrets or network writes.
>
> END WITH:
>
> # FINAL VERDICT
>
> REVIEW: PASS
>
> or
>
> REVIEW: STOP
>
> If PASS, include the exact manual commands the user should execute next,
> but DO NOT execute them.
