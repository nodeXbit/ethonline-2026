# Project task packet 074: TASK — CHECKPOINT THE SUCCESSFUL ENSV2 ISSUER BOOTSTRAP

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — CHECKPOINT THE SUCCESSFUL ENSV2 ISSUER BOOTSTRAP
>
> The ENSv2 issuer bootstrap has physically/onchain PASSED.
>
> Do NOT perform any new blockchain write.
>
> Do NOT create any credential yet.
>
> Do NOT modify Android/HCE/firmware/Gate E.
>
> ============================================================
> VERIFIED BOOTSTRAP
> ============================================================
>
> Namespace:
>
> keys.demo-access.eth
>
> Issuer:
>
> 0xFa90e8301A22833B74378C5fA3a7c120Ac512685
>
> R1 UserRegistry:
>
> 0x0AeB395be893149c0b60D9DAC3Ba55139D0dcA1a
>
> R1 implementation:
>
> 0x624a25d67B59D587752EbEc8DdeD8827dAe52050
>
> R1 issuer root roles:
>
> ROLE_REGISTRAR
> ROLE_REGISTRAR_ADMIN
> ROLE_RENEW
> ROLE_RENEW_ADMIN
>
> DEV R1 root roles:
> 0
>
> S1 PermissionedResolver:
>
> 0x20766FB21498a99350F922ee3163F5a95F354a7f
>
> S1 implementation:
>
> 0x9EAe5C2730a7dD16BDD1DeE6421a1B91e3B0365e
>
> S1 issuer root roles:
>
> ROLE_SET_TEXT
> ROLE_SET_TEXT_ADMIN
> ROLE_SET_DATA
> ROLE_SET_DATA_ADMIN
>
> DEV S1 root roles:
> 0
>
> keys.demo-access.eth:
>
> REGISTERED
>
> owner:
>
> 0xFa90e8301A22833B74378C5fA3a7c120Ac512685
>
> subregistry:
>
> 0x0AeB395be893149c0b60D9DAC3Ba55139D0dcA1a
>
> resolver:
>
> 0x20766FB21498a99350F922ee3163F5a95F354a7f
>
> expiry:
>
> 1814392799
>
> owner roles:
>
> ROLE_RENEW only
>
> ============================================================
> BOOTSTRAP TX EVIDENCE
> ============================================================
>
> TX1 — R1
>
> 0x632c6d6f9c04a381f763013bf304d30c15bcccc440b41df9350829cf6e952253
>
> block:
> 11683691
>
> TX2 — S1
>
> 0x2e6e8f830527df1aa384fe09c0a51f27c8e8d7e2b0b3862a9c646aa29d38d94c
>
> block:
> 11683692
>
> TX3 — keys.demo-access.eth
>
> 0x5997d2a1ccbaf146de00dbddef79c49f00fedefd02fdb2399386910a1840230b
>
> block:
> 11683693
>
> Final verification block:
>
> 11683696
>
> DEV final nonce:
> 28 / 28
>
> Issuer nonce:
> 1 / 1
>
> ============================================================
> LEGACY FALLBACK
> ============================================================
>
> Require preserved:
>
> demo-access.eth -> existing R0
>
> guest-001:
> unchanged
>
> cred-001:
> unchanged
>
> S0:
> unchanged
>
> Do not change them.
>
> ============================================================
> PHASE 1 — FRESH READ-ONLY VERIFICATION
> ============================================================
>
> Before editing docs/config, perform a fresh read-only verification.
>
> Require:
>
> Sepolia 11155111
>
> R1 code/provenance correct
>
> S1 code/provenance correct
>
> keys:
> REGISTERED
>
> owner/subregistry/resolver/expiry/roles:
> exactly as above
>
> issuer root roles:
> exact
>
> DEV root roles in R1/S1:
> 0
>
> legacy fallback:
> intact
>
> No pending DEV or issuer transaction.
>
> No secret-bearing RPC output.
>
> ============================================================
> PHASE 2 — AUTHORITATIVE PUBLIC CONFIG
> ============================================================
>
> Create:
>
> config/issuer-space.json
>
> This file is PUBLIC configuration.
>
> It must contain only public reproducibility/trust data.
>
> Suggested data model:
>
> {
>   "version": 1,
>   "chainId": 11155111,
>   "network": "sepolia",
>
>   "namespace": "keys.demo-access.eth",
>   "parentNamespace": "demo-access.eth",
>
>   "issuerAddress": "...",
>
>   "ethRegistry": "...",
>   "parentRegistry": "...",
>   "issuerRegistry": "...",
>   "issuerResolver": "...",
>
>   "verifiableFactory": "...",
>   "userRegistryImplementation": "...",
>   "permissionedResolverImplementation": "...",
>
>   "namespaceExpiry": 1814392799,
>
>   "bootstrap": {
>     "r1DeploymentTx": "...",
>     "r1DeploymentBlock": 11683691,
>     "s1DeploymentTx": "...",
>     "s1DeploymentBlock": 11683692,
>     "namespaceRegistrationTx": "...",
>     "namespaceRegistrationBlock": 11683693
>   },
>
>   "source": {
>     "ensContractsV2Commit":
>       "97a57293f3b4279d94b571e678edb53ce62638f4"
>   }
> }
>
> Adapt field names if repo conventions suggest something clearer.
>
> Do NOT include:
>
> RPC URL
> API key
> private key
> Privy auth data
> local paths
> secret configuration
>
> ============================================================
> PHASE 3 — CONFIG VALIDATION
> ============================================================
>
> Add the smallest read-only validation/test needed so public config cannot drift
> silently.
>
> Prefer reusing the planner/ENS helpers.
>
> Validate at least:
>
> chainId
>
> namespace
>
> R0/R1/S1 addresses
>
> Factory implementation provenance
>
> owner
>
> expiry
>
> R1 issuer roles
>
> S1 issuer roles
>
> keys owner roles
>
> No blockchain writes.
>
> Do not create a broad config framework.
>
> ============================================================
> PHASE 4 — PROJECT DOCUMENTS
> ============================================================
>
> Update:
>
> STATUS.md
> DECISIONS.md
> WORKLOG.md
>
> Update PROJECT.md if the product architecture there is materially stale.
>
> The docs should now clearly state:
>
> - Mobile Issuer Admission PASS.
> - Android Privy issuer is:
>   0xFa90...
> - isolated issuer namespace exists:
>   keys.demo-access.eth
> - R1/S1 are live and controlled by issuer I.
> - DEV performed bootstrap only.
> - legacy guest-001 fallback remains intact.
> - Android is intended primary issuer/holder product surface.
> - physical ESP32/PN532 is reference verifier.
> - next objective is ONE complete credential vertical.
>
> Do not claim staff/visitor/contractor exist yet.
>
> Do not claim final product UI exists yet.
>
> Do not claim physical dynamic credential discovery exists yet.
>
> ============================================================
> PHASE 5 — VALIDATION
> ============================================================
>
> Run:
>
> node --test
> git diff --check
>
> If config validation has a specific command, run it.
>
> Require no blockchain write.
>
> Review diff for secrets.
>
> ============================================================
> PHASE 6 — COMMIT / PUSH
> ============================================================
>
> If validation passes:
>
> Commit the authoritative bootstrap config + documentation.
>
> Suggested commit:
>
> docs: checkpoint ENSv2 issuer namespace
>
> If config/test changes are substantive enough to warrant:
>
> chore: add issuer namespace configuration
>
> plus a docs commit, two commits are acceptable.
>
> Avoid trivial commit fragmentation.
>
> Push main to origin only with the existing repository authorization rules.
>
> If Codex requires explicit push approval, STOP and ask.
>
> After push:
>
> git fetch origin
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
>
> Require clean and synchronized.
>
> ============================================================
> RETURN
> ============================================================
>
> # ENSV2 ISSUER NAMESPACE — CHECKPOINT
>
> ## FRESH ONCHAIN VERIFICATION
>
> ## CONFIG
>
> File:
>
> Key values:
>
> Validation:
>
> ## DOCUMENTATION
>
> Files:
>
> ## TESTS
>
> Node:
>
> diff check:
>
> ## SECURITY
>
> Secrets:
> 0
>
> Blockchain writes:
> 0
>
> ## GIT
>
> Commit(s):
>
> HEAD:
>
> origin/main:
>
> Status:
>
> ## NEXT
>
> Do not implement it.
>
> Describe only the first credential vertical:
>
> staff-001.keys.demo-access.eth
>
> End:
>
> ISSUER NAMESPACE: CHECKPOINTED
>
> or, if push approval is required:
>
> ISSUER NAMESPACE: READY TO PUSH
