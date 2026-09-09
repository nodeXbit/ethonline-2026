import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';
import { createAccessActionHandler, createJsonRequester } from './public/access-action.js';

const inactive = {
  status: 'REGISTERED', owner: '0x1111111111111111111111111111111111111111',
  resolver: '0x2222222222222222222222222222222222222222',
  access: { active: false, validUntil: '1800000000' }, authorization: 'DENY',
};
const active = { ...inactive, access: { active: true, validUntil: '1800000000' },
  authorization: 'ALLOW' };

test('structured non-2xx API error is rendered with its safe stage and card preserved', async () => {
  const feedback = [];
  const pending = [];
  let renders = 0;
  const requestJson = createJsonRequester(async () => ({
    ok: false,
    status: 503,
    json: async () => ({ error: {
      code: 'PRE_SUBMISSION_RPC_FAILED',
      message: 'Sepolia RPC temporarily unavailable before transaction submission. No transaction was sent. Retry is safe.',
      stage: 'Sepolia connection', retryable: true, noTransactionSent: true,
    } }),
  }));
  const changeAccess = createAccessActionHandler({
    getCredential: () => inactive,
    isCredentialUsable: () => true,
    requestJson,
    render: () => { renders++; },
    setPending: (action, value) => pending.push([action, value]),
    setFeedback: (message, tone) => feedback.push({ message, tone }),
    shorten: value => value,
  });

  await changeAccess();
  assert.equal(renders, 0);
  assert.deepEqual(pending, [['activate', true], ['activate', false]]);
  assert.equal(feedback.at(-1).tone, 'error');
  assert.match(feedback.at(-1).message, /No transaction was sent\. Retry is safe/);
  assert.match(feedback.at(-1).message, /Stage: Sepolia connection/);
  assert.doesNotMatch(feedback.at(-1).message, /^HTTP request failed\.$/);
});

test('non-2xx error containing a submitted hash remains locked and never repeats POST', async () => {
  const hash = `0x${'34'.repeat(32)}`;
  let posts = 0;
  const feedback = [];
  const pending = [];
  const requestJson = createJsonRequester(async () => {
    posts++;
    return {
      ok: false, status: 500,
      json: async () => ({ error: { message: 'Submitted transaction needs inspection.',
        stage: 'Sepolia transaction receipt', transactionHash: hash } }),
    };
  });
  const changeAccess = createAccessActionHandler({
    getCredential: () => inactive,
    isCredentialUsable: () => true,
    requestJson,
    render: () => assert.fail('confirmed card must be preserved'),
    setPending: (action, value) => pending.push([action, value]),
    setFeedback: (message, tone) => feedback.push({ message, tone }),
    shorten: value => value,
  });

  await changeAccess();
  await changeAccess();
  assert.equal(posts, 1);
  assert.deepEqual(pending, [['activate', true]]);
  assert.equal(feedback.at(-1).tone, 'warning');
  assert.match(feedback.at(-1).message, /Do not click again/);
  assert.match(feedback.at(-1).message, new RegExp(hash));
});

test('double invocation starts one POST and a failed reconciliation preserves POST state', async () => {
  let credential = inactive;
  let releasePost;
  const postBlocked = new Promise(resolve => { releasePost = resolve; });
  const requests = [];
  const rendered = [];
  const feedback = [];
  const pending = [];
  const changeAccess = createAccessActionHandler({
    getCredential: () => credential,
    isCredentialUsable: () => true,
    requestJson: async (path, options) => {
      requests.push({ path, method: options?.method ?? 'GET' });
      if (options?.method === 'POST') {
        await postBlocked;
        return { changed: true, transactionHash: `0x${'ab'.repeat(32)}`, credential: active };
      }
      throw new Error('reconciliation unavailable');
    },
    render: value => { credential = value; rendered.push(value); },
    setPending: (action, value) => pending.push([action, value]),
    setFeedback: (message, tone) => feedback.push({ message, tone }),
    shorten: value => value,
  });

  const first = changeAccess();
  const second = changeAccess();
  await second;
  assert.deepEqual(requests, [{ path: '/api/activate', method: 'POST' }]);
  assert.deepEqual(pending, [['activate', true]]);

  releasePost();
  await first;
  assert.deepEqual(requests, [
    { path: '/api/activate', method: 'POST' },
    { path: '/api/credential', method: 'GET' },
  ]);
  assert.deepEqual(rendered, [active]);
  assert.equal(credential, active);
  assert.deepEqual(pending, [['activate', true], ['activate', false]]);
  assert.equal(feedback.at(-1).tone, 'warning');
  assert.match(feedback.at(-1).message, /showing confirmed state/);
});

test('HTTP 202 performs only read-only reconciliation and never repeats POST', async () => {
  let credential = inactive;
  let reads = 0;
  const requests = [];
  const rendered = [];
  const feedback = [];
  const pending = [];
  const changeAccess = createAccessActionHandler({
    getCredential: () => credential,
    isCredentialUsable: () => true,
    requestJson: async (path, options) => {
      requests.push({ path, method: options?.method ?? 'GET' });
      if (options?.method === 'POST') {
        return { pending: true, transactionHash: `0x${'cd'.repeat(32)}`,
          requestedState: 'ACTIVE', transactionState: 'CONFIRMATION_UNKNOWN' };
      }
      reads++;
      return reads === 1 ? inactive : active;
    },
    render: value => { credential = value; rendered.push(value); },
    setPending: (action, value) => pending.push([action, value]),
    setFeedback: (message, tone) => feedback.push({ message, tone }),
    shorten: value => value,
    recoveryDelays: [0, 0],
    wait: async () => {},
  });

  await changeAccess();
  assert.deepEqual(requests, [
    { path: '/api/activate', method: 'POST' },
    { path: '/api/credential', method: 'GET' },
    { path: '/api/credential', method: 'GET' },
  ]);
  assert.deepEqual(rendered, [active]);
  assert.deepEqual(pending, [['activate', true], ['activate', false]]);
  assert.ok(feedback.some(item => item.message.includes('TRANSACTION SUBMITTED')));
  assert.equal(feedback.at(-1).tone, 'success');
});

test('unresolved HTTP 202 stays locked and tells the user to reload', async () => {
  const requests = [];
  const feedback = [];
  const pending = [];
  const changeAccess = createAccessActionHandler({
    getCredential: () => inactive,
    isCredentialUsable: () => true,
    requestJson: async (path, options) => {
      requests.push({ path, method: options?.method ?? 'GET' });
      if (options?.method === 'POST') {
        return { pending: true, transactionHash: `0x${'ef'.repeat(32)}`,
          requestedState: 'ACTIVE', transactionState: 'CONFIRMED_SUCCESS' };
      }
      return inactive;
    },
    render: () => assert.fail('stale state must not be rendered as the target'),
    setPending: (action, value) => pending.push([action, value]),
    setFeedback: (message, tone) => feedback.push({ message, tone }),
    shorten: value => value,
    recoveryDelays: [0, 0],
    wait: async () => {},
  });

  await changeAccess();
  await changeAccess();
  assert.equal(requests.filter(item => item.method === 'POST').length, 1);
  assert.equal(requests.filter(item => item.method === 'GET').length, 2);
  assert.deepEqual(pending, [['activate', true]]);
  assert.ok(feedback.some(item => item.message.includes('CONFIRMED ON SEPOLIA')));
  assert.match(feedback.at(-1).message, /Reload to reconcile from ENSv2/);
  assert.equal(feedback.at(-1).tone, 'warning');
});

test('reload recovery performs GET polling without any POST', async () => {
  const recovery = { pending: true, transactionHash: `0x${'12'.repeat(32)}`,
    requestedState: 'ACTIVE', transactionState: 'CONFIRMATION_UNKNOWN' };
  let credential = { ...inactive, recovery };
  const requests = [];
  const pending = [];
  const changeAccess = createAccessActionHandler({
    getCredential: () => credential,
    isCredentialUsable: () => false,
    requestJson: async (path, options) => {
      requests.push({ path, method: options?.method ?? 'GET' });
      return active;
    },
    render: value => { credential = value; },
    setPending: (action, value) => pending.push([action, value]),
    setFeedback: () => {},
    shorten: value => value,
    recoveryDelays: [0],
    wait: async () => {},
  });

  await changeAccess.recover(recovery);
  assert.deepEqual(requests, [{ path: '/api/credential', method: 'GET' }]);
  assert.equal(credential, active);
  assert.deepEqual(pending, [['activate', true], ['activate', false]]);
});

test('markup and event wiring expose only one non-submit action path', async () => {
  const [html, app] = await Promise.all([
    readFile(new URL('./public/index.html', import.meta.url), 'utf8'),
    readFile(new URL('./public/app.js', import.meta.url), 'utf8'),
  ]);
  assert.match(html, /<button id="action-button" type="button"/);
  assert.doesNotMatch(html, /<form\b/i);
  assert.equal((app.match(/addEventListener\(['"]click['"], changeAccess\)/g) ?? []).length, 1);
  assert.doesNotMatch(app, /addEventListener\(['"]submit['"]/);
});
