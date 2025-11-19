package com.faacil.facial_recognition.common.antispoofing

import com.faacil.facial_recognition.common.ml.FaceFrame
import com.google.mlkit.vision.face.Face

/**
 * Procesa eventos de liveness: parpadeo y giro a izquierda/derecha.
 *
 * Reglas básicas:
 * - Parpadeo: ambos ojos pasan de
 *   a) abiertos a cerrados (< 0.35)
 *   b) vuelven a abrirse (> 0.6) en <= 2.5s
 *
 * - Giro izquierda: yaw (headEulerAngleY) <= -15° mantenido por >= 300ms.
 * - Giro derecha: yaw >= +15° mantenido por >= 300ms.
 *
 * Máquina de estados y uso:
 * - Orden guiado: Blink -> TurnLeft -> TurnRight -> Completed.
 * - Invocar [onFrame] con cada frame de [FaceFrame].
 * - Usar el valor retornado [State] para actualizar la UI (prompt y barra de progreso).
 *
 * Consideraciones:
 * - Si un frame no contiene rostro, el progreso se mantiene.
 * - Los umbrales son conservadores para dispositivos con probabilidades de ojo ruidosas.
 */
class LivenessProcessor(
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) {
    data class State(
        val currentStep: Step,
        val blinkDone: Boolean,
        val leftDone: Boolean,
        val rightDone: Boolean,
        val facePresent: Boolean,
        val yaw: Float = 0f,
    ) {
        val progress: Float = listOf(blinkDone, leftDone, rightDone).count { it }.toFloat() / 3f
        val completed: Boolean = blinkDone && leftDone && rightDone
    }

    // Determina el estado de parpadeo
    private var blinkPhase: BlinkPhase = BlinkPhase.WaitingOpen
    private var blinkStartTs: Long = 0L
    private var lastClosedTs: Long = 0L
    private var blinkDone: Boolean = false

    // Determina los movimientos de cabeza
    private var leftSince: Long? = null
    private var rightSince: Long? = null
    private var leftDone: Boolean = false
    private var rightDone: Boolean = false

    // Estado actual para la UI
    enum class Step { Blink, TurnLeft, TurnRight, Completed }

    // Estado actual del parpadero
    private enum class BlinkPhase { WaitingOpen, SeenOpen, SeenClosed }

    /**
     * Resetea el estado interno del procesador de liveness.
     */
    fun reset() {
        blinkPhase = BlinkPhase.WaitingOpen
        blinkStartTs = 0L
        lastClosedTs = 0L
        blinkDone = false
        leftSince = null
        rightSince = null
        leftDone = false
        rightDone = false
    }

    /**
     * Procesa un nuevo frame y devuelve el estado actualizado del flujo de liveness.
     * Si no se detecta rostro en el frame, no se pierde el progreso previo.
     */
    fun onFrame(frame: FaceFrame): State {
        val face = frame.faces.firstOrNull()
        val ts = timeProvider()

        // Valida si no hay rostro en el frame actual
        if (face == null) {
            // Restaura el estado
            reset()

            return buildState(null)
        }

        processBlink(face, ts)
        processYaw(face, ts)

        return buildState(face)
    }

    /**
     * Detecta parpadeo válido usando las probabilidades de ojos abiertos provistas por ML Kit.
     * Exige una secuencia Open -> Closed -> Open dentro de una ventana temporal razonable.
     */
    private fun processBlink(face: Face, ts: Long) {
        if (blinkDone) return

        val leftEye = face.leftEyeOpenProbability ?: return
        val rightEye = face.rightEyeOpenProbability ?: return

        // Probabilidad media de ojo abierto
        val open = (leftEye + rightEye) / 2f

        when (blinkPhase) {
            BlinkPhase.WaitingOpen -> {
                if (open > 0.6f) blinkPhase = BlinkPhase.SeenOpen
            }

            BlinkPhase.SeenOpen -> {
                if (open < 0.35f) {
                    blinkPhase = BlinkPhase.SeenClosed
                    blinkStartTs = ts
                    lastClosedTs = ts
                }
            }

            BlinkPhase.SeenClosed -> {
                if (open < 0.35f) {
                    lastClosedTs = ts
                } else if (open > 0.6f) {
                    // Reabrió ojos, validar ventana temporal
                    val duration = ts - blinkStartTs

                    if (duration in 80..2500) {
                        blinkDone = true
                    } else {
                        // Reiniciar si fue muy largo/lento
                        blinkPhase = BlinkPhase.WaitingOpen
                    }
                }
            }
        }
    }

    /**
     * Evalúa giros de cabeza a izquierda y derecha a partir del yaw (headEulerAngleY).
     * Requiere mantener el umbral por un tiempo mínimo ([holdMs]).
     */
    private fun processYaw(face: Face, ts: Long) {
        val yaw = face.headEulerAngleY // negativo izquierda, positivo derecha
        val threshold = 15f
        val holdMs = 300L

        if (!leftDone) {
            if (yaw <= -threshold) {
                if (leftSince == null) {
                    leftSince = ts
                }

                if (ts - (leftSince ?: ts) >= holdMs) {
                    leftDone = true
                }
            } else {
                leftSince = null
            }
        }

        if (!rightDone) {
            if (yaw >= threshold) {
                if (rightSince == null) {
                    rightSince = ts
                }

                if (ts - (rightSince ?: ts) >= holdMs) {
                    rightDone = true
                }
            } else {
                rightSince = null
            }
        }
    }

    /**
     * Construye el [State] expuesto a la UI con el siguiente paso e información auxiliar.
     */
    private fun buildState(face: Face?): State {
        val yaw = face?.headEulerAngleY ?: 0f
        val next = when {
            !blinkDone -> Step.Blink
            !leftDone -> Step.TurnLeft
            !rightDone -> Step.TurnRight
            else -> Step.Completed
        }

        return State(
            currentStep = next,
            blinkDone = blinkDone,
            leftDone = leftDone,
            rightDone = rightDone,
            facePresent = face != null,
            yaw = yaw
        )
    }
}
