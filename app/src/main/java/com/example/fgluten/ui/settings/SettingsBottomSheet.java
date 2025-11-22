package com.example.fgluten.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.fgluten.R;
import com.example.fgluten.util.SettingsManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Bottom sheet dialog for managing application settings.
 * 
 * This fragment provides a modal interface for users to configure application-wide
 * preferences including:
 * 
 * **Theme Settings:**
 * - Light theme mode
 * - Dark theme mode
 * - System default (follows device setting)
 * 
 * **Unit Preferences:**
 * - Distance measurement in kilometers/meters (metric)
 * - Distance measurement in miles/feet (imperial)
 * 
 * **User Experience:**
 * - Modal bottom sheet design for focused settings access
 * - Immediate application of theme changes
 * - Persistent storage via SettingsManager
 * - Intuitive radio button selection interface
 * 
 * The dialog integrates with SettingsManager for seamless preference management
 * and provides immediate visual feedback when settings are changed.
 * 
 * @see SettingsManager for underlying preference storage
 * @see AppCompatDelegate for theme management
 * 
 * @author FGluten Development Team
 */
public class SettingsBottomSheet extends BottomSheetDialogFragment {

    public static SettingsBottomSheet newInstance() {
        return new SettingsBottomSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RadioGroup themeGroup = view.findViewById(R.id.theme_group);
        RadioGroup unitsGroup = view.findViewById(R.id.units_group);
        Button closeButton = view.findViewById(R.id.close_button);

        int currentMode = SettingsManager.getThemeMode(requireContext());
        switch (currentMode) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                ((RadioButton) view.findViewById(R.id.theme_light)).setChecked(true);
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                ((RadioButton) view.findViewById(R.id.theme_dark)).setChecked(true);
                break;
            default:
                ((RadioButton) view.findViewById(R.id.theme_system)).setChecked(true);
                break;
        }

        boolean useMiles = SettingsManager.useMiles(requireContext());
        ((RadioButton) view.findViewById(useMiles ? R.id.unit_miles : R.id.unit_km)).setChecked(true);

        themeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            if (checkedId == R.id.theme_light) {
                mode = AppCompatDelegate.MODE_NIGHT_NO;
            } else if (checkedId == R.id.theme_dark) {
                mode = AppCompatDelegate.MODE_NIGHT_YES;
            }
            SettingsManager.setThemeMode(requireContext(), mode);
        });

        unitsGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean miles = checkedId == R.id.unit_miles;
            SettingsManager.setUseMiles(requireContext(), miles);
        });

        closeButton.setOnClickListener(v -> dismiss());
    }
}
