# Project task packet 001: TASK — Implement the first minimal ENSv2 Sepolia vertical slice for the

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: private key
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Implement the first minimal ENSv2 Sepolia vertical slice for the
> nodeXbit/ethonline-2026 repository.
>
> WHY
>
> We need observable evidence that ENSv2 is authoritative for access
> credential state:
>
> WRITE real ENSv2 state
> → READ real ENSv2 state = ALLOW
> → revoke/unregister
> → READ changed ENSv2 state = DENY.
>
> CONTEXT
>
> - Branch: main.
> - Expected working tree: clean.
> - Node.js 24 is installed.
> - Primary sponsor: ENS / ENSv2.
> - Hardware/NFC is already validated but is OUT OF SCOPE here.
> - ENSv2 Beta is on Sepolia, chainId 11155111.
> - Use direct ENSv2 contract writes via viem.
> - Do NOT use ENSv1 patterns.
> - Do NOT use ENS CLI.
> - Do NOT deploy custom Solidity.
>
> Current official Sepolia addresses:
>
> ETHRegistry:
> 0xbdc85dd5b15d7ecb354cd7cb6f2c50b4f2c4f0e2
>
> UserRegistryImpl:
> 0x624a25d67b59d587752ebec8dded8827dae52050
>
> VerifiableFactory:
> 0x10dc6333cdfe1fcef624c6e0a8221b91804cd7ef
>
> Read AGENTS.md, STATUS.md and PROJECT.md before making edits.
>
> SCOPE
>
> Create only:
>
> package.json
> .env.example
> scripts/ensv2/contracts.mjs
> scripts/ensv2/setup.mjs
> scripts/ensv2/lifecycle.mjs
>
> package.json:
> - ES modules.
> - viem >= 2.35.0 as the only runtime dependency.
> - npm script ensv2:setup
> - npm script ensv2:lifecycle
> - use Node 24 native --env-file=.env.local
> - do not add dotenv.
>
> .env.example:
>
> ENS_PARENT_NAME=
> DEV_PRIVATE_KEY=
> [REDACTED: private key]
>
> SEPOLIA_RPC_URL should be optional in code; use viem's Sepolia HTTP
> transport fallback if not supplied.
>
> contracts.mjs:
> - Pin/document the current ENSv2 Sepolia Beta addresses above.
> - Include only minimal verified ABI fragments.
> - Include comments that these addresses are beta and must be rechecked
>   before future use.
>
> setup.mjs must:
>
> 1. Guard chainId === 11155111.
> 2. Derive the account from DEV_PRIVATE_KEY.
> 3. Never print DEV_PRIVATE_KEY.
> 4. Verify bytecode exists at ETHRegistry, UserRegistryImpl and
>    VerifiableFactory.
> 5. Validate ENS_PARENT_NAME is a direct .eth 2LD.
> 6. Read the parent owner from ETHRegistry and require it equals the
>    DEV wallet.
> 7. Read the parent's current subregistry.
> 8. STOP without writing if a non-zero unexpected subregistry already
>    exists.
> 9. Deploy a UserRegistry proxy through VerifiableFactory.deployProxy(
>    UserRegistryImpl, salt, initializeData
>    ).
> 10. initialize(rootAccount, roleBitmap) with the DEV wallet as rootAccount.
> 11. Grant only the minimum root operational roles required for this slice:
>     ROLE_REGISTRAR = 1n << 0n
>     ROLE_UNREGISTER = 1n << 12n
> 12. Connect the new UserRegistry to the parent with
>     ETHRegistry.setSubregistry(...).
> 13. Read getSubregistry again and assert it equals the deployed proxy.
> 14. Print public evidence only:
>     account
>     chainId
>     parent name
>     proxy address
>     transaction hashes
>     block numbers.
>
> Use simulateContract before writes where useful.
> Wait for receipts and assert receipt.status === success.
>
> lifecycle.mjs must:
>
> 1. Read the UserRegistry from the parent's current getSubregistry().
> 2. STOP if it is zero.
> 3. Use label "cred-001".
> 4. Require it is AVAILABLE before starting.
> 5. Register:
>    owner = DEV wallet
>    subregistry = zero address
>    resolver = zero address
>    credential roleBitmap = 0n
>    expiry = latest block timestamp + 24 hours
> 6. Wait for the register receipt.
> 7. Read:
>    findTokenId("cred-001")
>    getState(...)
>    getOwner(...)
>    getExpiry(...)
>    latest block timestamp
> 8. Assert:
>    status == REGISTERED
>    owner == DEV wallet
>    expiry > block timestamp
> 9. Compute authorization only from those real reads and print:
>    AUTHORIZATION: ALLOW
> 10. Call unregister(tokenId).
> 11. Wait for receipt.
> 12. Read state/owner/expiry again.
> 13. Assert:
>     status == AVAILABLE
>     owner == zero address
> 14. Print:
>     AUTHORIZATION: DENY
> 15. Print both transaction hashes and enough public values to use as
>     sponsor evidence.
>
> NO-TOUCH
>
> - firmware/
> - NFC
> - Android
> - iOS
> - UI
> - account abstraction
> - metadata
> - resolver records
> - NFTs/branding
> - custom Solidity
> - other sponsors
> - PROJECT.md
> - STATUS.md
> - WORKLOG.md for this task
> - README.md
>
> SECURITY
>
> - Never create .env.local.
> - Never read or print .env.local.
> - Never print a private key.
> - Never ask me to paste a private key.
> - Never hardcode secrets.
> - Never commit or push.
> - Never send a transaction.
> - Do not run the transaction scripts yourself.
>
> VALIDATION
>
> Before finishing:
> - inspect git diff
> - run syntax checks that do not require secrets
> - ensure firmware is untouched
> - ensure .env.local is still absent
> - report every created/modified file
> - explain any ABI/function assumption
>
> ACCEPTANCE CRITERIA
>
> The implementation should be ready for me to run manually:
>
> npm install
> npm run ensv2:setup
> npm run ensv2:lifecycle
>
> and those scripts should prove:
>
> real ENSv2 register
> → real read / ALLOW
> → real unregister
> → real read / DENY.
>
> EVIDENCE REQUIRED
>
> Return:
> - git status --short
> - git diff --stat
> - syntax-check results
> - files created
> - concise explanation of setup.mjs
> - concise explanation of lifecycle.mjs
>
> STOP IF
>
> - repository is not clean at start
> - official ABI required by the implementation cannot be verified
> - contract semantics differ from this task
> - a secret would need to be exposed
> - scope would expand beyond the files above
> - firmware would need modification.
