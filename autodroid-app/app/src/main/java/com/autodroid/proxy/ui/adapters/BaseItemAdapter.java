// BaseItemAdapter.java
package com.autodroid.proxy.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.autodroid.proxy.R;

import java.util.List;
import java.util.Map;

public class BaseItemAdapter extends RecyclerView.Adapter<BaseItemAdapter.ItemViewHolder> {
    private List<Map<String, Object>> items;
    private OnItemClickListener listener;
    private int itemLayoutRes;

    public interface OnItemClickListener {
        void onItemClick(Map<String, Object> item);
    }

    public BaseItemAdapter(List<Map<String, Object>> items, OnItemClickListener listener, int itemLayoutRes) {
        this.items = items;
        this.listener = listener;
        this.itemLayoutRes = itemLayoutRes;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(itemLayoutRes, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Map<String, Object> item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public void updateItems(List<Map<String, Object>> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        private TextView titleTextView;
        private TextView subtitleTextView;
        private TextView statusTextView;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.item_title);
            subtitleTextView = itemView.findViewById(R.id.item_subtitle);
            statusTextView = itemView.findViewById(R.id.item_status);
        }

        public void bind(Map<String, Object> item) {
            // Bind common fields
            if (titleTextView != null) {
                titleTextView.setText((String) item.getOrDefault("title", "Unknown"));
            }

            if (subtitleTextView != null) {
                subtitleTextView.setText((String) item.getOrDefault("subtitle", ""));
            }

            if (statusTextView != null) {
                statusTextView.setText((String) item.getOrDefault("status", ""));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });
        }
    }
}