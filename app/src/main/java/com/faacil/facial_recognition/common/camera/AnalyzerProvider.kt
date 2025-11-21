package com.faacil.facial_recognition.common.camera

import androidx.camera.core.ImageAnalysis
import java.util.concurrent.ExecutorService

typealias AnalyzerProvider = (ExecutorService) -> ImageAnalysis.Analyzer
