# LockENS Four-Smartphone Gate Stand

## Informe de integración y validación física

**Fecha:** 13 de septiembre de 2026

**Zona horaria:** Europe/Madrid

**Alcance:** desde la conexión de los cuatro smartphones hasta la regresión
física de Front Door, Lab y Server Room.

**Estado:** Gate Stand físicamente validado; políticas de recurso y matriz ALLOW
todavía pendientes.

Este documento está preparado para incorporarse como contexto al proyecto de
ChatGPT. No contiene números de serie ADB, direcciones IP privadas, secretos,
firmas, pruebas criptográficas completas ni datos de autenticación.

## 1. Resumen ejecutivo

Se conectaron cuatro smartphones Android y se transformó el Android Gate Reader
ya validado en un equipo de demostración formado por un teléfono portador de la
credencial y tres puertas visuales independientes:

| Rol actual | Dispositivo | Android/API | Función |
|---|---|---:|---|
| Holder | Pixel 9 Pro XL | Android 17 / API 37 | Publica `staff-001` por HCE |
| Front Door | Oppo CPH2195 | Android 13 / API 33 | Reader Mode para Front Door |
| Lab | Seeker | Android 16 / API 36 | Reader Mode para Lab |
| Server Room | Samsung SM-A536B | Android 15 / API 35 | Reader Mode para Server Room |

Los cuatro dispositivos terminaron con NFC encendido. Las tres puertas ejecutan
la misma APK, pero cada una conserva un perfil de recurso persistente y no
muestra selector en la pantalla normal. Un único Node local conserva la decisión
autoritativa y mantiene sesiones aisladas para las tres puertas.

La regresión final usó la credencial existente:

`staff-001.keys.demo-access.eth`

Las tres puertas obtuvieron correctamente:

`RESOURCE_POLICY_MISSING` y `allowed=false`

Esto es el resultado esperado porque la credencial tiene holder, registro y
acceso global válidos, pero todavía no existe su política `resources.v1`.

## 2. Límites de seguridad preservados

- El teléfono Gate es transporte no confiable; no decide ALLOW o DENY.
- Node vuelve a validar holder, registro, acceso global, política de recurso,
  prueba, frescura y contexto de la sesión.
- El recurso queda ligado de forma inmutable a cada sesión.
- No se modificaron el AID, los APDU, el challenge de 104 bytes, la prueba del
  holder, los límites de frescura ni las defensas de replay/TOCTOU.
- La puerta visual sólo inicia su animación de apertura después de una respuesta
  Node con `allowed=true`.
- DENY y cualquier error técnico mantienen la puerta cerrada.
- El acceso HTTP local sin TLS se limita a `127.0.0.1`; no se habilitó cleartext
  general en Android.
- No se modificaron Studio, firmware, ESP32 ni PN532.
- Blockchain writes durante este trabajo: **0**.
- Políticas aplicadas durante este trabajo: **0**.

## 3. Arquitectura final de la prueba

```text
Pixel holder (HCE + staff-001)
              |
              | NFC / ISO-DEP
              v
Oppo Front Door | Seeker Lab | Samsung Server Room
              |
              | túnel localhost independiente por gate
              v
Un Node autoritativo en el PC
              |
              | lecturas ENS y verificación de política
              v
ALLOW o DENY autoritativo
```

El PC puede quedar oculto durante la presentación, pero debe permanecer
encendido. La configuración actual elimina los cables visibles mediante ADB por
Wi-Fi en una red privada; no convierte aún Node en un servicio autónomo alojado
en los teléfonos.

## 4. Cronología del trabajo

### 4.1 Android Gate Reader de referencia

El punto de partida fue el verificador físico principal ESP32 + PN532,
checkpoint `ec82f3f`, cuyo resultado conocido era:

`staff-001 -> Lab -> holder valid -> global Allowed -> RESOURCE_POLICY_MISSING`

Después se construyó el fallback Android Reader Mode, conservando el mismo HCE,
protocolo y verificador Node. El checkpoint del Android Gate Reader es
`734619c`. La primera topología usó Pixel como lector y Seeker como holder y
validó físicamente SELECT, GET_CREDENTIAL, challenge, prueba y DENY de Node.

### 4.2 Inventario de cuatro teléfonos

Al conectar los cuatro smartphones se hizo inventario explícito por modelo y se
comprobó NFC, Reader Mode, IsoDep, versión Android y presencia de la APK. Los
identificadores físicos se guardaron únicamente en `.runtime`, que no se
versiona.

La intención inicial era mantener el Seeker como holder. Durante las pruebas se
observó que Samsung podía leerlo, pero la combinación Oppo lector + Seeker HCE
fallaba en la interfaz RF. No se alteró el protocolo para adaptarlo a un único
fabricante.

### 4.3 Cambio de topología por compatibilidad NFC

El Pixel demostró HCE estable frente a los tres lectores. Por decisión del
usuario se fijó la topología actual:

- Pixel como holder principal.
- Oppo como Front Door.
- Seeker como Lab.
- Samsung como Server Room.

Este cambio mantuvo intacta la autorización y resolvió la incompatibilidad RF
sin modificar PN532 ni HCE.

### 4.4 Fallos de integración observados

Durante la primera matriz de tres puertas aparecieron estados atascados en
`READING`, `VERIFYING`, `PENDING` o `FAILED`, ausencia temporal del nombre ENS y
falta de reinicio del lector.

Se aislaron tres causas:

1. La publicación HCE del Pixel tenía una ventana de frescura de 60 segundos y
   podía caducar durante una demostración.
2. Android bloqueaba la llamada HTTP del gate a `127.0.0.1`, produciendo
   `NODE_UNAVAILABLE`, aunque `adb reverse` y Node respondían.
3. Los estados terminales y errores técnicos no limpiaban siempre la UI para el
   siguiente participante.

### 4.5 Correcciones aplicadas

- Refresco automático del snapshot HCE del holder cada 45 segundos mientras la
  app está visible y existe una credencial seleccionada.
- Se conserva el límite de frescura de seguridad de 60 segundos.
- Configuración de red Android que permite cleartext exclusivamente para
  `127.0.0.1`.
- Resultado autoritativo o error visible durante 10 segundos, seguido de un
  retorno limpio a READY.
- Un toque nuevo puede asumir la superficie visual, aunque en la presentación
  se recomienda separar los teléfonos y esperar READY.
- Sustitución del marco rojo de DENY por una animación grande con símbolo de
  prohibido sobre la puerta cerrada.
- Panel reducido a Credential, Holder, Global Access, Resource Access, Proof y
  Final Decision.

### 4.6 Gate Stand visual

Se generaron tres assets originales locales, sin descargas ni dependencias de
runtime:

- Front Door: lobby, recepción, sofás, plantas e iluminación cálida.
- Lab: bancos, microscopios, instrumental, armarios y taburetes.
- Server Room: pasillo de racks, LEDs, refrigeración y cableado.

La misma escena se recorta dentro de la abertura de la puerta. El panel móvil
oculta esa zona mientras está cerrado; si Node devuelve ALLOW, la animación
revela el mobiliario correspondiente al recurso. En DENY la escena interior
permanece cubierta.

### 4.7 Eliminación de cables visibles

Se activó ADB TCP únicamente en los tres gates sobre la misma red privada. El
Pixel holder no necesita depuración inalámbrica. Para cada gate se comprobó:

- conexión ADB por Wi-Fi;
- instalación del APK mediante el identificador inalámbrico;
- túnel independiente hacia el puerto local de Node;
- lanzamiento del perfil persistente correcto.

Existe un reconector local ignorado por Git en
`.runtime/start-wireless-gates.ps1`. Si un teléfono reinicia y desactiva el modo
ADB TCP, puede ser necesario habilitarlo otra vez por USB o utilizar el emparejado
oficial de Wireless Debugging.

## 5. Regresión física final de las tres puertas

### 5.1 Lab

**Gate:** Seeker / Lab

**Evidencia:** 04:21:06-04:21:08, Europe/Madrid

```text
HOLDER — VERIFYING
GET_CREDENTIAL 9000 credential=staff-001.keys.demo-access.eth
decision=RESOURCE_POLICY_MISSING allowed=false
```

La puerta permaneció cerrada y regresó a READY con todos los campos limpios.
Además del intento final se observaron varias ejecuciones anteriores con el
mismo resultado autoritativo.

### 5.2 Front Door

**Gate:** Oppo / Front Door

**Evidencia final:** 04:24:42-04:24:44, Europe/Madrid

```text
HOLDER — VERIFYING
GET_CREDENTIAL 9000 credential=staff-001.keys.demo-access.eth
decision=RESOURCE_POLICY_MISSING allowed=false
```

Front Door permaneció cerrada y volvió a READY. Hubo un intento intermedio con
`GET_STATUS_STATUS`; falló cerrado y el reintento inmediato completó la
autorización esperada. Esto se considera una incidencia de transporte
transitoria, no un cambio de política.

### 5.3 Server Room

**Gate:** Samsung / Server Room

**Evidencia:** 04:26:22-04:26:23, Europe/Madrid

```text
HOLDER — VERIFYING
GET_CREDENTIAL 9000 credential=staff-001.keys.demo-access.eth
decision=RESOURCE_POLICY_MISSING allowed=false
```

Se capturó visualmente:

- Credential: `staff-001.keys.demo-access.eth`
- Holder: `VERIFIED`
- Global Access: `ALLOWED`
- Resource Access: `MISSING`
- Proof: `FRESH`
- Final Decision: `ACCESS DENIED / RESOURCE_POLICY_MISSING`
- puerta cerrada con símbolo de prohibido;
- contador `NEXT SCAN IN 10s`;
- retorno posterior a READY con campos limpios.

Un segundo contacto iniciado antes de separar los teléfonos produjo después un
`TRANSPORT_FAILURE`. El sistema volvió a fallar cerrado y se recuperó. La
secuencia recomendada evita mantener ambos teléfonos juntos después del
resultado.

## 6. Resultado de la matriz actual

| Credencial | Gate | Transporte | Node | Puerta | Reset |
|---|---|---|---|---|---|
| staff-001 | Front Door | PASS | `RESOURCE_POLICY_MISSING` | Cerrada | PASS |
| staff-001 | Lab | PASS | `RESOURCE_POLICY_MISSING` | Cerrada | PASS |
| staff-001 | Server Room | PASS | `RESOURCE_POLICY_MISSING` | Cerrada | PASS |

La igualdad del resultado entre gates es correcta para el estado onchain
actual: falta la política de recursos de `staff-001`.

## 7. Validación automatizada y de instalación

- Android `testDebugUnitTest`: **261/261**, 0 fallos, 0 errores.
- Android `assembleDebug`: **PASS**.
- Node `node --test`: **238/238**, 0 fallos.
- `git diff --check`: **PASS**; sólo avisos de conversión LF/CRLF de Windows.
- Instalación inalámbrica del APK: **PASS en los tres gates**.
- Perfiles persistentes leídos desde almacenamiento privado:
  `front-door`, `lab`, `server-room`.
- Node local accesible en el puerto 8792: **PASS**.
- Tres túneles independientes simultáneos: **PASS**.
- Revisión visual física de las tres pantallas READY: **PASS**.

Las capturas y logs completos permanecen bajo `.runtime/gate-screens/` y no se
añaden al repositorio para evitar datos específicos de los dispositivos.

## 8. Estado Git y checkpoints

- Baseline físico NFC principal: `ec82f3f`.
- Android Gate Reader: `734619c`.
- Gate Stand base ya publicado en `origin/main`: `37331ab`.
- Refinamiento visual local: `dc6dde4`.
- Símbolo animado de DENY local: `47ef36`.
- Al cerrar la validación física, los últimos fixes de refresco, red, reset y
  assets originales seguían **sin commit**.
- No se hizo commit ni push durante la prueba física.

La condición física previa al checkpoint se cumplió y el usuario autorizó por
separado el checkpoint final. Este informe forma parte de ese checkpoint; su SHA
resultante se registra en el resultado posterior al push para evitar una
referencia circular dentro del propio commit.

## 9. Políticas y blockchain

Las políticas de la demo todavía no se han aplicado. En particular, no se ha
creado `resources.v1` para STAFF, VISITOR o CONTRACTOR durante esta fase.

Por ese motivo todavía no puede validarse físicamente la apertura ALLOW ni la
matriz final prevista:

| Caso futuro | Resultado objetivo |
|---|---|
| STAFF -> Lab | ACCESS GRANTED |
| STAFF -> Server Room | ACCESS GRANTED |
| VISITOR -> Lab | RESOURCE_NOT_ALLOWED |
| VISITOR -> Front Door | ACCESS GRANTED |
| CONTRACTOR -> Lab | ACCESS_SUSPENDED |

Antes de cualquier escritura onchain deben cumplirse, en orden:

1. revisar el diff completo ya físicamente validado;
2. autorizar commit y push;
3. dejar el repositorio limpio y exigir `HEAD == origin/main`;
4. revisar el plan exacto de transacciones y nonces;
5. obtener autorización explícita para las escrituras blockchain;
6. ejecutar y validar la matriz física ALLOW/DENY completa.

## 10. Operación recomendada para la demo

1. Encender PC y ejecutar el reconector local de gates.
2. Confirmar que las tres pantallas muestran READY.
3. Mantener el Pixel desbloqueado en My Keys con la credencial deseada
   seleccionada y `Ready to tap`.
4. Acercar la parte superior trasera del Pixel al gate durante 2-3 segundos.
5. Esperar el resultado final sin moverlo bruscamente.
6. Separar completamente los teléfonos cuando aparezca ALLOW o DENY.
7. Esperar el retorno a READY antes del siguiente participante.

Para una instalación realmente autónoma y sin PC visible ni operativo, la
opción recomendada es mover el mismo Node a un mini-PC dedicado en la red local.
Eso requiere diseñar transporte LAN autenticado y no forma parte de este cambio
visual.

## 11. Veredicto

El equipo de cuatro smartphones está operativo sin cables USB visibles y las
tres puertas han pasado su regresión física frente al Node autoritativo. Los
fallos técnicos observados fueron fail-closed y recuperables. La animación de
DENY, el contador de 10 segundos, el reset limpio, los perfiles persistentes y
las sesiones aisladas están validados.

El Gate Stand está físicamente validado e incluido en su checkpoint autorizado.
El sistema completo de demo aún no debe declararse terminado porque faltan las
políticas onchain y la matriz física final con casos ALLOW y DENY.
