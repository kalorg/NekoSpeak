package com.nekospeak.tts.data

data class VoiceDefinition(
    val id: String,
    val name: String,
    val gender: String, // "Male" or "Female"
    val region: String = "US",
    val description: String? = null,
    val sampleUrl: String? = null, // Link to HuggingFace sample
    val modelType: String // "pocket_v1", "kokoro_v1.0", "kitten_nano"
)

object VoiceDefinitions {
    
    // Voix françaises Pocket-TTS (corpus Expresso, Kyutai) — locuteurs natifs français
    // Licence CC BY-NC : usage personnel OK
    val POCKET_VOICES = listOf(
        VoiceDefinition("narrateur", "Le Narrateur", "Male", "French", "Narration posée et grave, idéale pour les romans", "https://huggingface.co/kyutai/tts-voices/blob/main/expresso/ex03-ex02_narration_001_channel1_674s.wav", "pocket_v1"),
        VoiceDefinition("conteuse", "La Conteuse", "Female", "French", "Narration féminine envoûtante pour les récits sombres", "https://huggingface.co/kyutai/tts-voices/blob/main/expresso/ex04-ex01_narration_001_channel1_605s.wav", "pocket_v1"),
        VoiceDefinition("murmure", "Le Murmure", "Male", "French", "Chuchotement grave, atmosphère inquiétante", "https://huggingface.co/kyutai/tts-voices/blob/main/expresso/ex01-ex02_whisper_001_channel1_579s.wav", "pocket_v1"),
        VoiceDefinition("chuchoteuse", "La Chuchoteuse", "Female", "French", "Chuchotement féminin, frissons garantis", "https://huggingface.co/kyutai/tts-voices/blob/main/expresso/ex04-ex03_whisper_001_channel1_198s.wav", "pocket_v1"),
        VoiceDefinition("effroi", "L'Effroi", "Male", "French", "Voix grave marquée par la crainte et l'épouvante", "https://huggingface.co/kyutai/tts-voices/blob/main/expresso/ex03-ex01_awe_001_channel1_1323s.wav", "pocket_v1"),
        VoiceDefinition("frayeur", "La Frayeur", "Female", "French", "Voix féminine tremblante, tension et peur", "https://huggingface.co/kyutai/tts-voices/blob/main/expresso/ex04-ex02_fearful_001_channel1_316s.wav", "pocket_v1"),
        VoiceDefinition("douleur", "La Douleur", "Male", "French", "Mélancolie et compassion, passages tristes", "https://huggingface.co/kyutai/tts-voices/blob/main/expresso/ex03-ex02_sad-sympathetic_001_channel1_454s.wav", "pocket_v1"),
        VoiceDefinition("sangfroid", "Le Sang-froid", "Male", "French", "Calme imperturbable, descriptions et pauses", "https://huggingface.co/kyutai/tts-voices/blob/main/expresso/ex03-ex01_calm_001_channel1_1143s.wav", "pocket_v1"),
        VoiceDefinition("apaisee", "L'Apaisée", "Female", "French", "Douceur féminine, moments de répit", "https://huggingface.co/kyutai/tts-voices/blob/main/expresso/ex04-ex02_calm_002_channel1_480s.wav", "pocket_v1"),
        VoiceDefinition("colere", "La Colère", "Male", "French", "Rage contenue, confrontations et antagonistes", "https://huggingface.co/kyutai/tts-voices/blob/main/expresso/ex03-ex01_angry_001_channel1_201s.wav", "pocket_v1"),
        VoiceDefinition("defi", "Le Défi", "Female", "French", "Colère féminine, répliques cinglantes", "https://huggingface.co/kyutai/tts-voices/blob/main/expresso/ex04-ex02_angry_001_channel1_119s.wav", "pocket_v1"),
        VoiceDefinition("naturel_m", "Compagnon", "Male", "French", "Dialogue naturel masculin", "https://huggingface.co/kyutai/tts-voices/blob/main/expresso/ex01-ex02_default_001_channel1_168s.wav", "pocket_v1"),
        VoiceDefinition("naturel_f", "Compagne", "Female", "French", "Dialogue naturel féminin", "https://huggingface.co/kyutai/tts-voices/blob/main/expresso/ex04-ex03_default_002_channel2_239s.wav", "pocket_v1")
    )

    // Kokoro Standard Voices — seule voix française disponible dans le pack ONNX Kokoro-82M
    // Fixes: https://github.com/siva-sub/NekoSpeak/issues/5
    val KOKORO_VOICES = listOf(
        // Voix française (Kokoro v1.0 multilingual)
        VoiceDefinition("ff_siwis", "Siwis", "Female", "FR", "Voix féminine française (multilingue)", null, "kokoro_v1.0")
    )
    
    // Celebrity Voices (Downloaded on-demand from HuggingFace)
    // WARNING: Celebrity voice cloning raises ethical/legal concerns. Use responsibly.
    // Liste vidée : application orientée lecture en français, clonage de personnalités désactivé.
    val CELEBRITY_VOICES = listOf<VoiceDefinition>()
    
    // OmniVoice preset voices — voice design system
    // These use instruct-based voice control (gender + pitch → text description)
    // The model generates voice characteristics from the description
    val OMNIVOICE_VOICES = listOf(
        // English voices
        VoiceDefinition("ov_alex", "Alex (F, Low)", "Female", "US", "Low-pitched female voice", null, "omnivoice"),
        VoiceDefinition("ov_bella", "Bella (F, Mid)", "Female", "US", "Medium-pitched female voice", null, "omnivoice"),
        VoiceDefinition("ov_clara", "Clara (F, High)", "Female", "US", "High-pitched female voice", null, "omnivoice"),
        VoiceDefinition("ov_daniel", "Daniel (M, Low)", "Male", "US", "Low-pitched male voice", null, "omnivoice"),
        VoiceDefinition("ov_eric", "Eric (M, Mid)", "Male", "US", "Medium-pitched male voice", null, "omnivoice"),
        VoiceDefinition("ov_felix", "Felix (M, High)", "Male", "US", "High-pitched male voice", null, "omnivoice"),
        VoiceDefinition("ov_glen", "Glen (N, Low)", "Male", "US", "Low-pitched neutral voice", null, "omnivoice"),
        VoiceDefinition("ov_hayden", "Hayden (N, Mid)", "Male", "US", "Medium-pitched neutral voice", null, "omnivoice"),
        VoiceDefinition("ov_iris", "Iris (N, High)", "Female", "US", "High-pitched neutral voice", null, "omnivoice"),
        // Multilingual voices
        VoiceDefinition("ov_zh_female", "中文女声", "Female", "CN", "Chinese female voice", null, "omnivoice"),
        VoiceDefinition("ov_zh_male", "中文男声", "Male", "CN", "Chinese male voice", null, "omnivoice"),
        VoiceDefinition("ov_ja_female", "日本語女声", "Female", "JP", "Japanese female voice", null, "omnivoice"),
        VoiceDefinition("ov_ja_male", "日本語男声", "Male", "JP", "Japanese male voice", null, "omnivoice"),
        VoiceDefinition("ov_ko_female", "한국어 여성", "Female", "KR", "Korean female voice", null, "omnivoice"),
        VoiceDefinition("ov_ko_male", "한국어 남성", "Male", "KR", "Korean male voice", null, "omnivoice"),
        VoiceDefinition("ov_fr_female", "Française", "Female", "FR", "French female voice", null, "omnivoice"),
        VoiceDefinition("ov_de_female", "Deutsch", "Female", "DE", "German female voice", null, "omnivoice"),
        VoiceDefinition("ov_es_female", "Español", "Female", "ES", "Spanish female voice", null, "omnivoice"),
        VoiceDefinition("ov_ar_male", "العربية", "Male", "SA", "Arabic male voice", null, "omnivoice"),
        VoiceDefinition("ov_ru_male", "Русский", "Male", "RU", "Russian male voice", null, "omnivoice"),
        VoiceDefinition("ov_hi_female", "हिन्दी", "Female", "IN", "Hindi female voice", null, "omnivoice"),
    )
    
    // Helper to get all available voices for a model
    fun getVoicesForModel(modelType: String): List<VoiceDefinition> {
        return when (modelType) {
            "pocket_v1" -> POCKET_VOICES + CELEBRITY_VOICES
            "kokoro_v1.0" -> KOKORO_VOICES
            "kitten_nano" -> listOf(VoiceDefinition("kitten_v1", "Kitten", "Female", "US", "Standard Kitten Voice", null, "kitten_nano"))
            "omnivoice" -> OMNIVOICE_VOICES
            else -> emptyList()
        }
    }
    
    // Check if a voice ID requires download (celebrity voices)
    fun requiresDownload(voiceId: String): Boolean {
        return CELEBRITY_VOICES.any { it.id == voiceId }
    }
}