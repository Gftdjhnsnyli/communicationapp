package com.example.communicationapp.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "messages",
        foreignKeys = @ForeignKey(
                entity = ConversationEntity.class,
                parentColumns = "conversationKey",
                childColumns = "conversationKey",
                onDelete = ForeignKey.CASCADE),
        indices = {
                @Index("conversationKey"),
                @Index(value = "systemSmsId", unique = true),
                @Index(value = "incomingFingerprint", unique = true)
        })
public class MessageEntity {
    @PrimaryKey
    @NonNull
    public String localId;
    public Long systemSmsId;
    @NonNull
    public String conversationKey;
    public Long systemThreadId;
    @NonNull
    public String address;
    @NonNull
    public String body;
    public long date;
    public boolean outgoing;
    public boolean read;
    public int sendState;
    public int deliveryState;
    public int partCount;
    public Integer errorCode;
    public int dispatchState;
    public int attemptCount;
    public Integer subscriptionId;
    public String incomingFingerprint;

    @Ignore
    public MessageEntity(@NonNull String localId, Long systemSmsId,
                         @NonNull String conversationKey, Long systemThreadId,
                         @NonNull String address, @NonNull String body, long date,
                         boolean outgoing, boolean read, int sendState,
                         int deliveryState, int partCount, Integer errorCode) {
        this(localId, systemSmsId, conversationKey, systemThreadId, address, body, date,
                outgoing, read, sendState, deliveryState, partCount, errorCode,
                SmsState.DISPATCH_NONE, 0, null, null);
    }

    public MessageEntity(@NonNull String localId, Long systemSmsId,
                         @NonNull String conversationKey, Long systemThreadId,
                         @NonNull String address, @NonNull String body, long date,
                         boolean outgoing, boolean read, int sendState,
                         int deliveryState, int partCount, Integer errorCode,
                         int dispatchState, int attemptCount, Integer subscriptionId,
                         String incomingFingerprint) {
        this.localId = localId;
        this.systemSmsId = systemSmsId;
        this.conversationKey = conversationKey;
        this.systemThreadId = systemThreadId;
        this.address = address;
        this.body = body;
        this.date = date;
        this.outgoing = outgoing;
        this.read = read;
        this.sendState = sendState;
        this.deliveryState = deliveryState;
        this.partCount = partCount;
        this.errorCode = errorCode;
        this.dispatchState = dispatchState;
        this.attemptCount = attemptCount;
        this.subscriptionId = subscriptionId;
        this.incomingFingerprint = incomingFingerprint;
    }
}
