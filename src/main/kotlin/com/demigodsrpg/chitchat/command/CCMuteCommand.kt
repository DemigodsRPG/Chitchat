package com.demigodsrpg.chitchat.command

import com.demigodsrpg.chitchat.Chitchat
import com.demigodsrpg.chitchat.util.JsonFileUtil
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.OfflinePlayer
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

class CCMuteCommand(private val INST: Chitchat, private val JSON: JsonFileUtil) : TabExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender is Player && sender.hasPermission("chitchat.mute")) {
            if (args.isNotEmpty()) {
                if (command.name == "ccmute") {
                    if ("list".equals(args[0], ignoreCase = true)) {
                        sender.performCommand("ccmutelist")
                        return true
                    }
                    Bukkit.getScheduler().scheduleAsyncDelayedTask(INST) {
                        val mutedId = Bukkit.getOfflinePlayer(args[0]).uniqueId.toString()
                        if (!INST.MUTE_MAP.containsKey(mutedId)) {
                            try {
                                INST.MUTE_MAP.put(mutedId, System.currentTimeMillis().toDouble() +
                                        argsToMilliseconds(args))
                                if (Chitchat.savingMutes) {
                                    JSON.saveToFile("mutes", INST.MUTE_MAP)
                                }
                                sender.sendMessage(ChatColor.YELLOW.toString() + "Muted " + args[0])
                            } catch (oops: IllegalArgumentException) {
                                sender.sendMessage(ChatColor.RED.toString() + args[2].toUpperCase() +
                                        " is an unsupported unit of time, try again.")
                            } catch (oops: Exception) {
                                sender.sendMessage(ChatColor.RED.toString() + args[0] + " does not exist, try again.")
                            }

                        } else {
                            sender.sendMessage(ChatColor.RED.toString() + "That player is already muted.")
                        }
                    }
                } else {
                    Bukkit.getScheduler().scheduleAsyncDelayedTask(INST) {
                        val mutedId = Bukkit.getOfflinePlayer(args[0]).uniqueId.toString()
                        if (INST.MUTE_MAP.containsKey(mutedId)) {
                            try {
                                INST.MUTE_MAP.remove(mutedId)
                                sender.sendMessage(ChatColor.YELLOW.toString() + "Unmuted " + args[0])
                            } catch (oops: Exception) {
                                sender.sendMessage(ChatColor.RED.toString() + args[0] + " does not exist, try again.")
                            }

                        } else {
                            sender.sendMessage(ChatColor.RED.toString() + "That player isn't currently muted.")
                        }
                    }
                }
            } else {
                return false
            }
        } else {
            sender.sendMessage(ChatColor.RED.toString() + "You don't have permission to use that command.")
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        val guess = ArrayList<String>()
        if (sender is Player && sender.hasPermission("chitchat.mute")) {
            if (args.size == 1) {
                if (command.name == "ccunmute") {
                    guess.addAll(INST.MUTE_MAP.keys.stream().map(UUID::fromString).
                            map(Bukkit::getOfflinePlayer).filter({ muted ->
                        muted.name != null && muted.name.
                                toLowerCase().startsWith(args[0].toLowerCase())
                    }).collect(Collectors.toList()).
                            map(OfflinePlayer::getName))
                } else {
                    guess.addAll(Bukkit.getOnlinePlayers().stream().
                            filter({ online -> online.name.toLowerCase().startsWith(args[0].toLowerCase()) }).
                            map(Player::getName).collect(Collectors.toList()))
                }
            }
        }
        return guess
    }

    @Throws(IllegalArgumentException::class)
    private fun argsToMilliseconds(args: Array<String>): Long {
        try {
            // Establish the value
            var `val` = Integer.valueOf(args[1])!!.toLong()

            // Only accept values in these bounds
            if (`val` > 0 || `val` <= 600) {
                // Grab the unit
                var unit = args[2]
                if (!unit.toUpperCase().endsWith("S")) {
                    unit += "S"
                }

                if (unit.equals("WEEKS", ignoreCase = true)) {
                    unit = "DAYS"
                    `val` *= 7
                } else if (unit.equals("YEARS", ignoreCase = true)) {
                    unit = "DAYS"
                    `val` *= 365
                }

                // Convert to milliseconds
                return TimeUnit.valueOf(unit.toUpperCase()).toMillis(`val`)
            }
        } catch (ignored: ArrayIndexOutOfBoundsException) {
        } catch (ignored: NumberFormatException) {
        }

        // Default to 15 minutes
        return 900_000L
    }
}
