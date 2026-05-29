package com.amplan.amplprotections.economy;

import com.amplan.amplprotections.AmplProtections;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.HashMap;
import java.util.Map;

public class EconomyManager {

    private final AmplProtections plugin;
    private Economy vaultEconomy;
    private boolean enabled;
    private final Map<String, Double> protectionCosts = new HashMap<>();
    private String bypassPermission;

    public EconomyManager(AmplProtections plugin) {
        this.plugin = plugin;
        this.enabled = false;
        setupVault();
        loadConfig();
    }

    private void setupVault() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault no encontrado. La economia estara deshabilitada.");
            return;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("No se encontro un plugin de economia compatible con Vault.");
            return;
        }

        vaultEconomy = rsp.getProvider();
        plugin.getLogger().info("Vault hook exitoso. Economia: " + vaultEconomy.getName());
    }

    private void loadConfig() {
        ConfigurationSection econSection = plugin.getConfig().getConfigurationSection("economy");
        if (econSection == null) {
            enabled = false;
            return;
        }

        enabled = econSection.getBoolean("enabled", true) && vaultEconomy != null;
        bypassPermission = econSection.getString("bypass-permission", "amplprotections.economy.bypass");

        ConfigurationSection costsSection = econSection.getConfigurationSection("protection-costs");
        if (costsSection != null) {
            for (String key : costsSection.getKeys(false)) {
                protectionCosts.put(key, costsSection.getDouble(key, 0.0));
            }
        }
    }

    public void reload() {
        protectionCosts.clear();
        loadConfig();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean hasBypass(Player player) {
        return player.hasPermission(bypassPermission);
    }

    public double getCost(String typeId) {
        return protectionCosts.getOrDefault(typeId, 0.0);
    }

    public boolean hasBalance(Player player, double amount) {
        if (!enabled || vaultEconomy == null) return true;
        return vaultEconomy.has(player, amount);
    }

    public boolean charge(Player player, double amount) {
        if (!enabled || vaultEconomy == null || hasBypass(player)) return true;
        if (!vaultEconomy.has(player, amount)) return false;
        return vaultEconomy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(Player player, double amount) {
        if (!enabled || vaultEconomy == null) return false;
        return vaultEconomy.depositPlayer(player, amount).transactionSuccess();
    }

    public double getBalance(Player player) {
        if (!enabled || vaultEconomy == null) return -1;
        return vaultEconomy.getBalance(player);
    }

    public String format(double amount) {
        if (!enabled || vaultEconomy == null) return String.format("%.2f", amount);
        return vaultEconomy.format(amount);
    }

    public Economy getVaultEconomy() {
        return vaultEconomy;
    }
}
