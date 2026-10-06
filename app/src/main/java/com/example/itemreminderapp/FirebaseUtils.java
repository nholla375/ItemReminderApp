package com.example.itemreminderapp;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class FirebaseUtils {
    public static final String[] CATEGORIES = {
            "School", "Electronics", "Clothing", "Personal", "Sports", "Other"
    };

    public static final String[] FILTER_CATEGORIES = {
            "All", "School", "Electronics", "Clothing", "Personal", "Sports", "Other"
    };

    private FirebaseUtils() {
    }

    public static FirebaseUser getCurrentUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    public static String getCurrentUid() {
        FirebaseUser user = getCurrentUser();
        return user == null ? "" : user.getUid();
    }

    public static String getCurrentEmail() {
        FirebaseUser user = getCurrentUser();
        return user == null || user.getEmail() == null ? "" : user.getEmail();
    }

    public static void ensureUserProfileExists() {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            return;
        }

        String email = user.getEmail() == null ? "" : user.getEmail().trim();
        DocumentReference profileReference = FirebaseFirestore.getInstance()
                .collection("userProfiles")
                .document(user.getUid());

        profileReference.get().addOnSuccessListener(snapshot -> {
            Map<String, Object> profile = new HashMap<>();
            profile.put("uid", user.getUid());
            profile.put("email", email);
            profile.put("emailLower", email.toLowerCase(Locale.US));
            profile.put("updatedAt", System.currentTimeMillis());
            if (!snapshot.exists() || !snapshot.contains("createdAt")) {
                profile.put("createdAt", System.currentTimeMillis());
            }
            profileReference.set(profile, SetOptions.merge());
        });
    }

    public static String formatTimestamp(long timestamp) {
        if (timestamp <= 0) {
            return "";
        }
        return new SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault())
                .format(new Date(timestamp));
    }
}
