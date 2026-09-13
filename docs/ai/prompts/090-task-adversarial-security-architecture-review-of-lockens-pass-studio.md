# Project task packet 090: TASK — ADVERSARIAL SECURITY / ARCHITECTURE REVIEW OF LOCKENS PASS STUDIO

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — ADVERSARIAL SECURITY / ARCHITECTURE REVIEW OF LOCKENS PASS STUDIO
>
> MODEL EXPECTATION
>
> This is a high-risk pre-write adversarial review.
>
> DO NOT implement anything.
>
> DO NOT modify any file.
>
> DO NOT perform blockchain writes.
>
> DO NOT sign anything.
>
> DO NOT create any credential.
>
> DO NOT commit or push.
>
> The purpose is to try to prove the new generic Pass Studio UNSAFE or
> architecturally incorrect before the first real Studio transaction.
>
> Be adversarial.
>
> Do not merely confirm the implementation because tests pass.
>
> ============================================================
> CURRENT STATE
> ============================================================
>
> Last pushed clean baseline:
>
> 09e8153a01bca6283a773d41c77572442b742337
>
> The worktree now intentionally contains the uncommitted LockENS Pass Studio
> implementation.
>
> Current implementation report:
>
> - Android tests: 179 passed
> - Node tests: 173 passed
> - assembleDebug: PASS
> - git diff --check: PASS
> - no blockchain writes
> - no signatures
> - no HCE/NFC changes
> - no Node/firmware changes
>
> Do NOT require a clean worktree: the Pass Studio diff is intentionally
> uncommitted.
>
> First reconstruct the exact local state and inspect every changed file.
>
> ============================================================
> AUTHORITATIVE ENSV2 DEPLOYMENT
> ============================================================
>
> Chain:
>
> Sepolia
> 11155111
>
> Namespace:
>
> keys.demo-access.eth
>
> Issuer/root-controller wallet:
>
> 0xFa90e8301A22833B74378C5fA3a7c120Ac512685
>
> R1 UserRegistry:
>
> 0x0AeB395be893149c0b60D9DAC3Ba55139D0dcA1a
>
> S1 PermissionedResolver:
>
> 0x20766FB21498a99350F922ee3163F5a95F354a7f
>
> Namespace expiry:
>
> 1814392799
>
> Pinned ENS contracts-v2 source commit:
>
> 97a57293f3b4279d94b571e678edb53ce62638f4
>
> Existing real regression credential:
>
> staff-001.keys.demo-access.eth
>
> Owner:
>
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> It MUST remain untouched.
>
> ============================================================
> AUDIT MISSION
> ============================================================
>
> Attempt to find:
>
> - authorization flaws;
> - wrong ENSv2 role assumptions;
> - incorrect ABI assumptions;
> - cross-credential state contamination;
> - cross-wallet state contamination;
> - duplicate-write possibilities;
> - unsafe retry paths;
> - wrong transaction recovery;
> - stale-state authorization;
> - incorrect expiry semantics;
> - incorrect transferability semantics;
> - incorrect lifecycle semantics;
> - wrong current-owner assumptions;
> - incorrect resolver authority;
> - UI/review mismatches that could cause irreversible wrong writes;
> - read-after-write race regressions;
> - integer/timestamp problems;
> - hidden hard-coded STAFF assumptions;
> - privilege escalation;
> - capability false positives;
> - unsafe management of credentials not owned by the manager;
> - anything that can make the UI say one thing while calldata does another.
>
> The review must distinguish:
>
> PROVEN BUG
>
> PROVEN SECURITY ISSUE
>
> DESIGN RISK
>
> PRODUCT LIMITATION
>
> FALSE POSITIVE / SAFE BY DESIGN
>
> ============================================================
> PHASE 1 — DIFF / ARCHITECTURE RECONSTRUCTION
> ============================================================
>
> Inspect every change since:
>
> 09e8153a01bca6283a773d41c77572442b742337
>
> Understand exactly:
>
> - PassDraft model;
> - templates;
> - generic issuance;
> - transaction/session identity;
> - capability model;
> - Studio navigation;
> - Manage discovery;
> - management actions;
> - ABI extensions;
> - recovery paths;
> - UI review/submission boundaries.
>
> Do not infer behavior from the implementation report alone.
>
> ============================================================
> PHASE 2 — PINNED ENSV2 SOURCE AUDIT
> ============================================================
>
> Using the locally pinned/source-referenced ENSv2 contracts at:
>
> 97a57293f3b4279d94b571e678edb53ce62638f4
>
> reconfirm exact semantics for every Studio operation.
>
> Audit at minimum:
>
> R1.register
>
> R1.renew
>
> R1 roles / root roles / token roles
>
> PermissionedResolver setText
>
> PermissionedResolver setData
>
> PermissionedResolver multicall
>
> role authorization
>
> ROLE_CAN_TRANSFER_ADMIN
>
> admin-role semantics
>
> tokenId/version behavior
>
> expiry behavior
>
> transfer behavior
>
> resolver permission scoping
>
> Do NOT trust comments in our Android code if pinned source differs.
>
> ============================================================
> PHASE 3 — TRANSFERABILITY ADVERSARIAL REVIEW
> ============================================================
>
> The implementation expects:
>
> non-transferable:
> roleBitmap = 0
>
> transferable:
> ROLE_CAN_TRANSFER_ADMIN
> bit 156
>
> Prove or disprove this against pinned source.
>
> Answer:
>
> 1. Does assigning ROLE_CAN_TRANSFER_ADMIN at registration to the owner actually
>    make the credential transferable in the intended way?
>
> 2. Does it unintentionally grant another privilege?
>
> 3. Can transferability later be changed with our current authorities?
>
> 4. What happens to relevant permissions on transfer?
>
> 5. Are there delegated-permission persistence hazards?
>
> 6. Is using transferability only at issuance the correct product model?
>
> Treat any uncertainty here as blocking before VISITOR issuance.
>
> ============================================================
> PHASE 4 — GENERIC ISSUANCE IDENTITY / ISOLATION
> ============================================================
>
> The old implementation originated as a single hard-coded staff issuance.
>
> Adversarially prove that the new system cannot confuse:
>
> credential A
> with
> credential B.
>
> Test/reason about:
>
> A draft reviewed for A then edited to B.
>
> A failed/pending A operation while B is opened.
>
> Confirmed TX1 A + records pending, then user creates B.
>
> App restart while A is incomplete.
>
> Wallet switch while issuance is incomplete.
>
> Template switch after Review.
>
> Artwork/description changes after Review.
>
> Same label with different case/normalization.
>
> Two different labels resulting in unexpected same node/token identity.
>
> Operation IDs/session keys.
>
> Transaction journal lookup.
>
> Recovery lookup.
>
> No operation/hash for one credential may ever authorize or advance another.
>
> ============================================================
> PHASE 5 — ACTIVE WALLET / PROVIDER ISOLATION
> ============================================================
>
> The app now supports multiple Privy wallets.
>
> Prove:
>
> - active wallet shown in UI == wallet provider used for submission;
> - preflight wallet == submitted wallet;
> - wallet cannot change silently between Review and final confirmation;
> - operation journal wallet identity is enforced;
> - recovery cannot continue using a different active wallet;
> - capability of wallet A cannot authorize a write submitted by wallet B;
> - wallet switching during incomplete operations is safely handled.
>
> Consider race conditions around UI wallet selection and async callbacks.
>
> Any realistic cross-wallet submission possibility is P0.
>
> ============================================================
> PHASE 6 — CAPABILITY / AUTHORIZATION AUDIT
> ============================================================
>
> Audit independently:
>
> canIssue
> canRenew
> canManageAccess
> canManagePresentation
>
> Check exact current roles necessary for each operation.
>
> Prove Studio does not rely merely on configured issuer address.
>
> Evaluate:
>
> - ALLOWED
> - DENIED
> - UNAVAILABLE
>
> and whether stale capability state can remain actionable.
>
> Final confirmation must re-check authority.
>
> Review-only visibility is not sufficient security.
>
> Management of a pass NOT owned by the manager is intentional if EAC authority
> permits it.
>
> Prove this is correct against S1/R1 semantics.
>
> ============================================================
> PHASE 7 — EXPIRY SEMANTICS
> ============================================================
>
> Adversarially inspect both:
>
> registrationExpiry
> accessValidUntil
>
> Prove:
>
> issuance:
> future
> registrationExpiry < namespaceExpiry
> accessValidUntil <= registrationExpiry
>
> renew:
> newExpiry > currentExpiry
> newExpiry < namespaceExpiry
> cannot reduce
>
> access-validity management:
> cannot silently exceed registration expiry
>
> restore:
> cannot present expired access as active/currently valid
>
> suspend:
> must preserve intended validUntil
>
> renew:
> must NOT silently extend accessValidUntil
>
> Timezone / milliseconds / seconds conversion:
>
> look for:
> - local-time ambiguity;
> - DST problems;
> - milliseconds accidentally passed as seconds;
> - BigInteger -> Long overflow;
> - float conversion;
> - off-by-one boundary errors.
>
> Use exact integers.
>
> ============================================================
> PHASE 8 — INITIAL SUSPENDED CREDENTIAL
> ============================================================
>
> CONTRACTOR template is initially:
>
> accessActive = false
>
> Audit whether:
>
> TX1 registration
> +
> TX2 resolver records
> +
> authoritative readback
>
> correctly reaches a terminal READY state whose product status is Suspended,
> not failed/incomplete.
>
> Ensure "READY" means:
>
> issuance fully configured
>
> not:
>
> access Allowed.
>
> A valid suspended credential must be a successful issuance.
>
> ============================================================
> PHASE 9 — MANAGEMENT ACTIONS
> ============================================================
>
> Audit:
>
> SUSPEND
>
> RESTORE
>
> CHANGE ACCESS VALIDITY
>
> CHANGE ARTWORK
>
> REMOVE ARTWORK
>
> CHANGE DESCRIPTION
>
> COMBINED PRESENTATION UPDATE
>
> RENEW REGISTRATION
>
> For each determine:
>
> - exact target;
> - exact ABI;
> - exact authority;
> - exact preflight;
> - pure Review;
> - final confirmation boundary;
> - transaction journal type;
> - no-hash behavior;
> - hash persistence;
> - receipt reconciliation;
> - minimum receipt-block readback;
> - restart recovery;
> - final authoritative invariant.
>
> Ensure management action A can never be mistaken for action B.
>
> ============================================================
> PHASE 10 — ARTWORK / DESCRIPTION SAFETY
> ============================================================
>
> Audit difference between:
>
> - blank because user intentionally removes artwork;
> - blank because image loading failed;
> - blank because input was not loaded;
> - unchanged artwork.
>
> A display/load failure MUST NEVER cause an onchain Remove Artwork operation.
>
> Review must show exact old → new value.
>
> Description update must not accidentally clear avatar and vice versa.
>
> ============================================================
> PHASE 11 — MANAGE DISCOVERY / TOKEN IDENTITY
> ============================================================
>
> Studio Manage uses known-R1 event discovery.
>
> Audit:
>
> - historical labels as discovery candidates;
> - current state as authority;
> - expired credentials;
> - transferred credentials;
> - tokenId regeneration / label reuse assumptions;
> - duplicates;
> - stale cached candidates;
> - current resolver/provenance.
>
> We intentionally do NOT support unregister/reissue.
>
> Prove current discovery assumptions remain safe under that scope.
>
> Do not treat tokenId as permanent credential identity if ENSv2 does not
> guarantee it.
>
> ============================================================
> PHASE 12 — TRANSACTION ENGINE INTEGRATION
> ============================================================
>
> The existing RecoverableTransactionEngine is security-critical.
>
> Verify the new Studio did NOT create a weaker parallel write path.
>
> Every Studio write must retain:
>
> explicit human review
>
> fresh preflight at final confirmation
>
> SUBMITTING_NO_HASH persisted before provider send
>
> one provider-send attempt per operation
>
> hash persisted immediately
>
> no blind retry after hash
>
> NO_BROADCAST_PROVEN only after nonce evidence
>
> UNKNOWN reconciliation
>
> receipt validation
>
> original tx intent validation
>
> receipt-block-aware final readback
>
> restart recovery
>
> confirmed finalization idempotency
>
> Check management actions as strictly as ISSUE_REGISTER / ISSUE_CONFIGURE.
>
> ============================================================
> PHASE 13 — REVIEW VS CALLDATA CONSISTENCY
> ============================================================
>
> For every type of write:
>
> what the user sees in Review
>
> must exactly correspond to:
>
> what is encoded and submitted.
>
> Look for:
>
> editable field not frozen at final confirmation
>
> old preview with new calldata
>
> template state changing under Review
>
> timezone mismatch
>
> truncated address masking a different exact address
>
> wrong artwork
>
> wrong active flag
>
> wrong validUntil
>
> wrong role bitmap
>
> wrong name
>
> wrong wallet
>
> Management reviews must show old → new where appropriate.
>
> ============================================================
> PHASE 14 — READ-ONLY LIVE SIMULATION
> ============================================================
>
> Using safe current Sepolia reads/simulations only:
>
> NO broadcasts.
>
> Use the current root-controller wallet address as `from` where simulation needs
> an account.
>
> Choose clearly fresh synthetic labels such as:
>
> astra-staff-audit-<safe suffix>
> astra-visitor-audit-<safe suffix>
> astra-contractor-audit-<safe suffix>
>
> Do NOT register them.
>
> For each template:
>
> - construct exact draft;
> - derive exact full name;
> - verify AVAILABLE;
> - encode TX1;
> - simulate TX1 if faithfully possible;
> - verify role bitmap;
> - construct TX2;
> - simulate the strongest faithfully possible configuration path.
>
> If TX2 cannot be faithfully simulated before registration, explain rather than
> pretending PASS.
>
> Also simulate/read-only validate management operations on existing staff-001
> where possible:
>
> - suspend
> - presentation update
> - renew
>
> Do not mutate state.
>
> ============================================================
> PHASE 15 — PROPERTY / ADVERSARIAL TESTING
> ============================================================
>
> Do not modify tests, but you MAY run temporary/read-only reasoning scripts under
> ignored .runtime if useful.
>
> Try edge cases:
>
> label:
> empty
> uppercase
> hyphen boundaries
> dot
> Unicode if unsupported
> max length
>
> recipient:
> zero
> invalid checksum / mixed case
> different active wallet
>
> expiry:
> now
> now+1
> namespace expiry
> namespace expiry+1
> access > registration
>
> template switches
>
> very long description/artwork URI
>
> wallet switch
>
> RPC unavailable
>
> capability unavailable
>
> pending nonce
>
> stale read block
>
> unknown tx
>
> duplicate confirmation callback
>
> restart at every transaction state
>
> Do not exhaustively fuzz for hours; timebox toward actionable findings.
>
> ============================================================
> PHASE 16 — EXISTING STAFF REGRESSION
> ============================================================
>
> Fresh read-only verify that:
>
> staff-001
>
> remains exactly:
>
> REGISTERED
>
> correct owner
>
> correct resolver
>
> correct expiry
>
> roles 0
>
> non-transferable
>
> description:
> Staff Access Pass
>
> avatar:
> empty
>
> access:
> active
>
> and that the new generic Studio interprets it correctly.
>
> No write.
>
> ============================================================
> PHASE 17 — SECURITY / PRIVACY REVIEW
> ============================================================
>
> Look for:
>
> private keys
>
> auth tokens
>
> OTP/email leakage
>
> RPC secrets
>
> raw signed transactions
>
> raw holder proof/signature
>
> unsafe URI logging
>
> untrusted exception disclosure
>
> wallet-provider mixups
>
> No secrets should be persisted or logged.
>
> ============================================================
> PHASE 18 — PRODUCT / BOUNTY ARCHITECTURE REVIEW
> ============================================================
>
> Assess whether Pass Studio now truthfully demonstrates ENSv2 rather than merely
> wrapping hard-coded values.
>
> Evaluate visible use of:
>
> - hierarchy;
> - custom UserRegistry;
> - PermissionedResolver;
> - EAC capability gating;
> - ownership;
> - expiry/renew;
> - transferable vs non-transferable issuance;
> - access state;
> - metadata lifecycle.
>
> Identify any HIGH-value ENSv2 capability that is already architecturally
> available but currently invisible and would materially improve the demo with
> small risk.
>
> Do NOT recommend broad feature expansion merely for checkbox coverage.
>
> ============================================================
> FINDING FORMAT
> ============================================================
>
> For every material finding:
>
> ID:
>
> Severity:
> P0 / P1 / P2 / P3
>
> Category:
> SECURITY
> CORRECTNESS
> RECOVERY
> AUTHORIZATION
> ENS SEMANTICS
> UX TRUST
> PRODUCT
> TEST GAP
>
> Evidence:
>
> Exploit/failure scenario:
>
> Impact:
>
> Required before real write:
> YES / NO
>
> Smallest recommended fix:
>
> Do NOT implement it.
>
> ============================================================
> RETURN
> ============================================================
>
> # LOCKENS PASS STUDIO — ASTRA ADVERSARIAL REVIEW
>
> ## EXECUTIVE VERDICT
>
> REAL STUDIO WRITES:
>
> GO
> GO WITH REQUIRED FIXES
> STOP
>
> Why:
>
> ## PINNED ENSV2 SEMANTICS
>
> Register:
>
> Renew:
>
> Transferability:
>
> Resolver:
>
> EAC:
>
> ## P0 FINDINGS
>
> ## P1 FINDINGS
>
> ## P2/P3 FINDINGS
>
> ## CROSS-CREDENTIAL ISOLATION
>
> ## MULTI-WALLET ISOLATION
>
> ## CAPABILITY AUTHORIZATION
>
> ## EXPIRY / TIME
>
> ## TEMPLATES
>
> STAFF:
>
> VISITOR:
>
> CONTRACTOR:
>
> ## GENERIC ISSUE
>
> TX1:
>
> TX2:
>
> Recovery:
>
> ## MANAGEMENT
>
> Suspend:
>
> Restore:
>
> Access validity:
>
> Artwork:
>
> Description:
>
> Renew:
>
> ## TRANSACTION SAFETY
>
> ## REVIEW ↔ CALLDATA CONSISTENCY
>
> ## READ-ONLY LIVE SIMULATIONS
>
> Staff synthetic:
>
> Visitor synthetic:
>
> Contractor synthetic:
>
> Existing staff management simulations:
>
> ## EXISTING STAFF REGRESSION
>
> ## SECURITY
>
> Secrets:
>
> Provider/wallet isolation:
>
> ## BOUNTY / ENSV2 FIT
>
> Already strong:
>
> Highest-value remaining ENSv2 feature:
>
> Do NOT implement it.
>
> ## REQUIRED FIX SET BEFORE REAL WRITES
>
> Only P0/P1 items that truly block controlled tests.
>
> ## OPTIONAL AFTER FIRST 3 PASSES
>
> ## GIT
>
> Files modified:
> NONE
>
> Commit:
> NONE
>
> Push:
> NONE
>
> ## BLOCKCHAIN
>
> Writes:
> 0
>
> Signatures:
> 0
>
> End exactly:
>
> ASTRA REVIEW COMPLETE — NO IMPLEMENTATION PERFORMED
