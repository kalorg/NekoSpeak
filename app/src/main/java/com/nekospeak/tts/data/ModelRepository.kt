package com.nekospeak.tts.data

import android.content.Context
import android.os.StatFs
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class ModelFile(
    val fileName: String,
    val downloadUrl: String,
    val description: String
)

data class ModelInfo(
    val id: String,
    val name: String,
    val description: String,
    val files: List<ModelFile>,
    val version: String = "1.0",
    /** Taille totale approximative du téléchargement, pour le contrôle d'espace disque. */
    val requiredBytes: Long = 0L
)

object ModelRepository {
    private const val TAG = "ModelRepository"

    private const val HF_BASE = "https://huggingface.co/KevinAHM/pocket-tts-onnx/resolve/main"
    private const val BUNDLE = "onnx/french_24l"
    private const val VOICES_BASE = "https://huggingface.co/kyutai/tts-voices/resolve/main"

    val models = listOf(
        ModelInfo(
            id = "pocket_v1",
            name = "Pocket-TTS (Experimental)",
            description = "Required for voice cloning. Includes encoder, decoder, and flow matching models (~200MB total).",
            files = listOf(
                ModelFile("pocket/models/mimi_encoder_int8.onnx", "$HF_BASE/$BUNDLE/mimi_encoder_int8.onnx", "Mimi Encoder INT8 (21MB)"),
                ModelFile("pocket/models/text_conditioner_int8.onnx", "$HF_BASE/$BUNDLE/text_conditioner_int8.onnx", "Text Conditioner INT8 (16MB)"),
                ModelFile("pocket/models/flow_lm_main_int8.onnx", "$HF_BASE/$BUNDLE/flow_lm_main_int8.onnx", "Flow LM Main INT8 (76MB)"),
                ModelFile("pocket/models/flow_lm_flow_int8.onnx", "$HF_BASE/$BUNDLE/flow_lm_flow_int8.onnx", "Flow LM Flow INT8 (10MB)"),
                ModelFile("pocket/models/mimi_decoder_int8.onnx", "$HF_BASE/$BUNDLE/mimi_decoder_int8.onnx", "Mimi Decoder INT8 (23MB)"),
                ModelFile("pocket/tokenizer.model", "$HF_BASE/$BUNDLE/tokenizer.model", "Tokenizer (59KB)"),
                ModelFile("pocket/bos_before_voice.npy", "$HF_BASE/$BUNDLE/bos_before_voice.npy", "Voice BOS embedding"),
                // Voix françaises (corpus Expresso, locuteurs natifs FR, licence CC BY-NC)
                ModelFile("pocket/voices/narrateur.wav", "$VOICES_BASE/expresso/ex03-ex02_narration_001_channel1_674s.wav", "Le Narrateur (narration grave)"),
                ModelFile("pocket/voices/conteuse.wav", "$VOICES_BASE/expresso/ex04-ex01_narration_001_channel1_605s.wav", "La Conteuse (narration féminine)"),
                ModelFile("pocket/voices/murmure.wav", "$VOICES_BASE/expresso/ex01-ex02_whisper_001_channel1_579s.wav", "Le Murmure (chuchotement grave)"),
                ModelFile("pocket/voices/chuchoteuse.wav", "$VOICES_BASE/expresso/ex04-ex03_whisper_001_channel1_198s.wav", "La Chuchoteuse (chuchotement)"),
                ModelFile("pocket/voices/effroi.wav", "$VOICES_BASE/expresso/ex03-ex01_awe_001_channel1_1323s.wav", "L'Effroi (voix grave, crainte)"),
                ModelFile("pocket/voices/frayeur.wav", "$VOICES_BASE/expresso/ex04-ex02_fearful_001_channel1_316s.wav", "La Frayeur (peur, tension)"),
                ModelFile("pocket/voices/douleur.wav", "$VOICES_BASE/expresso/ex03-ex02_sad-sympathetic_001_channel1_454s.wav", "La Douleur (mélancolie)"),
                ModelFile("pocket/voices/sangfroid.wav", "$VOICES_BASE/expresso/ex03-ex01_calm_001_channel1_1143s.wav", "Le Sang-froid (calme)"),
                ModelFile("pocket/voices/apaisee.wav", "$VOICES_BASE/expresso/ex04-ex02_calm_002_channel1_480s.wav", "L'Apaisée (calme)"),
                ModelFile("pocket/voices/colere.wav", "$VOICES_BASE/expresso/ex03-ex01_angry_001_channel1_201s.wav", "La Colère (rage contenue)"),
                ModelFile("pocket/voices/defi.wav", "$VOICES_BASE/expresso/ex04-ex02_angry_001_channel1_119s.wav", "Le Défi (colère féminine)"),
                ModelFile("pocket/voices/naturel_m.wav", "$VOICES_BASE/expresso/ex01-ex02_default_001_channel1_168s.wav", "Dialogues naturels (homme)"),
                ModelFile("pocket/voices/naturel_f.wav", "$VOICES_BASE/expresso/ex04-ex03_default_002_channel2_239s.wav", "Dialogues naturels (femme)")
            ),
            requiredBytes = 300_000_000L // ~300 Mo
        ),
        ModelInfo(
            id = "kokoro_v1.0",
            name = "Kokoro v1.0",
            description = "High quality, expressive standard voices.",
            files = listOf(
                ModelFile("kokoro-v1.0.int8.onnx", "https://github.com/siva-sub/NekoSpeak/releases/download/v1.0.0/kokoro-v1.0.int8.onnx", "Model Weights"),
                ModelFile("voices-v1.0.bin", "https://github.com/siva-sub/NekoSpeak/releases/download/v1.0.0/voices-v1.0.bin", "Voice Pack")
            ),
            requiredBytes = 200_000_000L // ~200 Mo
        ),
        ModelInfo(
            id = "kitten_nano",
            name = "Kitten TTS Nano",
            description = "Lightning fast, low latency model.",
            files = listOf(
                ModelFile("kitten_tts_nano_v0_1.onnx", "https://github.com/siva-sub/NekoSpeak/releases/download/v1.0.0/kitten_tts_nano_v0_1.onnx", "Model Weights"),
                ModelFile("voices.npz", "https://github.com/siva-sub/NekoSpeak/releases/download/v1.0.0/voices.npz", "Voice Pack")
            ),
            requiredBytes = 150_000_000L // ~150 Mo
        ),
        ModelInfo(
            id = "omnivoice",
            name = "OmniVoice (Diffusion TTS)",
            description = "600+ language multilingual TTS with voice cloning. 2.3GB main model + 83MB decoder. Optional 624MB encoder for voice cloning. ~3GB total download.",
            files = listOf(
                ModelFile("omnivoice/omnivoice-main-split.onnx", "https://huggingface.co/Gigsu/vocoloco-onnx/resolve/main/omnivoice-main-split.onnx", "Main Model Graph (1MB)"),
                ModelFile("omnivoice/omnivoice-main.onnx_data_00", "https://huggingface.co/Gigsu/vocoloco-onnx/resolve/main/omnivoice-main.onnx_data_00", "Main Model Shard 1/5 (592MB)"),
                ModelFile("omnivoice/omnivoice-main.onnx_data_01", "https://huggingface.co/Gigsu/vocoloco-onnx/resolve/main/omnivoice-main.onnx_data_01", "Main Model Shard 2/5 (488MB)"),
                ModelFile("omnivoice/omnivoice-main.onnx_data_02", "https://huggingface.co/Gigsu/vocoloco-onnx/resolve/main/omnivoice-main.onnx_data_02", "Main Model Shard 3/5 (492MB)"),
                ModelFile("omnivoice/omnivoice-main.onnx_data_03", "https://huggingface.co/Gigsu/vocoloco-onnx/resolve/main/omnivoice-main.onnx_data_03", "Main Model Shard 4/5 (500MB)"),
                ModelFile("omnivoice/omnivoice-main.onnx_data_04", "https://huggingface.co/Gigsu/vocoloco-onnx/resolve/main/omnivoice-main.onnx_data_04", "Main Model Shard 5/5 (264MB)"),
                ModelFile("omnivoice/omnivoice-decoder.onnx", "https://huggingface.co/Gigsu/vocoloco-onnx/resolve/main/omnivoice-decoder.onnx", "Audio Decoder (83MB)"),
                ModelFile("omnivoice/omnivoice-encoder-fixed.onnx", "https://huggingface.co/Gigsu/vocoloco-onnx/resolve/main/omnivoice-encoder-fixed.onnx", "Audio Encoder - Voice Cloning (624MB)"),
                ModelFile("omnivoice/omnivoice-config.json", "https://huggingface.co/Gigsu/vocoloco-onnx/resolve/main/omnivoice-config.json", "Model Config"),
                ModelFile("omnivoice/omnivoice-main-manifest.json", "https://huggingface.co/Gigsu/vocoloco-onnx/resolve/main/omnivoice-main-manifest.json", "Shard Manifest")
            ),
            requiredBytes = 3_300_000_000L // ~3,3 Go (shards + decoder + encoder 624 Mo)
        )
    )

    private val _downloadStates = mutableMapOf<String, MutableStateFlow<Float>>()

    // Permet à l'UI d'observer le dernier message d'erreur détaillé
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    fun getDownloadProgress(modelId: String): StateFlow<Float>? {
        return _downloadStates[modelId]?.asStateFlow()
    }

    fun isInstalled(context: Context, modelId: String): Boolean {
        val model = models.find { it.id == modelId } ?: return false
        return model.files.all { fileDef ->
            if (modelId == "omnivoice" && fileDef.fileName.contains("encoder")) return@all true
            val file = File(context.filesDir, fileDef.fileName)
            file.exists() && file.length() > 1024
        }
    }

    /**
     * Écrit les erreurs réseau dans un fichier persistant (files/download_errors.log)
     * pour pouvoir les récupérer même sans ADB ni Logcat.
     */
    private fun logErrorToFile(context: Context, message: String) {
        try {
            val logFile = File(context.filesDir, "download_errors.log")
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            FileOutputStream(logFile, true).use { output ->
                output.write("[$timestamp] $message\n".toByteArray(Charsets.UTF_8))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Impossible d'écrire dans download_errors.log", e)
        }
    }

    suspend fun downloadModel(context: Context, modelId: String, onComplete: (Boolean) -> Unit) = withContext(Dispatchers.IO) {
        val model = models.find { it.id == modelId } ?: return@withContext

        if (_downloadStates.containsKey(modelId)) return@withContext // Already downloading

        val progressFlow = MutableStateFlow(0f)
        _downloadStates[modelId] = progressFlow

        try {
            // Contrôle préalable de l'espace disque interne.
            // Cause fréquente d'échec "aléatoire" en cours de téléchargement (ENOSPC),
            // en particulier pour OmniVoice (~3,3 Go).
            if (model.requiredBytes > 0L) {
                val stat = StatFs(context.filesDir.absolutePath)
                val alreadyOnDisk = model.files
                    .map { File(context.filesDir, it.fileName) }
                    .filter { it.exists() && it.length() > 1024 }
                    .sumOf { it.length() }
                val remainingBytes = (model.requiredBytes - alreadyOnDisk).coerceAtLeast(0L)
                if (stat.availableBytes < remainingBytes) {
                    val requiredMb = remainingBytes / (1024L * 1024L)
                    val freeMb = stat.availableBytes / (1024L * 1024L)
                    val msg = "Espace insuffisant sur le stockage interne : " +
                        "$requiredMb Mo nécessaires, seulement $freeMb Mo libres. " +
                        "Libérez de l'espace (ou supprimez d'autres modèles dans le Gestionnaire de modèles) puis réessayez."
                    Log.e(TAG, msg)
                    _lastError.value = msg
                    logErrorToFile(context, msg)
                    withContext(Dispatchers.Main) { onComplete(false) }
                    return@withContext
                }
            }

            val totalFiles = model.files.size
            var filesDownloaded = 0

            for (fileDef in model.files) {
                val targetFile = File(context.filesDir, fileDef.fileName)

                // Skip if already downloaded and valid
                if (targetFile.exists() && targetFile.length() > 1024) {
                    Log.d(TAG, "File already exists: ${fileDef.fileName}")
                    filesDownloaded++
                    progressFlow.value = filesDownloaded.toFloat() / totalFiles
                    continue
                }

                // Create parent directories
                targetFile.parentFile?.mkdirs()

                Log.i(TAG, "Downloading: ${fileDef.downloadUrl}")

                val success = downloadFile(fileDef.downloadUrl, targetFile, { fileProgress ->
                    val totalProgress = (filesDownloaded + fileProgress) / totalFiles
                    progressFlow.value = totalProgress
                }) { errorMessage ->
                    // Trace l'URL exacte en échec dans download_errors.log et pour l'UI
                    val msg = "Échec du fichier: ${fileDef.fileName}\nURL: ${fileDef.downloadUrl}\n$errorMessage"
                    _lastError.value = msg
                    logErrorToFile(context, msg)
                }

                if (!success) {
                    Log.e(TAG, "Failed to download: ${fileDef.fileName}")
                    withContext(Dispatchers.Main) { onComplete(false) }
                    return@withContext
                }

                filesDownloaded++
                Log.i(TAG, "Downloaded: ${fileDef.fileName} (${targetFile.length() / 1024}KB)")
            }

            Log.i(TAG, "Model $modelId download complete")
            _lastError.value = null
            withContext(Dispatchers.Main) { onComplete(true) }

        } catch (e: Exception) {
            Log.e(TAG, "Download failed for $modelId", e)
            e.printStackTrace()
            val msg = "Erreur inattendue pour $modelId: ${e.message}"
            _lastError.value = msg
            logErrorToFile(context, msg)
            withContext(Dispatchers.Main) { onComplete(false) }
        } finally {
            _downloadStates.remove(modelId)
        }
    }

    private fun downloadFile(urlString: String, targetFile: File, onProgress: (Float) -> Unit, onError: (String) -> Unit): Boolean {
        var connection: HttpURLConnection? = null
        try {
            var currentUrl = urlString
            var redirectCount = 0
            val maxRedirects = 5

            // Handle redirects manually (HuggingFace uses them for LFS files)
            while (redirectCount < maxRedirects) {
                val url = URL(currentUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 30000
                connection.readTimeout = 120000  // 2 min timeout for large files
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "NekoSpeak-TTS/1.0")
                connection.instanceFollowRedirects = false  // Handle redirects manually

                val responseCode = connection.responseCode
                Log.d(TAG, "Response code for $currentUrl: $responseCode")

                when (responseCode) {
                    HttpURLConnection.HTTP_OK -> break  // Success, proceed to download
                    HttpURLConnection.HTTP_MOVED_TEMP, HttpURLConnection.HTTP_MOVED_PERM,
                    HttpURLConnection.HTTP_SEE_OTHER, 307, 308 -> {
                        // Follow redirect
                        val newUrl = connection.getHeaderField("Location")
                        if (newUrl == null) {
                            Log.e(TAG, "Redirect without Location header")
                            onError("Redirection sans en-tête Location pour: $currentUrl")
                            return false
                        }
                        // Sécurité: n'accepter que les redirections http(s)
                        if (!newUrl.startsWith("http")) {
                            Log.e(TAG, "Refusing redirect with non-http scheme: $newUrl")
                            onError("Redirection refusée (schéma non http): $newUrl")
                            return false
                        }
                        Log.d(TAG, "Redirecting to: $newUrl")
                        currentUrl = newUrl
                        connection.disconnect()
                        redirectCount++
                    }
                    else -> {
                        Log.e(TAG, "HTTP error: $responseCode for $urlString")
                        onError("Erreur HTTP $responseCode pour: $urlString")
                        return false
                    }
                }
            }

            if (redirectCount >= maxRedirects) {
                Log.e(TAG, "Too many redirects for $urlString")
                onError("Trop de redirections pour: $urlString")
                return false
            }

            val contentLength = connection!!.contentLengthLong
            Log.d(TAG, "Content length: $contentLength bytes")

            var bytesDownloaded = 0L

            connection.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead

                        if (contentLength > 0) {
                            onProgress(bytesDownloaded.toFloat() / contentLength)
                        }
                    }
                }
            }

            Log.i(TAG, "Downloaded ${bytesDownloaded / 1024}KB to ${targetFile.name}")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Download error: ${e.message}", e)
            onError("Exception: ${e.message}")
            targetFile.delete() // Clean up partial download
            return false
        } finally {
            connection?.disconnect()
        }
    }

    fun deleteModel(context: Context, modelId: String) {
        val model = models.find { it.id == modelId } ?: return
        model.files.forEach { fileDef ->
             val file = File(context.filesDir, fileDef.fileName)
             if (file.exists()) file.delete()
        }
    }
}
