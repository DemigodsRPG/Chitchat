package com.demigodsrpg.chitchat.tag

import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.entity.Player

class NameTag : PlayerTag() {
    override val name: String
        get() = "name"

    override val priority: Int
        get() = 999

    override fun getComponentFor(tagSource: Player): TextComponent? {
        val ret = TextComponent("")
        for (component in TextComponent.fromLegacyText(tagSource.displayName)) {
            ret.addExtra(component)
        }
        return ret
    }
}
