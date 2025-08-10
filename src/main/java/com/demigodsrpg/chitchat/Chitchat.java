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
import com.demigodsrpg.chitchat.example.SpecificChatTag;
import com.demigodsrpg.chitchat.example.WorldChatTag;
import com.demigodsrpg.chitchat.format.ChatFormat;
import com.demigodsrpg.chitchat.util.*;
import com.google.common.collect.ImmutableList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
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

    // -- IMPORTANT LISTS -- //
    ConcurrentMap<String, Double> MUTE_MAP;
    ConcurrentMap<String, String> REPLY_MAP;

    // -- OPTIONS -- //

    boolean OVERRIDE_ME;
    boolean SAVE_MUTES;
    List<String> MUTED_COMMANDS;

    // -- BUKKIT ENABLE/DISABLE -- //

    @SuppressWarnings("unchecked")
    @Override
    public void onEnable() {
        // Define static objects
        INST = this;
        FORMAT = new ChatFormat();

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
            FORMAT.add(new WorldChatTag())
                    .add(new PlayerTag("example-prefix", "chitchat.admin", admin, 3))
                    .add(new SpecificChatTag("hqm", "HmmmQuestionMark", dev, 3))
                    .add(new SpecificChatTag("hqm2", "PseudoHQM", dev, 3))
                    .add(new SpecificChatTag("hqm3", "HQM", dev, 3))
                    .add(new NameTag());
        }

        // Register events
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        // Register commands
        MuteListCommand muteListCommand = new MuteListCommand(this);
        MuteCommand muteCommand = new MuteCommand(this, JSON);
        MessageCommand msgCommand = new MessageCommand(this);
        getCommand("mutelist").setExecutor(muteListCommand);
        getCommand("mute").setExecutor(muteCommand);
        getCommand("mute").setTabCompleter(muteCommand);
        getCommand("unmute").setExecutor(muteCommand);
        getCommand("unmute").setTabCompleter(muteCommand);
        getCommand("message").setExecutor(msgCommand);
        getCommand("reply").setExecutor(msgCommand);

        // Setup mute list
        MUTE_MAP = new ConcurrentHashMap<>();

        // Setup private message map
        REPLY_MAP = new ConcurrentHashMap<>();

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
        Bukkit.getScheduler().scheduleAsyncRepeatingTask(this, () -> MUTE_MAP.entrySet().stream().
                filter(entry -> entry.getValue() < System.currentTimeMillis()).
                forEach((Map.Entry<String, Double> entry) -> MUTE_MAP.remove(entry.getKey())), 30, 30);

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

    // -- OPTION GETTERS -- //

    public boolean savingMutes() {
        return SAVE_MUTES;
    }
}
