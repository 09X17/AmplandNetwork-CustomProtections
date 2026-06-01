package com.amplan.amplprotections.menu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import com.amplan.amplprotections.AmplProtections;
import com.amplan.amplprotections.model.PlayerRank;
import com.amplan.amplprotections.model.ProtectionRegion;
import com.amplan.amplprotections.utils.ItemBuilder;
import com.amplan.amplprotections.utils.MessageUtils;
import com.amplan.amplprotections.utils.SkullUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class MembersMenu implements MenuManager.CustomMenu {

    private final AmplProtections plugin;
    private final ProtectionRegion region;
    private final Inventory inventory;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<Integer, UUID> slotToMember = new HashMap<>();

    private final String menuTitle;
    private final Material borderMaterial;
    private final String borderName;
    private final int borderCustomModelData;
    private final String borderSkullValue;
    private final Material separatorMaterial;
    private final String separatorName;
    private final int separatorCustomModelData;
    private final String separatorSkullValue;
    private final Material headerMaterial;
    private final String headerDisplayName;
    private final List<String> headerLore;
    private final int headerCustomModelData;
    private final String headerSkullValue;
    private final Material memberMaterial;
    private final String memberDisplayName;
    private final List<String> memberLore;
    private final int memberCustomModelData;
    private final String memberSkullValue;
    private final Material emptyMaterial;
    private final String emptyDisplayName;
    private final List<String> emptyLore;
    private final int emptyCustomModelData;
    private final String emptySkullValue;
    private final int backSlot;
    private final Material backMaterial;
    private final String backDisplayName;
    private final List<String> backLore;
    private final int backCustomModelData;
    private final String backSkullValue;
    private final int refreshSlot;
    private final Material refreshMaterial;
    private final String refreshDisplayName;
    private final List<String> refreshLore;
    private final int refreshCustomModelData;
    private final String refreshSkullValue;
    private final int kickAllSlot;
    private final Material kickAllMaterial;
    private final String kickAllDisplayName;
    private final List<String> kickAllLore;
    private final int kickAllCustomModelData;
    private final String kickAllSkullValue;
    private final String rankOwnerDisplay;
    private final String rankOwnerColor;
    private final String rankSecondaryOwnerDisplay;
    private final String rankSecondaryOwnerColor;
    private final String rankAdminDisplay;
    private final String rankAdminColor;
    private final String rankMemberDisplay;
    private final String rankMemberColor;
    private final String actionPromote;
    private final String actionPromoteToSecondary;
    private final String actionDemoteToAdmin;
    private final String actionKickSecondaryOwner;
    private final String actionKickAdmin;
    private final String actionKickMember;

    private Component parse(String key, String fallback) {
        return Objects.requireNonNull(
                mm.deserialize(Objects.requireNonNullElse(msg(key), fallback)));
    }

    @SuppressWarnings("ThisEscapedInObjectConstruction")
    public MembersMenu(AmplProtections plugin, ProtectionRegion region) {
        this.plugin = plugin;
        this.region = region;

        org.bukkit.configuration.file.FileConfiguration menuConfig = plugin.getMenuConfigManager().getMembersMenu();

        this.menuTitle = getConfigString(menuConfig, "title", MessageUtils.lang("members.menu-title"));
        this.inventory = Bukkit.createInventory(this, 54, mm.deserialize(menuTitle));

        ConfigurationSection borderCfg = menuConfig != null ? menuConfig.getConfigurationSection("border") : null;
        this.borderMaterial = getMaterial(borderCfg, "material", Material.BLACK_STAINED_GLASS_PANE);
        this.borderName = getConfigString(borderCfg, "display-name", "<dark_gray> ");
        this.borderCustomModelData = getCustomModelData(borderCfg, "custom-model-data", -1);
        this.borderSkullValue = getConfigString(borderCfg, "skull-value", null);

        ConfigurationSection sepCfg = menuConfig != null ? menuConfig.getConfigurationSection("separator") : null;
        this.separatorMaterial = getMaterial(sepCfg, "material", Material.GRAY_STAINED_GLASS_PANE);
        this.separatorName = getConfigString(sepCfg, "display-name", "<dark_gray> ");
        this.separatorCustomModelData = getCustomModelData(sepCfg, "custom-model-data", -1);
        this.separatorSkullValue = getConfigString(sepCfg, "skull-value", null);

        ConfigurationSection headerCfg = menuConfig != null ? menuConfig.getConfigurationSection("header") : null;
        this.headerMaterial = getMaterial(headerCfg, "material", Material.KNOWLEDGE_BOOK);
        this.headerDisplayName = getConfigString(headerCfg, "display-name", MessageUtils.lang("members.menu-info-btn"));
        this.headerLore = getConfigList(headerCfg, "lore", Arrays.asList(
                MessageUtils.lang("members.menu-info-lore-protection"),
                MessageUtils.lang("members.menu-info-lore-owner"),
                MessageUtils.lang("members.menu-info-lore-members"),
                "",
                MessageUtils.lang("members.menu-info-lore-left"),
                MessageUtils.lang("members.menu-info-lore-right")));
        this.headerCustomModelData = getCustomModelData(headerCfg, "custom-model-data", -1);
        this.headerSkullValue = getConfigString(headerCfg, "skull-value", null);

        ConfigurationSection memberCfg = menuConfig != null ? menuConfig.getConfigurationSection("member-item") : null;
        this.memberMaterial = getMaterial(memberCfg, "material", Material.PLAYER_HEAD);
        this.memberDisplayName = getConfigString(memberCfg, "display-name", "%color%<b>%name%</b>");
        this.memberLore = getConfigList(memberCfg, "lore", Arrays.asList(
                "<gray>Rango: %color%%rank%",
                "<gray>Se unio: %joined%",
                "",
                "%action%"));
        this.memberCustomModelData = getCustomModelData(memberCfg, "custom-model-data", -1);
        this.memberSkullValue = getConfigString(memberCfg, "skull-value", null);

        ConfigurationSection emptyCfg = menuConfig != null ? menuConfig.getConfigurationSection("empty-item") : null;
        this.emptyMaterial = getMaterial(emptyCfg, "material", Material.PLAYER_HEAD);
        this.emptyDisplayName = getConfigString(emptyCfg, "display-name", MessageUtils.lang("members.menu-empty-btn"));
        this.emptyLore = getConfigList(emptyCfg, "lore", Arrays.asList(
                MessageUtils.lang("members.menu-empty-lore-1"),
                "",
                MessageUtils.lang("members.menu-empty-lore-2"),
                MessageUtils.lang("members.menu-empty-lore-3")));
        this.emptyCustomModelData = getCustomModelData(emptyCfg, "custom-model-data", -1);
        this.emptySkullValue = getConfigString(emptyCfg, "skull-value", null);

        ConfigurationSection backCfg = menuConfig != null ? menuConfig.getConfigurationSection("back-button") : null;
        this.backSlot = backCfg != null ? backCfg.getInt("slot", 49) : 49;
        this.backMaterial = getMaterial(backCfg, "material", Material.ARROW);
        this.backDisplayName = getConfigString(backCfg, "display-name", MessageUtils.lang("members.menu-back-btn"));
        this.backLore = getConfigList(backCfg, "lore",
                Collections.singletonList(MessageUtils.lang("members.menu-back-lore")));
        this.backCustomModelData = getCustomModelData(backCfg, "custom-model-data", -1);
        this.backSkullValue = getConfigString(backCfg, "skull-value", null);

        ConfigurationSection refreshCfg = menuConfig != null ? menuConfig.getConfigurationSection("refresh-button")
                : null;
        this.refreshSlot = refreshCfg != null ? refreshCfg.getInt("slot", 47) : 47;
        this.refreshMaterial = getMaterial(refreshCfg, "material", Material.COMPASS);
        this.refreshDisplayName = getConfigString(refreshCfg, "display-name", MessageUtils.lang("members.menu-refresh-btn"));
        this.refreshLore = getConfigList(refreshCfg, "lore",
                Collections.singletonList(MessageUtils.lang("members.menu-refresh-lore")));
        this.refreshCustomModelData = getCustomModelData(refreshCfg, "custom-model-data", -1);
        this.refreshSkullValue = getConfigString(refreshCfg, "skull-value", null);

        ConfigurationSection kickAllCfg = menuConfig != null ? menuConfig.getConfigurationSection("kick-all-button")
                : null;
        this.kickAllSlot = kickAllCfg != null ? kickAllCfg.getInt("slot", 51) : 51;
        this.kickAllMaterial = getMaterial(kickAllCfg, "material", Material.TNT);
        this.kickAllDisplayName = getConfigString(kickAllCfg, "display-name", MessageUtils.lang("members.menu-kick-all-btn"));
        this.kickAllLore = getConfigList(kickAllCfg, "lore", Arrays.asList(
                MessageUtils.lang("members.menu-kick-all-lore-1"),
                "",
                MessageUtils.lang("members.menu-kick-all-lore-2")));
        this.kickAllCustomModelData = getCustomModelData(kickAllCfg, "custom-model-data", -1);
        this.kickAllSkullValue = getConfigString(kickAllCfg, "skull-value", null);

        ConfigurationSection ranksCfg = menuConfig != null ? menuConfig.getConfigurationSection("ranks") : null;
        this.rankOwnerDisplay = getConfigString(ranksCfg, "owner.display", MessageUtils.lang("info.member-owner"));
        this.rankOwnerColor = getConfigString(ranksCfg, "owner.color", "<gold>");
        this.rankSecondaryOwnerDisplay = getConfigString(ranksCfg, "secondary-owner.display", MessageUtils.lang("info.member-secondary-owner"));
        this.rankSecondaryOwnerColor = getConfigString(ranksCfg, "secondary-owner.color", "<light_purple>");
        this.rankAdminDisplay = getConfigString(ranksCfg, "admin.display", MessageUtils.lang("info.member-admin"));
        this.rankAdminColor = getConfigString(ranksCfg, "admin.color", "<red>");
        this.rankMemberDisplay = getConfigString(ranksCfg, "member.display", MessageUtils.lang("info.member-member"));
        this.rankMemberColor = getConfigString(ranksCfg, "member.color", "<green>");

        ConfigurationSection actionsCfg = menuConfig != null ? menuConfig.getConfigurationSection("actions") : null;
        this.actionPromote = getConfigString(actionsCfg, "promote-to-admin", "<green>Clic derecho: Ascender a Admin");
        this.actionPromoteToSecondary = getConfigString(actionsCfg, "promote-to-secondary",
                "<light_purple>Clic derecho: Ascender a Dueno Secundario");
        this.actionDemoteToAdmin = getConfigString(actionsCfg, "demote-to-admin",
                "<yellow>Clic derecho: Degradar a Admin");
        this.actionKickSecondaryOwner = getConfigString(actionsCfg, "kick-secondary-owner",
                "<red>Clic izquierdo: Expulsar Dueno Secundario");
        this.actionKickAdmin = getConfigString(actionsCfg, "kick-admin", "<red>Clic izquierdo: Expulsar Admin");
        this.actionKickMember = getConfigString(actionsCfg, "kick-member", "<red>Clic izquierdo: Expulsar Miembro");
    }

    public void setMenuItems() {
        inventory.clear();
        slotToMember.clear();
        fillBorder();
        buildHeader();
        buildMemberList();
        buildFooter();
    }

    public void setMenuItems(Player player) {
        setMenuItems();
    }

    private void fillBorder() {
        ItemStack border = buildConfigurableItem(borderMaterial, borderName, Collections.emptyList(),
                borderCustomModelData, borderSkullValue, null);
        for (int i = 0; i < 9; i++)
            inventory.setItem(i, border);
        for (int i = 45; i < 54; i++)
            inventory.setItem(i, border);
        for (int row = 1; row <= 4; row++) {
            inventory.setItem(row * 9, border);
            inventory.setItem(row * 9 + 8, border);
        }
    }

    private void buildHeader() {
        int memberCount = region.getMembers().size();
        String[] loreArray = headerLore.stream()
                .map(line -> line.replace("%name%", region.getCustomName())
                        .replace("%owner%", region.getOwnerName())
                        .replace("%count%", String.valueOf(memberCount)))
                .toArray(String[]::new);

        ItemStack infoItem = buildConfigurableItem(headerMaterial, headerDisplayName, Arrays.asList(loreArray),
                headerCustomModelData, headerSkullValue, null);
        inventory.setItem(4, infoItem);

        ItemStack separator = buildConfigurableItem(separatorMaterial, separatorName, Collections.emptyList(),
                separatorCustomModelData, separatorSkullValue, null);
        inventory.setItem(3, separator);
        inventory.setItem(5, separator);
        inventory.setItem(2, separator.clone());
        inventory.setItem(6, separator.clone());
    }

    private void buildMemberList() {
        int[] memberSlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        };

        List<UUID> allMembers = new ArrayList<>();
        allMembers.add(region.getOwnerUniqueId());
        allMembers.addAll(region.getMembers().keySet());

        if (allMembers.isEmpty()) {
            String[] emptyLoreArray = emptyLore.toArray(String[]::new);
            ItemStack emptyItem = buildConfigurableItem(emptyMaterial, emptyDisplayName, Arrays.asList(emptyLoreArray),
                    emptyCustomModelData, emptySkullValue, null);
            inventory.setItem(22, emptyItem);
            return;
        }

        for (int i = 0; i < allMembers.size() && i < memberSlots.length; i++) {
            UUID memberUuid = allMembers.get(i);
            OfflinePlayer target = Bukkit.getOfflinePlayer(memberUuid);
            String targetName = target.getName() != null ? target.getName() : MessageUtils.lang("members.rank-unknown");
            PlayerRank rank = region.getRank(memberUuid);

            String rankDisplay = getRankDisplay(rank);
            String rankColor = getRankColor(rank);

            String actionLore;
            String kickAction;

            switch (rank) {
                case OWNER -> {
                    actionLore = MessageUtils.lang("members.rank-cannot-change-owner");
                    kickAction = "";
                }
                case SECONDARY_OWNER -> {
                    actionLore = actionDemoteToAdmin;
                    kickAction = actionKickSecondaryOwner;
                }
                case ADMIN -> {
                    actionLore = actionPromoteToSecondary;
                    kickAction = actionKickAdmin;
                }
                default -> {
                    actionLore = actionPromote;
                    kickAction = actionKickMember;
                }
            }

            String displayName = memberDisplayName
                    .replace("%color%", rankColor)
                    .replace("%name%", targetName);

            Long joinTimestamp = region.getMemberJoinDate(memberUuid);
            String joinDateStr = formatJoinDate(joinTimestamp);

            List<String> processedLore = new ArrayList<>();
            for (String line : memberLore) {
                processedLore.add(line
                        .replace("%color%", rankColor)
                        .replace("%rank%", rankDisplay)
                        .replace("%action%", actionLore)
                        .replace("%kick%", kickAction)
                        .replace("%joined%", joinDateStr));
            }

            ItemStack head = buildConfigurableItem(memberMaterial, displayName, processedLore, memberCustomModelData,
                    memberSkullValue, target);

            inventory.setItem(memberSlots[i], head);
            slotToMember.put(memberSlots[i], memberUuid);
        }
    }

    private void buildFooter() {
        ItemStack backBtn = buildConfigurableItem(backMaterial, backDisplayName, backLore, backCustomModelData,
                backSkullValue, null);
        inventory.setItem(backSlot, backBtn);

        ItemStack refreshBtn = buildConfigurableItem(refreshMaterial, refreshDisplayName, refreshLore,
                refreshCustomModelData, refreshSkullValue, null);
        inventory.setItem(refreshSlot, refreshBtn);

        ItemStack removeAllBtn = buildConfigurableItem(kickAllMaterial, kickAllDisplayName, kickAllLore,
                kickAllCustomModelData, kickAllSkullValue, null);
        inventory.setItem(kickAllSlot, removeAllBtn);
    }

    private ItemStack buildConfigurableItem(Material material, String displayName, List<String> lore,
            int customModelData, String skullValue, OfflinePlayer player) {
        ItemStack item;
        if (material == Material.PLAYER_HEAD && skullValue != null && !skullValue.isEmpty()) {
            item = SkullUtils.createSkullWithTexture(skullValue);
        } else if (material == Material.PLAYER_HEAD && player != null) {
            item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(player);
                item.setItemMeta(meta);
            }
        } else {
            item = new ItemBuilder(material)
                    .setDisplayName(displayName)
                    .setLore(lore.toArray(String[]::new))
                    .build();
        }

        if (material == Material.PLAYER_HEAD) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (displayName != null) {
                    meta.displayName(mm.deserialize(displayName));
                }
                if (!lore.isEmpty()) {
                    List<net.kyori.adventure.text.Component> components = new ArrayList<>();
                    for (String line : lore) {
                        components.add(mm.deserialize(line));
                    }
                    meta.lore(components);
                }
                if (customModelData != -1) {
                    meta.setCustomModelData(customModelData);
                }
                item.setItemMeta(meta);
            }
        } else if (customModelData != -1) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(customModelData);
                item.setItemMeta(meta);
            }
        }

        return item;
    }

    private String getRankDisplay(PlayerRank rank) {
        return switch (rank) {
            case OWNER -> rankOwnerDisplay;
            case SECONDARY_OWNER -> rankSecondaryOwnerDisplay;
            case ADMIN -> rankAdminDisplay;
            case MEMBER -> rankMemberDisplay;
        };
    }

    private String getRankColor(PlayerRank rank) {
        return switch (rank) {
            case OWNER -> rankOwnerColor;
            case SECONDARY_OWNER -> rankSecondaryOwnerColor;
            case ADMIN -> rankAdminColor;
            case MEMBER -> rankMemberColor;
        };
    }

    @Override
    public void handleMenuClick(int slot, Player player) {
        handleMenuClick(slot, player, ClickType.LEFT);
    }

    @Override
    public void handleMenuClick(int slot, Player player, ClickType clickType) {
        if (slot == backSlot) {
            Location loc = player.getLocation();
            if (loc == null) {
                return;
            }
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            plugin.getMenuManager().openMenu(player, new MainProtectionMenu(plugin, region));
            return;
        }

        if (slot == refreshSlot) {
            Location loc = player.getLocation();
            if (loc == null) {
                return;
            }
            player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
            setMenuItems();
            return;
        }

        if (slot == kickAllSlot) {
            if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
                if (!region.getOwnerUniqueId().equals(player.getUniqueId())) {
                    player.sendMessage(mm.deserialize(msg("members.no-permission-owner")));
                    Location loc = player.getLocation();
                    if (loc == null) {
                        return;
                    }
                    player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
                    return;
                }
                Location loc = player.getLocation();
                if (loc == null) {
                    return;
                }
                player.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
                List<UUID> members = new ArrayList<>(region.getMembers().keySet());
                for (UUID memberUuid : members) {
                    region.removeMember(memberUuid);
                    plugin.getProtectionManager().getProtectionDao().removeMemberAsync(region.getDatabaseId(),
                            memberUuid);
                }
                player.sendMessage(mm.deserialize(msg("menu.members-all-kicked")));
                setMenuItems();
            } else {
                Location loc = player.getLocation();
                if (loc == null) {
                    return;
                }
                player.sendMessage(mm.deserialize(msg("menu.kick-all-shift-warning")));
                player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            }
            return;
        }

        if (slotToMember.containsKey(slot)) {
            UUID targetUuid = slotToMember.get(slot);
            PlayerRank viewerRank = region.getRank(player.getUniqueId());
            boolean isOwner = region.getOwnerUniqueId().equals(player.getUniqueId());

            if (region.getOwnerUniqueId().equals(targetUuid)) {
                player.sendMessage(mm.deserialize(msg("members.is-owner")
                        .replace("%member%", region.getOwnerName())));
                Location loc = player.getLocation();
                if (loc == null) {
                    return;
                }
                player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
                return;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
            String targetName = target.getName() != null ? target.getName() : targetUuid.toString();
            PlayerRank currentRank = region.getMembers().get(targetUuid);
            if (currentRank == null)
                currentRank = PlayerRank.MEMBER;

            boolean canManage = isOwner;
            if (!isOwner && viewerRank == PlayerRank.ADMIN) {
                canManage = currentRank == PlayerRank.MEMBER;
            }
            if (!isOwner && viewerRank == PlayerRank.SECONDARY_OWNER) {
                canManage = currentRank == PlayerRank.MEMBER || currentRank == PlayerRank.ADMIN;
            }

            if (!canManage) {
                player.sendMessage(mm.deserialize(msg("members.no-permission-manage")
                        .replace("%member%", targetName)));
                Location loc = player.getLocation();
                if (loc == null) {
                    return;
                }
                player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
                return;
            }

            if (clickType == ClickType.RIGHT) {
                if (!isOwner && viewerRank != PlayerRank.SECONDARY_OWNER) {
                    player.sendMessage(mm.deserialize(msg("members.no-permission-owner")));
                    Location loc = player.getLocation();
                    if (loc == null) {
                        return;
                    }
                    player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
                    return;
                }
                String safeName = Objects.requireNonNullElse(targetName, MessageUtils.lang("members.rank-unknown"));
                String ownerName = Objects.requireNonNullElse(player.getName(), MessageUtils.lang("members.rank-unknown"));

                switch (currentRank) {
                    case MEMBER -> {
                        region.addMember(targetUuid, PlayerRank.ADMIN);
                        plugin.getProtectionManager().getProtectionDao().saveMemberAsync(region.getDatabaseId(),
                                targetUuid, "ADMIN");
                        player.sendMessage(parse("members.promoted", MessageUtils.lang("members.rank-promoted"))
                                .replaceText(b -> b.matchLiteral("%member%").replacement(safeName)));
                        if (target.isOnline()) {
                            Player targetPlayer = target.getPlayer();
                            if (targetPlayer != null)
                                targetPlayer.sendMessage(parse("members.notify-promoted", MessageUtils.lang("members.rank-promoted"))
                                        .replaceText(b -> b.matchLiteral("%owner%").replacement(ownerName)));
                        }
                    }
                    case ADMIN -> {
                        region.addMember(targetUuid, PlayerRank.SECONDARY_OWNER);
                        plugin.getProtectionManager().getProtectionDao().saveMemberAsync(region.getDatabaseId(),
                                targetUuid, "SECONDARY_OWNER");
                        player.sendMessage(parse("members.promoted-secondary", MessageUtils.lang("members.rank-promoted"))
                                .replaceText(b -> b.matchLiteral("%member%").replacement(safeName)));
                        if (target.isOnline()) {
                            Player targetPlayer = target.getPlayer();
                            if (targetPlayer != null)
                                targetPlayer.sendMessage(parse("members.notify-promoted-secondary", MessageUtils.lang("members.rank-promoted"))
                                        .replaceText(b -> b.matchLiteral("%owner%").replacement(ownerName)));
                        }
                    }
                    case SECONDARY_OWNER -> {
                        region.addMember(targetUuid, PlayerRank.ADMIN);
                        plugin.getProtectionManager().getProtectionDao().saveMemberAsync(region.getDatabaseId(),
                                targetUuid, "ADMIN");
                        player.sendMessage(parse("members.demoted-to-admin", MessageUtils.lang("members.rank-demoted"))
                                .replaceText(b -> b.matchLiteral("%member%").replacement(safeName)));
                        if (target.isOnline()) {
                            Player targetPlayer = target.getPlayer();
                            if (targetPlayer != null)
                                targetPlayer.sendMessage(parse("members.notify-demoted", MessageUtils.lang("members.rank-demoted"))
                                        .replaceText(b -> b.matchLiteral("%owner%").replacement(ownerName)));
                        }
                    }
                    default -> {
                    }
                }
                Location loc = player.getLocation();
                if (loc == null) {
                    return;
                }
                player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
                setMenuItems();
            } else {
                region.removeMember(targetUuid);
                plugin.getProtectionManager().saveRegionAsync(region);
                plugin.getProtectionManager().getProtectionDao().removeMemberAsync(region.getDatabaseId(), targetUuid);
                player.sendMessage(mm.deserialize(msg("members.removed").replace("%member%", targetName)));
                if (target.isOnline()) {
                    Player targetPlayer = target.getPlayer();
                    if (targetPlayer != null) {
                        String ownerName = Objects.requireNonNullElse(player.getName(), MessageUtils.lang("members.rank-unknown"));
                        targetPlayer.sendMessage(parse("members.notify-removed", MessageUtils.lang("members.rank-removed"))
                                .replaceText(b -> b.matchLiteral("%owner%").replacement(ownerName)));
                    }
                }
                Location loc = player.getLocation();
                if (loc == null) {
                    return;
                }
                player.playSound(loc, Sound.UI_BUTTON_CLICK, 1f, 1f);
                setMenuItems();
            }
        }
    }

    private String msg(String key) {
        return MessageUtils.lang(key);
    }

    private String getConfigString(ConfigurationSection section, String path, String def) {
        return section != null ? section.getString(path, def) : def;
    }

    private Material getMaterial(ConfigurationSection section, String path, Material def) {
        if (section == null)
            return def;
        String matName = section.getString(path);
        if (matName == null)
            return def;
        try {
            return Material.valueOf(matName);
        } catch (IllegalArgumentException e) {
            return def;
        }
    }

    private List<String> getConfigList(ConfigurationSection section, String path, List<String> def) {
        return section != null ? section.getStringList(path) : def;
    }

    private int getCustomModelData(ConfigurationSection section, String path, int def) {
        return section != null ? section.getInt(path, def) : def;
    }

    private String formatJoinDate(Long timestamp) {
        if (timestamp == null || timestamp <= 0)
            return MessageUtils.lang("members.join-date-unknown");
        java.time.Instant instant = java.time.Instant.ofEpochMilli(timestamp);
        java.time.LocalDateTime dt = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return "<gray>" + dt.format(fmt);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }
}
