import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';
import { createAccessActionHandler } from './public/access-action.js';

const inactive = {
  status: 'REGISTERED', owner: '0x1111111111111111111111111111111111111111',
  resolver: '0x2222222222222222222222222222222222222222',
  access: { active: false, validUntil: '1800000000' }, authorization: 'DENY',
};
const active = { ...inactive, access: { active: true, validUntil: '1800000000' },
  authorization: 'ALLOW' };

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
