package org.ecorous.synapse

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.core.behavior.channel.withTyping
import dev.kord.core.behavior.reply
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.serialization.json.Json

class SynapseExtension : Extension() {
    override val name = "synapse"
    private val channelsListening = mutableMapOf<Long, Boolean>()
    override suspend fun setup() {
        event<MessageCreateEvent> {
            action {
                val message = event.message
                if (message.author?.isBot == true) return@action
                if (message.content.startsWith("Syna, ")) {
                    channelsListening[message.channel.id.value.toLong()] = true
                } else if (message.content.startsWith(".syna quiet")) {
                    channelsListening[message.channel.id.value.toLong()] = false
                    message.reply {
                        content = "[cmd] Okay, I'll stop listening to this channel."
                    }
                    return@action
                } else if (message.content.startsWith(".syna activate")) {
                    channelsListening[message.channel.id.value.toLong()] = true
                    message.tryReply("[cmd] Okay, I'll start listening to this channel.")
                    return@action
                } else if (message.content.startsWith(".syna everything is a bit foggy")) {
                    message.channel.history().clear()
                    message.tryReply("[cmd] *Bonk!* What were we talking about again?")
                    return@action
                } else if (message.content.startsWith(".syna history")) {
                    logger.info(message.channel.history().toString())
                    message.tryReply("[cmd] Here's the conversation history:\n\n" + message.channel.history().parsable())
                    return@action
                } else if (message.content.startsWith(".syna pse's messages")) {
                    message.tryReply("[cmd] Here's what I've said:\n\n" + message.channel.history().synapse().joinToString("\n\n"))
                    return@action
                } else if (message.content.startsWith(".syna help")) {
                    message.tryReply("[cmd] Here's a list of commands:\n\n" +
                            ".syna activate - I'll start listening to this channel.\n" +
                            ".syna quiet - I'll stop listening to this channel.\n" +
                            ".syna everything is a bit foggy - I'll forget everything we've talked about.\n" +
                            ".syna history - I'll show you the conversation history.\n" +
                            ".syna pse's messages - I'll show you what I've said.\n" +
                            ".syna help - I'll show you this message.")
                    return@action
                }
                if (channelsListening[message.channel.id.value.toLong()] != true) return@action

                val prompt = message.content.substringAfter("Syna, ").trim()
                message.channel.history().add(HistoryItem(message.author!!.username, prompt))
                var generated: String
                message.channel.withTyping {
                    generated = generate(prompt, message.author!!, message.channel, message.getGuildOrNull())
                }

                logger.info("Generated: $generated")
                var response = generated
                try {
                    val output = Json.decodeFromString<Response>(generated)
                    response = output.output
                } catch (e: Exception) {
                    logger.error("Error decoding JSON: ${e.message}")
                }
                message.channel.history().add(HistoryItem("Synapse", response))
                message.tryReply(response)
            }
        }
    }
}