package com.example.FGluten.ui.restaurant;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.FGluten.R;
import com.example.FGluten.data.Restaurant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RestaurantListFragment extends Fragment {

    private final List<Restaurant> restaurants = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_restaurant_list, container, false);
        ListView listView = root.findViewById(R.id.restaurant_list_view);

        // Sample data
        restaurants.clear();
        restaurants.add(new Restaurant("Cafe Good", "123 Main St", true,
                Arrays.asList("GF Burger", "Salad"), 0.0, 0.0));
        restaurants.add(new Restaurant("Pizza Place", "456 Elm St", false,
                new ArrayList<>(), 0.0, 0.0));

        List<String> names = new ArrayList<>();
        for (Restaurant r : restaurants) {
            names.add(r.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, names);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Restaurant selected = restaurants.get(position);
            Bundle bundle = new Bundle();
            bundle.putSerializable("restaurant", selected);
            Navigation.findNavController(view).navigate(R.id.action_restaurantListFragment_to_restaurantDetailFragment, bundle);
        });

        return root;
    }
}
