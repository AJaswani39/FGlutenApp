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

        String missingData = getString(R.string.missing_data);

        String name = null;
        String address = null;
        String gfStatus = null;
        List<String> menu = null;


        Bundle args = getArguments();
        if (args != null) {
            Restaurant restaurant = (Restaurant) args.getSerializable("restaurant");
            if (restaurant != null) {
                name = restaurant.getName();
                address = restaurant.getAddress();
                gfStatus = restaurant.determineIfGlutenFree();
                menu = restaurant.getGlutenFreeMenu();
            }
        }

        nameView.setText(name != null ? name : missingData);
        addressView.setText(address != null ? address : missingData);
        gfStatusView.setText(gfStatus != null ? gfStatus : missingData);
        menuView.setText(menu != null ? TextUtils.join("\n", menu) : missingData);

        return root;
    }
}
