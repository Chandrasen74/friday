package com.friday.assistant.ai

import android.content.Context
import com.friday.assistant.data.ModelStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

/**
 * LiteRT-LM integration scaffold.
 *
 * Direct LiteRT-LM runtime dependency is not bundled in this build due to current
 * Kotlin/AGP toolchain compatibility constraints in the project. The API surface
 * and UI flow are kept ready so the runtime can be wired in once the toolchain is
 * aligned with a compatible LiteRT-LM release.
 */
class LiteRtLmService(private val appContext: Context) {
    private var loadedPath: String? = null

    suspend fun loadModel(modelPath: String, @Suppress("UNUSED_PARAMETER") useGpu: Boolean = false): Result<ModelStatus> {
        return runCatching {
            val file = File(modelPath)
            require(file.exists() && file.isFile) { "Model file not found: $modelPath" }
            require(file.extension.equals("litertlm", ignoreCase = true)) { "Expected a .litertlm model file." }
            loadedPath = file.absolutePath
            ModelStatus(
                configuredPath = modelPath,
                resolvedPath = file.absolutePath,
                isReady = false,
                statusText = "Model file found (runtime pending)",
                lastError = "LiteRT-LM runtime not bundled in this build. Toolchain-compatible LiteRT-LM integration is pending."
            )
        }
    }

    fun generateReply(prompt: String): Flow<String> = flow {
        emit(
            "On-device LiteRT-LM runtime is not active in this build yet, so I can't run local inference for: \"${prompt.take(80)}\".\n\n" +
                "The app UI and model-loading flow are ready. Next step is to align the project's Kotlin/AGP toolchain with a compatible LiteRT-LM release and enable the Engine/Conversation runtime."
        )
    }.flowOn(Dispatchers.IO)

    suspend fun unload(): ModelStatus {
        delay(10)
        loadedPath = null
        return ModelStatus(statusText = "No local model loaded")
    }

    fun currentStatus(configuredPath: String = "", loading: Boolean = false, error: String? = null): ModelStatus {
        val path = loadedPath
        return ModelStatus(
            configuredPath = configuredPath,
            resolvedPath = path,
            isReady = false,
            isLoading = loading,
            statusText = when {
                loading -> "Checking model..."
                path != null -> "Model file found (runtime pending)"
                error != null -> "Model error"
                else -> "No local model loaded"
            },
            lastError = error
        )
    }

    companion object {
        fun discoverDefaultModelPath(context: Context): String? {
            val dirs = listOfNotNull(
                context.getExternalFilesDir(null)?.let { File(it, "models") },
                File(context.filesDir, "models")
            )
            val found = dirs
                .flatMap { dir -> dir.listFiles()?.toList().orEmpty() }
                .firstOrNull { it.isFile && it.extension.equals("litertlm", ignoreCase = true) }
            return found?.absolutePath
        }
    }
}
