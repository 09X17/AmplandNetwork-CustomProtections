package com.amplan.amplprotections.utils;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;


public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }
 
    public ItemBuilder(ItemStack item) {
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
    }

    public ItemBuilder setAmount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public ItemBuilder setDisplayName(String name) {
        if (meta != null && name != null) {
            meta.displayName(mm.deserialize(name));
        }
        return this;
    }

    public ItemBuilder setLore(String... loreLines) {
        if (meta != null && loreLines != null) {
            List<Component> serializedLore = new ArrayList<>();
            for (String line : loreLines) {
                serializedLore.add(mm.deserialize(line));
            }
            meta.lore(serializedLore);
        }
        return this;
    }

    public ItemBuilder setLore(List<String> loreLines) {
        if (meta != null && loreLines != null) {
            List<Component> serializedLore = new ArrayList<>();
            for (String line : loreLines) {
                serializedLore.add(mm.deserialize(line));
            }
            meta.lore(serializedLore);
        }
        return this;
    }

    public ItemBuilder addEnchantment(Enchantment enchant, int level) {
        if (meta != null) {
            meta.addEnchant(enchant, level, true);
        }
        return this;
    }

    public ItemBuilder setGlow(boolean glow) {
        if (meta != null) {
            if (glow) {
                meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else {
                for (Enchantment enchant : meta.getEnchants().keySet()) {
                    meta.removeEnchant(enchant);
                }
            }
        }
        return this;
    }

    public ItemBuilder addItemFlags(ItemFlag... flags) {
        if (meta != null) {
            meta.addItemFlags(flags);
        }
        return this;
    }

    public ItemBuilder setCustomModelData(Integer data) {
        if (meta != null) {
            meta.setCustomModelData(data);
        }
        return this;
    }

    public ItemStack build() {
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }
}
