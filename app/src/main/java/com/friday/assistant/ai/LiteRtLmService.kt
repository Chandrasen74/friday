package com.friday.assistant.ai

import android.content.Context
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

class LiteRtLmService(private val appContext: Context) {
    private val mutex = Mutex()
    private var engine: Engine? = null
    private var loadedPath: String? = null
    @Volatile private var activeGenerations: Int = 0

    suspend fun loadModel(modelPath: String, useGpu: Boolean = false): Result<ModelStatus> = mutex.withLock {
        withContext(Dispatchers.IO) {
            runCatching {
                require(activeGenerations == 0) { "Cannot load model while generation is in progress. Stop generation first." }
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

    fun generateReply(
        prompt: String,
        history: List<Pair<String, String>> = emptyList(),
        systemPrompt: String = com.friday.assistant.data.PersonaConfig.DEFAULT_PERSONA_PROMPT,
        memories: List<String> = emptyList()
    ): Flow<String> = callbackFlow {
        val eng = mutex.withLock { engine }
            ?: run { close(IllegalStateException("No local model is loaded. Add a .litertlm model in Settings and load it.")); return@callbackFlow }
        activeGenerations++
        val systemInstruction = buildString {
            append(systemPrompt.trim().ifBlank { com.friday.assistant.data.PersonaConfig.DEFAULT_PERSONA_PROMPT })
            val mem = memories.map { it.trim() }.filter { it.isNotBlank() }.take(20)
            if (mem.isNotEmpty()) {
                append("\n\nPersistent memories about the user/context (use when relevant, do not invent):\n")
                mem.forEachIndexed { i, m -> append("- ").append(m.take(300)).append('\n') }
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
            activeGenerations = (activeGenerations - 1).coerceAtLeast(0)
            close(e); return@callbackFlow
        }

        val fullPrompt = buildString {
            val recent = history.takeLast(6)
            if (recent.isNotEmpty()) {
                append("Previous conversation:\n")
                recent.forEach { (role, text) ->
                    val clean = text.trim().take(800)
                    if (clean.isNotEmpty()) {
                        append(role).append(": ").append(clean).append("\n")
                    }
                }
                append("\n")
            }
            append("User: ").append(prompt.trim())
        }

        val buffer = StringBuilder()
        var finished = false
        fun finishClose(err: Throwable? = null) {
            if (finished) return
            finished = true
            activeGenerations = (activeGenerations - 1).coerceAtLeast(0)
            if (err != null) close(err) else close()
        }
        try {
            conv.sendMessageAsync(
                fullPrompt,
                object : MessageCallback {
                    override fun onMessage(message: Message) {
                        if (finished) return
                        val chunk = message.toString()
                        if (chunk.isNotEmpty()) {
                            buffer.append(chunk)
                            trySend(buffer.toString())
                        }
                    }

                    override fun onDone() {
                        if (finished) return
                        val text = buffer.toString().trim()
                        if (text.isEmpty()) trySend("(empty response)") else trySend(text)
                        finishClose()
                    }

                    override fun onError(t: Throwable) {
                        if (finished) return
                        finishClose(t)
                    }
                }
            )
        } catch (e: Throwable) {
            finishClose(e)
        }

        awaitClose {
            runCatching { conv.close() }
            if (!finished) {
                finished = true
                activeGenerations = (activeGenerations - 1).coerceAtLeast(0)
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun unload(): ModelStatus = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (activeGenerations > 0) error("Cannot unload while generation is in progress. Stop generation first.")
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

        suspend fun downloadModel(context: Context, url: String): Result<String> = withContext(Dispatchers.IO) {
            runCatching {
                val u = url.trim()
                require(u.startsWith("http://") || u.startsWith("https://")) { "Enter a valid http(s) URL" }
                val rawName = u.substringAfterLast('/').substringBefore('?').ifBlank { "model.litertlm" }
                val sanitized = rawName.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80).ifBlank { "model.litertlm" }
                val fileName = if (sanitized.endsWith(".litertlm", true)) sanitized else "$sanitized.litertlm"
                val outDir = File(context.filesDir, "models").apply { mkdirs() }
                val outFile = File(outDir, fileName)
                val tmp = File(outDir, "$fileName.tmp")
                val conn = (java.net.URL(u).openConnection() as java.net.HttpURLConnection).apply {
                    connectTimeout = 20000
                    readTimeout = 60000
                    instanceFollowRedirects = true
                }
                try {
                    val code = conn.responseCode
                    if (code !in 200..299) error("Download failed: HTTP $code")
                    val maxBytes = 5L * 1024L * 1024L * 1024L
                    val len = conn.contentLengthLong
                    if (len > maxBytes) error("Model too large (>${maxBytes / (1024 * 1024)} MB limit)")
                    var total = 0L
                    conn.inputStream.use { input ->
                        java.io.FileOutputStream(tmp).use { output ->
                            val buf = ByteArray(DEFAULT_BUFFER_SIZE)
                            while (true) {
                                val read = input.read(buf)
                                if (read <= 0) break
                                total += read
                                if (total > maxBytes) error("Model too large (>${maxBytes / (1024 * 1024)} MB limit)")
                                output.write(buf, 0, read)
                            }
                            output.flush()
                        }
                    }
                    if (total <= 0L) error("Downloaded file is empty")
                    if (outFile.exists()) outFile.delete()
                    if (!tmp.renameTo(outFile)) {
                        tmp.copyTo(outFile, overwrite = true)
                        tmp.delete()
                    }
                    outFile.absolutePath
                } finally {
                    runCatching { tmp.delete() }
                    conn.disconnect()
                }
            }
        }

        suspend fun discoverDefaultModelPath(context: Context): String? {
            ensureBundledModel(context)?.let { return it }
            val found = listAvailableModels(context).firstOrNull()
            return found
        }
    }
}
