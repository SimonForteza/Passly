# Passly Validador (app móvil)

App de React Native (Expo SDK 58) para el **validador** en la puerta del evento. Escanea el QR de
la entrada con la cámara del celular y lo valida contra el backend.

**No tiene backend propio.** Usa exactamente los mismos endpoints que la pantalla web `/validar`
(CLAUDE.md §2: "la API no cambia entre una y otra"):

| Paso | Endpoint |
|---|---|
| Ingresar | `POST /api/auth/login` → JWT |
| Productoras de quien valida | `GET /api/productoras/mias` |
| Eventos a validar | `GET /api/eventos?productora={id}` (solo `PUBLICADO`) |
| Validar | `POST /api/accesos/validaciones` `{ idEvento, contenidoQr }` |

Toda la autorización sigue en el backend: rol `VALIDADOR` + membresía en la productora del
evento, uso único atómico en Tickets. La app solo muestra el resultado (VÁLIDO / YA USADO /
OTRO EVENTO / INVÁLIDO). El token vive en memoria; solo se guarda la dirección del servidor.

## Compilar e instalar el APK (Android)

Requisitos: Node 20+, Android SDK (`ANDROID_HOME`), JDK 17+ y el celular con **depuración USB**
activada.

```powershell
cd app-validador
npm install
npx expo prebuild -p android          # genera android/ (no se versiona)
cd android
.\gradlew.bat assembleRelease          # firmado con la clave debug: sirve para instalar por USB
adb install -r app\build\outputs\apk\release\app-release.apk
```

Para desarrollo con recarga en caliente: `npx expo run:android` con el celular conectado.

**No agregar `react-native-safe-area-context`.** Su código C++ genera rutas de más de 260
caracteres y el build falla en Windows (`Filename longer than 260 characters`). Los márgenes de
las barras del sistema se resuelven en `App.tsx` con `StatusBar.currentHeight` y un margen fijo.

## Conectar el celular al backend

1. Celular y notebook en **la misma red**. Si la red de la facultad aísla los dispositivos,
   compartir internet desde el celular (hotspot) y conectar la notebook ahí.
2. IP de la notebook: `ipconfig` → "Dirección IPv4" del adaptador Wi-Fi.
3. Permitir el puerto 8080 en el firewall de Windows (PowerShell **como administrador**, una vez):
   ```powershell
   New-NetFirewallRule -DisplayName "Passly 8080" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow
   ```
4. Prueba previa: en el navegador del celular abrir `http://<IP>:8080/actuator/health` → `UP`.
5. En la app, campo **Servidor**: `http://<IP>:8080`. Queda guardado para la próxima vez.

El APK permite HTTP plano (`usesCleartextTraffic`, vía `expo-build-properties`) porque el backend
local no tiene HTTPS. La cámara nativa, a diferencia de la del navegador, no exige HTTPS.

## Uso

1. Ingresar como `validador@passly.test` / `passly1234` (perfil demo).
2. Elegir el evento.
3. Apuntar al QR (puede estar en la pantalla de la notebook o en otro celular).
4. "Validar siguiente entrada" para el próximo. Si la cámara falla, pegar el contenido
   `PASSLY:v1:…` en "Ingreso manual del QR".
