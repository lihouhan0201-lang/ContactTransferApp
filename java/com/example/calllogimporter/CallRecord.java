package com.example.calllogimporter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CallRecord {
    private String name;
    private String phoneNumber;
    private String type; // INCOMING, OUTGOING, MISSED
    private long timestamp;
    private int duration; // 秒

    public CallRecord(String name, String phoneNumber, String type, long timestamp, int duration) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.type = type;
        this.timestamp = timestamp;
        this.duration = duration;
    }

    // Getters
    public String getName() { return name; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getType() { return type; }
    public long getTimestamp() { return timestamp; }
    public int getDuration() { return duration; }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String displayName = (name != null && !name.trim().isEmpty()) ? name : phoneNumber;
        return String.format(Locale.getDefault(), "%s - %s (%s) %s",
                displayName, phoneNumber, type, sdf.format(new Date(timestamp)));
    }
}