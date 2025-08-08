package com.demigodsrpg.chitchat.tag;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class NameTag extends PlayerTag {
    @Override
    public String getName() {
        return "name";
    }

    @Override
    public int getPriority() {
        return 999;
    }

    @Override
    public Component getComponentFor(Player tagSource) {
        return tagSource.displayName();
    }
}
