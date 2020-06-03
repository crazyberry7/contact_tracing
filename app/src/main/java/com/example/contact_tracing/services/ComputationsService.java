package com.example.contact_tracing.services;

import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.example.contact_tracing.DisplayStatisticsActivity;
import com.example.contact_tracing.utils.ComputeNumContacts;
import com.example.contact_tracing.utils.Installation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class ComputationsService extends Service {
    private DatabaseReference mDatabase;

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private Location mCurrentLocation;
    private String appId;
    private static final String TAG = "ComputationsService";
    private Installation installation;
    private Context context;
    @Override
    public IBinder onBind(Intent intent) {
        // We don't want to bind; return null.
        return null;
    }


    public void onCreate() {
        Log.d(TAG, "Computations Service started.");
        context = this;
        mDatabase = FirebaseDatabase.getInstance().getReference();
        retrieveLocationAndComputeContacts();
        installation = new Installation();
        appId = installation.id(this, mDatabase);

    }
    public void retrieveLocationAndComputeContacts() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        LocationRequest locationRequest = LocationRequest.create()
                .setNumUpdates(1)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                mCurrentLocation = locationResult.getLastLocation();
                //update user lat and lon
                Double lat = mCurrentLocation.getLatitude();
                Double lon = mCurrentLocation.getLongitude();

                DatabaseReference ref = mDatabase.child("LocationTimestamps").child(appId).child(installation.returnFirstLocationTimestampId());
                ref.child("lat").setValue(lat);
                ref.child("lon").setValue(lon);


                ComputeNumContacts computeNumContacts = new ComputeNumContacts(appId, mDatabase, context);
                computeNumContacts.calcNumContacts();
                //int numUniqueContacts = computeNumContacts.getNumUniqueContacts();
                //int numContacts = computeNumContacts.getNumContacts();
                //sendMyBroadcast(numContacts, numUniqueContacts);

            }
        };
        mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());
    }

    public void sendMyBroadcast(int numContacts, int numUniqueContacts) {
        try {
            Intent broadCastIntent = new Intent();
            broadCastIntent.setAction(DisplayStatisticsActivity.BROADCAST_ACTION);
            // uncomment this line if you want to send data
            // TO:DO send data back
            // 1) Total Contacts
            // 2) # Unique Contacts
            broadCastIntent.putExtra("numContacts", numContacts);
            broadCastIntent.putExtra("numUniqueContacts", numUniqueContacts);
            sendBroadcast(broadCastIntent);
            //stopSelf();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
