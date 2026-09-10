package io.github.nodexbit.ethonline2026.hce

import kotlinx.coroutines.delay

data class GateC2HarnessResult(
    val state: HceApduProcessor.State,
    val signature: ByteArray,
)

/** Drives the frozen APDU v1 against the same processor used by HostApduService. */
object GateC2ManualHarness {
    suspend fun run(processor: HceApduProcessor): GateC2HarnessResult {
        requireSuccess(processor.process(selectApdu()), "SELECT")
        requireSuccess(processor.process(sendChallengeApdu()), "SEND_CHALLENGE")

        repeat(MAX_STATUS_POLLS) {
            val status = processor.process(statusApdu())
            require(status.size == 3 && status.copyOfRange(1, 3).contentEquals(HceApduProcessor.SUCCESS)) {
                "GET_STATUS failed"
            }
            when (status[0]) {
                HceApduProcessor.State.PROCESSING.wireValue -> delay(STATUS_POLL_MILLIS)
                HceApduProcessor.State.READY.wireValue -> {
                    val response = processor.process(signatureApdu())
                    require(response.size == HceApduProcessor.SIGNATURE_LENGTH + 2)
                    require(response.copyOfRange(65, 67).contentEquals(HceApduProcessor.SUCCESS))
                    return GateC2HarnessResult(
                        state = HceApduProcessor.State.READY,
                        signature = response.copyOfRange(0, 65),
                    )
                }
                HceApduProcessor.State.ERROR.wireValue -> error("HCE proof provider failed")
                else -> error("Unexpected HCE state")
            }
        }
        error("HCE proof provider timed out")
    }

    fun selectApdu(): ByteArray =
        byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, HceApduProcessor.AID.size.toByte()) +
            HceApduProcessor.AID

    fun sendChallengeApdu(): ByteArray = byteArrayOf(
        HceApduProcessor.APPLICATION_CLA.toByte(),
        HceApduProcessor.INS_SEND_CHALLENGE.toByte(),
        HceApduProcessor.PROTOCOL_VERSION.toByte(),
        0x00,
        HceChallenge.BODY_LENGTH.toByte(),
    ) + GateC2TestVector.challengeBody()

    fun statusApdu(): ByteArray = byteArrayOf(
        HceApduProcessor.APPLICATION_CLA.toByte(),
        HceApduProcessor.INS_GET_STATUS.toByte(),
        HceApduProcessor.PROTOCOL_VERSION.toByte(),
        0x00,
    )

    fun signatureApdu(): ByteArray = byteArrayOf(
        HceApduProcessor.APPLICATION_CLA.toByte(),
        HceApduProcessor.INS_GET_SIGNATURE.toByte(),
        HceApduProcessor.PROTOCOL_VERSION.toByte(),
        0x00,
    )

    private fun requireSuccess(response: ByteArray, command: String) {
        require(response.contentEquals(HceApduProcessor.SUCCESS)) { "$command failed" }
    }

    private const val STATUS_POLL_MILLIS = 250L
    private const val MAX_STATUS_POLLS = 120
}
