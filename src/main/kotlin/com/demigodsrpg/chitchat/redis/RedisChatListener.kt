package com.demigodsrpg.chitchat.redis

import com.demigodsrpg.chitchat.RChitchat
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.chat.ComponentSerializer
import org.bukkit.Bukkit
import org.redisson.core.MessageListener

class RedisChatListener : MessageListener<String> {
    override fun onMessage(ignored: String, message: String?) {
        if (message != null && !message.startsWith(RChitchat.serverId + "$")) {
            val component = TextComponent(*ComponentSerializer.parse(message))
            for (player in Bukkit.getOnlinePlayers()) {
                player.spigot().sendMessage(component)
            }
        }
    }
}
