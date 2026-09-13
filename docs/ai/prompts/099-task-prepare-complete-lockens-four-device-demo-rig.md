# Project task packet 099: TASK — PREPARE COMPLETE LOCKENS FOUR-DEVICE DEMO RIG

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — PREPARE COMPLETE LOCKENS FOUR-DEVICE DEMO RIG
>
> MISSION
>
> Prepare the complete LockENS demo environment using the four Android devices
> now connected to the PC.
>
> This task should take the project from the current Gate Stand implementation
> to a reproducible final demo configuration.
>
> Work sequentially.
>
> Do NOT open parallel workstreams.
>
> ============================================================
> TARGET DEMO TOPOLOGY
> ============================================================
>
> DEVICE 1:
>
> SEEKER
> LockENS main app
> Holder + Studio / issuer
>
> Expected embedded-wallet roles:
>
> ISSUER
> STAFF HOLDER
> VISITOR HOLDER
> CONTRACTOR HOLDER
>
> DEVICE 2:
>
> FRONT DOOR GATE
>
> DEVICE 3:
>
> LAB GATE
>
> DEVICE 4:
>
> SERVER ROOM GATE
>
> PC:
>
> one local Node authoritative verifier
>
> The PC should NOT be visually required during the final demo.
>
> ============================================================
> IMPORTANT CURRENT STATE
> ============================================================
>
> Remote physical NFC baseline:
>
> ec82f3f578b610ff3978de10d197f1032e449809
>
> Local Android Gate Reader checkpoint:
>
> 734619c
>
> Gate Stand changes remain uncommitted.
>
> Current Gate Stand implementation includes:
>
> - persistent front-door / lab / server-room profiles;
> - no normal-screen resource selector;
> - vertical full-screen UI;
> - central virtual door;
> - six-field security panel;
> - open animation only after Node allowed=true;
> - Node multi-session isolation.
>
> Reported validation:
>
> Android 261/261
>
> Node 238/238
>
> assemble PASS
>
> diff check PASS
>
> Physical Gate Stand regression has NOT yet been performed.
>
> ============================================================
> NON-NEGOTIABLE RULES
> ============================================================
>
> Before any onchain write:
>
> 1. finish Gate Stand physical regression;
> 2. checkpoint/push the validated Gate Stand;
> 3. require clean HEAD == origin/main.
>
> Never mix an uncommitted UI implementation with the demo credential writes.
>
> Use explicit:
>
> adb -s <SERIAL>
>
> for EVERY device-specific command.
>
> Never persist real device serial numbers, ADB IDs or local machine-specific
> paths into tracked repository files.
>
> They may exist under ignored .runtime only.
>
> No secrets.
>
> ============================================================
> PHASE 1 — FOUR-DEVICE INVENTORY
> ============================================================
>
> Run:
>
> adb devices -l
>
> Identify all four devices unambiguously.
>
> Determine which is:
>
> SEEKER
>
> and which three can support:
>
> NFC ReaderMode
> IsoDep
> Gate Stand
>
> Record the LOCAL mapping under ignored:
>
> .runtime/demo-device-map.json
>
> Suggested logical identifiers only:
>
> holder
> frontDoor
> lab
> serverRoom
>
> Do NOT commit physical serial numbers.
>
> For every gate phone verify:
>
> NFC enabled
> ReaderMode available
> IsoDep available
> Gate Reader APK compatible
>
> For Seeker verify:
>
> LockENS installed
> NFC enabled
> current app data preserved
>
> Do not clear any application data.
>
> ============================================================
> PHASE 2 — GATE STAND PHYSICAL REGRESSION FIRST
> ============================================================
>
> Use one known-working gate phone first.
>
> Configure:
>
> LAB
>
> Use existing:
>
> staff-001.keys.demo-access.eth
>
> Expected current chain state:
>
> registration valid
> owner valid
> access.v1 Allowed
> resources.v1 MISSING
>
> Physically execute:
>
> STAFF
> → LAB
>
> Require:
>
> holder proof verified
>
> and final:
>
> ACCESS DENIED
> RESOURCE_POLICY_MISSING
>
> The Gate Stand visual door MUST remain closed.
>
> Open animation shown:
> NO
>
> No blockchain write.
>
> If this regression fails:
>
> STOP.
>
> Do not proceed to demo credential provisioning.
>
> ============================================================
> PHASE 3 — CHECKPOINT GATE STAND
> ============================================================
>
> If physical regression passes:
>
> inspect the complete diff since 734619c.
>
> Validate:
>
> Android
> Node
> assemble
> git diff --check
> secrets
>
> Commit the Gate Stand changes.
>
> Then push BOTH:
>
> 734619c
> +
> Gate Stand commit
>
> to origin/main.
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
> HEAD == origin/main
> ahead/behind 0/0
>
> Record:
>
> DEMO_GATE_BASELINE_SHA
>
> ============================================================
> PHASE 4 — FINAL THREE-GATE VISUAL DESIGN
> ============================================================
>
> Do NOT use external downloaded imagery.
>
> Do NOT add large dependencies.
>
> Use the existing GateDoorView / Android drawing stack to provide three visually
> distinct procedural scenes.
>
> FRONT DOOR
>
> Theme:
>
> modern building entrance / lobby
> daylight
> glass / architectural entrance
> warm interior visible after opening
>
> LAB
>
> Theme:
>
> clean technical laboratory
> white / cool surfaces
> glass or controlled-access lab door
> benches/equipment silhouettes visible after opening
>
> SERVER ROOM
>
> Theme:
>
> secure darker industrial entrance
> reinforced door
> server racks / indicator lights visible after opening
>
> The scenes should feel premium and clearly different even from several feet
> away.
>
> Do not chase photorealism.
>
> Prioritize clarity on a vertical smartphone.
>
> ============================================================
> GATE SECURITY PANEL
> ============================================================
>
> Show only useful information.
>
> Credential
>
> Holder
>
> Registration
>
> Global Access
>
> Resource Access
>
> Proof
>
> Then final:
>
> ACCESS GRANTED
>
> or:
>
> ACCESS DENIED
>
> Technical deny category may appear in smaller text.
>
> Do not show:
>
> namehash
> block numbers
> RPC URLs
> calldata
> raw proof
> wallet provider diagnostics
> internal exceptions
>
> ============================================================
> ANIMATION
> ============================================================
>
> READY:
>
> door closed
>
> VERIFYING:
>
> subtle progress animation
> door remains closed
>
> ALLOW:
>
> ONLY after authoritative Node allowed=true
>
> door visibly opens
> interior environment becomes visible
>
> DENY / ERROR:
>
> door remains closed
>
> No animation may imply physical hardware actuation.
>
> Use wording:
>
> VIRTUAL GATE OPEN
>
> not:
>
> PHYSICAL DOOR OPENED
>
> ============================================================
> PHASE 5 — INSTALL / PROVISION THREE GATES
> ============================================================
>
> Install the final Gate Stand APK on the three gate devices using explicit
> device serials.
>
> Configure persistently:
>
> PHONE A:
> front-door
>
> PHONE B:
> lab
>
> PHONE C:
> server-room
>
> Verify each profile survives Activity restart.
>
> Ensure each device remains suitable for vertical demo use.
>
> Use FLAG_KEEP_SCREEN_ON or current app mechanism if available rather than
> machine-specific hacks.
>
> ============================================================
> PHASE 6 — ONE NODE FOR THREE GATES
> ============================================================
>
> Use the existing localhost-only Node verifier.
>
> All three phones should connect to the same verifier.
>
> Configure adb reverse independently per device to the SAME validated host port,
> if current architecture supports this.
>
> Example conceptually:
>
> front phone:
> reverse device localhost → PC verifier
>
> lab phone:
> reverse device localhost → PC verifier
>
> server phone:
> reverse device localhost → PC verifier
>
> Verify sessions remain isolated by:
>
> gate
> credential
> resource
> challenge
> proof
>
> Prepare ONE reproducible command/script for starting the demo verifier.
>
> Prefer a tracked generic script if useful.
>
> Do not commit:
>
> device serials
> machine paths
> local IPs
>
> If device-specific setup is needed, keep it under .runtime.
>
> ============================================================
> PHASE 7 — DEMO RUNBOOK
> ============================================================
>
> Create a concise local runbook under .runtime for the actual recording day.
>
> It should contain:
>
> 1. connect devices
> 2. start Node
> 3. configure adb reverse
> 4. launch the three gates
> 5. launch Seeker
> 6. verify correct wallets/passes
> 7. exact demo matrix
>
> Do not put personal/device-specific information into tracked docs.
>
> If useful, create a generic tracked script/document with placeholders.
>
> ============================================================
> PHASE 8 — HOLDER WALLET INVENTORY
> ============================================================
>
> On Seeker, READ FIRST.
>
> Inventory current Privy embedded Ethereum wallets.
>
> Determine which actual wallet is:
>
> ISSUER:
> 0xFa90e8301A22833B74378C5fA3a7c120Ac512685
>
> Existing STAFF holder:
> 0x3419148731087b970d2059C53780163B452D5FF7
>
> Verify whether both are available inside the current Privy account.
>
> We want final holder roles:
>
> STAFF
> VISITOR
> CONTRACTOR
>
> Prefer:
>
> existing STAFF wallet for STAFF
>
> and create ONLY as many additional embedded wallets as necessary for:
>
> VISITOR
> CONTRACTOR
>
> Do not create unnecessary wallets.
>
> Wallet creation is a USER-SENSITIVE action.
>
> Prepare the UI and instructions, but STOP for user confirmation if a new wallet
> must actually be created.
>
> After creation, store only public addresses in local ignored demo state.
>
> Do not expose Privy internal identifiers unnecessarily.
>
> ============================================================
> PHASE 9 — FINAL DEMO CREDENTIAL PLAN
> ============================================================
>
> Use existing:
>
> staff-001.keys.demo-access.eth
>
> as STAFF.
>
> Do NOT issue another STAFF credential unless a genuine blocker exists.
>
> STAFF desired final state:
>
> global access:
> Allowed
>
> resources:
> Front Door
> Lab
> Server Room
>
> non-transferable
>
> VISITOR:
>
> choose a fresh, human-readable unused label.
>
> Preferred candidates:
>
> visitor-demo
> visitor-demo-a
> visitor-demo-b
>
> Check BOTH current availability and historical registration events.
>
> Never reuse a historically used label.
>
> Recipient:
> VISITOR holder wallet
>
> global access:
> Allowed
>
> resources:
> Front Door ONLY
>
> transferable:
> YES
>
> description:
> Visitor Pass
>
> Use safe validity well beyond the demo/submission window and below namespace
> expiry.
>
> CONTRACTOR:
>
> fresh unused label.
>
> Preferred candidates:
>
> contractor-demo
> contractor-demo-a
> contractor-demo-b
>
> Recipient:
> CONTRACTOR holder wallet
>
> global access:
> Suspended initially
>
> resources:
> Lab ONLY
>
> transferable:
> NO
>
> description:
> Contractor Access Pass
>
> Use safe validity well beyond the demo/submission window and below namespace
> expiry.
>
> ============================================================
> PHASE 10 — ISSUER PRE-WRITE READINESS
> ============================================================
>
> BEFORE ANY WRITE:
>
> freshly verify:
>
> issuer active wallet
> issuer R1/S1 authority
> issuer nonce
> issuer Sepolia balance
> estimated gas
> namespace expiry
> candidate name availability
> holder addresses
> exact resource IDs
> registration/access expiry values
>
> Estimate whether issuer balance safely covers:
>
> STAFF resource update
> VISITOR TX1 + TX2
> CONTRACTOR TX1 + TX2
>
> with margin.
>
> If insufficient:
>
> STOP.
>
> Do not partially provision the demo accidentally.
>
> ============================================================
> PHASE 11 — STAFF RESOURCE POLICY
> ============================================================
>
> Prepare exact Studio operation:
>
> staff-001
>
> old:
> resources.v1 MISSING
>
> new:
> Front Door
> Lab
> Server Room
>
> This is one:
>
> RESOURCE_POLICY_UPDATE
>
> using the hardened Studio management path.
>
> Require exact human Review.
>
> STOP at final blockchain confirmation.
>
> The USER performs the final confirmation.
>
> After the transaction:
>
> authoritatively verify:
>
> resources.v1 exactly decodes to
> Front Door
> Lab
> Server Room
>
> and no unrelated STAFF field changed.
>
> Record public:
>
> transaction hash
> confirmed block
>
> in local demo evidence.
>
> ============================================================
> PHASE 12 — PHYSICAL STAFF ALLOW
> ============================================================
>
> Immediately test:
>
> STAFF wallet selected
> staff-001 selected
>
> → LAB gate
>
> Require:
>
> credential valid
> holder verified
> registration valid
> global access Allowed
> resource access Lab allowed
> proof valid
>
> final:
>
> ACCESS GRANTED
>
> and visual:
>
> door opens ONLY after authoritative allow
>
> This is the first must-have final-demo proof.
>
> If this fails:
>
> STOP before issuing more credentials unless failure is clearly unrelated.
>
> ============================================================
> PHASE 13 — VISITOR ISSUANCE
> ============================================================
>
> Prepare VISITOR issuance completely in Studio.
>
> We want to reuse THIS REAL issuance as optional video evidence.
>
> Before final confirmation:
>
> - enable Do Not Disturb on Seeker;
> - verify no email / OTP / notification / personal data is visible;
> - prepare optional screen recording if safe.
>
> Do not require the final video to depend on this recording.
>
> The USER performs the actual final confirmations.
>
> TX1:
> register
>
> TX2:
> description
> access.v1
> resources.v1 = Front Door only
> and normal template fields
>
> After each transaction:
>
> authoritative receipt + state validation.
>
> At completion require:
>
> READY
>
> correct owner
> Allowed
> Front Door only
> transferable
>
> If a clean screen recording was captured, preserve it under ignored .runtime.
>
> Do not commit it yet.
>
> ============================================================
> PHASE 14 — CONTRACTOR ISSUANCE
> ============================================================
>
> Same controlled process.
>
> Expected final state:
>
> READY
>
> correct owner
>
> global:
> Suspended
>
> resources:
> Lab
>
> non-transferable
>
> A suspended credential MUST still be a successfully issued READY credential.
>
> ============================================================
> PHASE 15 — FINAL HOLDER CONFIGURATION
> ============================================================
>
> On Seeker verify:
>
> STAFF holder wallet
> → staff-001 selected correctly
>
> VISITOR holder wallet
> → visitor credential selected correctly
>
> CONTRACTOR holder wallet
> → contractor credential selected correctly
>
> Switching wallet must switch/select the correct pass.
>
> No stale HCE session may survive a wallet switch.
>
> Do not require logout/login between actors.
>
> ============================================================
> PHASE 16 — FINAL PHYSICAL MATRIX
> ============================================================
>
> Run the minimum high-value matrix.
>
> A.
>
> STAFF
> → LAB
>
> EXPECTED:
> ACCESS GRANTED
>
> B.
>
> STAFF
> → SERVER ROOM
>
> EXPECTED:
> ACCESS GRANTED
>
> C.
>
> VISITOR
> → LAB
>
> EXPECTED:
> ACCESS DENIED
> RESOURCE_NOT_ALLOWED
>
> D.
>
> VISITOR
> → FRONT DOOR
>
> EXPECTED:
> ACCESS GRANTED
>
> E.
>
> CONTRACTOR
> → LAB
>
> EXPECTED:
> ACCESS DENIED
> ACCESS_SUSPENDED
>
> For every case capture sanitized evidence:
>
> gate
> credential
> holder verified
> global access
> resource access
> proof result
> final decision
>
> Do NOT display/store raw real signatures or proof bytes unnecessarily.
>
> ============================================================
> PHASE 17 — THREE-GATE DRESS REHEARSAL
> ============================================================
>
> Put all three gates into READY state.
>
> Seeker should be the only device moved between them.
>
> Perform the intended video sequence from start to finish.
>
> No terminals should need interaction during the sequence.
>
> PC Node should remain hidden/background.
>
> Measure whether any recovery/manual reset is required.
>
> Return a recommended exact shot order.
>
> ============================================================
> PHASE 18 — FINAL CHECKPOINT AFTER DEMO STATE
> ============================================================
>
> After all matrix tests pass:
>
> update only necessary canonical docs with:
>
> final credential names
> public Sepolia tx hashes where useful
> final policies
> validated demo matrix
>
> Do NOT add:
>
> device serials
> local Windows paths
> personal data
> runtime logs
>
> Run full validation.
>
> Create final demo-system checkpoint and push.
>
> Require clean:
>
> HEAD == origin/main
>
> Record:
>
> DEMO_READY_SHA
>
> ============================================================
> IMPORTANT — NO SCOPE EXPANSION
> ============================================================
>
> Do NOT now implement:
>
> secure production gate enrollment
>
> standalone verifier entirely on Android
>
> VISITOR transfer demo
>
> Studio Permissions delegation
>
> PN532 SCL investigation
>
> PN532 negative-length library fix
>
> new Solidity
>
> backend/cloud server
>
> QR
>
> new wallet functionality
>
> unless one becomes an actual blocker for submission.
>
> ============================================================
> RETURN
> ============================================================
>
> # LOCKENS FOUR-DEVICE DEMO RIG — STATUS
>
> ## DEVICES
>
> Seeker:
>
> Front Door:
>
> Lab:
>
> Server Room:
>
> Do not print real ADB serial numbers in this report.
>
> ## GATE STAND
>
> Physical regression:
>
> DEMO_GATE_BASELINE_SHA:
>
> Front:
>
> Lab:
>
> Server:
>
> ## NODE
>
> Start command:
>
> Three-device isolation:
>
> ## WALLETS
>
> Issuer:
>
> Staff holder:
>
> Visitor holder:
>
> Contractor holder:
>
> Public wallet addresses are acceptable here only if necessary.
>
> ## CREDENTIALS
>
> Staff:
>
> Visitor:
>
> Contractor:
>
> ## POLICIES
>
> | Pass | Global | Front | Lab | Server |
>
> ## BLOCKCHAIN
>
> Staff policy tx:
>
> Visitor TX1:
>
> Visitor TX2:
>
> Contractor TX1:
>
> Contractor TX2:
>
> Issuer final nonce:
>
> No private information.
>
> ## FINAL PHYSICAL MATRIX
>
> STAFF → LAB:
>
> STAFF → SERVER ROOM:
>
> VISITOR → LAB:
>
> VISITOR → FRONT DOOR:
>
> CONTRACTOR → LAB:
>
> ## DRESS REHEARSAL
>
> ## TESTS
>
> Android:
>
> Node:
>
> assemble:
>
> diff:
>
> ## GIT
>
> DEMO_READY_SHA:
>
> HEAD == origin/main:
>
> clean:
>
> ## REMAINING BLOCKERS
>
> Only real blockers to submission.
>
> End exactly:
>
> LOCKENS DEMO RIG READY — FEATURE FREEZE RECOMMENDED
