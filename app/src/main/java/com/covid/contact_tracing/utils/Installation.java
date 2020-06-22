package com.covid.contact_tracing.utils;

import android.content.Context;
import android.util.Log;

import com.covid.contact_tracing.models.AnonymousUser;
import com.covid.contact_tracing.models.LocationTimestamp;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class Installation {
    private static String sID = null;
    private static final String INSTALLATION = "INSTALLATION";
    private DatabaseReference mDatabase;
    private String locationTimestampId;
    public synchronized String id(Context context, DatabaseReference database) {
        if (sID == null) {
            mDatabase = database;
            File installation = new File(context.getFilesDir(), INSTALLATION);
            installation.delete();
            try {
                if (!installation.exists()) {
                    writeInstallationFile(installation);
                } else {
                    sID = readInstallationFile(installation);
                }

                /**
                 //*  Location Update to Firebase *
                 String key = mDatabase.child(sID).push().getKey();
                 Map<String, Object> hopperUpdates = new HashMap<>();
                 LocationTimestamp locationTimestamp = new LocationTimestamp(35.444, 35.566, Instant.now().getEpochSecond());
                 hopperUpdates.put(key, locationTimestamp);
                 mDatabase.child(sID).child("locationUpdates").updateChildren(hopperUpdates);
                 **/
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return sID;
    }
    public String getAppId(File installation) throws IOException {
        RandomAccessFile f = new RandomAccessFile(installation, "r");
        byte[] bytes = new byte[(int) f.length()];
        f.readFully(bytes);
        f.close();
        return new String(bytes);
    }
    private String readInstallationFile(File installation) throws IOException {
        //installation.delete();
        String appId = getAppId(installation);
        // Store initial LocationTimestamp in Firebase
        LocationTimestamp locationTimestamp = new LocationTimestamp(appId, 0.0, 0.0, Instant.now().getEpochSecond());
        DatabaseReference ref = mDatabase.child("LocationTimestamps").child(appId).push();
        ref.setValue(locationTimestamp);
        locationTimestampId = ref.getKey();
        // Return appId
        return appId;
    }

    private void writeInstallationFile(File installation) throws IOException {
        FileOutputStream out = new FileOutputStream(installation);
        sID = UUID.randomUUID().toString();
        // Write new user entry to Firebase
        AnonymousUser newUser = new AnonymousUser(sID, new ArrayList<LocationTimestamp>(), false, 0,
                Instant.now().getEpochSecond(), 0, 0, new ArrayList<String>());
        DatabaseReference writeUserRef = mDatabase.child("Users").child(sID);
        writeUserRef.removeValue();
        writeUserRef.setValue(newUser, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference reference) {
                if (databaseError != null) {
                    Log.e("Firebase Operation", "Failed to write message", databaseError.toException());
                }
            }
        });
        // Write first location timestamp to Firebase
        LocationTimestamp locationTimestamp = new LocationTimestamp( sID, 0.0, 0.0, Instant.now().getEpochSecond());
        DatabaseReference ref = mDatabase.child("LocationTimestamps").child(sID).push();
        ref.setValue(locationTimestamp);
        locationTimestampId = ref.getKey();
        // Instantiate Seen Contacts in Firebase
        mDatabase.child("SeenContacts").child(sID).setValue(new ArrayList<String>(Arrays.asList(sID)));
        out.write(sID.getBytes());
        out.close();
    }
    public String returnFirstLocationTimestampId() {
        return locationTimestampId;
    }
}
