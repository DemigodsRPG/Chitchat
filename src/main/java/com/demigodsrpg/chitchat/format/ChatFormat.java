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
package com.demigodsrpg.chitchat.format;

import com.demigodsrpg.chitchat.ChatTag;
import com.google.common.collect.ImmutableList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * A class representing the format of chat.
 */
public class ChatFormat {
    // -- IMPORTANT DATA -- //

    private final List<ChatTag> chatTags = new LinkedList<>();

    // -- MUTATORS -- //

    /**
     * Add a player tag to the chat format.
     *
     * @param chatTag A player tag.
     * @return This chat format.
     */
    public ChatFormat add(ChatTag chatTag) {
        // Negative priorities don't exist
        if(chatTag.getPriority() < 0) {
            chatTags.add(0, chatTag);
        }

        // Make sure the priority fits into the linked list
        else if(chatTag.getPriority() + 1 > chatTags.size()) {
            chatTags.add(chatTag);
        }

        // Add to the correct spot
        else {
            chatTags.add(chatTag.getPriority(), chatTag);
        }
        return this;
    }

    /**
     * Add a collection of player tags to the chat format.
     *
     * @param chatTags A collection of player tags.
     * @return This chat format.
     */
    public ChatFormat addAll(Collection<ChatTag> chatTags) {
        chatTags.forEach(this::add);
        return this;
    }

    /**
     * Add an array of player tags to the chat format.
     *
     * @param chatTags An array of player tags.
     * @return This chat format.
     */
    public ChatFormat addAll(ChatTag[] chatTags) {
        Arrays.asList(chatTags).forEach(this::add);
        return this;
    }

    // -- GETTERS -- //

    /**
     * Get an immutable copy of the player tags.
     *
     * @return An immutable copy of the player tags.
     */
    public ImmutableList<ChatTag> getPlayerTags() {
        return ImmutableList.copyOf(chatTags);
    }

    /**
     * Get the representation of all the player tags.
     *
     * @param player The player for whom the tags will be applied.
     * @return The tag results.
     */
    public Component getTags(Player player) {
        ComponentBuilder<TextComponent, TextComponent.Builder> builder = Component.text();
        for (ChatTag tag : chatTags) {
            Component component = tag.getComponentFor(player);
            if (component != null) {
                builder.append(component);
            }
        }
        return builder.build();
    }

    /**
     * Get the final formatted message for this chat format.
     *
     * @param player The player chatting.
     * @param message The message being sent.
     * @return The final formatted message.
     */
    public Component getFormattedMessage(Player player, Component message) {
        Component finalMessage;
        if (player.hasPermission("chitchat.color")) {
            finalMessage = LegacyComponentSerializer.legacyAmpersand().
                    deserialize(PlainTextComponentSerializer.plainText().serialize(message));
        } else {
            finalMessage = message;
        }
        return getTags(player).
                append(Component.text(": ", NamedTextColor.GRAY)).
                append(Component.text("", NamedTextColor.DARK_GRAY)).
                append(finalMessage);
    }

    /**
     * Get the final formatted message (serialized as json) for this chat format.
     *
     * @param player  The player chatting.
     * @param message The message being sent.
     * @return The serialized message.
     */
    public String getSerializedMessage(Player player, Component message) {
        return JSONComponentSerializer.json().serialize(getFormattedMessage(player, message));
    }
}
