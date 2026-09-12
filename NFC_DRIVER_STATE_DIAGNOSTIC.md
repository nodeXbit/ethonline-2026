# Captura del estado interno I2C antes de recuperación

## Resultado físico confirmado — 12:24:40 UTC

Binario `8051a772d441f39309e07bbf115b4d001430a9ce08da6f237ddf3e529fc98e9a` instalado con autorización; cuatro hashes verificados. RESET manual confirmado, sin tap.

Secuencia capturada:

| Fase | Estado | Evento | BUSY | SCL | SDA | Retorno |
|---|---|---|---|---|---|---|
| 1: entrada | IDLE (5) | ALIVE (0) | 0 | 0 | 1 | 0 |
| 6: tras envío | TIMEOUT (8) | ALIVE (0) | 0 | 0 | 1 | 0* |
| 4: antes de reset sin limpieza | TIMEOUT (8) | ALIVE (0) | 0 | 0 | 1 | 0 |
| 5: después de reset sin limpieza | TIMEOUT (8) | ALIVE (0) | 0 | 0 | 1 | 0 |

*La fase 6 se registra antes de asignar el error final; ese 0 no significa éxito de la transacción. Después retorna ESP-IDF 259, Wire 4, escritura 49952 us y ACK -2 tras 560945 us.

Interpretación: timeout por la ruta síncrona sin evento DONE/NACK/TIMEOUT registrado; no aparece limpieza de bus (fases 2/3), y el reset posterior devuelve éxito. Por tanto, esta captura descarta la hipótesis de que el 0x103 procediese del fallo de limpieza. SCL ya se muestrea baja a la entrada, SDA alta. Los niveles instantáneos no identifican qué dispositivo conduce SCL ni sustituyen una forma de onda. Falta distinguir condición de línea/configuración del ESP32 y retención por el periférico; no hay fundamento para declarar averiado un chip.

Evidencia: `.runtime/nfc-driver-state-result.json`. Monitor inactivo y puerto cerrado. La estabilidad permanece pendiente; siguiente investigación centrada en SCL baja y ausencia de eventos antes de incrementar timeouts o cambiar la secuencia SAM.

Driver original: ESP-IDF b774170ff46, archivos i2c_master.c e i2c_private.h. Copia y manifest en `.runtime/driver-state-libraries`. Se conserva el original sin instrumentación en `i2c_master.original.txt`; no se modifican archivos de Arduino instalados.

La copia guarda hasta 12 registros en memoria durante la escritura I2C de SAM. Cada uno contiene fase, retorno, estado, evento, bus ocupado y niveles GPIO SCL/SDA. Se desactiva antes del sondeo ACK y se imprime después de ese sondeo. No hay impresión dentro del driver ni cambios deliberados en sus comandos, timeouts o recuperación. La lectura de estado/GPIO y recompilación sí pueden perturbar marginalmente el tiempo; un éxito aislado no demuestra reparación.

Fases: 1 entrada de transacción; 2 antes de reset con limpieza; 3 después de reset con limpieza; 4 antes de reset sin limpieza; 5 después de reset sin limpieza; 6 después de enviar, antes de decidir el retorno final.

Valores de estado según el header instalado: READ=0, READ_ALL=1, WRITE=2, START=3, STOP=4, IDLE=5, ACK_ERROR=6, DONE=7, TIMEOUT=8. Eventos: ALIVE=0, DONE=1, NACK=2, TIMEOUT=3. Los GPIO son muestras puntuales, no una captura eléctrica completa.

Interpretación: fase 3 con error identifica recuperación fallida; fase 6 con ACK_ERROR/NACK identifica rechazo I2C; TIMEOUT identifica la ruta de timeout, que se contrasta con el estado anterior y las líneas. El resultado SAM_IDF y Wire se conservan para correlación. Si no aparece ningún registro interno, el intento no valida esta instrumentación.

Prueba prevista tras autorización: un RESET manual, Seeker alejado, captura abierta antes de pulsar. Detenerse tras el primer fallo. No pedir pulsaciones múltiples; revisar cada resultado antes de continuar. No se ha flasheado este candidato.
