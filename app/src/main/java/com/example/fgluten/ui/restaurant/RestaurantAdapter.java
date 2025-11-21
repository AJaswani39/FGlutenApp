package com.example.fgluten.ui.restaurant;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fgluten.R;
import com.example.fgluten.data.Restaurant;
import com.example.fgluten.util.SettingsManager;

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
        private final TextView distanceTextView;
        private final TextView gfBadgeView;
        private final TextView metaView;
        private final Button openMapsButton;

        public RestaurantViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.restaurant_name);
            addressTextView = itemView.findViewById(R.id.restaurant_address);
            distanceTextView = itemView.findViewById(R.id.restaurant_distance);
            gfBadgeView = itemView.findViewById(R.id.restaurant_gf_badge);
            metaView = itemView.findViewById(R.id.restaurant_meta);
            openMapsButton = itemView.findViewById(R.id.restaurant_open_maps);
        }

        public void bind(Restaurant restaurant, OnRestaurantClickListener listener) {
            nameTextView.setText(restaurant.getName());
            addressTextView.setText(restaurant.getAddress());

            double distanceMeters = restaurant.getDistanceMeters();
            String distanceLabel = formatDistance(itemView.getContext(), distanceMeters);
            if (distanceLabel != null) {
                distanceTextView.setText(distanceLabel);
                distanceTextView.setVisibility(View.VISIBLE);
            } else {
                distanceTextView.setVisibility(View.GONE);
            }

            if (restaurant.hasGlutenFreeOptions()) {
                gfBadgeView.setVisibility(View.VISIBLE);
            } else {
                gfBadgeView.setVisibility(View.GONE);
            }

            String meta = buildMeta(restaurant);
            if (meta != null && !meta.isEmpty()) {
                metaView.setVisibility(View.VISIBLE);
                metaView.setText(meta);
            } else {
                metaView.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRestaurantClick(restaurant);
                }
            });

            openMapsButton.setOnClickListener(v -> openInMaps(restaurant));
        }

        private String formatDistance(Context context, double meters) {
            if (meters <= 0) {
                return null;
            }
            boolean useMiles = SettingsManager.useMiles(context);
            if (useMiles) {
                double miles = meters / 1609.34;
                if (miles >= 0.1) {
                    return context.getString(R.string.distance_miles_away, miles);
                } else {
                    int feet = (int) Math.round(meters * 3.28084);
                    return context.getString(R.string.distance_feet_away, feet);
                }
            } else {
                if (meters >= 1000) {
                    double km = meters / 1000.0;
                    return context.getString(R.string.distance_km_away, km);
                }
                int roundedMeters = (int) Math.round(meters);
                return context.getString(R.string.distance_m_away, roundedMeters);
            }
        }

        private String buildMeta(Restaurant restaurant) {
            StringBuilder sb = new StringBuilder();
            if (restaurant.getRating() != null) {
                sb.append(String.format("%.1f ?", restaurant.getRating()));
            }
            if (restaurant.getOpenNow() != null) {
                if (sb.length() > 0) sb.append("  ");
                sb.append(restaurant.getOpenNow() ? itemView.getContext().getString(R.string.meta_open_now)
                        : itemView.getContext().getString(R.string.meta_closed));
            }
            String scanLabel = menuScanLabel(itemView.getContext(), restaurant);
            if (!TextUtils.isEmpty(scanLabel)) {
                if (sb.length() > 0) sb.append(" \u2022 ");
                sb.append(scanLabel);
            }
            return sb.toString();
        }

        private String menuScanLabel(Context context, Restaurant restaurant) {
            Restaurant.MenuScanStatus status = restaurant.getMenuScanStatus();
            if (status == null) {
                return null;
            }
            if (status == Restaurant.MenuScanStatus.FETCHING) {
                return context.getString(R.string.menu_scan_pending_short);
            } else if (status == Restaurant.MenuScanStatus.SUCCESS) {
                if (restaurant.getGlutenFreeMenu() != null && !restaurant.getGlutenFreeMenu().isEmpty()) {
                    return context.getString(R.string.menu_scan_found_gf);
                }
                return context.getString(R.string.menu_scan_none_found);
            } else if (status == Restaurant.MenuScanStatus.NO_WEBSITE) {
                return context.getString(R.string.menu_scan_no_site);
            } else if (status == Restaurant.MenuScanStatus.FAILED) {
                return context.getString(R.string.menu_scan_unavailable);
            }
            return null;
        }

        private void openInMaps(Restaurant restaurant) {
            try {
                Uri uri = Uri.parse("geo:" + restaurant.getLatitude() + "," + restaurant.getLongitude() +
                        "?q=" + Uri.encode(restaurant.getName()));
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.setPackage("com.google.android.apps.maps");
                itemView.getContext().startActivity(intent);
            } catch (Exception e) {
                try {
                    Uri uri = Uri.parse("geo:" + restaurant.getLatitude() + "," + restaurant.getLongitude());
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    itemView.getContext().startActivity(intent);
                } catch (Exception ex) {
                    if (itemView.getContext() != null) {
                        android.widget.Toast.makeText(itemView.getContext(), itemView.getContext().getString(R.string.marker_directions_error), android.widget.Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }
}
