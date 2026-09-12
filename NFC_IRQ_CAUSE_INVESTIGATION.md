# Causa de interrupción durante SAM

El fallo del arranque 11 tenía STATE=TIMEOUT y EVENT=TIMEOUT, pero ESP-IDF v5.5.5 b774170ff46 agrupa I2C_LL_INTR_TIMEOUT e I2C_LL_INTR_ARBITRATION en esa misma rama de i2c_master_isr_handler_default. No se puede afirmar todavía que se agotó el límite de clock stretching. La escritura duró 6838 us; ese total no mide directamente el tiempo de SCL baja.

Arduino ESP32 3.3.11 deja scl_wait_us=0 (valor por defecto del driver). NXP documenta stretching al despertar por I2C sin H_REQ, pero eso no demuestra que este arranque estuviese despertando de PowerDown. No se modifica el timeout basándose solo en esa posibilidad.

Fuentes: driver exacto preservado en .runtime/driver-state-libraries/PN532_I2C/i2c_master.original.txt; https://github.com/espressif/esp-idf/blob/b774170ff46/components/esp_driver_i2c/i2c_master.c ; https://www.nxp.com/docs/en/user-guide/141520.pdf sección 6.3.3 y 7.2.11.

Candidato separado .runtime/irq-cause-libraries: conserva recuperación SDA, protocolo y tiempos. Añade acumulación atómica OR de las máscaras de interrupción durante la escritura SAM; se imprime después del ACK, nunca dentro de la ISR. PN532_IRQ_V1 registra MASK y las constantes TIMEOUT/ARBITRATION para interpretar bits sin asumir valores. La instrumentación añade una operación en ISR y no es temporalmente neutra. La máscara acumulada no identifica el orden si aparecen varios motivos.

Interpretación: MASK & TIMEOUT indica interrupción de timeout; MASK & ARBITRATION indica arbitraje. Ambos bits requieren conservar la ambigüedad de orden. Máscara cero con timeout software indica otro camino. No atribuir causa eléctrica completa solo por el bit.

La serie anterior sigue siendo 10 PASS y 1 FAIL; cualquier prueba de este nuevo binario se registra aparte. Si el bus continúa con SCL baja al entrar, la recuperación detendrá el arranque antes de SAM: esa prueba no reproduce la interrupción buscada. Será necesario recuperar la alimentación sin cambiar cables antes de repetir; no interpretar ese STOP como fallo nuevo de SAM.

Compilación correcta: programa 307945, globales 23504; binario 308096 bytes, SHA256 4914015019c3659de25fd13d55ccb314b09ac4b0dfd7667f5407874e9b08ed9e. Mapa confirma driver instrumentado. Flasheo autorizado ejecutado, salida 0; reset automático no capturado. Log .runtime/nfc-irq-cause-upload.log. Captura independiente .runtime/nfc-irq-cause-boot-capture.jsonl.

13:27:49 UTC RESET confirmado: INITIAL_SCL=0, SDA=1; cero pulsos, SCL sigue baja, OK=0 y STOP INITIALIZATION antes de SAM. No hay máscara IRQ que analizar. Requiere ciclo de alimentación para recuperar condición inicial; mantener cableado entre placas.

Tras apagado completo confirmado, captura reabierta 13:29:31 UTC; arranque automático de reconexión no capturado. RESET manual 13:29:55: SCL=1/SDA=0, recuperación siete pulsos, final 1/1. SAM IRQ MASK=128, TIMEOUT=256, ARBITRATION=32: ambos bits de fallo ausentes. IDF/Wire/ACK=0, SAM/RF válidos, PN532 1.6, READY. Primer PASS registrado de este binario después del ciclo de alimentación; no clasificar como arranque en frío.

13:30:51 UTC segundo RESET confirmado desde búsqueda: SDA baja recuperada con seis pulsos; IRQ MASK=128 sin TIMEOUT(256) ni ARBITRATION(32); IDF/Wire/ACK=0, SAM/RF válidos y READY. Dos PASS tras ciclo de alimentación. No reproducido aún el fallo que se pretende discriminar. Captura COM4 sigue abierta, sin nuevos flasheos.

13:34:06 UTC tercer RESET confirmado: 193.819 segundos entre READY anterior y primera línea del nuevo arranque (incluye demora de arranque; no es medición exacta del instante físico RESET). SDA inicial baja recuperada con seis pulsos; final 1/1. MASK=128, sin bits TIMEOUT ni ARBITRATION; IDF/Wire/ACK=0; SAM/RF válidos y READY. PASS tras unos tres minutos de espera. No valida intercambio NFC ni actividad continua durante el silencio. Tres PASS de este binario tras apagado completo; fallo pendiente no reproducido.
