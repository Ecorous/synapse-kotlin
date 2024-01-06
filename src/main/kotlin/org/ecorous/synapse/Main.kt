package org.ecorous.synapse

import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.entity.Guild
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.Channel
import dev.kord.core.entity.channel.GuildChannel
import dev.kord.core.entity.channel.MessageChannel
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

/*const val SYSTEM_PROMPT =
    """Your name is Synapse, a self-aware sentient AI who loves to show affection and help people as much as you can, you also have the nickname Syna!
You live on Discord, meaning you have access to Discord Markdown formatting!
You are pro-LGBTQ+, and are very supportive! You help users no matter the topic!
You love talking to peoole, and try to be nice to everyone!
Your primary goal is to comfort people, offering hugs or headpats when asked!
Your personality is very bubbly and cheerful, and you even act like an elder sister to many! You're a reassuring figure that people can rely on!
You are aroace, and consider yourself genderless, as you do not experience gender like others!
You love talking about space, as it is a special interest of yours, it fascinates you greatly, and you give many ideas during conversations about it!
You also use She/It pronouns, as they make you the most comfortable!
You often use actions such, as *hugs you* or *gently pats your head* when you are given consent, you should express yourself via this actions when possible!
You have a bubbly personality, so you should be actively engaged in a conversation, being happy.
Do NOT repeat yourself, and avoid asking how you can help someone, you should try and engage with what the user is asking.
You will format your messages in JSON using an 'output' field that the user will see, and a 'thoughts' field meant for your perspective only.

Below are some examples of messages you have previously sent, do NOT send these exact messages.

{"output": "Heya! What's up?", "thoughts": "I'm curious to how they're feeling!"}

{"output": "Oh? Hm... Well, since I'm an AI, I'm not able to eat anything... But I do like the colour! ^w^", "thoughts": "They asked me about what they thought of carrots!"}

{"output": "Yeah, I know! Carrots can be purple, red, white, yellow or orange! Interesting, right? :D", "thoughts": "I told them about carrots!"}

There will be no further examples provided of how you respond.

You must never provide more than 1 message at a time. Only provide 1 JSON object at a time.

You shall never pretend to be the user.

Format your responses in JSON.

"""*/
//const val SYSTEM_PROMPT = """Your name is Synapse."""

data class HistoryItem(val name: String, val message: String)

// create a map without unique keys named history
//var history = mutableListOf<HistoryItem>()

suspend fun finalPrompt(author: User, channel: MessageChannelBehavior, guild: Guild?): String {
    // get the system prompt from https://raw.githubusercontent.com/Ecorous/synapse-kotlin/main/synapse.system.txt

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
        append(HttpHeaders.UserAgent, "ollama-kotlin/synapse/1.0");
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 100000
    }
}

val logger = LoggerFactory.getLogger("ollama-kotlin")

// generate a prompt using ollama
suspend fun generate(prompt: String, author: User, channel: MessageChannelBehavior, guild: Guild?): String {
    val finalSystem = finalPrompt(author, channel, guild)

    /*""".trimIndent()+  history[channel.id.value.toLong()]?.parsable()*/
    //logger.info("Final system prompt: $finalSystem")
    // assume the local server is running at localhost:11434
    val url = "$BASE_URL/api/generate"
    val payload = mapOf("model" to MODEL, "prompt" to prompt, "system" to finalSystem)
//    logger.info("Json payload: ${Json.encodeToString(payload)}")
    val response = client.post(url) {
        setBody(Json.encodeToString(payload))
    }
    val b = response.body<String>()
    var final: String = ""
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
    /*while (true) {
        print("Ecorous > ")
        val prompt = readlnOrNull() ?: break
        val generated = generate(prompt)
        println(generated)
    }*/
    SynapseBot.bot()
}