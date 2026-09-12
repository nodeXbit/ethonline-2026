# Physically validated diagnostic build

These patches preserve existing instrumentation from the local libraries used by the physically validated firmware. They do not introduce a new transport implementation or fix the negative-length bug.

Reference application SHA-256: `4914015019c3659de25fd13d55ccb314b09ac4b0dfd7667f5407874e9b08ed9e` (308096 bytes). The checkpoint recompilation reproduced this hash using the existing local build directory. Different toolchains or build paths may produce different binary hashes; do not substitute an unvalidated binary based on source similarity.

## Dependencies and reconstruction

- Arduino ESP32 3.3.11, board `esp32:esp32:esp32s3`.
- ESP-IDF v5.5.5, commit `b774170ff46`.
- Local PN532 and PN532_I2C Arduino libraries: exact source fingerprints in `reference-manifest.json`. These installations have no library.properties version identifier; a matching upstream PN532 revision has not been established. This is an explicit portability limitation.
- Copy the Arduino core's `libraries/Wire` and the installed `PN532_I2C` to an isolated library root.
- Copy upstream [`i2c_master.c`](https://github.com/espressif/esp-idf/blob/b774170ff46/components/esp_driver_i2c/i2c_master.c) and [`i2c_private.h`](https://github.com/espressif/esp-idf/blob/b774170ff46/components/esp_driver_i2c/i2c_private.h) into the isolated PN532_I2C folder. Verify base fingerprints before patching; normalized hashes use UTF-8 with LF line endings.
- Apply the three patches relative to that library root (`git apply --check`, then `git apply`). Preserve the existing library layout and PN532 dependency. The patches contain only our diagnostic changes; upstream source retains its own license notices.
- Verify the patched normalized source fingerprints against the manifest. Never patch the globally installed libraries in place.

The current machine's isolated root is `.runtime/irq-cause-libraries`. Compile without flashing:

```powershell
& 'C:/Program Files/Arduino CLI/arduino-cli.exe' compile --fqbn esp32:esp32:esp32s3 --library .runtime/irq-cause-libraries/Wire --library .runtime/irq-cause-libraries/PN532_I2C --build-path .runtime/build-irq-cause --output-dir .runtime/checkpoint-firmware-dynamic firmware/pn532_dynamic_access
```

Check the linker map: `i2c_master_transmit` and `lockensTraceIrq` must resolve to the isolated PN532_I2C `i2c_master.c.o`, not the SDK's prebuilt implementation. A default dynamic build without these overrides omits the internal driver/IRQ traces and is not the referenced diagnostic artifact.

## What is preserved

- Wire's actual ESP-IDF write error before its conversion to a Wire result.
- PN532 SAM write and ACK durations/results, without logging payloads.
- Bounded driver state, event, bus-busy and SCL/SDA records, printed after the ACK operation.
- Accumulated SAM interrupt mask distinguishing the timeout and arbitration bits. ISR instrumentation adds timing overhead and does not preserve interrupt order.
- The standalone `../pn532_line_diagnostic` sketch observes released/bound line states without transmitting I2C commands. It is not the installed NFC application.

No `.runtime` binaries, raw serial proofs, APKs or private configuration are committed. Historical evidence is summarized in `../../docs/evidence/physical-nfc-2026-09-12.json` and the checkpoint report. SCL boot stability remains incompletely characterized.
