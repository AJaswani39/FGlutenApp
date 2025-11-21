package com.example.fgluten.ui.restaurant;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.fgluten.R;
import com.example.fgluten.data.Restaurant;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class RestaurantMarkerBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_RESTAURANT = "arg_restaurant";

    public static RestaurantMarkerBottomSheet newInstance(Restaurant restaurant) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_RESTAURANT, restaurant);
        RestaurantMarkerBottomSheet sheet = new RestaurantMarkerBottomSheet();
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_marker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Restaurant restaurant = getArguments() != null ? getArguments().getParcelable(ARG_RESTAURANT) : null;
        if (restaurant == null) {
            dismiss();
            return;
        }
        TextView title = view.findViewById(R.id.marker_title);
        TextView meta = view.findViewById(R.id.marker_meta);
        TextView address = view.findViewById(R.id.marker_address);
        Button navigate = view.findViewById(R.id.marker_navigate);

        title.setText(restaurant.getName());
        address.setText(restaurant.getAddress());

        StringBuilder sb = new StringBuilder();
        if (restaurant.getRating() != null) {
            sb.append(String.format("%.1f ★", restaurant.getRating()));
        }
        if (restaurant.getOpenNow() != null) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append(restaurant.getOpenNow() ? getString(R.string.meta_open_now) : getString(R.string.meta_closed));
        }
        meta.setText(sb.toString());
        meta.setVisibility(sb.length() > 0 ? View.VISIBLE : View.GONE);

        navigate.setOnClickListener(v -> openMaps(restaurant));
    }

    private void openMaps(Restaurant restaurant) {
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
            } catch (Exception ex) {
                if (getContext() != null) {
                    android.widget.Toast.makeText(getContext(), getString(R.string.marker_directions_error), android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
