# Observación A2 del arranque legacy

Fecha: 2026-09-12. Ventana: 10:14:19–10:16:19 UTC (12:14:19–12:16:19 Madrid).

Se reabrió COM4 a 115200 para observar el firmware legacy ya instalado. No hubo flash, recompilación, cambio de código, ajuste Android, escritura al protocolo ni cambio solicitado en hardware/cableado/alimentación. El teléfono debía permanecer alejado.

La consola de captura contó todos los bytes recibidos, además de clasificar las líneas. Resultado al cerrar por timeout:

| Medida | Resultado |
|---|---:|
| Apertura COM4 | Correcta |
| Eventos de error serie | 0 |
| Bytes UART recibidos | 0 |
| Bloques de datos recibidos | 0 |
| Líneas reconocidas/no reconocidas | 0 / 0 |
| Comandos enviados | 0 |

Se pidió un RESET manual y su confirmación. **La confirmación del usuario sigue pendiente al redactar este registro.** Por tanto, no se declara fallo del PN532 ni se clasifica la causa como común o específica del dinámico. SELECT e intercambio no alcanzados; B no ejecutado.

La captura terminó y COM4 quedó cerrado. La ausencia de datos queda ahora medida, a diferencia de la captura A1 que no contaba bytes crudos.

Evidencia: [eventos](.runtime/nfc-ab-a2-boot-events.jsonl) y [resumen](.runtime/nfc-ab-a2-boot-summary.json).
