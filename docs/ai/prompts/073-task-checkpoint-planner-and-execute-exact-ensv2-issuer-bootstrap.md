# Project task packet 073: TASK — CHECKPOINT PLANNER AND EXECUTE EXACT ENSV2 ISSUER BOOTSTRAP

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — CHECKPOINT PLANNER AND EXECUTE EXACT ENSV2 ISSUER BOOTSTRAP
>
> WHY
>
> The read-only issuer bootstrap plan has passed Control Tower review.
>
> Control Tower explicitly approves:
>
> keysExpiry:
>
> 1814392799
>
> 2027-06-30T21:59:59Z
> 2027-06-30 23:59:59 Europe/Madrid
>
> Expected R1:
>
> 0x0AeB395be893149c0b60D9DAC3Ba55139D0dcA1a
>
> Expected S1:
>
> 0x20766FB21498a99350F922ee3163F5a95F354a7f
>
> Exactly THREE ENSv2 bootstrap writes are authorized, but they MUST be performed
> sequentially with authoritative verification after every receipt.
>
> No other blockchain write is authorized.
>
> ============================================================
> CONTEXT
> ============================================================
>
> Current pushed baseline before planner files:
>
> 1122886a3f689f24d875942b4e36ad2e7d30e53c
>
> Planner result:
>
> BOOTSTRAP PLAN: PASS
>
> Planned writes:
>
> TX1
> Factory.deployProxy UserRegistryImpl -> R1
>
> TX2
> Factory.deployProxy PermissionedResolverImpl -> S1
>
> TX3
> R0.register("keys", I, R1, S1, ROLE_RENEW, 1814392799)
>
> Issuer I:
>
> 0xFa90e8301A22833B74378C5fA3a7c120Ac512685
>
> DEV:
>
> 0x4C60a5AD311510543B56d0408872A52e4AEEe19C
>
> ============================================================
> SCOPE
> ============================================================
>
> AUTHORIZED:
>
> - checkpoint the read-only planner/test first;
> - push that planner checkpoint;
> - fresh read-only baseline;
> - exactly three blockchain writes listed below;
> - receipt/readback verification;
> - safe public evidence collection.
>
> NOT AUTHORIZED:
>
> - any fourth blockchain write;
> - credential creation;
> - staff/visitor/contractor registration;
> - setParent;
> - setSubregistry;
> - setResolver;
> - renew;
> - access.v1 writes;
> - avatar/description writes;
> - role changes beyond the initialization bitmaps below;
> - Android changes;
> - HCE/NFC;
> - firmware;
> - Node verifier changes;
> - automatic transaction retry;
> - exposing secrets.
>
> ============================================================
> PHASE 0 — CHECKPOINT THE PLANNER
> ============================================================
>
> Current intended local files:
>
> scripts/ensv2/issuer-bootstrap-plan.mjs
> scripts/ensv2/issuer-bootstrap-plan.test.mjs
>
> Review them once more for:
>
> - read-only guarantee;
> - no wallet/private-key imports;
> - no broadcast path;
> - no secrets.
>
> Run:
>
> node --test
> git diff --check
>
> If green, commit exactly the planner/test.
>
> Suggested commit:
>
> chore: add ENSv2 issuer bootstrap planner
>
> Push this planner checkpoint to origin/main.
>
> Then require:
>
> git fetch origin
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> Require clean and synchronized before any blockchain write.
>
> If push/sync fails:
>
> STOP.
> NO blockchain write.
>
> ============================================================
> PHASE 1 — FRESH PRE-WRITE BASELINE
> ============================================================
>
> Immediately before TX1 require:
>
> chainId:
> 11155111
>
> fresh block
>
> DEV:
> latest == pending
>
> Expected current DEV nonce from prior plan was:
> 25 / 25
>
> But use the FRESH value, not a hard-coded nonce.
>
> Issuer I:
> latest == pending == 1
>
> keys.demo-access.eth:
> AVAILABLE
>
> Expected predicted addresses still:
>
> R1:
> 0x0AeB395be893149c0b60D9DAC3Ba55139D0dcA1a
>
> S1:
> 0x20766FB21498a99350F922ee3163F5a95F354a7f
>
> Require code size at R1:
> 0
>
> Require code size at S1:
> 0
>
> Require simulations still PASS using the approved expiry:
>
> 1814392799
>
> Legacy snapshot:
>
> guest-001 intact
> cred-001 intact
> demo-access.eth -> R0 intact
>
> If ANY baseline fact differs materially:
>
> STOP before TX1.
>
> ============================================================
> APPROVED ROLE BITMAPS
> ============================================================
>
> R1_ROOT_ROLES:
>
> ROLE_REGISTRAR
> ROLE_REGISTRAR_ADMIN
> ROLE_RENEW
> ROLE_RENEW_ADMIN
>
> Exact planned:
>
> decimal:
> 22301085480897544079999181647255793274257409
>
> hex:
> 0x1000100000000000000000000000000010001
>
> S1_ROOT_ROLES:
>
> ROLE_SET_TEXT
> ROLE_SET_TEXT_ADMIN
> ROLE_SET_DATA
> ROLE_SET_DATA_ADMIN
>
> Exact planned:
>
> decimal:
> 23384026202738964561993972738874522033471931547664
>
> hex:
> 0x100000001000000000000000000000001000000010
>
> KEYS_OWNER_ROLES:
>
> ROLE_RENEW ONLY
>
> decimal:
> 65536
>
> hex:
> 0x10000
>
> No additional role is authorized.
>
> ============================================================
> TX1 — DEPLOY R1
> ============================================================
>
> Authorize exactly:
>
> VerifiableFactory.deployProxy(
>     UserRegistryImpl,
>     approved saltR1,
>     approved initializer
> )
>
> Expected deployed address:
>
> 0x0AeB395be893149c0b60D9DAC3Ba55139D0dcA1a
>
> Use the planner-generated exact calldata.
>
> Do NOT reconstruct fields manually if the planner can generate them.
>
> Submit exactly ONCE.
>
> No automatic retry.
>
> After hash:
>
> wait/reconcile receipt.
>
> If receipt status != success:
>
> STOP.
>
> After receipt require BEFORE TX2:
>
> R1 address ==
> 0x0AeB395be893149c0b60D9DAC3Ba55139D0dcA1a
>
> code exists
>
> Factory.verifyContract(R1) ==
> 0x624a25d67b59d587752ebec8dded8827dae52050
>
> roles(ROOT, issuer I) ==
> R1_ROOT_ROLES exactly
>
> roles(ROOT, DEV) == 0
>
> DEV nonce:
> pre + 1
> latest == pending
>
> If ANY mismatch:
>
> STOP.
>
> DO NOT EXECUTE TX2.
>
> ============================================================
> TX2 — DEPLOY S1
> ============================================================
>
> Only if TX1 acceptance fully passed.
>
> Authorize exactly:
>
> VerifiableFactory.deployProxy(
>     PermissionedResolverImpl,
>     approved saltS1,
>     initialize(
>         I,
>         S1_ROOT_ROLES,
>         []
>     )
> )
>
> Expected address:
>
> 0x20766FB21498a99350F922ee3163F5a95F354a7f
>
> Use planner-generated exact calldata.
>
> Submit exactly ONCE.
>
> No automatic retry.
>
> After hash:
>
> wait/reconcile receipt.
>
> Require BEFORE TX3:
>
> receipt success
>
> S1 address ==
> 0x20766FB21498a99350F922ee3163F5a95F354a7f
>
> code exists
>
> Factory.verifyContract(S1) ==
> 0x9eae5c2730a7dd16bdd1dee6421a1b91e3b0365e
>
> roles(ROOT, I) ==
> S1_ROOT_ROLES exactly
>
> roles(ROOT, DEV) == 0
>
> mined initializer setters == []
>
> DEV nonce:
> previous + 1
> latest == pending
>
> If ANY mismatch:
>
> STOP.
>
> DO NOT EXECUTE TX3.
>
> ============================================================
> TX3 — REGISTER keys.demo-access.eth
> ============================================================
>
> Only if TX1 and TX2 fully passed.
>
> Authorize exactly:
>
> R0.register(
>     "keys",
>     I,
>     R1,
>     S1,
>     65536,
>     1814392799
> )
>
> Where:
>
> I:
> 0xFa90e8301A22833B74378C5fA3a7c120Ac512685
>
> R1:
> 0x0AeB395be893149c0b60D9DAC3Ba55139D0dcA1a
>
> S1:
> 0x20766FB21498a99350F922ee3163F5a95F354a7f
>
> owner roles:
> ROLE_RENEW only
>
> expiry:
> 1814392799
>
> Submit exactly ONCE.
>
> No automatic retry.
>
> ============================================================
> TX3 FINAL ACCEPTANCE
> ============================================================
>
> Require:
>
> receipt success
>
> keys:
> REGISTERED
>
> owner ==
> I
>
> subregistry ==
> R1
>
> resolver ==
> S1
>
> expiry ==
> 1814392799
>
> token-scoped roles(keys, I) ==
> 65536 exactly
>
> No unintended owner roles.
>
> Hierarchy:
>
> demo-access.eth still points to existing R0
>
> Factory provenance:
>
> R1 -> UserRegistryImpl
> S1 -> PermissionedResolverImpl
>
> Legacy:
>
> guest-001 unchanged
> cred-001 unchanged
> S0 unchanged
>
> DEV final nonce:
>
> initial fresh nonce + 3
>
> latest == pending
>
> Issuer nonce remains:
> 1
>
> because issuer performs no bootstrap write.
>
> ============================================================
> FAILURE / UNKNOWN RULE
> ============================================================
>
> If any submitted transaction returns a hash but confirmation becomes UNKNOWN:
>
> STOP.
>
> Do NOT resend.
>
> Reconcile that hash first.
>
> If submission fails before hash:
>
> STOP.
>
> Do NOT automatically retry.
>
> This authorization does NOT include replacement/retry transactions.
>
> ============================================================
> SECURITY
> ============================================================
>
> Never print:
>
> DEV private key
> RPC URL/API key
> raw signed transaction
> environment secrets
>
> Public output allowed:
>
> addresses
> tx hashes
> blocks
> gas
> roles
> expiry
> nonces
>
> ============================================================
> RETURN
> ============================================================
>
> # ENSV2 ISSUER BOOTSTRAP — EXECUTION
>
> ## PLANNER CHECKPOINT
>
> Commit:
> HEAD/origin:
> Clean:
>
> ## PRE-WRITE BASELINE
>
> Block:
> DEV nonce:
> Issuer nonce:
> keys:
> R1 code:
> S1 code:
>
> ## TX1 — R1
>
> Hash:
> Block:
> Gas:
> Receipt:
> Address:
> Implementation:
> Issuer root roles:
> DEV root roles:
> Nonce:
>
> TX1 ACCEPTANCE:
> PASS / STOP
>
> ## TX2 — S1
>
> Hash:
> Block:
> Gas:
> Receipt:
> Address:
> Implementation:
> Issuer root roles:
> DEV root roles:
> Setters:
> Nonce:
>
> TX2 ACCEPTANCE:
> PASS / STOP
>
> ## TX3 — keys.demo-access.eth
>
> Hash:
> Block:
> Gas:
> Receipt:
>
> Status:
> Owner:
> Subregistry:
> Resolver:
> Expiry:
> Owner roles:
>
> Nonce:
>
> TX3 ACCEPTANCE:
> PASS / STOP
>
> ## LEGACY REGRESSION
>
> demo-access.eth -> R0:
> guest-001:
> cred-001:
> S0:
>
> ## FINAL STATE
>
> R1:
> S1:
> keys.demo-access.eth:
> Issuer:
> DEV nonce:
> Issuer nonce:
>
> ## WRITES
>
> Planner checkpoint:
> Git-only
>
> Blockchain writes:
> MUST equal 3 on complete PASS
>
> Any other blockchain writes:
> MUST equal 0
>
> ## SECURITY
>
> Secrets:
> 0
>
> Retries:
> 0
>
> ## GIT
>
> Do NOT yet update product config/docs after writes.
> Do NOT create credentials.
> Do NOT commit post-write documentation yet.
>
> End exactly:
>
> ISSUER BOOTSTRAP: PASS
>
> or:
>
> ISSUER BOOTSTRAP: STOP — <exact reason>
