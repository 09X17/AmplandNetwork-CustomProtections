package com.amplan.amplprotections.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import com.amplan.amplprotections.AmplProtections;
import com.amplan.amplprotections.utils.MessageUtils;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class ChatSearchListener implements Listener {

    private final AmplProtections plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<UUID, BukkitTask> pendingSearches = new ConcurrentHashMap<>();

    public ChatSearchListener(AmplProtections plugin) {
        this.plugin = plugin;
    }

    public void startSearch(Player player) {

        UUID uuid = player.getUniqueId();
        Location loc = player.getLocation();
        if (loc == null) {
            return;
        }

        cancelSearch(player);

        player.sendMessage(mm.deserialize(MessageUtils.lang("search.chat-prompt")));
        player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {

            if (pendingSearches.remove(uuid) != null) {
                player.sendMessage(mm.deserialize(MessageUtils.lang("search.chat-timeout")));
                player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            }

        }, 600L);

        pendingSearches.put(uuid, task);
    }

    public void cancelSearch(Player player) {

        BukkitTask old = pendingSearches.remove(player.getUniqueId());

        if (old != null) {
            old.cancel();
        }
    }

    public boolean isSearching(Player player) {
        return pendingSearches.containsKey(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {

        Player player = event.getPlayer();

        if (!isSearching(player)) {
            return;
        }

        event.setCancelled(true);

        String query = PlainTextComponentSerializer.plainText()
                .serialize(event.message())
                .trim();

        BukkitTask task = pendingSearches.remove(player.getUniqueId());

        if (task != null) {
            task.cancel();
        }

        if (query.isEmpty()) {
            Location loc = player.getLocation();
            if (loc != null) {
                player.sendMessage(mm.deserialize(MessageUtils.lang("search.empty-query")));
                player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            }
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            List<OfflinePlayer> matchedPlayers = new ArrayList<>();
            String lowerQuery = query.toLowerCase();
            for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                String name = op.getName();
                if (name != null && name.toLowerCase().contains(lowerQuery)) {
                    matchedPlayers.add(op);
                    if (matchedPlayers.size() >= 21) break;
                }
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;

                Location loc = player.getLocation();
                if (loc == null) return;

                if (matchedPlayers.isEmpty()) {
                    player.sendMessage(mm.deserialize(
                            MessageUtils.lang("search.player-not-found")
                                    .replace("%search%", query)));

                    player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
                    return;
                }

                player.sendMessage(mm.deserialize(MessageUtils.lang("search.found")
                        .replace("%count%", String.valueOf(matchedPlayers.size()))));
                player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);

                plugin.getMenuManager().openMenu(player, new SearchMenu(plugin, matchedPlayers, 1));
            });
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelSearch(event.getPlayer());
    }

    public void shutdown() {

        for (BukkitTask task : pendingSearches.values()) {
            task.cancel();
        }

        pendingSearches.clear();
    }
}