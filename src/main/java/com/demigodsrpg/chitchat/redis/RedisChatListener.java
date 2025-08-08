package com.demigodsrpg.chitchat.redis;

import com.demigodsrpg.chitchat.RChitchat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.redisson.api.listener.MessageListener;

public class RedisChatListener implements MessageListener<String> {
    private final RChitchat R_INST;

    public RedisChatListener(RChitchat rInst) {
        R_INST = rInst;
    }

    @Override
    public void onMessage(CharSequence ignored, String message) {
        if (message != null && !message.startsWith(R_INST.getServerId() + "$")) {
            Component component = JSONComponentSerializer.json().deserialize(message);
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(component);
            }
        }
    }
}
