package com.friday.assistant.ai

import android.content.Context
import android.util.Log
import com.friday.assistant.data.ModelStatus
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicInteger

class LiteRtLmService(private val appContext: Context) {
    private val mutex = Mutex()
    private var engine: Engine? = null
    private var loadedPath: String? = null
    private val activeGenerations = AtomicInteger(0)

    suspend fun loadModel(modelPath: String, useGpu: Boolean = false): Result<ModelStatus> = mutex.withLock {
        withContext(Dispatchers.IO) {
            runCatching {
                require(activeGenerations.get() == 0) { "Cannot load model while generation is in progress. Stop generation first." }
                closeInternal()
                val file = File(modelPath)
                require(file.exists() && file.isFile) { "Model file not found: $modelPath" }
                require(file.extension.equals("litertlm", ignoreCase = true)) { "Expected a .litertlm model file." }

                Log.i("FRIDAY", "MODEL: loading ${file.name} (${file.length() / (1024 * 1024)} MB) gpu=$useGpu")
                val t0 = System.currentTimeMillis()
                val config = EngineConfig(
                    modelPath = file.absolutePath,
                    backend = if (useGpu) Backend.GPU() else Backend.CPU(),
                    cacheDir = appContext.cacheDir.absolutePath
                )
                val newEngine = Engine(config)
                newEngine.initialize()
                engine = newEngine
                loadedPath = file.absolutePath
                Log.i("FRIDAY", "MODEL: loaded ${file.name} in ${System.currentTimeMillis() - t0} ms")
                ModelStatus(
                    configuredPath = modelPath,
                    resolvedPath = file.absolutePath,
                    isReady = true,
                    statusText = "Model ready (${file.name})"
                )
            }.onFailure {
                Log.w("FRIDAY", "MODEL: load failed: ${it.message}")
                closeInternal()
            }
        }
    }

    fun generateReply(
        prompt: String,
        history: List<Pair<String, String>> = emptyList(),
        systemPrompt: String = com.friday.assistant.data.PersonaConfig.DEFAULT_PERSONA_PROMPT,
        memories: List<String> = emptyList()
    ): Flow<String> = callbackFlow {
        val eng = mutex.withLock { engine }
            ?: run { close(IllegalStateException("No local model is loaded. Add a .litertlm model in Settings and load it.")); return@callbackFlow }
        activeGenerations.incrementAndGet()
        var generationCounted = true
        val completed = java.util.concurrent.atomic.AtomicBoolean(false)
        fun complete(err: Throwable? = null) {
            if (completed.compareAndSet(false, true)) {
                if (generationCounted) {
                    generationCounted = false
                    activeGenerations.updateAndGet { current -> if (current > 0) current - 1 else 0 }
                }
                if (err != null) close(err) else close()
            }
        }

        val systemInstruction = buildString {
            append(systemPrompt.trim().ifBlank { com.friday.assistant.data.PersonaConfig.DEFAULT_PERSONA_PROMPT })
            val mem = memories.map { it.trim() }.filter { it.isNotBlank() }.take(20)
            if (mem.isNotEmpty()) {
                append("\n\nPersistent memories about the user/context (use when relevant, do not invent):\n")
                mem.forEachIndexed { _, m -> append("- ").append(m.take(300)).append('\n') }
            }
        }
        val conv = try {
            eng.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(systemInstruction),
                    samplerConfig = SamplerConfig(topK = 40, topP = 0.95, temperature = 0.4)
                )
            )
        } catch (e: Exception) {
            complete(e)
            return@callbackFlow
        }

        val userPrompt = prompt.trim().ifBlank { prompt }

        val buffer = StringBuilder()
        val genStart = System.currentTimeMillis()
        try {
            conv.sendMessageAsync(
                userPrompt,
                object : MessageCallback {
                    override fun onMessage(message: Message) {
                        if (completed.get()) return
                        val chunk = message.toString()
                        if (chunk.isNotEmpty()) {
                            buffer.append(chunk)
                            val cleaned = sanitizeLocalOutput(buffer.toString(), streaming = true)
                            if (cleaned.isNotBlank()) trySend(cleaned)
                        }
                    }

                    override fun onDone() {
                        if (completed.get()) return
                        val text = sanitizeLocalOutput(buffer.toString(), streaming = false).ifBlank { "(empty response)" }
                        Log.i("FRIDAY", "GEN: model done ${text.length} chars in ${System.currentTimeMillis() - genStart} ms")
                        trySend(text)
                        complete()
                    }

                    override fun onError(t: Throwable) {
                        if (completed.get()) return
                        Log.w("FRIDAY", "GEN: model error: ${t.message}")
                        complete(t)
                    }
                }
            )
        } catch (e: Throwable) {
            Log.w("FRIDAY", "GEN: send failed: ${e.message}")
            complete(e)
        }

        awaitClose {
            runCatching { conv.close() }
            // if cancelled/closed before callback completion, ensure we release the generation slot
            if (completed.compareAndSet(false, true) && generationCounted) {
                generationCounted = false
                activeGenerations.updateAndGet { current -> if (current > 0) current - 1 else 0 }
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun unload(): ModelStatus = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (activeGenerations.get() > 0) error("Cannot unload while generation is in progress. Stop generation first.")
            closeInternal()
            ModelStatus(statusText = "No local model loaded")
        }
    }

    private fun closeInternal() {
        runCatching { engine?.close() }
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

    private fun sanitizeLocalOutput(raw: String, streaming: Boolean): String {
        if (raw.isEmpty()) return raw
        var text = raw.replace("\uFEFF", "")
        // Drop common leading control tokens/role markers that can appear with some LiteRT/chat templates.
        val leadingPatterns = listOf(
            Regex("""^(?:<\|im_start\|>\s*assistant\s*)+""", RegexOption.IGNORE_CASE),
            Regex("""^(?:<\|assistant\|>\s*)+""", RegexOption.IGNORE_CASE),
            Regex("""^(?:assistant\s*[:：]\s*)+""", RegexOption.IGNORE_CASE),
            Regex("""^(?:model\s*[:：]\s*)+""", RegexOption.IGNORE_CASE)
        )
        var changed = true
        var guard = 0
        while (changed && guard++ < 8) {
            changed = false
            val trimmedStart = text.trimStart('\n', '\r', ' ', '\t')
            if (trimmedStart !== text && trimmedStart != text) {
                // keep intentional leading spaces only after real content appears; during early stream trim aggressively
                text = trimmedStart
                changed = true
            }
            for (p in leadingPatterns) {
                val m = p.find(text)
                if (m != null && m.range.first == 0 && m.value.isNotEmpty()) {
                    text = text.removeRange(m.range)
                    changed = true
                }
            }
        }
        // Remove trailing end tokens if emitted
        text = text.replace(Regex("""<\|im_end\|>\s*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""<\|endoftext\|>\s*$""", RegexOption.IGNORE_CASE), "")
        return if (streaming) text.trimStart('\n', '\r') else text.trim()
    }

    data class BundledCopyProgress(
        val fileName: String,
        val fileIndex: Int,
        val fileCount: Int,
        val bytesCopied: Long,
        val bytesTotal: Long?
    )

    companion object {
        private const val BUNDLED_ASSET_DIR = "models"

        suspend fun ensureBundledModel(
            context: Context,
            onProgress: ((BundledCopyProgress) -> Unit)? = null
        ): String? = withContext(Dispatchers.IO) {
            ensureBundledModels(context, onProgress)
            val copied = listAvailableModels(context)
            copied.firstOrNull { it.substringAfterLast('/').startsWith("Qwen3", ignoreCase = true) }
                ?: copied.firstOrNull()
        }

        private fun ensureBundledModels(
            context: Context,
            onProgress: ((BundledCopyProgress) -> Unit)? = null
        ) {
            val assets = runCatching { context.assets.list(BUNDLED_ASSET_DIR)?.toList().orEmpty() }
                .getOrDefault(emptyList())
                .filter { it.endsWith(".litertlm", ignoreCase = true) }
            if (assets.isEmpty()) return
            val outDir = File(context.filesDir, "models").apply { mkdirs() }
            val pending = assets.filter { name ->
                val outFile = File(outDir, name)
                !(outFile.exists() && outFile.length() > 0L)
            }
            if (pending.isEmpty()) return
            pending.forEachIndexed { index, bundledName ->
                val outFile = File(outDir, bundledName)
                Log.i("FRIDAY", "MODEL: copying bundled $bundledName (${index + 1}/${pending.size})")
                val tmp = File(outDir, "$bundledName.tmp")
                val total = runCatching {
                    context.assets.openFd("$BUNDLED_ASSET_DIR/$bundledName").use { it.length }
                }.getOrNull()
                try {
                    onProgress?.invoke(
                        BundledCopyProgress(
                            fileName = bundledName,
                            fileIndex = index + 1,
                            fileCount = pending.size,
                            bytesCopied = 0L,
                            bytesTotal = total
                        )
                    )
                    context.assets.open("$BUNDLED_ASSET_DIR/$bundledName").use { input ->
                        FileOutputStream(tmp).use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var copied = 0L
                            var lastReport = 0L
                            while (true) {
                                val read = input.read(buffer)
                                if (read <= 0) break
                                output.write(buffer, 0, read)
                                copied += read
                                if (copied - lastReport >= 4L * 1024L * 1024L || read < buffer.size) {
                                    lastReport = copied
                                    onProgress?.invoke(
                                        BundledCopyProgress(
                                            fileName = bundledName,
                                            fileIndex = index + 1,
                                            fileCount = pending.size,
                                            bytesCopied = copied,
                                            bytesTotal = total
                                        )
                                    )
                                }
                            }
                            output.flush()
                            onProgress?.invoke(
                                BundledCopyProgress(
                                    fileName = bundledName,
                                    fileIndex = index + 1,
                                    fileCount = pending.size,
                                    bytesCopied = copied,
                                    bytesTotal = total ?: copied
                                )
                            )
                        }
                    }
                    if (outFile.exists()) outFile.delete()
                    if (!tmp.renameTo(outFile)) {
                        tmp.copyTo(outFile, overwrite = true)
                        tmp.delete()
                    }
                    Log.i("FRIDAY", "MODEL: bundled ready $bundledName")
                } finally {
                    runCatching { tmp.delete() }
                }
            }
        }

        fun modelDirs(context: Context): List<File> = listOfNotNull(
            File(context.filesDir, "models"),
            context.getExternalFilesDir(null)?.let { File(it, "models") }
        )

        fun listAvailableModels(context: Context): List<String> = modelDirs(context)
            .flatMap { dir -> dir.listFiles()?.toList().orEmpty() }
            .filter { it.isFile && it.extension.equals("litertlm", ignoreCase = true) }
            .map { it.absolutePath }
            .distinct()
            .sorted()

        suspend fun discoverDefaultModelPath(
            context: Context,
            onProgress: ((BundledCopyProgress) -> Unit)? = null
        ): String? = withContext(Dispatchers.IO) {
            ensureBundledModels(context, onProgress)
            val found = listAvailableModels(context)
            found.firstOrNull { it.substringAfterLast('/').startsWith("Qwen3", ignoreCase = true) }
                ?: found.firstOrNull()
        }
    }
}
