const defaultRecoveryDelays = [500, 1000, 2000, 3000, 5000, 5000, 5000, 5000];
const delay = milliseconds => new Promise(resolve => setTimeout(resolve, milliseconds));

export function sameCredentialIdentity(before, after) {
  return before.name === after.name && before.status === after.status &&
    before.owner.toLowerCase() === after.owner.toLowerCase() &&
    before.tokenId === after.tokenId && before.registryExpiry === after.registryExpiry &&
    before.resolver.toLowerCase() === after.resolver.toLowerCase();
}

export function accessTargetAchieved(credential, action) {
  if (action === 'activate') {
    return credential.access?.active === true && credential.authorization === 'ALLOW';
  }
  return credential.access?.active === false && credential.authorization === 'DENY';
}

export function createAccessActionHandler({
  getCredential, isCredentialUsable, requestJson, render, setPending, setFeedback, shorten,
  recoveryDelays = defaultRecoveryDelays, wait = delay,
}) {
  let inFlight = false;

  async function reconcileSubmitted(current, action, result) {
    const submitted = shorten(result.transactionHash);
    setFeedback(result.transactionState === 'CONFIRMED_SUCCESS'
      ? `CONFIRMED ON SEPOLIA — REFRESHING CREDENTIAL STATE… ${submitted}`
      : `TRANSACTION SUBMITTED — WAITING FOR SEPOLIA CONFIRMATION… ${submitted}`,
    'pending');
    for (const milliseconds of recoveryDelays) {
      await wait(milliseconds);
      try {
        const credential = await requestJson('/api/credential');
        if (sameCredentialIdentity(current, credential) && accessTargetAchieved(credential, action)) {
          render(credential);
          setFeedback(`${action === 'deactivate' ? 'Deactivation' : 'Activation'} confirmed · ${submitted}`,
            'success');
          return true;
        }
      } catch {
        // Read-only reconciliation continues within the bounded polling window.
      }
    }
    setFeedback(`Transaction submitted. Confirmation is taking longer than expected. Reload to reconcile from ENSv2. ${submitted}`,
      'warning');
    return false;
  }

  async function recover(recovery) {
    const current = getCredential();
    if (inFlight || !current || !recovery?.pending) return;
    const action = recovery.requestedState === 'ACTIVE' ? 'activate' : 'deactivate';
    inFlight = true;
    setPending(action, true);
    if (await reconcileSubmitted(current, action, recovery)) {
      inFlight = false;
      setPending(action, false);
    }
  }

  async function changeAccess() {
    const current = getCredential();
    if (inFlight || !current || !isCredentialUsable(current)) return;

    inFlight = true;
    const deactivate = current.authorization === 'ALLOW';
    const action = deactivate ? 'deactivate' : 'activate';
    setPending(action, true);
    let keepLocked = false;

    try {
      const result = await requestJson(`/api/${action}`, { method: 'POST' });
      if (result.pending) {
        keepLocked = !(await reconcileSubmitted(current, action, result));
        return;
      }

      render(result.credential);
      const confirmedMessage = result.changed
        ? `${deactivate ? 'Deactivation' : 'Activation'} confirmed${result.recovered ? ' after recovery' : ''} · ${shorten(result.transactionHash)}`
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
      if (!keepLocked) {
        inFlight = false;
        setPending(action, false);
      }
    }
  }

  changeAccess.recover = recover;
  return changeAccess;
}
