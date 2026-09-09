export function createAccessActionHandler({
  getCredential, isCredentialUsable, requestJson, render, setPending, setFeedback, shorten,
}) {
  let inFlight = false;

  return async function changeAccess() {
    const current = getCredential();
    if (inFlight || !current || !isCredentialUsable(current)) return;

    inFlight = true;
    const deactivate = current.authorization === 'ALLOW';
    const action = deactivate ? 'deactivate' : 'activate';
    setPending(action, true);

    try {
      const result = await requestJson(`/api/${action}`, { method: 'POST' });
      render(result.credential);
      const confirmedMessage = result.changed
        ? `${deactivate ? 'Deactivation' : 'Activation'} confirmed · ${shorten(result.transactionHash)}`
        : `Access is already in the requested ${deactivate ? 'inactive' : 'active'} state.`;
      setFeedback(confirmedMessage, 'success');

      try {
        render(await requestJson('/api/credential'));
        setFeedback(confirmedMessage, 'success');
      } catch {
        setFeedback(`${confirmedMessage} Refresh/reconciliation failed; showing confirmed state.`, 'warning');
      }
    } catch (error) {
      setFeedback(error instanceof Error ? error.message : 'The update failed. You can retry.', 'error');
    } finally {
      inFlight = false;
      setPending(action, false);
    }
  };
}
