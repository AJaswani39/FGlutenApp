package com.example.fgluten.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fgluten.databinding.FragmentHomeBinding;
import com.example.fgluten.R;
import com.example.fgluten.data.Restaurant;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private CachedAdapter cachedAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.ctaFindRestaurants.setOnClickListener(v ->
                NavHostFragment.findNavController(HomeFragment.this)
                        .navigate(R.id.nav_restaurant_list));

        cachedAdapter = new CachedAdapter();
        RecyclerView recyclerView = binding.cachedRecycler;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(cachedAdapter);

        homeViewModel.getText().observe(getViewLifecycleOwner(), title -> {
            binding.homeTitle.setText(getString(R.string.home_title));
            binding.homeSubtitle.setText(getString(R.string.home_subtitle));
        });

        homeViewModel.getCachedRestaurants().observe(getViewLifecycleOwner(), restaurants -> {
            boolean hasData = restaurants != null && !restaurants.isEmpty();
            binding.lastNearbyCard.setVisibility(hasData ? View.VISIBLE : View.GONE);
            cachedAdapter.setRestaurants(restaurants);
        });

        homeViewModel.isPermissionGranted().observe(getViewLifecycleOwner(), granted -> {
            boolean showBanner = granted != null && !granted;
            binding.permissionBanner.setVisibility(showBanner ? View.VISIBLE : View.GONE);
            binding.permissionButton.setOnClickListener(v -> {
                NavHostFragment.findNavController(HomeFragment.this)
                        .navigate(R.id.nav_restaurant_list);
            });
        });
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class CachedAdapter extends RecyclerView.Adapter<CachedAdapter.CachedViewHolder> {
        private List<Restaurant> restaurants = new ArrayList<>();

        void setRestaurants(List<Restaurant> data) {
            restaurants = data != null ? data : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public CachedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_cached_restaurant, parent, false);
            return new CachedViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CachedViewHolder holder, int position) {
            holder.bind(restaurants.get(position));
        }

        @Override
        public int getItemCount() {
            return restaurants.size();
        }

        static class CachedViewHolder extends RecyclerView.ViewHolder {
            private final TextView name;
            private final TextView meta;

            CachedViewHolder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.cached_name);
                meta = itemView.findViewById(R.id.cached_meta);
            }

            void bind(Restaurant restaurant) {
                name.setText(restaurant.getName());
                StringBuilder sb = new StringBuilder();
                if (restaurant.hasGlutenFreeOptions()) {
                    sb.append(itemView.getContext().getString(R.string.gluten_free_badge));
                }
                double dist = restaurant.getDistanceMeters();
                if (dist > 0) {
                    if (sb.length() > 0) sb.append(" \u2022 ");
                    if (dist >= 1000) {
                        sb.append(String.format("%.1f km", dist / 1000.0));
                    } else {
                        sb.append(String.format("%d m", Math.round(dist)));
                    }
                }
                meta.setText(sb.toString());
            }
        }
    }
}
