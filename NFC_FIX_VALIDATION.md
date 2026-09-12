# Corrección del diagnóstico NFC — 12 septiembre 2026

## Validación NFC completa posterior: PASS

A las 11:32 UTC (13:32 Madrid), un tap confirmado por el usuario completó SELECT → GET_CREDENTIAL → SEND_CHALLENGE → STATUS READY → GET_SIGNATURE → verificación → confirmación del controlador.

- SELECT: Android devolvió 9000; PN532 confirmó intercambio correcto.
- GET_CREDENTIAL: STAFF exacto, 30 bytes de nombre más 9000.
- SEND_CHALLENGE: Android recibió 109 bytes, coincidieron credencial y recurso, y devolvió 9000. PN532 registró ACK=0, estado de controlador 00 y respuesta de 2 bytes 9000. La trama I2C de 119 bytes funcionó en este intento.
- GET_SIGNATURE: Android devolvió 67 bytes incluyendo 9000; el host recibió una firma de 65 bytes. El monitor confirmó titular, registro y prueba fresca.
- Decisión: ACCESS DENIED / RESOURCE_POLICY_MISSING, confirmada por el controlador. Es la denegación de política esperada para STAFF/Lab; no fue un fallo de transporte. Sin escrituras blockchain ni cambios en Android.

Limitación: antes de este tap, otro RESET manual produjo WIRE_RC=2; el siguiente produjo 0 y READY. La inicialización sigue siendo intermitente. Un intercambio completo valida el protocolo en este intento, pero no demuestra fiabilidad sostenida ni identifica la causa de las fallas previas.

Evidencia consolidada: `.runtime/nfc-init-audit-hce-result.json`; intento previo fallido: `.runtime/nfc-init-audit-hce-blocked.json`; captura Android: `.runtime/nfc-init-audit-hce-validation.log`. Monitor dejado inactivo con el resultado final visible, puerto serie cerrado y captura Android detenida.

## Implementado

- Monitor: separa inicialización, lector listo y activación NFC. Los fallos técnicos muestran SESSION ERROR; ACCESS DENIED se conserva para una decisión confirmada. Un fallo de confirmación nunca muestra ACCESS GRANTED.
- Un STOP de inicialización conserva READER_INITIALIZATION_FAILED. GET_CREDENTIAL fallido ya no se interpreta automáticamente como falta de selección. El vencimiento de escucha conserva SESSION_TIMEOUT.
- Firmware: registra el retorno numérico de la misma comprobación I2C existente, sin repetirla ni cambiar sus argumentos. El mensaje ya no afirma que todos los errores sean NACK. Esta compilación incluye la instrumentación PN532 interna que ya existía en el código antes de esta tarea; no es idéntica al binario APDU restaurado.
- Monitor y observador aceptan únicamente el formato acotado de ese diagnóstico; no publican respuestas arbitrarias del dispositivo.

## Validación

53 tests Node aprobados (puente dinámico, verificador de recursos y servidor HTTP), incluidos reproducción del STOP antes de tap, conservación de código I2C, transición a lector listo y ausencia de concesión sin confirmación. Ejecutados sin aislamiento de procesos por las restricciones de Windows. Sin nuevas pruebas físicas. Sin cambios en Android, cableado, I2C, tamaños APDU, TTL ni política de acceso. No se ha flasheado.

## Siguiente prueba física, tras autorización de flasheo

Compilación completada: ESP32-S3, 306177 bytes de programa y 23152 bytes de variables globales. Binario de 306320 bytes en `.runtime/firmware-dynamic-init-audit/pn532_dynamic_access.ino.bin`; hash y resultado registrados en `.runtime/nfc-init-audit-build.json`.

1. Registrar hash del nuevo binario y preservar el restaurado `.runtime/firmware-dynamic-apdu/pn532_dynamic_access.ino.bin` para reversión.
2. Mantener COM3 y COM4, alimentación y cableado en su configuración actual. Seeker alejado durante toda la comprobación de arranque.
3. Detener el monitor anterior antes de usar el puerto para flashear. Flashear únicamente el binario identificado; conservar salida y verificación de hashes.
4. Arrancar el observador actualizado y abrir una sola ventana de escucha. Usar RESET manual una vez; no usar esptool para reiniciar durante esta comparación. El reset posterior al flasheo se registra por separado.
5. Capturar PN532_INIT_V1 y, si el retorno es 0, PN532_FIRMWARE y PRESENT_SEEKER. No solicitar tap todavía. Un timeout de la ventana no se cuenta como intento de acceso.
6. Interpretación: 5 es timeout; 4 es otro error; 2 es el resultado de fallo/NACK que entrega Wire; 0 permite pasar a consulta de versión y configuración. Si faltan registros, clasificar como captura/arranque inconcluso. No inferir un componente averiado.
7. Repetir únicamente el control de arranque con las mismas condiciones antes de ensayar SELECT. Una vez estable, retomar el intercambio con STAFF y correlacionar lector/Android. El fallo de SEND_CHALLENGE sigue pendiente; cambiar la etiqueta del monitor no lo resuelve.

La aplicación del monitor que ya estuviera ejecutándose necesita reiniciarse para cargar este código. No se considera actualizada una sesión antigua por haber cambiado los archivos.

## Resultado físico posterior, autorizado

Flasheado el binario SHA-256 `13528830db96b27c84dc14d7e8ccc6a46822ada30a959aa7f70d6071471e5bb0` en COM4: salida 0 y cuatro hashes verificados. Observador reiniciado con el código actualizado.

Dos RESET manuales confirmados por el usuario, con Seeker alejado, alcanzaron WIRE_RC=0, PN532_FIRMWARE=1.6 y PRESENT_SEEKER a las 11:27:38 y 11:28:07 UTC. El monitor mostró READER_READY, final=null. No hubo tap, SELECT, challenge ni decisión de acceso. Se cerró deliberadamente el observador al completar el control; no fue un fallo de la web.

Evidencia: `.runtime/nfc-init-audit-upload.log`, `.runtime/nfc-init-audit-boot-result.json` y `.runtime/dynamic-apdu-status-events.jsonl`. El arranque pasa en estas dos ejecuciones; no demuestra la causa de las fallas anteriores ni resuelve todavía SEND_CHALLENGE. Siguiente paso: captura correlacionada de lector y Android con STAFF, usando el mismo RESET manual y sin cambiar conexiones.
