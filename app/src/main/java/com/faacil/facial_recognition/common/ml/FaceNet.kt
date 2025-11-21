package com.faacil.facial_recognition.common.ml

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorOperator
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.support.tensorbuffer.TensorBufferFloat
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Clase que implementa FaceNet, un modelo de red neuronal para el reconocimiento facial.
 *
 * @param context El contexto de la aplicación.
 * @param useGpu Indica si se debe utilizar la GPU para la inferencia.
 * @param useXNNPack Indica si se debe utilizar el delegado XNNPack de TensorFlow Lite.
 */
class FaceNet(
    context: Context,
    useGpu: Boolean = true,
    useXNNPack: Boolean = true,
) {
    // Tamaño de la imagen de entrada para el modelo FaceNet.
    private val imgSize = 160

    // Tamaño del embedding de salida.
    // Usamos 512 dimensiones (Inception ResNet v1) para mayor precisión.
    private val embeddingDim = 512

    private var interpreter: Interpreter
    private val imageTensorProcessor =
        ImageProcessor
            .Builder()
            .add(ResizeOp(imgSize, imgSize, ResizeOp.ResizeMethod.BILINEAR))
            .add(StandardizeOp())
            .build()

    init {
        // Inicializa el intérprete de TFLite.
        val interpreterOptions = Interpreter.Options().apply {
            // Agrega el delegado de GPU si es compatible.
            // Ver -> https://www.tensorflow.org/lite/performance/gpu#android
            if (useGpu) {
                if (CompatibilityList().isDelegateSupportedOnThisDevice) {
                    addDelegate(GpuDelegate(CompatibilityList().bestOptionsForThisDevice))
                }
            } else {
                // Número de hilos para el cómputo.
                numThreads = 4
            }
            useXNNPACK = useXNNPack
            useNNAPI = true
        }

        // Usamos facenet_512.tflite para alta precisión.
        interpreter =
            Interpreter(FileUtil.loadMappedFile(context, "facenet_512.tflite"), interpreterOptions)
    }

    /**
     * Obtiene un embedding facial utilizando FaceNet.
     *
     * @param image El bitmap de la imagen facial.
     * @return Un array de floats que representa el embedding facial.
     */
    suspend fun getFaceEmbedding(image: Bitmap) =
        withContext(Dispatchers.Default) {
            return@withContext runFaceNet(convertBitmapToBuffer(image))[0]
        }

    /**
     * Ejecuta el modelo FaceNet.
     *
     * @param inputs Los datos de entrada para el modelo.
     * @return Un array de arrays de floats con los resultados del modelo.
     */
    private fun runFaceNet(inputs: Any): Array<FloatArray> {
        val faceNetModelOutputs = Array(1) { FloatArray(embeddingDim) }
        interpreter.run(inputs, faceNetModelOutputs)
        return faceNetModelOutputs
    }

    /**
     * Redimensiona el bitmap dado y lo convierte a un ByteBuffer.
     *
     * @param image El bitmap a convertir.
     * @return Un ByteBuffer con los datos de la imagen.
     */
    private fun convertBitmapToBuffer(image: Bitmap): ByteBuffer =
        imageTensorProcessor.process(TensorImage.fromBitmap(image)).buffer

    /**
     * Operación para realizar la estandarización de la imagen.
     * x' = ( x - media ) / desviación_estándar
     */
    class StandardizeOp : TensorOperator {
        override fun apply(p0: TensorBuffer?): TensorBuffer {
            val pixels = p0!!.floatArray
            val mean = pixels.average().toFloat()
            var std = sqrt(pixels.map { pi -> (pi - mean).pow(2) }.sum() / pixels.size.toFloat())
            std = max(std, 1f / sqrt(pixels.size.toFloat()))

            for (i in pixels.indices) {
                pixels[i] = (pixels[i] - mean) / std
            }

            val output = TensorBufferFloat.createFixedSize(p0.shape, DataType.FLOAT32)
            output.loadArray(pixels)

            return output
        }
    }
}


