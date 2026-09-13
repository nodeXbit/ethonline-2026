# Project task packet 012: TASK — Review, checkpoint, document and push the completed Gate A holder-proof

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Review, checkpoint, document and push the completed Gate A holder-proof
> milestone.
>
> The user explicitly authorizes:
>
> * read-only review/validation
> * committing the two scoped Gate A implementation files
> * scoped project-state documentation updates described below
> * committing those documentation updates
> * pushing the resulting commits to origin/main
>
> Do NOT implement Gate B.
> Do NOT install Android tooling.
> Do NOT modify Gate A unless review finds a concrete defect.
>
> CURRENT VERIFIED STATE
>
> Expected base checkpoint:
>
> 676e12766e3a1928664aba8b55000a6ffdd67b38
>
> Current uncommitted Gate A files:
>
> scripts/security/holder-proof.mjs
> scripts/security/holder-proof.test.mjs
>
> Verified result:
>
> * baseline full suite: 62 passed
> * new Gate A tests: 17 passed
> * final full suite: 79 passed
> * canonical full test command:
>   node --test --test-isolation=none
> * git diff --check: PASS
> * no dependencies changed
> * no environment/private-key contents accessed
> * zero blockchain writes
>
> SECURITY MODEL VERIFIED
>
> Server-issued EIP-712:
>
> domain:
> name: ENSv2 Access
> version: 1
> chainId: 11155111
>
> AccessChallenge:
> bytes32 credential
> bytes32 resource
> bytes32 nonce
> uint64 expiresAt
>
> Verifier:
>
> server-issued PENDING challenge
> → canonical typed data
> → recover signer
> → signer must equal CURRENT ENSv2 owner
> → consume challenge exactly once
> → THEN evaluate existing ENS authorization through isAuthorized
> → ALLOW / DENY.
>
> Important one-shot rule:
>
> valid current holder + ENS policy DENY
> → challenge remains CONSUMED
> → later ENS state change cannot make the same proof reusable.
>
> PRE-COMMIT REVIEW
>
> 1. Confirm:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> Require:
>
> HEAD == origin/main == 676e12766e3a1928664aba8b55000a6ffdd67b38
>
> with only the two expected untracked Gate A files.
>
> 2. Read both Gate A files completely.
>
> Review specifically:
>
> * canonical typed data comes only from stored challenge
> * production nonce uses secure Node randomness
> * no client-defined domain/message is trusted
> * current owner is checked at verification time
> * wrong signer does not consume
> * malformed/invalid proof does not consume
> * valid current-holder proof DOES consume before ENS policy evaluation
> * replay after policy DENY is impossible
> * PENDING → CONSUMED is exactly-once within the in-memory model
> * existing isAuthorized helper is reused
> * no copied second ENS policy exists
> * no real secret/test credential is present
> * test private keys are clearly TEST ONLY
> * no hidden blockchain write path exists
>
> If a real security defect is found:
> STOP.
> Do not commit.
>
> Do not perform stylistic refactors.
>
> 3. Rerun:
>
> node --test --test-isolation=none
>
> Require:
>
> 79 passed
> 0 failed
>
> Also run relevant syntax validation, git diff/check validation and safe secret
> scan.
>
> IMPLEMENTATION COMMIT
>
> If review passes, stage ONLY:
>
> scripts/security/holder-proof.mjs
> scripts/security/holder-proof.test.mjs
>
> Commit:
>
> feat: add EIP-712 holder proof verifier
>
> Do not include documentation in this commit.
>
> DOCUMENTATION UPDATE
>
> After implementation commit, update project-state documentation locally.
>
> Modify only where materially necessary:
>
> STATUS.md
> WORKLOG.md
> DECISIONS.md
>
> Do not modify README.md yet.
> Do not modify PROJECT.md unless an existing statement becomes factually wrong.
>
> STATUS.md
>
> Record Gate A as PASS:
>
> * server-issued EIP-712 challenges
> * current-owner signature verification
> * replay protection
> * one-shot consumption before ENS policy evaluation
> * composition with existing isAuthorized
> * 79-test full suite
>
> Set current/next technical objective to:
>
> Gate B — prove native Privy Android embedded EOA can sign the exact
> ENSv2 Access EIP-712 challenge and Node/viem recovers the same wallet address.
>
> Record known Gate B prerequisites:
>
> * Android toolchain is currently missing
> * native Privy Android typed-data support remains empirically unverified
>
> Do not claim Android/NFC holder proof works yet.
>
> WORKLOG.md
>
> Add concise evidence for Gate A including:
>
> * 62-test baseline clarified
> * 17 holder-proof tests
> * 79 final tests
> * server-issued challenge model
> * current ENS owner comparison
> * valid-holder proof consumes even if ENS policy denies
> * replay after access-state change rejected
> * no dependencies or blockchain writes
>
> Capture the technical learning:
>
> authentication proof and authorization policy are separate:
> EIP-712 proves wallet control; ENSv2 remains authoritative for ownership and
> access.
>
> DECISIONS.md
>
> Record the accepted holder-proof v1 decisions:
>
> * EIP-712 domain ENSv2 Access v1 on Sepolia
> * credential = ENS namehash
> * resource binding
> * verifier-generated random nonce
> * short expiry
> * server-issued challenges only
> * current ENS owner evaluated at verification
> * successful holder proof is one-shot
> * consume before later ENS policy outcome
> * existing ENS access policy remains source of truth
> * in-memory challenge store is sufficient for current hackathon verifier
> * static NFC UID must not be an authorization factor in the future secure path
>
> Clearly state that Android/Privy/HCE are not yet implemented.
>
> DOCUMENTATION VALIDATION
>
> Before documentation commit:
>
> * inspect documentation diff
> * git diff --check
> * confirm no secrets/API keys/endpoints
> * confirm no environment files staged
> * confirm implementation files are unchanged after their first commit
>
> DOCUMENTATION COMMIT
>
> Commit:
>
> docs: record holder proof Gate A
>
> PUSH
>
> Push both new commits to origin/main.
>
> FINAL VERIFICATION
>
> Run:
>
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> Require:
>
> working tree clean
> HEAD == origin/main
>
> RETURN
>
> # GATE A CHECKPOINT
>
> ## REVIEW
>
> ## IMPLEMENTATION COMMIT
>
> ## DOCUMENTATION COMMIT
>
> ## PUSH
>
> ## FINAL TESTS
>
> ## FINAL GIT STATE
>
> ## NEXT OBJECTIVE
>
> Include both new commit SHAs and pushed range.
>
> Confirm explicitly:
>
> * 79 tests pass
> * working tree clean
> * local main == origin/main
> * no dependency added
> * no secret committed
> * no blockchain write occurred
> * Android/Privy/NFC were not implemented
>
> End exactly:
>
> GATE A CHECKPOINT: PASS
>
> or
>
> GATE A CHECKPOINT: STOP — <reason>
