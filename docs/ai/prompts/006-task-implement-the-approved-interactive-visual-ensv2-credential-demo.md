# Project task packet 006: TASK — Implement the approved interactive visual ENSv2 credential demo.

- Classification: **ORIGINAL / SANITIZED**
- Redactions applied: none
- Source: retained project-specific Control Tower packet in the local Codex record

---

> TASK
>
> Implement the approved interactive visual ENSv2 credential demo.
>
> The prior read-only plan is accepted.
>
> GOAL
>
> Build the smallest polished local UI that makes the already-verified persistent
> credential lifecycle visually obvious:
>
> persistent ENSv2 credential
> → INACTIVE / DENY
> → activate through UI
> → ACTIVE / ALLOW
> → deactivate through UI
> → INACTIVE / DENY
>
> The browser must always render authoritative ENSv2 state.
>
> ARCHITECTURE
>
> Use:
>
> * Node 24 built-in HTTP server
> * existing viem dependency
> * vanilla HTML/CSS/JS
> * loopback only
> * no new npm dependencies
>
> Do NOT introduce React, Next.js, Vite, Tailwind, Express, database, wallet SDK,
> account abstraction or any other framework/service.
>
> FILES TO CREATE
>
> demo/server.mjs
> demo/server.test.mjs
> demo/public/index.html
> demo/public/styles.css
> demo/public/app.js
>
> FILES TO MODIFY
>
> scripts/ensv2/persistent-access.mjs
> scripts/ensv2/access-record.test.mjs
> package.json
>
> Do NOT modify README.md or STATUS.md yet.
> Those should only be updated after automated and manual validation pass.
>
> REUSE EXISTING VERIFIED LOGIC
>
> Do not duplicate ENSv2 write logic.
>
> Extract from persistent-access.mjs a small exported function such as:
>
> updateAccess(context, action)
>
> where action is strictly:
>
> activate
> or
> deactivate
>
> The existing CLI commands must continue to call the same function and preserve
> their current behavior/output.
>
> The function must:
>
> * read current credential state
> * derive next access.v1 via existing validated nextAccess logic
> * simulate setData
> * submit transaction
> * wait for successful receipt
> * read credential again
> * verify credential identity invariants
> * verify requested access state
> * return only public state + transaction evidence
>
> Do not expose generic contract-write functionality.
>
> SERVER
>
> Create demo/server.mjs.
>
> Bind ONLY:
>
> 127.0.0.1:4173
>
> Never:
> 0.0.0.0
>
> Use node:http.
>
> STATIC ROUTES
>
> Serve only explicit allowlisted routes:
>
> GET /
> GET /index.html
> GET /styles.css
> GET /app.js
>
> Do not implement generic filesystem path resolution.
>
> API
>
> GET /api/credential
>
> Read authoritative ENSv2 state.
>
> Return JSON:
>
> {
> "name": "cred-001.demo-access.eth",
> "status": "...",
> "owner": "0x...",
> "registryExpiry": "...",
> "tokenId": "...",
> "resolver": "0x...",
> "access": {
> "active": false,
> "validUntil": "..."
> },
> "authorization": "ALLOW|DENY"
> }
>
> Chain integer values must be decimal strings.
>
> No:
>
> * private key
> * environment variables
> * RPC URL
> * raw viem errors
> * stacks
> * wallet-client data
>
> POST /api/activate
>
> Hardcoded action only.
>
> No request parameters.
>
> No request body.
>
> Use updateAccess(..., "activate").
>
> Wait for confirmed receipt and verified readback.
>
> Return:
>
> {
> "transactionHash": "0x...",
> "credential": { ...public credential state... }
> }
>
> POST /api/deactivate
>
> Same constraints.
>
> Use updateAccess(..., "deactivate").
>
> WRITE SECURITY
>
> Before accepting POST:
>
> * require Host exactly compatible with:
>   127.0.0.1:4173
> * require Origin exactly:
>   http://127.0.0.1:4173
> * reject absent or different browser Origin
> * no CORS headers
> * require empty request body
> * reject Content-Length > 0
> * reject Transfer-Encoding bodies
> * allow only the exact two POST routes
> * maintain one in-process write lock
> * concurrent write => HTTP 409
>
> The server must not expose:
>
> * generic action endpoint
> * contract address input
> * calldata input
> * credential-name input
> * resolver input
> * arbitrary transaction endpoint
>
> SEP0LIA SAFETY
>
> All writes must retain existing checks:
>
> * chainId == 11155111
> * demo-access.eth
> * current verified UserRegistry
> * verified PermissionedResolver provenance
> * access.v1
> * persistent credential identity invariants
>
> Do not expose persistent setup through HTTP.
>
> ERRORS
>
> Create a strict safe public error serializer.
>
> Never serialize raw Error objects.
>
> Allowed public fields only:
>
> code
> message
> stage
> transactionHash
>
> Redact URLs if present.
>
> Suggested status handling:
>
> 403 invalid Host/Origin
> 404 unknown route
> 405 wrong method
> 409 concurrent/incompatible state
> 502 Sepolia/RPC failure
> 500 safe unexpected failure
>
> API responses:
>
> Cache-Control: no-store
>
> STATIC SECURITY HEADERS
>
> Include appropriate restrictive headers:
>
> Content-Security-Policy
> X-Content-Type-Options: nosniff
> Referrer-Policy: no-referrer
> frame-ancestors 'none'
>
> Keep CSP compatible with local static assets.
>
> UI
>
> Build one polished credential card.
>
> No fake blockchain aesthetics.
>
> Show:
>
> DEMO ACCESS
> DIGITAL KEY
> cred-001.demo-access.eth
>
> Credential status:
> REGISTERED
>
> Access visual state:
>
> ACTIVE
> or
> INACTIVE
>
> Authorization:
>
> ALLOW
> or
> DENY
>
> Holder:
> shortened address
>
> Access valid until:
> human-readable local time
>
> Include optional compact technical details:
>
> * tokenId shortened
> * resolver shortened
>
> Do not overwhelm the primary card.
>
> ACTIVE appearance:
>
> * visually bright / illuminated
> * strong positive status
>
> INACTIVE appearance:
>
> * muted but still clearly a persistent credential
> * do NOT hide/remove the card
>
> Important product message visually:
>
> credential persists
> access changes
>
> BUTTON
>
> When INACTIVE/DENY and credential valid:
> Activate access
>
> When ACTIVE/ALLOW:
> Deactivate access
>
> During transaction:
>
> * preserve previous confirmed card
> * disable controls
> * show:
>   Activating on Sepolia…
>   or
>   Deactivating on Sepolia…
>
> Do NOT optimistically switch ACTIVE/INACTIVE.
>
> After POST success:
>
> perform GET /api/credential again
>
> and render ONLY that authoritative response.
>
> SUCCESS
>
> Show:
>
> * confirmed action
> * shortened public transaction hash
>
> ERROR
>
> Keep previous confirmed state.
> Show safe error.
> Allow retry.
>
> EXPIRED ACCESS
>
> If access.active is true but validUntil <= current chain timestamp:
>
> UI must render:
>
> INACTIVE
> DENY
>
> not ACTIVE.
>
> CREDENTIAL INVALID
>
> If registry credential is not valid/registered:
>
> keep the card visible
> show unavailable state
> disable writes.
>
> TESTS
>
> demo/server.test.mjs must test without real transactions:
>
> * public state serialization
> * BigInt/string serialization
> * route allowlist
> * 404
> * 405
> * Host rejection
> * Origin rejection
> * nonempty POST body rejection
> * write lock / 409
> * safe error serialization
> * no secret fields
> * security headers
>
> Extend access-record.test.mjs for the exported updateAccess function using the
> existing mocked-chain harness.
>
> Validate that:
>
> * CLI path still uses the same function
> * INACTIVE → ACTIVE preserves identity
> * ACTIVE → INACTIVE preserves identity
> * confirmed readback is required
> * failed write/readback cannot report success
>
> PACKAGE.JSON
>
> Add only:
>
> "demo": "node --env-file=.env.local demo/server.mjs"
>
> No new dependency.
>
> NO-TOUCH
>
> firmware/
> PROJECT.md
> STATUS.md
> WORKLOG.md
> README.md
> .env*
> package-lock.json
> scripts/nfc/bridge.mjs unless a genuine implementation blocker is found
> previous validated setup/lifecycle architecture
> Android/iOS
> Aliro
> account abstraction
> transfers
> loyalty
> NFT metadata
> custom Solidity
>
> DO NOT
>
> * install anything
> * access or print .env.local contents
> * send transactions
> * start a server that performs writes during implementation validation
> * commit
> * push
>
> VALIDATION
>
> Run:
>
> * syntax checks
> * existing ENSv2 tests
> * new server tests
> * git diff --check
> * inspect git diff
> * confirm no secret strings/configuration are present in public assets
> * confirm demo server binds only 127.0.0.1
> * confirm browser JS contains no viem signing/wallet/private-key logic
> * confirm no no-touch file changed
>
> You may start the server only in a safe mode/read-only validation context if
> doing so cannot broadcast writes.
>
> RETURN
>
> # UI IMPLEMENTATION RESULT
>
> ## FILES
>
> ## SERVER
>
> ## API
>
> ## UI
>
> ## SECURITY
>
> ## TESTS
>
> ## MANUAL VALIDATION STEPS
>
> ## RISKS
>
> End exactly:
>
> REVIEW: PASS
>
> or
>
> REVIEW: STOP
