package com.demigodsrpg.chitchat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ChatListener implements Listener {

    private final Chitchat INST;
    private final Component DELETE_CROSS_BASE = Component.textOfChildren(
            Component.text("[", NamedTextColor.DARK_GRAY),
            Component.text("X", NamedTextColor.DARK_RED, TextDecoration.BOLD),
            Component.text("]", NamedTextColor.DARK_GRAY)
    ).hoverEvent(Component.text("Delete?", NamedTextColor.RED));

    public ChatListener(Chitchat inst) {
        INST = inst;
    }

    // -- BUKKIT CHAT LISTENER -- //

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncChatEvent chat) {
        if (INST.getMuteMap().containsKey(chat.getPlayer().getUniqueId().toString())) {
            chat.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFinalChat(AsyncChatEvent chat) {
        final Component formattedMessage = INST.FORMAT.getFormattedMessage(chat.getPlayer(), chat.originalMessage());
        chat.renderer((source, sourceDisplayName, message, viewer) -> {
            Component finalMessage = formattedMessage;

            if(viewer == source || (viewer instanceof Player player && player.hasPermission("chitchat.delete"))) {
                Component deleteCross = DELETE_CROSS_BASE.
                        clickEvent(ClickEvent.callback(audience -> Bukkit.getServer().deleteMessage(chat.signedMessage())));
                finalMessage =  finalMessage.appendSpace().append(deleteCross);
            }

            return finalMessage;
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPreprocessCommand(PlayerCommandPreprocessEvent command) {
        Player player = command.getPlayer();
        String[] commandMsg = command.getMessage().split("\\s+");

        // Muted commands
        if (INST.getMuteMap().containsKey(player.getUniqueId().toString())) {
            if (Chitchat.getInst().MUTED_COMMANDS.contains(commandMsg[0].toLowerCase().substring(1))) {
                command.setCancelled(true);
                player.sendMessage(Component.text("I'm sorry " + player.getName() + ", I'm afraid I can't do that.", NamedTextColor.RED));
            }
        }

        // /me <message>
        else if (Chitchat.getInst().OVERRIDE_ME && commandMsg.length > 1 && commandMsg[0].equals("/me")) {
            command.setCancelled(true);
            if (Chitchat.getInst().MUTED_COMMANDS.contains("me") && INST.getMuteMap().containsKey(player.
                    getUniqueId().toString())) {
                player.sendMessage(Component.text("I'm sorry " + player.getName() + ", I'm afraid I can't do that.", NamedTextColor.RED));
            } else {
                Component message = Component.text(PlainTextComponentSerializer.plainText().serialize(player.displayName()) + " " + command.getMessage().substring(4)).decorate(TextDecoration.ITALIC);
                Bukkit.getServer().sendMessage(message);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        // Remove all useless reply data
        String playerName = event.getPlayer().getName();
        INST.getReplyMap().remove(playerName);
        if (INST.getReplyMap().containsValue(playerName)) {
            INST.getReplyMap().entrySet().stream().
                    filter(entry -> entry.getValue().equals(playerName)).
                    forEach(entry -> INST.getReplyMap().remove(entry.getKey()));
        }
    }
}
