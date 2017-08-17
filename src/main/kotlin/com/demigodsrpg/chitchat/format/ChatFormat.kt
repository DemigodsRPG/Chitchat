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
package com.demigodsrpg.chitchat.format

import com.demigodsrpg.chitchat.tag.ChatScope
import com.demigodsrpg.chitchat.tag.PlayerTag
import com.google.common.collect.ImmutableList
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.chat.ComponentSerializer
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import java.util.*
import java.util.function.Consumer

/**
 * A class representing the format of chat.
 */
class ChatFormat {
    // -- IMPORTANT DATA -- //

    private val playerTags = LinkedList<PlayerTag>()

    // -- MUTATORS -- //

    /**
     * Add a player tag to the chat format.
     *
     * @param playerTag A player tag.
     * @return This chat format.
     */
    fun add(playerTag: PlayerTag): ChatFormat {
        when {
            playerTag.priority < 0 -> playerTags.add(0, playerTag)
            playerTag.priority + 1 > playerTags.size -> playerTags.add(playerTag)
            else -> playerTags.add(playerTag.priority, playerTag)
        }
        return this
    }

    /**
     * Add a collection of player tags to the chat format.
     *
     * @param playerTags A collection of player tags.
     * @return This chat format.
     */
    fun addAll(playerTags: Collection<PlayerTag>): ChatFormat {
        playerTags.forEach(Consumer<PlayerTag> { this.add(it) })
        return this
    }

    /**
     * Add an array of player tags to the chat format.
     *
     * @param playerTags An array of player tags.
     * @return This chat format.
     */
    fun addAll(playerTags: Array<PlayerTag>): ChatFormat {
        Arrays.asList(*playerTags).forEach(Consumer<PlayerTag> { this.add(it) })
        return this
    }

    // -- GETTERS -- //

    /**
     * Get an immutable copy of the player tags.
     *
     * @return An immutable copy of the player tags.
     */
    fun getPlayerTags(): ImmutableList<PlayerTag> {
        return ImmutableList.copyOf(playerTags)
    }

    /**
     * Get the representation of all of the player tags.
     *
     * @param player The player for whom the tags will be applied.
     * @param scope The scope for the tag to be presented in.
     * @return The tag results.
     */
    fun getTags(parent: TextComponent, player: Player, scope: ChatScope): TextComponent {
        playerTags.stream().filter { tag -> tag.scope == scope || ChatScope.ALL == tag.scope }.forEach { tag ->
            val component = tag.getComponentFor(player)
            if (component != null) {
                parent.addExtra(component.duplicate())
            }
        }
        return parent
    }

    /**
     * Get the final formatted message for this chat format.
     *
     * @param player The player chatting.
     * @param scope The scope for the message to be presented in.
     * @param message The message being sent.
     * @return The final formatted message.
     */
    fun getFormattedMessage(player: Player, scope: ChatScope, message: String): BaseComponent {
        var ret = TextComponent("")
        ret = getTags(ret, player, scope)
        ret.color = net.md_5.bungee.api.ChatColor.GRAY
        val next = TextComponent(": ")
        next.color = net.md_5.bungee.api.ChatColor.DARK_GRAY
        ret.addExtra(next)
        var finalMessage = ChatColor.WHITE.toString() + message
        if (player.hasPermission("chitchat.color")) {
            finalMessage = ChatColor.translateAlternateColorCodes('&', finalMessage)
        }
        for (component in TextComponent.fromLegacyText(finalMessage)) {
            ret.addExtra(component)
        }
        return ret
    }

    /**
     * Get the final formatted message (serialized as json) for this chat format.
     *
     * @param player  The player chatting.
     * @param scope   The scope for the message to be presented in.
     * @param message The message being sent.
     * @return The serialized message.
     */
    fun getSerializedMessage(player: Player, scope: ChatScope, message: String): String {
        return ComponentSerializer.toString(getFormattedMessage(player, scope, message))
    }

    /**
     * Should this message not be sent over bungee?
     *
     * @param player The player.
     * @return If the message should be sent over bungee.
     */
    fun shouldCancelRedis(player: Player): Boolean {
        return getPlayerTags().any { it.cancelRedis(player) }
    }
}
