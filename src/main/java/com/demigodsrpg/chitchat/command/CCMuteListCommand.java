package com.demigodsrpg.chitchat.command;

import com.demigodsrpg.chitchat.Chitchat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class CCMuteListCommand implements CommandExecutor {

    private final Chitchat INST;

    public CCMuteListCommand(Chitchat inst) {
        INST = inst;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equals("ccmutelist")) {
            if (!INST.getMuteMap().isEmpty()) {
                sender.sendMessage(Component.text("// -- Currently Muted Players -- //", NamedTextColor.YELLOW));
                for (Map.Entry<String, Double> entry : INST.getMuteMap().entrySet()) {
                    String mutedName = Bukkit.getOfflinePlayer(UUID.fromString(entry.getKey())).getName();
                    Component mutedDate = prettyDate(entry.getValue().longValue());
                    sender.sendMessage(Component.text( "  - ", NamedTextColor.YELLOW).
                            append(Component.text((mutedName != null ? mutedName : entry.getKey()), NamedTextColor.WHITE)).
                            append(mutedDate));
                }
            } else {
                sender.sendMessage(Component.text("There are currently no muted players.", NamedTextColor.RED));
            }
        }
        return true;
    }

    private Component prettyDate(long time) {
        long diff = (time - System.currentTimeMillis()) / 1_000;
        int dayDiff = Math.round(diff / 86_400);

        Component ret = Component.text(" for ", NamedTextColor.YELLOW);

        if (dayDiff == 0 && diff < 50) {
            ret.append(Component.text("less than a minute.", NamedTextColor.WHITE));
        } else if (diff < 60) {
            ret.append(Component.text("a minute.", NamedTextColor.WHITE));
        } else if (diff < 120) {
            ret.append(Component.text("a couple minutes.", NamedTextColor.WHITE));
        } else if (diff < 3_300) {
            ret.append(Component.text("~" + Math.round((float) diff / 60) + " minutes.", NamedTextColor.WHITE));
        } else if (diff < 7_200) {
            ret.append(Component.text("an hour.", NamedTextColor.WHITE));
        } else if (diff < 82_800) {
            ret.append(Component.text(Math.round((float) diff / 3_600) + " hours.", NamedTextColor.WHITE));
        } else if (dayDiff <= 1) {
            ret.append(Component.text("a day.", NamedTextColor.WHITE));
        } else if (dayDiff < 7) {
            ret.append(Component.text("~" + dayDiff + " days.", NamedTextColor.WHITE));
        } else if (dayDiff < 28) {
            ret.append(Component.text("~" + (int) Math.ceil((double) dayDiff / 7) + " weeks.", NamedTextColor.WHITE));
        } else {
            ret = Component.text(" until ", NamedTextColor.YELLOW).
                    append(Component.text(new SimpleDateFormat("EEE, MMM dd, yyyy.").
                    format(new Date(time)), NamedTextColor.WHITE));
        }

        return ret;
    }
}
