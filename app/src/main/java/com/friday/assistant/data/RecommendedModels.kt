package com.friday.assistant.data

/**
 * Curated list of on-device (.litertlm) models that are known to work well on
 * mid-range phones (4 GB RAM class). Sizes are the actual file sizes of the
 * hosted artifacts on Hugging Face (litert-community). Every URL here is
 * anonymously downloadable (verified); Google's Gemma repos are gated and
 * return HTTP 401 without a login, so no Gemma models are listed.
 *
 * Models larger than ~700 MB (1.5B+) are deliberately excluded: they need
 * more RAM and storage than this device class comfortably has.
 */
data class RecommendedModel(
    val id: String,
    val displayName: String,
    val fileName: String,
    val sizeLabel: String,
    val description: String,
    val url: String
)

object RecommendedModels {
    val CATALOG: List<RecommendedModel> = listOf(
        RecommendedModel(
            id = "granite-350m-int8",
            displayName = "Granite 4.0 350M (compact)",
            fileName = "granite-4.0-h-350m_int8.litertlm",
            sizeLabel = "416 MB",
            description = "Compact alternative with a different reply style. IBM Granite hybrid architecture, 8-bit quantized.",
            url = "https://huggingface.co/litert-community/granite-4.0-h-350m/resolve/main/granite-4.0-h-350m_int8.litertlm"
        ),
        RecommendedModel(
            id = "qwen3-0.6b",
            displayName = "Qwen3 0.6B (default)",
            fileName = "Qwen3-0.6B_dynamic_wi4b32_afp32.litertlm",
            sizeLabel = "343 MB",
            description = "Balanced default, same model bundled with the app. Best all-round small model for this app.",
            url = "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/main/Qwen3-0.6B_dynamic_wi4b32_afp32.litertlm"
        ),
        RecommendedModel(
            id = "granite-350m-fp16",
            displayName = "Granite 4.0 350M (best quality)",
            fileName = "granite-4.0-h-350m_fp16.litertlm",
            sizeLabel = "690 MB",
            description = "Highest quality of the three. Full-precision 350M hybrid, slower on CPU and needs about 700 MB free RAM.",
            url = "https://huggingface.co/litert-community/granite-4.0-h-350m/resolve/main/granite-4.0-h-350m_fp16.litertlm"
        )
    )
}
