package com.example.communicationapp;

public class Message {
    private long id;
    private String address;
    private String body;
    private long date;
    private int type; // 1 for received, 2 for sent
    private int status; // -1 for none, 0 for complete, 32 for pending, etc.

    public Message(long id, String address, String body, long date, int type, int status) {
        this.id = id;
        this.address = address;
        this.body = body;
        this.date = date;
        this.type = type;
        this.status = status;
    }

    public long getId() { return id; }
    public String getAddress() { return address; }
    public String getBody() { return body; }
    public long getDate() { return date; }
    public int getType() { return type; }
    public int getStatus() { return status; }
}