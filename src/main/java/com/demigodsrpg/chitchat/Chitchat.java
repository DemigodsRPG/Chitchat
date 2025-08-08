/*
 * This file is part of Chitchat, licensed under the MIT License (MIT).
 *
 * Copyright (c) DemigodsRPG.com <http://www.demigodsrpg.com>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.demigodsrpg.chitchat;

import com.demigodsrpg.chitchat.command.*;
import com.demigodsrpg.chitchat.format.ChatFormat;
import com.demigodsrpg.chitchat.tag.*;
import com.demigodsrpg.chitchat.util.*;
import com.google.common.collect.ImmutableList;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The simplest plugin for chitchat.
 */
public class Chitchat extends JavaPlugin {

    // -- IMPORTANT OBJECTS -- //

    static Chitchat INST;
    ChatFormat FORMAT;
    JsonFileUtil JSON;
    LibraryHandler LIBRARIES;

    // -- IMPORTANT LISTS -- //
    ConcurrentMap<String, Double> MUTE_MAP;
    ConcurrentMap<String, String> REPLY_MAP;

    // -- OPTIONS -- //

    boolean OVERRIDE_ME;
    boolean USE_REDIS;
    boolean SAVE_MUTES;
    List<String> MUTED_COMMANDS;

    // -- BUKKIT ENABLE/DISABLE -- //

    @SuppressWarnings("unchecked")
    @Override
    public void onEnable() {
        // Define static objects
        INST = this;
        FORMAT = new ChatFormat();
        LIBRARIES = new LibraryHandler(this);

        // Handle local data saves
        JSON = new JsonFileUtil(getDataFolder(), true);

        // Handle config
        getConfig().options().copyDefaults(true);
        saveConfig();

        // Override /me
        OVERRIDE_ME = getConfig().getBoolean("override_me", true);

        // Muted commands
        MUTED_COMMANDS = ImmutableList.copyOf(getConfig().getStringList("muted-commands"));

        // Default tags
        if(getConfig().getBoolean("use_examples", true)) {
            Component admin = Component.text("[A]", NamedTextColor.DARK_RED).
                    hoverEvent(HoverEvent.showText(Component.text("Administrator", NamedTextColor.DARK_RED)));
            Component dev = Component.text("[D]", NamedTextColor.DARK_GRAY).
                    hoverEvent(HoverEvent.showText(Component.text("Developer", NamedTextColor.DARK_GRAY)));
            FORMAT.add(new WorldPlayerTag())
                    .add(new DefaultPlayerTag("example-prefix", "chitchat.admin", admin, 3))
                    .add(new SpecificPlayerTag("hqm", "HmmmQuestionMark", dev, 3))
                    .add(new SpecificPlayerTag("hqm2", "PseudoHQM", dev, 3))
                    .add(new SpecificPlayerTag("hqm3", "HQM", dev, 3))
                    .add(new NameTag());
            }

        // Register events
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        // Register commands
        CCReloadCommand reloadCommand = new CCReloadCommand(this);
        CCMuteListCommand muteListCommand = new CCMuteListCommand(this);
        CCMuteCommand muteCommand = new CCMuteCommand(this, JSON);
        CCMsgCommand msgCommand = new CCMsgCommand(this);
        getCommand("ccreload").setExecutor(reloadCommand);
        getCommand("ccmutelist").setExecutor(muteListCommand);
        getCommand("ccmute").setExecutor(muteCommand);
        getCommand("ccmute").setTabCompleter(muteCommand);
        getCommand("ccunmute").setExecutor(muteCommand);
        getCommand("ccunmute").setTabCompleter(muteCommand);
        getCommand("ccmsg").setExecutor(msgCommand);
        getCommand("ccreply").setExecutor(msgCommand);

        // Will we use redis?
        USE_REDIS = false; //getConfig().getBoolean("redis.use", false);

        // Redis stuff
        if (USE_REDIS) {
            // Add the required libraries
            LIBRARIES.addMavenLibrary(LibraryHandler.MAVEN_CENTRAL, Depends.ORG_REDISSON,
                    Depends.REDISSON, Depends.REDISSON_VER);
            LIBRARIES.addMavenLibrary(LibraryHandler.MAVEN_CENTRAL, Depends.ORG_SLF4J,
                    Depends.SLF4J_API, Depends.SLF4J_API_VER);
            LIBRARIES.addMavenLibrary(LibraryHandler.MAVEN_CENTRAL, Depends.COM_ESOTERICSOFTWARE,
                    Depends.KYRO, Depends.KYRO_VER);
            LIBRARIES.addMavenLibrary(LibraryHandler.MAVEN_CENTRAL, Depends.COM_FASTERXML_JACKSON_CORE,
                    Depends.JACKSON_CORE, Depends.JACKSON_VER);
            LIBRARIES.addMavenLibrary(LibraryHandler.MAVEN_CENTRAL, Depends.COM_FASTERXML_JACKSON_CORE,
                    Depends.JACKSON_DATABIND, Depends.JACKSON_DATABIND_VER);
            LIBRARIES.addMavenLibrary(LibraryHandler.MAVEN_CENTRAL, Depends.IO_NETTY,
                    Depends.NETTY, Depends.NETTY_VER);

            // Setup redis related stuff
            new RChitchat(this);
        }

        if (!USE_REDIS) {
            // Setup mute list
            MUTE_MAP = new ConcurrentHashMap<>();

            // Setup private message map
            REPLY_MAP = new ConcurrentHashMap<>();
        }

        // Handle mute settings
        SAVE_MUTES = getConfig().getBoolean("save_mutes", false);
        if (savingMutes()) {
            try {
                MUTE_MAP = new ConcurrentHashMap<>(JSON.loadFromFile("mutes"));
            } catch (Exception oops) {
                getLogger().severe("Unable to load saved mutes, did someone tamper with the data?");
            }
        }

        // Clean up old mutes (only one server should do this to avoid unnecessary threads
        if (!USE_REDIS || getConfig().getBoolean("redis.clean_old_mutes", false)) {
            Bukkit.getScheduler().scheduleAsyncRepeatingTask(this, () -> MUTE_MAP.entrySet().stream().
                    filter(entry -> entry.getValue() < System.currentTimeMillis()).
                    forEach((Map.Entry<String, Double> entry) -> MUTE_MAP.remove(entry.getKey())), 30, 30);
        }
    }

    @Override
    public void onDisable() {
        // Manually unregister events
        HandlerList.unregisterAll(this);

        // Save mutes
        if (savingMutes()) {
            JSON.saveToFile("mutes", MUTE_MAP);
        }
    }

    // -- INST API METHODS -- //

    /**
     * Get the map of all muted players and their mute length in milliseconds.
     *
     * @return The map of all muted players.
     */
    public Map<String, Double> getMuteMap() {
        return MUTE_MAP;
    }

    /**
     * Get a map of all recent reply pairs.
     *
     * @return Map of all recent reply pairs.
     */
    public Map<String, String> getReplyMap() {
        return REPLY_MAP;
    }

    // -- STATIC API METHODS -- //

    /**
     * Get the chat format for adding tags or changing other settings.
     *
     * @return The enabled chat format.
     */
    public static ChatFormat getChatFormat() {
        return getInst().FORMAT;
    }

    /**
     * Set the entire chat format to a custom version.
     *
     * @param chatFormat A custom chat format.
     * @deprecated Only use this if you know what you are doing.
     */
    @Deprecated
    public static void setChatFormat(ChatFormat chatFormat) {
        getInst().FORMAT = chatFormat;
    }

    /**
     * Get the instance of this plugin.
     *
     * @return The current instance of this plugin.
     */
    public static Chitchat getInst() {
        return INST;
    }

    /**
     * Send a message through the Chitchat plugin. Includes the redis chat channel.
     *
     * @param message The message to be sent.
     */
    @SuppressWarnings("unchecked")
    public static void sendMessage(Component message) {
        if (getInst().USE_REDIS) {
            RChitchat.REDIS_CHAT.publish(RChitchat.getInst().getServerId() + "$" + JSONComponentSerializer.json().serialize(message));
        }
        sendMessage(message, (ForwardingAudience) Bukkit.getServer());
    }

    /**
     * Send a message through the Chitchat plugin, exclusive to a list of recipients.
     *
     * @param message    The message to be sent.
     * @param recipients The recipients of this message.
     */
    public static void sendMessage(Component message, ForwardingAudience recipients) {
        for (Audience player : recipients.audiences()) {
            player.sendMessage(message);
        }
    }

    /**
     * Send a message through the Chitchat plugin. Includes the redis chat channel.
     *
     * @param message The message to be sent.
     * @deprecated This method is depreciated in favor of the new BaseComponent based method.
     */
    @Deprecated
    public static void sendMessage(String message) {
        sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
    }

    // -- OPTION GETTERS -- //

    public boolean usingRedis() {
        return USE_REDIS;
    }

    public boolean savingMutes() {
        return !USE_REDIS && SAVE_MUTES;
    }
}
