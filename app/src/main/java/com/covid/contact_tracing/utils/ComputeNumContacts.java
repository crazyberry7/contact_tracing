package com.covid.contact_tracing.utils;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.covid.contact_tracing.DisplayStatisticsActivity;
import com.covid.contact_tracing.models.LocationTimestamp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

/**
 * appId - denotes individual's id
 * userId - denotes all other user ids
 * latestSync - latest time which individual location data has been cross-referenced
 * with all other users in a set time frame
 */
public class ComputeNumContacts {
    private String appId;
    private DatabaseReference mDatabase;
    private HashSet<String> seenContacts = new HashSet<>();
    private Long latestProcessedTime;
    private Context computationsServiceContext;
    // Variables to store data from Firebase
    private Long latestSync = null;
    private Integer numUniqueContacts;
    private Integer numContacts;
    private HashSet<String> keys = new HashSet<>();
    private ArrayList<LocationTimestamp> allLocationTimestamps = new ArrayList<>();
    private ArrayList<LocationTimestamp> userLocationTimestamps = new ArrayList<>();

    public ComputeNumContacts(String appId, DatabaseReference database, Context context) {
        this.appId = appId;
        this.mDatabase = database;
        this.computationsServiceContext = context;
    }

    /**
     * Calculates number of people in contact (within 6 feet, at the same time, down to second)
     */
    public void calcNumContacts() {
        getLatestSyncAndContactInfo();
    }

    /**
     * Helpers
     **/
    private void getLatestSyncAndContactInfo() {
        Query retrieveLatestTimestamp = mDatabase.child("Users").child(appId);
        // Retrieve latest sync time
        retrieveLatestTimestamp.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Object obj = dataSnapshot.getValue();
                HashMap<String, Object> map = (HashMap<String, Object>) obj;
                latestSync = (long) map.get("latestTimeProcessed");
                numUniqueContacts = (int) (long) map.get("numUniqueContacts");
                numContacts = (int) (long) map.get("numContacts");
                Long currentTime = Instant.now().getEpochSecond();

                getUserLocationTimestamps(latestSync, currentTime);
                getUserIds(currentTime);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * @return every other userId in Firebase that isn't ours
     */
    private void getUserIds(final Long currentTime) {
        Query q = mDatabase.child("Users");
        q.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Object obj = dataSnapshot.getValue();
                HashMap<String, Object> map = (HashMap<String, Object>) obj;
                for (String key : map.keySet()) {
                    if (!key.equals(appId)) {
                        keys.add(key);
                    }

                }
                getAllLocationTimestamps(currentTime);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void recursiveGetTimestamps(final Stack<String> keys, final Long currentTime) {
        if (!keys.isEmpty()) {
            String key = keys.pop();
            Query q = mDatabase.child("LocationTimestamps").child(key).orderByChild("timestamp");
            //.startAt(latestSync).endAt(currentTime);
            q.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                        Object obj = postSnapshot.getValue();
                        HashMap<String, Object> map = (HashMap<String, Object>) obj;
                        String key = (String) map.get("userId");
                        assert key != null;
                        if (!key.equals(appId)) {
                            Double lon = (Double) map.get("lon");
                            Double lat = (Double) map.get("lat");
                            Long timestamp = (Long) map.get("timestamp");
                            String userId = (String) map.get("userId");
                            LocationTimestamp res = new LocationTimestamp(userId, lat, lon, timestamp);
                            allLocationTimestamps.add(res);
                        }

                    }
                    recursiveGetTimestamps(keys, currentTime);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        } else {
            Collections.sort(allLocationTimestamps, new Comparator<LocationTimestamp>() {
                @Override
                public int compare(LocationTimestamp t1, LocationTimestamp t2) {
                    return t1.timestamp.compareTo(t2.timestamp);
                }
            });
            getSeenContacts(userLocationTimestamps, allLocationTimestamps, currentTime);
        }
    }

    private void getAllLocationTimestamps(Long currentTime) {
        Stack<String> stack = new Stack<>();
        stack.addAll(keys);
        recursiveGetTimestamps(stack, currentTime);
    }

    private void getUserLocationTimestamps(Long latestSync, Long currentTime) {
        Query q = mDatabase.child("LocationTimestamps").child(appId).orderByChild("timestamp").startAt(latestSync).endAt(currentTime);
        q.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    Object obj = postSnapshot.getValue();
                    HashMap<String, Object> map = (HashMap<String, Object>) obj;
                    Double lat = (Double) map.get("lat");
                    Double lon = (Double) map.get("lon");
                    Long timestamp = (Long) map.get("timestamp");
                    LocationTimestamp userLocationUpdate = new LocationTimestamp(appId, lat, lon, timestamp);
                    userLocationTimestamps.add(userLocationUpdate);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void getSeenContacts(final ArrayList<LocationTimestamp> userTimestamps, final ArrayList<LocationTimestamp> allTimestamps, final Long currTime) {
        Query q = mDatabase.child("SeenContacts").child(appId);
        q.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                        String seenContact = (String) postSnapshot.getValue();
                        seenContacts.add(seenContact);
                    }
                }
                processLocationTimestamps(userTimestamps, allTimestamps, currTime);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * Compare user's location data with every other user's data
     * Update user's metrics in Firebase via call to updateUserData()
     */
    private void processLocationTimestamps(final ArrayList<LocationTimestamp> userTimestamps, final ArrayList<LocationTimestamp> allTimestamps, final Long currTime) {
        // Update latest sync time
        latestProcessedTime = currTime;
        if (!allTimestamps.isEmpty()) {
            int index = 0;
            boolean reachedEnd = false;
            for (LocationTimestamp userLocationTimestamp : userTimestamps) {
                Long userTime = userLocationTimestamp.timestamp;
                Double lat1 = userLocationTimestamp.lat;
                Double lon1 = userLocationTimestamp.lon;
                LocationTimestamp otherUser = allTimestamps.get(index);
                Long otherUserTime = otherUser.timestamp;
                while (!userTime.equals(otherUserTime)) {
                    index += 1;
                    if (index >= allLocationTimestamps.size()) {
                        break;
                    }
                    otherUser = allLocationTimestamps.get(index);
                    otherUserTime = otherUser.timestamp;

                }
                if (index >= allLocationTimestamps.size()) {
                    break;
                }
                while (userTime.equals(otherUserTime)) {
                    Double lat2 = otherUser.lat;
                    Double lon2 = otherUser.lon;
                    // check if within 6 feet
                    double dist = distance(lat1, lon1, lat2, lon2);
                    if (dist <= 6.0d) {
                        numContacts += 1;
                        seenContacts.add(otherUser.userId);
                    }
                    index += 1;
                    otherUser = allLocationTimestamps.get(index);
                    otherUserTime = otherUser.timestamp;
                }
            }
        }
        try {
            numUniqueContacts = seenContacts.size();
            updateUserData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUserData() {
        // update number of total contacts, number of unique contacts
        // update latest time processed
        DatabaseReference ref = mDatabase.child("Users").child(appId);
        ref.child("latestTimeProcessed").setValue(latestProcessedTime);
        ref.child("numUniqueContacts").setValue(numUniqueContacts);
        ref.child("numContacts").setValue(numContacts);
        // update seen contacts list
        ArrayList<String> seenContactsArr = new ArrayList<>(seenContacts);
        mDatabase.child("SeenContacts").child(appId).setValue(seenContactsArr);
        // Send broadcast to MainActivity signalling service is finished
        sendBroadcastToMainActivity(numContacts, numUniqueContacts, computationsServiceContext);
    }

    private void sendBroadcastToMainActivity(int numContacts, int numUniqueContacts, Context context) {
        try {
            Intent broadCastIntent = new Intent();
            broadCastIntent.setAction(DisplayStatisticsActivity.BROADCAST_ACTION);
            broadCastIntent.putExtra("numContacts", numContacts);
            broadCastIntent.putExtra("numUniqueContacts", numUniqueContacts);
            broadCastIntent.putExtra("appId", appId);
            context.sendBroadcast(broadCastIntent);
            //stopSelf();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static double distance(double lat1, double lon1, double lat2, double lon2) {
        if ((lat1 == lat2) && (lon1 == lon2)) {
            return 0;
        } else {
            double theta = lon1 - lon2;
            double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
            dist = Math.acos(dist);
            dist = Math.toDegrees(dist);
            dist = dist * 60 * 1.1515;
            dist = dist * 1609.344;
            return (dist);
        }
    }
}
