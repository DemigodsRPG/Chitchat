package com.demigodsrpg.chitchat.command

import com.demigodsrpg.chitchat.Chitchat
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import java.text.SimpleDateFormat
import java.util.*

class CCMuteListCommand(private val INST: Chitchat) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (command.name == "ccmutelist") {
            if (!INST.MUTE_MAP.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW.toString() + "// -- Currently Muted Players -- //")
                for ((key, value) in INST.MUTE_MAP) {
                    val mutedName = Bukkit.getOfflinePlayer(UUID.fromString(key)).name
                    val mutedDate = prettyDate(value.toLong())
                    sender.sendMessage(ChatColor.YELLOW.toString() + "  - " + ChatColor.WHITE + (mutedName ?: key) +
                            mutedDate)
                }
            } else {
                sender.sendMessage(ChatColor.RED.toString() + "There are currently no muted players.")
            }
        }
        return true
    }

    private fun prettyDate(time: Long): String {
        val diff = (time - System.currentTimeMillis()) / 1000
        val dayDiff = Math.round((diff / 86400).toFloat())

        var ret = ChatColor.YELLOW.toString() + " for " + ChatColor.WHITE

        if (dayDiff == 0 && diff < 50) {
            ret += "less than a minute."
        } else if (diff < 60) {
            ret += "a minute."
        } else if (diff < 120) {
            ret += "a couple minutes."
        } else if (diff < 3300) {
            ret += "~" + Math.round((diff / 60).toFloat()) + " minutes."
        } else if (diff < 7200) {
            ret += "an hour."
        } else if (diff < 82800) {
            ret += Math.round((diff / 3600).toFloat()).toString() + " hours."
        } else if (dayDiff <= 1) {
            ret += "a day."
        } else if (dayDiff < 7) {
            ret += "~$dayDiff days."
        } else if (dayDiff < 28) {
            ret += "~" + Math.ceil((dayDiff / 7).toDouble()).toInt() + " weeks."
        } else {
            ret = ChatColor.YELLOW.toString() + " until " + ChatColor.WHITE + SimpleDateFormat("EEE, MMM dd, yyyy.").
                    format(Date(time))
        }

        return ret
    }
}
