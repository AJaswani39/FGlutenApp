package com.example.fgluten.ui.gallery;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.fgluten.R;

/**
 * ViewModel for the GalleryFragment content management.
 * 
 * This ViewModel manages the data and business logic for the gallery section:
 * 
 * **Core Responsibilities:**
 * - Text content management for the gallery interface
 * - Resource string management for internationalization support
 * - LiveData provisioning for reactive UI updates
 * 
 * **Current Implementation:**
 * - Simple text content management
 * - Resource-based string provisioning
 * - LiveData pattern for reactive updates
 * 
 * **Architecture:**
 * - Extends AndroidViewModel for application-wide state management
 * - Uses LiveData for reactive programming patterns
 * - Follows MVVM architecture for clear separation of concerns
 * - Integrates with Android resources for text content
 * 
 * **Design Pattern:**
 * - Demonstrates proper ViewModel implementation
 * - Shows LiveData usage for reactive programming
 * - Follows Android Jetpack architecture guidelines
 * 
 * **Future Enhancement Potential:**
 * - Could be extended to handle image gallery data
 * - May integrate with photo storage or APIs
 * - Potential for media content management
 * 
 * The ViewModel provides a clean abstraction layer between the UI (GalleryFragment)
 * and the data/content management, following Android best practices for
 * lifecycle-aware component architecture.
 * 
 * @see GalleryFragment for UI layer integration
 * 
 * @author FGluten Development Team
 */
public class GalleryViewModel extends AndroidViewModel {

    // ========== REACTIVE STATE MANAGEMENT ==========
    
    /** LiveData for text content displayed in the gallery */
    private final MutableLiveData<String> mText;

    /**
     * Constructor that initializes ViewModel state and content.
     * 
     * This constructor:
     * 1. Calls super constructor for AndroidViewModel initialization
     * 2. Initializes the LiveData for text content
     * 3. Sets up default content from application resources
     * 
     * The text content is loaded from string resources to support
     * internationalization and easy content management.
     * 
     * @param application Android Application context for ViewModel initialization
     */
    public GalleryViewModel(@NonNull Application application) {
        super(application);
        
        // ========== INITIALIZE TEXT CONTENT ==========
        mText = new MutableLiveData<>();
        // Load text content from string resources
        // This allows for easy localization and content management
        mText.setValue(getApplication().getString(R.string.gallery_fragment_text));
    }

    /**
     * Provides LiveData for text content display.
     * 
     * This method returns a LiveData object that can be observed by the UI
     * for changes to the gallery text content. Currently, it provides static
     * content from resources, but could be extended to support dynamic content.
     * 
     * The returned LiveData follows Android lifecycle-aware patterns and will
     * automatically be cleaned up when the associated fragment is destroyed.
     * 
     * @return LiveData containing text content for the gallery fragment
     */
    public LiveData<String> getText() {
        return mText;
    }
}