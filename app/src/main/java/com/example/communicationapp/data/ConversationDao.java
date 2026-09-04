package com.example.communicationapp.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY lastMessageAt DESC")
    LiveData<List<ConversationEntity>> observeAll();

    @Query("SELECT * FROM conversations")
    List<ConversationEntity> listAll();

    @Query("SELECT * FROM conversations WHERE address LIKE '%' || :query || '%' ESCAPE '\\' " +
            "OR contactName LIKE '%' || :query || '%' ESCAPE '\\' " +
            "OR EXISTS(SELECT 1 FROM messages WHERE messages.conversationKey = " +
            "conversations.conversationKey AND body LIKE '%' || :query || '%' ESCAPE '\\') " +
            "ORDER BY lastMessageAt DESC")
    LiveData<List<ConversationEntity>> search(String query);

    @Query("SELECT * FROM conversations WHERE conversationKey = :key LIMIT 1")
    ConversationEntity findByKey(String key);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(ConversationEntity conversation);

    @Query("UPDATE conversations SET contactName = :contactName WHERE conversationKey = :key")
    void updateContactName(String key, String contactName);

    @Query("UPDATE conversations SET systemThreadId = COALESCE(:threadId, systemThreadId), " +
            "address = :address, contactName = COALESCE(:contactName, contactName), " +
            "lastMessage = :body, lastMessageAt = :date WHERE conversationKey = :key " +
            "AND lastMessageAt <= :date")
    void updateSummary(String key, Long threadId, String address, String contactName,
                       String body, long date);

    @Query("UPDATE conversations SET unreadCount = " +
            "(SELECT COUNT(*) FROM messages WHERE conversationKey = :key " +
            "AND outgoing = 0 AND read = 0) WHERE conversationKey = :key")
    void refreshUnreadCount(String key);

    @Query("UPDATE conversations SET unreadCount = 0 WHERE conversationKey = :key")
    void clearUnread(String key);

    @Query("DELETE FROM conversations WHERE NOT EXISTS " +
            "(SELECT 1 FROM messages WHERE messages.conversationKey = conversations.conversationKey)")
    void deleteEmpty();

    @Query("UPDATE conversations SET " +
            "lastMessage = COALESCE((SELECT body FROM messages WHERE messages.conversationKey = " +
            "conversations.conversationKey ORDER BY date DESC, localId DESC LIMIT 1), ''), " +
            "lastMessageAt = COALESCE((SELECT date FROM messages WHERE messages.conversationKey = " +
            "conversations.conversationKey ORDER BY date DESC, localId DESC LIMIT 1), 0), " +
            "systemThreadId = (SELECT systemThreadId FROM messages WHERE messages.conversationKey = " +
            "conversations.conversationKey ORDER BY date DESC, localId DESC LIMIT 1), " +
            "unreadCount = (SELECT COUNT(*) FROM messages WHERE messages.conversationKey = " +
            "conversations.conversationKey AND outgoing = 0 AND read = 0)")
    void refreshAllSummaries();
}
