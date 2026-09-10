import { GateELineBuffer } from '../scripts/security/gate-e-secure-bridge.mjs';

export function selectController(devices) {
  const candidates = devices.filter(device =>
    (device.vendorId?.toUpperCase() === '1A86' && device.productId?.toUpperCase() === '55D3') ||
    /\bCH343\b/i.test(`${device.friendlyName ?? ''} ${device.manufacturer ?? ''}`));
  if (candidates.length !== 1) throw new Error(candidates.length ? 'CH343_AMBIGUOUS' : 'CH343_UNAVAILABLE');
  return candidates[0];
}

export function readinessObserver() {
  const expected = ['PN532_FIRMWARE=1.6', 'GATE_E_READY', 'PRESENT_SEEKER'];
  let index = 0;
  let failed = false;
  const buffer = new GateELineBuffer();
  return {
    receive(chunk) {
      const { lines, overflow } = buffer.push(chunk);
      if (overflow) failed = true;
      for (const raw of lines) {
        const line = raw.replace(/\r$/, '');
        if (line.startsWith('GATE_E: STOP') ||
            (line.startsWith('PN532_FIRMWARE=') && line !== expected[0]) ||
            /^(TARGET_ACTIVATION:|SELECT:|WAITING_CHALLENGE|PROOF=|AUTHORIZATION:)/.test(line)) failed = true;
        if (line === expected[index]) index++;
      }
    },
    result: () => failed ? 'FIRMWARE_NOT_READY' : index === expected.length
      ? 'BOOT_OBSERVED' : 'MANUAL_COLD_BOOT_REQUIRED',
  };
}

export async function inspectSerial({ SerialPort, path, observeMs = 5000, platform = process.platform }) {
  // The installed Windows binding disables DTR with hupcl:false and RTS with rtscts:false.
  // Do not assume other drivers/platforms have the same reset behavior.
  if (platform !== 'win32') throw new Error('PASSIVE_SERIAL_UNSUPPORTED');
  const observer = readinessObserver();
  const port = new SerialPort({ path, baudRate: 115200, autoOpen: false, lock: true,
    hupcl: false, rtscts: false });
  let failure;
  const onData = chunk => observer.receive(chunk);
  const onError = () => { failure = 'SERIAL_ERROR'; };
  port.on('data', onData);
  port.on('error', onError);
  try {
    await new Promise((resolve, reject) => {
      let abandoned = false;
      const timer = setTimeout(() => { abandoned = true; reject(new Error('SERIAL_OPEN_TIMEOUT')); }, 5000);
      port.open(error => {
        clearTimeout(timer);
        if (abandoned) {
          if (!error && port.isOpen) port.close(() => {});
          return;
        }
        if (error) reject(new Error('SERIAL_BUSY_OR_UNAVAILABLE'));
        else resolve();
      });
    });
    await new Promise(resolve => setTimeout(resolve, observeMs));
    if (failure || !port.isOpen) throw new Error(failure ?? 'SERIAL_CLOSED');
    return observer.result();
  } finally {
    // No write(), set(), reset, protocol command, or challenge exists in this path.
    if (port.isOpen) await new Promise((resolve, reject) => {
      const timer = setTimeout(() => reject(new Error('SERIAL_CLOSE_TIMEOUT')), 5000);
      port.close(error => { clearTimeout(timer); error ? reject(new Error('SERIAL_CLOSE_FAILED')) : resolve(); });
    });
    port.off('data', onData);
    port.off('error', onError);
  }
}
