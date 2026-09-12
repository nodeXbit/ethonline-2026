# Coordinación de la prueba NFC

La última prueba solicitó tap demasiado cerca de la caducidad del monitor. El monitor cerró a las 13:39:05 UTC. Android recibió SELECT a las 13:39:21 y contestó 9000; GET_CREDENTIAL contestó 6985 con credential=NONE. La observación anterior de Ready to tap no demuestra que siguiera disponible en ese momento.

NfcSelectionState.current exige publicación de hasta 60000 ms y snapshot de hasta 60 segundos, además de wallet, propiedad y registro válidos. MainActivity actualiza el texto cada segundo. No se ha demostrado un defecto del refresco visual; no se modifica Android ni su vigencia.

Cambio: límite global de sesión serie del monitor de 120000 a 600000 ms para preparar RESET, teléfono y tap. Los límites independientes posteriores al desafío y su frescura no se cambian. El límite de diez minutos cubre la sesión completa, no reinicia el plazo al hacer tap.

Validación: 17 pruebas de dynamic-gate-bridge y gate-monitor correctas. No constituye validación física del nuevo plazo. Observador reiniciado para cargar el cambio; sin flashear.

Procedimiento: capturar RESET y confirmar READY; refrescar STAFF y esperar final de actualización; comprobar Ready to tap e indicar tap inmediatamente. Si no se hace antes de caducar la selección, refrescar de nuevo; no rebajar controles para aceptarla. Guardar evidencia de ambos lados. Resultado esperado para STAFF/Lab sigue dependiendo de la política del recurso: un rechazo RESOURCE_POLICY_MISSING con prueba y confirmación válidas sería intercambio completado, no autorización de acceso.

Validación física completada 13:43:43–13:43:47 UTC, tap confirmado. SELECT, GET_CREDENTIAL STAFF, desafío 109 bytes/9000, STATUS READY, firma 67 bytes/9000 y prueba de 65 bytes recibida. Holder Verified, registration Valid, global Allowed, proof Fresh, controller Confirmed. Resultado de autorización ACCESS DENIED / RESOURCE_POLICY_MISSING para Lab. Intercambio completo PASS; no se escribió política onchain. Evidencia .runtime/nfc-coordinated-hce-result.json y Android .runtime/nfc-irq-hce-android.log (15:43:43–15:43:57 Madrid). Firmware 49140150…b08ed9e. No resuelve ni reproduce el fallo intermitente SCL/SAM pendiente.
