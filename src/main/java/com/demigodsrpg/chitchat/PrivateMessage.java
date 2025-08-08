package com.demigodsrpg.chitchat;

import com.google.gson.GsonBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PrivateMessage implements Serializable {
    // -- SERIAL VERSION UID -- //

    private static final long serialVersionUID = 1L;

    // -- TRANSIENT DATA -- //

    private final transient Chitchat INST;

    // -- META DATA -- //

    private final String target;
    private final String sender;
    private final String message;

    // -- CONSTRUCTORS -- //

    @SuppressWarnings("unchecked")
    public PrivateMessage(Chitchat inst, String json) {
        INST = inst;
        Map<String, Object> map = new GsonBuilder().create().fromJson(json, Map.class);
        target = map.get("target").toString();
        sender = map.get("sender").toString();
        message = map.get("message").toString();
    }

    public PrivateMessage(Chitchat inst, String target, String sender, String message) {
        INST = inst;
        this.target = target;
        this.sender = sender;
        this.message = message;
    }

    // -- GETTERS -- //

    public String getTarget() {
        return target;
    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public Component getFormattedMessage(boolean out) {
       return Component.text("PM" + (out ? " to" : " from") + " <", NamedTextColor.DARK_GRAY).
                append(Component.text((out ? target : sender), NamedTextColor.DARK_AQUA)).
                append(Component.text(">: ", NamedTextColor.DARK_GRAY)).
                append(Component.text(message, NamedTextColor.GRAY));
    }

    public String getLogMessage() {
        return "PM <" + sender + " to " + target + ">: " + message;
    }

    public String toJson() {
        Map<String, Object> map = new HashMap<>();
        map.put("target", target);
        map.put("sender", sender);
        map.put("message", message);
        return new GsonBuilder().create().toJson(map, Map.class);
    }

    // -- SEND -- //

    public void send() {
        // Get the sender
        OfflinePlayer sender = Bukkit.getOfflinePlayer(this.sender);

        // Check if the player is on this server
        if (Bukkit.getPlayer(target) != null) {
            Bukkit.getPlayer(target).sendMessage(getFormattedMessage(false));
            Chitchat.getInst().getLogger().info(getLogMessage());
        } else if (Chitchat.getInst().USE_REDIS) {
            // Nope, send through redis
            RChitchat.REDIS_MSG.publishAsync(toJson());
        } else if (sender.isOnline()) {
            // Something went wrong
            Objects.requireNonNull(sender.getPlayer()).sendMessage(NamedTextColor.RED + "There was an error sending a private message.");
            return;
        }

        // Send the 'sent' message
        if (sender.isOnline()) {
            Objects.requireNonNull(sender.getPlayer()).sendMessage(getFormattedMessage(true));
        }

        // Add to the reply map
        INST.getReplyMap().put(this.sender, this.target);
        INST.getReplyMap().put(this.target, this.sender);
    }
}
