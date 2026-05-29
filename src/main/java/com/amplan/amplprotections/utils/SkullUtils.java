package com.amplan.amplprotections.utils;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkullUtils {

    private static final Pattern URL_PATTERN = Pattern.compile("https?://textures\\.minecraft\\.net/texture/([a-f0-9]+)");

    /**
     * Decodifica un valor base64 de textura de cabeza y extrae el hash de la textura.
     * El valor base64 decodificado tiene formato JSON: {"textures":{"SKIN":{"url":"..."}}}
     */
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

    /**
     * Aplica una textura personalizada a un PLAYER_HEAD usando un valor base64.
     */
    public static ItemStack createSkullWithTexture(String base64Value) {
        ItemStack item = new ItemStack(org.bukkit.Material.PLAYER_HEAD);
        applyTextureToSkull(item, base64Value);
        return item;
    }

    /**
     * Aplica una textura personalizada a un ItemStack que ya es PLAYER_HEAD.
     * Retorna true si se aplicó correctamente, false si falló.
     */
    @SuppressWarnings("deprecation")
    public static boolean applyTextureToSkull(ItemStack item, String base64Value) {
        if (item == null || item.getType() != org.bukkit.Material.PLAYER_HEAD) {
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
