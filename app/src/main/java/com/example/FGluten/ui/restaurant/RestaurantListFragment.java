package com.example.FGluten.ui.restaurant;

import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;


import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.FGluten.R;
import com.example.FGluten.databinding.FragmentRestaurantListBinding;

import java.util.ArrayList;

public class RestaurantListFragment extends Fragment {

    private FragmentRestaurantListBinding binding;
    private RestaurantAdapter adapter;

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
            Bundle bundle = new Bundle();
            bundle.putSerializable("restaurant", restaurant);
            NavHostFragment.findNavController(RestaurantListFragment.this)
                    .navigate(R.id.action_restaurantListFragment_to_restaurantDetailFragment, bundle);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}