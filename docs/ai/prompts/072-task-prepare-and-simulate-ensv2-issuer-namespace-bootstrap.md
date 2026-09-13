# Project task packet 072: TASK — PREPARE AND SIMULATE ENSV2 ISSUER NAMESPACE BOOTSTRAP

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — PREPARE AND SIMULATE ENSV2 ISSUER NAMESPACE BOOTSTRAP
>
> WHY
>
> Mobile Issuer Admission is CLOSED and checkpointed.
>
> Current synchronized baseline:
>
> HEAD == origin/main ==
> 1122886a3f689f24d875942b4e36ad2e7d30e53c
>
> M1 proved that the dedicated Privy Android issuer can send real Sepolia
> transactions and recover/reconcile them.
>
> Dedicated issuer I:
>
> 0xFa90e8301A22833B74378C5fA3a7c120Ac512685
>
> M1 transaction:
>
> 0x6c4f42f2d368936d4aaf7edf3e0395c376f92b699563b34fee4ed053a0a53e32
>
> Issuer nonce after M1:
>
> 1
>
> The next architecture target is an isolated ENSv2 issuer namespace.
>
> THIS TASK IS PLAN + SIMULATION ONLY.
>
> NO blockchain write is authorized.
>
> ============================================================
> TARGET ARCHITECTURE
> ============================================================
>
> Existing fallback — MUST REMAIN UNTOUCHED:
>
> demo-access.eth
> ├── guest-001.demo-access.eth
> └── existing R0 / S0 path
>
> New additive branch:
>
> demo-access.eth
> └── keys.demo-access.eth
>     ├── owner: I
>     ├── subregistry: NEW R1 UserRegistry
>     └── resolver: NEW S1 PermissionedResolver
>
> R1 will later contain:
>
> staff-001.keys.demo-access.eth
> visitor-001.keys.demo-access.eth
> contractor-001.keys.demo-access.eth
>
> ============================================================
> KNOWN ADDRESSES
> ============================================================
>
> Chain:
>
> Sepolia
> 11155111
>
> ETHRegistry:
>
> 0xbdc85dd5b15d7ecb354cd7cb6f2c50b4f2c4f0e2
>
> Existing UserRegistry R0:
>
> 0x2d249472B83A453086254Acd8a42913D8e45a2Fd
>
> Existing legacy resolver S0:
>
> 0x0B723c0C2170F2ea508F2f4e3e122C6c0782744C
>
> VerifiableFactory:
>
> 0x10dc6333cdfe1fcef624c6e0a8221b91804cd7ef
>
> UserRegistryImpl:
>
> 0x624a25d67b59d587752ebec8dded8827dae52050
>
> PermissionedResolverImpl:
>
> 0x9eae5c2730a7dd16bdd1dee6421a1b91e3b0365e
>
> DEV/operator:
>
> 0x4C60a5AD311510543B56d0408872A52e4AEEe19C
>
> Issuer I:
>
> 0xFa90e8301A22833B74378C5fA3a7c120Ac512685
>
> ============================================================
> LOCKED ROLE MODEL TO VERIFY
> ============================================================
>
> Do not change these silently.
>
> R1 ROOT roles for issuer I:
>
> ROLE_REGISTRAR
> ROLE_REGISTRAR_ADMIN
> ROLE_RENEW
> ROLE_RENEW_ADMIN
>
> Expected constants:
>
> ROLE_REGISTRAR       = 1 << 0
> ROLE_RENEW           = 1 << 16
>
> admin(role)           = role << 128
>
> So:
>
> ROLE_REGISTRAR_ADMIN = 1 << 128
> ROLE_RENEW_ADMIN     = 1 << 144
>
> Reason:
>
> I is intended to be the administrator of its OWN issuer namespace and may need
> future delegation/rotation there.
>
> Do not grant unrelated root roles.
>
> S1 ROOT roles for issuer I:
>
> ROLE_SET_TEXT
> ROLE_SET_TEXT_ADMIN
> ROLE_SET_DATA
> ROLE_SET_DATA_ADMIN
>
> Expected:
>
> ROLE_SET_TEXT        = 1 << 4
> ROLE_SET_TEXT_ADMIN  = 1 << 132
>
> ROLE_SET_DATA        = 1 << 36
> ROLE_SET_DATA_ADMIN  = 1 << 164
>
> Reason:
>
> I must:
>
> - set avatar
> - set description
> - set access.v1
> - later delegate narrowly scoped text permissions
>
> Do not grant unrelated resolver root roles.
>
> keys.demo-access.eth owner roles inside R0:
>
> ROLE_RENEW ONLY
>
> Do NOT include ROLE_RENEW_ADMIN unless exact pinned code proves it is necessary.
>
> Do NOT grant:
>
> ROLE_UNREGISTER
> ROLE_SET_SUBREGISTRY
> ROLE_SET_RESOLVER
> ROLE_CAN_TRANSFER_ADMIN
> upgrade/admin-all roles
>
> to keys owner unless separately justified.
>
> ============================================================
> EXPECTED BOOTSTRAP
> ============================================================
>
> Expected exactly THREE writes:
>
> TX1
>
> VerifiableFactory.deployProxy(
>     UserRegistryImpl,
>     saltR1,
>     encode(
>         UserRegistry.initialize(
>             I,
>             R1_ROOT_ROLES
>         )
>     )
> )
>
> TX2
>
> VerifiableFactory.deployProxy(
>     PermissionedResolverImpl,
>     saltS1,
>     encode(
>         PermissionedResolver.initialize(
>             I,
>             S1_ROOT_ROLES,
>             []
>         )
>     )
> )
>
> TX3
>
> R0.register(
>     "keys",
>     I,
>     R1,
>     S1,
>     ROLE_RENEW,
>     keysExpiry
> )
>
> This task MUST verify this exact structure against the pinned implementation.
>
> ============================================================
> NO-TOUCH
> ============================================================
>
> Do NOT modify or mutate:
>
> guest-001.demo-access.eth
> cred-001.demo-access.eth
> R0 configuration
> S0 configuration
> demo-access.eth parent pointer
> current holder
> access.v1 legacy records
> Android code
> HCE
> Node verifier
> firmware
>
> Do NOT:
>
> broadcast
> sign transactions
> deploy
> register
> renew
> commit
> push
> install dependencies
> print secrets
> print credential-bearing RPC URLs
>
> ============================================================
> PHASE 1 — VERIFY REPOSITORY
> ============================================================
>
> Run:
>
> git fetch origin
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> Require:
>
> clean
>
> HEAD == origin/main ==
> 1122886a3f689f24d875942b4e36ad2e7d30e53c
>
> Inspect AGENTS.md.
>
> STOP if not exact.
>
> ============================================================
> PHASE 2 — FRESH READ-ONLY ONCHAIN BASELINE
> ============================================================
>
> Using the existing safe configured Sepolia read path:
>
> require:
>
> chainId == 11155111
>
> fresh block
>
> DEV:
> latest == pending
> no pending transaction
>
> Issuer I:
> latest == pending == 1
> balance read succeeds
>
> Read and verify:
>
> R0 code/provenance
> S0 unchanged
> guest-001 intact
> cred-001 intact
>
> Read the current demo-access.eth / R0 hierarchy necessary to determine the
> maximum legal expiry for keys.
>
> Do not print RPC endpoint.
>
> ============================================================
> PHASE 3 — KEYS LABEL AVAILABILITY
> ============================================================
>
> Inspect:
>
> keys.demo-access.eth
>
> Require:
>
> not currently REGISTERED
>
> no unexpected stale/current token state that would make simple registration
> unsafe
>
> no existing subregistry/resolver pointer for an active keys entry
>
> If keys is not cleanly available:
>
> STOP.
>
> Do not silently choose another label.
>
> ============================================================
> PHASE 4 — PINNED ABI / SOURCE RECONFIRMATION
> ============================================================
>
> Using the repository's pinned ENSv2 source/artifacts, reconfirm exact deployed
> ABIs.
>
> Factory:
>
> deployProxy(
>     address implementation,
>     uint256 salt,
>     bytes data
> ) returns (address proxy)
>
> verifyContract(address proxy)
>     returns (address implementation)
>
> UserRegistry:
>
> initialize(
>     address rootAccount,
>     uint256 roleBitmap
> )
>
> register(
>     string label,
>     address owner,
>     address registry,
>     address resolver,
>     uint256 roleBitmap,
>     uint64 expiry
> ) returns (uint256)
>
> PermissionedResolver:
>
> initialize(
>     address admin,
>     uint256 roleBitmap,
>     bytes[] setters
> )
>
> Do not copy ABI guesses from docs when pinned artifact differs.
>
> ============================================================
> PHASE 5 — ROLE BITMAP COMPUTATION
> ============================================================
>
> Compute and print PUBLIC decimal/hex role bitmaps for:
>
> R1_ROOT_ROLES
>
> S1_ROOT_ROLES
>
> KEYS_OWNER_ROLES
>
> Verify each through pinned role constants.
>
> Explicitly prove:
>
> KEYS_OWNER_ROLES == ROLE_RENEW only
>
> and:
>
> ROLE_RENEW_ADMIN is NOT required for I to call renew on keys.
>
> List every effective role being granted.
>
> STOP if any bitmap grants an unintended role.
>
> ============================================================
> PHASE 6 — EXPIRY PLANNING
> ============================================================
>
> Do NOT invent keysExpiry.
>
> Read all relevant ancestor expiries.
>
> Determine:
>
> maximum legal keys expiry
>
> and recommend one practical keysExpiry that:
>
> - is comfortably after the hackathon;
> - allows demo credentials with expiries such as 2026-10-31;
> - stays safely below every relevant ancestor expiry;
> - leaves reasonable safety margin before its parent expires.
>
> Return:
>
> UTC timestamp
> ISO date
> Europe/Madrid date
> margin to nearest ancestor expiry
>
> Do not lock it into a write yet.
>
> Control Tower will approve the exact expiry.
>
> ============================================================
> PHASE 7 — SALTS AND EXPECTED PROXY ADDRESSES
> ============================================================
>
> Design deterministic PUBLIC salts for R1 and S1.
>
> Requirements:
>
> - reproducible;
> - no secret/random dependency;
> - distinct R1/S1 domains;
> - include enough project/issuer context to avoid accidental collision.
>
> Example conceptual domains:
>
> ethonline-2026:keys:r1:<issuer>
> ethonline-2026:keys:s1:<issuer>
>
> But inspect factory salt/address semantics before finalizing.
>
> Compute:
>
> saltR1
> saltS1
>
> and expected/predicted:
>
> R1 address
> S1 address
>
> using the EXACT factory/clone algorithm pinned by this project.
>
> Do not assume deployer == issuer.
>
> The actual deployer will be DEV.
>
> Require predicted R1/S1 currently have:
>
> code size == 0
>
> and are not already used/verified unexpectedly.
>
> If prediction cannot be proven safely:
>
> do not guess addresses.
>
> Use simulation-returned expected addresses instead.
>
> ============================================================
> PHASE 8 — INITIALIZER CALLDATA
> ============================================================
>
> Generate exact public calldata for:
>
> R1 initialize
>
> S1 initialize
>
> Then exact Factory.deployProxy calldata for TX1/TX2.
>
> No private data involved.
>
> Decode it back and require exact equality with intended:
>
> implementation
> salt
> admin/root
> roles
> setters []
>
> Round-trip test the calldata.
>
> ============================================================
> PHASE 9 — READ-ONLY SIMULATION / GAS ESTIMATION
> ============================================================
>
> Use eth_call / viem simulateContract / estimateContractGas as appropriate.
>
> NO wallet write.
>
> NO send.
>
> Simulate or safely estimate:
>
> TX1 deploy R1
>
> TX2 deploy S1
>
> TX3 R0.register keys using the planned R1/S1 addresses
>
> Use DEV as the simulated `account/from`.
>
> Require no revert.
>
> If TX3 cannot be faithfully simulated before R1/S1 exist, explain precisely
> why and perform the strongest available read-only validation instead.
>
> Return gas estimates individually and total.
>
> Do not manually invent gas limits for future execution.
>
> ============================================================
> PHASE 10 — POST-WRITE VERIFICATION CONTRACT
> ============================================================
>
> Define exact required verification AFTER each future write.
>
> After TX1:
>
> - receipt success
> - expected R1 address
> - code exists
> - factory verifyContract(R1) == UserRegistryImpl
> - I has exactly intended root roles
> - DEV did not accidentally receive R1 root authority
>
> After TX2:
>
> - receipt success
> - expected S1 address
> - code exists
> - factory verifyContract(S1) == PermissionedResolverImpl
> - I has exactly intended root roles
> - setters empty
> - DEV did not accidentally receive S1 root authority
>
> After TX3:
>
> - receipt success
> - keys REGISTERED
> - owner == I
> - subregistry == R1
> - resolver == S1
> - expiry == approved expiry
> - owner roles == ROLE_RENEW and nothing unintended
> - demo-access.eth hierarchy still points through existing R0
> - guest-001 intact
> - cred-001 intact
>
> Also require DEV nonce reconciliation after every write.
>
> ============================================================
> PHASE 11 — BUILD A REPRODUCIBLE PLANNER
> ============================================================
>
> If existing committed helpers are insufficient, create the smallest reusable
> Node planner.
>
> Preferred path:
>
> scripts/ensv2/issuer-bootstrap-plan.mjs
>
> It must be READ-ONLY ONLY.
>
> It must have no execution/broadcast mode.
>
> It may:
>
> - read chain
> - inspect roles
> - compute salts
> - compute/predict addresses
> - encode calldata
> - simulate
> - estimate gas
> - generate a sanitized plan
>
> It MUST NOT import or use a private-key wallet client.
>
> Prefer publicClient only.
>
> If some simulation requires `account`, pass the PUBLIC DEV address as `from`,
> not a signer.
>
> No new dependency expected; use existing viem.
>
> Tests required for:
>
> role bitmaps
> salt determinism
> initializer round-trip
> address prediction if implemented
> keys label validation
> expiry bounds
> no signer/write APIs in planner
>
> Write generated runtime plan only under ignored `.runtime` if persistence is
> useful.
>
> Do not create authoritative config/issuer-space.json yet.
>
> R1/S1 are not authoritative until receipts exist.
>
> ============================================================
> PHASE 12 — VALIDATION
> ============================================================
>
> Run relevant:
>
> node --test
>
> git diff --check
>
> If a new planner is added, run it read-only.
>
> No blockchain writes.
>
> Return actual test counts.
>
> ============================================================
> RETURN
> ============================================================
>
> # ENSV2 ISSUER BOOTSTRAP — PLAN ONLY
>
> ## REPO
>
> HEAD:
> origin/main:
> clean:
>
> ## FRESH ONCHAIN BASELINE
>
> Block:
> DEV nonce:
> Issuer nonce:
> Issuer balance:
>
> Legacy fallback:
> guest-001:
> cred-001:
>
> ## KEYS AVAILABILITY
>
> keys.demo-access.eth:
> AVAILABLE / STOP
>
> ## CONTRACTS
>
> Factory:
> R0:
> UserRegistryImpl:
> PermissionedResolverImpl:
>
> ## ROLE BITMAPS
>
> R1:
> decimal:
> hex:
> roles:
>
> S1:
> decimal:
> hex:
> roles:
>
> keys owner:
> decimal:
> hex:
> roles:
>
> ## EXPIRY
>
> Nearest ancestor expiry:
>
> Maximum legal keys expiry:
>
> Recommended keys expiry:
>
> UTC:
> Madrid:
> Safety margin:
>
> ## SALTS
>
> R1:
> S1:
>
> ## EXPECTED ADDRESSES
>
> R1:
> S1:
>
> How derived:
>
> Existing code at either address:
> YES / NO
>
> ## TX1 — R1 PLAN
>
> Target:
> Function:
> Initializer:
> Calldata digest:
> Simulation:
> Gas estimate:
>
> ## TX2 — S1 PLAN
>
> Target:
> Function:
> Initializer:
> Calldata digest:
> Simulation:
> Gas estimate:
>
> ## TX3 — KEYS PLAN
>
> Target:
> Function:
> label:
> owner:
> subregistry:
> resolver:
> roles:
> expiry:
> Simulation:
> Gas estimate:
>
> ## TOTAL ESTIMATED GAS
>
> ## POST-WRITE ACCEPTANCE
>
> TX1:
> TX2:
> TX3:
>
> ## PLANNER
>
> Files changed:
>
> Read-only guarantee:
>
> Tests:
>
> ## SECURITY
>
> Blockchain writes:
> 0
> Signing:
> 0
> Secrets:
> 0
>
> ## GIT
>
> Do NOT commit/push.
>
> ## VERDICT
>
> BOOTSTRAP PLAN:
> PASS / STOP
>
> If PASS end exactly:
>
> NEXT AUTHORIZATION REQUIRED:
> APPROVE EXACT KEYS EXPIRY AND THREE BOOTSTRAP WRITES.
>
> Do not execute them.
