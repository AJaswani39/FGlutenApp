package com.example.fgluten.ui.slideshow;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.fgluten.R;

public class SlideshowViewModel extends AndroidViewModel {

    private final MutableLiveData<String> mText;

    public SlideshowViewModel(@NonNull Application application) {
        super(application);
        mText = new MutableLiveData<>();
        mText.setValue(getApplication().getString(R.string.slideshow_fragment_text));
    }

    public LiveData<String> getText() {
        return mText;
    }
}