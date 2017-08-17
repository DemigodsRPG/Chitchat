package com.demigodsrpg.chitchat

import com.google.gson.GsonBuilder
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import java.io.Serializable
import java.util.*

class PrivateMessage : Serializable {

    // -- TRANSIENT DATA -- //

    @Transient
    private lateinit var INST: Chitchat

    // -- META DATA -- //

    var target: String
    var sender: String
    var message: String

    // -- CONSTRUCTORS -- //

    constructor(json: String) {
        val map = GsonBuilder().create().fromJson(json, Map::class.java)
        target = map.get("target").toString()
        sender = map.get("sender").toString()
        message = map.get("message").toString()
    }

    constructor(target: String, sender: String, message: String) {
        this.target = target
        this.sender = sender
        this.message = message
    }

    // -- GETTERS -- //

    fun getFormattedMessage(out: Boolean): String {
        return ChatColor.DARK_GRAY.toString() + "PM" + (if (out) " to" else " from") + " <" + ChatColor.DARK_AQUA +
                (if (out) target else sender) + ChatColor.DARK_GRAY + ">: " + ChatColor.GRAY + message
    }

    fun getLogMessage(): String {
        return "PM <$sender to $target>: $message"
    }

    fun toJson(): String {
        val map = HashMap<String, Any>()
        map.put("target", target)
        map.put("sender", sender)
        map.put("message", message)
        return GsonBuilder().create().toJson(map, Map::class.java)
    }

    // -- SEND -- //

    fun send() {
        // Get the sender
        val sender = Bukkit.getOfflinePlayer(this.sender)

        // Check if the player is on this server
        if (Bukkit.getPlayer(target) != null) {
            Bukkit.getPlayer(target).sendMessage(getFormattedMessage(false))
            INST.logger.info(getLogMessage())
        } else if (INST.USE_REDIS) {
            // Nope, send through redis
            RChitchat.REDIS_MSG.publishAsync(toJson())
        } else if (sender.isOnline) {
            // Something went wrong
            sender.player.sendMessage(ChatColor.RED.toString() + "There was an error sending a private message.")
            return;
        }

        // Send the 'sent' message
        if (sender.isOnline) {
            sender.player.sendMessage(getFormattedMessage(true));
        }

        // Add to the reply map
        INST.REPLY_MAP.put(this.sender, this.target)
        INST.REPLY_MAP.put(this.target, this.sender)
    }
}

