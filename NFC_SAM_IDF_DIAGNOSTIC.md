# Diagnóstico del error ESP-IDF de la escritura SAM

## Resultado físico confirmado

Binario `48de1d60bf67e9a8e6cc831797ad78adc9200f7bdef15ad141a6174286c2ab4c` flasheado con autorización, salida 0 y cuatro hashes verificados. Primera ventana expiró sin RESET confirmado. Ventana reabierta a las 12:02:55 UTC; RESET confirmado y arranque capturado a las 12:04:27 UTC.

Resultado: ESP-IDF 259 (0x103, ESP_ERR_INVALID_STATE), Wire 4, escritura 49959 microsegundos; ACK -2 tras 560942 microsegundos. Fallo en SAM antes de READY; sin tap. La categoría original ya está identificada, pero no la condición interna que la produjo. No permite afirmar por sí sola que el bus no estuviese inicializado, que exista avería física o que baste con reiniciar el driver.

Evidencia: `.runtime/nfc-sam-idf-retry-result.json`. Monitor inactivo, puerto cerrado. Pendiente revisar las rutas de retorno de la versión exacta del driver para diferenciar error de estado, NACK y recuperación de transacciones.

Se conserva el retorno exacto de i2cWrite en una copia aislada de Wire, antes de que endTransmission lo traduzca a 0/2/4/5. La copia de PN532_I2C lee ese valor inmediatamente después de endTransmission y lo imprime al terminar el sondeo ACK, junto al diagnóstico existente. No hay registro serie durante la escritura o el sondeo. No se alteran comandos, reintentos ni límites configurados. Instrumentar añade instrucciones y salida serie posterior; no se afirma impacto temporal cero.

Copias en `.runtime/sam-idf-libraries/Wire` y `.runtime/sam-idf-libraries/PN532_I2C`, seleccionadas explícitamente con `--library`. Las librerías instaladas permanecen intactas. El firmware anterior está preservado en `.runtime/firmware-sam-wire-trace`.

Formato nuevo: PN532_SAM_IDF_V1=RC:<entero>. Valor -2147483647 significa que no se alcanzó i2cWrite, no un código ESP-IDF. El valor se guarda antes del sondeo ACK para que las lecturas posteriores no sustituyan la causa original. La instrumentación está destinada a este diagnóstico con un único usuario Wire; no es una API de producción para múltiples tareas.

El observador registra únicamente el entero como PN532_SAM_IDF. Tras flasheo autorizado, capturar un RESET manual con Seeker alejado. Correlacionar error original, Wire RC, ACK RC y duraciones; no cambiar parámetros basándose solo en duración. Si el error corresponde a un timeout interno, distinguirlo del código que devuelve la capa superior. Si el arranque pasa, no declarar resuelto el fallo intermitente.

No se ha flasheado este candidato. La estabilidad física sigue pendiente.
