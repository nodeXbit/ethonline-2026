# Project task packet 028: TASK — Perform ONLY the read-only live Gate E Sepolia preflight.

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Perform ONLY the read-only live Gate E Sepolia preflight.
>
> DO NOT send any blockchain transaction.
>
> DO NOT provision guest-001.
> DO NOT activate/deactivate anything.
> DO NOT change files.
> DO NOT commit or push.
>
> WHY
>
> Gate E implementation is checkpointed and pushed.
>
> Before authorizing live writes, prove that the current Sepolia state,
> permissions, resolver provenance, target credential state, nonce state and
> transaction simulations are all correct.
>
> CURRENT EXPECTED REPOSITORY STATE
>
> Expected:
>
> HEAD == origin/main ==
> b6159981f1e01195df9183ecc9a535a072edf2c7
>
> Expected tracked worktree:
> clean
>
> First run:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> If tracked state is dirty/ahead/behind/divergent:
> STOP.
>
> NO SOURCE EDITS.
>
> KNOWN LIVE CONFIGURATION
>
> Chain:
>
> Sepolia
> chainId 11155111
>
> Parent:
>
> demo-access.eth
>
> Existing UserRegistry proxy:
>
> 0x2d249472B83A453086254Acd8a42913D8e45a2Fd
>
> Verified reusable PermissionedResolver:
>
> 0x0B723c0C2170F2ea508F2f4e3e122C6c0782744C
>
> DEV operator public address:
>
> 0x4C60a5AD311510543B56d0408872A52e4AEEe19C
>
> Existing reference credential:
>
> cred-001.demo-access.eth
>
> New Gate E credential target:
>
> guest-001.demo-access.eth
>
> Intended new owner:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> This is the empirically verified Privy embedded Ethereum EOA.
>
> Do not require the Privy device for this preflight.
>
> SECURITY
>
> Existing local environment/configuration may be USED by the already-existing
> code for RPC configuration if required.
>
> However:
>
> - do not cat/open/print environment files
> - do not print RPC credentials
> - do not print DEV_PRIVATE_KEY
> - do not derive/show private key material
> - do not modify environment files
>
> Use the known DEV public address for read-only account/simulation context where
> possible.
>
> PHASE 1 — NETWORK / NONCE
>
> Using read-only RPC:
>
> 1. require chain ID 11155111;
> 2. record a pinned/current block number and timestamp;
> 3. query DEV transaction counts:
>
> latest nonce
> pending nonce
>
> Require:
>
> latest == pending
>
> If pending > latest:
> STOP.
>
> Do not replace/cancel any transaction.
>
> PHASE 2 — PARENT / REGISTRY STATE
>
> Verify from current onchain state:
>
> 1. demo-access.eth still resolves to / delegates to the expected UserRegistry
>    architecture used by this project;
> 2. UserRegistry proxy address is still:
>
>    0x2d249472B83A453086254Acd8a42913D8e45a2Fd
>
> 3. expected registry bytecode exists;
> 4. any existing provenance/implementation verification used by project tooling
>    still passes.
>
> Do not redeploy anything.
>
> PHASE 3 — OPERATOR PERMISSIONS
>
> Verify the DEV operator currently has every role/capability required for the
> planned writes.
>
> At minimum establish:
>
> A. UserRegistry permission needed to register guest-001.
>
> B. PermissionedResolver role needed to write access.v1 for the guest node.
>
> Use the actual current role constants/helpers from the repository.
>
> Do not infer permission merely because earlier transactions worked.
>
> Return the actual relevant role/bitmap evidence in public numeric/hex form if
> safe.
>
> Do not modify roles.
>
> PHASE 4 — RESOLVER PROVENANCE
>
> Verify the planned resolver:
>
> 0x0B723c0C2170F2ea508F2f4e3e122C6c0782744C
>
> still satisfies the project's existing verified-resolver checks.
>
> At minimum require:
>
> - bytecode exists;
> - expected proxy/provenance verification passes;
> - current implementation is the expected PermissionedResolver implementation
>   according to existing project logic;
> - DEV has the required SET_DATA capability.
>
> Do not deploy a new resolver.
>
> If provenance is no longer verifiable:
> STOP.
>
> PHASE 5 — REFERENCE CREDENTIAL INVARIANT
>
> Read:
>
> cred-001.demo-access.eth
>
> using the existing coherent credential-state helper.
>
> Record only enough evidence to confirm it remains the previous reference
> credential.
>
> Require:
>
> - status REGISTERED
> - owner unchanged from the established project state
> - tokenId/resource identity unchanged if current helper exposes them
> - resolver unchanged
> - registry expiry still valid
> - access state readable
>
> Do NOT require access to be ACTIVE or INACTIVE for this preflight.
>
> The purpose is only:
>
> cred-001 exists and is intact.
>
> Do not mutate it.
>
> PHASE 6 — TARGET CREDENTIAL STATE
>
> Read:
>
> guest-001.demo-access.eth
>
> through the existing credential-state path.
>
> Expected:
>
> AVAILABLE / unregistered
>
> If it is already REGISTERED:
> STOP.
>
> Report:
>
> - status
> - owner if any
> - registry expiry
> - resolver if any
> - token/resource if relevant
>
> Do not unregister or overwrite anything.
>
> Also inspect the existing PermissionedResolver access.v1 data for the
> guest-001 namehash.
>
> Classify it:
>
> - absent/default
> - valid INACTIVE
> - valid ACTIVE
> - malformed/unexpected
>
> Do not write it.
>
> This determines whether initial setup requires one or two transactions.
>
> PHASE 7 — EXACT PROVISIONING PARAMETERS
>
> Inspect the CURRENT committed implementation of:
>
> npm run ensv2:persistent:setup
>
> and the --credential-label / --credential-owner parameterization.
>
> Without running a write, resolve the exact effective parameters that would be
> used for:
>
> --credential-label guest-001
> --credential-owner 0x3419148731087b970d2059C53780163B452D5FF7
>
> Report:
>
> - full credential name
> - namehash/node
> - intended owner
> - UserRegistry
> - resolver
> - role bitmap assigned to the credential
> - registration expiry that would be requested / how it is derived
> - access.v1 value that setup would request
> - access validUntil value/how it is derived
>
> Do not invent values.
>
> Use actual current code/defaults.
>
> PHASE 8 — SIMULATE INITIAL access.v1 WRITE
>
> If current access.v1 is not already the exact valid INACTIVE state that setup
> would require:
>
> perform a READ-ONLY publicClient.simulateContract-equivalent for the exact
> resolver setData call setup would send.
>
> Account context:
>
> DEV public address
>
> Require simulation:
> PASS
>
> Capture safe evidence:
>
> - target contract
> - function
> - node
> - key = access.v1
> - intended decoded access state
> - whether simulation succeeded
>
> Do NOT call writeContract.
> Do NOT sign.
> Do NOT broadcast.
>
> If simulation fails:
> STOP.
>
> If the correct INACTIVE record already exists:
> report SET_DATA REQUIRED: NO and do not simulate an unnecessary write unless
> useful strictly as read-only validation.
>
> PHASE 9 — SIMULATE guest-001 REGISTRATION
>
> Using the exact current register call produced by existing tooling, run a
> READ-ONLY simulation for registering:
>
> guest-001
>
> owner:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> with the verified resolver and intended expiry/roles.
>
> Account context:
>
> DEV public address
>
> Require:
> simulation PASS.
>
> Do not sign or broadcast.
>
> Capture:
>
> - contract
> - function
> - label
> - intended owner
> - resolver
> - expiry
> - role bitmap
> - simulation result
>
> If simulation fails:
> STOP.
>
> PHASE 10 — EXPECTED WRITE COUNT
>
> Based on actual current state, state exactly:
>
> EXPECTED PROVISIONING WRITES:
> 0 / 1 / 2
>
> Expected normal fresh state is likely:
>
> 1. setData access.v1 INACTIVE
> 2. register guest-001
>
> but do not assume that if current resolver data makes one step unnecessary.
>
> For every expected write, name:
>
> - contract
> - function
> - purpose
>
> Do not authorize them.
>
> PHASE 11 — POST-PROVISION EXPECTED STATE
>
> State the exact expected read-only outcome after a future successful
> provisioning:
>
> guest-001.demo-access.eth
>
> status:
> REGISTERED
>
> owner:
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> resolver:
> 0x0B723c0C2170F2ea508F2f4e3e122C6c0782744C
>
> access.active:
> false
>
> AUTHORIZATION:
> DENY
>
> Do not claim this state currently exists.
>
> PHASE 12 — LIVE VALIDATION WRITE BUDGET
>
> Without executing anything, enumerate the complete later Gate E live write
> budget.
>
> Provisioning:
> - exact expected write count from Phase 10
>
> Then validation:
>
> A. INACTIVE physical DENY:
> 0 writes
>
> B. activate guest-001:
> maximum 1 resolver setData write if state change required
>
> C. ACTIVE physical ALLOW:
> 0 writes
>
> D. same-proof replay DENY:
> 0 writes
>
> E. deactivate guest-001:
> maximum 1 resolver setData write if state change required
>
> F. final INACTIVE physical DENY:
> 0 writes
>
> State the maximum total Sepolia writes required from the current preflight
> state through the complete Gate E validation.
>
> Do NOT execute any.
>
> PHASE 13 — FINAL SAFETY CHECK
>
> Before returning verify again:
>
> - DEV latest nonce
> - DEV pending nonce
> - no transaction was sent during this task
> - git status unchanged
> - cred-001 unchanged
> - guest-001 still unprovisioned
>
> RETURN
>
> # GATE E LIVE PREFLIGHT
>
> ## REPO STATE
>
> ## NETWORK / NONCE
>
> ## USERREGISTRY
>
> ## OPERATOR PERMISSIONS
>
> ## RESOLVER PROVENANCE
>
> ## CRED-001 INVARIANT
>
> ## GUEST-001 CURRENT STATE
>
> ## PROVISIONING PARAMETERS
>
> ## SETDATA SIMULATION
>
> ## REGISTER SIMULATION
>
> ## EXPECTED PROVISIONING WRITES
>
> State exact number.
>
> ## EXPECTED POST-PROVISION STATE
>
> ## FULL LIVE WRITE BUDGET
>
> State exact maximum count.
>
> ## FINAL SAFETY CHECK
>
> ## USER ACTION REQUIRED
>
> USER ACTION REQUIRED must say:
>
> None.
>
> No transaction is authorized by this preflight.
>
> End exactly:
>
> LIVE PREFLIGHT: PASS
>
> or
>
> LIVE PREFLIGHT: STOP — <reason>
>
> STOP IF
>
> Stop immediately if:
>
> - repository is not clean/synchronized;
> - pending nonce exists;
> - wrong chain;
> - expected UserRegistry pointer/address changed;
> - required DEV role missing;
> - resolver provenance fails;
> - cred-001 invariant is unexpectedly broken;
> - guest-001 is already registered;
> - existing guest access.v1 is malformed/unexpected;
> - setData simulation fails;
> - registration simulation fails;
> - any operation would require broadcasting a transaction.
>
> Do not repair any failed preflight condition.
> Report it to Control Tower.
