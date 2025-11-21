package com.faacil.facial_recognition.common.camera

import android.graphics.Bitmap

interface CaptureController {
    fun captureBitmap(callback: (result: Bitmap?) -> Unit)
    fun rebind()
}