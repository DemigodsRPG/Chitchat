package com.demigodsrpg.chitchat

import com.demigodsrpg.chitchat.tag.ChatScope
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerQuitEvent

class ChatListener(private val INST: Chitchat) : Listener {

    // -- BUKKIT CHAT LISTENER -- //

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onChat(chat: AsyncPlayerChatEvent) {
        if (INST.MUTE_MAP.keys.contains(chat.player.uniqueId.toString())) {
            chat.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onFinalChat(chat: AsyncPlayerChatEvent) {
        Chitchat.sendMessage(INST.FORMAT.getFormattedMessage(chat.player, ChatScope.LOCAL, chat.message),
                chat.recipients)
        if (Chitchat.INST.USE_REDIS && !INST.FORMAT.shouldCancelRedis(chat.player)) {
            RChitchat.REDIS_CHAT.publishAsync(RChitchat.serverId + "$" +
                    INST.FORMAT.getSerializedMessage(chat.player, ChatScope.CHANNEL, chat.message))
        }
        chat.recipients.clear()
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPreprocessCommand(command: PlayerCommandPreprocessEvent) {
        val player = command.player
        val commandMsg = command.message.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        // Muted commands
        if (INST.MUTE_MAP.keys.contains(player.uniqueId.toString())) {
            if (Chitchat.INST.MUTED_COMMANDS.contains(commandMsg[0].toLowerCase().substring(1))) {
                command.isCancelled = true
                player.sendMessage(ChatColor.RED.toString() + "I'm sorry " + player.name +
                        ", I'm afraid I can't do that.")
            }
        } else if (Chitchat.INST.OVERRIDE_ME && commandMsg.size > 1 && commandMsg[0] == "/me") { // /me <message>
            command.isCancelled = true
            if (Chitchat.INST.MUTED_COMMANDS.contains("me") && INST.MUTE_MAP.keys.
                    contains(player.uniqueId.toString())) {
                player.sendMessage(ChatColor.RED.toString() + "I'm sorry " + player.name +
                        ", I'm afraid I can't do that.")
            } else {
                var message = command.message.substring(1)
                message = ChatColor.ITALIC.toString() + ChatColor.stripColor(player.displayName + " " +
                        message.substring(3))
                Chitchat.sendMessage(TextComponent(message))
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onQuit(event: PlayerQuitEvent) {
        // Remove all useless reply data
        val playerName = event.player.name
        INST.REPLY_MAP.remove(playerName)
        if (INST.REPLY_MAP.containsValue(playerName)) {
            INST.REPLY_MAP.entries.stream().filter({ entry -> entry.value == playerName }).
                    forEach { entry -> INST.REPLY_MAP.remove(entry.key) }
        }
    }
}
