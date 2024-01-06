package org.ecorous.synapse

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.utils.env
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent

object SynapseBot {
    private val TOKEN = env("TOKEN")

    @OptIn(PrivilegedIntent::class)
    suspend fun bot() {
        val bot = ExtensibleBot(TOKEN) {
            extensions {
                add(::SynapseExtension)
            }
            intents {
                +Intent.MessageContent
            }
            chatCommands {
                enabled = true
                defaultPrefix = ".syna"
            }
        }
        bot.start()
    }
}

suspend fun Message.tryReply(messageContent: String) {
    try {
        this.reply { content = messageContent }
    } catch (e: Exception) {
        this.channel.createMessage(messageContent)
    }
}