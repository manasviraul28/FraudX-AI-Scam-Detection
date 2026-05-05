package com.fraudx.app

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class FraudClassifier(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var vocab: Map<String, Int> = emptyMap()
    private val maxLen = 100

    init {
        try {
            interpreter = Interpreter(loadModelFile())
            vocab = loadVocab()
        } catch (e: Exception) {
            interpreter = null
        }
    }

    private fun loadModelFile(): ByteBuffer {
        val assetFd = context.assets.openFd("fraud_model.tflite")
        val inputStream = FileInputStream(assetFd.fileDescriptor)
        return inputStream.channel.map(
            FileChannel.MapMode.READ_ONLY,
            assetFd.startOffset,
            assetFd.declaredLength
        )
    }

    private fun loadVocab(): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        try {
            context.assets.open("vocab.txt").bufferedReader().forEachLine { line ->
                val parts = line.split("\t")
                if (parts.size == 2) map[parts[0]] = parts[1].toIntOrNull() ?: 0
            }
        } catch (e: Exception) { }
        return map
    }

    fun classifyText(text: String): Float {
        interpreter?.let {
            try {
                val tokens = text.lowercase().split(" ")
                    .mapNotNull { word -> vocab[word] }
                val padded = IntArray(maxLen) { i -> if (i < tokens.size) tokens[i] else 0 }
                val inputBuffer = ByteBuffer.allocateDirect(4 * maxLen).apply {
                    order(ByteOrder.nativeOrder())
                    padded.forEach { v -> putInt(v) }
                    rewind()
                }
                val outputBuffer = ByteBuffer.allocateDirect(4).apply {
                    order(ByteOrder.nativeOrder())
                }
                it.run(inputBuffer, outputBuffer)
                outputBuffer.rewind()
                return outputBuffer.float
            } catch (e: Exception) { }
        }
        return when {
            RiskEngine.calculateRisk(text).contains("HIGH")   -> 0.92f
            RiskEngine.calculateRisk(text).contains("MEDIUM") -> 0.55f
            else -> 0.1f
        }
    }

    fun getRiskLabel(text: String): String {
        val score = classifyText(text)
        return when {
            score >= 0.75f -> "HIGH RISK — Scam Detected (${(score * 100).toInt()}%)"
            score >= 0.45f -> "MEDIUM RISK — Suspicious (${(score * 100).toInt()}%)"
            else           -> "LOW RISK — Looks Safe (${(score * 100).toInt()}%)"
        }
    }

    fun isHighRisk(text: String) = classifyText(text) >= 0.75f
}