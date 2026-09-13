# Project task packet 002: TASK — Implement the smallest physical NFC → ENSv2 authorization vertical slice.

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Implement the smallest physical NFC → ENSv2 authorization vertical slice.
>
> WHY
>
> The hardware path and ENSv2 path are independently verified.
>
> We now need to prove:
>
> physical ISO14443A tag
> → ESP32-S3 + PN532
> → serial UID
> → PC-side mapping
> → real ENSv2 read
> → ALLOW / DENY.
>
> There is one operational gap discovered during inspection:
>
> The existing lifecycle.mjs always registers and then unregisters cred-001.
> For the physical demo we need explicit issue/revoke controls so the same
> NFC bridge can observe both states independently.
>
> CONTEXT
>
> Repository:
> nodeXbit/ethonline-2026
>
> Current branch:
> main
>
> Expected initial state:
> clean at commit 891ab0d.
>
> Verified project state:
>
> ENS parent:
> demo-access.eth
>
> Verified UserRegistry:
> 0x2d249472B83A453086254Acd8a42913D8e45a2Fd
>
> Credential label:
> cred-001
>
> Sepolia:
> chainId 11155111
>
> Existing firmware output at 115200 baud:
>
> ISO14443A tag detected; UID=XX:XX:XX:XX (4 bytes)
>
> or:
>
> ISO14443A tag detected; UID=XX:XX:XX:XX:XX:XX:XX (7 bytes)
>
> Firmware emits repeatedly every ~1.5s if a tag remains present.
>
> IMPORTANT SECURITY BOUNDARY
>
> The static NFC UID is ONLY a prototype/demo identifier.
>
> It is clonable.
>
> Do not describe or implement it as secure proof of credential ownership.
>
> Aliro/challenge-response/mobile HCE are OUT OF SCOPE.
>
> SCOPE
>
> Create:
>
> 1. scripts/nfc/bridge.mjs
> 2. scripts/ensv2/credential-state.mjs
> 3. .env.nfc.example
>
> Modify:
>
> 4. package.json
>
> Do not modify existing lifecycle.mjs unless an actual blocker makes it
> unavoidable. Prefer leaving the already validated lifecycle evidence intact.
>
> Do not modify package-lock.json manually. It will be updated by the user
> through npm install after code review.
>
> DEPENDENCY
>
> Add:
>
> serialport ^13.0.0
>
> Do NOT install it during this task.
>
> PACKAGE SCRIPTS
>
> Add:
>
> "nfc:bridge":
> node --env-file=.env.nfc.local scripts/nfc/bridge.mjs
>
> "ensv2:issue":
> node --env-file=.env.local scripts/ensv2/credential-state.mjs issue
>
> "ensv2:revoke":
> node --env-file=.env.local scripts/ensv2/credential-state.mjs revoke
>
> ENVIRONMENT SEPARATION
>
> .env.nfc.example must contain only:
>
> ENS_PARENT_NAME=demo-access.eth
> SEPOLIA_RPC_URL=
> NFC_SERIAL_PORT=
> NFC_DEMO_UID=
>
> The NFC bridge MUST NOT require, read, import or log DEV_PRIVATE_KEY.
>
> BRIDGE REQUIREMENTS
>
> scripts/nfc/bridge.mjs must:
>
> 1. Be read-only with respect to Ethereum.
>
> 2. Never construct a wallet client.
>
> 3. Never import or read DEV_PRIVATE_KEY.
>
> 4. Open NFC_SERIAL_PORT at 115200 baud using serialport.
>
> 5. Read newline-delimited data.
>
> 6. Ignore startup/debug lines that do not match the NFC detection format.
>
> 7. Match exactly the existing firmware detection pattern:
>
>    ISO14443A tag detected; UID=<UID> (4 bytes)
>
>    or:
>
>    ISO14443A tag detected; UID=<UID> (7 bytes)
>
> 8. Normalize UIDs to uppercase colon-separated format.
>
> 9. Accept only 4-byte or 7-byte UIDs.
>
> 10. Compare the detected normalized UID with NFC_DEMO_UID.
>
> 11. If the UID does not match:
>
>     * print the normalized detected UID as public debugging evidence
>     * print exactly:
>       AUTHORIZATION: DENY
>     * close serial port
>     * exit successfully
>
>     No Ethereum RPC call is required for an unmapped UID.
>
> 12. If the UID matches, map it to:
>
>     cred-001
>
> 13. Create only a viem public client on Sepolia.
>
> 14. Require chainId === 11155111.
>
> 15. At one pinned block:
>
>     * derive the direct .eth parent label from ENS_PARENT_NAME
>
>     * find its token ID
>
>     * read its current owner
>
>     * read getSubregistry(parentLabel)
>
>     * require that pointer equals:
>
>       0x2d249472B83A453086254Acd8a42913D8e45a2Fd
>
>     * verify that address has bytecode
>
>     * resolve cred-001 with findTokenId
>
>     * read getState
>
>     * read getOwner
>
>     * read getExpiry
>
>     * use the pinned block timestamp
>
> 16. Authorization must be computed only from real reads:
>
>     ALLOW iff:
>
>     * status == REGISTERED
>     * credential owner == current parent owner
>     * expiry > pinned block timestamp
>
>     otherwise:
>     DENY
>
> 17. Print useful public evidence:
>
>     * detected normalized UID
>     * mapped credential name
>     * block number
>     * registry
>     * tokenId
>     * status
>     * owner
>     * expiry
>
> 18. Then print exactly one:
>     AUTHORIZATION: ALLOW
>     or
>     AUTHORIZATION: DENY
>
> 19. Close the serial port and exit after the first authorization decision.
>
> 20. Serial errors, RPC errors, malformed configuration, unexpected registry
>     pointer or malformed ENS responses MUST:
>
>     * produce an error
>     * exit nonzero
>     * NOT print AUTHORIZATION: DENY
>
> A technical failure is not evidence of denied authorization.
>
> CREDENTIAL STATE CONTROLLER
>
> Create:
>
> scripts/ensv2/credential-state.mjs
>
> Supported actions:
>
> issue
> revoke
>
> It MAY use DEV_PRIVATE_KEY because this script performs deliberate writes.
>
> Reuse the existing ENSv2 contracts/ABI and patterns rather than inventing a
> new integration.
>
> For BOTH actions:
>
> * require Sepolia chainId 11155111
> * derive account from DEV_PRIVATE_KEY
> * never print the private key
> * resolve demo-access.eth's current UserRegistry
> * require it equals:
>   0x2d249472B83A453086254Acd8a42913D8e45a2Fd
> * verify registry bytecode
> * use simulateContract before writes
> * wait for transaction receipt
> * require receipt success
> * print transaction hash and block number
>
> issue:
>
> * label = cred-001
> * require current status AVAILABLE
> * register:
>   owner = DEV wallet
>   subregistry = zero address
>   resolver = zero address
>   roleBitmap = 0n
>   expiry = latest block timestamp + 24 hours
> * read back state
> * assert:
>   REGISTERED
>   owner == DEV wallet
>   expiry > block timestamp
> * print:
>   CREDENTIAL STATE: ACTIVE
>
> revoke:
>
> * resolve current tokenId from label
> * require current status REGISTERED
> * unregister(tokenId)
> * read the credential again using the stable label/current token ID
> * assert:
>   AVAILABLE
>   owner == zero address
> * print:
>   CREDENTIAL STATE: REVOKED
>
> Do not fake state transitions.
>
> NO-TOUCH
>
> * firmware/
> * PROJECT.md
> * STATUS.md
> * WORKLOG.md
> * README.md
> * existing ENSv2 deployment/setup architecture
> * UI
> * Android
> * iOS
> * account abstraction
> * Aliro
> * metadata
> * loyalty
> * custom cryptography
> * custom Solidity
>
> SECURITY
>
> * Do not access .env.local.
> * Do not create .env.local.
> * Do not create .env.nfc.local.
> * Do not request or print secrets.
> * Do not run transaction scripts.
> * Do not flash firmware.
> * Do not commit.
> * Do not push.
> * Do not install dependencies.
>
> VALIDATION
>
> Because serialport is not installed yet:
>
> 1. Run syntax checks on every created/modified .mjs file.
> 2. Validate parser/normalizer logic using local pure-function test inputs
>    without opening hardware.
> 3. Verify bridge.mjs contains no DEV_PRIVATE_KEY reference.
> 4. Verify bridge.mjs creates no wallet client.
> 5. Verify credential-state.mjs performs no write before its required
>    preflight checks.
> 6. Inspect git diff.
> 7. Report git status --short.
> 8. Confirm firmware is untouched.
> 9. Confirm .env.local was not accessed.
> 10. Confirm no transaction was sent.
>
> RETURN
>
> # IMPLEMENTATION RESULT
>
> ## FILES
>
> ## BRIDGE FLOW
>
> ## ISSUE/REVOKE FLOW
>
> ## VALIDATION
>
> ## MANUAL STEPS
>
> ## RISKS
>
> End exactly:
>
> REVIEW: PASS
>
> or
>
> REVIEW: STOP
