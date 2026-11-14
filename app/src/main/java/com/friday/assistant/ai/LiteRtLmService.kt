package com.friday.assistant.ai

import android.content.Context
import com.friday.assistant.data.ModelStatus
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class LiteRtLmService(private val appContext: Context) {
    private val mutex = Mutex()
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var loadedPath: String? = null

    suspend fun loadModel(modelPath: String, useGpu: Boolean = false): Result<ModelStatus> = mutex.withLock {
        withContext(Dispatchers.IO) {
            runCatching {
                closeInternal()
                val file = File(modelPath)
                require(file.exists() && file.isFile) { "Model file not found: $modelPath" }
                require(file.extension.equals("litertlm", ignoreCase = true)) { "Expected a .litertlm model file." }

                val config = EngineConfig(
                    modelPath = file.absolutePath,
                    backend = if (useGpu) Backend.GPU() else Backend.CPU(),
                    cacheDir = appContext.cacheDir.absolutePath
                )
                val newEngine = Engine(config)
                newEngine.initialize()
                val newConversation = newEngine.createConversation(
                    ConversationConfig(
                        systemInstruction = Contents.of(
                            "You are FRIDAY, a helpful local-first assistant running on the user's Android device. Be concise, clear, and practical."
                        ),
                        samplerConfig = SamplerConfig(topK = 40, topP = 0.95, temperature = 0.7)
                    )
                )
                engine = newEngine
                conversation = newConversation
                loadedPath = file.absolutePath
                ModelStatus(
                    configuredPath = modelPath,
                    resolvedPath = file.absolutePath,
                    isReady = true,
                    statusText = "Model ready (${file.name})"
                )
            }.onFailure {
                closeInternal()
            }
        }
    }

    fun generateReply(prompt: String): Flow<String> = flow {
        val conv = mutex.withLock { conversation }
            ?: error("No local model is loaded. Add a .litertlm model in Settings and load it.")
        conv.sendMessageAsync(prompt).collect { chunk ->
            emit(chunk.toString())
        }
    }.flowOn(Dispatchers.IO)

    suspend fun unload(): ModelStatus = mutex.withLock {
        withContext(Dispatchers.IO) {
            closeInternal()
            ModelStatus(statusText = "No local model loaded")
        }
    }

    private fun closeInternal() {
        runCatching { conversation?.close() }
        runCatching { engine?.close() }
        conversation = null
        engine = null
        loadedPath = null
    }

    fun currentStatus(configuredPath: String = "", loading: Boolean = false, error: String? = null): ModelStatus {
        val ready = conversation != null && loadedPath != null
        return ModelStatus(
            configuredPath = configuredPath,
            resolvedPath = loadedPath,
            isReady = ready,
            isLoading = loading,
            statusText = when {
                loading -> "Loading model..."
                ready -> "Model ready (${File(loadedPath!!).name})"
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
