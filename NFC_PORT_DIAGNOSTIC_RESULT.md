# Diagnóstico de COM3/COM4 con firmware legacy

Fecha: 2026-09-12. Horas UTC; Madrid = UTC + 2.

**Conclusión comprobada: el error de inicialización I2C no es exclusivo del firmware dinámico. Se reprodujo en el legacy sin modificar ni volver a cargar su binario. La causa concreta sigue sin determinarse.**

## Condiciones y autorización

El usuario conectó COM3 además de COM4 y autorizó las pruebas necesarias. Se identificaron puertos, se observaron datos serie y se realizaron dos identificaciones ROM con reinicio controlado. No hubo escritura/borrado de flash, recompilación ni modificación de código. El legacy instalado sigue siendo SHA-256 `4695b8b862d2001603d754495e16e26872558d1aa5a2fb98ae79f1bd2c7e4d2e`.

La conexión adicional modifica la configuración respecto a A1/A2; no se presenta este ensayo como una comparación con alimentación/cableado idénticos a aquellas capturas. No se cambió el cableado I2C ni su selector.

## Identificación de puertos

| Puerto | Dispositivo |
|---|---|
| COM4 | CH343 USB-UART, VID 1A86 / PID 55D3 |
| COM3 | USB-Serial/JTAG nativo Espressif, VID 303A / PID 1001 |

La lectura ROM por COM3 confirmó ESP32-S3 QFN56 revisión v0.2 y MAC `<DEVICE_MAC>`, coincidente con las identificaciones anteriores por COM4. Son dos interfaces del mismo controlador.

Comando usado para cada identificación/reinicio, sin flash:

```powershell
esptool.exe --port COM3 --no-stub --after hard-reset chip-id
```

## Resultados observados

### 1. Observación pasiva

A las 10:23:26 se abrieron COM4 y COM3 a 115200. Durante 15 segundos no se recibieron bytes. Se liberó COM3 para identificar el chip y se mantuvo la captura de COM4.

### 2. Primer arranque controlado: PASS

La identificación por COM3 terminó correctamente y reinició el chip. COM4 capturó:

```text
ESP-ROM:esp32s3-20210327
rst:0x15 (USB_UART_CHIP_RESET),boot:0x8 (SPI_FAST_FLASH_BOOT)
PN532_FIRMWARE=1.6
GATE_E_READY
PRESENT_SEEKER
```

Readiness completo a las 10:24:02 UTC. La captura terminó con 465 bytes, 18 líneas y cero líneas desconocidas. Esto demuestra que COM4 puede recibir el arranque y que el legacy puede inicializar el PN532 en esta configuración.

### 3. Segundo arranque controlado: FAIL en INITIALIZATION

Se preparó el intercambio legacy con la opción existente de Android. El usuario confirmó que la primera wallet embebida es el holder `0x3419…5FF7`. Se abrió de nuevo COM4 y se repitió la identificación/reinicio por COM3, sin cambiar el binario ni desconectar alimentación entre ambos arranques.

A las 10:28:06 UTC se capturó:

```text
GATE_E: STOP - INITIALIZATION: PN532 did not ACK at I2C address 0x24
```

El ensayo se detuvo y cerró COM4. SELECT, desafío y firma no se alcanzaron. Desafíos emitidos: 0; pruebas recibidas: 0; comandos de autorización: 0; consultas de política ENS y escrituras blockchain: 0.

## Interpretación y límites

- El mismo legacy produjo un arranque correcto y otro con falta de ACK en I2C. La inicialización es intermitente en las condiciones observadas.
- El error explícito de ACK coincide con el de los intentos previos del firmware dinámico de diagnóstico. Por tanto, ese error no depende exclusivamente de la lógica dinámica/GET_CREDENTIAL.
- El fallo ocurre antes de SELECT y de los 104 bytes del desafío. ENS, firma y tamaño de ese desafío no intervienen todavía en esta etapa.
- No se ha aislado la causa dentro de la capa común: estado de arranque/reset, comunicación I2C, alimentación, dispositivo o software compartido. Que COM3 permitiera observar un arranque no demuestra por sí solo que arregle el problema ni que el CH343 esté averiado.
- La identificación ROM entra deliberadamente en modo descarga antes de regresar al arranque de flash. El mensaje de descarga de esa secuencia no prueba que el dispositivo estuviera previamente atascado en ese modo.
- No se ha resuelto ni reproducido aquí el fallo anterior de SEND_CHALLENGE: ningún intercambio NFC completo se ejecutó en estos dos arranques.
- No se ha ejecutado la fase B ni se ha flasheado firmware dinámico.

## Estado final

Repetición posterior, autorizada tras otra reconexión del usuario: a las 10:34:03 UTC el legacy volvió a producir el STOP de falta de ACK I2C en 0x24. Se observaron previamente el arranque SPI_FAST_FLASH_BOOT y la entrada al firmware por COM4; la captura recibió 506 bytes, sin líneas desconocidas. No hubo flash, código modificado, SELECT, desafío ni prueba. La reconexión no eliminó el fallo en esta ejecución. Evidencia adicional: [resultado tras reconectar](.runtime/nfc-reconnected-legacy-boot-result.json) y [captura completa sanitizada](.runtime/nfc-reconnected-legacy-boot-events.jsonl).

Legacy permanece instalado. COM4 cerrado, captura Android detenida y opción temporal legacy restaurada a OFF y verificada. El usuario conserva COM3 y COM4 conectados. No se alteró código.

## Evidencia

- [Resultado conjunto](.runtime/nfc-port-diagnostic-result.json).
- [Captura de los dos puertos y primer arranque](.runtime/nfc-dual-port-diagnostic-events.jsonl).
- [Primera identificación ROM](.runtime/nfc-com3-rom-identification.log).
- [Segundo arranque y STOP](.runtime/nfc-legacy-usb-reset-exchange-events.jsonl).
- [Resultado del segundo ensayo](.runtime/nfc-legacy-usb-reset-exchange-result.json).
