package com.example.fgluten.ui.restaurant;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.fgluten.R;
import com.example.fgluten.data.Restaurant;
import com.example.fgluten.util.SettingsManager;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.List;

public class RestaurantDetailFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_restaurant_detail, container, false);
        RestaurantViewModel viewModel = new ViewModelProvider(requireActivity()).get(RestaurantViewModel.class);

        TextView nameView = root.findViewById(R.id.detail_name);
        TextView addressView = root.findViewById(R.id.detail_address);
        TextView gfStatusView = root.findViewById(R.id.detail_gf_status);
        TextView menuView = root.findViewById(R.id.detail_menu);
        TextView ratingView = root.findViewById(R.id.detail_rating);
        TextView hoursView = root.findViewById(R.id.detail_hours);
        TextView distanceView = root.findViewById(R.id.detail_distance);
        TextView menuStatusView = root.findViewById(R.id.detail_menu_status);
        Button rescanButton = root.findViewById(R.id.detail_rescan);
        MaterialButtonToggleGroup favToggle = root.findViewById(R.id.favorite_toggle);
        Button favClear = root.findViewById(R.id.fav_clear);
        TextView favStatus = root.findViewById(R.id.detail_fav_status);
        TextView notesList = root.findViewById(R.id.detail_notes_list);
        EditText noteInput = root.findViewById(R.id.detail_note_input);
        Button noteAdd = root.findViewById(R.id.detail_note_add);

        String missingData = getString(R.string.missing_data);

        final Restaurant[] current = new Restaurant[1];


        Bundle args = getArguments();
        if (args != null) {
            Restaurant restaurant = (Restaurant) args.getParcelable("restaurant");
            if (restaurant != null) {
                current[0] = restaurant;
            }
        }

        renderRestaurant(current[0], missingData, nameView, addressView, gfStatusView, menuView, ratingView, hoursView, distanceView, menuStatusView, favToggle, favStatus, notesList);

        rescanButton.setOnClickListener(v -> {
            if (current[0] != null) {
                menuStatusView.setText(R.string.menu_scan_requested);
                viewModel.requestMenuRescan(current[0]);
            }
        });

        favToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (current[0] == null) return;
            String status = null;
            if (checkedId == R.id.fav_safe) {
                status = "safe";
            } else if (checkedId == R.id.fav_try) {
                status = "try";
            } else if (checkedId == R.id.fav_avoid) {
                status = "avoid";
            }
            viewModel.setFavoriteStatus(current[0], status);
            favStatus.setText(status != null ? getString(R.string.favorite_status_label, status) : "");
        });

        favClear.setOnClickListener(v -> {
            if (current[0] == null) return;
            favToggle.clearChecked();
            viewModel.setFavoriteStatus(current[0], null);
            favStatus.setText("");
        });

        noteAdd.setOnClickListener(v -> {
            if (current[0] == null) return;
            String note = noteInput.getText() != null ? noteInput.getText().toString().trim() : "";
            if (!note.isEmpty()) {
                viewModel.addCrowdNote(current[0], note);
                noteInput.setText("");
            }
        });

        viewModel.getRestaurantState().observe(getViewLifecycleOwner(), state -> {
            if (state == null || state.getRestaurants() == null || current[0] == null) {
                return;
            }
            Restaurant updated = findMatching(state.getRestaurants(), current[0]);
            if (updated != null) {
                current[0] = updated;
                renderRestaurant(updated, missingData, nameView, addressView, gfStatusView, menuView, ratingView, hoursView, distanceView, menuStatusView, favToggle, favStatus, notesList);
            }
        });

        return root;
    }

    private void renderRestaurant(Restaurant restaurant,
                                  String missingData,
                                  TextView nameView,
                                  TextView addressView,
                                  TextView gfStatusView,
                                  TextView menuView,
                                  TextView ratingView,
                                  TextView hoursView,
                                  TextView distanceView,
                                  TextView menuStatusView,
                                  MaterialButtonToggleGroup favToggle,
                                  TextView favStatusView,
                                  TextView notesListView) {
        String name = null;
        String address = null;
        Boolean hasGfOptions = null;
        List<String> menu = null;
        Double rating = null;
        Boolean openNow = null;
        double distanceMeters = 0;
        Restaurant.MenuScanStatus scanStatus = null;
        List<String> notes = null;

        if (restaurant != null) {
            name = restaurant.getName();
            address = restaurant.getAddress();
            hasGfOptions = restaurant.hasGlutenFreeOptions();
            menu = restaurant.getGlutenFreeMenu();
            rating = restaurant.getRating();
            openNow = restaurant.getOpenNow();
            distanceMeters = restaurant.getDistanceMeters();
            scanStatus = restaurant.getMenuScanStatus();
            notes = restaurant.getCrowdNotes();
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

        String menuStatusText;
        if (scanStatus == Restaurant.MenuScanStatus.FETCHING) {
            menuStatusText = getString(R.string.menu_scan_pending_detail);
        } else if (scanStatus == Restaurant.MenuScanStatus.SUCCESS) {
            if (menu != null && !menu.isEmpty()) {
                menuStatusText = getString(R.string.menu_scan_scanned_with_gf);
            } else {
                menuStatusText = getString(R.string.menu_scan_scanned_no_gf);
            }
        } else if (scanStatus == Restaurant.MenuScanStatus.NO_WEBSITE) {
            menuStatusText = getString(R.string.menu_scan_no_site);
        } else if (scanStatus == Restaurant.MenuScanStatus.FAILED) {
            menuStatusText = getString(R.string.menu_scan_unavailable);
        } else {
            menuStatusText = getString(R.string.menu_scan_not_started);
        }
        menuStatusView.setText(menuStatusText);

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

        if (favToggle != null) {
            favToggle.clearChecked();
            String status = restaurant != null ? restaurant.getFavoriteStatus() : null;
            if ("safe".equals(status)) favToggle.check(R.id.fav_safe);
            if ("try".equals(status)) favToggle.check(R.id.fav_try);
            if ("avoid".equals(status)) favToggle.check(R.id.fav_avoid);
            if (favStatusView != null) {
                favStatusView.setText(status != null ? getString(R.string.favorite_status_label, status) : "");
            }
        }

        if (notesListView != null) {
            if (notes != null && !notes.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                int count = 0;
                for (String n : notes) {
                    if (n == null || n.trim().isEmpty()) continue;
                    sb.append("\u2022 ").append(n.trim()).append("\n");
                    count++;
                }
                if (sb.length() > 0) {
                    notesListView.setText(sb.toString().trim());
                } else {
                    notesListView.setText(getString(R.string.crowd_notes_empty));
                }
            } else {
                notesListView.setText(getString(R.string.crowd_notes_empty));
            }
        }
    }

    private Restaurant findMatching(List<Restaurant> restaurants, Restaurant target) {
        if (restaurants == null || target == null) return null;
        for (Restaurant r : restaurants) {
            if (r.getPlaceId() != null && target.getPlaceId() != null && r.getPlaceId().equals(target.getPlaceId())) {
                return r;
            }
            if (target.getPlaceId() == null && r.getName() != null && r.getName().equals(target.getName())
                    && r.getAddress() != null && r.getAddress().equals(target.getAddress())) {
                return r;
            }
        }
        return null;
    }
}
