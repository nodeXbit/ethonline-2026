// Pure display mapping: policy is deliberately not an input.
export function attemptView(report) {
  const empty = { verifier: 'NOT RUN', controller: 'NOT RUN', replay: 'NOT CHECKED', confirmedAllow: false };
  if (!report || report.schemaVersion !== 1) return empty;
  if (report.state === 'IN_PROGRESS') return { ...empty, verifier: 'IN PROGRESS' };
  if (report.state !== 'COMPLETE') return empty;
  const transportFailure = report.transportResult === 'TRANSPORT_FAILURE';
  const verifier = transportFailure ? 'TRANSPORT FAILURE'
    : report.verifierResult === 'VERIFIER_ALLOW' ? 'ALLOW'
      : report.verifierResult === 'VERIFIER_DENY' ? 'DENY' : 'NOT RUN';
  const confirmed = report.controllerConfirmation === 'CONTROLLER_CONFIRMED' &&
    ['ALLOW', 'DENY'].includes(report.controllerDecision);
  const confirmedAllow = confirmed && !transportFailure && verifier === 'ALLOW' && report.controllerDecision === 'ALLOW';
  const controller = transportFailure ? 'TRANSPORT FAILURE'
    : confirmedAllow ? 'CONFIRMED ALLOW'
      : confirmed && report.controllerDecision === 'DENY' ? 'CONFIRMED DENY' : 'UNCONFIRMED';
  return { verifier, controller, confirmedAllow,
    replay: report.replayResult === 'DENIED' ? 'DENIED'
      : report.replayResult === 'UNEXPECTED_RESULT' ? 'UNEXPECTED RESULT — STOP' : 'NOT CHECKED' };
}
