const byId = id => document.getElementById(id);
const text = (id, value) => { byId(id).textContent = value ?? '—'; };
async function refresh() {
  try {
    const response = await fetch('/state');
    if (!response.ok) throw Error();
    const value = await response.json();
    byId('gate').value = value.selected.slug;
    text('resource', value.resource.displayName);
    text('status', value.status);
    text('credential', value.credential);
    for (const field of ['holder','registration','globalAccess','proof']) text(field, value.checks?.[field] ?? 'Not checked');
    const policy = value.checks?.resourcePolicy;
    text('resourcePolicy', `${value.resource.displayName} ${policy === 'Allowed' ? '✓' : policy === 'Denied' ? '✕' : '· Not checked'}`);
    text('controller', value.controller);
    text('final', value.final ?? value.status);
    text('reason', value.humanReason ?? '');
    const diagnostic = value.readerDiagnostic;
    const diagnosticText = diagnostic?.command ? `PN532 ${diagnostic.command}: write=${diagnostic.writeResult}, read=${diagnostic.readResult ?? 'not attempted'}` : diagnostic ? `I2C ${diagnostic.address}: ${diagnostic.wireResult}` : null;
    text('category', [value.reason, value.stage, diagnosticText].filter(Boolean).join(' · '));
    byId('decision').className = value.final === 'ACCESS GRANTED' ? 'granted' : value.final === 'ACCESS DENIED' ? 'denied' : value.final ? 'error' : '';
    byId('attempt').disabled = !value.physicalEnabled || value.busy;
    text('physical', value.physicalEnabled ? 'Start listening, then reset the reader and present the phone when it is ready. One proof per session.' : 'Physical proof mode is disabled.');
    text('next', value.busy ? `Current session is latched to ${value.resource.displayName}. Selection applies to the next session.` : 'Selection applies to the next session.');
  } catch {
    text('final', 'Monitor unavailable'); text('reason', 'Reconnect to verify the current result.');
    byId('decision').className = 'error'; byId('attempt').disabled = true;
  }
}
byId('gate').addEventListener('change', async event => { await fetch(`/gate/${event.target.value}`, { method: 'POST' }); await refresh(); });
byId('attempt').addEventListener('click', async () => { byId('attempt').disabled = true; await fetch('/attempt', { method: 'POST' }); await refresh(); });
refresh(); setInterval(refresh, 750);
