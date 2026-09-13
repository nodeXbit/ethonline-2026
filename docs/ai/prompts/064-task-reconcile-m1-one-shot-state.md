# Project task packet 064: TASK — RECONCILE M1 ONE-SHOT STATE

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK — RECONCILE M1 ONE-SHOT STATE
>
> The user attempted M1 once from the physical Android app.
>
> Current device behavior:
>
> M1 can no longer be invoked.
>
> The UI now reports:
>
> BLOCKED - ONE_SHOT_ALREADY_USED
>
> The user does not see a transaction in the Sepolia explorer.
>
> No reliable transaction hash was captured from the UI.
>
> DO NOT:
>
> - reset the one-shot marker
> - clear app data
> - reinstall the app
> - perform another transaction
> - call eth_sendTransaction
> - perform any ENS write
> - modify code yet
> - commit/push
> - expose RPC credentials
>
> This task is READ-ONLY.
>
> ============================================================
> OBJECTIVE
> ============================================================
>
> Determine whether the M1 transaction was actually broadcast/mined/pending, and
> determine exactly why the app now reports ONE_SHOT_ALREADY_USED.
>
> Issuer public address:
>
> 0xFa90e8301A22833B74378C5fA3a7c120Ac512685
>
> Issuer balance immediately before M1:
>
> 0.02 Sepolia ETH
>
> ============================================================
> PHASE 1 — INSPECT ONE-SHOT IMPLEMENTATION
> ============================================================
>
> Inspect the current uncommitted M1 implementation.
>
> Report exactly:
>
> - where the persistent one-shot marker is stored;
> - when it is set relative to:
>   - final user confirmation
>   - eth_sendTransaction invocation
>   - tx hash receipt
>   - receipt confirmation
> - what additional M1 state is persisted:
>   - tx hash?
>   - preNonce?
>   - stage?
>   - error?
>   - UNKNOWN state?
> - whether app restart can reconcile an already-used attempt.
>
> Do not modify anything.
>
> ============================================================
> PHASE 2 — READ-ONLY ONCHAIN RECONCILIATION
> ============================================================
>
> Using a SAFE read-only Sepolia path that cannot print RPC URLs/credentials:
>
> Read for issuer:
>
> 0xFa90e8301A22833B74378C5fA3a7c120Ac512685
>
> Require:
>
> chainId:
> 11155111
>
> Read:
>
> eth_getTransactionCount issuer "latest"
> eth_getTransactionCount issuer "pending"
> eth_getBalance issuer "latest"
>
> Return only:
>
> latest nonce
> pending nonce
> public balance
>
> Never print the RPC endpoint.
>
> Do not submit or sign anything.
>
> ============================================================
> PHASE 3 — INTERPRETATION
> ============================================================
>
> Classify:
>
> CASE A — CONFIRMED TRANSACTION
>
> latest nonce >= 1
>
> Then identify the actual outgoing transaction if possible using read-only
> public/onchain data or persisted app hash.
>
> Require exact tx details before calling M1 PASS.
>
> CASE B — PENDING TRANSACTION
>
> pending nonce > latest nonce
>
> STOP.
>
> Do not reset or resend.
>
> CASE C — NO TRANSACTION OBSERVED
>
> latest == pending == 0
> and issuer balance remains approximately the funded 0.02 ETH
>
> Then the one-shot marker was consumed without a blockchain transaction.
>
> Determine exact local failure stage from the implementation/persisted state if
> possible.
>
> Do not reset yet.
>
> ============================================================
> PHASE 4 — PERSISTED APP STATE
> ============================================================
>
> If device state can be inspected safely without exposing Privy secrets, inspect
> only M1-specific persisted keys/state.
>
> Do NOT dump all SharedPreferences/app storage.
>
> Do NOT expose:
>
> email
> OTP
> Privy tokens
> wallet credentials
> SDK secrets
>
> Return only M1 fields such as:
>
> oneShotUsed
> txHash if present
> stage/status
> preNonce if stored
>
> If ADB is unavailable, say so.
>
> ============================================================
> PHASE 5 — RECOVERY DESIGN
>
> DO NOT IMPLEMENT YET.
>
> If CASE C is proven, design a safe recovery mechanism.
>
> It must NOT simply remove the one-shot guard.
>
> Preferred behavior:
>
> USED_WITH_CONFIRMED_HASH
> -> never resend
>
> USED_WITH_UNKNOWN_HASH
> -> reconcile first
>
> USED_WITH_NO_BROADCAST_PROVEN
> -> explicit developer/user-authorized reset/re-arm
>
> A future retry must still require a new explicit authorization from Control
> Tower.
>
> Also identify whether the UI should have displayed the exact transaction
> summary before submission and whether it currently did.
>
> ============================================================
> RETURN
> ============================================================
>
> # M1 ONE-SHOT RECONCILIATION
>
> ## ONE-SHOT IMPLEMENTATION
>
> Marker location:
>
> Marker set at:
>
> Tx hash persistence:
>
> Recovery behavior:
>
> ## ONCHAIN STATE
>
> Chain:
>
> Issuer:
>
> Latest nonce:
>
> Pending nonce:
>
> Balance:
>
> ## PERSISTED M1 STATE
>
> One-shot used:
>
> Stored tx hash:
>
> Stored stage:
>
> Other safe M1 fields:
>
> ## CLASSIFICATION
>
> CONFIRMED TRANSACTION
> / PENDING TRANSACTION
> / NO TRANSACTION OBSERVED
> / UNKNOWN
>
> ## ROOT CAUSE
>
> If determinable.
>
> ## UI CONFIRMATION AUDIT
>
> Did the final UI actually show:
>
> Network:
> From:
> To:
> Value:
> Data:
> Gas-only purpose:
>
> YES / NO
>
> ## SAFE RECOVERY DESIGN
>
> Do not implement.
>
> ## SECURITY
>
> Blockchain writes during task:
> 0
>
> Signing:
> 0
>
> Secrets exposed:
> 0
>
> ## VERDICT
>
> If no transaction is observed, end:
>
> M1 RECONCILIATION: NO TRANSACTION OBSERVED — RECOVERY REQUIRED
>
> If a real transaction is found:
>
> M1 RECONCILIATION: TRANSACTION FOUND — <status>
>
> Otherwise:
>
> M1 RECONCILIATION: UNKNOWN — DO NOT RETRY
