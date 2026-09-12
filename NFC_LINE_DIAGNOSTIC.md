# Prueba aislada de niveles SCL/SDA

## Comparación tras apagado completo: líneas recuperadas

Usuario confirmó desconexión/reconexión de todas las alimentaciones conforme a la instrucción. Sin nuevo flasheo ni cambios de cableado, RESET manual confirmado y captura completa a las 12:42:44 UTC: SCL=1 y SDA=1 en las 30 muestras (10 antes, 10 con I2C y 10 después). Wire.begin/end correctos.

El mismo diagnóstico había observado SCL=0 en las 30 muestras anteriores. El ciclo de alimentación recuperó la condición observada; no es evidencia de un cortocircuito permanente. No identifica por sí solo el componente que retenía SCL ni la operación causante. No se ha validado aún el arranque SAM o NFC después de esta recuperación: sigue instalado el diagnóstico de líneas.

Evidencia preservada: `.runtime/nfc-line-before-powercycle-result.json` y `.runtime/nfc-line-after-powercycle-result.json`. Puerto cerrado al finalizar. Próximo ensayo: restaurar un binario NFC identificado con autorización y comparar primer arranque desde estado recuperado con reinicio en caliente, registrando dónde reaparece SCL baja. Evitar repetir flasheos y RESET sobre un bus ya retenido como si fueran pruebas independientes.

## Resultado físico: SCL baja incluso con GPIO como entrada

Flasheo autorizado del binario `39555491e693035443f564f7ac300744d5773127843cf5ca3a1187f8f44ebffe`, salida 0 y cuatro hashes verificados. RESET manual confirmado; captura completa a las 12:35:38 UTC. Wire.begin y Wire.end devolvieron éxito.

| Etapa | Muestras | SCL baja | SDA alta |
|---|---:|---:|---:|
| Antes de I2C, entradas con pull-up | 10 | 10 | 10 |
| I2C habilitado sin comandos | 10 | 10 | 10 |
| Wire cerrado, entradas con pull-up | 10 | 10 | 10 |

Esta ejecución muestra que el nivel bajo no depende de activar I2C ni de enviar SAM. Sigue presente cuando los GPIO están configurados como entradas. No identifica qué elemento del circuito conectado o del pin provoca la lectura baja, ni demuestra una avería del PN532. Un pull-up interno no constituye una medición de tensión.

Siguiente discriminación sin mover cableado: apagado completo de todas las fuentes (incluidos ambos USB), reconexión en la misma configuración y repetir este diagnóstico existente. Permitirá comprobar si el nivel persiste desde encendido en frío o corresponde a un estado retenido. Si persiste, software y muestras digitales no bastan para localizar el elemento: haría falta medir tensión o aislar eléctricamente con autorización. No se ha solicitado aún ese cambio físico.

Evidencia: `.runtime/nfc-line-result.json`, `.runtime/nfc-line-upload.log`. Captura terminada, puerto cerrado. El diagnóstico de líneas permanece instalado; no ejecuta control de acceso.

Sketch independiente `firmware/pn532_line_diagnostic`, sin PN532 ni comandos I2C/NFC. Mantiene GPIO17/18, buffer 128 y Wire del mismo core Arduino ESP32 3.3.11.

Tres etapas: entrada con pull-up antes de I2C; Wire iniciado sin transmitir; Wire terminado y ambos GPIO configurados otra vez como entradas con pull-up. Cada etapa toma diez muestras a intervalos de 50 ms y las imprime después. Informa el resultado de begin y end. No fuerza nivel alto push-pull ni genera pulsos de recuperación deliberados. Inicializar/desinicializar el periférico puede producir transiciones; esta es una intervención controlada, no observación eléctrica pasiva.

Si SCL es baja antes y después de liberar el ESP32, investigar carga/circuito externo o estado del PN532 sin declarar una avería. Si baja solo al habilitar I2C y sube al terminarlo, investigar configuración/control del ESP32. Si todas las muestras son altas, esta ejecución no reproduce la retención; comparar con el arranque SAM. Los niveles digitales no miden tensión ni identifican pulsos entre muestras.

Al instalar este diagnóstico el ESP32 deja temporalmente de ejecutar el control de acceso. La prueba termina en reposo; no se enviará un tap. El binario funcional y el diagnóstico de driver anterior se preservan. Tras autorización, usar `.runtime/nfc-line-capture.mjs`, esperar CAPTURE_READY y solicitar un solo RESET manual. El resultado se guarda en `.runtime/nfc-line-result.json`.

No se ha flasheado este candidato.
