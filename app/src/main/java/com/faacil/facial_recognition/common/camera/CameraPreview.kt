package com.faacil.facial_recognition.common.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.media.Image
import android.util.Size
import android.view.ViewGroup
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

typealias AnalyzerProvider = (ExecutorService) -> ImageAnalysis.Analyzer

@SuppressLint("RestrictedApi")
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    analyzerProvider: AnalyzerProvider,
    targetResolution: Size = Size(720, 1280),
    onCaptureController: ((CaptureController) -> Unit)? = null,
    onCameraReady: (() -> Unit)? = null,
    onCameraError: ((Throwable) -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
    }

    AndroidView(
        factory = {
            previewView.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
                // Modo compatible evita problemas de black screen en algunos dispositivos/emu
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        modifier = modifier
    )

    LaunchedEffect(Unit) {
        val analyzer = analyzerProvider(cameraExecutor)
        val binder: () -> Unit = {
            bindCameraUseCases(
                context = context,
                lifecycleOwner = lifecycleOwner,
                previewView = previewView,
                analyzer = analyzer,
                targetResolution = targetResolution,
                cameraExecutor = cameraExecutor,
                imageCapture = imageCapture,
                onReady = { onCameraReady?.invoke() },
                onError = { onCameraError?.invoke(it) }
            )
        }
        binder()
        onCaptureController?.invoke(object : CaptureController {
            override fun captureJpeg(callback: (result: ByteArray?) -> Unit) {
                imageCapture.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                            val bytes = imageProxyToJpeg(image)
                            image.close()
                            callback(bytes)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            callback(null)
                        }
                    }
                )
            }

            override fun rebind() {
                binder()
            }
        })
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

interface CaptureController {
    fun captureJpeg(callback: (result: ByteArray?) -> Unit)
    fun rebind()
}

private fun bindCameraUseCases(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    analyzer: ImageAnalysis.Analyzer,
    targetResolution: Size,
    cameraExecutor: ExecutorService,
    imageCapture: ImageCapture,
    onReady: (() -> Unit)? = null,
    onError: ((Throwable) -> Unit)? = null,
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        // Intentos en cascada: (FRONT, 16:9) -> (FRONT, 4:3) -> (BACK, 16:9) -> (BACK, 4:3)
        val selectors = listOf(
            CameraSelector.DEFAULT_FRONT_CAMERA,
            CameraSelector.DEFAULT_BACK_CAMERA
        )
        val aspects = listOf(AspectRatio.RATIO_16_9, AspectRatio.RATIO_4_3)

        var lastError: Exception? = null
        outer@ for (selector in selectors) {
            for (aspect in aspects) {
                try {
                    cameraProvider.unbindAll()

                    // Alternar ImplementationMode según intento: primero PERFORMANCE, luego COMPATIBLE
                    previewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE

                    val preview = Preview.Builder()
                        .setTargetAspectRatio(aspect)
                        .build()
                        .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                    val analysis = ImageAnalysis.Builder()
                        .setTargetAspectRatio(aspect)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { it.setAnalyzer(cameraExecutor, analyzer) }

                    // Reconfigurar también el ImageCapture al aspect actual
                    val captureBuilder = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetAspectRatio(aspect)
                        .build()

                    // actualizamos la instancia interna a través de reflexión simple: no es necesario, usamos referencia pasada
                    // bind con imageCapture pasado si se prefiere; aquí usamos el recibido para mantener control externo
                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture, analysis)

                    onReady?.invoke()
                    lastError = null
                    break@outer
                } catch (e: Exception) {
                    lastError = e
                    try {
                        // Segundo intento del mismo combo pero en modo COMPATIBLE
                        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE

                        val preview = Preview.Builder()
                            .setTargetAspectRatio(aspect)
                            .build()
                            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                        val analysis = ImageAnalysis.Builder()
                            .setTargetAspectRatio(aspect)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { it.setAnalyzer(cameraExecutor, analyzer) }

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture, analysis)

                        onReady?.invoke()
                        lastError = null
                        break@outer
                    } catch (e2: Exception) {
                        lastError = e2
                    }
                }
            }
        }

        if (lastError != null) {
            onError?.invoke(lastError!!)
        }
    }, ContextCompat.getMainExecutor(context))
}

private fun imageProxyToJpeg(imageProxy: androidx.camera.core.ImageProxy): ByteArray? {
    // ImageCapture suele entregar formato JPEG (1 plane). Si es así, devolvemos los bytes directos.
    return try {
        if (imageProxy.format == ImageFormat.JPEG || imageProxy.planes.size == 1) {
            val buffer = imageProxy.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            bytes
        } else {
            // Para YUV_420_888 convertimos a NV21 -> JPEG
            val bitmap = imageProxyToBitmap(imageProxy) ?: return null
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, baos)
            val bytes = baos.toByteArray()
            baos.close()
            bytes
        }
    } catch (t: Throwable) {
        null
    }
}

// Conversión simple YUV -> Bitmap, referencial para previsualización/captura
private fun imageProxyToBitmap(image: androidx.camera.core.ImageProxy): Bitmap? {
    return try {
        // Si ya viene en JPEG, decodificamos directo del primer plane
        if (image.format == ImageFormat.JPEG || image.planes.size == 1) {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }

        // Método para YUV_420_888: convertir a NV21 y luego a JPEG para decodificar
        val nv21 = yuv420888ToNv21(image) ?: return null
        val yuvImage = android.graphics.YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, out)
        val imageBytes = out.toByteArray()
        android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    } catch (t: Throwable) {
        null
    }
}

private fun yuv420888ToNv21(image: androidx.camera.core.ImageProxy): ByteArray? {
    val yBuffer = image.planes[0].buffer
    val uBuffer = image.planes[1].buffer
    val vBuffer = image.planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)
    yBuffer.get(nv21, 0, ySize)
    val chromaRowStride = image.planes[1].rowStride
    val chromaPixelStride = image.planes[1].pixelStride

    // Copia VU intercalado
    var offset = ySize
    val width = image.width
    val height = image.height
    val uvHeight = height / 2
    val vRowStride = image.planes[2].rowStride
    val vPixelStride = image.planes[2].pixelStride
    val uRowStride = image.planes[1].rowStride
    val uPixelStride = image.planes[1].pixelStride

    val vBufferArr = ByteArray(vSize)
    vBuffer.get(vBufferArr)
    val uBufferArr = ByteArray(uSize)
    uBuffer.get(uBufferArr)

    for (row in 0 until uvHeight) {
        val vRowStart = row * vRowStride
        val uRowStart = row * uRowStride
        for (col in 0 until width / 2) {
            val vIndex = vRowStart + col * vPixelStride
            val uIndex = uRowStart + col * uPixelStride
            nv21[offset++] = vBufferArr[vIndex]
            nv21[offset++] = uBufferArr[uIndex]
        }
    }
    return nv21
}
