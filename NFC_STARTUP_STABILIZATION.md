# Estabilización de arranque PN532 — candidato pendiente de prueba física

## Resultado posterior del candidato: NO estabilizado

Usuario autorizó flasheo. Binario `fd21250dc6c26584f158a012c57b88b18073a123ea8d0e00eed487d93c41d02d` instalado en COM4 con salida 0 y cuatro hashes verificados. El primer RESET manual confirmado produjo WIRE_RC=5 a las 11:39:50.868 UTC, sin versión ni READY. Serie detenida según el criterio previsto; no se realizaron los otros nueve arranques ni hubo tap. El código 5 no se reintenta deliberadamente. El resultado no ejercita ni valida la recuperación del código 2.

Evidencia: `.runtime/nfc-startup-retry-upload.log`, `.runtime/nfc-startup-series-events.jsonl`, `.runtime/nfc-startup-series-result.json`. Captura serie terminada y puerto cerrado. El candidato permanece instalado; no se ha efectuado una reversión sin autorización.

Siguiente investigación: validez de la sonda de dirección como condición obligatoria antes de hablar con PN532 y comportamiento de despertar/clock stretching. No basta con aumentar el número de reintentos de NACK para tratar este timeout.

## Evidencia y fundamento

El binario anterior completó un intercambio NFC con STAFF, incluyendo SEND_CHALLENGE de 109 bytes y firma verificada. También registró un arranque WIRE_RC=2 seguido de otro WIRE_RC=0 tras RESET manual. No se ha establecido la causa física de esa intermitencia.

[NXP UM0701-02, sección 6.2.4, página impresa 42](https://www.nxp.com/docs/en/user-guide/141520.pdf) advierte de que la dirección I2C puede no ser reconocida inmediatamente después de un intercambio. Nuestra comprobación previa abortaba en el primer fallo. La librería instalada PN532_I2C inicia Wire y espera 500 ms; no implementa recuperación de esa comprobación.

## Cambio acotado

Solo la comprobación de dirección del arranque admite ahora hasta 10 intentos, separados por 50 ms, exclusivamente cuando Wire devuelve 2. Cada resultado se registra con PN532_INIT_V1, incluido el primero. Los otros errores y timeouts abortan inmediatamente; agotar intentos sigue cerrando el arranque. La espera añadida máxima es 450 ms, más el tiempo de las llamadas I2C.

Estos límites son una decisión conservadora de implementación, no tiempos prescritos por NXP. El código 2 es la categoría que expone Wire y no identifica por sí solo la causa exacta. No se añade reset de bus, cambio de reloj, de cableado o de alimentación. No se reintentan comandos NFC, firmas ni decisiones. La versión del PN532, SAM y configuración RF siguen siendo obligatorias antes de READY.

## Validación de software

Compilación ESP32-S3 aprobada: 306213 bytes de programa y 23152 bytes globales. 55 tests Node aprobados, incluidos registros de recuperación parcial y agotamiento de intentos que nunca producen READY ni un challenge prematuro. Estos tests validan el puente; no sustituyen la ejecución física del bucle C++.

Binario y hash: `.runtime/nfc-startup-retry-build.json`. Reversión preservada: `.runtime/firmware-dynamic-init-audit/pn532_dynamic_access.ino.bin`, SHA-256 `13528830db96b27c84dc14d7e8ccc6a46822ada30a959aa7f70d6071471e5bb0`.

## Prueba física propuesta tras autorización de flasheo

Mantener ambos USB conectados, cableado y alimentación. Seeker alejado. Registrar separadamente el reset automático del flasheo. Capturar 10 arranques por RESET manual: cinco manteniendo el puerto abierto y cinco abriendo/cerrando el observador entre arranques, sin esptool para reiniciarlos. Registrar todos los códigos por arranque y exigir versión/configuración/READY en cada uno. No mezclar taps con esta serie.

Si aparece 2 seguido de 0 dentro de un arranque, hay evidencia directa de recuperación. Si todos los primeros intentos son 0, la serie prueba arranques correctos pero no ejercita la recuperación. Si se agotan los intentos o falla otra etapa, detener la serie y analizar esa captura; no aumentar límites ciegamente. Solo después repetir un intercambio STAFF completo para descartar una regresión observable. Diez arranques correctos son una comprobación práctica, no una garantía estadística de fiabilidad.

No se ha flasheado este candidato ni se declara estabilizado el hardware.
