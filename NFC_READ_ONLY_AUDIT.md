# Auditoría NFC previa a cambios — 12 septiembre 2026

Alcance: revisión de código, dependencias instaladas y evidencia ya capturada. Sin cambios de código, compilaciones, flasheos, apertura de puertos ni nuevos intentos físicos. Solo se crea este informe. Las horas de evidencia indicadas aquí son UTC (Madrid: +2 horas).

## Dictamen

Hay un defecto confirmado en la presentación del monitor: convierte un fallo técnico anterior al tap en ACCESS DENIED. Esto no demuestra una denegación de la credencial ni una avería de los chips. La causa del fallo de inicialización sigue abierta. El fallo anterior de SEND_CHALLENGE es otro incidente y necesita investigarse por separado.

## Hallazgos confirmados

1. **Alta: resultado de acceso falso en términos de presentación.** `scripts/security/dynamic-gate-bridge.mjs:126` asigna ACCESS DENIED a cualquier `fail`, incluyendo TRANSPORT_FAILURE. El temporizador de 120 segundos comienza al abrir el puerto, incluso sin tap. `demo/gate-public/app.js` muestra ese resultado en rojo. Reproducción offline ejecutada con la clase real: `begin(); fail({reason:'TRANSPORT_FAILURE'})` devuelve ACCESS DENIED, sin puerto, teléfono, credencial ni prueba. El cierre seguro debe conservarse, pero distinguir error técnico, espera agotada y denegación de política.

2. **Alta: el diagnóstico I2C pierde la causa.** `firmware/pn532_secure_access/pn532_secure_access.ino:321` convierte cualquier resultado distinto de cero de `Wire.endTransmission()` en «PN532 did not ACK». La implementación instalada de Arduino ESP32 3.3.11 devuelve 2 para ciertos fallos/NACK, 4 para otros errores y 5 para timeout. El registro actual no permite distinguirlos. No es correcto inferir de esa frase que el PN532 está averiado ni que se ha medido específicamente un NACK de dirección. El código fuente actual contiene instrumentación adicional respecto al binario restaurado; no se atribuyen todas sus modificaciones a ese binario.

3. **Media: GET_CREDENTIAL también se clasifica demasiado ampliamente.** `DynamicGateBridge.receiveLine` convierte cualquier STOP en esa etapa en NO_SELECTED_CREDENTIAL, incluyendo errores de intercambio. Un fallo de transporte puede presentarse al usuario como ausencia de selección. Hace falta conservar etapa y categoría técnica antes de recomendar cambiar la credencial.

4. **Media: Android puede dejar de estar listo aunque siga apareciendo una selección.** `NfcSelectionState.current()` exige validación y snapshot recientes (60 segundos), además de propiedad y wallet vigentes. `readyText()` responde «Select a pass to use NFC» para distintas causas. `publish()` reemplaza la identidad y el procesador comprueba su identidad por referencia durante la sesión. Hay un riesgo de invalidación por caducidad o republicación; no se ha demostrado que sea la causa del fallo de SEND_CHALLENGE observado. Conviene distinguir selección, frescura, servicio y sesión activa sin relajar las comprobaciones de seguridad.

## Qué ocurrió sin acercar el Seeker

Fuente: `.runtime/restored-dynamic-attempt-result.json`.

- 11:07:26: se abrió COM4 para escuchar. El intento lo había armado el asistente mediante POST `/attempt`; no fue un tap ni una solicitud espontánea del teléfono.
- Se ejecutó un reinicio por COM3 mediante esptool, sin flashear.
- 11:07:45.936: el firmware emitió STOP en INITIALIZATION; a continuación AUTHORIZATION: DENY.
- 11:07:45.971: se cerró el puerto serie.
- 11:07:46.286: el monitor mostró ACCESS DENIED / TRANSPORT_FAILURE.
- No se alcanzaron SELECT, challenge ni prueba del titular; el registro Android estaba vacío.

La inicialización I2C precede al intercambio NFC. Por tanto, el contenido ENS enviado por Android no explica este fallo previo al tap. La inspección del frontend no encontró una llamada para cerrar la ventana: consulta `/state` cada 750 ms y muestra el estado. La secuencia registrada demuestra el cierre de la sesión serie después del STOP, no un cierre del navegador. Un fallo de navegador necesitaría evidencia propia.

## Lo que sí funcionó y límites de la comparación

En `.runtime/android-pn532-paired-retry-result.json`, Android recibió SELECT y devolvió 9000; recibió GET_CREDENTIAL y devolvió 32 bytes: el nombre `staff-001.keys.demo-access.eth` y 9000. El lector registró el mismo nombre. No existe evidencia de que Android enviara mal esa credencial en esa prueba.

Después hubo aproximadamente 1,104 segundos entre WAITING_CHALLENGE y el envío del challenge por el host. El puente hace una lectura RPC en esa fase. El lector falló aproximadamente 88 ms después, sin respuesta APDU, y Android no registró recepción de SEND_CHALLENGE. «Enviado» por el host no demuestra entrega al servicio HCE. La latencia y el transporte son candidatos de investigación, no causas probadas. Tampoco hay evidencia de rechazo por TTL o firma en ese intento.

Según `.runtime/nfc-port-diagnostic-result.json`, el mismo legacy sin nuevo flasheo inicializó PN532 1.6 a las 10:24:02 y falló en inicialización a las 10:28:06. El error también apareció con el dinámico restaurado. Esto demuestra que no es exclusivo del firmware dinámico, pero no descarta software compartido, reinicios, estado del periférico o alimentación. COM3 se añadió después de A1/A2: comparar todas esas pruebas como un A/B estricto sería incorrecto. No se completó un intercambio legacy de control en esta serie.

## Monitor y control del puerto

El monitor usa `hupcl:false` y `rtscts:false`. En la dependencia local `node_modules/@serialport/bindings-cpp/src/serialport_win.cpp`, esas opciones deshabilitan respectivamente DTR y RTS al abrir. No se ha encontrado evidencia para afirmar que el monitor los activa por defecto. Sí hubo reinicios explícitos mediante COM3, que deben tratarse como una variable del procedimiento. No hay captura eléctrica de las líneas que permita descartarlas por completo.

## Qué aprender de NFC Tools y Android

[Android HCE](https://developer.android.com/develop/connectivity/nfc/hce) documenta un flujo iniciado por el lector: SELECT selecciona el servicio y `processCommandApdu()` recibe comandos. La aplicación responde, no introduce automáticamente una credencial ENS en el lector. El callback corre en el hilo principal y no debe bloquearse. Nuestra implementación separa la solicitud asíncrona de prueba y el sondeo posterior; debe medirse su comportamiento físico, no cambiarla solo por usar un protocolo propio.

[NFC Tools Pro, publicación de Wakdev](https://play.google.com/store/apps/details?id=com.wakdev.nfctools.pro) ofrece emulación de registros NDEF. Puede servir como referencia funcional para indicar cuándo la emulación está preparada y para una prueba independiente de compatibilidad NDEF. No implementa por ello el AID, los comandos de LockENS o su firma de prueba. Sustituir la app por NFC Tools manteniendo nuestros comandos no sería un control equivalente. Una prueba NDEF requeriría un lector compatible con ese protocolo y quedaría fuera del A/B sin cambios solicitado. No se ha instalado ni ejecutado.

## Orden propuesto, sin ejecutar cambios

1. Corregir la clasificación del monitor: inicializando, lector listo, NFC iniciado, error técnico y decisión verificada. Mantener cerrado el acceso ante errores. Conservar etapa, motivo e identificador de sesión.
2. Separar el control de arranque del ensayo NFC. Fijar binario, conexión de ambos USB, alimentación, apertura serie y método de reset; no alternar reset manual y esptool dentro de la comparación. Validar primero PN532_READY repetible usando las mismas condiciones.
3. Si sigue fallando la inicialización, capturar el código I2C real mediante un cambio diagnóstico mínimo y autorizado. Los registros actuales no contienen esa información y no puede reconstruirse retrospectivamente.
4. Solo con arranque estable, comparar SELECT e intercambio legacy con el dinámico. Registrar tiempos por APDU, longitudes y errores internos; conservar la correlación con Android. Investigar la espera RPC antes de SEND_CHALLENGE sin asumir que reducir bytes o aumentar TTL lo resolverá.
5. Mejorar los motivos de disponibilidad Android y comprobar expiración/republicación durante la sesión. El envío correcto de STAFF ya observado es un punto de control, no una validación completa del proceso de firma.

No hay fundamento suficiente para cambiar cables, comprar chips o reemplazar firmware por otro repositorio como solución demostrada. Tampoco para declarar toda la app Android correcta. La auditoría confirma errores de diagnóstico y presentación, y delimita dos fallos físicos distintos cuya causa definitiva todavía no está establecida.
