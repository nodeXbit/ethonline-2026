# Project

- Goal: build a valid, functional, deployed, and explainable ETHGlobal prototype that connects an ENSv2-managed digital access credential to a physical NFC interaction.
- Problem: owners and operators of physical spaces need a simple way to grant temporary access with authoritative credential state that is open, inspectable, and easy to update.
- Initial user: the owner or operator of a physical space granting temporary access.
- Product direction: an open physical-access credential prototype in which ENSv2 on Sepolia is the authoritative state and an ESP32-S3 with a PN532 bridges that state to a physical NFC interaction.
- Core flow: an operator creates or updates access state through ENSv2 on Sepolia; a user presents an NFC identifier; the prototype resolves the associated credential state and produces an access decision.
- Primary sponsor: ENS / ENSv2.
- MVP: one end-to-end Sepolia vertical slice demonstrating ENSv2-authoritative credential state and a physical ISO14443A NFC interaction through the ESP32-S3 and PN532.
- Non-goals: production-grade access control, complex infrastructure, multiple superficial sponsor integrations, cryptographic NFC challenge-response, Android HCE, and unrelated Hermes/Knowledge OS work.
- NFC security boundary: a static NFC UID is acceptable only as a prototype/demo identifier. It is clonable and must not be represented as secure proof of credential ownership or as anti-cloning security.
