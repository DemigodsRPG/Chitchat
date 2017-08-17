package com.demigodsrpg.chitchat.util

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player

import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

class TitleUtil @Throws(ClassNotFoundException::class, NoSuchMethodException::class, IllegalAccessException::class,
        InstantiationException::class, NoSuchFieldException::class)
constructor() {
    private val NMS: String
    private val CB: String

    private val CB_CRAFTPLAYER: Class<out Player>
    private val NMS_ENTITY_PLAYER: Class<*>
    private val NMS_PLAYER_CONN: Class<*>
    private val NMS_ICHAT_BASE: Class<*>
    private val NMS_PACKET: Class<*>
    private val NMS_PACKET_PLAY_TITLE: Class<*>
    private val NMS_TITLE_ACTION: Class<out Enum<*>>
    private val NMS_CHAT_SERIALIZER: Class<*>

    private val GET_HANDLE: Method
    private val SEND_PACKET: Method
    private val ICHAT_A: Method

    private val PLAYER_CONN: Field

    private val ACTION_ARRAY: Array<Any>

    init {
        val name = Bukkit.getServer().javaClass.`package`.name
        val version = name.substring(name.lastIndexOf('.') + 1) + "."

        // Common classpaths
        NMS = "net.minecraft.server." + version
        CB = "org.bukkit.craftbukkit." + version

        // Classes being used
        CB_CRAFTPLAYER = Class.forName(CB + "entity.CraftPlayer") as Class<out Player>
        NMS_ENTITY_PLAYER = Class.forName(NMS + "EntityPlayer")
        NMS_PLAYER_CONN = Class.forName(NMS + "PlayerConnection")
        NMS_ICHAT_BASE = Class.forName(NMS + "IChatBaseComponent")
        NMS_PACKET = Class.forName(NMS + "Packet")
        NMS_PACKET_PLAY_TITLE = Class.forName(NMS + "PacketPlayOutTitle")
        NMS_TITLE_ACTION = Class.forName(NMS + "PacketPlayOutTitle\$EnumTitleAction") as Class<out Enum<*>>
        NMS_CHAT_SERIALIZER = Class.forName(NMS + "IChatBaseComponent\$ChatSerializer")

        // Methods being used
        GET_HANDLE = CB_CRAFTPLAYER.getMethod("getHandle")
        SEND_PACKET = NMS_PLAYER_CONN.getMethod("sendPacket", NMS_PACKET)
        ICHAT_A = NMS_CHAT_SERIALIZER.getMethod("a", String::class.java)

        // Fields being used
        PLAYER_CONN = NMS_ENTITY_PLAYER.getDeclaredField("playerConnection")

        // Title action array
        ACTION_ARRAY = NMS_TITLE_ACTION.enumConstants as Array<Any>
    }

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
    fun sendTitle(player: Player, fadeInTicks: Int, stayTicks: Int, fadeOutTicks: Int, title: String?,
                  subtitle: String?) {
        var title = title
        var subtitle = subtitle
        try {
            clearTitle(player, true)

            val craftPlayer = CB_CRAFTPLAYER.cast(player)
            val entityPlayer = GET_HANDLE.invoke(craftPlayer)
            val connection = PLAYER_CONN.get(entityPlayer)

            val packetPlayOutTimes = NMS_PACKET_PLAY_TITLE.getConstructor(NMS_TITLE_ACTION, NMS_ICHAT_BASE,
                    Integer.TYPE, Integer.TYPE, Integer.TYPE).newInstance(ACTION_ARRAY[2], null, fadeInTicks,
                    stayTicks, fadeOutTicks)
            SEND_PACKET.invoke(connection, packetPlayOutTimes)

            if (subtitle != null) {
                subtitle = subtitle.replace("%player%".toRegex(), player.displayName)
                subtitle = ChatColor.translateAlternateColorCodes('&', subtitle)
                val titleSub = ICHAT_A.invoke(null, "{\"text\": \"$subtitle\"}")
                val packetPlayOutSubTitle = NMS_PACKET_PLAY_TITLE.getConstructor(NMS_TITLE_ACTION, NMS_ICHAT_BASE).
                        newInstance(ACTION_ARRAY[1], titleSub)
                SEND_PACKET.invoke(connection, packetPlayOutSubTitle)
            }

            if (title != null) {
                title = title.replace("%player%".toRegex(), player.displayName)
                title = ChatColor.translateAlternateColorCodes('&', title)
                val titleMain = ICHAT_A.invoke(null, "{\"text\": \"$title\"}")
                val packetPlayOutTitle = NMS_PACKET_PLAY_TITLE.getConstructor(NMS_TITLE_ACTION, NMS_ICHAT_BASE).
                        newInstance(ACTION_ARRAY[0], titleMain)
                SEND_PACKET.invoke(connection, packetPlayOutTitle)
            }
        } catch (oops: IllegalAccessException) {
            oops.printStackTrace()
        } catch (oops: InvocationTargetException) {
            oops.printStackTrace()
        } catch (oops: NoSuchMethodException) {
            oops.printStackTrace()
        } catch (oops: InstantiationException) {
            oops.printStackTrace()
        }
    }

    /**
     * Clear or reset the title data for a specified player.
     *
     * @param player The player being cleared/reset.
     * @param reset  True if reset, false for clear.
     */
    fun clearTitle(player: Player, reset: Boolean) {
        try {
            val craftPlayer = CB_CRAFTPLAYER.cast(player)
            val entityPlayer = GET_HANDLE.invoke(craftPlayer)
            val connection = PLAYER_CONN.get(entityPlayer)

            val packetPlayOutClear = NMS_PACKET_PLAY_TITLE.getConstructor(NMS_TITLE_ACTION, NMS_ICHAT_BASE).
                    newInstance(ACTION_ARRAY[if (reset) 4 else 3], null)
            SEND_PACKET.invoke(connection, packetPlayOutClear)
        } catch (oops: Exception) {
            oops.printStackTrace()
        }
    }
}
