# AI usage disclosure

LockENS was built through a human-directed, spec-driven workflow with substantial
AI assistance. AI accelerated implementation and review; it did not independently
choose the product, authorize sensitive actions, operate the physical demo, or
make the release decision.

## Tools

- **ChatGPT** was used as a control tower for planning, research, architecture
  discussion, review, debugging strategy, release/compliance work, and learning
  support. It produced detailed task packets and stop conditions that the
  participant reviewed and issued.
- **Codex** was used for repository inspection, scoped implementation, tests,
  builds, local operational validation, evidence review, history sanitation,
  and documentation changes. Codex also invoked OpenAI image generation for the
  three disclosed Gate Stand backgrounds.
- No additional AI tool is claimed because the reviewed project evidence does
  not establish another material contributor.

No specific ChatGPT, Codex, or image-generation model is claimed: the retained
evidence identifies the tools/workflow but does not reliably establish a model
name for every contribution.

## Human contribution and control

The participant selected LockENS and its scope, made product and architecture
trade-offs, approved significant changes, controlled wallet and blockchain
authorizations, configured the physical Android/NFC hardware, performed and
observed manual demo validation, accepted or rejected AI proposals, controlled
feature freeze and release decisions, and remains responsible for final
narration and submission.

Sensitive operations were never delegated implicitly. Task packets required
explicit human authorization for wallet creation, blockchain writes, firmware
flashing, history rewriting, and publication. Physical outcomes were reported
only after participant observation.

## AI contribution map

| Layer | AI assistance | Human involvement |
| --- | --- | --- |
| Android | Architecture discussion; scoped Kotlin/HCE/wallet/Studio/Gate Stand implementation; review and test support. | Chose product behavior, approved changes, configured devices, performed UI/NFC validation, and authorized wallet actions. |
| Node | ENSv2 read/write workflow, holder-proof verifier, local gate service, failure handling, tests, and debugging support. | Selected authority boundaries, approved writes, operated the live service, and observed outcomes. |
| Firmware | ESP32-S3/PN532 protocol and diagnostic assistance, compile validation, and bounded troubleshooting. | Wired and operated hardware, authorized flashes/resets, and judged physical results. |
| Tests | Generation and maintenance of Node/Android security, regression, and synthetic fixture tests; execution and result analysis. | Set acceptance criteria and approved fixture classifications and release gates. |
| Documentation | Drafting, consistency checks, privacy remediation, attribution research, and release-document preparation. | Supplied decisions/evidence, reviewed claims, authorized history sanitation, and owns final publication. |
| Planning/release | Task decomposition, threat modeling, audit plans, stop conditions, and release checks. | Acted as control authority and made all scope, risk, and release decisions. |
| Artwork/assets | Three Gate Stand backgrounds generated from preserved project prompts; C2PA retained. Attribution review for remote Wikimedia artwork. | Directed themes/layout, selected the product use, reviewed the result, and approved integration. |

## Specifications and prompts

The repository's genuine planning and evidence documents are indexed in
[docs/ai/SPECS.md](docs/ai/SPECS.md). The complete available project-specific
task-packet corpus, sanitized with explicit redaction markers, and the exact
Gate Stand image prompts are in
[docs/ai/PROMPTS.md](docs/ai/PROMPTS.md). Private operational values, unrelated
conversation, account data, and credentials are intentionally excluded.
