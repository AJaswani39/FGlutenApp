package com.example.FGluten.ui.restaurant;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.FGluten.R;
import com.example.FGluten.data.Restaurant;

import java.util.ArrayList;
import java.util.List;

public class RestaurantListFragment extends Fragment {

    private final List<Restaurant> restaurants = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_restaurant_list, container, false);
        RecyclerView recyclerView = root.findViewById(R.id.restaurant_recycler);
        // Sample data
        restaurants.clear();
        restaurants.add(new Restaurant("Cafe Good", "123 Main St", true,
                Arrays.asList("GF Burger", "Salad"), 0.0, 0.0));
        restaurants.add(new Restaurant("Pizza Place", "456 Elm St", false,
                new ArrayList<>(), 0.0, 0.0));

        RestaurantAdapter adapter = new RestaurantAdapter(restaurants, restaurant -> {

        RestaurantViewModel viewModel = new ViewModelProvider(this).get(RestaurantViewModel.class);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, new ArrayList<>());
        listView.setAdapter(adapter);

        viewModel.getRestaurants().observe(getViewLifecycleOwner(), list -> {
            restaurants.clear();
            restaurants.addAll(list);

            List<String> names = new ArrayList<>();
            for (Restaurant r : restaurants) {
                names.add(r.getName());
            }
            adapter.clear();
            adapter.addAll(names);
            adapter.notifyDataSetChanged();
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Restaurant selected = restaurants.get(position);

            Bundle bundle = new Bundle();
            bundle.putSerializable("restaurant", restaurant);
            NavHostFragment.findNavController(RestaurantListFragment.this)
                    .navigate(R.id.action_restaurantListFragment_to_restaurantDetailFragment, bundle);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        return root;
    }
}
