package com.friday.assistant.data

/**
 * Curated list of on-device (.litertlm) models that are known to work well on
 * mid-range phones (4 GB RAM class). Sizes are the actual file sizes of the
 * hosted artifacts on Hugging Face (litert-community).
 *
 * Models larger than ~600 MB (1.5B+) are deliberately excluded: they need
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
            id = "gemma3-270m",
            displayName = "Gemma 3 270M (fastest)",
            fileName = "gemma3-270m-it-q8.litertlm",
            sizeLabel = "304 MB",
            description = "Smallest and fastest. Short replies, quick first token. Good for testing and quick questions.",
            url = "https://huggingface.co/litert-community/gemma-3-270m-it/resolve/main/gemma3-270m-it-q8.litertlm"
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
            id = "gemma3-1b",
            displayName = "Gemma 3 1B (best quality)",
            fileName = "gemma3-1b-it-int4.litertlm",
            sizeLabel = "584 MB",
            description = "Noticeably better replies than the two above. Slower on CPU and needs more free RAM.",
            url = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.litertlm"
        )
    )
}
