package com.example.communicationapp.data;

import android.app.Activity;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public abstract class MessageDao {
    @Query("SELECT COUNT(*) FROM messages")
    public abstract int count();

    @Query("SELECT systemSmsId FROM messages WHERE systemSmsId IS NOT NULL")
    public abstract List<Long> listSystemIds();

    @Query("DELETE FROM messages WHERE systemSmsId = :systemId")
    public abstract void deleteBySystemId(long systemId);

    @Query("SELECT * FROM messages WHERE conversationKey = :key ORDER BY date ASC, localId ASC")
    public abstract LiveData<List<MessageEntity>> observeForConversation(String key);

    @Query("SELECT * FROM messages WHERE localId = :localId LIMIT 1")
    public abstract MessageEntity findByLocalId(String localId);

    @Query("SELECT * FROM messages WHERE systemSmsId = :systemId LIMIT 1")
    public abstract MessageEntity findBySystemId(long systemId);

    @Query("SELECT * FROM messages WHERE incomingFingerprint = :fingerprint LIMIT 1")
    public abstract MessageEntity findByIncomingFingerprint(String fingerprint);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract long insert(MessageEntity message);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertParts(List<SmsPartEntity> parts);

    @Query("UPDATE messages SET dispatchState = :state, attemptCount = attemptCount + :attemptIncrement, " +
            "errorCode = :errorCode WHERE localId = :localId")
    public abstract void updateDispatch(String localId, int state, int attemptIncrement,
                                        Integer errorCode);

    @Query("UPDATE messages SET dispatchState = 4, sendState = 3, errorCode = :errorCode " +
            "WHERE localId = :localId")
    public abstract void markDispatchFailed(String localId, int errorCode);

    @Query("UPDATE messages SET subscriptionId = :subscriptionId WHERE localId = :localId")
    public abstract void updateSubscription(String localId, int subscriptionId);

    @Query("SELECT systemSmsId FROM messages WHERE conversationKey = :key AND outgoing = 0 " +
            "AND systemSmsId IS NOT NULL")
    public abstract List<Long> listIncomingSystemIds(String key);

    @Query("UPDATE messages SET systemSmsId = :systemId, systemThreadId = COALESCE(:threadId, systemThreadId) " +
            "WHERE localId = :localId")
    public abstract void attachSystemId(String localId, long systemId, Long threadId);

    @Query("UPDATE messages SET conversationKey = :conversationKey, systemThreadId = :threadId, " +
            "address = :address, body = :body, date = :date, outgoing = :outgoing, " +
            "read = CASE WHEN read = 1 THEN 1 ELSE :read END, " +
            "sendState = CASE WHEN sendState = 3 THEN 3 ELSE :sendState END, " +
            "deliveryState = CASE WHEN deliveryState IN (2, 3) THEN deliveryState ELSE :deliveryState END, " +
            "errorCode = :errorCode " +
            "WHERE systemSmsId = :systemId")
    public abstract void updateFromSystem(long systemId, String conversationKey, Long threadId,
                                          String address, String body, long date, boolean outgoing,
                                          boolean read, int sendState, int deliveryState,
                                          Integer errorCode);

    @Query("SELECT * FROM messages WHERE systemSmsId IS NULL AND outgoing = 1 " +
            "AND dispatchState IN (2, 3) " +
            "AND conversationKey = :key AND body = :body AND date BETWEEN :from AND :to " +
            "ORDER BY ABS(date - :target) LIMIT 1")
    public abstract MessageEntity findPendingMatch(String key, String body, long from,
                                                   long to, long target);

    @Query("UPDATE messages SET read = 1 WHERE conversationKey = :key AND outgoing = 0")
    public abstract void markRead(String key);

    @Query("UPDATE sms_parts SET sentState = :state, sentResultCode = :resultCode " +
            "WHERE messageId = :messageId AND partIndex = :partIndex")
    abstract void updateSentPart(String messageId, int partIndex, int state, int resultCode);

    @Query("UPDATE sms_parts SET deliveryState = :state, deliveryResultCode = :resultCode " +
            "WHERE messageId = :messageId AND partIndex = :partIndex")
    abstract void updateDeliveredPart(String messageId, int partIndex, int state, int resultCode);

    @Query("UPDATE messages SET sendState = CASE " +
            "WHEN EXISTS(SELECT 1 FROM sms_parts WHERE messageId = :messageId AND sentState = 2) THEN 3 " +
            "WHEN NOT EXISTS(SELECT 1 FROM sms_parts WHERE messageId = :messageId AND sentState != 1) THEN 2 " +
            "ELSE 1 END, errorCode = CASE WHEN :resultCode = -1 THEN errorCode ELSE :resultCode END " +
            "WHERE localId = :messageId")
    abstract void recomputeSendState(String messageId, int resultCode);

    @Query("UPDATE messages SET deliveryState = CASE " +
            "WHEN EXISTS(SELECT 1 FROM sms_parts WHERE messageId = :messageId AND deliveryState = 2) THEN 3 " +
            "WHEN NOT EXISTS(SELECT 1 FROM sms_parts WHERE messageId = :messageId AND deliveryState != 1) THEN 2 " +
            "ELSE 1 END WHERE localId = :messageId")
    abstract void recomputeDeliveryState(String messageId);

    @Transaction
    public void applySentResult(String messageId, int partIndex, int resultCode) {
        int state = resultCode == Activity.RESULT_OK
                ? SmsState.PART_COMPLETE : SmsState.PART_FAILED;
        updateSentPart(messageId, partIndex, state, resultCode);
        recomputeSendState(messageId, resultCode);
    }

    @Transaction
    public void applyDeliveryResult(String messageId, int partIndex, int resultCode) {
        int state = resultCode == Activity.RESULT_OK
                ? SmsState.PART_COMPLETE : SmsState.PART_FAILED;
        updateDeliveredPart(messageId, partIndex, state, resultCode);
        recomputeDeliveryState(messageId);
    }
}
