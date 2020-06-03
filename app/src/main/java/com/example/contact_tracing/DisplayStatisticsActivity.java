package com.example.contact_tracing;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.contact_tracing.services.ComputationsService;
import com.example.contact_tracing.services.ForegroundLocationService;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.util.Locale;

public class DisplayStatisticsActivity extends AppCompatActivity {
    public static final String BROADCAST_ACTION = "com.example.contact_tracing";

    static final int REQUEST_FINE_LOCATION = 3;
    static final int REQUEST_BACKGROUND_LOCATION = 4;
    static final int REQUEST_BOTH_LOCATIONS = 5;
    BroadcastReceiver myBroadCastReceiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_statistics);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        // Set Toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        // Set at_risk health bar
        /**
        ProgressBar pb = (ProgressBar) findViewById(R.id.determinateBar);
        pb.getIndeterminateDrawable().setColorFilter(
                ContextCompat.getColor(this, R.color.colorPrimaryDark),
                android.graphics.PorterDuff.Mode.SRC_IN);
         **/
        boolean permissionAccessBothLocationApproved =
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (permissionAccessBothLocationApproved) {
            // Start Computations Service
            startComputationsService();
            // Instantiate broadcast receiver
            myBroadCastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    // TO:DO
                    // populate the main contact tracer page
                    Integer numContacts = (Integer) intent.getIntExtra("numContacts", 0);
                    Integer numUniqueContacts = (Integer) intent.getIntExtra("numUniqueContacts", 0);
                    TextView contacts = findViewById(R.id.contacts_value);
                    TextView uniqueContacts = findViewById(R.id.unique_contacts_value);
                    contacts.setText(String.format(Locale.getDefault(), "%d", numContacts));
                    uniqueContacts.setText(String.format(Locale.getDefault(), "%d", numUniqueContacts));
                    System.out.println("broadcast received from computations service");
                }
            };
            registerMyReceiver();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    }, REQUEST_BOTH_LOCATIONS
            );
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        // START foreground location data collection (using threading)
        final Intent foregroundLocationIntent = new Intent(this.getApplication(), ForegroundLocationService.class);
        startService(foregroundLocationIntent);
        // STOP background location data collection


    }
    @Override
    protected void onPause() {
        super.onPause();
        // STOP foreground location data collection
        // START background location data collection
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // make sure to unregister your receiver after finishing of this activity
        unregisterReceiver(myBroadCastReceiver);
        // STOP background location data collection
    }


    private void startComputationsService() {
        try {
            Intent initialComputationServiceIntent = new Intent(this, ComputationsService.class);
            startService(initialComputationServiceIntent);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    /**
     * This method is responsible to register an action to BroadCastReceiver
     */
    private void registerMyReceiver() {
        try {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BROADCAST_ACTION);
            this.registerReceiver(myBroadCastReceiver, intentFilter);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    /**
     * Start Survey Activity
     */
    public void startSurvey(View view) {
        Intent intent = new Intent(this, DisplaySurveyActivity.class);
        startActivity(intent);
    }
}
