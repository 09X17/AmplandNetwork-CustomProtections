package com.amplan.amplprotections.utils;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;


public class ColorUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
   
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern AMP_PATTERN = Pattern.compile("&([0-9a-fk-orA-FK-OR])");

    public static Component parse(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        

        String processed = convertLegacyToMini(text);
        return MINI_MESSAGE.deserialize(processed);
    }

    public static String convertLegacyToMini(String text) {
        if (text == null) return "";

        String result = HEX_PATTERN.matcher(text).replaceAll("<#$1>");

        result = AMP_PATTERN.matcher(result).replaceAll(match -> {
            String code = match.group(1).toLowerCase();
            return switch (code) {
                case "0" -> "<black>";
                case "1" -> "<dark_blue>";
                case "2" -> "<dark_green>";
                case "3" -> "<dark_aqua>";
                case "4" -> "<dark_red>";
                case "5" -> "<dark_purple>";
                case "6" -> "<gold>";
                case "7" -> "<gray>";
                case "8" -> "<dark_gray>";
                case "9" -> "<blue>";
                case "a" -> "<green>";
                case "b" -> "<aqua>";
                case "c" -> "<red>";
                case "d" -> "<light_purple>";
                case "e" -> "<yellow>";
                case "f" -> "<white>";
                case "k" -> "<obfuscated>";
                case "l" -> "<b>";
                case "m" -> "<strikethrough>";
                case "n" -> "<u>";
                case "o" -> "<i>";
                case "r" -> "<reset>";
                default -> "&" + code;
            };
        });

        return result;
    }

    public static List<Component> parseLore(List<String> lines) {
        return lines.stream().map(ColorUtils::parse).collect(Collectors.toList());
    }

    public static String stripColors(String text) {
        return MINI_MESSAGE.stripTags(convertLegacyToMini(text));
    }
}