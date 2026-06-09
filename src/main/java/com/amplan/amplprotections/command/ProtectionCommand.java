package com.amplan.amplprotections.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.amplan.amplprotections.AmplProtections;
import com.amplan.amplprotections.manager.ProtectionManager;
import com.amplan.amplprotections.menu.BulkManageMenu;
import com.amplan.amplprotections.menu.BuyMenu;
import com.amplan.amplprotections.menu.MainProtectionMenu;
import com.amplan.amplprotections.menu.MergeMenu;
import com.amplan.amplprotections.menu.PlayerProtectionListMenu;
import com.amplan.amplprotections.menu.PresetMenu;
import com.amplan.amplprotections.menu.RentalMenu;
import com.amplan.amplprotections.menu.RollbackMenu;
import com.amplan.amplprotections.model.PlayerRank;
import com.amplan.amplprotections.model.ProtectionRegion;
import com.amplan.amplprotections.utils.MessageUtils;
import com.amplan.amplprotections.utils.ParticleUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class ProtectionCommand implements CommandExecutor, TabCompleter {

    private final AmplProtections plugin;
    private final ProtectionManager manager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public ProtectionCommand(AmplProtections plugin) {
        this.plugin = plugin;
        this.manager = plugin.getProtectionManager();
    }

    private String msg(String key) {
        return MessageUtils.lang(key);
    }

    private Component parse(String key, String fallback) {
        return Objects.requireNonNull(
                mm.deserialize(Objects.requireNonNullElse(msg(key), fallback)));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize(msg("general.only-players")));
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        ProtectionRegion region = manager.getRegionAt(player.getLocation());

        switch (args[0].toLowerCase()) {
            case "menu" -> handleMenu(player, region);
            case "info" -> handleInfo(player, region);
            case "lore" -> handleLore(player, region, args);
            case "add" -> handleAddMember(player, region, args);
            case "remove", "remover" -> handleRemoveMember(player, region, args);
            case "promote", "promover", "ascender" -> handlePromoteMember(player, region, args);
            case "demote", "degradar" -> handleDemoteMember(player, region, args);
            case "members", "miembros" -> handleListMembers(player, region);
            case "flag" -> handleFlagChange(player, region, args);
            case "view", "preview" -> handleViewPreview(player, region);
            case "list", "lista" -> handleListProtections(player);
            case "buy", "comprar" -> handleBuy(player);
            case "merge", "fusionar" -> handleMerge(player, region);
            case "preset" -> handlePreset(player, region, args);
            case "hologram", "holo" -> handleHologram(player, region);
            case "rent", "alquiler" -> handleRent(player, region, args);
            case "rollback", "rb" -> handleRollback(player, region, args);
            case "transfer", "transferir" -> handleTransfer(player, region, args);
            case "bulk", "masivo" -> handleBulkManage(player);
            case "debug" -> handleDebug(player, region, args);
            default -> sendHelpMessage(player);
        }
        return true;
    }

    private void handleMenu(Player player, ProtectionRegion region) {
        if (region == null) {
            player.sendMessage(mm.deserialize(msg("general.not-in-protection")));
            return;
        }
        if (!region.getOwnerUniqueId().equals(player.getUniqueId())
                && !player.hasPermission("amplprotections.admin.bypass")) {
            player.sendMessage(mm.deserialize(msg("members.no-permission-owner")));
            return;
        }
        plugin.getMenuManager().openMenu(player, new MainProtectionMenu(plugin, region));
    }

    private void handleInfo(Player player, ProtectionRegion region) {
        if (region == null) {
            player.sendMessage(mm.deserialize(msg("info.no-region")));
            return;
        }

        String ownerName = region.getOwnerName();
        String typeId = region.getTypeId().replace("_", " ").toUpperCase();
        int radius = region.getRadius();
        String size = (radius * 2 + 1) + "x" + (radius * 2 + 1);

        player.sendMessage(mm.deserialize(msg("info.title")));
        player.sendMessage(mm.deserialize(msg("info.land-id").replace("%land%", region.getLandId())));
        player.sendMessage(mm.deserialize(msg("info.owner").replace("%owner%", ownerName)));
        player.sendMessage(mm.deserialize(msg("info.type").replace("%type%", typeId)));
        player.sendMessage(mm.deserialize(msg("info.size").replace("%size%", size)));
        player.sendMessage(mm.deserialize(msg("info.center")
                .replace("%x%", String.valueOf(region.getBlockX()))
                .replace("%y%", String.valueOf(region.getBlockY()))
                .replace("%z%", String.valueOf(region.getBlockZ()))));

        Map<UUID, PlayerRank> members = region.getMembers();
        int memberCount = members.size();

        player.sendMessage(mm.deserialize(msg("info.members-title").replace("%count%", String.valueOf(memberCount))));

        if (members.isEmpty()) {
            player.sendMessage(mm.deserialize(msg("info.members-none")));
        } else {
            for (Map.Entry<UUID, PlayerRank> entry : members.entrySet()) {
                UUID uuid = entry.getKey();
                PlayerRank rank = entry.getValue();
                String name = Bukkit.getOfflinePlayer(uuid).getName();
                if (name == null)
                    name = uuid.toString().substring(0, 8);

                String rankDisplay = getRankDisplay(rank);
                String rankColor = getRankColor(rank);

                player.sendMessage(mm.deserialize(msg("info.member-entry")
                        .replace("%name%", name)
                        .replace("%rank%", rankColor + rankDisplay)));
            }
        }

        List<String> enabledFlags = plugin.getConfig().getStringList("flags.enabled");
        long enabledCount = enabledFlags.stream().filter(f -> {
            if (com.amplan.amplprotections.model.FlagPermissionLevel.isEnvironmental(f)) {
                return region.isBooleanFlagEnabled(f);
            }
            return region.isFlagEnabled(f);
        }).count();
        long disabledCount = enabledFlags.size() - enabledCount;

        player.sendMessage(mm.deserialize(msg("info.flags-summary")
                .replace("%enabled%", String.valueOf(enabledCount))
                .replace("%disabled%", String.valueOf(disabledCount))));
    }

    private String getRankDisplay(PlayerRank rank) {
        return switch (rank) {
            case OWNER -> msg("info.member-owner");
            case SECONDARY_OWNER -> msg("info.member-secondary-owner");
            case ADMIN -> msg("info.member-admin");
            case MEMBER -> msg("info.member-member");
        };
    }

    private String getRankColor(PlayerRank rank) {
        return switch (rank) {
            case OWNER -> "<gold>";
            case SECONDARY_OWNER -> "<light_purple>";
            case ADMIN -> "<red>";
            case MEMBER -> "<green>";
        };
    }

    private void handleLore(Player player, ProtectionRegion region, String[] args) {
        if (region == null) {
            player.sendMessage(mm.deserialize(msg("general.not-in-protection")));
            return;
        }
        if (!region.getOwnerUniqueId().equals(player.getUniqueId())
                && !player.hasPermission("amplprotections.admin.bypass")) {
            player.sendMessage(mm.deserialize(msg("members.no-permission-owner")));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(mm.deserialize(msg("commands.usage-lore")));
            return;
        }

        StringBuilder loreBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1)
                loreBuilder.append(" ");
            loreBuilder.append(args[i]);
        }
        String lore = loreBuilder.toString();

        region.setCustomLore(lore);
        manager.saveRegionAsync(region);
        player.sendMessage(mm.deserialize(msg("commands.lore-updated")));
    }

    private void handleAddMember(Player player, ProtectionRegion region, String[] args) {
        if (region == null) {
            player.sendMessage(mm.deserialize(msg("general.not-in-protection")));
            return;
        }
        if (!canManageMembers(player, region)) {
            player.sendMessage(mm.deserialize(msg("members.no-permission-manage")));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(mm.deserialize(msg("commands.usage-add")));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(mm.deserialize(msg("general.player-not-found")));
            return;
        }

        if (region.getOwnerUniqueId().equals(target.getUniqueId())) {
            player.sendMessage(mm.deserialize(msg("members.is-owner").replace("%member%", target.getName())));
            return;
        }

        if (region.isMember(target.getUniqueId())) {
            player.sendMessage(mm.deserialize(msg("members.already-member").replace("%member%", target.getName())));
            return;
        }

        manager.addMember(region, target.getUniqueId(), "MEMBER");
        player.sendMessage(mm.deserialize(msg("members.added").replace("%member%", target.getName())));
        target.sendMessage(mm.deserialize(msg("members.notify-promoted").replace("%owner%", player.getName())));
    }

    private void handleRemoveMember(Player player, ProtectionRegion region, String[] args) {
        if (region == null) {
            player.sendMessage(mm.deserialize(msg("general.not-in-protection")));
            return;
        }
        if (!canManageMembers(player, region)) {
            player.sendMessage(mm.deserialize(msg("members.no-permission-manage")));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(mm.deserialize(msg("commands.usage-remove")));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(mm.deserialize(msg("general.player-not-found")));
            return;
        }

        if (region.getOwnerUniqueId().equals(target.getUniqueId())) {
            player.sendMessage(mm.deserialize(msg("members.is-owner").replace("%member%", target.getName())));
            return;
        }

        if (!region.isMember(target.getUniqueId())) {
            player.sendMessage(mm.deserialize(msg("members.not-member").replace("%member%", target.getName())));
            return;
        }

        manager.removeMember(region, target.getUniqueId());
        player.sendMessage(mm.deserialize(msg("members.removed").replace("%member%", target.getName())));
        target.sendMessage(mm.deserialize(msg("members.notify-removed").replace("%owner%", player.getName())));
    }

    private void handlePromoteMember(Player player, ProtectionRegion region, String[] args) {
        if (region == null) {
            player.sendMessage(mm.deserialize(msg("general.not-in-protection")));
            return;
        }
        if (!region.getOwnerUniqueId().equals(player.getUniqueId()) &&
                region.getRank(player.getUniqueId()) != PlayerRank.SECONDARY_OWNER) {
            player.sendMessage(mm.deserialize(msg("members.no-permission-owner")));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(mm.deserialize(msg("commands.usage-promote")));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(mm.deserialize(msg("general.player-not-found")));
            return;
        }

        if (region.getOwnerUniqueId().equals(target.getUniqueId())) {
            player.sendMessage(mm.deserialize(msg("members.is-owner").replace("%member%", target.getName())));
            return;
        }

        PlayerRank currentRank = region.getMembers().get(target.getUniqueId());
        if (currentRank == null) {
            player.sendMessage(mm.deserialize(msg("members.not-member").replace("%member%", target.getName())));
            return;
        }

        if (currentRank == PlayerRank.SECONDARY_OWNER) {
            player.sendMessage(
                    mm.deserialize(msg("members.promote-already-max").replace("%member%", target.getName())));
            return;
        }

        PlayerRank newRank = currentRank == PlayerRank.MEMBER ? PlayerRank.ADMIN : PlayerRank.SECONDARY_OWNER;
        region.addMember(target.getUniqueId(), newRank);
        manager.getProtectionDao().saveMemberAsync(region.getDatabaseId(), target.getUniqueId(), newRank.name());

        String msgKey = newRank == PlayerRank.ADMIN ? "members.promoted" : "members.promoted-secondary";
        player.sendMessage(mm.deserialize(msg(msgKey).replace("%member%", target.getName())));

        if (newRank == PlayerRank.SECONDARY_OWNER) {
            target.sendMessage(
                    mm.deserialize(msg("members.notify-promoted-secondary").replace("%owner%", player.getName())));
        } else {
            target.sendMessage(mm.deserialize(msg("members.notify-promoted").replace("%owner%", player.getName())));
        }
    }

    private void handleDemoteMember(Player player, ProtectionRegion region, String[] args) {
        if (region == null) {
            player.sendMessage(mm.deserialize(msg("general.not-in-protection")));
            return;
        }
        if (!region.getOwnerUniqueId().equals(player.getUniqueId()) &&
                region.getRank(player.getUniqueId()) != PlayerRank.SECONDARY_OWNER) {
            player.sendMessage(mm.deserialize(msg("members.no-permission-owner")));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(mm.deserialize(msg("commands.usage-demote")));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(mm.deserialize(msg("general.player-not-found")));
            return;
        }

        PlayerRank currentRank = region.getMembers().get(target.getUniqueId());
        if (currentRank == null) {
            player.sendMessage(mm.deserialize(msg("members.not-member").replace("%member%", target.getName())));
            return;
        }

        if (currentRank == PlayerRank.MEMBER) {
            player.sendMessage(mm.deserialize(msg("members.demote-already-min").replace("%member%", target.getName())));
            return;
        }

        PlayerRank newRank = currentRank == PlayerRank.SECONDARY_OWNER ? PlayerRank.ADMIN : PlayerRank.MEMBER;
        region.addMember(target.getUniqueId(), newRank);
        manager.getProtectionDao().saveMemberAsync(region.getDatabaseId(), target.getUniqueId(), newRank.name());

        String msgKey = newRank == PlayerRank.ADMIN ? "members.demoted-to-admin" : "members.demoted";
        player.sendMessage(mm.deserialize(msg(msgKey).replace("%member%", target.getName())));
        target.sendMessage(mm.deserialize(msg("members.notify-demoted").replace("%owner%", player.getName())));
    }

    private void handleListMembers(Player player, ProtectionRegion region) {
        if (region == null) {
            player.sendMessage(mm.deserialize(msg("general.not-in-protection")));
            return;
        }

        player.sendMessage(mm.deserialize(msg("members.list-header").replace("%protection%", region.getCustomName())));

        Map<UUID, PlayerRank> members = region.getMembers();
        if (members.isEmpty()) {
            player.sendMessage(mm.deserialize(msg("members.list-empty")));
            return;
        }

        for (Map.Entry<UUID, PlayerRank> entry : members.entrySet()) {
            UUID uuid = entry.getKey();
            PlayerRank rank = entry.getValue();
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            if (name == null)
                name = uuid.toString().substring(0, 8);

            String entryMsg = msg("members.list-entry")
                    .replace("%name%", name)
                    .replace("%rank%", rank.getDisplayName());
            player.sendMessage(mm.deserialize(entryMsg));
        }
    }

    private void handleViewPreview(Player player, ProtectionRegion region) {
        if (region == null) {
            player.sendMessage(mm.deserialize(msg("view.no-region")));
            return;
        }
        player.sendMessage(mm.deserialize(msg("view.preview-start")));
        ParticleUtils.showProtectionView(plugin, player, region);
        plugin.getGlowManager().spawnGlowTemp(region, 200);
    }

    private void handleListProtections(Player player) {
        List<ProtectionRegion> regions = manager.getRegionsByOwner(player.getUniqueId());
        if (regions.isEmpty()) {
            player.sendMessage(mm.deserialize(MessageUtils.lang("commands.list-no-protections")));
            return;
        }
        Location loc = player.getLocation();
        if (loc == null)
            return;
        player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
        plugin.getMenuManager().openMenu(player, new PlayerProtectionListMenu(plugin, player));
    }

    private void handleBuy(Player player) {
        Location loc = player.getLocation();
        if (loc == null)
            return;
        player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
        plugin.getMenuManager().openMenu(player, new BuyMenu(plugin));
    }

    private void handleMerge(Player player, ProtectionRegion region) {
        if (region == null) {
            player.sendMessage(mm.deserialize(msg("general.not-in-protection")));
            return;
        }
        if (!region.getOwnerUniqueId().equals(player.getUniqueId())
                && !player.hasPermission("amplprotections.admin.bypass")) {
            player.sendMessage(mm.deserialize(msg("members.no-permission-owner")));
            return;
        }

        boolean mergeEnabled = plugin.getConfig().getBoolean("merge.enabled", true);
        if (!mergeEnabled) {
            player.sendMessage(mm.deserialize(msg("merge.disabled")));
            return;
        }

        java.util.List<ProtectionRegion> adjacent = manager.getAdjacentRegions(region);
        if (adjacent.isEmpty()) {
            player.sendMessage(mm.deserialize(msg("merge.no-adjacent")));
            return;
        }

        Location loc = player.getLocation();
        if (loc == null)
            return;

        player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
        plugin.getMenuManager().openMenu(player, new MergeMenu(plugin, region));
    }

    private void handlePreset(Player player, ProtectionRegion region, String[] args) {
        if (args.length < 2) {
            if (region != null && region.getOwnerUniqueId().equals(player.getUniqueId())) {
                Location loc = player.getLocation();
                if (loc == null)
                    return;
                player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
                plugin.getMenuManager().openMenu(player, new PresetMenu(plugin, region, player));
            } else {
                player.sendMessage(mm.deserialize(msg("general.not-in-protection")));
            }
            return;
        }

        String subCmd = args[1].toLowerCase();
        switch (subCmd) {
            case "apply", "aplicar" -> {
                if (args.length < 3) {
                    player.sendMessage(mm.deserialize(msg("preset.usage-apply")));
                    return;
                }
                if (region == null || !region.getOwnerUniqueId().equals(player.getUniqueId())) {
                    player.sendMessage(mm.deserialize(msg("general.not-in-protection")));
                    return;
                }

                String presetName = args[2];
                com.amplan.amplprotections.model.FlagPreset preset = plugin.getPresetManager()
                        .getBuiltInPreset(presetName);
                if (preset == null) {
                    player.sendMessage(mm.deserialize(msg("preset.not-found")));
                    return;
                }

                preset.applyToRegion(region);
                manager.getProtectionDao().saveFlagsAsync(region);
                String msg = msg("preset.applied");
                player.sendMessage(mm.deserialize(
                        (msg.isEmpty() ? msg("preset.applied") : msg).replace("%preset%", presetName)));
            }
            case "create", "crear" -> {
                if (args.length < 3) {
                    player.sendMessage(mm.deserialize(msg("preset.usage-create")));
                    return;
                }
                if (region == null || !region.getOwnerUniqueId().equals(player.getUniqueId())) {
                    player.sendMessage(mm.deserialize(msg("general.not-in-protection")));
                    return;
                }

                String presetName = args[2];
                boolean created = plugin.getPresetManager().createCustomPreset(player, presetName,
                        new com.amplan.amplprotections.model.FlagPreset(presetName, player.getUniqueId(), false) {
                            {
                                getFlags().putAll(region.getFlags());
                            }
                        });

                if (created) {
                    String msg = msg("preset.create-success");
                    player.sendMessage(mm.deserialize(
                            (msg.isEmpty() ? msg("preset.create-success") : msg).replace("%name%", presetName)));
                } else {
                    String msg = msg("preset.create-exists");
                    player.sendMessage(
                            mm.deserialize((msg.isEmpty() ? msg("preset.create-exists") : msg)));
                }
            }
            case "delete", "eliminar" -> {
                if (args.length < 3) {
                    player.sendMessage(mm.deserialize(msg("preset.usage-delete")));
                    return;
                }
                String presetName = args[2];
                boolean deleted = plugin.getPresetManager().deleteCustomPreset(player, presetName);
                if (deleted) {
                    String msg = msg("preset.delete-success");
                    player.sendMessage(mm.deserialize(
                            (msg.isEmpty() ? msg("preset.delete-success") : msg).replace("%name%", presetName)));
                } else {
                    player.sendMessage(mm.deserialize(msg("preset.not-found")));
                }
            }
            default -> player.sendMessage(mm.deserialize(MessageUtils.lang("commands.preset-invalid-sub")));
        }
    }

    private void handleHologram(Player player, ProtectionRegion region) {
        if (region == null) {
            player.sendMessage(mm.deserialize(msg("hologram.not-in-protection")));
            return;
        }
        if (!region.getOwnerUniqueId().equals(player.getUniqueId())
                && !player.hasPermission("amplprotections.admin.bypass")) {
            player.sendMessage(mm.deserialize(msg("members.no-permission-owner")));
            return;
        }
        Location loc = player.getLocation();
        if (loc == null)
            return;
        if (!player.hasPermission(
                Objects.requireNonNullElse(
                        plugin.getConfig().getString("holograms.toggle-permission"),
                        "amplprotections.hologram.toggle"))) {
            player.sendMessage(parse("hologram.no-permission", "<red>No tienes permiso."));
            return;
        }

        boolean newState = !region.isHologramEnabled();
        region.setHologramEnabled(newState);
        manager.getProtectionDao().saveHologramStateAsync(region.getDatabaseId(), newState);

        if (newState) {
            plugin.getHologramManager().spawnHologram(region);
            player.sendMessage(mm.deserialize(msg("hologram.toggled-on")));
        } else {
            plugin.getHologramManager().removeHologram(region);
            player.sendMessage(mm.deserialize(msg("hologram.toggled-off")));
        }
    }

    private void handleRent(Player player, ProtectionRegion region, String[] args) {
        if (region == null) {
            player.sendMessage(mm.deserialize(msg("general.not-in-protection")));
            return;
        }

        if (args.length < 2) {
            if (region.getOwnerUniqueId().equals(player.getUniqueId())
                    || player.hasPermission("amplprotections.admin.bypass")) {
                Location loc = player.getLocation();
                if (loc == null)
                    return;
                player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
                plugin.getMenuManager().openMenu(player, new RentalMenu(plugin, region));
            } else {
                plugin.getRentalManager().acceptRental(region, player);
            }
            return;
        }

        String subCmd = args[1].toLowerCase();
        switch (subCmd) {
            case "set", "configurar" -> {
                if (!region.getOwnerUniqueId().equals(player.getUniqueId())
                        && !player.hasPermission("amplprotections.admin.bypass")) {
                    player.sendMessage(mm.deserialize(msg("members.no-permission-owner")));
                    return;
                }
                if (args.length < 4) {
                    player.sendMessage(mm.deserialize(msg("commands.rent-usage-set")));
                    return;
                }
                try {
                    double price = Double.parseDouble(args[2]);
                    int days = Integer.parseInt(args[3]);
                    int maxDays = plugin.getConfig().getInt("rental.max-rental-days", 30);
                    if (days > maxDays) {
                        player.sendMessage(
                                mm.deserialize(msg("commands.rent-max-days").replace("%max%", String.valueOf(maxDays))));
                        return;
                    }
                    plugin.getRentalManager().setRental(region, price, days, player);
                } catch (NumberFormatException e) {
                    player.sendMessage(mm.deserialize(msg("commands.rent-invalid-numbers")));
                }
            }
            case "cancel", "cancelar" -> {
                plugin.getRentalManager().cancelRental(region, player);
            }
            case "info", "informacion" -> {
                com.amplan.amplprotections.model.Rental rental = plugin.getRentalManager().getRental(region);
                if (rental != null) {
                    player.sendMessage(mm.deserialize(msg("commands.rent-info-title")));
                    player.sendMessage(mm.deserialize(msg("commands.rent-info-renter")
                            .replace("%renter%", Bukkit.getOfflinePlayer(rental.getRenterUuid()).getName())));
                    player.sendMessage(mm.deserialize(msg("commands.rent-info-price")
                            .replace("%price%", String.format("%.2f", rental.getPrice()))));
                    player.sendMessage(mm.deserialize(msg("commands.rent-info-days")
                            .replace("%days%", formatTime(rental.getRemainingSeconds()))));
                    player.sendMessage(mm.deserialize(msg("commands.rent-info-auto-renew")
                            .replace("%auto%", rental.isAutoRenew() ? msg("commands.yes") : msg("commands.no"))));
                } else {
                    player.sendMessage(mm.deserialize(msg("commands.rent-no-active")));
                }
            }
            default -> player.sendMessage(mm.deserialize(msg("commands.rent-invalid-sub")));
        }
    }

    private void handleRollback(Player player, ProtectionRegion region, String[] args) {
        if (!player.hasPermission("amplprotections.rollback.admin")) {
            player.sendMessage(mm.deserialize(msg("general.no-permission")));
            return;
        }
        if (region == null) {
            player.sendMessage(mm.deserialize(msg("general.not-in-protection")));
            return;
        }

        if (args.length < 2) {
            Location loc = player.getLocation();
            if (loc == null)
                return;
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            plugin.getMenuManager().openMenu(player, new RollbackMenu(plugin, region));
            return;
        }

        String subCmd = args[1].toLowerCase();
        switch (subCmd) {
            case "player", "jugador" -> {
                if (args.length < 4) {
                    player.sendMessage(mm.deserialize(msg("commands.rollback-usage-player")));
                    return;
                }
                org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                try {
                    int minutes = Integer.parseInt(args[3]);
                    long since = System.currentTimeMillis() - (minutes * 60L * 1000L);
                    plugin.getRollbackManager().executeRollback(region, target.getUniqueId(), since, player);
                } catch (NumberFormatException e) {
                    player.sendMessage(mm.deserialize(msg("commands.rollback-invalid-minutes")));
                }
            }
            case "all", "todo" -> {
                if (args.length < 3) {
                    player.sendMessage(mm.deserialize(msg("commands.rollback-usage-all")));
                    return;
                }
                try {
                    int minutes = Integer.parseInt(args[2]);
                    long since = System.currentTimeMillis() - (minutes * 60L * 1000L);
                    plugin.getRollbackManager().executeRollback(region, null, since, player);
                } catch (NumberFormatException e) {
                    player.sendMessage(mm.deserialize(msg("commands.rollback-invalid-minutes")));
                }
            }
            case "preview", "vista" -> {
                if (args.length < 3) {
                    player.sendMessage(mm.deserialize(msg("commands.rollback-usage-preview")));
                    return;
                }
                try {
                    int minutes = Integer.parseInt(args[2]);
                    long since = System.currentTimeMillis() - (minutes * 60L * 1000L);
                    plugin.getRollbackManager().getChangeCount(region, null, since).thenAccept(count -> {
                        player.sendMessage(mm.deserialize(msg("commands.rollback-preview-count")
                                .replace("%count%", String.valueOf(count))
                                .replace("%minutes%", String.valueOf(minutes))));
                    });
                } catch (NumberFormatException e) {
                    player.sendMessage(mm.deserialize(msg("commands.rollback-invalid-minutes")));
                }
            }
            default -> player.sendMessage(mm.deserialize(msg("commands.rollback-invalid-sub")));
        }
    }

    private String formatTime(long seconds) {
        if (seconds <= 0)
            return MessageUtils.lang("time-expired");
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long mins = (seconds % 3600) / 60;
        String dSuffix = MessageUtils.lang("time-days");
        String hSuffix = MessageUtils.lang("time-hours");
        String mSuffix = MessageUtils.lang("time-minutes");
        if (days > 0)
            return days + dSuffix + " " + hours + hSuffix;
        if (hours > 0)
            return hours + hSuffix + " " + mins + mSuffix;
        return mins + mSuffix;
    }

    private void handleFlagChange(Player player, ProtectionRegion region, String[] args) {
        if (region == null) {
            player.sendMessage(mm.deserialize(msg("general.not-in-protection")));
            return;
        }
        if (!region.getOwnerUniqueId().equals(player.getUniqueId())
                && !player.hasPermission("amplprotections.admin.bypass")) {
            player.sendMessage(mm.deserialize(msg("members.no-permission-owner")));
            return;
        }
        if (args.length < 3 || !manager.isValidFlag(args[1].toLowerCase())) {
            player.sendMessage(mm.deserialize(msg("commands.usage-flag")));
            return;
        }

        String flag = args[1].toLowerCase();

        org.bukkit.configuration.ConfigurationSection flagsMenu = plugin.getConfig()
                .getConfigurationSection("flags.menu");
        if (flagsMenu != null) {
            org.bukkit.configuration.ConfigurationSection flagSection = flagsMenu.getConfigurationSection(flag);
            if (flagSection != null && !flagSection.getBoolean("enabled", true)) {
                player.sendMessage(mm.deserialize(msg("commands.flag-disabled-by-admin")));
                return;
            }
        }

        boolean isBool = com.amplan.amplprotections.model.FlagPermissionLevel.isEnvironmental(flag);

        if (isBool) {
            String input = args[2].toLowerCase();
            boolean boolValue;
            switch (input) {
                case "true", "on", "1" -> boolValue = true;
                case "false", "off", "0" -> boolValue = false;
                default -> {
                    player.sendMessage(mm.deserialize(msg("commands.flag-invalid-value")));
                    return;
                }
            }
            region.setBooleanFlag(flag, boolValue);
            plugin.getProtectionManager().getProtectionDao().saveBooleanFlagAsync(region.getDatabaseId(), flag, boolValue);
            String stateMsg = boolValue ? msg("commands.flag-boolean-state-on") : msg("commands.flag-boolean-state-off");
            player.sendMessage(mm.deserialize(msg("commands.flag-updated")
                    .replace("%flag%", MessageUtils.getFlagName(flag))
                    .replace("%value%", stateMsg)));
        } else {
            String input = args[2].toLowerCase();
            com.amplan.amplprotections.model.FlagPermissionLevel level;
            try {
                level = com.amplan.amplprotections.model.FlagPermissionLevel.valueOf(input.toUpperCase());
            } catch (IllegalArgumentException e) {
                level = Boolean.parseBoolean(input) ? com.amplan.amplprotections.model.FlagPermissionLevel.EVERYONE
                        : com.amplan.amplprotections.model.FlagPermissionLevel.NONE;
            }

            region.setFlagLevel(flag, level);
            plugin.getProtectionManager().getProtectionDao().saveFlagAsync(region.getDatabaseId(), flag, level);
            String levelDisplay = switch (level) {
                case NONE -> MessageUtils.lang("flag-levels.none.display");
                case OWNER -> MessageUtils.lang("flag-levels.owner.display");
                case MEMBERS -> MessageUtils.lang("flag-levels.members.display");
                case ADMINS -> MessageUtils.lang("flag-levels.admins.display");
                case EVERYONE -> MessageUtils.lang("flag-levels.everyone.display");
            };
            player.sendMessage(mm.deserialize(msg("commands.flag-updated")
                    .replace("%flag%", MessageUtils.getFlagName(flag))
                    .replace("%value%", levelDisplay)));
        }
    }

    private void handleTransfer(Player player, ProtectionRegion region, String[] args) {
        if (region == null) {
            player.sendMessage(mm.deserialize(msg("general.not-in-protection")));
            return;
        }
        if (!region.getOwnerUniqueId().equals(player.getUniqueId())
                && !player.hasPermission("amplprotections.admin.bypass")) {
            player.sendMessage(mm.deserialize(msg("transfer.no-permission")));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(mm.deserialize(msg("transfer.usage")));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(mm.deserialize(msg("general.player-not-found")));
            return;
        }

        if (region.getOwnerUniqueId().equals(target.getUniqueId())) {
            player.sendMessage(mm.deserialize(msg("transfer.already-owner")));
            return;
        }

        region.getMembers().remove(target.getUniqueId());
        manager.getProtectionDao().deleteMemberAsync(region.getDatabaseId(), target.getUniqueId());

        manager.getProtectionDao().transferOwnershipAsync(region.getDatabaseId(), target.getUniqueId()).thenRun(() -> {
            manager.updateOwnerInMemory(region, target.getUniqueId());
        });

        player.sendMessage(mm.deserialize(msg("transfer.success")
                .replace("%player%", target.getName())
                .replace("%region%", region.getCustomName())));
        target.sendMessage(mm.deserialize(msg("transfer.notify-received")
                .replace("%owner%", player.getName())
                .replace("%region%", region.getCustomName())));

        if (plugin.getHologramManager() != null) {
            plugin.getHologramManager().removeHologram(region);
            if (region.isHologramEnabled()) {
                plugin.getHologramManager().spawnHologram(region);
            }
        }
    }

    private void handleBulkManage(Player player) {
        List<ProtectionRegion> regions = manager.getRegionsByOwner(player.getUniqueId());
        if (regions.isEmpty()) {
            player.sendMessage(mm.deserialize(msg("bulk-menu.no-protections")));
            return;
        }
        Location loc = player.getLocation();
        if (loc == null) return;
        player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
        plugin.getMenuManager().openMenu(player, new BulkManageMenu(plugin, player));
    }

    private boolean canManageMembers(Player player, ProtectionRegion region) {
        if (region.getOwnerUniqueId().equals(player.getUniqueId()))
            return true;
        if (player.hasPermission("amplprotections.admin.bypass"))
            return true;
        PlayerRank rank = region.getMembers().get(player.getUniqueId());
        return rank != null && rank.canManage();
    }

    private void handleDebug(Player player, ProtectionRegion region, String[] args) {
        if (!player.hasPermission("amplprotections.admin.bypass") && !player.isOp()) {
            player.sendMessage(mm.deserialize(msg("commands.debug-no-permission")));
            return;
        }

        player.sendMessage(mm.deserialize(msg("commands.debug-title")));

        player.sendMessage(mm.deserialize("<gray>Player: <white>" + player.getName()));
        player.sendMessage(mm.deserialize("<gray>UUID: <white>" + player.getUniqueId()));
        
        Location loc = Objects.requireNonNull(player.getLocation());
        String world = Objects.requireNonNull(loc.getWorld()).getName();

        player.sendMessage(mm.deserialize("<gray>Location: <white>" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()));
        player.sendMessage(mm.deserialize("<gray>World: <white>" + world));
        player.sendMessage(mm.deserialize("<gray>Has Admin Bypass: <white>" + player.hasPermission("amplprotections.admin.bypass")));
        player.sendMessage(mm.deserialize("<gray>Is Op: <white>" + player.isOp()));

        if (region == null) {
            player.sendMessage(mm.deserialize(msg("commands.debug-no-protection")));
        } else {
            player.sendMessage(mm.deserialize(""));
            player.sendMessage(mm.deserialize(msg("commands.debug-protection-title")));
            player.sendMessage(mm.deserialize(msg("commands.debug-protection-id").replace("%id%", region.getLandId())));
            player.sendMessage(mm.deserialize(msg("commands.debug-protection-owner")
                    .replace("%owner%", region.getOwnerName() + " (" + region.getOwnerUniqueId() + ")")));
            player.sendMessage(mm.deserialize(msg("commands.debug-protection-type").replace("%type%", region.getTypeId())));
            player.sendMessage(
                    mm.deserialize("<gray>Is Owner: <white>" + region.getOwnerUniqueId().equals(player.getUniqueId())));
            player.sendMessage(mm.deserialize("<gray>Is Member: <white>" + region.isMember(player.getUniqueId())));
            player.sendMessage(mm.deserialize("<gray>Player Rank: <white>" + region.getRank(player.getUniqueId())));
            player.sendMessage(mm.deserialize("<gray>Bounds: <white>" + region.getMinX() + "," + region.getMinZ()
                    + " - " + region.getMaxX() + "," + region.getMaxZ()));

            player.sendMessage(mm.deserialize(""));
            player.sendMessage(mm.deserialize(msg("commands.debug-flags-title")));

            List<String> flagKeys = new ArrayList<>(region.getFlags().keySet());
            flagKeys.sort(String::compareToIgnoreCase);

            for (String flag : flagKeys) {
                boolean isBool = com.amplan.amplprotections.model.FlagPermissionLevel.isEnvironmental(flag);
                if (isBool) {
                    boolean enabled = region.isBooleanFlagEnabled(flag);
                    String stateColor = enabled ? msg("commands.flag-boolean-state-on") : msg("commands.flag-boolean-state-off");
                    player.sendMessage(mm.deserialize(msg("commands.debug-flag-boolean")
                            .replace("%flag%", MessageUtils.getFlagName(flag))
                            .replace("%state%", stateColor)));
                } else {
                    com.amplan.amplprotections.model.FlagPermissionLevel level = region.getFlagLevel(flag);
                    boolean canAct = region.canPlayerAct(flag, player.getUniqueId());
                    String canActStr = canAct ? msg("commands.yes") : msg("commands.no");
                    player.sendMessage(mm.deserialize(msg("commands.debug-flag-granular")
                            .replace("%flag%", MessageUtils.getFlagName(flag))
                            .replace("%level%", level.getDisplayName())
                            .replace("%can%", canActStr)));
                }
            }

            if (args.length > 1) {
                String testFlag = args[1].toLowerCase();
                boolean testIsBool = com.amplan.amplprotections.model.FlagPermissionLevel.isEnvironmental(testFlag);

                player.sendMessage(mm.deserialize(""));
                player.sendMessage(mm.deserialize(msg("commands.debug-flag-test-title").replace("%flag%", testFlag)));

                if (testIsBool) {
                    boolean testEnabled = region.isBooleanFlagEnabled(testFlag);
                    player.sendMessage(mm.deserialize(msg("commands.debug-flag-test-type").replace("%type%", msg("commands.boolean-type"))));
                    player.sendMessage(mm.deserialize("<gray>isBooleanFlagEnabled: <white>" + testEnabled));

                    if (args.length > 2) {
                        String newValStr = args[2].toLowerCase();
                        boolean newVal;
                        switch (newValStr) {
                            case "true", "on", "1" -> newVal = true;
                            case "false", "off", "0" -> newVal = false;
                            default -> {
                                player.sendMessage(mm.deserialize(msg("commands.flag-invalid-value")));
                                return;
                            }
                        }
                        region.setBooleanFlag(testFlag, newVal);
                        manager.getProtectionDao().saveBooleanFlagAsync(region.getDatabaseId(), testFlag, newVal);
                        String stateStr = newVal ? msg("commands.flag-boolean-state-on") : msg("commands.flag-boolean-state-off");
                        player.sendMessage(mm.deserialize(msg("commands.debug-flag-changed")
                                .replace("%flag%", MessageUtils.getFlagName(testFlag))
                                .replace("%value%", stateStr)));
                    }
                } else {
                    com.amplan.amplprotections.model.FlagPermissionLevel testLevel = region.getFlagLevel(testFlag);
                    boolean testCanAct = region.canPlayerAct(testFlag, player.getUniqueId());
                    boolean testCanPerform = region.canPerformAction(testFlag, region.getRank(player.getUniqueId()));

                    player.sendMessage(mm.deserialize(msg("commands.debug-flag-test-type").replace("%type%", msg("commands.granular-type"))));
                    player.sendMessage(mm.deserialize(msg("commands.debug-flag-test-level")
                            .replace("%level%", testLevel.getDisplayName())
                            .replace("%value%", String.valueOf(testLevel.getValue()))));
                    player.sendMessage(mm.deserialize(msg("commands.debug-flag-test-can-act")
                            .replace("%can%", String.valueOf(testCanAct))));
                    player.sendMessage(mm.deserialize(msg("commands.debug-flag-test-can-perform")
                            .replace("%can%", String.valueOf(testCanPerform))));
                    player.sendMessage(mm.deserialize("<gray>isFlagEnabled: <white>" + region.isFlagEnabled(testFlag)));

                    if (args.length > 2) {
                        String newLevelStr = args[2].toUpperCase();
                        try {
                            com.amplan.amplprotections.model.FlagPermissionLevel newLevel = com.amplan.amplprotections.model.FlagPermissionLevel
                                    .valueOf(newLevelStr);
                            region.setFlagLevel(testFlag, newLevel);
                            manager.getProtectionDao().saveFlagAsync(region.getDatabaseId(), testFlag, newLevel);
                            boolean afterCanAct = region.canPlayerAct(testFlag, player.getUniqueId());
                            player.sendMessage(mm.deserialize(msg("commands.debug-flag-changed")
                                    .replace("%flag%", MessageUtils.getFlagName(testFlag))
                                    .replace("%value%", newLevel.getDisplayName())));
                            player.sendMessage(
                                    mm.deserialize(msg("commands.debug-can-act-after").replace("%can%", String.valueOf(afterCanAct))));
                        } catch (IllegalArgumentException e) {
                            player.sendMessage(
                                    mm.deserialize(msg("commands.flag-invalid-level")));
                        }
                    }
                }
            }

            player.sendMessage(mm.deserialize(""));
            player.sendMessage(mm.deserialize(msg("commands.debug-members-header").replace("%count%", String.valueOf(region.getMembers().size()))));
            for (Map.Entry<UUID, PlayerRank> entry : region.getMembers().entrySet()) {
                String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                if (name == null)
                    name = entry.getKey().toString().substring(0, 8);
                player.sendMessage(mm.deserialize(msg("commands.debug-member-entry")
                        .replace("%name%", name)
                        .replace("%rank%", entry.getValue().getDisplayName())));
            }
        }
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(mm.deserialize(msg("commands.help-title")));
        player.sendMessage(mm.deserialize(msg("commands.help-menu")));
        player.sendMessage(mm.deserialize(msg("commands.help-add")));
        player.sendMessage(mm.deserialize(msg("commands.help-remove")));
        player.sendMessage(mm.deserialize(msg("commands.help-promote")));
        player.sendMessage(mm.deserialize(msg("commands.help-demote")));
        player.sendMessage(mm.deserialize(msg("commands.help-members")));
        player.sendMessage(mm.deserialize(msg("commands.help-flag")));
        player.sendMessage(mm.deserialize(msg("commands.help-view")));
        player.sendMessage(mm.deserialize(msg("commands.help-list")));
        player.sendMessage(mm.deserialize(msg("commands.help-buy")));
        player.sendMessage(mm.deserialize(msg("commands.help-merge")));
        player.sendMessage(mm.deserialize(msg("commands.help-preset")));
        player.sendMessage(mm.deserialize(msg("commands.help-hologram")));
        player.sendMessage(mm.deserialize(msg("commands.help-rent")));
        player.sendMessage(mm.deserialize(msg("commands.help-rollback")));
        player.sendMessage(mm.deserialize(msg("commands.help-transfer")));
        player.sendMessage(mm.deserialize(msg("commands.help-bulk")));
        player.sendMessage(mm.deserialize(msg("commands.help-info")));
        player.sendMessage(mm.deserialize(msg("commands.help-lore")));
        player.sendMessage(mm.deserialize(msg("commands.debug-help-line")));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> subs = List.of("menu", "info", "lore", "add", "remove", "promote", "demote", "members", "flag",
                    "view", "list", "buy", "merge", "preset", "hologram", "rent", "rollback", "transfer", "bulk", "debug");
            return subs.stream().filter(s -> s.startsWith(input)).collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("flag")) {
            return new ArrayList<>(manager.getAllAllowedFlags());
        }
        if (args.length == 2 && List.of("add", "remove", "promote", "demote").contains(args[0].toLowerCase())) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
            ProtectionRegion debugRegion = manager.getRegionAt(((Player) sender).getLocation());
            if (debugRegion != null) {
                return new ArrayList<>(debugRegion.getFlags().keySet());
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("flag")) {
            String flag = args[1].toLowerCase();
            if (com.amplan.amplprotections.model.FlagPermissionLevel.isEnvironmental(flag)) {
                return List.of("true", "false");
            }
            return List.of("NONE", "OWNER", "MEMBERS", "ADMINS", "EVERYONE");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("debug")) {
            String debugFlag = args[1].toLowerCase();
            if (com.amplan.amplprotections.model.FlagPermissionLevel.isEnvironmental(debugFlag)) {
                return List.of("true", "false");
            }
            return List.of("NONE", "OWNER", "MEMBERS", "ADMINS", "EVERYONE");
        }
        return null;
    }
}
