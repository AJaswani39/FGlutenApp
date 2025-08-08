package com.example.FGluten.ui.restaurant;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.FGluten.R;
import com.example.FGluten.data.Restaurant;

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

        Bundle args = getArguments();
        if (args != null) {
            Restaurant restaurant = (Restaurant) args.getSerializable("restaurant");
            if (restaurant != null) {
                nameView.setText(restaurant.getName());
                addressView.setText(restaurant.getAddress());
                gfStatusView.setText(restaurant.determineIfGlutenFree());
                menuView.setText(TextUtils.join("\n", restaurant.getGlutenFreeMenu()));
            }
        }

        return root;
    }
}
