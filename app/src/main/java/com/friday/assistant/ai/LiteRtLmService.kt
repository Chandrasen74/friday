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
                engine = newEngine
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
        val eng = mutex.withLock { engine }
            ?: error("No local model is loaded. Add a .litertlm model in Settings and load it.")
        val conv = eng.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of(
                    "You are FRIDAY, a helpful on-device assistant. Answer the user's question directly, clearly, and completely. " +
                        "Do not offer menus of how you can help unless asked. Stay on topic."
                ),
                samplerConfig = SamplerConfig(topK = 32, topP = 0.9, temperature = 0.3)
            )
        )
        try {
            conv.sendMessageAsync(prompt).collect { chunk ->
                val text = chunk.toString()
                if (text.isNotEmpty()) emit(text)
            }
        } finally {
            runCatching { conv.close() }
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
        val path = loadedPath
        val ready = engine != null && path != null
        return ModelStatus(
            configuredPath = configuredPath,
            resolvedPath = path,
            isReady = ready,
            isLoading = loading,
            statusText = when {
                loading -> "Loading model..."
                ready -> "Model ready (${File(path).name})"
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
