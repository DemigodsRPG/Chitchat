package com.demigodsrpg.chitchat;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class NameTag extends ChatTag {
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
