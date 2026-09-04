package com.example.communicationapp.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "conversations")
public class ConversationEntity {
    @PrimaryKey
    @NonNull
    public String conversationKey;
    public Long systemThreadId;
    @NonNull
    public String address;
    public String contactName;
    @NonNull
    public String lastMessage;
    public long lastMessageAt;
    public int unreadCount;

    public ConversationEntity(@NonNull String conversationKey, Long systemThreadId,
                              @NonNull String address, String contactName,
                              @NonNull String lastMessage, long lastMessageAt, int unreadCount) {
        this.conversationKey = conversationKey;
        this.systemThreadId = systemThreadId;
        this.address = address;
        this.contactName = contactName;
        this.lastMessage = lastMessage;
        this.lastMessageAt = lastMessageAt;
        this.unreadCount = unreadCount;
    }

    public String getDisplayName() {
        return contactName == null || contactName.isBlank() ? address : contactName;
    }
}
