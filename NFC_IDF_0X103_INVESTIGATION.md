# Investigación de 0x103 — 12 septiembre 2026

## Captura adicional de mensajes existentes

Sin flasheo, se amplió el observador para reconocer mensajes i2c.master y se capturó un RESET confirmado a las 12:15:37 UTC. Resultado idéntico: ESP-IDF 259, Wire 4, 49959 us de escritura, ACK -2 tras 560942 us. Cero mensajes i2c.master reconocidos. No se puede deducir de su ausencia qué rama se ejecutó ni que esos mensajes estén efectivamente disponibles en la salida serie de este binario. La prueba no alcanzó el objetivo de distinguir las ramas; repetirla sin cambiar la observabilidad no aportaría ese dato.

Evidencia: `.runtime/nfc-idf-driver-result.json`. Puerto cerrado; sin NFC. Para la siguiente captura hace falta instrumentación explícita de la rama/estado/evento dentro de la versión exacta del driver, guardada antes de su recuperación y publicada después. No basta otro envoltorio del mismo retorno 0x103.

## Identificación local y evidencia física

El archivo instalado `esp32s3-libs/3.3.11/versions.txt` identifica ESP-IDF v5.5.5, commit b774170ff46. El header local confirma versión 5.5.5 y ESP_ERR_INVALID_STATE=0x103. Arduino ESP32 es 3.3.11.

La captura `.runtime/nfc-sam-idf-retry-result.json` contiene error 259, Wire 4, escritura de 49959 us y ACK -2 tras 560942 us. No hubo NFC. La instrumentación conserva el retorno de i2cWrite, pero no el estado interno ni las interrupciones del driver.

## Rutas verificadas en la versión exacta

Fuente: [i2c_master.c, commit b774170ff46](https://github.com/espressif/esp-idf/blob/b774170ff46/components/esp_driver_i2c/i2c_master.c).

En el camino síncrono hay dos explicaciones relevantes: fallo de limpieza del bus, o finalización sin estado DONE. Esta última agrupa NACK y timeout; un timeout puede acabar convertido en INVALID_STATE. La limpieza hardware tiene un límite de 50 ms. El camino de error ya reinicia la máquina de estados, por lo que añadir otro reset sin diagnóstico no constituye una solución demostrada. El origen exacto entre esas rutas requiere registrar estado/evento antes de la recuperación.

## Lo que significa para nuestro montaje

Los aproximadamente 50 ms medidos coinciden tanto con el timeout por defecto de Wire como con el límite de limpieza; esa duración no permite elegir una causa. El header local ESP32-S3 habilita limpieza hardware y fija por defecto 2000 us de espera SCL; el wrapper Arduino deja scl_wait_us=0, que selecciona el valor por defecto. Son parámetros a contrastar, no prueba de que clock stretching sea la causa.

La librería PN532 instalada ignora Wire.endTransmission y continúa sondeando ACK. Por eso el -2 posterior es insuficiente para diagnosticar el envío. Confirmamos pérdida de información en dos capas; no confirmamos aún el origen eléctrico o temporal de la transacción fallida.

## Incidencias externas

[Espressif #14030](https://github.com/espressif/esp-idf/issues/14030) describe fallos persistentes tras NACK y sensibilidad a printf, pero corresponde a IDF 5.2.2 y Wokwi. No acredita el mismo defecto en nuestra versión ni aporta una corrección validada para este montaje. No debe usarse como justificación para cambiar versión o añadir esperas arbitrarias.

## Siguiente medición necesaria

Capturar en una sola ejecución la rama de fallo, estado/evento I2C, condición de bus ocupado y resultado de limpieza, antes de que el driver los reinicie. Conservar también el error original y tiempos. Puede hacerse mediante diagnóstico de la versión exacta del driver o capturando sus mensajes existentes si están habilitados. El observador actual filtra esos mensajes y no los conserva; no pueden reconstruirse de la captura anterior.

No se modificó código, no se compiló ni se flasheó durante esta investigación. El fallo queda acotado a la transacción/recuperación I2C anterior a NFC; la causa final permanece sin determinar y no se declara estabilizado.
