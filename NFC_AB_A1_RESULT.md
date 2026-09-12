# Resultado A1: firmware legacy

Fecha: 2026-09-12. Plan: [NFC_AB_TEST_PLAN.md](NFC_AB_TEST_PLAN.md).

**Resultado: INCONCLUSO ANTES DE NFC. Firmware legacy cargado y verificado; arranque no observado mediante los marcadores capturados. B no ejecutado.**

## Ejecución

- El usuario autorizó expresamente la fase A.
- Se detuvo el monitor dinámico después de comprobar que no tenía un intento activo.
- Se cargó el artefacto legacy existente, sin recompilar ni modificar código: `.runtime/firmware-legacy/pn532_secure_access.ino.bin`.
- SHA-256: `4695b8b862d2001603d754495e16e26872558d1aa5a2fb98ae79f1bd2c7e4d2e`.
- La carga terminó con código 0; esptool verificó los cuatro segmentos escritos y efectuó su reset final habitual por RTS.
- No se solicitó cambiar cables, selector, fuente de alimentación, teléfono ni APK, ni desconectar alimentación. Se conservó COM4 y el mismo montaje.
- Se activó temporalmente la opción existente de Android `Legacy NFC regression mode`. El APK mostró tres wallets embebidas; la identidad de la primera wallet del SDK seguía pendiente de verificación. No se emitió un desafío.
- Se abrió la captura serie a 115200 a las 10:05:16 UTC (12:05:16 Madrid), con el teléfono alejado.
- El usuario indicó que aún no había pulsado RESET. Sin cerrar el puerto ni cambiar el firmware, se inició otra ventana de observación de 120 segundos a las 10:07:14 UTC. Esto no amplió un intercambio NFC: todavía no había comenzado ninguno.
- Posteriormente el usuario confirmó que sí pulsó RESET en esa segunda ventana. No hay medida independiente del instante exacto del botón.
- A las 10:09:14 UTC agotó la ventana y se cerró COM4. No se capturaron marcadores reconocidos de firmware ni eventos nuevos de Android HCE.
- Se desactivó de nuevo el modo legacy Android y se cerró la consola de captura. El firmware legacy permanece instalado; no se ejecutó B.

## Etapas

| Etapa | Resultado |
|---|---|
| Carga del artefacto correcto | PASS |
| PN532 init | NOT_OBSERVED |
| SELECT | NOT_REACHED |
| Intercambio legacy 104/67 bytes | NOT_REACHED |
| Comprobación local de firma | NOT_REACHED |
| Cierre de captura serie | Completado |

No hubo desafíos, firmas recibidas, comandos de autorización ni escrituras blockchain. No se ejecutó un tap coordinado.

## Límite de la captura y decisión

El observador guardaba líneas completas de una lista permitida, incluyendo readiness y STOP, y redactaba pruebas. No guardó un contador de bytes UART crudos ni las líneas no reconocidas. Por ello, la falta de marcadores **no demuestra que se recibieran cero bytes**, ni que el PN532 fallara en su inicialización.

Los intentos dinámicos anteriores sí habían producido un STOP explícito de falta de ACK I2C en 0x24. A1 no reprodujo ese marcador: no es correcto equiparar ambos resultados.

**Decisión:** no hay evidencia suficiente para clasificar la causa como transporte común o firmware dinámico específico. El ensayo quedó sin verificación de arranque, antes de intervenir Android/NFC. Un resultado negativo de observación no se presentará como prueba de avería del PN532 ni como éxito del legacy.

Antes de una nueva comparación hace falta una observación de arranque interpretable con el mismo montaje. No se cambia código ni se inicia otra carga/ensayo automáticamente. B permanece pendiente de autorización.

## Evidencias

- [Registro de carga](.runtime/nfc-ab-a1-flash.json).
- [Salida de esptool](.runtime/nfc-ab-a1-flash.log).
- [Eventos serie sanitizados](.runtime/nfc-ab-a1-events.jsonl).
- [Resultado A1 y captura Android](.runtime/nfc-ab-a1-result.json).
