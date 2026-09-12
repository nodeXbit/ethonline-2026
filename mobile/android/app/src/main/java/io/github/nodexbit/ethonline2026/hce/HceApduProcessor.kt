package io.github.nodexbit.ethonline2026.hce

import io.github.nodexbit.ethonline2026.AccessResources
import io.github.nodexbit.ethonline2026.CredentialAbi
import io.github.nodexbit.ethonline2026.CredentialValidation
import io.github.nodexbit.ethonline2026.IssuerSpace

class HceApduProcessor(private val proofProvider: ProofProvider,
    private val identitySource: (() -> HceIdentity?)? = null,
    private val nowSeconds: () -> ULong = { (System.currentTimeMillis() / 1000).toULong() }) {
    enum class State(val wireValue: Byte) {
        IDLE(0x00),
        PROCESSING(0x01),
        READY(0x02),
        ERROR(0x03),
    }

    private var selected = false
    private var generation = 0L
    private var state = State.IDLE
    private var challenge: HceChallenge? = null
    private var proof: ByteArray? = null
    private var identity: HceIdentity? = null
    private var credentialRead = false
    private val seenNonces = linkedMapOf<String, ULong>()

    @Synchronized
    fun process(commandApdu: ByteArray?): ByteArray {
        val command = commandApdu ?: return WRONG_LENGTH.copyOf()
        if (command.size < HEADER_LENGTH) return WRONG_LENGTH.copyOf()

        val cla = command[0].toUByte().toInt()
        val ins = command[1].toUByte().toInt()
        val p1 = command[2].toUByte().toInt()
        val p2 = command[3].toUByte().toInt()

        if (cla == ISO_CLA && ins == INS_SELECT) return select(command, p1, p2)
        if (cla != APPLICATION_CLA) return CLASS_NOT_SUPPORTED.copyOf()
        if (ins !in APPLICATION_INSTRUCTIONS) return INS_NOT_SUPPORTED.copyOf()
        if (!selected) return CONDITIONS_NOT_SATISFIED.copyOf()
        if (p1 != PROTOCOL_VERSION || p2 != 0x00) return INCORRECT_P1_P2.copyOf()
        if (identitySource != null && (identity == null || identitySource.invoke() !== identity)) {
            reset()
            return CONDITIONS_NOT_SATISFIED.copyOf()
        }

        return when (ins) {
            INS_SEND_CHALLENGE -> sendChallenge(command)
            INS_GET_STATUS -> getStatus(command)
            INS_GET_SIGNATURE -> getSignature(command)
            INS_GET_CREDENTIAL -> getCredential(command)
            else -> INS_NOT_SUPPORTED.copyOf()
        }
    }

    @Synchronized
    fun reset() {
        generation += 1
        selected = false
        state = State.IDLE
        challenge = null
        proof = null
        identity = null
        credentialRead = false
    }

    @Synchronized
    fun currentState(): State = state

    @Synchronized
    fun currentChallenge(): HceChallenge? = challenge?.copy()

    /** Debug metadata only: never include the nonce, APDU body, provider details or proof. */
    @Synchronized
    fun diagnosticSummary(command: ByteArray?): String {
        val value = identity
        val safeName = value?.credential?.takeIf {
            it.toByteArray(Charsets.UTF_8).size in 1..64 &&
                runCatching { CredentialValidation.normalizeFullName(it) }.getOrNull() == it
        } ?: "NONE"
        val context = "selected=$selected credentialRead=$credentialRead state=${state.name} credential=$safeName chain=${value?.chainId}"
        if (command == null || command.size != HEADER_LENGTH + 1 + HceChallenge.BODY_LENGTH ||
            command[0].toUByte().toInt() != APPLICATION_CLA || command[1].toUByte().toInt() != INS_SEND_CHALLENGE ||
            command[4].toUByte().toInt() != HceChallenge.BODY_LENGTH) return context
        val decoded = HceChallenge.decode(command.copyOfRange(5, command.size))
        val now = nowSeconds()
        val resource = "0x" + decoded.resource.joinToString("") { "%02x".format(it.toInt() and 255) }
        val credentialMatches = value != null && safeName != "NONE" &&
            decoded.credential.contentEquals(CredentialAbi.namehash(value.credential))
        val remaining = if (decoded.expiresAt >= now) (decoded.expiresAt - now).toString() else "EXPIRED"
        return "$context credentialMatches=$credentialMatches resourceKnown=${AccessResources.all.any { it.resourceId == resource }}" +
            " expiresAt=${decoded.expiresAt} now=$now remainingSeconds=$remaining"
    }

    private fun select(command: ByteArray, p1: Int, p2: Int): ByteArray {
        reset()
        if (p1 != 0x04 || p2 != 0x00) return INCORRECT_P1_P2.copyOf()
        if (command.size < HEADER_LENGTH + 1) return WRONG_LENGTH.copyOf()
        val length = command[4].toUByte().toInt()
        val hasNoLe = command.size == HEADER_LENGTH + 1 + length
        val hasZeroLe = command.size == HEADER_LENGTH + 2 + length && command.last() == 0.toByte()
        if (!hasNoLe && !hasZeroLe) return WRONG_LENGTH.copyOf()
        val candidateAid = command.copyOfRange(5, 5 + length)
        if (!candidateAid.contentEquals(AID)) return NOT_FOUND.copyOf()
        selected = true
        identity = identitySource?.invoke()
        return SUCCESS.copyOf()
    }

    private fun getCredential(command: ByteArray): ByteArray {
        if (!isDataFreeCommand(command)) return WRONG_LENGTH.copyOf()
        val value = identity ?: return CONDITIONS_NOT_SATISFIED.copyOf()
        val payload = value.credential.toByteArray(Charsets.UTF_8)
        if (payload.size !in 1..64 || value.chainId != IssuerSpace.chainId ||
            runCatching { CredentialValidation.normalizeFullName(value.credential) }.getOrNull() != value.credential) {
            reset()
            return CONDITIONS_NOT_SATISFIED.copyOf()
        }
        credentialRead = true
        return payload + SUCCESS
    }

    private fun sendChallenge(command: ByteArray): ByteArray {
        if (command.size < HEADER_LENGTH + 1) return WRONG_LENGTH.copyOf()
        val length = command[4].toUByte().toInt()
        if (length != HceChallenge.BODY_LENGTH || command.size != HEADER_LENGTH + 1 + length) {
            return WRONG_LENGTH.copyOf()
        }

        val decoded = try {
            HceChallenge.decode(command.copyOfRange(5, command.size))
        } catch (_: IllegalArgumentException) {
            return WRONG_LENGTH.copyOf()
        }

        if (identitySource != null) {
            val value = identity
            val now = nowSeconds()
            val nonce = decoded.nonce.joinToString("") { "%02x".format(it.toInt() and 255) }
            seenNonces.entries.removeAll { it.value <= now }
            if (!credentialRead || value == null || challenge != null ||
                !decoded.credential.contentEquals(CredentialAbi.namehash(value.credential)) ||
                AccessResources.all.none { it.resourceId == "0x" + decoded.resource.joinToString("") { byte -> "%02x".format(byte.toInt() and 255) } } ||
                decoded.expiresAt <= now || decoded.expiresAt - now > 60uL ||
                seenNonces.containsKey(nonce) || seenNonces.size >= 128) {
                reset()
                return CONDITIONS_NOT_SATISFIED.copyOf()
            }
            seenNonces[nonce] = decoded.expiresAt
        }

        generation += 1
        val requestGeneration = generation
        challenge = decoded
        proof = null
        state = State.PROCESSING
        (identity?.proofProvider ?: proofProvider).requestProof(decoded.copy()) { result ->
            completeProof(requestGeneration, result)
        }
        return SUCCESS.copyOf()
    }

    private fun getStatus(command: ByteArray): ByteArray {
        if (!isDataFreeCommand(command)) return WRONG_LENGTH.copyOf()
        return byteArrayOf(state.wireValue) + SUCCESS
    }

    private fun getSignature(command: ByteArray): ByteArray {
        if (!isDataFreeCommand(command)) return WRONG_LENGTH.copyOf()
        val currentProof = proof
        if (identitySource != null && (challenge?.expiresAt ?: 0uL) <= nowSeconds()) {
            reset()
            return CONDITIONS_NOT_SATISFIED.copyOf()
        }
        if (state != State.READY || currentProof == null) {
            return CONDITIONS_NOT_SATISFIED.copyOf()
        }
        // Retain proof for retrying an interrupted read. SELECT or a new challenge
        // clears it; Gate A remains responsible for cryptographic replay prevention.
        return currentProof.copyOf() + SUCCESS
    }

    private fun isDataFreeCommand(command: ByteArray): Boolean =
        command.size == HEADER_LENGTH ||
            (command.size == HEADER_LENGTH + 1 && command[4] == 0.toByte())

    @Synchronized
    private fun completeProof(requestGeneration: Long, result: Result<ByteArray>) {
        if (requestGeneration != generation || state != State.PROCESSING) return
        if (identitySource != null && (identitySource.invoke() !== identity ||
                (challenge?.expiresAt ?: 0uL) <= nowSeconds())) { reset(); return }
        val candidate = result.getOrNull()
        if (candidate == null || candidate.size != SIGNATURE_LENGTH) {
            proof = null
            state = State.ERROR
            return
        }
        proof = candidate.copyOf()
        state = State.READY
    }

    companion object {
        const val AID_HEX = "F0454E5356324331"
        const val APPLICATION_CLA = 0x80
        const val PROTOCOL_VERSION = 0x01
        const val INS_SEND_CHALLENGE = 0x10
        const val INS_GET_STATUS = 0x20
        const val INS_GET_SIGNATURE = 0x30
        const val INS_GET_CREDENTIAL = 0x40
        const val SIGNATURE_LENGTH = 65

        private const val ISO_CLA = 0x00
        private const val INS_SELECT = 0xA4
        private const val HEADER_LENGTH = 4
        private val APPLICATION_INSTRUCTIONS = setOf(
            INS_SEND_CHALLENGE,
            INS_GET_STATUS,
            INS_GET_SIGNATURE,
            INS_GET_CREDENTIAL,
        )
        val AID: ByteArray = byteArrayOf(
            0xF0.toByte(), 0x45, 0x4E, 0x53, 0x56, 0x32, 0x43, 0x31,
        )

        val SUCCESS = byteArrayOf(0x90.toByte(), 0x00)
        val WRONG_LENGTH = byteArrayOf(0x67, 0x00)
        val CONDITIONS_NOT_SATISFIED = byteArrayOf(0x69, 0x85.toByte())
        val INCORRECT_P1_P2 = byteArrayOf(0x6A, 0x86.toByte())
        val NOT_FOUND = byteArrayOf(0x6A, 0x82.toByte())
        val INS_NOT_SUPPORTED = byteArrayOf(0x6D, 0x00)
        val CLASS_NOT_SUPPORTED = byteArrayOf(0x6E, 0x00)
    }
}
