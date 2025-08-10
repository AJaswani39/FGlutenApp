
package com.example.fgluten.ui.restaurant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fgluten.R;
import com.example.fgluten.data.Restaurant;

import java.util.ArrayList;
import java.util.List;

public class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.RestaurantViewHolder> {

    public interface OnRestaurantClickListener {
        void onRestaurantClick(Restaurant restaurant);
    }

    private List<Restaurant> restaurants = new ArrayList<>();
    private final OnRestaurantClickListener listener;

    public RestaurantAdapter(List<Restaurant> restaurants, OnRestaurantClickListener listener) {
        if (restaurants != null) {
            this.restaurants = restaurants;
        }
        this.listener = listener;
    }

    public void setRestaurants(List<Restaurant> restaurants) {
        this.restaurants = restaurants != null ? restaurants : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RestaurantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_restaurant, parent, false);
        return new RestaurantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RestaurantViewHolder holder, int position) {
        Restaurant restaurant = restaurants.get(position);
        holder.bind(restaurant, listener);
    }

    @Override
    public int getItemCount() {
        return restaurants.size();
    }

    static class RestaurantViewHolder extends RecyclerView.ViewHolder {

        private final TextView nameTextView;
        private final TextView addressTextView;

        public RestaurantViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.restaurant_name);
            addressTextView = itemView.findViewById(R.id.restaurant_address);
        }

        public void bind(Restaurant restaurant, OnRestaurantClickListener listener) {
            nameTextView.setText(restaurant.getName());
            addressTextView.setText(restaurant.getAddress());
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRestaurantClick(restaurant);
                }
            });
        }
    }
}
