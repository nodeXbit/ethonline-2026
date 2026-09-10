import { credentialIdentity } from '../ensv2/access-record.mjs';

// Public demo identity only. Generic persistent-access defaults remain cred-001.
export const demoConfig = Object.freeze({
  credential: credentialIdentity('guest-001'),
  expectedOwner: '0x3419148731087b970d2059C53780163B452D5FF7',
  resourceName: 'demo-access.eth:door-001',
  resourceId: '0xf2bde8f2654ee7267a06860eca03e0935d8818b006f5f51c2fce9d56a9b441cd',
  chainId: 11155111,
  expectedResolver: '0x0B723c0C2170F2ea508F2f4e3e122C6c0782744C',
  validityMinimumRemainingSeconds: 86400n,
});
