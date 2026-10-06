package com.example.itemreminderapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;

public class ItemTest {

    @Test
    public void defaultsAreSafeForNewItems() {
        Item item = new Item();

        assertEquals("Other", item.getCategory());
        assertEquals("", item.getOwnerUid());
        assertEquals("", item.getOwnerEmail());
        assertTrue(item.getSharedWithUids().isEmpty());
        assertTrue(item.getSharedWithEmails().isEmpty());
        assertFalse(item.isFavorite());
        assertFalse(item.isArchived());
        assertFalse(item.isHasReminder());
    }

    @Test
    public void normalizeDefaultsRepairsNullableFirestoreFields() {
        Item item = new Item();
        item.setItemName(null);
        item.setNotes(null);
        item.setLocalImagePath(null);
        item.setFormattedDate(null);
        item.setOwnerUid(null);
        item.setOwnerEmail(null);
        item.setCategory(null);
        item.setSharedWithUids(null);
        item.setSharedWithEmails(null);

        item.normalizeDefaults();

        assertEquals("", item.getItemName());
        assertEquals("", item.getNotes());
        assertEquals("", item.getLocalImagePath());
        assertEquals("", item.getFormattedDate());
        assertEquals("", item.getOwnerUid());
        assertEquals("", item.getOwnerEmail());
        assertEquals("Other", item.getCategory());
        assertTrue(item.getSharedWithUids().isEmpty());
        assertTrue(item.getSharedWithEmails().isEmpty());
    }

    @Test
    public void ownershipAndSharingHelpersUseUidMembership() {
        Item item = new Item();
        item.setOwnerUid("owner-1");
        ArrayList<String> shared = new ArrayList<>();
        shared.add("reader-1");
        item.setSharedWithUids(shared);

        assertTrue(item.isOwnedBy("owner-1"));
        assertFalse(item.isOwnedBy("reader-1"));
        assertTrue(item.isSharedWith("reader-1"));
        assertFalse(item.isSharedWith("other"));
    }
}
