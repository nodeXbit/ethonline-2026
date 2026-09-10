export const gateETiming = Object.freeze({
  challengeTtlSeconds: 60n,
  postChallengeDeadlineMs: 50_000,
  proofTimeoutMs: 30_000,
  ensVerificationBudgetMs: 8_000,
  firmwareConfirmationTimeoutMs: 2_000,
  maxBlockAgeSeconds: 60n,
  maxFutureSkewSeconds: 15n,
});

// The largest inbound protocol line is currently a 65-byte proof encoded as hex
// (138 ASCII bytes including its prefix). This leaves substantial diagnostic headroom.
export const gateESerialMaxLineBytes = 1_024;
