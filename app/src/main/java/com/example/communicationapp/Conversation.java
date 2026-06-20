package com.example.communicationapp;

public class Conversation {
    private String threadId;
    private String address;
    private String contactName;
    private String lastMessage;
    private long date;

    public Conversation(String threadId, String address, String contactName, String lastMessage, long date) {
        this.threadId = threadId;
        this.address = address;
        this.contactName = contactName;
        this.lastMessage = lastMessage;
        this.date = date;
    }

    public String getThreadId() { return threadId; }
    public String getAddress() { return address; }
    public String getContactName() { return contactName != null ? contactName : address; }
    public String getLastMessage() { return lastMessage; }
    public long getDate() { return date; }
}