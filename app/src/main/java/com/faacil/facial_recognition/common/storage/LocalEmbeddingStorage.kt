package com.faacil.facial_recognition.common.storage

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

/**
 * Clase para almacenar y recuperar embeddings faciales localmente usando SharedPreferences.
 *
 * @param context El contexto de la aplicación.
 */
class LocalEmbeddingStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("face_prefs", Context.MODE_PRIVATE)

    /**
     * Guarda un embedding facial en SharedPreferences.
     * El embedding se convierte a una cadena JSON antes de guardarlo.
     *
     * @param embedding La lista de floats que representa el embedding facial.
     */
    fun saveEmbedding(embedding: List<Float>) {
        val jsonArray = JSONArray()
        embedding.forEach { jsonArray.put(it) }
        prefs.edit().putString("local_embedding", jsonArray.toString()).apply()
    }

    /**
     * Recupera el embedding facial guardado de SharedPreferences.
     * La cadena JSON se convierte de nuevo a una lista de floats.
     *
     * @return La lista de floats que representa el embedding facial, o null si no se encuentra.
     */
    fun getEmbedding(): List<Float>? {
        val jsonString = prefs.getString("local_embedding", null) ?: return null
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<Float>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getDouble(i).toFloat())
            }
            list
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Elimina el embedding facial guardado de SharedPreferences.
     */
    fun clear() {
        prefs.edit().remove("local_embedding").apply()
    }
}
