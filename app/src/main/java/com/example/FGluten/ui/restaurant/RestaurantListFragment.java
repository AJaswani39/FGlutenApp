import android.view.ViewGroup;

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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        RestaurantViewModel viewModel = new ViewModelProvider(this).get(RestaurantViewModel.class);
        binding = FragmentRestaurantListBinding.inflate(inflater, container, false);

        adapter = new RestaurantAdapter(new ArrayList<>(), restaurant -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("restaurant", restaurant);
            NavHostFragment.findNavController(RestaurantListFragment.this)
                    .navigate(R.id.action_restaurantListFragment_to_restaurantDetailFragment, bundle);
        });
        binding.restaurantRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.restaurantRecyclerView.setAdapter(adapter);

        viewModel.getRestaurants().observe(getViewLifecycleOwner(), adapter::setRestaurants);

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}