import { readFileSync } from 'node:fs';
import { decodeAbiParameters, encodeAbiParameters, keccak256, stringToHex } from 'viem';

export const resourceKey = 'resources.v1';
export const resourceSchema = [{ type: 'bytes32[]' }];
export function validateResources(config) {
  const slugs = ['front-door', 'lab', 'server-room'];
  if (config?.version !== 1 || config.resources?.length !== 3) throw Error('RESOURCE_CONFIG_INVALID');
  for (const [index, item] of config.resources.entries()) {
    if (item.slug !== slugs[index] || item.displayName !== ['Front Door', 'Lab', 'Server Room'][index] ||
        item.resourceId !== keccak256(stringToHex(`lockens:resource:v1:${item.slug}`))) throw Error('RESOURCE_CONFIG_INVALID');
  }
  return Object.freeze(config.resources.map(item => Object.freeze({ ...item })));
}
export const resources = validateResources(JSON.parse(readFileSync(new URL('../../config/access-resources.json', import.meta.url))));
export function resourceBySlug(slug) {
  const value = resources.find(item => item.slug === slug);
  if (!value) throw Error('UNKNOWN_RESOURCE');
  return value;
}
export function encodeResources(ids) {
  if (!Array.isArray(ids) || ids.length > 3 || ids.some(id => !resources.some(r => r.resourceId === id))) throw Error('RESOURCE_POLICY_INVALID');
  return encodeAbiParameters(resourceSchema, [[...new Set(ids)].sort()]);
}
export function decodeResources(raw) {
  if (raw === '0x' || raw == null) return null;
  try {
    if (typeof raw !== 'string' || !/^0x[0-9a-fA-F]+$/.test(raw) || raw.length > 322) throw Error();
    const [ids] = decodeAbiParameters(resourceSchema, raw);
    if (encodeResources(ids) !== raw.toLowerCase()) throw Error();
    return ids;
  } catch { throw Error('RESOURCE_POLICY_INVALID'); }
}
