package com.example.contact_tracing.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;

import com.example.contact_tracing.utils.Installation;
import com.example.contact_tracing.models.LocationTimestamp;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ForegroundLocationService extends Service {
    private Location mCurrentLocation;
    private static final int FOREGROUND_LOCATION_SERVICE = 911;
    private Context context;
    private Thread workerThread;
    public ForegroundLocationService() {
        this.context = this;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        /**workerThread = new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                //getMainLooper().prepare();
                // Start foreground service
                startForeground(FOREGROUND_LOCATION_SERVICE, getNotification());
                boolean running = true;
                while (running) {
                    // Collect location and store in Firebase here

                    if (Thread.interrupted()) {
                        return;
                    }
                }

            }
        });
         **/

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        // Create Thread Pool
        ExecutorService mThreadPool = Executors.newSingleThreadExecutor();
        // Create foreground notification banner
        startForeground(FOREGROUND_LOCATION_SERVICE, getNotification());
        // Run grab location code
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(5000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationCallback mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                // Create new thread for callback
                mThreadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        mCurrentLocation = locationResult.getLastLocation();
                        //update user lat and lon
                        double lat = mCurrentLocation.getLatitude();
                        double lon = mCurrentLocation.getLongitude();
                        File installationPath = new File(context.getFilesDir(), "INSTALLATION");
                        Installation installation = new Installation();
                        try {
                            String appId = installation.getAppId(installationPath);
                            LocationTimestamp userLocationTimestamp = new LocationTimestamp(appId, lat, lon, Instant.now().getEpochSecond());
                            DatabaseReference ref = mDatabase.child("LocationTimestamps").child(appId).push();
                            ref.setValue(userLocationTimestamp);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });

            }
        };
        mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());
        return START_NOT_STICKY;
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private Notification getNotification() {
        NotificationChannel channel = new NotificationChannel("channel_01", "My Channel", NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        assert notificationManager != null;
        notificationManager.createNotificationChannel(channel);
        Notification.Builder builder = new Notification.Builder(getApplicationContext(), "channel_01").setAutoCancel(true);
        return builder.build();
    }
}
