package com.example.calllogimporter;

public class Contact {
    private String name;
    private String phoneNumber;
    private long contactId;

    public Contact(String name, String phoneNumber, long contactId) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.contactId = contactId;
    }

    // Getters
    public String getName() { return name; }
    public String getPhoneNumber() { return phoneNumber; }
    public long getContactId() { return contactId; }

    @Override
    public String toString() {
        return name + " - " + phoneNumber;
    }
}