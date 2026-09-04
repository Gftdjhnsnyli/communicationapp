package com.example.communicationapp.data;

public final class SmsState {
    public static final int SEND_NONE = 0;
    public static final int SEND_PENDING = 1;
    public static final int SEND_SENT = 2;
    public static final int SEND_FAILED = 3;

    public static final int DELIVERY_NONE = 0;
    public static final int DELIVERY_PENDING = 1;
    public static final int DELIVERY_COMPLETE = 2;
    public static final int DELIVERY_FAILED = 3;

    public static final int PART_PENDING = 0;
    public static final int PART_COMPLETE = 1;
    public static final int PART_FAILED = 2;

    public static final int DISPATCH_NONE = 0;
    public static final int DISPATCH_QUEUED = 1;
    public static final int DISPATCH_HANDOFF_STARTED = 2;
    public static final int DISPATCH_HANDED_OFF = 3;
    public static final int DISPATCH_FAILED = 4;

    private SmsState() {}
}
