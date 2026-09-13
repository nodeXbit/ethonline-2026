# Project task packet 003: TASK — Implement the smallest persistent ENSv2 credential access lifecycle.

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Implement the smallest persistent ENSv2 credential access lifecycle.
>
> WHY
>
> The previous physical demo proved:
>
> NFC → ENSv2 → DENY → ALLOW → DENY
>
> but used unregister() to revoke access, which burns/deactivates the ENSv2
> credential.
>
> The product requires ownership lifetime and access lifetime to be separate.
>
> We now need to prove:
>
> credential remains REGISTERED and owned
> → access record INACTIVE → DENY
> → same credential access ACTIVE → ALLOW
> → same credential access INACTIVE → DENY
> → credential remains REGISTERED with same owner/token/resolver.
>
> CONTEXT
>
> Repo:
> nodeXbit/ethonline-2026
>
> Expected worktree:
> clean main synchronized with origin/main.
>
> Existing:
>
> parent:
> demo-access.eth
>
> UserRegistry:
> 0x2d249472B83A453086254Acd8a42913D8e45a2Fd
>
> physical NFC UID:
> 91:2D:E3:06
>
> credential label:
> cred-001
>
> Current credential state:
> AVAILABLE after the previous test's unregister().
>
> Current official ENSv2 Sepolia Beta:
>
> PermissionedResolverImpl:
> 0x9eae5c2730a7dd16bdd1dee6421a1b91e3b0365e
>
> VerifiableFactory:
> 0x10dc6333cdfe1fcef624c6e0a8221b91804cd7ef
>
> ETHRegistry:
> 0xbdc85dd5b15d7ecb354cd7cb6f2c50b4f2c4f0e2
>
> ACCESS RECORD DESIGN
>
> PermissionedResolver data record:
>
> key:
> access.v1
>
> value:
> ABI encoding of:
>
> (bool active, uint64 validUntil)
>
> Use viem encodeAbiParameters/decodeAbiParameters.
>
> This is application-specific schema v1.
>
> RESOLVER PERMISSIONS
>
> ROLE_SET_DATA:
> 1n << 36n
>
> ROLE_SET_DATA_ADMIN:
> 1n << 164n
>
> If a new PermissionedResolver proxy must be deployed, initialize the DEV
> wallet with exactly:
>
> ROLE_SET_DATA | ROLE_SET_DATA_ADMIN
>
> Do not grant unrelated resolver roles.
>
> RESOLVER DISCOVERY / DEPLOYMENT
>
> Before deploying anything:
>
> 1. Inspect whether demo-access.eth already points to a nonzero resolver.
>
> 2. If it does:
>
>    * require deployed bytecode
>    * verify it through VerifiableFactory.verifyContract(
>      resolver,
>      PermissionedResolverImpl
>      )
>    * verify/simulate that DEV wallet can setData()
>    * reuse it if valid.
>
> 3. If there is no usable existing resolver:
>
>    * use the official VerifiableFactory
>    * canonical resolver salt scheme:
>      keccak256(abi.encode(
>      keccak256("OwnedResolver"),
>      DEV wallet,
>      uint256(0)
>      ))
>    * initialize(admin, roleBitmap, [])
>    * simulate before write
>    * deploy the proxy
>    * verify receipt, ProxyDeployed event, bytecode and factory provenance.
>
> 4. If canonical deployment collides with an unknown existing proxy or
>    provenance/control cannot be proven:
>    STOP. Do not guess or deploy a random alternative.
>
> PERSISTENT CREDENTIAL
>
> Create a new script:
>
> scripts/ensv2/persistent-access.mjs
>
> Supported commands:
>
> setup
> activate
> deactivate
> inspect
>
> setup:
>
> * require credential currently AVAILABLE
> * establish/reuse the PermissionedResolver
> * register cred-001 in the existing UserRegistry
> * owner = DEV wallet
> * subregistry = zero
> * resolver = PermissionedResolver proxy
> * credential roleBitmap = 0n
> * registry expiry must be long-lived relative to access state:
>   use a safe future expiry that does not exceed the .eth parent's expiry
> * initialize access.v1 explicitly as:
>   active = false
>   validUntil = latest block timestamp + 24 hours
> * read everything back
> * print:
>   PERSISTENT CREDENTIAL: READY
>   ACCESS STATE: INACTIVE
>
> activate:
>
> * require credential REGISTERED and unexpired
> * require expected resolver pointer
> * read/decode access.v1
> * preserve validUntil if it is still future; otherwise set a new +24h one
> * write:
>   active = true
> * read back
> * print:
>   ACCESS STATE: ACTIVE
>
> deactivate:
>
> * require credential REGISTERED and unexpired
> * require expected resolver pointer
> * read/decode access.v1
> * preserve validUntil
> * write:
>   active = false
> * read back
> * print:
>   ACCESS STATE: INACTIVE
>
> inspect:
>
> READ ONLY.
>
> Print:
>
> * credential status
> * owner
> * registry expiry
> * tokenId
> * resource
> * resolver
> * access.active
> * access.validUntil
> * current block timestamp
> * computed authorization
>
> It must never write.
>
> BRIDGE CHANGE
>
> Modify scripts/nfc/bridge.mjs.
>
> Keep the existing serial/NFC logic.
>
> For a mapped UID:
>
> 1. Resolve current UserRegistry as before.
> 2. Read credential state at one pinned block.
> 3. Require credential resolver is nonzero and has bytecode.
> 4. Read:
>    data(namehash("cred-001.demo-access.eth"), "access.v1")
> 5. Decode:
>    (bool active, uint64 validUntil)
> 6. ALLOW iff:
>
>    * registry status == REGISTERED
>    * credential owner != zero address
>    * registry expiry > pinned block timestamp
>    * access.active == true
>    * access.validUntil > pinned block timestamp
> 7. Do NOT require credential owner == parent owner.
> 8. Print:
>
>    * owner
>    * registry expiry
>    * tokenId
>    * resolver
>    * access.active
>    * access.validUntil
>    * AUTHORIZATION result
> 9. Missing access.v1 may be treated as DENY / not configured.
> 10. Malformed resolver data, RPC failure or unexpected resolver pointer is
>     an ERROR, not DENY.
>
> PACKAGE SCRIPTS
>
> Add:
>
> ensv2:persistent:setup
> ensv2:access:activate
> ensv2:access:deactivate
> ensv2:persistent:inspect
>
> NO-TOUCH
>
> * firmware/
> * previous validated lifecycle.mjs
> * previous credential-state.mjs
> * hardware configuration
> * Android/iOS
> * Aliro
> * account abstraction
> * UI
> * metadata renderer
> * loyalty
> * transfer implementation
> * custom Solidity
>
> SECURITY
>
> * Never print/read private key contents.
> * Never ask for secrets.
> * No mainnet.
> * No custom crypto.
> * Do not send transactions during implementation.
> * Do not commit or push.
> * Do not access .env.local during implementation.
>
> VALIDATION
>
> Without sending transactions:
>
> * syntax checks
> * unit-test access.v1 encode/decode with pure local values
> * verify bridge has no private-key/wallet-client/write references
> * inspect all write preflight ordering
> * validate exact PermissionedResolver ABI fragments against current
>   official ENSv2 source/docs
> * git diff --check
> * confirm firmware untouched
>
> RETURN
>
> # IMPLEMENTATION RESULT
>
> ## FILES
>
> ## RESOLVER FLOW
>
> ## PERSISTENT CREDENTIAL FLOW
>
> ## BRIDGE AUTHORIZATION
>
> ## VALIDATION
>
> ## MANUAL STEPS
>
> ## RISKS
>
> End:
>
> REVIEW: PASS
>
> or
>
> REVIEW: STOP
