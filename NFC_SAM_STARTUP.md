# Arranque mediante SAMConfiguration — candidato preparado

## Resultado físico posterior: estabilidad NO validada

Flasheado con autorización en COM4, salida 0 y cuatro hashes verificados. SHA-256 `feb1d8fb998f2bef27652413ab9dea1dd8f3d3b9d03f795bb40923b446dc5132`.

Primer RESET manual: PASS a las 11:47:18 UTC; SAM y RF write=0/read=0, versión 1.6 y READY. Siguiente RESET capturado: FAIL a las 11:47:46 UTC; SAM writeResult=-2 y readResult=null. La librería define -2 como timeout esperando ACK; no demuestra que SAM devolviese una respuesta de rechazo. Su writeCommand no propaga el retorno de Wire.endTransmission, por lo que falta distinguir escritura I2C fallida de ACK ausente tras escritura correcta. El límite local de sondeo ACK es PN532_ACK_WAIT_TIME=10.

El monitor cerró COM4 al fallar. El usuario indicó posteriormente que había pulsado 3 o 4 veces adicionales; las 2 o 3 posteriores al cierre no tienen captura y no cuentan como PASS ni FAIL. No hubo tap. No se completó la serie de estabilidad. Evidencia: `.runtime/nfc-sam-startup-result.json` y `.runtime/nfc-sam-startup-upload.log`.

El candidato permanece instalado. Siguiente diagnóstico necesario: capturar el retorno de la escritura I2C real del comando SAM y la fase de espera ACK, sin inferir la causa ni aumentar tiempos a ciegas. Eliminar la sonda previa no ha bastado para estabilizar este montaje.

La sonda de dirección del candidato anterior falló con timeout 5 y detuvo el arranque antes de enviar comandos PN532. Reintentar el código 2 no resolvió ese fallo.

La implementación de [Adafruit PN532](https://github.com/adafruit/Adafruit-PN532/blob/master/Adafruit_PN532.cpp), funciones begin y wakeup, omite la comprobación de dirección I2C para un PN532 potencialmente dormido y usa SAMConfig para entrar en modo normal. Esto fundamenta revisar nuestra secuencia; no demuestra retrospectivamente que el timeout observado fuese causado por reposo. Adafruit también contempla un pin de reset; no se ha añadido ni simulado ese cable en nuestra adaptación.

## Cambio

Eliminada la sonda vacía y su bucle de reintentos. Después de iniciar Wire mediante la librería existente se envía SAMConfiguration (14, 01, 14, 01), seguido de GetFirmwareVersion y RFConfiguration. READY requiere completar todas esas etapas. SAM y RF requieren ACK correcto y respuesta validada por la librería con longitud de payload cero. Se registran WRITE_RC y READ_RC sin exponer payloads; esos códigos pertenecen a la librería PN532 y no deben interpretarse como códigos Wire. El timeout de lectura permanece en 1000 ms.

El monitor y observador entienden PN532_BOOT_START_V1 y PN532_BOOT_V1. Se mantiene compatibilidad con registros anteriores PN532_INIT_V1. No se altera la librería instalada, cableado, reloj I2C, buffers ni APDUs. No hay reintentos de pruebas de acceso ni nuevas escrituras blockchain.

## Validación y límites

Compilación ESP32-S3 aprobada: 306221 bytes de programa, 23152 bytes globales. 56 tests Node aprobados; incluye fallo SAM sin posibilidad de anunciar READY. Estos tests no validan físicamente el despertar del PN532. Binario y SHA-256 en `.runtime/nfc-sam-startup-build.json`. No flasheado.

Después de autorización: usar observador actualizado para un primer RESET manual con Seeker alejado. Exigir resultados SAM y RF write=0/read=0, versión PN532 y READY. Detener ante cualquier fallo y conservar la etapa exacta; no sustituir un fallo por éxito. Solo tras pasar ese control ampliar la serie de arranques y finalmente repetir el intercambio STAFF. El capturador antiguo `.runtime/nfc-startup-series.mjs` exige sondas Wire y no es compatible con esta secuencia; no debe usarse sin adaptación.
