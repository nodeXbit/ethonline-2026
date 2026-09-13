# Project task packet 106: MISSION — FINAL DEMO RUNTIME RESTORE

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> MISSION — FINAL DEMO RUNTIME RESTORE
>
> This is an OPERATIONAL DIAGNOSIS / VALIDATION task.
>
> FEATURE FREEZE remains active.
>
> DO NOT modify tracked files.
> DO NOT modify Git history.
> DO NOT commit.
> DO NOT push.
> DO NOT install anything.
> DO NOT perform blockchain writes.
> DO NOT create, revoke, suspend, transfer, or modify credentials.
> DO NOT change ENSv2 state.
> DO NOT rotate secrets.
> DO NOT expose local secrets, RPC credentials, device serials, MAC addresses, private IPs, emails, or personal paths.
>
> You MAY:
>
> * inspect local repository files;
> * inspect private local runtime scripts/configuration without printing sensitive values;
> * execute read-only diagnostics;
> * start/stop the existing local demo runtime/processes when required;
> * use existing ADB connections;
> * perform normal offchain holder proof / NFC demo interactions;
> * read Sepolia/RPC state;
> * give the human exact physical/UI actions to perform.
>
> ============================================================
> CONTEXT
> =======
>
> Exact release candidate:
>
> d60572284854c005ab2d7c9f90f62da82ffc56b3
>
> Expected repository state:
>
> HEAD == origin/main
> clean worktree
> FEATURE FREEZE ACTIVE
>
> Final documented demo previously passed:
>
> * physical NFC demo matrix
> * 7 credentials
> * 14 authorized Sepolia transactions already completed
> * Android tests/build PASS
> * Node 238/238
> * dynamic + legacy firmware compile
> * wallet switching/discovery/HCE invalidation PASS
>
> The user has now manually opened the LockENS apps and reports:
>
> NONE OF THE GATES CURRENTLY OPEN.
>
> Do not assume this is a code regression.
>
> Likely operational dependencies include:
>
> Android holder / HCE
> ↕
> Android gate readers
> ↕
> ADB / local connectivity
> ↕
> Node authoritative gate service on Windows
> ↕
> RPC
> ↕
> ENSv2 Sepolia
>
> The final demo uses:
>
> * one Android holder;
> * three Android NFC gate stands;
> * Node running on the PC as authoritative verifier;
> * existing demo credentials;
> * existing onchain state.
>
> ============================================================
> SOURCE PRIORITY
> ===============
>
> 1. Current LOCAL repository and runtime state.
> 2. FINAL_DEMO_STATE.md
> 3. STATUS.md
> 4. Current relevant runtime/startup documentation.
> 5. Existing local scripts under ignored/private runtime directories.
> 6. README only where consistent with the final demo state.
>
> If documentation conflicts, report the discrepancy.
>
> ============================================================
> TASK 1 — VERIFY FROZEN REPOSITORY STATE
> =======================================
>
> READ ONLY.
>
> Verify:
>
> * git status -sb
> * HEAD
> * origin/main
>
> Expected:
>
> d60572284854c005ab2d7c9f90f62da82ffc56b3
>
> If the tracked repository is dirty, ahead, behind or divergent:
>
> STOP.
>
> Ignored runtime/log/build files are not by themselves a reason to stop.
>
> ============================================================
> TASK 2 — RECONSTRUCT THE EXACT FINAL RUNTIME
> ============================================
>
> Read the final demo documentation and relevant existing private runtime/startup scripts.
>
> Determine exactly what must be running for:
>
> holder NFC
> → gate Android
> → Node
> → RPC/ENSv2
> → authoritative ALLOW/DENY
>
> Inspect, where relevant:
>
> * existing `.runtime` startup scripts;
> * gate/bridge startup procedures;
> * Node entry points;
> * required ports;
> * ADB forwarding/reverse configuration;
> * holder/gate app state;
> * current selected holder pass/wallet state;
> * HCE publication state;
> * required local env/config existence;
> * RPC connectivity;
> * existing device connectivity.
>
> Do NOT print:
>
> * ADB/device serials;
> * private paths;
> * private IPs;
> * environment values;
> * private keys;
> * RPC credentials.
>
> Use neutral labels such as:
>
> <HOLDER>
> <FRONT_DOOR>
> <LAB>
> <SERVER_ROOM>
>
> ============================================================
> TASK 3 — LOCATE THE FAILURE LAYER
> =================================
>
> Do not change code.
>
> Determine which layer first fails:
>
> A. holder app / wallet / selected credential
> B. HCE publication
> C. physical NFC
> D. Android gate reader
> E. ADB / transport
> F. Node bridge/runtime
> G. holder-proof verification
> H. RPC connectivity
> I. ENSv2 reads
> J. resource policy
> K. final gate UI result handling
>
> Use the smallest diagnostic necessary at each layer.
>
> Do not shotgun-debug all layers simultaneously.
>
> For each checked layer return:
>
> PASS
> FAIL
> NOT REACHED
>
> plus a short sanitized reason.
>
> ============================================================
> TASK 4 — RESTORE THE EXISTING RUNTIME
> =====================================
>
> If the failure is operational rather than code-related, restore the documented runtime using ONLY existing tooling/scripts/configuration.
>
> Examples of acceptable actions:
>
> * restart an existing Node process;
> * start an existing bridge;
> * restore an ADB forwarding/reverse rule;
> * reconnect an already authorized device;
> * reopen/reselect an existing demo pass;
> * restore HCE publication through normal app UI;
> * restart an existing app/process;
> * verify existing RPC connectivity.
>
> Do not:
>
> * edit source;
> * patch scripts;
> * install dependencies;
> * rebuild architecture;
> * create credentials;
> * perform blockchain transactions;
> * modify ENS state.
>
> If a code change appears necessary:
>
> STOP and report evidence.
>
> ============================================================
> TASK 5 — HUMAN ACTIONS
> ======================
>
> Whenever physical interaction is required, stop automation and tell the user EXACTLY:
>
> DEVICE:
> APP/SCREEN:
> ACTION:
> EXPECTED RESULT:
>
> Do not assume they know which phone, screen, button, wallet, credential or NFC action you mean.
>
> Use role names rather than model names where possible.
>
> ============================================================
> TASK 6 — MINIMUM FINAL VALIDATION
> =================================
>
> Once runtime appears restored, validate exactly TWO representative cases using the already-existing final demo state:
>
> CASE A — ALLOW
>
> Existing STAFF credential
> → LAB gate
> → expected authoritative ALLOW
> → virtual gate animation opens
>
> CASE B — DENY
>
> Existing VISITOR credential
> → LAB gate
> → expected authoritative DENY
> → gate remains closed
>
> Do not create or modify credentials to obtain these results.
>
> Do not perform onchain writes.
>
> If the exact documented role/resource names differ, use the corresponding already-validated final matrix entries and explain the substitution.
>
> For both cases capture only sanitized evidence:
>
> * role/credential label;
> * resource label;
> * ALLOW/DENY;
> * denial reason where applicable;
> * confirmation that Node produced the authoritative decision.
>
> Do not print:
>
> * proofs;
> * signatures;
> * challenges;
> * device serials;
> * RPC URLs;
> * private wallet keys;
> * raw credential payloads.
>
> ============================================================
> TASK 7 — DETERMINE ROOT CAUSE
> =============================
>
> Classify the root cause as one of:
>
> RUNTIME_NOT_STARTED
> DEVICE_CONNECTIVITY
> ADB_TRANSPORT
> HCE_STATE
> APP_STATE
> RPC_CONNECTIVITY
> CONFIGURATION
> CODE_REGRESSION
> UNKNOWN
>
> Explain:
>
> * what was missing/broken;
> * why opening the apps alone did not work;
> * what exact startup sequence is required after a PC/device restart;
> * whether this is expected operational behavior or a defect.
>
> ============================================================
> NO-TOUCH
> ========
>
> Do not change:
>
> * source code;
> * docs;
> * Git history;
> * Git refs;
> * `.env` contents;
> * wallets;
> * credentials;
> * ENSv2 configuration/state;
> * Privy configuration;
> * firmware;
> * artwork;
> * release documentation.
>
> ============================================================
> OUTPUT
> ======
>
> Return:
>
> # FINAL DEMO RUNTIME PREFLIGHT
>
> ## REPOSITORY STATE
>
> ## REQUIRED RUNTIME TOPOLOGY
>
> ## LAYER DIAGNOSIS
>
> Table:
>
> Layer | Result | Sanitized evidence
>
> ## ROOT CAUSE
>
> ## ACTIONS PERFORMED
>
> ## HUMAN ACTIONS PERFORMED
>
> ## ALLOW VALIDATION
>
> ## DENY VALIDATION
>
> ## RESTART PROCEDURE
>
> Give the minimum exact procedure the user must follow in future after restarting the PC/devices.
>
> Do not include private identifiers.
>
> ## CODE CHANGE REQUIRED
>
> YES / NO
>
> If YES:
> STOP and explain the smallest proven defect.
>
> ## VERDICT
>
> One exact line:
>
> DEMO RUNTIME PASS — READY FOR RELEASE REMEDIATION
>
> or
>
> DEMO RUNTIME FAIL — <specific blocker>
>
> ============================================================
> STOP IF
> =======
>
> Stop immediately if:
>
> * tracked repo state differs from the frozen candidate;
> * restoring the runtime requires editing code;
> * restoring it requires installing software;
> * an onchain write appears necessary;
> * an existing credential must be modified;
> * a secret would have to be printed;
> * a device requires an unexpected destructive reset.
>
> NO CODE CHANGES.
> NO GIT CHANGES.
> NO BLOCKCHAIN WRITES.
> NO INSTALLS.
