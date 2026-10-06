package com.example.itemreminderapp;

import java.util.ArrayList;

public class Item {
    private String itemId;
    private String itemName;
    private String notes;
    private String localImagePath;
    private double latitude;
    private double longitude;
    private boolean hasLocation;
    private long timestamp;
    private String formattedDate;

    private String ownerUid;
    private String ownerEmail;
    private ArrayList<String> sharedWithUids;
    private ArrayList<String> sharedWithEmails;
    private String category;
    private boolean favorite;
    private boolean archived;
    private boolean hasReminder;
    private long reminderTimeMillis;

    public Item() {
        setDefaultValues();
    }

    public Item(String itemId, String itemName, String notes, String localImagePath,
                double latitude, double longitude, boolean hasLocation, long timestamp,
                String formattedDate) {
        setDefaultValues();
        this.itemId = itemId;
        this.itemName = itemName;
        this.notes = notes;
        this.localImagePath = localImagePath;
        this.latitude = latitude;
        this.longitude = longitude;
        this.hasLocation = hasLocation;
        this.timestamp = timestamp;
        this.formattedDate = formattedDate;
    }

    public Item(String itemId, String itemName, String notes, String localImagePath,
                String ownerUid, String ownerEmail, double latitude, double longitude,
                boolean hasLocation, long timestamp, String formattedDate, String category,
                boolean hasReminder, long reminderTimeMillis) {
        this(itemId, itemName, notes, localImagePath, latitude, longitude, hasLocation,
                timestamp, formattedDate);
        this.ownerUid = ownerUid;
        this.ownerEmail = ownerEmail;
        this.category = cleanCategory(category);
        this.hasReminder = hasReminder;
        this.reminderTimeMillis = reminderTimeMillis;
    }

    private void setDefaultValues() {
        sharedWithUids = new ArrayList<>();
        sharedWithEmails = new ArrayList<>();
        category = "Other";
        ownerUid = "";
        ownerEmail = "";
    }

    public void normalizeDefaults() {
        if (sharedWithUids == null) {
            sharedWithUids = new ArrayList<>();
        }
        if (sharedWithEmails == null) {
            sharedWithEmails = new ArrayList<>();
        }
        category = cleanCategory(category);
        if (ownerUid == null) {
            ownerUid = "";
        }
        if (ownerEmail == null) {
            ownerEmail = "";
        }
        if (itemName == null) {
            itemName = "";
        }
        if (notes == null) {
            notes = "";
        }
        if (formattedDate == null) {
            formattedDate = "";
        }
        if (localImagePath == null) {
            localImagePath = "";
        }
    }

    private String cleanCategory(String value) {
        return value == null || value.trim().isEmpty() ? "Other" : value.trim();
    }

    public boolean isOwnedBy(String uid) {
        return uid != null && uid.equals(ownerUid);
    }

    public boolean isSharedWith(String uid) {
        return uid != null && getSharedWithUids().contains(uid);
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getLocalImagePath() {
        return localImagePath;
    }

    public void setLocalImagePath(String localImagePath) {
        this.localImagePath = localImagePath;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean isHasLocation() {
        return hasLocation;
    }

    public void setHasLocation(boolean hasLocation) {
        this.hasLocation = hasLocation;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getFormattedDate() {
        return formattedDate;
    }

    public void setFormattedDate(String formattedDate) {
        this.formattedDate = formattedDate;
    }

    public String getOwnerUid() {
        return ownerUid;
    }

    public void setOwnerUid(String ownerUid) {
        this.ownerUid = ownerUid;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public ArrayList<String> getSharedWithUids() {
        if (sharedWithUids == null) {
            sharedWithUids = new ArrayList<>();
        }
        return sharedWithUids;
    }

    public void setSharedWithUids(ArrayList<String> sharedWithUids) {
        this.sharedWithUids = sharedWithUids == null ? new ArrayList<>() : sharedWithUids;
    }

    public ArrayList<String> getSharedWithEmails() {
        if (sharedWithEmails == null) {
            sharedWithEmails = new ArrayList<>();
        }
        return sharedWithEmails;
    }

    public void setSharedWithEmails(ArrayList<String> sharedWithEmails) {
        this.sharedWithEmails = sharedWithEmails == null ? new ArrayList<>() : sharedWithEmails;
    }

    public String getCategory() {
        return cleanCategory(category);
    }

    public void setCategory(String category) {
        this.category = cleanCategory(category);
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public boolean isHasReminder() {
        return hasReminder;
    }

    public void setHasReminder(boolean hasReminder) {
        this.hasReminder = hasReminder;
    }

    public long getReminderTimeMillis() {
        return reminderTimeMillis;
    }

    public void setReminderTimeMillis(long reminderTimeMillis) {
        this.reminderTimeMillis = reminderTimeMillis;
    }
}
