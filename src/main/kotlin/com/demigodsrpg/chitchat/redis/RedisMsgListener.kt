package com.demigodsrpg.chitchat.redis

import com.demigodsrpg.chitchat.Chitchat
import com.demigodsrpg.chitchat.PrivateMessage
import org.bukkit.Bukkit
import org.redisson.core.MessageListener

class RedisMsgListener(private val INST: Chitchat) : MessageListener<String> {
    override fun onMessage(ignored: String, json: String) {
        val message = PrivateMessage(json)
        INST.logger.info(message.getLogMessage())
        val offline = Bukkit.getOfflinePlayer(message.target)
        if (offline.isOnline) {
            offline.player.sendMessage(message.getFormattedMessage(false))
        }
    }
}