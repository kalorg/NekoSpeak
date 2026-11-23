package com.nekospeak.tts.data

import android.content.Context
import android.os.StatFs
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Voix fine-tunée hébergée sur le repo HuggingFace privé (un dossier par voix).
 *
 * Structure côté repo (pruvost/narrateur-vocoloco) :
 *
 *   narrateur/
 *     voice.json   ← manifest de métadonnées (voir ci-dessous)
 *     omnivoice-main-split.onnx   ~1 Mo  (graphe ONNX)
 *     omnivoice-main.onnx_data    ~2,4 Go (poids externes fp32)
 *     ref.wav                      ~10 s (référence de clonage, 24 kHz mono)
 *
 * voice.json — tous les champs sont optionnels sauf id :
 * {
 *   "id": "narrateur",
 *   "name": "Le Narrateur",
 *   "description": "Voix narrateur dark fantasy (LoRA FR)",
 *   "language": "fr",
 *   "gender": "Male",
 *   "refText": "<transcription exacte de ref.wav>",
 *   "files": { "graph": "...", "data": "...", "refAudio": "..." },
 *   "dataMinBytes": 1000000000
 * }
 *
 * Ajouter une voix côté serveur = uploader un nouveau dossier (avec son
 * voice.json) : aucune modification de l'application n'est nécessaire.
 */
data class HfVoice(
    val id: String,
    val name: String,
    val description: String,
    val language: String,
    val gender: String,
    val refText: String?,
    val dir: String,               // chemin du dossier dans le repo ("narrateur")
    val graphFile: String,
    val dataFile: String,
    val refAudioFile: String,
    val dataMinBytes: Long
)

/**
 * Catalogue des voix du repo HF privé : découverte (API tree + voice.json),
 * téléchargement par voix, suivi de progression, suppression, migration de
 * l'installation « narrateur » antérieure (backbone en racine de omnivoice/).
 */
object HfVoiceCatalog {
    private const val TAG = "HfVoiceCatalog"

    /** Repo HF privé hébergeant les voix (un dossier par voix). */
    const val HF_REPO_ID = "pruvost/narrateur-vocoloco"
    private const val HF_API_TREE = "https://huggingface.co/api/models/$HF_REPO_ID/tree/main"
    private const val HF_RESOLVE = "https://huggingface.co/$HF_REPO_ID/resolve/main"

    private const val VOICES_DIR = "hf_voices"
    private const val CATALOG_CACHE = "hf_catalog_cache.json"

    // Noms de fichiers par défaut si voice.json ne les précise pas
    const val DEFAULT_GRAPH = "omnivoice-main-split.onnx"
    const val DEFAULT_DATA = "omnivoice-main.onnx_data"
    const val DEFAULT_REF = "ref.wav"
    const val DEFAULT_DATA_MIN_BYTES = 1_000_000_000L

    private val gson = Gson()

    /** Progression des téléchargements actifs : id voix → fraction [0..1]. */
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    // --- voice.json ---

    private class VoiceManifest {
        var id: String? = null
        var name: String? = null
        var description: String? = null
        var language: String? = null
        var gender: String? = null
        var refText: String? = null
        var files: Map<String, String>? = null
        var dataMinBytes: Long? = null
    }

    private fun parseManifest(json: String, dir: String): HfVoice? {
        return try {
            val m = gson.fromJson(json, VoiceManifest::class.java)
            val id = (m.id ?: dir.substringAfterLast('/')).trim()
            if (id.isBlank()) return null
            HfVoice(
                id = id,
                name = (m.name ?: id).trim(),
                description = m.description ?: "",
                language = m.language ?: "fr",
                gender = m.gender ?: "Male",
                refText = m.refText,
                dir = dir,
                graphFile = m.files?.get("graph") ?: DEFAULT_GRAPH,
                dataFile = m.files?.get("data") ?: DEFAULT_DATA,
                refAudioFile = m.files?.get("refAudio") ?: DEFAULT_REF,
                dataMinBytes = m.dataMinBytes ?: DEFAULT_DATA_MIN_BYTES
            )
        } catch (e: Exception) {
            Log.w(TAG, "voice.json invalide dans '$dir' : ${e.message}")
            null
        }
    }

    // --- Chemins locaux ---

    fun voicesRoot(context: Context): File = File(context.filesDir, VOICES_DIR)

    fun voiceDir(context: Context, voiceId: String): File = File(voicesRoot(context), voiceId)

    /** Une voix locale est complète si graphe + poids + référence sont présents. */
    fun isLocalVoiceComplete(dir: File, voice: HfVoice): Boolean {
        val graph = File(dir, voice.graphFile)
        val data = File(dir, voice.dataFile)
        val ref = File(dir, voice.refAudioFile)
        // >= pour le data : les manifests HF peuvent poser dataMinBytes = taille
        // exacte du fichier (le notebook l'écrit ainsi), et une taille égale
        // signifie que le téléchargement est complet.
        return graph.length() > 1024 &&
               data.length() >= voice.dataMinBytes &&
               ref.length() > 1024
    }

    /**
     * Voix installées : scan des dossiers locaux hf_voices (un par voix)
     * avec voice.json valide et fichiers complets. Fonctionne hors ligne.
     */
    fun installedVoices(context: Context): List<HfVoice> {
        val root = voicesRoot(context)
        val baseDirs = root.listFiles()?.filter { it.isDirectory } ?: return emptyList()
        val result = mutableListOf<HfVoice>()
        for (dir in baseDirs) {
            val manifestFile = File(dir, "voice.json")
            if (!manifestFile.exists() || manifestFile.length() < 10) continue
            val voice = try {
                parseManifest(manifestFile.readText(), dir.name)
            } catch (e: Exception) {
                null
            } ?: continue
            if (!voice.id.equals(dir.name, ignoreCase = true) && voice.dir != dir.name) {
                // Le manifest déclare un id différent du dossier : on fait
                // confiance au dossier (chemin local = source de vérité).
                Log.w(TAG, "voice.json id='${voice.id}' != dossier='${dir.name}'")
            }
            if (isLocalVoiceComplete(dir, voice)) result.add(voice)
        }
        return result.sortedBy { it.id }
    }

    fun isInstalled(context: Context, voiceId: String): Boolean {
        val dir = voiceDir(context, voiceId)
        val manifestFile = File(dir, "voice.json")
        if (!manifestFile.exists()) return false
        val voice = try {
            parseManifest(manifestFile.readText(), dir.name)
        } catch (e: Exception) {
            null
        } ?: return false
        return isLocalVoiceComplete(dir, voice)
    }

    // --- Découverte réseau ---

    /**
     * Récupère le catalogue depuis HuggingFace : liste les dossiers racine du
     * repo (API tree), puis télécharge chaque voice.json.
     * Repli automatique sur l'état local en cas d'échec réseau.
     */
    suspend fun fetchCatalog(context: Context, token: String): List<HfVoice> =
        withContext(Dispatchers.IO) {
            val dirs = try {
                listRepoDirectories(token)
            } catch (e: Exception) {
                Log.w(TAG, "API tree HF inaccessible : ${e.message}")
                null
            } ?: return@withContext installedVoices(context)

            val voices = mutableListOf<HfVoice>()
            for (dir in dirs) {
                val manifestJson = try {
                    fetchText("$HF_RESOLVE/$dir/voice.json", token)
                } catch (e: Exception) {
                    null
                } ?: continue
                parseManifest(manifestJson, dir)?.let { voices.add(it) }
            }
            if (voices.isNotEmpty()) {
                try {
                    cacheCatalog(context, voices)
                } catch (e: Exception) {
                    Log.w(TAG, "Cache catalogue non écrit : ${e.message}")
                }
            }
            Log.i(TAG, "Catalogue HF : ${voices.size} voix découverte(s)")
            voices
        }

    /** Liste les dossiers racine du repo (type == "directory"). */
    private fun listRepoDirectories(token: String): List<String>? {
        val body = fetchText(HF_API_TREE, token) ?: return null
        val array = JsonParser.parseString(body).asJsonArray
        val dirs = mutableListOf<String>()
        for (element in array) {
            val obj = element.asJsonObject
            if (obj.get("type")?.asString == "directory") {
                obj.get("path")?.asString?.let { dirs.add(it.trim('/')) }
            }
        }
        return dirs
    }

    /** Petit GET texte avec en-tête Authorization (repos HF privé). */
    private fun fetchText(urlString: String, token: String): String? {
        var connection: HttpURLConnection? = null
        var currentUrl = urlString
        for (redirect in 0 until 4) {
            val url = URL(currentUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "NekoSpeak-TTS/1.0")
            if (token.isNotBlank() && currentUrl.startsWith("https://huggingface.co/")) {
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            connection.instanceFollowRedirects = false
            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    return connection.inputStream.bufferedReader().use { it.readText() }
                }
                in 300..399 -> {
                    val location = connection.getHeaderField("Location") ?: return null
                    currentUrl = URL(URL(currentUrl), location).toString()
                    connection.disconnect()
                }
                else -> {
                    Log.w(TAG, "GET $urlString → HTTP $code")
                    connection.disconnect()
                    return null
                }
            }
        }
        return null
    }

    private fun cacheCatalog(context: Context, voices: List<HfVoice>) {
        val cache = mutableListOf<Map<String, String?>>()
        for (v in voices) {
            cache.add(
                mapOf(
                    "id" to v.id, "name" to v.name, "description" to v.description,
                    "language" to v.language, "gender" to v.gender, "refText" to v.refText,
                    "dir" to v.dir, "graphFile" to v.graphFile, "dataFile" to v.dataFile,
                    "refAudioFile" to v.refAudioFile
                )
            )
        }
        File(context.filesDir, CATALOG_CACHE).writeText(gson.toJson(cache))
    }

    // --- Téléchargement ---

    /**
     * Télécharge une voix (voice.json + graphe + poids + référence) dans
     * filesDir/hf_voices/<id>/. Retourne null en cas de succès, sinon le
     * message d'erreur.
     */
    suspend fun downloadVoice(
        context: Context,
        voice: HfVoice,
        token: String,
        onProgress: (Float) -> Unit
    ): String? = withContext(Dispatchers.IO) {
        val dir = voiceDir(context, voice.id)
        dir.mkdirs()

        // Poids relatifs : le fichier data domine la progression (~2,4 Go)
        val parts = listOf(
            Triple("voice.json", "voice.json", 0.01f),
            Triple(voice.refAudioFile, voice.refAudioFile, 0.01f),
            Triple(voice.graphFile, voice.graphFile, 0.03f),
            Triple(voice.dataFile, voice.dataFile, 0.95f)
        )

        // Contrôle préalable de l'espace disque (~poids + marge)
        val requiredBytes = voice.dataMinBytes + 100_000_000L
        val stat = StatFs(context.filesDir.absolutePath)
        if (stat.availableBytes < requiredBytes) {
            val freeMb = stat.availableBytes / (1024L * 1024L)
            val needMb = requiredBytes / (1024L * 1024L)
            return@withContext "Espace insuffisant : ${needMb} Mo nécessaires, $freeMb Mo libres."
        }

        _downloadProgress.value = _downloadProgress.value + (voice.id to 0f)
        var done = 0f
        try {
            for ((localName, remoteName, weight) in parts) {
                val target = File(dir, localName)
                val expected = when (localName) {
                    "voice.json" -> 10L
                    voice.dataFile -> voice.dataMinBytes
                    else -> 1024L
                }
                if (target.exists() && target.length() >= expected) {
                    // Reprise : fichier déjà complet (>= car le manifest HF peut
                    // poser dataMinBytes = taille exacte du fichier)
                    done += weight
                    _downloadProgress.value = _downloadProgress.value + (voice.id to done)
                    onProgress(done)
                    continue
                }

                val remotePath = "${voice.dir}/$remoteName"
                var lastTick = 0L
                val error = ModelRepository.downloadFileTo(
                    "$HF_RESOLVE/$remotePath", target, token,
                    onProgress = { p ->
                        val total = done + p * weight
                        _downloadProgress.value = _downloadProgress.value + (voice.id to total)
                        val now = System.currentTimeMillis()
                        if (now - lastTick > 200) {
                            lastTick = now
                            onProgress(total.coerceAtMost(0.999f))
                        }
                    },
                    onError = { }
                )

                if (!error.isNullOrEmpty()) {
                    // Nettoyer un poids partiellement téléchargé
                    if (localName == voice.dataFile) target.delete()
                    return@withContext "Échec ($remoteName) : $error"
                }
                done += weight
                _downloadProgress.value = _downloadProgress.value + (voice.id to done)
                onProgress(done.coerceAtMost(0.999f))
            }

            // Marquer le manifest local avec l'id du dossier (source de vérité)
            normalizeLocalManifest(dir, voice)
            onProgress(1f)
            _downloadProgress.value = _downloadProgress.value + (voice.id to 1f)
            Log.i(TAG, "Voix '${voice.id}' téléchargée dans ${dir.absolutePath}")
            null
        } finally {
            // La progression reste visible jusqu'au prochain loadVoices(),
            // on retire juste l'entrée pour ne pas polluer la map.
            _downloadProgress.value = _downloadProgress.value - voice.id
        }
    }

    /** Réécrit le voice.json local en alignant id/dir sur le dossier. */
    private fun normalizeLocalManifest(dir: File, voice: HfVoice) {
        try {
            val manifest = mapOf(
                "id" to dir.name,
                "name" to voice.name,
                "description" to voice.description,
                "language" to voice.language,
                "gender" to voice.gender,
                "refText" to voice.refText,
                "files" to mapOf(
                    "graph" to voice.graphFile,
                    "data" to voice.dataFile,
                    "refAudio" to voice.refAudioFile
                ),
                "dataMinBytes" to voice.dataMinBytes
            )
            File(dir, "voice.json").writeText(gson.toJson(manifest))
        } catch (e: Exception) {
            Log.w(TAG, "Manifest local non réécrit : ${e.message}")
        }
    }

    fun deleteVoice(context: Context, voiceId: String): Boolean {
        val dir = voiceDir(context, voiceId)
        if (!dir.exists()) return false
        return dir.deleteRecursively()
    }

    // --- Migration de l'installation antérieure ---

    /**
     * Migration de l'ancien layout : backbone narrateur posé à la racine de
     * filesDir/omnivoice/ (ModelRepository d'origine) → filesDir/hf_voices/
     * narrateur/ avec voice.json et référence copiée depuis l'asset embarqué.
     * Ne fait rien si la voix narrateur est déjà migrée ou absente.
     */
    fun migrateLegacyNarrateur(context: Context): Boolean {
        val targetDir = voiceDir(context, "narrateur")
        if (targetDir.exists() && File(targetDir, "voice.json").exists()) return false

        val legacyDir = File(context.filesDir, "omnivoice")
        val legacyGraph = File(legacyDir, DEFAULT_GRAPH)
        val legacyData = File(legacyDir, DEFAULT_DATA)
        if (!legacyGraph.exists() || legacyData.length() <= DEFAULT_DATA_MIN_BYTES) return false

        return try {
            targetDir.mkdirs()
            if (!legacyGraph.renameTo(File(targetDir, DEFAULT_GRAPH)) ||
                !File(targetDir, DEFAULT_GRAPH).exists()) {
                // rename peut échouer sur certains appareils → copie de secours
                legacyGraph.copyTo(File(targetDir, DEFAULT_GRAPH), overwrite = true)
                legacyGraph.delete()
            }
            if (!legacyData.renameTo(File(targetDir, DEFAULT_DATA)) ||
                !File(targetDir, DEFAULT_DATA).exists()) {
                legacyData.copyTo(File(targetDir, DEFAULT_DATA), overwrite = true)
                legacyData.delete()
            }

            // Référence : copie de l'asset embarqué (clip_0733)
            val refFile = File(targetDir, DEFAULT_REF)
            if (!refFile.exists() || refFile.length() < 1024) {
                context.assets.open("narrateur_ref.wav").use { input ->
                    refFile.outputStream().use { output -> input.copyTo(output) }
                }
            }

            val manifest = mapOf(
                "id" to "narrateur",
                "name" to "Le Narrateur",
                "description" to "Voix narrateur dark fantasy (LoRA FR)",
                "language" to "fr",
                "gender" to "Male",
                "refText" to com.nekospeak.tts.engine.OmniVoiceEngine.NARRATEUR_REF_TEXT,
                "files" to mapOf(
                    "graph" to DEFAULT_GRAPH,
                    "data" to DEFAULT_DATA,
                    "refAudio" to DEFAULT_REF
                ),
                "dataMinBytes" to DEFAULT_DATA_MIN_BYTES
            )
            File(targetDir, "voice.json").writeText(gson.toJson(manifest))
            Log.i(TAG, "Voix narrateur migrée vers ${targetDir.absolutePath}")
            // Supprimer le .onnx_data résiduel éventuel (partiel)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Migration narrateur échouée : ${e.message}")
            false
        }
    }
}
