package com.amplan.amplprotections.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.amplan.amplprotections.AmplProtections;
import com.amplan.amplprotections.manager.ProtectionManager;
import com.amplan.amplprotections.menu.AdminMenu;
import com.amplan.amplprotections.model.ProtectionRegion;
import com.amplan.amplprotections.utils.MessageUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class AdminProtectionCommand implements CommandExecutor, TabCompleter {

    private final AmplProtections plugin;
    private final ProtectionManager manager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public AdminProtectionCommand(AmplProtections plugin) {
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
        if (!sender.hasPermission("amplprotections.admin.use")) {
            sender.sendMessage(mm.deserialize(msg("general.no-permission")));
            return true;
        }

        if (args.length == 0) {
            sendAdminHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "give" -> handleGive(sender, args);
            case "list" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(mm.deserialize(msg("admin.only-players-list")));
                    return true;
                }
                plugin.getMenuManager().openMenu(player, new AdminMenu(plugin, player, 1));
            }
            case "info" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(mm.deserialize(msg("admin.info-only-players")));
                    return true;
                }
                handleInfo(player);
            }
            case "delete" -> handleDelete(sender);
            case "reload" -> {
                plugin.reloadPluginFiles();
                sender.sendMessage(mm.deserialize(msg("general.reload-success")));
            }
            default -> sender.sendMessage(mm.deserialize(msg("admin.subcommand-invalid")));
        }
        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(mm.deserialize(msg("admin.give-usage")));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(mm.deserialize(msg("general.player-not-found")));
            return;
        }

        String typeId = args[2];
        if (!manager.isValidProtectionType(typeId)) {
            sender.sendMessage(mm.deserialize(msg("admin.type-not-found").replace("%type%", typeId)));
            return;
        }

        ItemStack item = manager.createProtectionItem(typeId);
        target.getInventory().addItem(item);
        sender.sendMessage(mm.deserialize(msg("admin.give-success")
                .replace("%type%", typeId)
                .replace("%player%", target.getName())));
    }

    private void handleInfo(Player player) {
        Location loc = player.getLocation();
        ProtectionRegion region = manager.getRegionAt(loc);
        if (region == null) {
            player.sendMessage(mm.deserialize(msg("admin.info-no-region")));
            return;
        }

        OfflinePlayer owner = Bukkit.getOfflinePlayer(region.getOwnerUniqueId());
        String ownerName = owner.getName() != null ? owner.getName() : "Desconocido";

        player.sendMessage(mm.deserialize(msg("admin.info-title")));
        player.sendMessage(mm.deserialize(msg("admin.info-config-id").replace("%type%", region.getTypeId())));
        player.sendMessage(mm.deserialize(msg("admin.info-owner").replace("%owner%", ownerName)));
        player.sendMessage(mm.deserialize(msg("admin.info-center")
                .replace("%x%", String.valueOf(region.getBlockX()))
                .replace("%y%", String.valueOf(region.getBlockY()))
                .replace("%z%", String.valueOf(region.getBlockZ()))));
    }

    private void handleDelete(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(parse("general.only-players", "<red>Only players."));
            return;
        }
        ProtectionRegion region = manager.getRegionAt(player.getLocation());
        if (region == null) {
            player.sendMessage(mm.deserialize(
                    Objects.requireNonNullElse(
                            msg("admin.delete-no-region"),
                            "<red>No region found.")));
            return;
        }
        manager.removeRegion(region);
        player.sendMessage(mm.deserialize(
                Objects.requireNonNullElse(
                        msg("admin.delete-done"),
                        "<green>Region deleted.")));
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage(mm.deserialize(msg("admin.help-title")));
        sender.sendMessage(mm.deserialize(msg("admin.help-give")));
        sender.sendMessage(mm.deserialize(msg("admin.help-other")));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (!sender.hasPermission("amplprotections.admin.use"))
            return completions;

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (String sub : List.of("give", "list", "info", "delete", "reload")) {
                if (sub.startsWith(input))
                    completions.add(sub);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            ConfigurationSection section = plugin.getBlocksConfig().getConfigurationSection("protection-blocks");
            if (section != null) {
                String input = args[2].toLowerCase();
                for (String key : section.getKeys(false)) {
                    if (key.toLowerCase().startsWith(input))
                        completions.add(key);
                }
            }
        }
        return completions;
    }
}
