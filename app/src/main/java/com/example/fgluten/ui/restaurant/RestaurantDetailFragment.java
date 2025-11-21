package com.example.fgluten.ui.restaurant;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fgluten.R;
import com.example.fgluten.data.Restaurant;
import com.example.fgluten.util.SettingsManager;

import java.util.List;

public class RestaurantDetailFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_restaurant_detail, container, false);

        TextView nameView = root.findViewById(R.id.detail_name);
        TextView addressView = root.findViewById(R.id.detail_address);
        TextView gfStatusView = root.findViewById(R.id.detail_gf_status);
        TextView menuView = root.findViewById(R.id.detail_menu);
        TextView ratingView = root.findViewById(R.id.detail_rating);
        TextView hoursView = root.findViewById(R.id.detail_hours);
        TextView distanceView = root.findViewById(R.id.detail_distance);

        String missingData = getString(R.string.missing_data);

        String name = null;
        String address = null;
        Boolean hasGfOptions = null;
        List<String> menu = null;
        Double rating = null;
        Boolean openNow = null;
        double distanceMeters = 0;


        Bundle args = getArguments();
        if (args != null) {
            Restaurant restaurant = (Restaurant) args.getParcelable("restaurant");
            if (restaurant != null) {
                name = restaurant.getName();
                address = restaurant.getAddress();
                hasGfOptions = restaurant.hasGlutenFreeOptions();
                menu = restaurant.getGlutenFreeMenu();
                rating = restaurant.getRating();
                openNow = restaurant.getOpenNow();
                distanceMeters = restaurant.getDistanceMeters();
            }
        }

        nameView.setText(name != null ? name : missingData);
        addressView.setText(address != null ? address : missingData);

        // GF Summary
        String gfSummary;
        if (hasGfOptions != null && !hasGfOptions) {
            gfSummary = getString(R.string.gf_none);
        } else if (menu != null && !menu.isEmpty()) {
            if (menu.size() >= 3) {
                gfSummary = getString(R.string.gf_many);
            } else {
                gfSummary = getString(R.string.gf_limited);
            }
        } else if (hasGfOptions != null && hasGfOptions) {
            gfSummary = getString(R.string.gf_limited);
        } else {
            gfSummary = missingData;
        }
        gfStatusView.setText(gfSummary);

        // Menu lines
        menuView.setText(menu != null && !menu.isEmpty() ? TextUtils.join("\n", menu) : getString(R.string.gf_menu_unknown));

        // Rating
        if (rating != null) {
            ratingView.setVisibility(View.VISIBLE);
            ratingView.setText(getString(R.string.detail_rating, rating));
        } else {
            ratingView.setVisibility(View.GONE);
        }

        // Hours
        if (openNow != null) {
            hoursView.setVisibility(View.VISIBLE);
            hoursView.setText(openNow ? R.string.detail_open_now : R.string.detail_closed);
        } else {
            hoursView.setVisibility(View.VISIBLE);
            hoursView.setText(R.string.detail_hours_unknown);
        }

        // Distance
        if (distanceMeters > 0) {
            boolean useMiles = SettingsManager.useMiles(requireContext());
            String label;
            if (useMiles) {
                double miles = distanceMeters / 1609.34;
                if (miles >= 0.1) {
                    label = getString(R.string.distance_miles_away, miles);
                } else {
                    int feet = (int) Math.round(distanceMeters * 3.28084);
                    label = getString(R.string.distance_feet_away, feet);
                }
            } else {
                if (distanceMeters >= 1000) {
                    double km = distanceMeters / 1000.0;
                    label = getString(R.string.distance_km_away, km);
                } else {
                    int roundedMeters = (int) Math.round(distanceMeters);
                    label = getString(R.string.distance_m_away, roundedMeters);
                }
            }
            distanceView.setVisibility(View.VISIBLE);
            distanceView.setText(label);
        } else {
            distanceView.setVisibility(View.GONE);
        }

        return root;
    }
}
