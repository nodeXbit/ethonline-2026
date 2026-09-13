# Project task packet 005: TASK — Fix only the operational blockers found in the final read-only audit of the

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Fix only the operational blockers found in the final read-only audit of the
> persistent ENSv2 credential implementation.
>
> Do not redesign the access-record model.
>
> TARGET OUTCOME
>
> `npm run ensv2:persistent:setup` must become safely resumable from onchain
> state after normal interruption points.
>
> The user must NOT need to manually copy or configure ENS_ACCESS_RESOLVER.
>
> The runtime must discover or deterministically derive the authoritative
> resolver itself.
>
> OFFICIAL FACTS TO USE
>
> VerifiableFactory uses CREATE2.
>
> For deployProxy(implementation, userSalt, data):
>
> outerSalt = keccak256(abi.encode(caller, userSalt))
>
> The proxy address is deterministic from:
>
> * factory address
> * factory.proxyLogic()
> * caller/deployer
> * userSalt
>
> Canonical OwnedResolver salt:
>
> keccak256(
> abi.encode(
> keccak256("OwnedResolver"),
> owner,
> uint256(0)
> )
> )
>
> ENS tooling uses this deterministic address specifically so an OwnedResolver
> can be rediscovered from the owner and deployment can short-circuit when the
> proxy already exists.
>
> Current pinned ENSv2 deployment/source remains:
>
> contracts-v2 commit:
> 97a57293f3b4279d94b571e678edb53ce62638f4
>
> PermissionedResolverImpl:
> 0x9eae5c2730a7dd16bdd1dee6421a1b91e3b0365e
>
> VerifiableFactory:
> 0x10dc6333cdfe1fcef624c6e0a8221b91804cd7ef
>
> UserRegistry:
> 0x2d249472B83A453086254Acd8a42913D8e45a2Fd
>
> Credential:
> cred-001.demo-access.eth
>
> SCOPE
>
> Modify only the existing persistent-credential task files as required:
>
> * package.json only if command definitions need correction
> * scripts/ensv2/access-record.mjs
> * scripts/ensv2/persistent-access.mjs
> * scripts/ensv2/access-record.test.mjs
> * scripts/ensv2/persistent-access.md
> * scripts/nfc/bridge.mjs
>
> Do NOT touch previous validated lifecycle scripts or firmware.
>
> 1. REMOVE MANUAL ENS_ACCESS_RESOLVER REQUIREMENT
>
> Remove ENS_ACCESS_RESOLVER as a required runtime configuration.
>
> Do not require the user to copy the resolver into:
>
> * .env.local
> * .env.nfc.local
> * shell environment
>
> Do not add it to .env.nfc.example.
>
> Update persistent-access.md accordingly.
>
> For an already REGISTERED credential:
>
> * get the resolver directly from UserRegistry.getResolver("cred-001")
> * verify its bytecode
> * call VerifiableFactory.verifyContract(resolver)
> * require returned implementation ==
>   PermissionedResolverImpl
> * use that resolver as authoritative.
>
> For an AVAILABLE credential:
>
> * if a usable existing parent resolver is intentionally supported by the
>   current design, verify it fully before reuse;
> * otherwise derive the canonical OwnedResolver candidate deterministically.
>
> 2. DETERMINISTIC RESOLVER PREDICTION
>
> Implement the exact VerifiableFactory CREATE2 prediction required for the
> canonical OwnedResolver.
>
> Do not invent an approximate formula.
>
> Use the official VerifiableFactory / ENS reference implementation semantics.
>
> Read factory.proxyLogic() onchain.
>
> Compute:
>
> userSalt =
> keccak256(
> abi.encode(
> keccak256(toBytes("OwnedResolver")),
> DEV wallet,
> uint256(0)
> )
> )
>
> outerSalt =
> keccak256(
> abi.encode(
> DEV wallet,
> userSalt
> )
> )
>
> Derive the exact proxy CREATE2 address using the factory's real clone creation
> bytecode semantics.
>
> Before first deployment, when no code exists at the predicted address:
>
> * simulate deployProxy(...)
> * require the simulated returned proxy address equals the locally predicted
>   address.
>
> If they differ:
> STOP before writing.
>
> When code already exists at the predicted address:
>
> * DO NOT call deployProxy again
> * verifyContract(predictedResolver)
> * require implementation == PermissionedResolverImpl
> * verify DEV control needed for setData via safe simulation/read checks
> * reuse it.
>
> Add local tests for deterministic prediction.
>
> 3. RESUMABLE SETUP STATE MACHINE
>
> Rework setup so it reconstructs progress from current onchain state.
>
> Do not assume setup always starts from scratch.
>
> Case A — credential AVAILABLE, resolver not deployed:
>
> * derive canonical resolver
> * deploy it
> * continue
>
> Case B — credential AVAILABLE, resolver already deployed:
>
> * verify and reuse resolver
> * continue
>
> Case C — credential AVAILABLE, access.v1 missing:
>
> * set access.v1 INACTIVE before registration
> * initial:
>   active = false
>   validUntil = current block timestamp + 24 hours
> * continue
>
> Case D — credential AVAILABLE, access.v1 already exists:
>
> * if it is valid, INACTIVE, and validUntil is still future:
>   preserve it; do not rewrite unnecessarily
> * if ACTIVE/stale in this pre-registration state:
>   safely force INACTIVE before registration
> * continue
>
> Case E — credential REGISTERED:
>
> Treat the credential's resolver pointer as authoritative.
>
> Require:
>
> * expected UserRegistry
> * owner is the expected DEV holder for this slice
> * registry expiry is future
> * resolver is nonzero
> * resolver bytecode exists
> * factory verification passes
> * implementation == PermissionedResolverImpl
>
> Then inspect access.v1.
>
> If access.v1 exists and is valid:
>
> * DO NOT reset ACTIVE to INACTIVE
> * preserve current access state
> * setup should complete successfully without changing it.
>
> If access.v1 is missing:
>
> * initialize it to INACTIVE as a fail-closed recovery step.
>
> Then print successful READY state.
>
> A rerun after successful setup must therefore be idempotent and must not
> deactivate an already activated credential.
>
> 4. UNKNOWN/PENDING TRANSACTION SAFETY
>
> Before a setup write, compare the DEV account's latest and pending transaction
> counts if viem supports this safely.
>
> If there is an unresolved pending transaction from the DEV account:
>
> STOP with a clear message telling the user to wait for its result before
> rerunning setup.
>
> Do not send a potentially duplicate recovery transaction while an earlier
> transaction may still be pending.
>
> This is a safety guard; do not attempt transaction replacement.
>
> 5. KEEP INACTIVE-BEFORE-REGISTRATION ORDER
>
> Do NOT reverse the ordering.
>
> Maintain:
>
> resolver verified/deployed
> → access.v1 explicitly INACTIVE
> → credential register
>
> Reason:
> a missing or stale resolver record must never create a temporary authorization
> window when the credential becomes REGISTERED.
>
> 6. REMOVE ARBITRARY 30-DAY RULE
>
> The 30-day parent-expiry requirement is not an ENSv2 requirement.
>
> Replace it with invariants derived from this slice.
>
> Initial access deadline:
>
> initialAccessValidUntil = snapshot.timestamp + 24 hours
>
> Credential registry expiry:
>
> target approximately one year,
> but always <= parent expiry.
>
> Require:
>
> credentialRegistryExpiry > initialAccessValidUntil
>
> and:
>
> credentialRegistryExpiry <= parentExpiry
>
> If those cannot both be satisfied:
> STOP.
>
> Do not introduce another arbitrary fixed multi-day threshold.
>
> 7. FIX EXPIRED-CREDENTIAL BRIDGE SEMANTICS
>
> ENSv2 can return zero effective owner/resolver for an expired credential.
>
> The bridge must evaluate registry validity BEFORE requiring a resolver.
>
> For mapped credential:
>
> If any of these is false:
>
> * status == REGISTERED
> * owner != zero
> * registry expiry > pinned block timestamp
>
> then this is a normal authorization result:
>
> AUTHORIZATION: DENY
>
> and the bridge must not require a resolver afterwards.
>
> Only for a registry-valid credential:
>
> * read resolver
> * resolver == zero => DENY because access policy is not configured
> * nonzero resolver with missing bytecode => ERROR
> * factory provenance failure => ERROR
> * wrong implementation => ERROR
> * access.v1 == 0x => DENY
> * malformed access.v1 => ERROR
> * active false => DENY
> * validUntil <= timestamp => DENY
> * otherwise ALLOW
>
> RPC failures remain ERROR, never DENY.
>
> 8. INSPECT MUST REQUIRE NO PRIOR RESOLVER KNOWLEDGE
>
> `npm run ensv2:persistent:inspect` must be read-only.
>
> If credential REGISTERED:
>
> * discover resolver from credential
> * verify it
> * display it
> * display registry state
> * display access state
> * display computed authorization
>
> If credential AVAILABLE:
>
> * report the credential is not currently registered
> * it may also print the canonical predicted resolver for public/debugging
>   evidence
> * do not require ENS_ACCESS_RESOLVER.
>
> 9. RECOVERY TESTS
>
> Add/modify local tests to cover at minimum:
>
> * no resolver deployed
> * deterministic predicted resolver already deployed
> * inactive record already initialized
> * credential already REGISTERED + INACTIVE
> * credential already REGISTERED + ACTIVE
> * registered credential with missing access.v1
> * rerunning setup does not deactivate ACTIVE
> * malformed access record
> * expired registry credential => DENY without resolver requirement
> * wrong factory provenance => ERROR
> * wrong resolver implementation => ERROR
>
> Model setup as a state machine where practical so these recovery paths can be
> tested without onchain writes.
>
> 10. FINAL RECOVERY AUDIT
>
> After edits, perform a new read-only interruption table for:
>
> A. before resolver deployment
> B. after resolver deployment
> C. after access record initialization
> D. after credential registration
> E. after final validation
>
> The target classification for normal confirmed transaction outcomes is:
>
> SAFE IDEMPOTENT
>
> If any normal confirmed interruption still requires manual recovery:
> REVIEW: STOP
>
> NO-TOUCH
>
> * firmware/
> * scripts/ensv2/lifecycle.mjs
> * scripts/ensv2/credential-state.mjs
> * existing validated ENSv2 setup
> * STATUS.md
> * WORKLOG.md
> * PROJECT.md
> * README.md
> * Android/iOS
> * Aliro
> * account abstraction
> * UI
> * metadata
> * transfers
> * custom Solidity
>
> SECURITY
>
> * Do not access .env.local
> * Do not access .env.nfc.local
> * Do not ask for or print private keys
> * Do not install dependencies
> * Do not send transactions
> * Do not commit
> * Do not push
>
> VALIDATION
>
> Run only safe local/read-only validation:
>
> * syntax checks
> * all local tests
> * deterministic resolver prediction tests
> * recovery state-machine tests
> * bridge fail-closed tests
> * git diff --check
> * git status --short
>
> Confirm no no-touch file changed.
>
> RETURN
>
> # RECOVERY FIX RESULT
>
> ## FILES
>
> ## DETERMINISTIC RESOLVER DISCOVERY
>
> ## SETUP STATE MACHINE
>
> ## BRIDGE EXPIRY SEMANTICS
>
> ## TESTS
>
> ## INTERRUPTION RECOVERY TABLE
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
