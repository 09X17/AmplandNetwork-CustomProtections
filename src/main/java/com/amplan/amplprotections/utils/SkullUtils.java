package com.amplan.amplprotections.utils;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

public class SkullUtils {

    private static final Pattern URL_PATTERN = Pattern.compile("https?://textures\\.minecraft\\.net/texture/([a-f0-9]+)");

    public static String extractTextureHash(String base64Value) {
        if (base64Value == null || base64Value.isEmpty()) {
            return null;
        }
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64Value);
            String decoded = new String(decodedBytes);
            Matcher matcher = URL_PATTERN.matcher(decoded);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
        return null;
    }

    public static ItemStack createSkullWithTexture(String base64Value) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        applyTextureToSkull(item, base64Value);
        return item;
    }

    @SuppressWarnings("deprecation")
    public static boolean applyTextureToSkull(ItemStack item, String base64Value) {
        if (item == null || item.getType() != Material.PLAYER_HEAD || base64Value == null || base64Value.isEmpty()) {
            return false;
        }

        String hash = extractTextureHash(base64Value);
        if (hash == null) {
            return false;
        }

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) {
            return false;
        }

        try {
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(URI.create("https://textures.minecraft.net/texture/" + hash).toURL());
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
            item.setItemMeta(meta);
            return true;
        } catch (MalformedURLException | IllegalArgumentException e) {
            return false;
        }
    }
}
