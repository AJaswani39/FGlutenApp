package com.example.fgluten;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.NavOptions;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.core.view.GravityCompat;

import com.example.fgluten.databinding.ActivityMainBinding;
import com.example.fgluten.ui.settings.SettingsBottomSheet;
import com.example.fgluten.util.SettingsManager;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply saved theme before inflating
        SettingsManager.setThemeMode(this, SettingsManager.getThemeMode(this));
        super.onCreate(savedInstanceState);

        com.example.fgluten.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        super.setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_restaurant_list)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        navigationView.setNavigationItemSelectedListener(item -> {
            boolean handled = false;
            NavOptions options = new NavOptions.Builder()
                    .setPopUpTo(R.id.mobile_navigation, true)
                    .build();
            if (item.getItemId() == R.id.nav_home || item.getItemId() == R.id.nav_restaurant_list) {
                navController.navigate(item.getItemId(), null, options);
                handled = true;
            }
            if (!handled) {
                handled = NavigationUI.onNavDestinationSelected(item, navController);
            }
            if (handled) {
                drawer.closeDrawer(GravityCompat.START);
                item.setChecked(true);
            }
            return handled;
        });
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            SettingsBottomSheet.newInstance().show(getSupportFragmentManager(), "settings_bottom_sheet");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
