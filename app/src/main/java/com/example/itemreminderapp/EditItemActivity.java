package com.example.itemreminderapp;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class EditItemActivity extends AppCompatActivity {
    private static final int REQUEST_NOTIFICATION_PERMISSION = 203;

    private EditText itemNameEditText;
    private EditText notesEditText;
    private Spinner categorySpinner;
    private TextView reminderTextView;
    private String itemId;
    private Item currentItem;
    private boolean hasReminder;
    private long reminderTimeMillis;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (auth.getCurrentUser() == null) {
            openLogin();
            return;
        }

        setContentView(R.layout.activity_edit_item);

        itemNameEditText = findViewById(R.id.itemNameEditText);
        notesEditText = findViewById(R.id.notesEditText);
        categorySpinner = findViewById(R.id.categorySpinner);
        reminderTextView = findViewById(R.id.reminderTextView);
        Button setReminderButton = findViewById(R.id.setReminderButton);
        Button removeReminderButton = findViewById(R.id.removeReminderButton);
        Button saveChangesButton = findViewById(R.id.saveChangesButton);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                FirebaseUtils.CATEGORIES);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        itemId = getIntent().getStringExtra("itemId");
        if (itemId == null || itemId.isEmpty()) {
            Toast.makeText(this, "Item not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setReminderButton.setOnClickListener(v -> showReminderPickers());
        removeReminderButton.setOnClickListener(v -> clearReminder());
        saveChangesButton.setOnClickListener(v -> saveChanges());
        loadItem();
    }

    private void loadItem() {
        db.collection("items").document(itemId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    currentItem = documentSnapshot.toObject(Item.class);
                    FirebaseUser user = auth.getCurrentUser();
                    if (currentItem == null || user == null) {
                        Toast.makeText(this, "Could not load item.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    currentItem.normalizeDefaults();
                    currentItem.setItemId(documentSnapshot.getId());
                    if (!currentItem.isOwnedBy(user.getUid())) {
                        Toast.makeText(this, "Shared items are read-only.", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    itemNameEditText.setText(currentItem.getItemName());
                    notesEditText.setText(currentItem.getNotes());
                    selectCategory(currentItem.getCategory());
                    hasReminder = currentItem.isHasReminder();
                    reminderTimeMillis = currentItem.getReminderTimeMillis();
                    updateReminderText();
                })
                .addOnFailureListener(error -> Toast.makeText(this, "Could not load item.", Toast.LENGTH_SHORT).show());
    }

    private void selectCategory(String category) {
        for (int i = 0; i < FirebaseUtils.CATEGORIES.length; i++) {
            if (FirebaseUtils.CATEGORIES[i].equals(category)) {
                categorySpinner.setSelection(i);
                return;
            }
        }
        categorySpinner.setSelection(FirebaseUtils.CATEGORIES.length - 1);
    }

    private void showReminderPickers() {
        Calendar calendar = Calendar.getInstance();
        if (reminderTimeMillis > System.currentTimeMillis()) {
            calendar.setTimeInMillis(reminderTimeMillis);
        }

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            new TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                reminderTimeMillis = calendar.getTimeInMillis();
                hasReminder = reminderTimeMillis > System.currentTimeMillis();
                if (!hasReminder) {
                    reminderTimeMillis = 0;
                    Toast.makeText(this, "Choose a future reminder time.", Toast.LENGTH_SHORT).show();
                }
                updateReminderText();
                requestNotificationPermissionIfNeeded();
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void clearReminder() {
        hasReminder = false;
        reminderTimeMillis = 0;
        updateReminderText();
    }

    private void updateReminderText() {
        reminderTextView.setText(hasReminder
                ? "Reminder: " + FirebaseUtils.formatTimestamp(reminderTimeMillis)
                : "Reminder: none");
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATION_PERMISSION);
        }
    }

    private void saveChanges() {
        FirebaseUser user = auth.getCurrentUser();
        if (currentItem == null || user == null) {
            openLogin();
            return;
        }
        if (!currentItem.isOwnedBy(user.getUid())) {
            Toast.makeText(this, "Shared items are read-only.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String newName = itemNameEditText.getText().toString().trim();
        String newNotes = notesEditText.getText().toString().trim();
        String category = String.valueOf(categorySpinner.getSelectedItem());

        if (newName.isEmpty()) {
            Toast.makeText(this, "Please enter an item name.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("itemName", newName);
        updates.put("notes", newNotes);
        updates.put("category", category);
        updates.put("hasReminder", hasReminder);
        updates.put("reminderTimeMillis", reminderTimeMillis);

        db.collection("items").document(itemId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    ReminderScheduler.cancelReminder(this, itemId);
                    if (hasReminder) {
                        ReminderScheduler.scheduleReminder(this, itemId, newName, reminderTimeMillis);
                    }
                    Toast.makeText(this, buildSaveMessage(), Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(error -> Toast.makeText(this, "Could not save changes.", Toast.LENGTH_SHORT).show());
    }

    private String buildSaveMessage() {
        if (hasReminder && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return "Item updated. Reminder saved, but notifications are disabled.";
        }
        return "Item updated";
    }

    private void openLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION
                && (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
            Toast.makeText(this, "Reminder saved, but notifications are disabled.", Toast.LENGTH_LONG).show();
        }
    }
}
