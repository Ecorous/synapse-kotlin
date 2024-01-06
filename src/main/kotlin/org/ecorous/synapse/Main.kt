package org.ecorous.synapse

import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.entity.Guild
import dev.kord.core.entity.User
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

@Serializable
data class TokenGenResponse(val length: Int, val token: String)

@Serializable
data class GenerateChunk(
    val model: String,
    @SerialName("created_at")
    val createdAt: String,
    val response: String,
    val done: Boolean,
    val context: List<Int> = listOf(),
    @SerialName("total_duration")
    val totalDuration: Long? = null,
    @SerialName("load_duration")
    val loadDuration: Long? = null,
    @SerialName("prompt_eval_count")
    val promptEvalCount: Int? = null,
    @SerialName("prompt_eval_duration")
    val promptEvalDuration: Long? = null,
    @SerialName("eval_count")
    val evalCount: Int? = null,
    @SerialName("eval_duration")
    val evalDuration: Long? = null,
)

@Serializable
data class Response(val output: String, val thoughts: String)

const val BASE_URL = "http://localhost:11434"
const val MODEL = "mistral:7b"

data class HistoryItem(val name: String, val message: String)

suspend fun finalPrompt(author: User, channel: MessageChannelBehavior, guild: Guild?): String {
    val systemPrompt = client.get("https://raw.githubusercontent.com/Ecorous/synapse-kotlin/main/synapse.system.txt").body<String>()

    return systemPrompt.replace("{guildLine}", if (guild != null ) "The server you are in is called ${guild.name}" else "You are currently in a private DM")
        .replace("{channelMention}", channel.mention)
        .replace("{authorName}", author.username)
        .replace("{history}", channel.history().parsable())
}

fun MessageChannelBehavior.history(): MutableList<HistoryItem> {
    if (history[this.id.value.toLong()] == null) {
        history[this.id.value.toLong()] = mutableListOf()
    }
    return history[this.id.value.toLong()] ?: mutableListOf()
}

var history = mutableMapOf<Long, MutableList<HistoryItem>>()

fun List<HistoryItem>.parsable(): String {
    var final = ""
    forEach { item ->
        final += "${item.name}: ${item.message.filterNot { it == '\n' }}\n\n"
    }
    return final
}

fun List<HistoryItem>.synapse(): List<String> {
    return this.filter { it.name == "Synapse" }.map { it.message }
}

val client = HttpClient(CIO) {
    headers {
        append(HttpHeaders.ContentType, "application/json")
        append(HttpHeaders.Accept, "application/json")
        append(HttpHeaders.UserAgent, "synapseai-kotlin/1.0")
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 100000
    }
}

val logger: org.slf4j.Logger = LoggerFactory.getLogger("synapse-ai")

suspend fun generate(prompt: String, author: User, channel: MessageChannelBehavior, guild: Guild?): String {
    val finalSystem = finalPrompt(author, channel, guild)

    val url = "$BASE_URL/api/generate"
    val payload = mapOf("model" to MODEL, "prompt" to prompt, "system" to finalSystem)
    val response = client.post(url) {
        setBody(Json.encodeToString(payload))
    }
    val b = response.body<String>()
    var final = ""
    b.lines().forEach { line ->
        if (line.isEmpty()) return@forEach
        val g = Json.decodeFromString<GenerateChunk>(line).response
        final += g
    }
    return final
}

suspend fun main() {

    logger.info("synapse...")
    val response = client.get("https://tokengen.ecorous.org/generate/256")
    val r = Json.decodeFromString<TokenGenResponse>(response.body())
    logger.info("Unique token for this launch: ${r.token}")
    SynapseBot.bot()
}