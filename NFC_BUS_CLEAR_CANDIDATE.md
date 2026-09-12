# RecuperaciÃ³n de bus antes del arranque PN532

Estado: candidato compilado, pendiente de autorizaciÃ³n de flasheo y validaciÃ³n fÃ­sica. No se ha cambiado el firmware instalado en este paso.

## Evidencia y alcance

La comparaciÃ³n anterior registrÃ³ READY a las 12:48:43 UTC y, tras RESET en caliente, entrada SCL=1/SDA=0 con ACK_ERROR en SAM a las 12:49:23 UTC. No prueba quiÃ©n mantenÃ­a SDA baja ni que esta recuperaciÃ³n vaya a resolver todos los fallos.

NXP UM10204, secciÃ³n 3.1.16, prescribe hasta nueve pulsos de reloj para SDA retenida. Si SCL permanece baja, indica reset del dispositivo o ciclo de alimentaciÃ³n. Fuente: https://www.nxp.com/docs/en/user-guide/UM10204.pdf

El manual PN532 admite abortar una operaciÃ³n mediante trama ACK o un comando nuevo. SAM ya es un comando nuevo: aÃ±adir otra trama no garantiza resolver un enlace que no logra transmitir. No se aÃ±ade una trama de aborto en este candidato. Fuente: https://www.nxp.com/docs/en/user-guide/141520.pdf, secciÃ³n 6.2.2.2.

## Cambio

`firmware/pn532_secure_access/pn532_bus_recovery.h` se ejecuta Ãºnicamente en el firmware dinÃ¡mico, antes de Wire.begin. Observa GPIO17/18 como entradas con pull-up. Espera SCL alta hasta 10 ms; si SDA estÃ¡ baja, usa salidas open-drain, hasta nueve pulsos con comprobaciÃ³n de SCL real y un STOP cuando SDA se libera. HIGH significa liberar, nunca conducir activamente a nivel alto. Siempre devuelve ambos pines a entrada. Si las lÃ­neas no quedan altas, detiene INITIALIZATION. No hay reintentos indefinidos.

La espera de 10 ms es un lÃ­mite de implementaciÃ³n, no una garantÃ­a temporal del PN532. SCL retenida de forma persistente sigue sin tener recuperaciÃ³n por software demostrada.

Se conservan el driver instrumentado, la secuencia SAM/version/RF y todas las comprobaciones del intercambio NFC. El observador registra `PN532_BUS_CLEAR` con niveles iniciales/finales, pulsos y resultado. Debe reiniciarse para cargar ese parser antes de la prueba.

## ValidaciÃ³n realizada

- Arduino CLI, ESP32 3.3.11, fqbn esp32:esp32:esp32s3: compilaciÃ³n correcta, programa 307629 bytes, globales 23496.
- Binario `.runtime/firmware-bus-clear/pn532_dynamic_access.ino.bin`: 307776 bytes; SHA-256 `a737125122fe776f5aa9025e69fdaa61e3df93da93edc2c378715c6354fd20ec`.
- Mapa de enlace: i2c_master_transmit y lockensTraceArm proceden del mismo objeto instrumentado usado en la comparaciÃ³n anterior.
- Sintaxis del observador y git diff --check correctos. No se han ejecutado pruebas fÃ­sicas ni simulaciones elÃ©ctricas.

CompilaciÃ³n reproducible:

```powershell
& 'C:/Program Files/Arduino CLI/arduino-cli.exe' compile --fqbn esp32:esp32:esp32s3 --library .runtime/driver-state-libraries/Wire --library .runtime/driver-state-libraries/PN532_I2C --build-path .runtime/build-driver-state --output-dir .runtime/firmware-bus-clear firmware/pn532_dynamic_access
```

## Prueba despuÃ©s de autorizaciÃ³n

Conservar hardware, alimentaciÃ³n, cables, I2C y telÃ©fono alejado. Registrar carga y reset automÃ¡tico por separado; no llamar arranque en frÃ­o a un RESET manual posterior. Abrir captura con el observador actualizado y pedir un RESET individual. Exigir niveles altos, SAM vÃ¡lido, versiÃ³n y READY. Si pasa, pedir un segundo RESET desde bÃºsqueda activa, en la misma captura. Ante fallo detener la serie. Un arranque que ya encuentre ambas lÃ­neas altas no demuestra que la rama de recuperaciÃ³n funcione. Para validar esa rama debe constar SDA inicial baja, pulsos, liberaciÃ³n y SAM correcto. La estabilidad requerirÃ¡ mÃ¡s reinicios individuales y despuÃ©s repetir el intercambio NFC.

## Hallazgo independiente, sin modificar

PN532::inListPassiveTarget espera readResponse con timeout 30000. En la copia actual PN532_I2C::readResponse, getResponseLength devuelve int16_t pero se asigna directamente a uint8_t: un error -1 se convierte en 255 y provoca una solicitud posterior de 263 bytes. Es un defecto verificable por inspecciÃ³n; no demuestra la causa de SDA baja al reiniciar. Se mantiene fuera de este candidato para aislar la variable del arranque y queda pendiente de correcciÃ³n y prueba especÃ­fica.

Flasheo autorizado y ejecutado el 2026-09-12: salida 0, cuatro segmentos con hash verificado. Registro .runtime/nfc-bus-clear-upload.log; límite temporal .runtime/nfc-bus-clear-boundary.json. El usuario autoriza desde este punto los flasheos necesarios para esta tarea sin nueva confirmación. Observador reiniciado con parser de recuperación; pendiente RESET manual registrado.

Validación física 2026-09-12 13:08:28 UTC, RESET individual confirmado: INITIAL_SCL=1, INITIAL_SDA=0, PULSES=7, niveles finales 1/1, OK=1. SAM: ESP-IDF=0, Wire=0, ACK=0, escritura 1828 us, ACK 1910 us; SAM y RF con WRITE_RC=0/READ_RC=0; versión 1.6 y READY. Primera recuperación observada de SDA baja seguida de arranque válido; no demuestra todavía estabilidad repetida ni recuperación de SCL retenida. Evidencia: .runtime/nfc-bus-clear-boot-capture.jsonl. Captura independiente abierta sin límite de dos minutos. La confirmación anterior no tuvo un arranque registrado y no se cuenta como éxito ni fallo del firmware.

Segundo RESET de la captura continua confirmado, 13:09:11 UTC: líneas iniciales y finales 1/1, cero pulsos, SAM IDF/Wire/ACK=0, SAM y RF respuestas válidas, READY. Total del candidato: tres arranques manuales registrados correctos (13:01:11, 13:08:28, 13:09:11), uno de ellos recuperando SDA baja con siete pulsos. Las pulsaciones no capturadas quedan excluidas. La captura continua permanece abierta; monitor web inactivo. Pendiente ampliar repetición y repetir intercambio NFC con este binario; no declarar resuelto el anterior caso de SCL baja.

Serie inicial ampliada completada a las 13:18:26 UTC: diez arranques registrados correctos, cuatro recuperaciones de SDA baja. Detalle en NFC_BOOT_STABILITY_SERIES.md. Pendientes búsqueda prolongada con intercambio NFC posterior, arranque en frío y caso SCL retenida.

Actualización: el arranque 11 falló durante SAM partiendo de ambas líneas altas; timeout del controlador con SCL baja posterior. La recuperación de SDA no cubre este caso. Ver NFC_BOOT_STABILITY_SERIES.md y .runtime/nfc-bus-clear-hce-result.json. NFC posterior aún no validado.
