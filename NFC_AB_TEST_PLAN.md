# Prueba A/B controlada: legacy frente a dinámico

Estado: preparada, sin ejecutar. Fecha: 2026-09-12.

Instrucción vigente: no modificar código, no recompilar, no cambiar hardware, cableado, modo I2C, teléfono ni alimentación. No flashear hasta autorización explícita del usuario. Este documento no autoriza ninguna carga.

## Objetivo

Comprobar primero con el firmware legacy ya construido:

`PN532 init -> SELECT -> SEND_CHALLENGE -> STATUS -> GET_SIGNATURE`

El resultado se refiere al transporte de una APDU de 109 bytes (104 de cuerpo) y una respuesta de firma de 67 bytes (65 + 9000). No se evalúa política ENS ni acceso a Lab. No habrá escrituras blockchain ni decisiones ALLOW.

## Condiciones fijadas

| Variable | Condición |
|---|---|
| Controlador | Mismo ESP32-S3 revisión v0.2, COM4 |
| Lector | Mismo PN532 y mismo selector I2C |
| Conexiones | SDA GPIO17, SCL GPIO18, misma alimentación y GND; no mover cables |
| Alimentación | Mantener fuente, cable USB, puerto y conexiones actuales; no introducir un ciclo de alimentación como tratamiento entre A/B |
| Arranque | Tras cada carga, abrir captura serie; un RESET manual con teléfono alejado; no resets automáticos adicionales |
| Teléfono | Mismo Seeker, ADB <GATE_SERIAL_2>; desbloqueado, NFC habilitado |
| APK | El instalado, sin reinstalar: SHA-256 45004d6ecd239cb1ac317dc9f684cfdc4bb3182e06ac527f67c11b11b00ce909; verificado leyendo su base.apk |
| Identidad esperada | 0x3419148731087b970d2059C53780163B452D5FF7 |
| Pase y recurso del desafío | STAFF y Lab, con los mismos bytes32 para A y B |
| APDU | AID F0454E5356324331; desafío 104; orden 109; respuesta de firma 67 |
| Tiempos | Mismos límites existentes: TTL 60 s, polling firmware 250 ms, espera de firma firmware 20 s; ninguna ampliación |
| Presentación | Una presentación por intento, mismo punto/orientación; teléfono inmóvil; no pulsar Start attempt |
| Procesos | Un único propietario de COM4; Gate Monitor y bridges de política no intervienen en el intercambio aislado |

Las diferencias inevitables entre intentos son el nonce, la caducidad y la firma. Se generan de nuevo; nunca se reutiliza una prueba anterior. Registrar también duración entre GET_CREDENTIAL/WAITING_CHALLENGE y envío del desafío para no confundir diferencias de tiempo con diferencias de firmware.

## Artefactos fijados

### A: legacy ya construido

Ruta: `.runtime/firmware-legacy/pn532_secure_access.ino.bin`.

SHA-256: `4695b8b862d2001603d754495e16e26872558d1aa5a2fb98ae79f1bd2c7e4d2e`.

Tamaño: 305184 bytes. Target: `esp32:esp32:esp32s3`; core registrado: 3.3.11.

Comando exacto, pendiente de autorización:

```powershell
arduino-cli upload --fqbn esp32:esp32:esp32s3 --port COM4 --input-dir .runtime/firmware-legacy firmware/pn532_secure_access
```

### B: dinámico original ya construido, sin los diagnósticos posteriores

Ruta: `.runtime/firmware-dynamic/pn532_dynamic_access.ino.bin`.

SHA-256: `e14e61d27ca7725d0e8a0912af8ccb3168d6fca3650b11d69c22aec2cfb7f513`.

Tamaño: 305584 bytes. Mismo target/core. Se elige el dinámico original para no mezclar la incorporación del flujo dinámico con el posterior diagnóstico interno.

Comando reservado para una autorización posterior; no ejecutar después de A automáticamente:

```powershell
arduino-cli upload --fqbn esp32:esp32:esp32s3 --port COM4 --input-dir .runtime/firmware-dynamic firmware/pn532_dynamic_access
```

A y B comparten exactamente los binarios de bootloader y particiones verificados:

- Bootloader: `5f95cbc8b40b7a373f9cb85b534cb7a570cdad30ca27c7021775d830ca03276b`.
- Particiones: `148b959cbff1c38aa8e1d5c0ba9d612c54997b945e56a63f41223eef650653a1`.

El firmware actualmente instalado es el diagnóstico interno `ce1bbe0020eb9a667c983c34d46d0f1d33c321250062875a7141823ad04c06b6`. Sus dos fallos previos se conservan como antecedentes; no sustituyen una ejecución B bajo este plan.

## Condición de compatibilidad Android y límite de inferencia

El legacy no envía GET_CREDENTIAL. En el APK actual, el procesador dinámico exige ese comando antes de SEND_CHALLENGE y, si falta, rechaza el desafío. Dejarlo en modo dinámico convertiría la prueba legacy completa en una prueba de incompatibilidad conocida.

Para A se debe usar la opción ya existente `Settings -> Developer diagnostics -> Legacy NFC regression mode (first embedded wallet)`. No requiere modificar ni reinstalar el APK. Esta opción se activa solamente al ejecutar A y queda registrada. Comprobar antes del intercambio que el signer legacy corresponde al holder; este modo usa la primera wallet embebida y no basta con suponer que coincide con la wallet activa. Si no puede verificarse esa identidad, detenerse antes de emitir el desafío.

Para B se usa el modo dinámico con STAFF seleccionado y estado fresco. No se cambia de modo dentro de una sesión NFC.

**Límite explícito:** el arranque del PN532 sucede sin intervención del teléfono y permite comparar los artefactos sin ese factor. El intercambio completo compara dos flujos compatibles con modos HCE diferentes; un resultado distinto no permite culpar exclusivamente al firmware ESP32. La clasificación correcta será transporte común, flujo dinámico o resultado inconcluso, según las capturas. No hay un A/B de firmware puro de extremo a extremo con estos dos protocolos y el APK actual sin cambiar código.

## Ejecución A después de la autorización

1. Verificar otra vez el hash legacy, COM4 y que no existe un intento activo. Cerrar el monitor/observador que pudiera competir por el puerto. No ejecutar `demo:bridge` ni el Gate Monitor como verificador: incorporan política y podrían emitir ALLOW.
2. Ejecutar exactamente la carga A. Guardar salida de esptool, resultado y hashes verificados. Si falla, detenerse; no recompilar ni cambiar parámetros.
3. Preparar la opción legacy del APK e identidad del signer. Mantener la app en primer plano, el mismo montaje y la alimentación conectada.
4. Abrir una captura serie a 115200 antes del RESET. Capturar en paralelo únicamente la etiqueta `LockENS-HCE` de Android. El operador serie interactúa con el protocolo existente; no se crean ni modifican fuentes de firmware, APK o bridge.
5. Pedir un RESET manual con el teléfono alejado. Exigir `PN532_FIRMWARE=1.6`, `GATE_E_READY` y `PRESENT_SEEKER` de ese arranque. La versión implica respuesta a la consulta; cualquier STOP de INITIALIZATION detiene la prueba antes del tap.
6. Tras readiness, una presentación del teléfono. Exigir `TARGET_ACTIVATION: PASS`, `SELECT: PASS` y RX/TX Android compatibles con SELECT/9000.
7. En `WAITING_CHALLENGE`, enviar una sola línea `CHALLENGE=0x...` generada en ese momento, con el formato existente de 104 bytes. Usar `issueAccessChallenge` y `serializeChallenge` del repositorio como utilidades en la consola de operación, sin lanzar su CLI de política ni escribir un nuevo programa. No usar una fixture caducada ni registrar nonce/cuerpo.
8. Exigir `SEND_CHALLENGE: PASS`, progreso de STATUS a READY y `GET_SIGNATURE: PASS`. Capturar tamaños/SW en Android y comprobar que la línea PROOF contiene exactamente 65 bytes; mantener sus bytes en memoria, no en el log.
9. Recuperar localmente el signer usando el esquema EIP-712 existente y comprobar holder/caducidad/uso único. Esa comprobación distingue transporte completo de prueba inválida; no consulta ENS ni concede acceso. La firma real del desafío forma parte del intercambio legacy solicitado; no es una transacción blockchain.
10. Cerrar el protocolo con `AUTHORIZATION=DENY`, solamente después del intercambio, y registrar su confirmación. Es una terminación técnica del ensayo, no una decisión de política de recursos. Nunca enviar ALLOW. Ante un STOP anterior, registrar el fallo y cerrar COM4 sin continuar.
11. Guardar el resultado A, desactivar el modo legacy con el teléfono alejado y detenerse. B queda pendiente de su propia autorización. No repetir A automáticamente para buscar un éxito.

El operador de consola conservará los límites y parsers existentes; no imprimirá serie cruda, typed data, prueba ni nonce. No hay un CLI existente específico de transporte que pueda ejecutarse sin política: por eso no se prescribe como sustituto el bridge de producción. La secuencia de consola se ejecutará solo tras la autorización, sin cambios de código del proyecto.

## Evidencia obligatoria

Guardar un registro nuevo por intento, sin sobrescribir los anteriores:

- Identificador A1/B1, fecha UTC y hora local, hashes de firmware/APK y modo HCE.
- Confirmación de que fuente de alimentación, USB, cableado, selector y teléfono son los mismos.
- Salida de carga verificada; instante de apertura COM4 y RESET confirmado.
- Init, versión PN532, SELECT, SEND_CHALLENGE, STATUS, GET_SIGNATURE, longitudes y SW que realmente estén disponibles.
- Latencias entre etapas; conteo de comandos, sin sus cuerpos.
- Recepción de 65 bytes de prueba y resultado de comprobación local del signer; contenido redactado.
- STOP exacto permitido, último paso alcanzado, cierre del puerto y resultado por etapa.

Estados separados: `INIT`, `SELECT`, `LEGACY_EXCHANGE`/`DYNAMIC_EXCHANGE`, `PROOF_CHECK`, `SESSION_CLOSE`; cada uno PASS/FAIL/NOT_REACHED. Un fallo de firma o un 6985 devuelto por Android no se etiqueta automáticamente como fallo I2C.

## Matriz de decisión

| Observación | Decisión permitida |
|---|---|
| A falla en INIT, igual que los dos antecedentes dinámicos | El fallo también aparece con legacy: no es exclusivo del flujo dinámico. Investigar la capa común de arranque/I2C; no atribuirlo automáticamente al cableado o al chip. |
| A pasa INIT pero falla SELECT/intercambio | La inicialización funciona. Comparar la etapa y RX/TX Android; un error HCE/firmante/configuración puede hacer el resultado inconcluso. |
| A completa el intercambio | El transporte legacy funciona en esa ejecución. A solo no demuestra que el dinámico sea culpable ni que el sistema sea estable. Procede B autorizado. |
| A pasa y B falla INIT | Diferencia asociada a los artefactos/arranque o intermitencia; el teléfono no intervino. No confundir con ENS ni con el tamaño de APDU. |
| A pasa y B falla después de SELECT | Problema localizado al flujo dinámico bajo esas condiciones; distinguir GET_CREDENTIAL, tiempos y rechazo HCE de fallo de entrega. No afirmar causa exclusiva del firmware sin evidencia adicional. |
| A y B fallan con el mismo error de transporte | Evidencia a favor de un problema común; conservar todas las ejecuciones, no seleccionar solo las favorables. |
| A y B pasan | Fallo no reproducido en el ensayo aislado; revisar diferencias con el recorrido de producción. No declarar una reparación. |
| Resultados variables o condiciones no iguales | Inconcluso. Cualquier repetición se identifica y autoriza como nuevo intento; no alterar parámetros. |

Si hace falta confirmar causalidad tras A/B, se propone posteriormente una vuelta A2, con autorización separada y las mismas condiciones. Una única pareja no elimina una avería intermitente.

## Verificaciones de preparación

Se leyeron el sketch generado de la compilación legacy, el firmware actual, la selección de procesador HCE y las utilidades serie/criptográficas existentes. Se comprobaron hashes de A/B, bootloader, particiones y APK instalado; COM4 existe y el monitor estaba sin intento activo. No se abrió una sesión NFC, no se cambiaron ajustes Android, no se modificó código, no se recompiló ni se flasheó durante esta preparación.

**Pendiente: autorización del usuario para la carga y ejecución A.**
