# Diagnóstico de escritura SAM y ACK

## Resultado físico: escritura I2C fallida confirmada

Flasheado con autorización el binario `a85ef5f22e9cd2fc6cef6a9f8c07fd258efbf039f0067e5dd1e96a43e9660673`, salida 0 y cuatro hashes verificados. RESET manual confirmado, Seeker alejado.

A las 11:54:21 UTC se capturó WIRE_RC=4, WIRE_US=49956, ACK_RC=-2, ACK_US=560946. La escritura falló antes de esperar el ACK. La librería ignoró ese retorno y continuó el sondeo; no se puede concluir que SAM llegara al PN532. Wire 4 representa otros errores: los aproximadamente 50 ms no bastan para reclasificarlo como Wire 5. La duración real de ACK tampoco equivale al contador de 10 ms de la librería, porque las llamadas I2C consumen tiempo adicional.

No se alcanzaron versión, RF, READY ni NFC. Puerto cerrado tras error; monitor inactivo. Evidencia consolidada `.runtime/nfc-sam-wire-result.json`, carga `.runtime/nfc-sam-wire-upload.log`.

Siguiente diagnóstico: conservar el error ESP-IDF que Wire reduce a 4 y comprobar inicialización/estado del bus. No ampliar el timeout ACK como solución a esta captura: el envío anterior ya falló. La causa de bajo nivel sigue sin determinarse y el arranque no está estabilizado.

Se prepara una copia aislada de la librería instalada en `.runtime/sam-wire-trace-library/PN532_I2C`, seleccionada explícitamente con `arduino-cli compile --library`. La librería instalada no se modifica; hashes de ambas copias en `.runtime/sam-wire-trace-library/manifest.json`.

La copia conserva el resultado de Wire.endTransmission dentro de writeCommand, mide su duración y la de readAckFrame y emite PN532_SAM_WIRE_V1 solo para SAM (0x14). No vuelve a escribir ni cambia el ACK, sus argumentos o límites. El retorno original de writeCommand se conserva incluso cuando ignora un fallo Wire: este candidato diagnostica, todavía no corrige el transporte.

El registro serie se emite después de terminar la espera ACK y antes de leer la respuesta SAM. Añade una pequeña demora en ese punto; no es instrumentación de impacto temporal cero. No imprime comandos completos, credenciales, firmas o secretos. El observador guarda únicamente campos numéricos acotados.

Interpretación prevista:

- WIRE_RC distinto de 0: la escritura I2C ha fallado; ACK_RC por sí solo no permite atribuir el problema al PN532.
- WIRE_RC=0 y ACK_RC=-2: Wire dio por completada la escritura, pero no se obtuvo el ACK dentro del sondeo existente. No prueba por sí solo que baste con aumentar el timeout.
- WIRE_RC=0 y ACK_RC=0: envío y ACK correctos; si falla después, examinar READ_RC del registro PN532_BOOT_V1.

No hay flasheo implícito. Tras autorización se capturará un RESET manual con el Seeker alejado y se detendrá ante fallo, sin pedir varias pulsaciones mientras una sesión pueda cerrarse. Se preserva el firmware SAM anterior para reversión.
