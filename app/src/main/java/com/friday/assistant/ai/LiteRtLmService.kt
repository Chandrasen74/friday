package com.friday.assistant.ai

import android.content.Context
import com.friday.assistant.data.ModelStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

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
        private const val BUNDLED_ASSET_DIR = "models"

        suspend fun ensureBundledModel(context: Context): String? = withContext(Dispatchers.IO) {
            val assets = runCatching { context.assets.list(BUNDLED_ASSET_DIR)?.toList().orEmpty() }
                .getOrDefault(emptyList())
            val bundledName = assets.firstOrNull { it.endsWith(".litertlm", ignoreCase = true) } ?: return@withContext null
            val outDir = File(context.filesDir, "models").apply { mkdirs() }
            val outFile = File(outDir, bundledName)
            if (!outFile.exists() || outFile.length() == 0L) {
                val tmp = File(outDir, "$bundledName.tmp")
                context.assets.open("$BUNDLED_ASSET_DIR/$bundledName").use { input ->
                    FileOutputStream(tmp).use { output ->
                        input.copyTo(output)
                        output.flush()
                    }
                }
                if (outFile.exists()) outFile.delete()
                if (!tmp.renameTo(outFile)) {
                    tmp.copyTo(outFile, overwrite = true)
                    tmp.delete()
                }
            }
            outFile.absolutePath
        }

        suspend fun discoverDefaultModelPath(context: Context): String? {
            ensureBundledModel(context)?.let { return it }
            val dirs = listOfNotNull(
                File(context.filesDir, "models"),
                context.getExternalFilesDir(null)?.let { File(it, "models") }
            )
            val found = dirs
                .flatMap { dir -> dir.listFiles()?.toList().orEmpty() }
                .firstOrNull { it.isFile && it.extension.equals("litertlm", ignoreCase = true) }
            return found?.absolutePath
        }
    }
}
