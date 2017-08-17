package com.demigodsrpg.chitchat.command

import com.demigodsrpg.chitchat.Chitchat
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class CCReloadCommand(private val INST: Chitchat) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender.hasPermission("chitchat.reload")) {
            Bukkit.getServer().pluginManager.disablePlugin(INST)
            Bukkit.getServer().pluginManager.enablePlugin(INST)
        } else {
            sender.sendMessage(ChatColor.RED.toString() + "You don't have permission to use that command.")
        }
        return true
    }
}