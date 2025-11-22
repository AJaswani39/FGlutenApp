package com.example.fgluten.ui.restaurant;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.example.fgluten.R;
import com.example.fgluten.data.Restaurant;
import com.example.fgluten.util.SettingsManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.List;

/**
 * Rich, quick-glance detail sheet for a restaurant with actions (favorites, notes, menu, maps).
 */
public class RestaurantDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_RESTAURANT = "arg_restaurant";

    private RestaurantViewModel viewModel;
    private Restaurant current;

    public static RestaurantDetailBottomSheet newInstance(Restaurant restaurant) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_RESTAURANT, restaurant);
        RestaurantDetailBottomSheet sheet = new RestaurantDetailBottomSheet();
        sheet.setArguments(args);
        return sheet;
    }

    public static void show(FragmentManager fragmentManager, Restaurant restaurant) {
        if (fragmentManager == null || restaurant == null) {
            return;
        }
        RestaurantDetailBottomSheet sheet = newInstance(restaurant);
        sheet.show(fragmentManager, "restaurant_detail_sheet");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_restaurant_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(RestaurantViewModel.class);
        current = getArguments() != null ? getArguments().getParcelable(ARG_RESTAURANT) : null;
        if (current == null) {
            dismissAllowingStateLoss();
            return;
        }

        TextView nameView = view.findViewById(R.id.sheet_name);
        TextView addressView = view.findViewById(R.id.sheet_address);
        TextView metaView = view.findViewById(R.id.sheet_meta);
        TextView gfStatusView = view.findViewById(R.id.sheet_gf_status);
        TextView menuStatusView = view.findViewById(R.id.sheet_menu_status);
        TextView menuItemsView = view.findViewById(R.id.sheet_menu_items);
        TextView notesList = view.findViewById(R.id.sheet_notes_list);
        TextView favStatus = view.findViewById(R.id.sheet_fav_status);
        EditText noteInput = view.findViewById(R.id.sheet_note_input);
        Button addNote = view.findViewById(R.id.sheet_note_add);
        Button openMaps = view.findViewById(R.id.sheet_open_maps);
        Button openMenu = view.findViewById(R.id.sheet_open_menu);
        Button rescanButton = view.findViewById(R.id.sheet_rescan);
        MaterialButtonToggleGroup favToggle = view.findViewById(R.id.sheet_favorite_toggle);
        Button favClear = view.findViewById(R.id.sheet_fav_clear);

        render(current, nameView, addressView, metaView, gfStatusView, menuStatusView, menuItemsView, notesList, favStatus, openMenu, rescanButton, favToggle);

        openMaps.setOnClickListener(v -> openInMaps(current));
        openMenu.setOnClickListener(v -> openMenuUrl(current));
        rescanButton.setOnClickListener(v -> {
            if (current != null) {
                menuStatusView.setText(R.string.menu_scan_pending_detail);
                viewModel.requestMenuRescan(current);
            }
        });

        favToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || current == null) return;
            String status = null;
            if (checkedId == R.id.sheet_fav_safe) {
                status = "safe";
            } else if (checkedId == R.id.sheet_fav_try) {
                status = "try";
            } else if (checkedId == R.id.sheet_fav_avoid) {
                status = "avoid";
            }
            viewModel.setFavoriteStatus(current, status);
            favStatus.setText(status != null ? getString(R.string.favorite_status_label, status) : "");
        });

        favClear.setOnClickListener(v -> {
            if (current == null) return;
            favToggle.clearChecked();
            viewModel.setFavoriteStatus(current, null);
            favStatus.setText("");
        });

        addNote.setOnClickListener(v -> {
            if (current == null) return;
            String note = noteInput.getText() != null ? noteInput.getText().toString().trim() : "";
            if (!note.isEmpty()) {
                viewModel.addCrowdNote(current, note);
                noteInput.setText("");
            }
        });

        viewModel.getRestaurantState().observe(getViewLifecycleOwner(), state -> {
            if (state == null || state.getRestaurants() == null || current == null) return;
            Restaurant updated = findMatching(state.getRestaurants(), current);
            if (updated != null) {
                current = updated;
                render(updated, nameView, addressView, metaView, gfStatusView, menuStatusView, menuItemsView, notesList, favStatus, openMenu, rescanButton, favToggle);
            }
        });
    }

    private void render(Restaurant restaurant,
                        TextView nameView,
                        TextView addressView,
                        TextView metaView,
                        TextView gfStatusView,
                        TextView menuStatusView,
                        TextView menuItemsView,
                        TextView notesListView,
                        TextView favStatusView,
                        Button openMenuButton,
                        Button rescanButton,
                        MaterialButtonToggleGroup favToggle) {
        if (restaurant == null) {
            return;
        }
        nameView.setText(!TextUtils.isEmpty(restaurant.getName()) ? restaurant.getName() : getString(R.string.missing_data));
        addressView.setText(!TextUtils.isEmpty(restaurant.getAddress()) ? restaurant.getAddress() : "");

        metaView.setText(buildMeta(restaurant));

        String gfSummary = buildGfSummary(restaurant);
        gfStatusView.setText(gfSummary);

        String menuStatusText = buildMenuStatus(restaurant);
        menuStatusView.setText(menuStatusText);

        List<String> menu = restaurant.getGlutenFreeMenu();
        if (menu != null && !menu.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String line : menu) {
                if (TextUtils.isEmpty(line)) continue;
                sb.append("\u2022 ").append(line.trim()).append("\n");
            }
            menuItemsView.setText(sb.toString().trim());
        } else {
            menuItemsView.setText(getString(R.string.gf_menu_unknown));
        }

        List<String> notes = restaurant.getCrowdNotes();
        if (notes != null && !notes.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String n : notes) {
                if (TextUtils.isEmpty(n)) continue;
                sb.append("\u2022 ").append(n.trim()).append("\n");
            }
            notesListView.setText(sb.toString().trim());
        } else {
            notesListView.setText(getString(R.string.crowd_notes_empty));
        }

        favToggle.clearChecked();
        String status = restaurant.getFavoriteStatus();
        if ("safe".equals(status)) favToggle.check(R.id.sheet_fav_safe);
        if ("try".equals(status)) favToggle.check(R.id.sheet_fav_try);
        if ("avoid".equals(status)) favToggle.check(R.id.sheet_fav_avoid);
        favStatusView.setText(!TextUtils.isEmpty(status) ? getString(R.string.favorite_status_label, status) : "");

        boolean hasMenuUrl = !TextUtils.isEmpty(restaurant.getMenuUrl());
        openMenuButton.setEnabled(hasMenuUrl);
        openMenuButton.setText(hasMenuUrl ? getString(R.string.detail_menu_button) : getString(R.string.detail_menu_unavailable));

        boolean canRescan = !TextUtils.isEmpty(restaurant.getPlaceId());
        rescanButton.setEnabled(canRescan);
    }

    private String buildMeta(Restaurant restaurant) {
        StringBuilder sb = new StringBuilder();
        if (restaurant.getRating() != null) {
            sb.append(String.format("%.1f \u2605", restaurant.getRating()));
        }
        if (restaurant.getOpenNow() != null) {
            if (sb.length() > 0) sb.append(" \u2022 ");
            sb.append(restaurant.getOpenNow() ? getString(R.string.meta_open_now) : getString(R.string.meta_closed));
        }
        String distanceLabel = formatDistance(restaurant.getDistanceMeters());
        if (!TextUtils.isEmpty(distanceLabel)) {
            if (sb.length() > 0) sb.append(" \u2022 ");
            sb.append(distanceLabel);
        }
        return sb.toString();
    }

    private String formatDistance(double meters) {
        if (meters <= 0) return null;
        boolean useMiles = SettingsManager.useMiles(requireContext());
        if (useMiles) {
            double miles = meters / 1609.34;
            if (miles >= 0.1) {
                return getString(R.string.distance_miles_away, miles);
            }
            int feet = (int) Math.round(meters * 3.28084);
            return getString(R.string.distance_feet_away, feet);
        } else {
            if (meters >= 1000) {
                double km = meters / 1000.0;
                return getString(R.string.distance_km_away, km);
            }
            int roundedMeters = (int) Math.round(meters);
            return getString(R.string.distance_m_away, roundedMeters);
        }
    }

    private String buildGfSummary(Restaurant restaurant) {
        List<String> menu = restaurant.getGlutenFreeMenu();
        if (restaurant.hasGlutenFreeOptions()) {
            if (menu != null && menu.size() >= 3) {
                return getString(R.string.gf_many);
            }
            return getString(R.string.gf_limited);
        }
        if (menu != null && !menu.isEmpty()) {
            return getString(R.string.gf_limited);
        }
        return getString(R.string.gf_menu_unknown);
    }

    private String buildMenuStatus(Restaurant restaurant) {
        Restaurant.MenuScanStatus scanStatus = restaurant.getMenuScanStatus();
        if (scanStatus == Restaurant.MenuScanStatus.FETCHING) {
            return getString(R.string.menu_scan_pending_detail);
        } else if (scanStatus == Restaurant.MenuScanStatus.SUCCESS) {
            if (restaurant.getGlutenFreeMenu() != null && !restaurant.getGlutenFreeMenu().isEmpty()) {
                return getString(R.string.menu_scan_scanned_with_gf);
            }
            return getString(R.string.menu_scan_scanned_no_gf);
        } else if (scanStatus == Restaurant.MenuScanStatus.NO_WEBSITE) {
            return getString(R.string.menu_scan_no_site);
        } else if (scanStatus == Restaurant.MenuScanStatus.FAILED) {
            return getString(R.string.menu_scan_unavailable);
        }
        return getString(R.string.menu_scan_not_started);
    }

    private void openMenuUrl(Restaurant restaurant) {
        if (restaurant == null || TextUtils.isEmpty(restaurant.getMenuUrl())) {
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(restaurant.getMenuUrl()));
            startActivity(intent);
        } catch (Exception ignored) {
            // swallow; nothing else to do here.
        }
    }

    private void openInMaps(Restaurant restaurant) {
        if (restaurant == null) return;
        try {
            Uri uri = Uri.parse("geo:" + restaurant.getLatitude() + "," + restaurant.getLongitude() +
                    "?q=" + Uri.encode(restaurant.getName()));
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setPackage("com.google.android.apps.maps");
            startActivity(intent);
        } catch (Exception e) {
            try {
                Uri uri = Uri.parse("geo:" + restaurant.getLatitude() + "," + restaurant.getLongitude());
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            } catch (Exception ignored) {
                // quietly ignore if no maps app.
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
