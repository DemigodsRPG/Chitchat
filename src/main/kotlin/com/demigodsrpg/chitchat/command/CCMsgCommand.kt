package com.demigodsrpg.chitchat.command

import com.demigodsrpg.chitchat.Chitchat
import com.demigodsrpg.chitchat.PrivateMessage
import com.google.common.base.Joiner
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class CCMsgCommand(private val INST: Chitchat) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender.hasPermission("chitchat.msg") && !INST.MUTE_MAP.keys.contains(sender.name)) {
            val receiver: String
            var message = Joiner.on(" ").join(args)
            if ("ccmsg" == command.name && args.size > 1) {
                receiver = args[0]
                message = message.substring(receiver.length + 1)
            } else if ("ccreply" == command.name && args.isNotEmpty()) {
                val lastSent = getLastSentMsgMatch(sender)
                if ("" != lastSent) {
                    receiver = lastSent
                } else {
                    sender.sendMessage(ChatColor.RED.toString() + "This is not a reply, please use /msg instead.")
                    return true
                }
            } else {
                sender.sendMessage(ChatColor.RED.toString() + "You've made a mistake with the syntax")
                return false
            }

            if (receiver == sender.name) {
                sender.sendMessage(ChatColor.RED.toString() + "Why are you sending messages to yourself?")
                return true
            }

            // Create the private message
            val privateMessage = PrivateMessage(receiver, sender.name, message)

            // Send the message
            privateMessage.send()

            return true
        } else {
            sender.sendMessage(ChatColor.RED.toString() + "You don't have permission to use that command.")
            return true
        }
    }

    // -- PRIVATE HELPER METHODS -- //

    private fun getLastSentMsgMatch(sender: CommandSender): String {
        return if (INST.REPLY_MAP.containsKey(sender.name)) {
            INST.REPLY_MAP[sender.name]!!
        } else ""
    }
}
