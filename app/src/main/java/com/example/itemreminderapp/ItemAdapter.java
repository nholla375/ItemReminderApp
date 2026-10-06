package com.example.itemreminderapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {
    public interface OnItemClickListener {
        void onItemClick(Item item);
    }

    private final List<Item> allItems = new ArrayList<>();
    private final List<Item> visibleItems = new ArrayList<>();
    private final OnItemClickListener listener;
    private boolean showingSharedItems = false;

    public ItemAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<Item> items) {
        allItems.clear();
        allItems.addAll(items);
        visibleItems.clear();
        visibleItems.addAll(items);
        notifyDataSetChanged();
    }

    public void setShowingSharedItems(boolean showingSharedItems) {
        this.showingSharedItems = showingSharedItems;
        notifyDataSetChanged();
    }

    public void filter(String query, String category) {
        String cleanQuery = query == null ? "" : query.trim().toLowerCase(Locale.getDefault());
        String cleanCategory = category == null ? "All" : category.trim();
        visibleItems.clear();

        for (Item item : allItems) {
            String name = item.getItemName() == null ? "" : item.getItemName().toLowerCase(Locale.getDefault());
            String notes = item.getNotes() == null ? "" : item.getNotes().toLowerCase(Locale.getDefault());
            String itemCategory = item.getCategory();

            boolean matchesSearch = cleanQuery.isEmpty() || name.contains(cleanQuery) || notes.contains(cleanQuery);
            boolean matchesCategory = "All".equals(cleanCategory) || itemCategory.equals(cleanCategory);
            if (matchesSearch && matchesCategory) {
                visibleItems.add(item);
            }
        }

        notifyDataSetChanged();
    }

    public int getVisibleItemCount() {
        return visibleItems.size();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = visibleItems.get(position);
        String star = item.isFavorite() ? "\u2605 " : "\u2606 ";
        holder.itemNameTextView.setText(star + item.getItemName());
        holder.dateTextView.setText(item.getFormattedDate());
        holder.categoryTextView.setText("Category: " + item.getCategory());

        String notes = item.getNotes();
        if (notes == null || notes.trim().isEmpty()) {
            holder.notesTextView.setText("No notes added");
        } else {
            holder.notesTextView.setText(notes);
        }

        if (showingSharedItems) {
            holder.sharedByTextView.setVisibility(View.VISIBLE);
            String ownerEmail = item.getOwnerEmail() == null || item.getOwnerEmail().isEmpty()
                    ? "another user"
                    : item.getOwnerEmail();
            holder.sharedByTextView.setText("Shared by: " + ownerEmail);
        } else {
            holder.sharedByTextView.setVisibility(View.GONE);
        }

        String localImagePath = item.getLocalImagePath();
        File imageFile = localImagePath == null || localImagePath.isEmpty() ? null : new File(localImagePath);
        Object imageSource = imageFile != null && imageFile.exists() ? imageFile : R.drawable.photo_placeholder;
        Glide.with(holder.itemView.getContext())
                .load(imageSource)
                .placeholder(R.drawable.photo_placeholder)
                .centerCrop()
                .into(holder.thumbnailImageView);

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return visibleItems.size();
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnailImageView;
        TextView itemNameTextView;
        TextView dateTextView;
        TextView categoryTextView;
        TextView notesTextView;
        TextView sharedByTextView;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnailImageView = itemView.findViewById(R.id.thumbnailImageView);
            itemNameTextView = itemView.findViewById(R.id.itemNameTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            categoryTextView = itemView.findViewById(R.id.categoryTextView);
            notesTextView = itemView.findViewById(R.id.notesPreviewTextView);
            sharedByTextView = itemView.findViewById(R.id.sharedByTextView);
        }
    }
}
