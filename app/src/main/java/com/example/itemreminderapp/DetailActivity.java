package com.example.itemreminderapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DetailActivity extends AppCompatActivity {
    private ImageView itemImageView;
    private TextView itemNameTextView;
    private TextView notesTextView;
    private TextView dateTextView;
    private TextView categoryTextView;
    private TextView reminderTextView;
    private TextView locationTextView;
    private TextView photoUnavailableTextView;
    private TextView sharedBadgeTextView;
    private TextView readOnlyTextView;
    private TextView sharedWithLabelTextView;
    private LinearLayout sharedWithContainer;
    private Button favoriteButton;
    private Button shareButton;
    private Button removeAllSharesButton;
    private Button editButton;
    private Button deleteButton;
    private String itemId;
    private Item currentItem;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (auth.getCurrentUser() == null) {
            openLogin();
            return;
        }

        setContentView(R.layout.activity_detail);

        itemImageView = findViewById(R.id.itemImageView);
        itemNameTextView = findViewById(R.id.itemNameTextView);
        notesTextView = findViewById(R.id.notesTextView);
        dateTextView = findViewById(R.id.dateTextView);
        categoryTextView = findViewById(R.id.categoryTextView);
        reminderTextView = findViewById(R.id.reminderTextView);
        locationTextView = findViewById(R.id.locationTextView);
        photoUnavailableTextView = findViewById(R.id.photoUnavailableTextView);
        sharedBadgeTextView = findViewById(R.id.sharedBadgeTextView);
        readOnlyTextView = findViewById(R.id.readOnlyTextView);
        sharedWithLabelTextView = findViewById(R.id.sharedWithLabelTextView);
        sharedWithContainer = findViewById(R.id.sharedWithContainer);
        favoriteButton = findViewById(R.id.favoriteButton);
        shareButton = findViewById(R.id.shareButton);
        removeAllSharesButton = findViewById(R.id.removeAllSharesButton);
        editButton = findViewById(R.id.editButton);
        deleteButton = findViewById(R.id.deleteButton);
        Button backButton = findViewById(R.id.backButton);

        itemId = getIntent().getStringExtra("itemId");
        if (itemId == null || itemId.isEmpty()) {
            Toast.makeText(this, "Item not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        editButton.setOnClickListener(v -> openEdit());
        deleteButton.setOnClickListener(v -> confirmDelete());
        favoriteButton.setOnClickListener(v -> toggleFavorite());
        shareButton.setOnClickListener(v -> showShareDialog());
        removeAllSharesButton.setOnClickListener(v -> removeAllShares());
        backButton.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (auth.getCurrentUser() == null) {
            openLogin();
            return;
        }
        if (itemId != null) {
            loadItem();
        }
    }

    private void loadItem() {
        db.collection("items").document(itemId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "Item no longer exists.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    currentItem = documentSnapshot.toObject(Item.class);
                    if (currentItem == null) {
                        Toast.makeText(this, "Could not read item.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    currentItem.normalizeDefaults();
                    currentItem.setItemId(documentSnapshot.getId());
                    showItem();
                })
                .addOnFailureListener(error -> Toast.makeText(this, "Could not load item.", Toast.LENGTH_SHORT).show());
    }

    private void showItem() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            openLogin();
            return;
        }

        boolean isOwner = currentItem.isOwnedBy(user.getUid());
        boolean isShared = currentItem.isSharedWith(user.getUid());
        if (!isOwner && !isShared) {
            Toast.makeText(this, "You do not have access to this item.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        itemNameTextView.setText(currentItem.getItemName());
        notesTextView.setText(currentItem.getNotes() == null || currentItem.getNotes().isEmpty()
                ? "No notes added"
                : currentItem.getNotes());
        dateTextView.setText(currentItem.getFormattedDate());
        categoryTextView.setText("Category: " + currentItem.getCategory());
        reminderTextView.setText(currentItem.isHasReminder()
                ? "Reminder: " + FirebaseUtils.formatTimestamp(currentItem.getReminderTimeMillis())
                : "Reminder: none");

        if (currentItem.isHasLocation()) {
            locationTextView.setText(String.format(Locale.getDefault(),
                    "Latitude: %.6f\nLongitude: %.6f",
                    currentItem.getLatitude(),
                    currentItem.getLongitude()));
        } else {
            locationTextView.setText("No GPS location saved");
        }

        sharedBadgeTextView.setVisibility(isOwner ? View.GONE : View.VISIBLE);
        readOnlyTextView.setVisibility(isOwner ? View.GONE : View.VISIBLE);
        favoriteButton.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        shareButton.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        editButton.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        deleteButton.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        sharedWithLabelTextView.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        sharedWithContainer.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        removeAllSharesButton.setVisibility(isOwner && !currentItem.getSharedWithEmails().isEmpty()
                ? View.VISIBLE
                : View.GONE);
        favoriteButton.setText(currentItem.isFavorite() ? "Remove Favorite" : "Add to Favorites");
        renderSharedEmails();

        showLocalImage();
    }

    private void showLocalImage() {
        String localImagePath = currentItem.getLocalImagePath();
        File imageFile = localImagePath == null || localImagePath.isEmpty() ? null : new File(localImagePath);
        if (imageFile != null && imageFile.exists()) {
            photoUnavailableTextView.setVisibility(View.GONE);
            Glide.with(this)
                    .load(imageFile)
                    .placeholder(R.drawable.photo_placeholder)
                    .into(itemImageView);
        } else {
            photoUnavailableTextView.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(R.drawable.photo_placeholder)
                    .into(itemImageView);
        }
    }

    private void renderSharedEmails() {
        sharedWithContainer.removeAllViews();
        if (currentItem == null || currentItem.getSharedWithEmails().isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No one yet");
            sharedWithContainer.addView(emptyText);
            return;
        }

        for (String email : currentItem.getSharedWithEmails()) {
            TextView emailText = new TextView(this);
            emailText.setText("- " + email);
            emailText.setTextSize(15);
            sharedWithContainer.addView(emailText);
        }
    }

    private void openEdit() {
        if (!currentUserOwnsItem()) {
            showReadOnlyToast();
            return;
        }
        Intent intent = new Intent(DetailActivity.this, EditItemActivity.class);
        intent.putExtra("itemId", itemId);
        startActivity(intent);
    }

    private void toggleFavorite() {
        if (!currentUserOwnsItem()) {
            showReadOnlyToast();
            return;
        }
        db.collection("items").document(itemId)
                .update("favorite", !currentItem.isFavorite())
                .addOnSuccessListener(unused -> loadItem())
                .addOnFailureListener(error -> Toast.makeText(this, "Could not update favorite.", Toast.LENGTH_SHORT).show());
    }

    private void showShareDialog() {
        if (!currentUserOwnsItem()) {
            showReadOnlyToast();
            return;
        }

        EditText emailEditText = new EditText(this);
        emailEditText.setHint("Enter user's email");
        emailEditText.setSingleLine(true);

        new AlertDialog.Builder(this)
                .setTitle("Share Item")
                .setView(emailEditText)
                .setPositiveButton("Share", null)
                .setNegativeButton("Cancel", null)
                .show()
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> shareWithEmail(emailEditText.getText().toString()));
    }

    private void shareWithEmail(String enteredEmail) {
        String emailLower = enteredEmail == null ? "" : enteredEmail.trim().toLowerCase(Locale.US);
        if (emailLower.isEmpty()) {
            Toast.makeText(this, "Enter an email address.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (emailLower.equals(currentItem.getOwnerEmail().toLowerCase(Locale.US))) {
            Toast.makeText(this, "You already own this item.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentItem.getSharedWithEmails().contains(emailLower)) {
            Toast.makeText(this, "This item is already shared with that user.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("userProfiles")
                .whereEqualTo("emailLower", emailLower)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        Toast.makeText(this, "No registered user found with that email.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String recipientUid = query.getDocuments().get(0).getId();
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("sharedWithUids", FieldValue.arrayUnion(recipientUid));
                    updates.put("sharedWithEmails", FieldValue.arrayUnion(emailLower));
                    db.collection("items").document(itemId).update(updates)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Item shared successfully.", Toast.LENGTH_SHORT).show();
                                loadItem();
                            })
                            .addOnFailureListener(error -> Toast.makeText(this, "Could not share item.", Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(error -> Toast.makeText(this, "Could not look up that user.", Toast.LENGTH_SHORT).show());
    }

    private void removeAllShares() {
        if (!currentUserOwnsItem()) {
            showReadOnlyToast();
            return;
        }
        db.collection("items").document(itemId)
                .update("sharedWithUids", new java.util.ArrayList<>(),
                        "sharedWithEmails", new java.util.ArrayList<>())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Sharing removed.", Toast.LENGTH_SHORT).show();
                    loadItem();
                })
                .addOnFailureListener(error -> Toast.makeText(this, "Could not remove sharing.", Toast.LENGTH_SHORT).show());
    }

    private void confirmDelete() {
        if (!currentUserOwnsItem()) {
            showReadOnlyToast();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Delete item?")
                .setMessage("This removes the saved item and its photo.")
                .setPositiveButton("Delete", (dialog, which) -> deleteItem())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteItem() {
        if (!currentUserOwnsItem()) {
            showReadOnlyToast();
            return;
        }
        ReminderScheduler.cancelReminder(this, itemId);
        String localImagePath = currentItem.getLocalImagePath();
        if (localImagePath != null && !localImagePath.isEmpty()) {
            File imageFile = new File(localImagePath);
            if (imageFile.exists()) {
                imageFile.delete();
            }
        }

        db.collection("items").document(itemId).delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(error -> Toast.makeText(this, "Could not delete item.", Toast.LENGTH_SHORT).show());
    }

    private boolean currentUserOwnsItem() {
        FirebaseUser user = auth.getCurrentUser();
        return currentItem != null && user != null && currentItem.isOwnedBy(user.getUid());
    }

    private void showReadOnlyToast() {
        Toast.makeText(this, "Shared items are read-only.", Toast.LENGTH_SHORT).show();
    }

    private void openLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
