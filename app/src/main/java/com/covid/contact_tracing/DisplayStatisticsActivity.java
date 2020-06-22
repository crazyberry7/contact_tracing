package com.covid.contact_tracing;

import android.Manifest;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertisingSet;
import android.bluetooth.le.AdvertisingSetCallback;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.navigation.ui.AppBarConfiguration;

import com.covid.contact_tracing.services.ComputationsService;
import com.covid.contact_tracing.services.ForegroundLocationService;
import com.covid.contact_tracing.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

public class DisplayStatisticsActivity extends AppCompatActivity {
    public static final String BROADCAST_ACTION = "com.example.contact_tracing";
    static final int DISPLAY_SURVEY_ACTIVITY = 5;
    static final int REQUEST_FINE_LOCATION = 3;
    static final int REQUEST_BACKGROUND_LOCATION = 4;
    static final int REQUEST_BOTH_LOCATIONS = 6;
    static final int REQUEST_ENABLE_BT = 99;
    private static final String LOG_TAG = "BLE";
    private BroadcastReceiver myBroadCastReceiver;
    private ProgressBar atRiskHealthBar;
    private TextView atRiskPercentage;
    private TextView atRiskBoolean;
    private DatabaseReference mDatabase;
    private String appId;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private Application mApplication;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_statistics);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        atRiskHealthBar = findViewById(R.id.atRiskHealthBar);
        atRiskPercentage = findViewById(R.id.atRiskPercentage);
        atRiskBoolean = findViewById(R.id.atRiskBoolean);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        mApplication = this.getApplication();
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();

        // Set Toolbar
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        boolean permissionAccessBothLocationApproved =
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (permissionAccessBothLocationApproved) {
            //signInAnonymously();
            // Start Computations Service
            startComputationsService();
            // START foreground location data collection (using threading)
            final Intent foregroundLocationIntent = new Intent(mApplication, ForegroundLocationService.class);
            startForegroundService(foregroundLocationIntent);
            // Instantiate broadcast receiver
            myBroadCastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    // TO:DO
                    // populate the main contact tracer page
                    Integer numContacts = intent.getIntExtra("numContacts", 0);
                    Integer numUniqueContacts = intent.getIntExtra("numUniqueContacts", 0);
                    appId = intent.getStringExtra("appId");
                    TextView contacts = findViewById(R.id.contacts_value);
                    TextView uniqueContacts = findViewById(R.id.unique_contacts_value);
                    contacts.setText(String.format(Locale.getDefault(), "%d", numContacts));
                    uniqueContacts.setText(String.format(Locale.getDefault(), "%d", numUniqueContacts));
                    System.out.println("broadcast received from computations service");
                    getAtRiskPercentage(false, -1);


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
    public void onStart() {
        super.onStart();
    }


    @Override
    protected void onResume() {
        super.onResume();
        // START foreground location data collection (using threading)
        //final Intent foregroundLocationIntent = new Intent(this.getApplication(), ForegroundLocationService.class);
        //startService(foregroundLocationIntent);
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
            startForegroundService(initialComputationServiceIntent);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void startBluetooth() {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        BluetoothLeAdvertiser bleAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        AdvertisingSetParameters parameters = (new AdvertisingSetParameters.Builder())
                .setLegacyMode(true) // True by default, but set here as a reminder.
                .setConnectable(true)
                .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
                .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_ULTRA_LOW)
                .build();
        AdvertiseData data = (new AdvertiseData.Builder()).addServiceData(ParcelUuid.fromString(appId), appId.getBytes()).build();
        AdvertisingSetCallback callback = new AdvertisingSetCallback() {
            @Override
            public void onAdvertisingSetStarted(AdvertisingSet advertisingSet, int txPower, int status) {
                Log.i(LOG_TAG, "onAdvertisingSetStarted(): txPower:" + txPower + " , status: "
                        + status);
                AdvertisingSet currentAdvertisingSet = advertisingSet;
            }

            @Override
            public void onAdvertisingDataSet(AdvertisingSet advertisingSet, int status) {
                Log.i(LOG_TAG, "onAdvertisingDataSet() :status:" + status);
            }

            @Override
            public void onScanResponseDataSet(AdvertisingSet advertisingSet, int status) {
                Log.i(LOG_TAG, "onScanResponseDataSet(): status:" + status);
            }

            @Override
            public void onAdvertisingSetStopped(AdvertisingSet advertisingSet) {
                Log.i(LOG_TAG, "onAdvertisingSetStopped():");
            }
        };

        bleAdvertiser.startAdvertisingSet(parameters, data, null, null, null, callback);
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
        startActivityForResult(intent, DISPLAY_SURVEY_ACTIVITY);
    }

    /**
     * Gather results from DisplaySurveyActivity
     */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == DISPLAY_SURVEY_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                int surveyRiskPercentage = data.getIntExtra("surveyRiskPercentage", -1);
                /**

                 // Update atRiskPercentage in Firebase
                 mDatabase.child("Users").child(appId).child("riskPercentage").setValue(atRisk);
                 // Update atRisk Boolean in Firebase
                 updateUserRiskAttr(atRisk);
                 updateRiskBarUI(atRisk);
                 **/
                getAtRiskPercentage(true, surveyRiskPercentage);
            }
        }
    }

    private void updateUserRiskAttr(int riskPercentage) {
        DatabaseReference ref = mDatabase.child("Users").child(appId).child("atRisk");

        if (riskPercentage >= 60) {
            ref.setValue(true);
            atRiskBoolean.setText("Yep");
        } else if (riskPercentage >= 30) {
            ref.setValue(true);
            atRiskBoolean.setText("Kinda");
        } else {
            ref.setValue(false);
            atRiskBoolean.setText("Nah");
        }
    }

    private void updateRiskBarUI(int riskPercentage) {
        atRiskHealthBar.setProgress(riskPercentage);
        atRiskPercentage.setText(String.format(Locale.getDefault(), "%d%%", riskPercentage));
    }

    private void updateUserRisk(int riskPercentage) {
        mDatabase.child("Users").child(appId).child("riskPercentage").setValue(riskPercentage);
    }

    private void getAtRiskPercentage(boolean fromSurveyResult, int surveyAtRiskPercentage) {
        Query q = mDatabase.child("Users").child(appId);
        q.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Object obj = dataSnapshot.getValue();
                    HashMap<String, Object> map = (HashMap<String, Object>) obj;
                    int riskPercentage;
                    if (fromSurveyResult && surveyAtRiskPercentage > -1) {
                        riskPercentage = surveyAtRiskPercentage;
                    } else {
                        riskPercentage = (int) (long) Objects.requireNonNull(map).get("riskPercentage");
                    }

                    updateUserRisk(riskPercentage);
                    updateRiskBarUI(riskPercentage);
                    updateUserRiskAttr(riskPercentage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void signInAnonymously() {
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("Firebase Authentication", "signInAnonymously:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            // Start Computations Service
                            startComputationsService();
                            // START foreground location data collection (using threading)
                            final Intent foregroundLocationIntent = new Intent(mApplication, ForegroundLocationService.class);
                            startService(foregroundLocationIntent);
                            // Instantiate broadcast receiver
                            myBroadCastReceiver = new BroadcastReceiver() {
                                @Override
                                public void onReceive(Context context, Intent intent) {
                                    // TO:DO
                                    // populate the main contact tracer page
                                    Integer numContacts = intent.getIntExtra("numContacts", 0);
                                    Integer numUniqueContacts = intent.getIntExtra("numUniqueContacts", 0);
                                    appId = intent.getStringExtra("appId");
                                    TextView contacts = findViewById(R.id.contacts_value);
                                    TextView uniqueContacts = findViewById(R.id.unique_contacts_value);
                                    contacts.setText(String.format(Locale.getDefault(), "%d", numContacts));
                                    uniqueContacts.setText(String.format(Locale.getDefault(), "%d", numUniqueContacts));
                                    System.out.println("broadcast received from computations service");
                                    getAtRiskPercentage(false, -1);


                                }
                            };
                            registerMyReceiver();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("Firebase Authentication", "signInAnonymously:failure", task.getException());
                            Toast.makeText(DisplayStatisticsActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

}
