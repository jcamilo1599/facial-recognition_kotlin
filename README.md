### Facial Recognition (Android, Kotlin + Jetpack Compose)

Aplicación de ejemplo que implementa un flujo de autenticación biométrica por rostro con requisitos de liveness (parpadeo y giros de cabeza), usando CameraX para la cámara, ML Kit para detección de rostro y un backend HTTP para registro y login por imagen. Arquitectura modular orientada a features.

#### Índice
- Objetivos y alcance
- Arquitectura y estructura del proyecto
- Dependencias principales (Version Catalog)
- Configuración e instalación
- Permisos y Manifest
- Flujo de cámara y procesamiento en tiempo real
- Liveness: reglas, umbrales y estado (blink → izquierda → derecha)
- UI Overlay y feedback
- API de backend: endpoints, formato y respuesta
- Seguridad y buenas prácticas
- Solución a problemas comunes (pantalla negra, 503, permisos)
- Roadmap y posibles extensiones (TFLite embeddings, anti-spoofing avanzado)

---

### Objetivos y alcance

- Registro de nuevos usuarios mediante captura de rostro desde la cámara frontal.
- Login/validación de identidad capturando una nueva imagen.
- Liveness básico obligatorio antes de capturar: parpadeo y giros de cabeza (izquierda y derecha).
- Envío de la imagen capturada (JPEG/PNG) al backend vía multipart `file` para registro/login.
- Mostrar el texto literal de la respuesta del backend en un diálogo al finalizar.

Fuera de alcance (base preparada pero no implementada aún):
- Generación de embeddings locales (TensorFlow Lite) y verificación local.
- Anti-spoofing avanzado (brillo/nitidez/glare/profundidad) más allá del liveness inicial.

---

### Arquitectura y estructura del proyecto

Proyecto single-module con organización por features y capa común reutilizable.

Estructura principal:

- app/src/main/java/com/faacil/facial_recognition
  - MainActivity.kt: navegación, tema, AlertDialog de resultado.
  - feature/
    - home/HomeScreen.kt: pantalla inicial con acciones.
    - registration/presentation/RegistrationScreen.kt: flujo de registro con cámara + liveness + upload a /register.
    - login/presentation/LoginScreen.kt: flujo de login con cámara + liveness + upload a /login.
  - common/
    - camera/CameraPreview.kt: Composable que integra CameraX (Preview + ImageAnalysis + ImageCapture), conversión de imagen y controlador de captura.
    - ml/FaceAnalyzer.kt: Analyzer con ML Kit Face Detection, entrega `FaceFrame` a cada frame.
    - ml/EmbeddingGenerator.kt: interfaz para embeddings (TFLite) futura.
    - antispoofing/
      - LivenessProcessor.kt: máquina de estados para parpadeo y giros (izq/der), con tiempos/umbrales.
      - AntiSpoofingModels.kt: modelos de resultado y configuración (para extensiones futuras).
    - ui/FaceOverlay.kt: overlay con máscara, marco guía, prompt y barra de progreso.
    - permissions/CameraPermission.kt: helper para gestionar permiso de cámara.
    - network/
      - ApiClient.kt: Retrofit + OkHttp configurado con logging BODY y `Accept: application/json`.
      - FaceApi.kt: endpoints multipart `register` y `login` devolviendo `Response<ResponseBody>`.

---

### Dependencias principales (Version Catalog)

Ver `gradle/libs.versions.toml` para versiones exactas:
- Jetpack Compose BOM, Material3, Activity Compose, Navigation Compose
- Coroutines
- CameraX: core, camera2, lifecycle, video, view, extensions (1.4.1)
- ML Kit Face Detection (16.1.7)
- TensorFlow Lite (base, soporte) — preparado para embeddings
- Retrofit + Moshi + OkHttp + Logging Interceptor
- Accompanist Permissions

---

### Configuración e instalación

Requisitos
- Android Studio Jellyfish+ / Koala+
- SDK compile/target 36, minSdk 25
- Dispositivo o emulador con cámara frontal (en emulador: configurar Front Camera)

Pasos
1. Clonar el repositorio y abrir con Android Studio.
2. Sincronizar Gradle.
3. Conectar un dispositivo físico o configurar emulador con cámara frontal.
4. Ejecutar la app.

---

### Permisos y Manifest

`AndroidManifest.xml` incluye:
- CAMERA: requerido para los flujos
- INTERNET y ACCESS_NETWORK_STATE: para el envío al backend
- uses-feature cámara

En tiempo de ejecución, `WithCameraPermission` solicita el permiso y bloquea el contenido hasta concederlo.

---

### Flujo de cámara y procesamiento en tiempo real

- `CameraPreview` monta `PreviewView` y vincula 3 casos de uso de CameraX:
  - Preview: muestra imagen en pantalla (frontal por defecto, con fallback a trasera si falla)
  - ImageAnalysis: entrega frames a `FaceAnalyzer` (ML Kit)
  - ImageCapture: captura una foto en memoria (JPEG si es posible); soporta conversión desde YUV a JPEG si el dispositivo no entrega JPEG directo.
- Robustez:
  - Intenta combinaciones de cámara (frontal→trasera) y aspectos (16:9→4:3) con modos PERFORMANCE/COMPATIBLE para reducir pantalla negra.
  - Controlador `CaptureController` permite reintentar el bind en caso de error y solicitar una captura programática.

Conversión de imagen
- Si `ImageCapture` entrega JPEG (planes==1), se leen bytes directamente del buffer.
- Si entrega YUV_420_888, se convierte a NV21 y luego a JPEG en memoria.

---

### Liveness: reglas, umbrales y estado

`LivenessProcessor` guía al usuario por pasos:
1. Blink (parpadeo): ojos de abiertos (>0.6) a cerrados (<0.35) y nuevamente abiertos en 80–2500 ms.
2. TurnLeft: yaw ≤ −15° sostenido ≥ 300 ms.
3. TurnRight: yaw ≥ +15° sostenido ≥ 300 ms.

Cuando se completan los 3 pasos, `completed=true` y la pantalla captura y envía la foto al backend.

La UI (`FaceOverlay`) muestra:
- Un marco guía centrado para posicionar el rostro.
- Un texto con la instrucción actual.
- Una barra de progreso (0..1) según pasos completados.

---

### API de backend

Base URL (configurada en `ApiClient`):
`https://facial-recognition-api-215011024799.us-central1.run.app/`

Endpoints (ver `FaceApi`):
- POST /register — multipart, campo `file` (imagen PNG/JPEG)
- POST /login — multipart, campo `file` (imagen PNG/JPEG)

Comportamiento en la app
- Tras completar liveness, se captura la imagen y se normaliza (resize a máx. lado 1024 y compresión JPEG Q≈85; si >800KB baja a 75/65).
- Se envía la imagen. Si el backend responde 503, se reintenta una vez con imagen más pequeña (máx. 640, Q=75).
- Se lee el cuerpo literal de la respuesta (exitosa o de error) y se muestra en un diálogo al volver a Home.

Ejemplos de respuestas esperadas (provistas por el backend):
- Login OK (200): `{ "user_id": "...", "message": "Login exitoso." }`
- Registro duplicado (409): `{ "error": "Este rostro ya ha sido registrado." }`
- Service Unavailable (503): "Service Unavailable" (u otro texto)

Nota: El cliente agrega `Accept: application/json` y habilita logging de nivel BODY (OkHttp) para diagnóstico.

---

### Seguridad y buenas prácticas

- La imagen capturada no se persiste en almacenamiento local; sólo se mantiene en memoria el tiempo necesario para subirla.
- No se guardan embeddings ni fotos en disco en esta versión.
- Se cierran los `ImageProxy` en el analyzer para evitar saturar el pipeline.
- En futuras iteraciones: cifrar en tránsito (HTTPS ya habilitado), firmas/nonce, validación en backend cruzando metadatos.

---

### Solución a problemas comunes

Pantalla negra al abrir cámara
- Asegúrate de que el emulador tenga cámara frontal configurada (Front Camera: Emulated/Virtual Scene o webcam0).
- Se usan fallbacks de aspectos (16:9→4:3) y modos de `PreviewView` (PERFORMANCE→COMPATIBLE).
- Si hay error, se muestra un texto en rojo y un botón "Reintentar" para rebind.

HTTP 503 desde la app pero no desde curl
- Verifica que la imagen no exceda ~1MB. La app reduce y recomprime antes de subir.
- La app reintenta una vez con imagen más pequeña.
- En Logcat (tag OkHttp) revisa `Content-Type`, `Content-Length` y cuerpo de respuesta.

Permiso de cámara
- Si negaste con "No volver a preguntar", debes ir a Ajustes del sistema para concederlo manualmente.

---

### Roadmap / Extensiones

- Implementar `EmbeddingGenerator` con TFLite (128/256 dims) a partir de rostro recortado/alineado.
- Anti-spoofing avanzado: brillo/nitidez/glare/depth checks con métricas del frame.
- Persistencia segura de plantillas/embeddings (si aplica) y validación local antes de enviar.
- Mejoras de UX: indicadores por cada check, guía dinámica con cabeza/ojos.
- Manejo detallado de errores por tipo de respuesta (409, 422, 503, etc.).

---

### Scripts de prueba (curl)

Ejemplos brindados por el backend (ten en cuenta el doble slash no es necesario en la app):

Registro
```
curl --location 'https://facial-recognition-api-215011024799.us-central1.run.app/register' \
--header 'accept: application/json' \
--form 'file=@"/ruta/a/tu/imagen.png"'
```

Login
```
curl --location 'https://facial-recognition-api-215011024799.us-central1.run.app/login' \
--header 'accept: application/json' \
--form 'file=@"/ruta/a/tu/imagen.png"'
```

---

### Licencia

Uso educativo/demostrativo. Ajusta según las políticas de tu organización.
