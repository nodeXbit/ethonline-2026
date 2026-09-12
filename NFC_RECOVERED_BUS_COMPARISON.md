# Comparación NFC después de recuperar SCL

## Resultado posterior autorizado

Restaurado binario 8051a772…fc98e9a, salida 0 y cuatro hashes verificados. Dos RESET manuales confirmados; sin tap ni cambios de cableado.

- 12:48:43 UTC: entrada SCL=1/SDA=1; SAM ESP-IDF=0, escritura 1829 us, ACK=0; configuración, versión 1.6 y READY correctos.
- 12:49:23 UTC: RESET desde lector READY/en búsqueda. Entrada SCL=1/SDA=0. Tras envío: estado ACK_ERROR=6, evento DONE=1, ambas líneas altas. El evento DONE posterior no borra el estado ACK_ERROR; no representa un envío válido. Reset interno sin limpieza devuelve 0; retorno final ESP-IDF=259, Wire=4, escritura 453 us, ACK=-2 tras 22443 us. Arranque detenido.

La segunda falla difiere de la anterior SCL baja/TIMEOUT sin evento. El mismo 0x103 estaba agrupando condiciones distintas. La comparación demuestra un fallo observado al reiniciar en caliente desde búsqueda NFC, no todavía su mecanismo causal completo. La lectura SDA baja inicial puede indicar una transacción/estado pendiente; no demuestra por sí sola quién conduce la línea.

Evidencia consolidada `.runtime/nfc-recovered-bus-result.json`, carga `.runtime/nfc-recovered-bus-upload.log`. Monitor inactivo, COM4 cerrado; firmware diagnóstico NFC instalado. Siguiente revisión: cómo finalizar o abortar la operación PN532 pendiente y resincronizar el enlace al reiniciar solo el ESP32, conservando validación de respuestas. No aumentar el timeout como explicación del ACK_ERROR.

Estado de partida: diagnóstico de líneas instalado; tras apagado completo ambas líneas altas en 30/30 muestras. Evidencia preservada en nfc-line-after-powercycle-result.json. No hay NFC activo.

Restauración propuesta: binario existente `.runtime/firmware-driver-state/pn532_dynamic_access.ino.bin`, 307104 bytes, SHA-256 `8051a772d441f39309e07bbf115b4d001430a9ce08da6f237ddf3e529fc98e9a`, nuevamente verificado. Conserva la traza interna y no requiere nuevos cambios de firmware. Observador actualizado comprobado sintácticamente.

Tras autorización de flasheo, registrar por separado el reset automático de carga: puede ejecutar el arranque y activar la búsqueda NFC antes de abrir la captura. No clasificar un RESET manual posterior como primer arranque en frío. Si se necesita observar realmente el primer arranque, detenerse y preparar captura que cubra la reanudación de ejecución; no inferirlo de un registro tardío.

Primera comprobación: abrir captura, pedir un RESET manual con Seeker alejado y registrar entrada SCL/SDA, SAM y READY o fallo. Si pasa, pedir otro RESET individual manteniendo la misma sesión y registrar si aparece SCL baja al reiniciar desde búsqueda NFC. Detener ante fallo, sin repetir sobre bus retenido. No pedir tap hasta terminar la comparación.

Si falla la primera captura tras la carga, no permite concluir si la retención la produjo el flasheo, su arranque automático u otra condición previa. Conservar esta limitación explícitamente. No atribuir causalidad al reset o al PN532 solo por correlación.

La restauración está preparada, no ejecutada en este paso.
