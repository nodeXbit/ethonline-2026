import { createAccessActionHandler, createJsonRequester } from './access-action.js';

const card = document.querySelector('#key-card');
const actionButton = document.querySelector('#action-button');
const feedback = document.querySelector('#feedback');
const fields = Object.fromEntries([
  'access-state', 'authorization', 'credential-name', 'credential-status', 'access-summary',
  'holder', 'valid-until', 'token-id', 'resolver',
].map(id => [id, document.querySelector(`#${id}`)]));

let confirmedCredential = null;
let pending = false;

const zeroAddress = /^0x0{40}$/i;
const shorten = value => value && value.length > 15
  ? `${value.slice(0, 7)}…${value.slice(-5)}` : value || '—';
const shortenToken = value => value && value.length > 16
  ? `${value.slice(0, 8)}…${value.slice(-6)}` : value || '—';

function formatUnix(value) {
  if (!value) return 'Not configured';
  const milliseconds = Number(value) * 1000;
  if (!Number.isSafeInteger(milliseconds)) return `Unix ${value}`;
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium', timeStyle: 'short',
  }).format(new Date(milliseconds));
}

function setFeedback(message, tone = '') {
  feedback.textContent = message;
  feedback.className = `feedback${tone ? ` is-${tone}` : ''}`;
}

function isCredentialUsable(credential) {
  return !credential.recovery?.pending && credential.status === 'REGISTERED' &&
    !zeroAddress.test(credential.owner) &&
    !zeroAddress.test(credential.resolver) && credential.access !== null;
}

function render(credential) {
  confirmedCredential = credential;
  const active = credential.authorization === 'ALLOW' && credential.access?.active === true;
  const usable = isCredentialUsable(credential);

  card.classList.toggle('is-active', active);
  card.classList.toggle('is-unavailable', !usable);
  card.classList.remove('is-loading');
  fields['access-state'].textContent = active ? 'ACTIVE' : usable ? 'INACTIVE' : 'UNAVAILABLE';
  fields.authorization.textContent = credential.authorization;
  fields['credential-name'].textContent = credential.name;
  fields['credential-status'].textContent = credential.status;
  fields['access-summary'].textContent = active ? 'ACTIVE' : 'INACTIVE';
  fields.holder.textContent = shorten(credential.owner);
  fields.holder.title = credential.owner;
  fields['valid-until'].textContent = formatUnix(credential.access?.validUntil);
  fields['token-id'].textContent = shortenToken(credential.tokenId);
  fields['token-id'].title = credential.tokenId;
  fields.resolver.textContent = shorten(credential.resolver);
  fields.resolver.title = credential.resolver;

  actionButton.disabled = pending || !usable;
  actionButton.textContent = active ? 'Deactivate access' : usable ? 'Activate access' : 'Credential unavailable';

  if (!usable) {
    setFeedback('The persistent credential is not currently valid or its access record is unavailable.', 'error');
  } else if (credential.access.active && !active) {
    setFeedback('The onchain access flag is expired or no longer authorizes entry. Activate to renew it.');
  }
}

const requestJson = createJsonRequester();

async function refresh() {
  const credential = await requestJson('/api/credential');
  render(credential);
  return credential;
}

const changeAccess = createAccessActionHandler({
  getCredential: () => confirmedCredential,
  isCredentialUsable,
  requestJson,
  render,
  setFeedback,
  shorten,
  setPending(action, value) {
    pending = value;
    if (value) {
      actionButton.disabled = true;
      actionButton.textContent = action === 'deactivate' ? 'Deactivating…' : 'Activating…';
      setFeedback(`${action === 'deactivate' ? 'DEACTIVATING' : 'ACTIVATING'} ON SEPOLIA — DO NOT CLICK AGAIN`,
        'pending');
    } else if (confirmedCredential) {
      render(confirmedCredential);
    }
  },
});

actionButton.addEventListener('click', changeAccess);

refresh().then(async credential => {
  if (credential.recovery?.pending) {
    await changeAccess.recover(credential.recovery);
  } else {
    setFeedback('Authoritative ENSv2 state loaded.');
  }
}).catch(error => {
  card.classList.remove('is-loading');
  card.classList.add('is-unavailable');
  fields['access-state'].textContent = 'UNAVAILABLE';
  fields.authorization.textContent = 'NO DECISION';
  actionButton.textContent = 'Credential unavailable';
  actionButton.disabled = true;
  setFeedback(error instanceof Error ? error.message : 'Unable to read ENSv2 state.', 'error');
});
