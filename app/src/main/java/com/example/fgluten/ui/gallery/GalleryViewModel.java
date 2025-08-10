package com.example.fgluten.ui.gallery;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.fgluten.R;

public class GalleryViewModel extends AndroidViewModel {

    private final MutableLiveData<String> mText;

    public GalleryViewModel(@NonNull Application application) {
        super(application);
        mText = new MutableLiveData<>();
        mText.setValue(getApplication().getString(R.string.gallery_fragment_text));
    }

    public LiveData<String> getText() {
        return mText;
    }
}