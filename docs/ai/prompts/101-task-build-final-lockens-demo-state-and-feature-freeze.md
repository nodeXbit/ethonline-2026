# Project task packet 101: TASK — BUILD FINAL LOCKENS DEMO STATE AND FEATURE FREEZE

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — BUILD FINAL LOCKENS DEMO STATE AND FEATURE FREEZE
> MISSION
> Take the current clean, pushed LockENS project from its validated Gate Stand
> baseline to its final demo-ready state.
> This is the LAST feature/provisioning workstream before release audit.
> Work sequentially.
> Do not create parallel workstreams.
> Do not add new product features.
> Current clean pushed baseline:
> FOUR_GATE_BASELINE_SHA
> 7d6f9cab030d16ce28c42afc8f13a25eaa3c588f
> Require first:
> git fetch origin
> git status -sb
> git rev-parse HEAD
> git rev-parse origin/main
> Require:
> clean
> HEAD == origin/main
> ahead/behind 0/0
> If not:
> STOP.
> Existing:
> staff-001.keys.demo-access.eth
> is historical/regression evidence.
> DO NOT modify it for the final demo.
> The final demo should start from three NEW holder wallets and three fresh demo
> credentials.
> All four wallets may live inside the SAME Privy account on the Pixel:
> 1. existing issuer
> 2. new STAFF holder
> 3. new VISITOR holder
> 4. new CONTRACTOR holder
> This intentionally demonstrates LockENS multi-wallet support.
> The holder wallets do not need ETH for holder-proof signing.
> Only the issuer requires Sepolia ETH for credential issuance/configuration.
> Target state:
> STAFF
> fresh label:
> prefer staff-demo
> fallback:
> staff-demo-a / staff-demo-b / timestamp-safe human variant
> owner:
> NEW STAFF HOLDER WALLET
> description:
> Staff Access Pass
> global access:
> Allowed
> resources:
> Front Door
> Lab
> Server Room
> transferability:
> NO
> VISITOR
> fresh label:
> prefer visitor-demo
> owner:
> NEW VISITOR HOLDER WALLET
> description:
> Visitor Pass
> global:
> Allowed
> resources:
> Front Door ONLY
> transferability:
> YES
> CONTRACTOR
> fresh label:
> prefer contractor-demo
> owner:
> NEW CONTRACTOR HOLDER WALLET
> description:
> Maintenance Contractor Pass
> global:
> Suspended
> resources:
> Lab ONLY
> transferability:
> NO
> PHASE 1 — EXISTING WALLET INVENTORY AND ROLE ASSIGNMENT
> ============================================================
>
> IMPORTANT:
>
> The current Privy account already contains 7 embedded Ethereum wallets,
> including the issuer.
>
> DO NOT create additional wallets by default.
>
> First inventory ALL existing embedded Ethereum wallets.
>
> Identify exactly:
>
> ISSUER:
> 0xFa90e8301A22833B74378C5fA3a7c120Ac512685
>
> For every OTHER wallet, perform READ-ONLY classification.
>
> Determine at minimum:
>
> - public address;
> - Sepolia ETH balance;
> - R1 root/token roles where relevant;
> - S1 root/name/key permissions where relevant;
> - whether it currently owns any credential under keys.demo-access.eth;
> - whether LockENS discovery currently associates passes with it;
> - whether any incomplete Studio issuance/management session exists locally;
> - whether it was previously used for a special regression/demo purpose.
>
> Do not perform blockchain writes.
>
> Do not expose Privy internal authentication identifiers.
>
> ============================================================
> HOLDER SELECTION
> ============================================================
>
> Select three existing wallets as:
>
> STAFF HOLDER
> VISITOR HOLDER
> CONTRACTOR HOLDER
>
> Prefer wallets that are:
>
> - NOT the issuer;
> - not privileged managers;
> - currently own no LockENS credentials;
> - have no incomplete transaction/session state;
> - have no special historical purpose;
> - require no ETH for the planned holder-proof demo.
>
> A zero ETH balance is acceptable.
>
> If at least 3 suitable wallets exist:
>
> DO NOT create any new wallet.
>
> If fewer than 3 suitable wallets exist:
>
> STOP and explain exactly why another wallet is necessary before asking the user
> to create one.
>
> ============================================================
> LOCAL ROLE MAP
> ============================================================
>
> Store the selected logical role mapping only under ignored local runtime state,
> for example:
>
> .runtime/final-demo-wallets.json
>
> Allowed:
>
> logical role
> public wallet address
>
> Do NOT store:
>
> private keys
> seed phrases
> auth tokens
> Privy secrets
> device identifiers
>
> The final demo should show only the wallets assigned to:
>
> ISSUER
> STAFF
> VISITOR
> CONTRACTOR
>
> The remaining existing wallets are simply ignored.
>
> ============================================================
> USER CHECKPOINT
> ============================================================
>
> Before any credential issuance, return:
>
> ISSUER:
> <public address>
>
> STAFF HOLDER:
> <public address>
>
> VISITOR HOLDER:
> <public address>
>
> CONTRACTOR HOLDER:
> <public address>
>
> UNUSED EXISTING WALLETS:
> <count only, unless an address is needed to explain a conflict>
>
> and explain why each selected holder wallet is safe to reuse.
>
> STOP for user approval of this role assignment before the first blockchain
> write.
> Switch explicitly back to ISSUER.
> Fresh read:
> issuer roles
> issuer nonce
> issuer Sepolia balance
> R1/S1 state
> namespace expiry
> Estimate total gas for:
> STAFF TX1 + TX2
> VISITOR TX1 + TX2
> CONTRACTOR TX1 + TX2
> plus safety margin.
> If balance is insufficient:
> STOP before issuing anything.
> For each candidate:
> STAFF
> VISITOR
> CONTRACTOR
> check:
> current availability
> historical registration events
> Never reuse a historically used label.
> Freeze exact final names before any transaction.
> Return names to user before first write.
> Use hardened Studio.
> Template:
> STAFF
> Recipient:
> new STAFF holder
> Resources:
> Front Door
> Lab
> Server Room
> Allowed:
> YES
> Transferable:
> NO
> Description:
> Staff Access Pass
> Safe expiry:
> comfortably beyond submission/demo
> and strictly below namespace expiry.
> Prepare exact Review.
> Verify:
> issuer wallet
> recipient full address
> credential name
> registration expiry
> access validity
> resources
> role bitmap
> description
> target contracts
> preflight
> STOP before final Create.
> USER performs final Create confirmation.
> After TX1/TX2:
> authoritative state verification.
> Require READY.
> Switch Pixel to STAFF holder.
> Select new STAFF credential.
> Verify HCE:
> Ready to tap.
> Test:
> STAFF → LAB
> Expected:
> ACCESS GRANTED
> Then:
> STAFF → SERVER ROOM
> Expected:
> ACCESS GRANTED
> Require:
> door opens only after Node allowed=true.
> If STAFF → LAB fails:
> STOP before further issuance unless failure is clearly transient transport and
> evidence shows no policy/state defect.
> Switch to issuer.
> Use hardened Studio.
> VISITOR:
> new VISITOR holder
> Allowed
> Front Door only
> Transferable
> Visitor Pass
> Prepare exact Review.
> STOP for USER final confirmation.
> After issuance verify:
> owner
> registration
> active
> validity
> resources = Front Door exactly
> transferability bit
> description
> READY
> Switch to issuer.
> CONTRACTOR:
> new CONTRACTOR holder
> Suspended
> Lab only
> Non-transferable
> Maintenance Contractor Pass
> Prepare exact Review.
> STOP for USER final confirmation.
> After issuance verify:
> READY
> owner correct
> active=false
> Lab policy
> non-transferable
> description
> READY must not be confused with Allowed.
> Verify physically on Pixel:
> switch to STAFF wallet
> → STAFF pass discovered/selected
> switch to VISITOR wallet
> → VISITOR pass discovered/selected
> switch to CONTRACTOR wallet
> → CONTRACTOR pass discovered/selected
> No stale credential may remain presented by HCE after wallet switch.
> Verify My Keys presentation.
> Do not create extra credentials merely to demonstrate stacked-card UX.
> Record whether the wallet-stack feature is present and healthy but treat
> multi-pass-in-one-wallet physical demonstration as optional.
> Run:
> STAFF → LAB
> expected ACCESS GRANTED
> STAFF → SERVER ROOM
> expected ACCESS GRANTED
> VISITOR → LAB
> expected RESOURCE_NOT_ALLOWED
> VISITOR → FRONT DOOR
> expected ACCESS GRANTED
> CONTRACTOR → LAB
> expected ACCESS_SUSPENDED
> Capture sanitized screenshots/evidence under ignored:
> .runtime/final-demo-evidence/
> For each:
> credential
> gate
> holder verified
> registration
> global access
> resource access
> proof state
> final decision
> No raw proof/signature bytes.
> Do NOT create another credential.
> Prepare two clean demonstrations that can be recorded later:
> A. CREATE WALKTHROUGH
> Open Studio → Create with a fresh unused PREVIEW label.
> Show:
> template
> recipient
> expiry
> access
> resources
> transferability
> description/artwork
> derived full ENS name
> Review
> DO NOT press final Create.
> Then exit safely.
> B. MANAGE WALKTHROUGH
> Open one final demo credential.
> Show available management actions:
> Suspend / Restore
> Access validity
> Resources
> Artwork
> Description
> Extend Registration
> Do NOT submit any management transaction.
> This lets the video prove Studio capability without unnecessary writes.
> Prepare three gates:
> Front Door
> Lab
> Server Room
> All READY.
> Node runs in background.
> Pixel only moves between gates.
> Rehearse intended sequence:
> STAFF wallet
> → Lab GRANTED
> VISITOR wallet
> → Lab DENIED
> same VISITOR
> → Front Door GRANTED
> CONTRACTOR wallet
> → Lab DENIED
> No terminal interaction during sequence.
> Record:
> average interaction time
> any manual recovery
> any stale HCE/session issue
> If instability appears:
> fix only a demonstrated blocker.
> No UX expansion.
> Update canonical docs only with public, useful final state:
> final demo credential names
> public holder wallet addresses if genuinely useful
> public tx hashes
> policy matrix
> physical validation matrix
> Never include:
> ADB serials
> private/local IPs
> absolute local paths
> emails
> Privy IDs
> runtime logs
> Run:
> Android tests
> assembleDebug
> Node tests
> firmware builds if still part of release validation
> git diff --check
> Commit/push final demo state/docs.
> Require:
> clean
> HEAD == origin/main
> Record:
> DEMO_READY_SHA
> Create/update a clear project status statement:
> FEATURE FREEZE
> No new product functionality before submission.
> Permitted after freeze:
> bug fixes required for submission
> security/privacy sanitation
> README/docs
> screenshots/diagrams
> video/submission materials
> Not permitted:
> new product features
> new sponsor integrations
> new protocol changes
> new gate architecture
> new wallet features
> Do NOT:
> modify historical staff-001
> create extra demo credentials
> implement Studio Permissions
> demonstrate VISITOR transfer
> change PN532
> investigate SCL
> add standalone Android authorization
> add backend/cloud service
> add Solidity
> add sponsor features
> Issuer:
> Staff holder:
> Visitor holder:
> Contractor holder:
> Only public addresses.
> Staff:
> Visitor:
> Contractor:
> Staff TX1:
> Staff TX2:
> Visitor TX1:
> Visitor TX2:
> Contractor TX1:
> Contractor TX2:
> | Pass | Global | Front | Lab | Server | Transferable |
> STAFF → LAB:
> STAFF → SERVER ROOM:
> VISITOR → LAB:
> VISITOR → FRONT DOOR:
> CONTRACTOR → LAB:
> Wallet switching:
> Pass discovery:
> HCE invalidation:
> Wallet stack feature:
> Create walkthrough:
> READY / NOT READY
> Manage walkthrough:
> READY / NOT READY
> No extra writes:
> YES / NO
> Android:
> Node:
> assemble:
> firmware:
> diff:
> DEMO_READY_SHA:
> HEAD == origin/main:
> clean:
> ACTIVE / NOT ACTIVE
> End exactly:
> LOCKENS DEMO READY — FEATURE FREEZE ACTIVE
