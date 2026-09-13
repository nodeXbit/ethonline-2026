# Project task packet 029: TASK — Provision ONLY the new Gate E secure credential on Sepolia and verify its

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Provision ONLY the new Gate E secure credential on Sepolia and verify its
> initial INACTIVE state.
>
> The user explicitly authorizes at most TWO blockchain transactions:
>
> 1. one PermissionedResolver setData write to initialize guest-001 access.v1 as
>    INACTIVE, only if still required;
>
> 2. one UserRegistry register write for guest-001 owned by the verified Privy
>    EOA, only if still required.
>
> NO OTHER BLOCKCHAIN WRITE IS AUTHORIZED.
>
> Do NOT activate access.
> Do NOT deactivate anything.
> Do NOT modify cred-001.
> Do NOT unregister or transfer anything.
> Do NOT deploy contracts.
> Do NOT change roles.
> Do NOT commit or push.
>
> CURRENT EXPECTED REPOSITORY STATE
>
> HEAD == origin/main ==
> b6159981f1e01195df9183ecc9a535a072edf2c7
>
> Working tree:
> clean
>
> Verify before execution.
>
> If repository state differs materially:
> STOP.
>
> VERIFIED PREFLIGHT EVIDENCE
>
> Chain:
> Sepolia
>
> Chain ID:
> 11155111
>
> Preflight block:
> 11673143
>
> DEV operator:
> 0x4C60a5AD311510543B56d0408872A52e4AEEe19C
>
> Preflight nonce:
> latest 18
> pending 18
>
> Parent:
> demo-access.eth
>
> UserRegistry:
> 0x2d249472B83A453086254Acd8a42913D8e45a2Fd
>
> Verified UserRegistry implementation:
> 0x624a25d67B59D587752EbEc8DdeD8827dAe52050
>
> Resolver:
> 0x0B723c0C2170F2ea508F2f4e3e122C6c0782744C
>
> Verified PermissionedResolver implementation:
> 0x9EAe5C2730a7dD16BDD1DeE6421a1B91e3B0365e
>
> Target credential:
> guest-001.demo-access.eth
>
> Label:
> guest-001
>
> Node:
> 0x0f9141a3389384fdeb3e72a77c7e891cd10eea2ce683d0a434ee9372ea9c69fc
>
> Intended owner:
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> Credential role bitmap:
> 0
>
> Preflight current guest state:
> AVAILABLE
> owner zero
> resolver zero
> access.v1 absent/default
>
> Preflight simulations:
> resolver setData PASS
> UserRegistry register PASS
>
> Expected provisioning writes:
> 2
>
> IMPORTANT
>
> The exact expiry/access-validUntil values from preflight were derived from that
> snapshot timestamp.
>
> Do NOT blindly reuse stale absolute timestamps if the existing setup command
> normally derives fresh values at execution time.
>
> Use the committed provisioning implementation and its current invariants.
>
> PHASE 1 — FRESH READ-ONLY SAFETY CHECK
>
> Immediately before any write:
>
> 1. verify chain ID 11155111;
> 2. verify HEAD/origin/main/worktree;
> 3. query DEV latest and pending nonce;
> 4. require latest == pending;
> 5. read guest-001 current state;
> 6. require guest-001 still AVAILABLE;
> 7. inspect guest access.v1;
> 8. verify resolver provenance;
> 9. verify required DEV registration and resolver roles;
> 10. verify cred-001 remains intact.
>
> If any relevant state changed from the preflight:
> STOP.
>
> Do not repair it automatically.
>
> PHASE 2 — EXECUTION PLAN
>
> Use the committed existing command:
>
> npm run ensv2:persistent:setup -- --credential-label guest-001 --credential-owner 0x3419148731087b970d2059C53780163B452D5FF7
>
> Before allowing it to send anything, inspect its current execution path and
> confirm for THIS state it will perform no more than:
>
> 1. setData access.v1 INACTIVE
> 2. register guest-001
>
> No resolver deployment.
> No role write.
> No subregistry write.
> No cred-001 write.
> No third transaction.
>
> If execution could produce any additional write:
> STOP.
>
> PHASE 3 — WRITE 1: INITIAL ACCESS.V1
>
> If the exact intended INACTIVE access record is still absent and the setup
> requires initialization:
>
> authorize exactly ONE:
>
> PermissionedResolver.setData(
>   guest-001 node,
>   "access.v1",
>   encoded INACTIVE record
> )
>
> Requirements:
>
> - use existing validated write path;
> - simulate immediately before broadcast if current code does so;
> - submit once;
> - capture transaction hash;
> - capture transaction nonce;
> - wait for confirmed successful receipt;
> - capture block number;
> - perform authoritative readback;
> - require access.active == false.
>
> If submission status becomes uncertain after a transaction hash exists:
>
> use the project's existing post-submission recovery logic.
>
> DO NOT RESUBMIT.
>
> If transaction reverts:
> STOP.
>
> If the correct record already exists at execution time:
> perform zero writes for this step and report a no-op.
>
> PHASE 4 — NONCE CHECK
>
> After Write 1/no-op:
>
> query latest and pending nonce.
>
> Require no unexplained pending transaction.
>
> If Write 1 occurred:
> expected nonce increase = exactly +1.
>
> PHASE 5 — WRITE 2: REGISTER GUEST-001
>
> Freshly read guest-001 immediately before register.
>
> Require:
> AVAILABLE.
>
> Register exactly:
>
> label:
> guest-001
>
> owner:
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> registry:
> the existing UserRegistry/subregistry semantics used by current tooling
>
> resolver:
> 0x0B723c0C2170F2ea508F2f4e3e122C6c0782744C
>
> role bitmap:
> 0
>
> expiry:
> fresh value produced by the existing validated persistent setup invariants.
>
> Authorize exactly ONE register transaction.
>
> Requirements:
>
> - simulate immediately before broadcast if current tooling does so;
> - submit once;
> - capture transaction hash;
> - capture transaction nonce;
> - wait for confirmed receipt;
> - capture block;
> - authoritative readback.
>
> Do not retry registration after a hash exists.
>
> If status becomes uncertain:
> recover/read only from the submitted hash/state.
>
> If register reverts:
> STOP.
>
> PHASE 6 — AUTHORITATIVE POST-PROVISION READBACK
>
> Using the existing coherent credential reader, require:
>
> credential:
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
> registry expiry:
> future / valid
>
> access.active:
> false
>
> access.validUntil:
> future according to setup invariants
>
> AUTHORIZATION:
> DENY
>
> Record:
>
> - resulting tokenId
> - resulting resource
> - expiry
> - access.validUntil
> - pinned verification block
>
> These become Gate E identity invariants.
>
> Do NOT activate access.
>
> PHASE 7 — REFERENCE CREDENTIAL SAFETY
>
> Read cred-001.demo-access.eth again.
>
> Require its established identity invariants remain unchanged.
>
> Do not require its access state to match any particular value.
>
> Confirm provisioning touched only guest-001 state.
>
> PHASE 8 — FINAL NONCE ACCOUNTING
>
> Record:
>
> baseline latest/pending nonce
> Write 1 nonce if present
> Write 2 nonce if present
> final latest/pending nonce
>
> Require:
>
> - total nonce-consuming writes <= 2
> - no pending transaction
> - no unexplained nonce gap
>
> Expected fresh-state normal case:
>
> 18 → 19 setData
> 19 → 20 register
> final 20/20
>
> But use actual fresh nonce evidence rather than blindly assuming 18 if chain
> state changed before execution.
>
> PHASE 9 — STOP
>
> After successful provisioning/readback:
>
> STOP.
>
> Do NOT:
>
> - run Gate E physical bridge
> - activate access
> - test NFC
> - check replay
> - deactivate
> - modify source
> - update docs
> - commit
> - push
>
> RETURN
>
> # GATE E SECURE CREDENTIAL PROVISIONING
>
> ## REPO STATE
>
> ## FRESH SAFETY CHECK
>
> ## WRITE 1 — ACCESS INITIALIZATION
>
> Include:
> - REQUIRED / NO-OP
> - hash if sent
> - nonce
> - block
> - receipt
> - readback
>
> ## WRITE 2 — REGISTRATION
>
> Include:
> - hash
> - nonce
> - block
> - receipt
> - readback
>
> ## GUEST-001 FINAL STATE
>
> Include:
>
> name
> status
> owner
> tokenId
> resource
> resolver
> registryExpiry
> access.active
> access.validUntil
> authorization
> verification block
>
> ## CRED-001 SAFETY
>
> ## NONCE ACCOUNTING
>
> ## TRANSACTION COUNT
>
> State exact:
> 0 / 1 / 2
>
> Require <= 2.
>
> ## SECURITY
>
> Confirm:
>
> - no private key printed
> - no Privy secret/config printed
> - no cred-001 mutation
> - no activation
> - no deactivation
> - no NFC authorization
> - no extra write
>
> ## GIT STATE
>
> ## NEXT SAFE VALIDATION
>
> State only:
>
> fresh physical Gate E proof against the INACTIVE guest-001 credential should
> be the next step and must require zero blockchain writes.
>
> End exactly:
>
> PROVISIONING: PASS
>
> or
>
> PROVISIONING: STOP — <exact reason>
