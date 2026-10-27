package ayx.copy.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Groq text generation. No SDK, plain HttpURLConnection + org.json,
 * so no new Gradle dependency is needed.
 *
 * Triggers (typed at the END of any text field, in any app):
 *   ..ai  -> improve / better likho (same language, clearer)
 *   ..ea  -> English me kar do
 *   ..ra  -> Roman me kar do (Latin script, no Devanagari/Arabic)
 * The trigger itself is removed; the whole text before it is the input.
 */
object GroqClient {

    enum class Mode { IMPROVE, ENGLISH, ROMAN }

    data class AiRequest(val mode: Mode, val input: String)

    fun detectTrigger(text: String): AiRequest? {
        val trimmed = text.trimEnd()
        val lower = trimmed.lowercase()
        val mode = when {
            lower.endsWith("..ai") -> Mode.IMPROVE
            lower.endsWith("..ea") -> Mode.ENGLISH
            lower.endsWith("..ra") -> Mode.ROMAN
            else -> return null
        }
        val input = trimmed.dropLast(4).trim()
        if (input.isEmpty()) return null
        return AiRequest(mode, input)
    }

    private fun systemPrompt(mode: Mode): String = when (mode) {
        Mode.IMPROVE -> "Rewrite the text to be clearer, polished and natural. Keep the same language and the same meaning. Fix grammar and flow. Do not add any explanation, heading or quotes. Output ONLY the improved text."
        Mode.ENGLISH -> "Rewrite the text in clear, natural English, keeping the same meaning. Do not add any explanation, heading or quotes. Output ONLY the English text."
        Mode.ROMAN -> "Rewrite the text in Roman script only (Hinglish / Roman Urdu written in Latin letters, natural spoken style). Never use Devanagari or Arabic script. Keep the same meaning. Do not add any explanation, heading or quotes. Output ONLY the Roman text."
    }

    suspend fun generate(apiKey: String, mode: Mode, input: String): String {
        require(apiKey.isNotBlank()) { "Groq key missing" }
        return withContext(Dispatchers.IO) {
            try {
                call(apiKey, "llama-3.3-70b-versatile", mode, input)
            } catch (e: GroqHttpException) {
                // Model moved/renamed -> fast fallback model, same prompt.
                if (e.code == 400 || e.code == 404) call(apiKey, "llama-3.1-8b-instant", mode, input)
                else throw e
            }
        }
    }

    private class GroqHttpException(val code: Int, message: String) : Exception(message)

    private fun call(apiKey: String, model: String, mode: Mode, input: String): String {
        val body = JSONObject().apply {
            put("model", model)
            put("temperature", 0.6)
            put("max_tokens", 1200)
            val msgs = JSONArray()
            msgs.put(JSONObject().put("role", "system").put("content", systemPrompt(mode)))
            msgs.put(JSONObject().put("role", "user").put("content", input))
            put("messages", msgs)
        }
        val conn = (URL("https://api.groq.com/openai/v1/chat/completions").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 60000
            doOutput = true
            setRequestProperty("Authorization", "Bearer " + apiKey.trim())
            setRequestProperty("Content-Type", "application/json")
        }
        conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.use { BufferedReader(InputStreamReader(it)).readText() }.orEmpty()
        conn.disconnect()
        if (code !in 200..299) {
            val msg = try { JSONObject(text).optJSONObject("error")?.optString("message") } catch (e: Exception) { null }
            throw GroqHttpException(code, "Groq $code: ${msg ?: text.take(200)}")
        }
        val out = JSONObject(text)
            .getJSONArray("choices").getJSONObject(0)
            .getJSONObject("message").getString("content").trim()
        require(out.isNotEmpty()) { "Groq returned empty text" }
        return out
    }
}
