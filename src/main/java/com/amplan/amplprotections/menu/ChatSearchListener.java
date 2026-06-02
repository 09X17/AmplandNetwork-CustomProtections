package com.amplan.amplprotections.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.amplan.amplprotections.model.ProtectionRegion;
import com.amplan.amplprotections.utils.MessageUtils;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class ChatSearchListener implements Listener {

    private final AmplProtections plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<UUID, BukkitTask> pendingSearches = new ConcurrentHashMap<>();
    private final Map<UUID, BulkAddCallback> bulkCallbacks = new ConcurrentHashMap<>();

    public ChatSearchListener(AmplProtections plugin) {
        this.plugin = plugin;
    }

    public interface BulkAddCallback {
        void onPlayerFound(Player searcher, String playerName, Set<Integer> selectedRegionIds);
    }

    public void addBulkCallback(Player player, Set<Integer> selectedRegionIds) {
        UUID uuid = player.getUniqueId();
        Location loc = player.getLocation();
        if (loc == null) return;

        cancelSearch(player);

        player.sendMessage(mm.deserialize(MessageUtils.lang("bulk-menu.search-prompt")));
        player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);

        bulkCallbacks.put(uuid, (searcher, playerName, ids) -> {
            processBulkAdd(searcher, playerName, ids);
        });

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (bulkCallbacks.remove(uuid) != null) {
                if (pendingSearches.remove(uuid) != null) {
                    player.sendMessage(mm.deserialize(MessageUtils.lang("search.chat-timeout")));
                    player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
                }
            }
        }, 600L);

        pendingSearches.put(uuid, task);
    }

    private void processBulkAdd(Player player, String targetName, Set<Integer> selectedRegionIds) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
            if (offline.getName() == null) {
                player.sendMessage(mm.deserialize(MessageUtils.lang("general.player-not-found")));
                Location loc = player.getLocation();
                if (loc != null) player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
                return;
            }
            targetName = offline.getName();
        } else {
            targetName = target.getName();
        }

        String finalTargetName = targetName;
        int addedCount = 0;

        for (int regionId : selectedRegionIds) {
            ProtectionRegion region = plugin.getProtectionManager().getAllRegions().stream()
                    .filter(r -> r.getDatabaseId() == regionId)
                    .findFirst().orElse(null);
            if (region == null) continue;

            UUID targetUuid = target != null ? target.getUniqueId() : Bukkit.getOfflinePlayer(finalTargetName).getUniqueId();

            if (region.getOwnerUniqueId().equals(targetUuid)) continue;
            if (region.isMember(targetUuid)) continue;

            region.addMember(targetUuid, com.amplan.amplprotections.model.PlayerRank.MEMBER);
            plugin.getProtectionManager().getProtectionDao().saveMemberAsync(region.getDatabaseId(), targetUuid, "MEMBER");
            addedCount++;
        }

        if (addedCount > 0) {
            player.sendMessage(mm.deserialize(MessageUtils.lang("bulk-menu.add-success")
                    .replace("%player%", finalTargetName)
                    .replace("%count%", String.valueOf(addedCount))));
            Location loc = player.getLocation();
            if (loc != null) player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

            if (target != null && target.isOnline()) {
                target.sendMessage(mm.deserialize(MessageUtils.lang("bulk-menu.notify-added")
                        .replace("%owner%", player.getName())
                        .replace("%count%", String.valueOf(addedCount))));
            }
        } else {
            player.sendMessage(mm.deserialize(MessageUtils.lang("bulk-menu.add-no-changes")));
            Location loc = player.getLocation();
            if (loc != null) player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
        }
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

        BulkAddCallback bulkCallback = bulkCallbacks.remove(player.getUniqueId());
        if (bulkCallback != null) {
            Player finalPlayer = player;
            String finalQuery = query;
            Set<Integer> selectedIds = plugin.getProtectionManager().getRegionsByOwner(player.getUniqueId()).stream()
                    .map(ProtectionRegion::getDatabaseId)
                    .collect(java.util.stream.Collectors.toSet());

            bulkCallback.onPlayerFound(finalPlayer, finalQuery, selectedIds);
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
        bulkCallbacks.clear();
    }
}