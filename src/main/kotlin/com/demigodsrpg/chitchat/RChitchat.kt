package com.demigodsrpg.chitchat

import com.demigodsrpg.chitchat.redis.RedisChatListener
import com.demigodsrpg.chitchat.redis.RedisMsgListener
import org.bukkit.event.Listener
import org.redisson.Config
import org.redisson.Redisson
import org.redisson.core.RTopic


class RChitchat(cc: Chitchat) : Listener {
    private val REDIS: Redisson

    init {
        // Define the instance
        inst = this

        // Get the server's id and chat channel
        SERVER_ID = cc.config.getString("redis.server_id", "minecraft")
        SERVER_CHANNEL = cc.config.getString("redis.channel", "default")

        // Configure and connect to redis
        val config = Config()
        config.useSingleServer().setAddress(cc.config.getString("redis.connection", "127.0.0.1:6379"))
        REDIS = Redisson.create(config)

        // Setup mute map
        cc.MUTE_MAP = REDIS.getMap("mute.map")

        // Setup reply map
        cc.REPLY_MAP = REDIS.getMap("reply.map")

        // Setup msg topic
        REDIS_MSG = REDIS.getTopic("msg.topic")

        // Setup chat topic
        REDIS_CHAT = REDIS.getTopic(SERVER_CHANNEL + "$" + "chat.topic")

        // Make sure everything connected, if not, disable the plugin
        try {
            // Check if redis connected
            REDIS.getBucket<Any>("test").exists()
            cc.logger.info("Redis connection was successful.")

            // Register the msg listener
            REDIS_CHAT.addListener(RedisChatListener())

            // Register the msg listener
            REDIS_MSG.addListener(RedisMsgListener(cc))
        } catch (ignored: Exception) {
            cc.logger.severe("Redis connection was unsuccessful!")
            cc.logger.severe("Disabling all Redis features.")
            cc.USE_REDIS = false
        }
    }

    companion object {
        // -- REDIS DATA -- //
        lateinit var inst: RChitchat
        lateinit internal var REDIS_CHAT: RTopic<String>
        lateinit internal var REDIS_MSG: RTopic<String>

        // -- OPTIONS -- //

        lateinit private var SERVER_CHANNEL: String
        lateinit private var SERVER_ID: String

        val serverChannel: String
            get() = SERVER_CHANNEL

        val serverId: String
            get() = SERVER_ID
    }
}
