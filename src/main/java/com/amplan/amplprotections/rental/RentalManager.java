package com.amplan.amplprotections.rental;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import com.amplan.amplprotections.AmplProtections;
import com.amplan.amplprotections.economy.EconomyManager;
import com.amplan.amplprotections.model.ProtectionRegion;
import com.amplan.amplprotections.model.Rental;
import com.amplan.amplprotections.utils.MessageUtils;

import net.kyori.adventure.text.minimessage.MiniMessage;

public class RentalManager {

    private final AmplProtections plugin;
    private final Map<Integer, Rental> activeRentals = new ConcurrentHashMap<>();
    private BukkitTask expiryCheckTask;
    private boolean enabled;
    private int checkIntervalTicks;
    private boolean defaultAutoRenew;

    public RentalManager(AmplProtections plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        ConfigurationSection rentalSection = plugin.getConfig().getConfigurationSection("rental");
        if (rentalSection == null) {
            enabled = true;
            checkIntervalTicks = 6000;
            defaultAutoRenew = false;
            return;
        }

        enabled = rentalSection.getBoolean("enabled", true);
        checkIntervalTicks = rentalSection.getInt("check-interval-ticks", 6000);
        defaultAutoRenew = rentalSection.getBoolean("default-auto-renew", false);
    }

    public void reload() {
        loadConfig();
        if (enabled) {
            startExpiryCheckTask();
        } else {
            stopExpiryCheckTask();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void startExpiryCheckTask() {
        if (expiryCheckTask != null) expiryCheckTask.cancel();
        if (!enabled) return;

        expiryCheckTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkExpirations, 200L, checkIntervalTicks);
    }

    public void stopExpiryCheckTask() {
        if (expiryCheckTask != null) {
            expiryCheckTask.cancel();
            expiryCheckTask = null;
        }
    }

    public void loadRentals() {
        plugin.getProtectionManager().getProtectionDao().loadAllRentalsAsync().thenAccept(rentals -> {
            if (rentals != null) {
                for (Rental rental : rentals) {
                    if (!rental.isExpired()) {
                        activeRentals.put(rental.getRegionId(), rental);
                    }
                }
            }
        });
    }

    public boolean setRental(ProtectionRegion region, double price, int days, Player owner) {
        if (!enabled) return false;

        EconomyManager economy = plugin.getEconomyManager();
        if (economy.isEnabled() && price > 0) {
            double taxPercent = plugin.getConfig().getDouble("rental.tax-percent", 5.0);
            double taxAmount = price * (taxPercent / 100.0);
            double totalCost = price + taxAmount;

            if (!economy.hasBalance(owner, totalCost)) {
                String msg = MessageUtils.lang("rental.insufficient-funds")
                        .replace("%amount%", economy.format(totalCost));
                owner.sendMessage(MiniMessage.miniMessage().deserialize(msg));
                return false;
            }

            if (!economy.charge(owner, totalCost)) {
                owner.sendMessage(MiniMessage.miniMessage().deserialize(MessageUtils.lang("rental.payment-failed")));
                return false;
            }
        }

        long now = System.currentTimeMillis();
        long endTime = now + (days * 24L * 60L * 60L * 1000L);
        Rental rental = new Rental(region.getDatabaseId(), owner.getUniqueId(), now, endTime, price, defaultAutoRenew, days);

        activeRentals.put(region.getDatabaseId(), rental);
        plugin.getProtectionManager().getProtectionDao().saveRentalAsync(rental);

        String msg = MessageUtils.lang("rental.set-success")
                .replace("%price%", economy.format(price)).replace("%days%", String.valueOf(days));
        owner.sendMessage(MiniMessage.miniMessage().deserialize(msg));
        return true;
    }

    public boolean acceptRental(ProtectionRegion region, Player renter) {
        if (!enabled) return false;

        Rental rental = activeRentals.get(region.getDatabaseId());
        if (rental == null || rental.isExpired()) {
            renter.sendMessage(MiniMessage.miniMessage().deserialize(MessageUtils.lang("rental.not-available")));
            return false;
        }

        if (rental.getRenterUuid().equals(renter.getUniqueId())) {
            renter.sendMessage(MiniMessage.miniMessage().deserialize(MessageUtils.lang("rental.already-renting")));
            return false;
        }

        EconomyManager economy = plugin.getEconomyManager();
        if (economy.isEnabled() && rental.getPrice() > 0) {
            if (!economy.hasBalance(renter, rental.getPrice())) {
                String msg = MessageUtils.lang("rental.insufficient-funds")
                        .replace("%amount%", economy.format(rental.getPrice()));
                renter.sendMessage(MiniMessage.miniMessage().deserialize(msg));
                return false;
            }

            if (!economy.charge(renter, rental.getPrice())) {
                renter.sendMessage(MiniMessage.miniMessage().deserialize(MessageUtils.lang("rental.payment-failed")));
                return false;
            }

            double taxPercent = plugin.getConfig().getDouble("rental.tax-percent", 5.0);
            double ownerAmount = rental.getPrice() * (1 - (taxPercent / 100.0));
            Player ownerPlayer = Bukkit.getPlayer(region.getOwnerUniqueId());
            if (ownerPlayer != null) {
                economy.deposit(ownerPlayer, ownerAmount);
            }
        }

        long now = System.currentTimeMillis();
        long endTime = now + rental.getDurationMillis();
        Rental newRental = new Rental(region.getDatabaseId(), renter.getUniqueId(), now, endTime, rental.getPrice(), rental.isAutoRenew(), rental.getDurationDays());

        activeRentals.put(region.getDatabaseId(), newRental);
        plugin.getProtectionManager().getProtectionDao().saveRentalAsync(newRental);

        String msg = MessageUtils.lang("rental.accept-success")
                .replace("%name%", region.getCustomName())
                .replace("%days%", String.valueOf(Math.max(1, newRental.getDurationDays())));
        renter.sendMessage(MiniMessage.miniMessage().deserialize(msg));

        Player ownerPlayer = Bukkit.getPlayer(region.getOwnerUniqueId());
        if (ownerPlayer != null) {
            String ownerMsg = MessageUtils.lang("rental.notify-owner")
                    .replace("%renter%", renter.getName())
                    .replace("%name%", region.getCustomName());
            ownerPlayer.sendMessage(MiniMessage.miniMessage().deserialize(ownerMsg));
        }

        return true;
    }

    public boolean cancelRental(ProtectionRegion region, Player player) {
        if (!enabled) return false;

        Rental rental = activeRentals.get(region.getDatabaseId());
        if (rental == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(MessageUtils.lang("rental.no-active")));
            return false;
        }

        if (!rental.getRenterUuid().equals(player.getUniqueId()) && !region.getOwnerUniqueId().equals(player.getUniqueId()) && !player.hasPermission("amplprotections.admin.bypass")) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(MessageUtils.lang("general.no-permission")));
            return false;
        }

        activeRentals.remove(region.getDatabaseId());
        plugin.getProtectionManager().getProtectionDao().deleteRentalAsync(region.getDatabaseId());

        player.sendMessage(MiniMessage.miniMessage().deserialize(MessageUtils.lang("rental.cancel-success")));
        return true;
    }

    public boolean isRenter(ProtectionRegion region, UUID playerUuid) {
        Rental rental = activeRentals.get(region.getDatabaseId());
        return rental != null && !rental.isExpired() && rental.getRenterUuid().equals(playerUuid);
    }

    public Rental getRental(ProtectionRegion region) {
        Rental rental = activeRentals.get(region.getDatabaseId());
        if (rental != null && rental.isExpired()) {
            activeRentals.remove(region.getDatabaseId());
            plugin.getProtectionManager().getProtectionDao().deleteRentalAsync(region.getDatabaseId());
            return null;
        }
        return rental;
    }

    public boolean hasActiveRental(ProtectionRegion region) {
        return getRental(region) != null;
    }

    private void checkExpirations() {
        List<Integer> expiredIds = new ArrayList<>();
        for (Map.Entry<Integer, Rental> entry : activeRentals.entrySet()) {
            Rental rental = entry.getValue();
            if (rental.isExpired()) {
                if (rental.isAutoRenew()) {
                    EconomyManager economy = plugin.getEconomyManager();
                    Player renter = Bukkit.getPlayer(rental.getRenterUuid());
                    if (renter != null && economy.isEnabled() && economy.hasBalance(renter, rental.getPrice())) {
                        economy.charge(renter, rental.getPrice());
                        long renewalMillis = rental.getDurationDays() > 0
                                ? rental.getDurationMillis()
                                : 86400000L;
                        rental.setEndTime(System.currentTimeMillis() + renewalMillis);
                        rental.setLastPayment(System.currentTimeMillis());
                        plugin.getProtectionManager().getProtectionDao().saveRentalAsync(rental);

                        ProtectionRegion region = plugin.getProtectionManager().getAllRegions().stream()
                                .filter(r -> r.getDatabaseId() == rental.getRegionId()).findFirst().orElse(null);
                        if (region != null) {
                            String msg = MessageUtils.lang("rental.auto-renewed").replace("%name%", region.getCustomName());
                            renter.sendMessage(MiniMessage.miniMessage().deserialize(msg));
                        }
                        continue;
                    }
                }

                expiredIds.add(entry.getKey());
                ProtectionRegion region = plugin.getProtectionManager().getAllRegions().stream()
                        .filter(r -> r.getDatabaseId() == entry.getKey()).findFirst().orElse(null);
                if (region != null) {
                    Player renter = Bukkit.getPlayer(rental.getRenterUuid());
                    if (renter != null) {
                        String msg = MessageUtils.lang("rental.expired").replace("%name%", region.getCustomName());
                        renter.sendMessage(MiniMessage.miniMessage().deserialize(msg));
                    }
                }
            }
        }

        for (Integer id : expiredIds) {
            activeRentals.remove(id);
            plugin.getProtectionManager().getProtectionDao().deleteRentalAsync(id);
        }
    }

    public void shutdown() {
        stopExpiryCheckTask();
        activeRentals.clear();
    }
}
