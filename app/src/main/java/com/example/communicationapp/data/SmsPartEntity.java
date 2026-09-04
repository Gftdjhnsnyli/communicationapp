package com.example.communicationapp.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(
        tableName = "sms_parts",
        primaryKeys = {"messageId", "partIndex"},
        foreignKeys = @ForeignKey(
                entity = MessageEntity.class,
                parentColumns = "localId",
                childColumns = "messageId",
                onDelete = ForeignKey.CASCADE),
        indices = @Index("messageId"))
public class SmsPartEntity {
    @NonNull
    public String messageId;
    public int partIndex;
    public int sentState;
    public int deliveryState;
    public Integer sentResultCode;
    public Integer deliveryResultCode;

    public SmsPartEntity(@NonNull String messageId, int partIndex, int sentState,
                         int deliveryState, Integer sentResultCode,
                         Integer deliveryResultCode) {
        this.messageId = messageId;
        this.partIndex = partIndex;
        this.sentState = sentState;
        this.deliveryState = deliveryState;
        this.sentResultCode = sentResultCode;
        this.deliveryResultCode = deliveryResultCode;
    }
}
