package com.example.contact_tracing.models;

import java.util.List;

public class AnonymousUser {
    public String id;
    public List<LocationTimestamp> locationTimestamps;
    public Boolean atRisk;
    public Long latestTimeProcessed;
    public Integer numContacts;
    public Integer numUniqueContacts;
    public List<String> seenContacts;

    public AnonymousUser(String id, List<LocationTimestamp> locationTimestamps, Boolean atRisk, Long latestTimeProcessed,
                         Integer numContacts, Integer numUniqueContacts, List<String> seenContacts) {
        this.id = id;
        this.locationTimestamps = locationTimestamps;
        this.atRisk = atRisk;
        this.latestTimeProcessed = latestTimeProcessed;
        this.numContacts = numContacts;
        this.numUniqueContacts = numUniqueContacts;
        this.seenContacts = seenContacts;
    }
}
