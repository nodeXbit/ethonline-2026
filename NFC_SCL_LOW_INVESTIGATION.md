# Investigación de SCL baja

Revisión de solo lectura del firmware, Wire, wrapper Arduino 3.3.11 y fuentes ESP-IDF b774170ff46. No se modificó ni flasheó código durante esta investigación.

## Hallazgos

1. El sketch fija SDA=17 y SCL=18 antes de nfc.begin. Wire.begin sin argumentos reutiliza esos pines; no los sustituye por los predeterminados 8/9. UART0 usa 43/44 en esta variante. No se encontró en el sketch otra asignación de GPIO18.
2. [Configuración I2C del commit exacto](https://github.com/espressif/esp-idf/blob/b774170ff46/components/esp_driver_i2c/i2c_common.c): configura SCL con entrada habilitada, salida open-drain, pull-up interno, sin pull-down y conectada al periférico I2C. Por ello no hay fundamento en el código para descartar gpio_get_level por entrada deshabilitada. No equivale a verificar los registros efectivos en ejecución.
3. La traza anterior muestra SCL=0 antes y después de la transacción, SDA=1, BUSY=0 y ningún evento final. Esto es compatible con una línea que no sube, pero no identifica quién la conduce ni demuestra ausencia de pulsos entre muestras.
4. El wrapper Arduino llama expresamente esp_log_level_set("i2c.master", ESP_LOG_NONE) al iniciar el bus. Esto explica que habilitar solo su captura en el observador no bastase. Fue una omisión en la revisión previa; no hacía falta interpretar aquella ausencia como evidencia del driver.

## Lo que falta medir

Una captura comparativa de GPIO18 antes de iniciar I2C, después de configurarlo y con la salida del ESP32 deshabilitada permite separar dos casos. Si continúa baja con el ESP32 liberado y entrada/pull-up confirmados, el nivel bajo no depende de que el ESP32 la esté conduciendo: habrá que investigar el periférico o el circuito conectado, sin asumir una avería. Si sube al liberar la salida, la configuración/controlador del ESP32 pasa a ser el principal candidato; el cambio puede también alterar el estado del bus y debe registrarse como intervención.

La prueba puede hacerse sin mover cables, pero requiere un firmware diagnóstico controlado. Debe deshabilitar la salida, no forzar un nivel alto push-pull contra otro dispositivo. Primero tomar muestras; no añadir pulsos de recuperación ni nuevos retries. Si se quiere cuantificar tensión o observar pulsos, los niveles digitales no sustituyen un instrumento externo.

Conclusión: hemos descartado por inspección la explicación de pines predeterminados y encontrado el silenciamiento de logs. La procedencia de SCL baja sigue sin demostrarse. No se justifica todavía sustituir hardware ni aumentar timeouts.
