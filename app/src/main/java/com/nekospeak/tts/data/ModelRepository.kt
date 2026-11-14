package com.nekospeak.tts.data

import android.content.Context
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
    val version: String = "1.0"
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
                ModelFile("pocket/voices/alba.wav", "$VOICES_BASE/alba-mackenna/casual.wav", "Alba Voice"),
                ModelFile("pocket/voices/marius.wav", "$VOICES_BASE/voice-donations/Selfie.wav", "Marius Voice"),
                ModelFile("pocket/voices/javert.wav", "$VOICES_BASE/voice-donations/Butter.wav", "Javert Voice"),
                ModelFile("pocket/voices/jean.wav", "$VOICES_BASE/ears/p010/freeform_speech_01.wav", "Jean Voice"),
                ModelFile("pocket/voices/fantine.wav", "$VOICES_BASE/vctk/p244_023.wav", "Fantine Voice"),
                ModelFile("pocket/voices/cosette.wav", "$VOICES_BASE/expresso/ex04-ex02_confused_001_channel1_499s.wav", "Cosette Voice"),
                ModelFile("pocket/voices/eponine.wav", "$VOICES_BASE/vctk/p262_023.wav", "Eponine Voice"),
                ModelFile("pocket/voices/azelma.wav", "$VOICES_BASE/vctk/p303_023.wav", "Azelma Voice")
            )
        ),
        ModelInfo(
            id = "kokoro_v1.0",
            name = "Kokoro v1.0",
            description = "High quality, expressive standard voices.",
            files = listOf(
                ModelFile("kokoro-v1.0.int8.onnx", "https://github.com/siva-sub/NekoSpeak/releases/download/v1.0.0/kokoro-v1.0.int8.onnx", "Model Weights"),
                ModelFile("voices-v1.0.bin", "https://github.com/siva-sub/NekoSpeak/releases/download/v1.0.0/voices-v1.0.bin", "Voice Pack")
            )
        ),
        ModelInfo(
            id = "kitten_nano",
            name = "Kitten TTS Nano",
            description = "Lightning fast, low latency model.",
            files = listOf(
                ModelFile("kitten_tts_nano_v0_1.onnx", "https://github.com/siva-sub/NekoSpeak/releases/download/v1.0.0/kitten_tts_nano_v0_1.onnx", "Model Weights"),
                ModelFile("voices.npz", "https://github.com/siva-sub/NekoSpeak/releases/download/v1.0.0/voices.npz", "Voice Pack")
            )
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
            )
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
