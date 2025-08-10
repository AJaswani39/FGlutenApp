package com.example.fgluten.ui.restaurant;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.fgluten.data.Restaurant;
import com.example.fgluten.data.RestaurantRepository;

import java.util.List;

public class RestaurantViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Restaurant>> restaurants;

    public RestaurantViewModel(@NonNull Application application) {
        super(application);
        restaurants = new MutableLiveData<>();
        RestaurantRepository repository = new RestaurantRepository();
        restaurants.setValue(repository.getRestaurants());
    }

    public LiveData<List<Restaurant>> getRestaurants() {
        return restaurants;
    }
}
