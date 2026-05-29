package com.amplan.amplprotections.model;

import java.util.UUID;

public class Rental {

    private int regionId;
    private UUID renterUuid;
    private long startTime;
    private long endTime;
    private double price;
    private boolean autoRenew;
    private long lastPayment;

    public Rental(int regionId, UUID renterUuid, long startTime, long endTime, double price, boolean autoRenew) {
        this.regionId = regionId;
        this.renterUuid = renterUuid;
        this.startTime = startTime;
        this.endTime = endTime;
        this.price = price;
        this.autoRenew = autoRenew;
        this.lastPayment = startTime;
    }

    public Rental(int regionId, UUID renterUuid, long startTime, long endTime, double price, boolean autoRenew, long lastPayment) {
        this.regionId = regionId;
        this.renterUuid = renterUuid;
        this.startTime = startTime;
        this.endTime = endTime;
        this.price = price;
        this.autoRenew = autoRenew;
        this.lastPayment = lastPayment;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > endTime;
    }

    public long getRemainingMillis() {
        return Math.max(0, endTime - System.currentTimeMillis());
    }

    public long getRemainingSeconds() {
        return getRemainingMillis() / 1000L;
    }

    public int getRegionId() { return regionId; }
    public void setRegionId(int regionId) { this.regionId = regionId; }
    public UUID getRenterUuid() { return renterUuid; }
    public void setRenterUuid(UUID renterUuid) { this.renterUuid = renterUuid; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public boolean isAutoRenew() { return autoRenew; }
    public void setAutoRenew(boolean autoRenew) { this.autoRenew = autoRenew; }
    public long getLastPayment() { return lastPayment; }
    public void setLastPayment(long lastPayment) { this.lastPayment = lastPayment; }
}
