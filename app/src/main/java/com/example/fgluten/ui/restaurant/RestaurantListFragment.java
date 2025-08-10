package com.example.fgluten.ui.restaurant;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fgluten.R;
import com.example.fgluten.data.Restaurant;
import com.example.fgluten.databinding.FragmentRestaurantListBinding;

import java.util.ArrayList;

public class RestaurantListFragment extends Fragment {

    private FragmentRestaurantListBinding binding;
    private RestaurantAdapter adapter;
    private RestaurantViewModel restaurantViewModel;

    @Override

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_restaurant_list, container, false);
        RecyclerView recyclerView = root.findViewById(R.id.restaurant_recycler);

        adapter = new RestaurantAdapter(new ArrayList<>(), restaurant -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("restaurant", restaurant);
            NavHostFragment.findNavController(RestaurantListFragment.this)
                    .navigate(R.id.action_restaurantListFragment_to_restaurantDetailFragment, bundle);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        restaurantViewModel = new ViewModelProvider(this).get(RestaurantViewModel.class);
        restaurantViewModel.getRestaurants().observe(getViewLifecycleOwner(), adapter::setRestaurants);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}