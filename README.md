### Facial Recognition (Android, Kotlin + Jetpack Compose)

Aplicación que implementa un sistema completo de autenticación biométrica facial. Incluye registro y
login online mediante un backend desarrollado en Python, así como autenticación local offline
mediante embeddings vectoriales. Utiliza CameraX, ML Kit y TensorFlow Lite.

---

### Características Principales

- **Registro y Login Online:** Captura de rostro con validación de liveness y envío a backend para
  almacenamiento centralizado.
- **Autenticación Local (Offline):** Verificación de identidad en el dispositivo sin conexión a
  internet, comparando el rostro actual con el perfil biométrico descargado previamente.
- **Liveness Detection:** Prueba de vida activa obligatoria (parpadeo y giros de cabeza) para evitar
  suplantación con fotos estáticas.
- **Almacenamiento Seguro:** Los vectores biométricos (embeddings) se almacenan localmente para
  permitir el acceso offline.
- **Alta Precisión:** Uso del modelo FaceNet (512 dimensiones) con preprocesamiento alineado entre
  móvil y backend.

---

### Arquitectura y Estructura del Proyecto

Estructura principal:

- `app/src/main/java/com/faacil/facial_recognition`
    - `feature/`
        - `registration/`: Flujo de registro (Cámara -> Liveness -> Backend).
        - `login/`: Flujo de login online (Cámara -> Liveness -> Backend -> Guardado de Embedding).
        - `local_auth/`: **(Nuevo)** Flujo de autenticación offline (Cámara -> Liveness ->
          Comparación Local).
    - `common/`
        - `camera/`: Implementación de CameraX. Incluye corrección automática de rotación de imagen
          basada en sensores.
        - `ml/`:
            - `FaceAnalyzer.kt`: Detección de rostros con ML Kit.
            - `FaceNet.kt`: **(Nuevo)** Wrapper de TensorFlow Lite para generar embeddings (512-d).
        - `storage/`:
            - `LocalEmbeddingStorage.kt`: **(Nuevo)** Gestión de persistencia de embeddings en
              SharedPreferences.
        - `antispoofing/`: Lógica de detección de vida (parpadeo, giros).

---

### Dependencias principales (Version Catalog)

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

En tiempo de ejecución, `WithCameraPermission` solicita el permiso y bloquea el contenido hasta
concederlo.

---

### Detalles Técnicos de Implementación

#### 1. Modelo de Reconocimiento

Se utiliza **FaceNet (Inception ResNet v1)** cuantizado a float32.

- **Archivo:** `facenet_512.tflite`
- **Input:** 160x160 px (RGB)
- **Output:** Vector de 512 dimensiones (Embedding)

#### 2. Pipeline de Preprocesamiento

Para garantizar la compatibilidad entre Android y el Backend (Python), se sigue un pipeline
estricto:

1. **Detección y Recorte:** Se usa ML Kit para detectar el rostro y recortar el bounding box exacto
   de la imagen original.
2. **Corrección de Rotación:** Se aplica la rotación necesaria al Bitmap según los metadatos
   EXIF/Sensor antes del recorte.
3. **Redimensionamiento:** El recorte se escala a 160x160 píxeles (Bilinear).
4. **Estandarización (Whitening):** Se normalizan los píxeles: `(pixel - mean) / std`.
5. **Normalización L2:** El vector resultante de 512 dimensiones se normaliza para que su magnitud
   sea 1.0.

#### 3. Comparación y Umbrales

La similitud se calcula mediante **Distancia Euclidiana**.

- **Umbral de Aceptación:** `0.8` (Ajustado para alta seguridad tras implementar recorte preciso).
- **Lógica:** `distancia < 0.8` implica identidad verificada.

---

### Flujo de Autenticación Local

1. El usuario realiza **Login Online** una vez.
2. El backend devuelve el embedding (vector de 512 floats) del usuario.
3. La app guarda este vector en `SharedPreferences` (seguro para datos privados de la app).
4. En futuros accesos, el usuario selecciona **"Autenticación Local"**.
5. Se realiza el proceso de Liveness y captura.
6. Se genera el embedding de la nueva foto en tiempo real (offline).
7. Se compara matemáticamente con el vector guardado.

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

- Tras completar liveness, se captura la imagen y se normaliza (resize a máx. lado 1024 y compresión
  JPEG Q≈85; si >800KB baja a 75/65).
- Se envía la imagen. Si el backend responde 503, se reintenta una vez con imagen más pequeña (máx.
  640, Q=75).
- Se lee el cuerpo literal de la respuesta (exitosa o de error) y se muestra en un diálogo al volver
  a Home.

Ejemplos de respuestas esperadas (provistas por el backend):

- Login OK (200): `{ "user_id": "...", "message": "Login exitoso." }`
- Registro duplicado (409): `{ "error": "Este rostro ya ha sido registrado." }`
- Service Unavailable (503): "Service Unavailable" (u otro texto)

Nota: El cliente agrega `Accept: application/json` y habilita logging de nivel BODY (OkHttp) para
diagnóstico.

---

### Seguridad y buenas prácticas

- **Privacidad:** La imagen capturada no se persiste en almacenamiento local; sólo se mantiene en
  memoria el tiempo necesario para el procesamiento.
- **Embeddings:** Los vectores biométricos se almacenan localmente para permitir la funcionalidad
  offline, pero no pueden reconstruirse para obtener la imagen original del rostro.
- **Gestión de Recursos:** Se cierran los `ImageProxy` y se liberan los Bitmaps inmediatamente
  después de su uso para evitar fugas de memoria.
- **Comunicación:** Todo el tráfico con el backend viaja cifrado (HTTPS).
