package com.amplan.amplprotections.utils;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import com.amplan.amplprotections.AmplProtections;
import com.amplan.amplprotections.language.LanguageManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class MessageUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final String PREFIX = "<dark_gray>[<gradient:gold:yellow>Protecciones</gradient>]<gray> ";

    public static Component parse(String text) {
        if (text == null)
            return Component.empty();
        return MINI_MESSAGE.deserialize(text);
    }

    public static void sendMessage(CommandSender sender, String message) {
        if (sender == null || message == null)
            return;
        sender.sendMessage(MINI_MESSAGE.deserialize(PREFIX + message));
    }

    public static void sendRawMessage(CommandSender sender, String message) {
        if (sender == null || message == null)
            return;
        sender.sendMessage(MINI_MESSAGE.deserialize(message));
    }

    public static List<Component> parseLore(List<String> lines) {
        List<Component> lore = new ArrayList<>();
        if (lines == null)
            return lore;
        for (String line : lines) {
            lore.add(MINI_MESSAGE.deserialize(line));
        }
        return lore;
    }

    public static String stripColor(String text) {
        if (text == null)
            return "";
        Component component = MINI_MESSAGE.deserialize(text);
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static String msg(FileConfiguration config, String key) {
        String prefix = config.getString("prefix");
        String msg = config.getString(key);

        if (prefix == null)
            prefix = "";
        if (msg == null)
            msg = "";

        return msg.replace("%prefix%", prefix);
    }

    public static String msg(FileConfiguration config, String key, String def) {
        String prefix = config.getString("prefix");
        String msg = config.getString(key, def);

        if (prefix == null)
            prefix = "";
        if (msg == null)
            msg = def != null ? def : "";

        return msg.replace("%prefix%", prefix);
    }

    public static String lang(String key) {
        LanguageManager langManager = AmplProtections.getInstance().getLanguageManager();
        if (langManager == null)
            return key;
        return langManager.getPrefixed(key);
    }

    public static String langRaw(String key) {
        LanguageManager langManager = AmplProtections.getInstance().getLanguageManager();
        if (langManager == null)
            return key;
        return langManager.getString(key);
    }

    public static String getFlagName(String flagKey) {
        String langKey = "flag-names." + flagKey.toLowerCase();
        String name = lang(langKey);
        if (name == null || name.isEmpty() || name.equals(langKey)) {
            return flagKey.toUpperCase();
        }
        return name;
    }
}
