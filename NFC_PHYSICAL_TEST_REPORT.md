# Informe de la primera prueba física Dynamic NFC

Fecha: 12 de septiembre de 2026.

Alcance: desde la tarea **TASK — PREPARE AND ORCHESTRATE FIRST CONTROLLED DYNAMIC NFC PHYSICAL TEST** hasta la instrucción de conservar los cables y el modo I2C. Todas las horas de la cronología son UTC; en Madrid corresponden a dos horas más.

## 1. Resultado

**Objetivo físico no alcanzado. Causa raíz sin determinar.**

Se completó el preflight, se cargaron tres versiones de firmware con autorización explícita y verificación de escritura, y se documentaron ocho intentos. Hubo además una sesión del monitor sin actividad NFC que agotó su espera.

En varios intentos se leyó correctamente `staff-001.keys.demo-access.eth` y Node emitió un desafío vinculado a Lab. No se recibió ni verificó una prueba del holder. Por tanto, no se llegó a la denegación física esperada por `RESOURCE_POLICY_MISSING`.

Los dos últimos intentos fallaron antes: el PN532 no respondió a la comprobación inicial de dirección I2C `0x24`. No se ha demostrado que este fallo y el fallo anterior de SEND_CHALLENGE tengan la misma causa.

## 2. Encargo y restricciones

El encargo partía de una implementación Dynamic NFC + Virtual Gates terminada en software, pero sin validación física. Debía preparar y coordinar un primer ensayo con:

| Campo | Valor exigido |
|---|---|
| Wallet activa | `0x3419148731087b970d2059C53780163B452D5FF7` |
| Pase | `staff-001.keys.demo-access.eth` |
| Puerta virtual | Lab |
| `access.v1` | Allowed |
| `resources.v1` | Ausente |
| Resultado esperado | `ACCESS DENIED / RESOURCE_POLICY_MISSING`, después de verificar una prueba válida |

Las restricciones iniciales fueron: no escribir en blockchain, no añadir recursos, no modificar código antes de registrar un fallo físico, no flashear automáticamente, proporcionar rollback, ocultar secretos y detenerse ante identidad/recurso incorrectos, fallback silencioso, desincronización o elusión de controles.

La instrucción inicial pedía no ejecutar las cargas; posteriormente el usuario autorizó expresamente cada una de las tres realizadas. No se ejecutó el rollback propuesto después ni se migró a UART/HSU. La última restricción del usuario es **mantener cableado, pines y modo I2C**.

## 3. Preflight completado

El registro previo al primer flash se guardó a las 08:12:15 UTC en [.runtime/dynamic-first-preflight.json](.runtime/dynamic-first-preflight.json).

- Baseline: `20c2dd9866f8c48bc0611dee34b9b272906a3429`. La implementación NFC/gates ya estaba sin commit al comenzar esta tarea; no debe atribuirse toda esa implementación al diagnóstico posterior.
- Seeker detectado por ADB; NFC habilitado; wallet y STAFF correctos. Se observó `Ready to tap` después del refresco de solo lectura.
- Consulta onchain a las 08:09:47 UTC, bloque Sepolia `11687656`: propietario esperado, registro válido, procedencia válida, acceso Allowed y política de recursos ausente.
- COM4 disponible; identificación ROM de ESP32-S3 revisión v0.2 verificada; puerto liberado después.
- Artefactos dinámico y legacy disponibles y con hashes registrados; comando exacto de rollback preparado antes de cargar firmware.
- Monitor local en `http://127.0.0.1:8791`: comprobaciones HTTP, selectores y fijación del recurso para la sesión aprobadas; Lab seleccionado.
- **Límite de validación:** no había superficie de navegador disponible para inspección visual renderizada. Las comprobaciones HTTP no equivalen a esa revisión visual.

El estado onchain citado corresponde al bloque registrado. No se ha realizado una nueva lectura onchain para redactar este informe.

## 4. Cronología de intentos

| Registro | Hora UTC | Firmware | Hecho observado | Resultado / límite |
|---|---|---|---|---|
| 1 | Evidencia guardada 08:17:31 | Dinámico inicial | Fallo antes de identificar la credencial. El usuario confirmó RESET y un tap. | `TRANSPORT_FAILURE`; la captura inicial no identifica la etapa inferior exacta. |
| 2 | Desafío 08:23:36 | Dinámico inicial | Arranque, SELECT, STAFF y preflight correctos. Node envió desafío de Lab. | SEND_CHALLENGE terminó con `expected exact 9000 response`; el SW real no quedó capturado. |
| 3 | Fallo 08:36:23 | Diagnóstico APDU | PN532 1.6, arranque y SELECT correctos. | GET_CREDENTIAL falló; monitor `NO_SELECTED_CREDENTIAL`. No alcanzó SEND_CHALLENGE. |
| Sesión adicional | 08:36:42–08:38:42 | Diagnóstico APDU | Nueva sesión iniciada por el usuario, sin actividad NFC registrada. | Agotó la espera de 120 segundos. No es evidencia de intercambio físico. |
| 4 | Fallo 08:41:41 | Diagnóstico APDU | STAFF identificado y desafío Lab emitido. | `EXCHANGE=FAIL; LEN=0; SW=NONE`; fallo ISO-DEP. |
| 5 | 08:51:12–08:51:14 | Diagnóstico APDU + trazas Android | Android recibió SELECT y GET_CREDENTIAL; respondió STAFF y `9000`. | Android no registró recepción de SEND_CHALLENGE; lector informó fallo ISO-DEP. |
| 6 | 08:55:36–08:55:38 | Misma combinación | Se repitió la captura anterior con STAFF correcto en ambos extremos. | SEND_CHALLENGE volvió a fallar sin llegar al servicio HCE de la aplicación. |
| 7 | Fallo 09:09:27 | Diagnóstico interno PN532 | Comprobación inicial de dirección I2C. | `PN532 did not ACK at I2C address 0x24`; cero eventos HCE. |
| 8 | Fallo 09:14:53 | Diagnóstico interno PN532 | Repetición después de desconectar y reconectar alimentación, confirmado por el usuario. | Mismo fallo de inicialización; cero eventos HCE. |

Después del intento 8, el usuario confirmó que los cables y el selector parecían firmes y sin cambios. Es una comprobación visual del usuario, no una medición eléctrica.

La denegación mostrada por el monitor en estos fallos **no valida** la denegación de política requerida: faltó completar el transporte y verificar la prueba del holder. No se observó ALLOW ni sustitución por `guest-001`.

## 5. Cambios realizados durante el diagnóstico

Se registraron fallos antes de introducir las siguientes modificaciones:

| Componente | Cambio | Propósito y validación |
|---|---|---|
| Observador Node local | Captura acotada de etapas, credencial, recurso, fallo APDU y eventos del monitor. | Distinguir etapas que el mensaje genérico `TRANSPORT_FAILURE` no mostraba; firmas y nonces redactados. |
| Firmware, primer diagnóstico | Línea de resultado de SEND_CHALLENGE: éxito/fallo, longitud y SW cuando existe una respuesta válida. | Evitar inferir un rechazo Android sin haber capturado el SW. Compilación aprobada. |
| Android debug | Metadatos RX/TX, longitud, cabecera, estado, credencial y desactivación; sin cuerpos de APDU, nonces ni firmas. | Saber si SEND_CHALLENGE llega al servicio HCE. APK compilado e instalado; 246 tests aprobados. |
| Tests Android | Dos pruebas adicionales del diagnóstico y su vinculación/redacción. | Total aumentó de 244 a 246; cero fallos, errores o tests omitidos en los informes revisados. |
| Firmware, diagnóstico interno | Observación de llamadas HAL de escritura/ACK y lectura, capacidad y estado PN532; impresión después del intercambio. | Identificar el error oculto por `inDataExchange=false`. Compila, pero los últimos intentos fallan antes de ejecutar ese diagnóstico. |
| Documentación | WORKLOG, STATUS, README y runbook actualizados; evidencia local conservada. | Mantener historial, límites y restricciones entre conversaciones. |

Archivos principales de código: `firmware/pn532_secure_access/pn532_secure_access.ino`, `HceApduProcessor.kt`, `GateC1HostApduService.kt`, `DynamicHceTest.kt` y `.runtime/dynamic-retry-observer.mjs`.

No se modificaron el desafío de 104 bytes, las APDU, el TTL, la vinculación wallet/credencial, las reglas de recursos ni los controles de replay como solución a estos fallos. No se modificaron las librerías Arduino instaladas globalmente. La compilación legacy también se comprobó.

Última validación de software registrada: **Node 224/224; Android 246/246; compilaciones dinámica y legacy aprobadas; revisión de espacios del diff aprobada**. Estas pruebas no certifican estabilidad del hardware.

## 6. Firmware y APK

Las tres cargas ESP32 terminaron con código de salida 0 y verificación de los hashes escritos. Los hashes de los artefactos se volvieron a comprobar al redactar el informe.

| Artefacto | Tamaño binario | Estado |
|---|---:|---|
| `.runtime/firmware-dynamic/pn532_dynamic_access.ino.bin` | 305584 bytes | Primera carga autorizada; completada 08:16:02 UTC. |
| `.runtime/firmware-dynamic-apdu/pn532_dynamic_access.ino.bin` | 305776 bytes | Segunda carga autorizada; usada en intentos 3–6. |
| `.runtime/firmware-dynamic-pn532-trace/pn532_dynamic_access.ino.bin` | 306272 bytes | Tercera carga autorizada; registrada 09:07:54 UTC; última instalada. |
| `.runtime/firmware-legacy/pn532_secure_access.ino.bin` | 305184 bytes | Rollback preparado; no ejecutado durante esta tarea. |

SHA-256, en el mismo orden:

```text
dynamic: e14e61d27ca7725d0e8a0912af8ccb3168d6fca3650b11d69c22aec2cfb7f513
apdu:    d3317cd06edd7b6b76d227602a65e54f10eeed6fff3ddfeb9725a980633fdf28
trace:   ce1bbe0020eb9a667c983c34d46d0f1d33c321250062875a7141823ad04c06b6
legacy:  4695b8b862d2001603d754495e16e26872558d1aa5a2fb98ae79f1bd2c7e4d2e
```

APK al preflight: `07c742314ed40354f6d6aa2783d5fb86426368755d2321a866b0bc828319719a`.

Último APK diagnóstico instalado: `45004d6ecd239cb1ac317dc9f684cfdc4bb3182e06ac527f67c11b11b00ce909`.

Comando de rollback preparado, **no ejecutado**:

```powershell
arduino-cli upload --fqbn esp32:esp32:esp32s3 --port COM4 --input-dir .runtime/firmware-legacy firmware/pn532_secure_access
```

## 7. Hipótesis y resultados del análisis

### Credencial ENS incorrecta en Android

Las capturas pareadas 5 y 6 muestran `staff-001.keys.demo-access.eth` correctamente seleccionado y devuelto por HCE con `9000`; el lector registra el mismo nombre. Por tanto, esas capturas no respaldan un error de nombre ENS como explicación del fallo de SEND_CHALLENGE. Esto no transforma el fallo anterior de GET_CREDENTIAL en un éxito: son intentos distintos.

### Reloj y TTL

Se midió que el teléfono estaba entre 742 y 865 ms por detrás del host. Se consideró un posible desfase en la ventana de validez, pero no se capturó un rechazo Android por TTL. En las dos capturas pareadas, SEND_CHALLENGE ni siquiera llegó al servicio HCE. El TTL no se cambió.

### Tamaños y antecedentes de otras conversaciones

No se recuperó memoria de conversaciones no disponibles: se consultó el historial del repositorio. Allí constan intercambios físicos anteriores, fuera de esta tarea, con desafío completo de 104 bytes y respuesta de 67 bytes.

El ajuste documentado es reservar un byte adicional para el estado PN532 antes de devolver los bytes de respuesta APDU. No se encontró evidencia en el historial revisado de reducir el desafío.

Los tamaños actuales siguen siendo: cuerpo 104, APDU 109, trama I2C de envío 119, buffer Wire configurado 128 bytes. El fallo reciente ocurre en una comprobación de dirección anterior a esa trama; no demuestra un problema de tamaño del desafío.

### Cambio de librerías o compilación

Se verificó que el binario de la caché de compilación coincide con el último artefacto flasheado. Comparado con el primer build dinámico de esta tarea, son idénticos los objetos compilados de PN532, PN532_I2C y Wire, así como `sdkconfig`, opciones de compilación y `build_opt.h`. El arranque hasta la comprobación de dirección coincide con el código Gate E registrado en git.

**Límite:** no es una comparación binaria con todos los firmwares históricos que funcionaron. Tampoco excluye una regresión de aplicación introducida por el nuevo diagnóstico. La aparición del fallo de arranque después de esa carga es una correlación pendiente de aislar.

### Búsqueda de alternativas

Se revisaron fuentes de [Elechouse](https://github.com/elechouse/PN532), [Seeed](https://github.com/Seeed-Studio/PN532), [Adafruit](https://github.com/adafruit/Adafruit-PN532) y [jef-sure/ESP-IDF](https://github.com/jef-sure/esp32-component-pn532). No se encontró validación de toda nuestra combinación. El límite de 62 bytes en `inDataExchange` del código Adafruit revisado impide una sustitución directa para nuestra APDU de 109 bytes.

Se propuso evaluar Seeed HSU. El usuario rechazó cambiar cables; esa vía no se implementó y queda descartada para el alcance actual. Los detalles y enlaces de código están en [.runtime/pn532-upstream-research.md](.runtime/pn532-upstream-research.md).

## 8. Límites y correcciones del proceso

- Reinstalar el firmware anterior habría sido una comparación diagnóstica, no una solución demostrada; tampoco era estable en todas las pruebas.
- La recomendación de cambiar a UART/HSU fue prematura respecto a la necesidad de explicar la regresión manteniendo el montaje existente.
- Ni el desfase de reloj ni el tamaño de los bytes se establecieron como causa raíz. No deben presentarse como soluciones encontradas.
- Los diagnósticos mejoraron la evidencia, pero todavía no produjeron el código interno PN532 del fallo original: el último firmware se detuvo antes de ese intercambio.
- No hay evidencia suficiente para atribuir el fallo actual exclusivamente a hardware, cableado, Android o firmware.

## 9. Estado al entregar este informe

- Monitor comprobado: accesible en `http://127.0.0.1:8791`, Lab seleccionado, sin intento activo, último resultado `ACCESS DENIED / TRANSPORT_FAILURE`, controlador `Not confirmed`.
- Último firmware instalado: diagnóstico interno PN532, hash `ce1bbe...` completo en la sección 6.
- Cableado y modo I2C conservados. No rollback ni nueva carga ejecutados después de los dos fallos de arranque.
- Cero escrituras blockchain y cero cambios en `resources.v1` durante esta tarea. Ninguna prueba del holder recibida/verificada en los intentos registrados.
- HEAD y referencia local `origin/main` siguen en `20c2dd9866f8c48bc0611dee34b9b272906a3429`; no hubo nuevos commits ni push de este trabajo. La comprobación de `origin/main` es local, no un nuevo fetch remoto.
- No se ha alcanzado ni certificado un firmware dinámico físicamente fiable.

El trabajo pendiente es aislar el fallo de arranque y el fallo de SEND_CHALLENGE conservando I2C, y completar después el recorrido STAFF → Lab → prueba válida → `RESOURCE_POLICY_MISSING`, con evidencia de cada etapa.

## 10. Evidencia local

Los archivos bajo `.runtime` son evidencia local ignorada por git; conservarlos junto al informe para un traspaso completo.

| Evidencia | Archivo |
|---|---|
| Preflight | [dynamic-first-preflight.json](.runtime/dynamic-first-preflight.json) |
| Primera carga | [dynamic-first-flash.json](.runtime/dynamic-first-flash.json) |
| Intentos 1–3 | [primero](.runtime/dynamic-first-tap-failure.json), [segundo](.runtime/dynamic-second-tap-result.json), [tercero](.runtime/dynamic-third-tap-result.json) |
| Fallo APDU con SW ausente | [dynamic-apdu-status-result.json](.runtime/dynamic-apdu-status-result.json) |
| Capturas pareadas | [primera](.runtime/android-pn532-paired-result.json), [repetición](.runtime/android-pn532-paired-retry-result.json) |
| Fallos de arranque | [primero](.runtime/pn532-internal-initialization-result.json), [tras reconectar](.runtime/pn532-powercycle-result.json) |
| Última carga | [pn532-internal-diagnostic-handoff.json](.runtime/pn532-internal-diagnostic-handoff.json) |
| Comparación de builds | [pn532-build-comparison.json](.runtime/pn532-build-comparison.json) |
| Cronología del observador | [dynamic-apdu-status-events.jsonl](.runtime/dynamic-apdu-status-events.jsonl) |
| Historial mantenido | [WORKLOG.md](WORKLOG.md) |
