# Serie de estabilidad del arranque

Firmware fijo: a737125122fe776f5aa9025e69fdaa61e3df93da93edc2c378715c6354fd20ec.
No cambiar código, binario, cables ni configuración durante esta serie.

Objetivo inicial: completar diez RESET manuales registrados con este candidato, contando los tres ya confirmados. Es una comprobación inicial de repetibilidad, no una certificación de estabilidad prolongada. Solicitar cada RESET por separado y detener la serie ante un fallo.

Por arranque exigir: recuperación final SCL/SDA altas; SAM con Wire/IDF/ACK correctos y respuesta válida; RF con respuesta válida; versión PN532 y READY. Registrar si SDA comenzó baja y cuántos pulsos necesitó. Excluir pulsaciones sin captura y no interpretar ausencia de mensajes como funcionamiento continuo.

Captura persistente: .runtime/nfc-bus-clear-boot-capture.jsonl. Primer arranque del candidato también consta en .runtime/dynamic-apdu-status-events.jsonl. El monitor web no controla esta captura y no debe abrir COM4 simultáneamente.

Después de la serie: probar búsqueda prolongada seguida de un intercambio NFC completo; comprobar arranques tras corte de alimentación conservando las conexiones. Preparar captura antes de considerar observado un arranque en frío. Corregir el error de conversión de longitud en una etapa separada, con prueba específica y nueva identidad de binario.

Resultados previos confirmados: 13:01:11 UTC PASS (líneas altas); 13:08:28 PASS (SDA baja, recuperada con siete pulsos); 13:09:11 PASS (líneas altas). Siguiente pulsación solicitada individualmente con captura abierta. Los resultados de esta serie no prueban la recuperación del caso SCL retenida.

4. 13:13:20 UTC, RESET confirmado: SDA inicial baja, SCL alta; cuatro pulsos, final 1/1; IDF/Wire/ACK=0, SAM/RF respuestas válidas, versión 1.6, READY. PASS. Dos recuperaciones de SDA registradas en cuatro arranques correctos.

5. 13:14:03 UTC, RESET confirmado: líneas iniciales/finales altas, cero pulsos; IDF/Wire/ACK=0, SAM/RF válidos, versión 1.6, READY. PASS.

6. 13:15:21 UTC, RESET confirmado: SDA inicial baja recuperada con dos pulsos; final 1/1; IDF/Wire/ACK=0, SAM/RF válidos, versión 1.6 y READY. PASS. Tres recuperaciones registradas (7, 4 y 2 pulsos).

7. 13:16:17 UTC, RESET confirmado: líneas altas, cero pulsos; IDF/Wire/ACK=0, SAM/RF válidos, versión 1.6, READY. PASS.

8. 13:16:57 UTC, RESET confirmado: líneas altas, cero pulsos; IDF/Wire/ACK=0, SAM/RF válidos, versión 1.6, READY. PASS.

9. 13:17:34 UTC, RESET confirmado: SDA baja recuperada con un pulso, final 1/1; IDF/Wire/ACK=0, SAM/RF válidos, versión 1.6, READY. PASS.

10. 13:18:26 UTC, RESET confirmado: líneas altas, cero pulsos; IDF/Wire/ACK=0, SAM/RF válidos, versión 1.6, READY. PASS.

Serie inicial completada: 10/10 arranques registrados correctos; cuatro recuperaciones de SDA baja (7, 4, 2 y 1 pulsos). No se observó SCL baja en estos diez arranques. Firmware fijo, sin nuevos flasheos durante la serie. No equivale a estabilidad prolongada ni valida NFC después de espera larga. Captura abierta; no pedir más RESET de esta serie.

Fallo posterior en el arranque 11 registrado, 13:22:54 UTC, al preparar intercambio NFC. RESET confirmado, sin tap solicitado. Líneas iniciales altas y recuperación omitida correctamente (0 pulsos). SAM entra con 1/1; fase6 STATE=8 TIMEOUT, EVENT=3 TIMEOUT, BUSY=1, SCL/SDA=0/0; tras reset interno sin bus clear BUSY=0, SCL/SDA=0/1. IDF=259, Wire=4, escritura 6838 us, ACK=-2 tras 561092 us. STOP INITIALIZATION. Total observado: 10 PASS y 1 FAIL; no afirmar estabilidad del candidato. Diferente del anterior TIMEOUT con EVENT=0 y SCL ya baja al inicio. Evidencia .runtime/nfc-bus-clear-hce-result.json. Cambio de captura serie previo documentado; no atribuir causalidad a ese cambio ni al tiempo de búsqueda sin comparación controlada.
