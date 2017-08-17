package com.demigodsrpg.chitchat

import com.demigodsrpg.chitchat.command.CCMsgCommand
import com.demigodsrpg.chitchat.command.CCMuteCommand
import com.demigodsrpg.chitchat.command.CCMuteListCommand
import com.demigodsrpg.chitchat.command.CCReloadCommand
import com.demigodsrpg.chitchat.format.ChatFormat
import com.demigodsrpg.chitchat.tag.DefaultPlayerTag
import com.demigodsrpg.chitchat.tag.NameTag
import com.demigodsrpg.chitchat.tag.SpecificPlayerTag
import com.demigodsrpg.chitchat.tag.WorldPlayerTag
import com.demigodsrpg.chitchat.util.JsonFileUtil
import com.demigodsrpg.chitchat.util.LibraryHandler
import com.demigodsrpg.chitchat.util.TitleUtil
import com.google.common.collect.ImmutableList
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * The simplest plugin for chitchat.
 */
class Chitchat : JavaPlugin() {

    // -- IMPORTANT OBJECTS -- //

    lateinit var FORMAT: ChatFormat
    lateinit var LIBRARIES: LibraryHandler
    lateinit var JSON: JsonFileUtil
    lateinit var TITLE: TitleUtil

    // -- IMPORTANT LISTS -- //
    lateinit var MUTE_MAP: ConcurrentMap<String, Double>
    lateinit var REPLY_MAP: ConcurrentMap<String, String>

    // -- OPTIONS -- //

    var OVERRIDE_ME: Boolean = true
    var USE_REDIS: Boolean = false
    var SAVE_MUTES: Boolean = true
    lateinit var MUTED_COMMANDS: List<String>

    // -- BUKKIT ENABLE/DISABLE -- //

    override fun onEnable() {
        // Define static objects
        INST = this
        FORMAT = ChatFormat()
        LIBRARIES = LibraryHandler(this)

        // Handle local data saves
        JSON = JsonFileUtil(dataFolder, true)

        // Setup TitleUtil
        try {
            TITLE = TitleUtil()
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: InstantiationException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        }

        // Handle config
        config.options().copyDefaults(true)
        saveConfig()

        // Override /me
        OVERRIDE_ME = config.getBoolean("override_me", true)

        // Muted commands
        MUTED_COMMANDS = ImmutableList.copyOf(config.getStringList("muted-commands"))

        // Default tags
        if (config.getBoolean("use_examples", true)) {
            val admin = TextComponent("[A]")
            admin.color = net.md_5.bungee.api.ChatColor.DARK_RED;
            admin.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentBuilder("Administrator").
                    color(net.md_5.bungee.api.ChatColor.DARK_RED).create());
            val dev = TextComponent("[D]")
            dev.color = net.md_5.bungee.api.ChatColor.DARK_GRAY
            dev.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentBuilder("Developer").
                    color(net.md_5.bungee.api.ChatColor.DARK_GRAY).create())
            FORMAT.add(WorldPlayerTag())
                    .add(DefaultPlayerTag("example-prefix", "chitchat.admin", admin, 3))
                    .add(SpecificPlayerTag("hqm", "HmmmQuestionMark", dev, 3))
                    .add(SpecificPlayerTag("hqm2", "AitchqueM", dev, 3))
                    .add(SpecificPlayerTag("hqm3", "hqm", dev, 3))
                    .add(NameTag())
        }

        // Register events
        server.pluginManager.registerEvents(ChatListener(this), this)

        // Register commands
        val reloadCommand = CCReloadCommand(this)
        val muteListCommand = CCMuteListCommand(this)
        val muteCommand = CCMuteCommand(this, JSON)
        val msgCommand = CCMsgCommand(this)
        getCommand("ccreload").executor = reloadCommand
        getCommand("ccmutelist").executor = muteListCommand
        getCommand("ccmute").executor = muteCommand
        getCommand("ccmute").tabCompleter = muteCommand
        getCommand("ccunmute").executor = muteCommand
        getCommand("ccunmute").tabCompleter = muteCommand
        getCommand("ccmsg").executor = msgCommand
        getCommand("ccreply").executor = msgCommand

        // Will we use redis?
        USE_REDIS = config.getBoolean("redis.use", true)

        // Redis stuff
        if (USE_REDIS) {
            // Add the required libraries
            LIBRARIES.addMavenLibrary(LibraryHandler.MAVEN_CENTRAL, Depends.ORG_REDISSON,
                    Depends.REDISSON, Depends.REDISSON_VER)
            LIBRARIES.addMavenLibrary(LibraryHandler.MAVEN_CENTRAL, Depends.ORG_SLF4J,
                    Depends.SLF4J_API, Depends.SLF4J_API_VER)
            LIBRARIES.addMavenLibrary(LibraryHandler.MAVEN_CENTRAL, Depends.COM_ESOTERICSOFTWARE,
                    Depends.KYRO, Depends.KYRO_VER)
            LIBRARIES.addMavenLibrary(LibraryHandler.MAVEN_CENTRAL, Depends.COM_FASTERXML_JACKSON_CORE,
                    Depends.JACKSON_CORE, Depends.JACKSON_VER)
            LIBRARIES.addMavenLibrary(LibraryHandler.MAVEN_CENTRAL, Depends.COM_FASTERXML_JACKSON_CORE,
                    Depends.JACKSON_DATABIND, Depends.JACKSON_VER)

            // Setup redis related stuff
            RChitchat(this)
        }

        if (!USE_REDIS) {
            // Setup mute list
            MUTE_MAP = ConcurrentHashMap()

            // Setup private message map
            REPLY_MAP = ConcurrentHashMap()
        }

        // Handle mute settings
        SAVE_MUTES = config.getBoolean("save_mutes", false)
        if (savingMutes) {
            try {
                MUTE_MAP = ConcurrentHashMap(JSON.loadFromFile("mutes")) as ConcurrentMap<String, Double>
            } catch (oops: Exception) {
                logger.severe("Unable to load saved mutes, did someone tamper with the data?")
            }
        }

        // Clean up old mutes (only one server should do this to avoid unnecessary threads
        if (!USE_REDIS || config.getBoolean("redis.clean_old_mutes", false)) {
            Bukkit.getScheduler().scheduleAsyncRepeatingTask(this, {
                MUTE_MAP.entries.stream().
                        filter { entry -> entry.value < System.currentTimeMillis() }.
                        forEach { entry -> MUTE_MAP.remove(entry.key) }
            }, 30, 30)
        }
    }

    override fun onDisable() {
        // Manually unregister events
        HandlerList.unregisterAll(this)

        // Save mutes
        if (savingMutes) {
            JSON.saveToFile("mutes", MUTE_MAP)
        }
    }

    companion object {

        // -- INSTANCE -- //

        lateinit var INST: Chitchat

        // -- STATIC API METHODS -- //

        /**
         * Send a title message to a player.
         *
         * @param player       The player receiving the message.
         * @param fadeInTicks  The ticks the message takes to fade in.
         * @param stayTicks    The ticks the message stays on screen (sans fades).
         * @param fadeOutTicks The ticks the message takes to fade out.
         * @param title        The title text.
         * @param subtitle     The subtitle text.
         */
        fun sendTitle(player: Player, fadeInTicks: Int, stayTicks: Int, fadeOutTicks: Int, title: String,
                      subtitle: String) {
            if (INST.TITLE != null) {
                INST.TITLE.sendTitle(player, fadeInTicks, stayTicks, fadeOutTicks, title, subtitle)
            }
        }

        /**
         * Clear or reset the title data for a specified player.
         *
         * @param player The player being cleared/reset.
         * @param reset  True if reset, false for clear.
         */
        fun clearTitle(player: Player, reset: Boolean) {
            if (INST.TITLE != null) {
                INST.TITLE.clearTitle(player, reset)
            }
        }

        /**
         * Send a message through the Chitchat plugin. Includes the redis chat channel.
         *
         * @param message The message to be sent.
         */
        fun sendMessage(message: BaseComponent) {
            if (INST.USE_REDIS) {
                RChitchat.REDIS_CHAT.publish(RChitchat.serverId + "$" + message.toLegacyText());
            }
            sendMessage(message, Bukkit.getServer().onlinePlayers)
        }

        /**
         * Send a message through the Chitchat plugin, exclusive to a list of recipients.
         *
         * @param message    The message to be sent.
         * @param recipients The recipients of this message.
         */
        fun sendMessage(message: BaseComponent, recipients: Collection<Player>) {
            for (player in recipients) {
                player.spigot().sendMessage(message)
            }
        }

        /**
         * Send a message through the Chitchat plugin. Includes the redis chat channel.
         *
         * @param message The message to be sent.
         * @deprecated This method is depreciated in favor of the new BaseComponent based method.
         */
        fun sendMessage(message: String) {
            val component = TextComponent()
            component.extra = ArrayList(TextComponent.fromLegacyText(message).asList())
            sendMessage(component)
        }

        // -- OPTION GETTERS -- //

        val usingRedis: Boolean
            get() = INST.USE_REDIS

        val savingMutes: Boolean
            get() = !INST.USE_REDIS && INST.SAVE_MUTES
    }
}