package com.example.itemreminderapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
    private ItemAdapter itemAdapter;
    private TextView emptyTextView;
    private EditText searchEditText;
    private Spinner categoryFilterSpinner;
    private FloatingActionButton addItemButton;
    private Button myItemsButton;
    private Button sharedItemsButton;
    private ListenerRegistration itemsListener;
    private boolean showingSharedItems = false;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (auth.getCurrentUser() == null) {
            openLogin();
            return;
        }
        FirebaseUtils.ensureUserProfileExists();

        setContentView(R.layout.activity_home);

        RecyclerView recyclerView = findViewById(R.id.itemsRecyclerView);
        emptyTextView = findViewById(R.id.emptyTextView);
        searchEditText = findViewById(R.id.searchEditText);
        categoryFilterSpinner = findViewById(R.id.categoryFilterSpinner);
        addItemButton = findViewById(R.id.addItemButton);
        myItemsButton = findViewById(R.id.myItemsButton);
        sharedItemsButton = findViewById(R.id.sharedItemsButton);

        findViewById(R.id.signOutButton).setOnClickListener(v -> {
            auth.signOut();
            openLogin();
        });

        itemAdapter = new ItemAdapter(item -> {
            Intent intent = new Intent(HomeActivity.this, DetailActivity.class);
            intent.putExtra("itemId", item.getItemId());
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(itemAdapter);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                FirebaseUtils.FILTER_CATEGORIES);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categoryFilterSpinner.setAdapter(categoryAdapter);

        addItemButton.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, AddItemActivity.class)));
        myItemsButton.setOnClickListener(v -> switchList(false));
        sharedItemsButton.setOnClickListener(v -> switchList(true));

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        categoryFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        switchList(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (itemsListener != null) {
            itemsListener.remove();
        }
    }

    private void switchList(boolean shared) {
        showingSharedItems = shared;
        addItemButton.setVisibility(shared ? View.GONE : View.VISIBLE);
        myItemsButton.setEnabled(shared);
        sharedItemsButton.setEnabled(!shared);
        myItemsButton.setText(shared ? "My Items" : "Viewing My Items");
        sharedItemsButton.setText(shared ? "Viewing Shared" : "Shared With Me");
        itemAdapter.setShowingSharedItems(shared);
        loadItems();
    }

    private void loadItems() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            openLogin();
            return;
        }

        if (itemsListener != null) {
            itemsListener.remove();
        }

        if (showingSharedItems) {
            itemsListener = db.collection("items")
                    .whereArrayContains("sharedWithUids", currentUser.getUid())
                    .addSnapshotListener((queryDocumentSnapshots, error) -> handleItemsSnapshot(queryDocumentSnapshots, error));
        } else {
            itemsListener = db.collection("items")
                    .whereEqualTo("ownerUid", currentUser.getUid())
                    .addSnapshotListener((queryDocumentSnapshots, error) -> handleItemsSnapshot(queryDocumentSnapshots, error));
        }
    }

    private void handleItemsSnapshot(com.google.firebase.firestore.QuerySnapshot queryDocumentSnapshots,
                                     com.google.firebase.firestore.FirebaseFirestoreException error) {
        if (error != null) {
            Toast.makeText(this, "Could not load items. Check your connection.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Item> items = new ArrayList<>();
        if (queryDocumentSnapshots != null) {
            queryDocumentSnapshots.forEach(document -> {
                Item item = document.toObject(Item.class);
                if (item != null) {
                    item.normalizeDefaults();
                    if (item.getItemId() == null || item.getItemId().isEmpty()) {
                        item.setItemId(document.getId());
                    }
                    if (!item.isArchived()) {
                        items.add(item);
                    }
                }
            });
        }

        Collections.sort(items, (a, b) -> {
            if (a.isFavorite() && !b.isFavorite()) {
                return -1;
            }
            if (!a.isFavorite() && b.isFavorite()) {
                return 1;
            }
            return Long.compare(b.getTimestamp(), a.getTimestamp());
        });
        itemAdapter.setItems(items);
        applyFilters();
    }

    private void applyFilters() {
        if (itemAdapter == null || searchEditText == null || categoryFilterSpinner == null) {
            return;
        }
        String selectedCategory = String.valueOf(categoryFilterSpinner.getSelectedItem());
        itemAdapter.filter(searchEditText.getText().toString(), selectedCategory);
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (itemAdapter.getVisibleItemCount() == 0) {
            emptyTextView.setVisibility(View.VISIBLE);
            emptyTextView.setText(showingSharedItems
                    ? "No items have been shared with you yet."
                    : "No saved items yet. Tap + to add one.");
        } else {
            emptyTextView.setVisibility(View.GONE);
        }
    }

    private void openLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
