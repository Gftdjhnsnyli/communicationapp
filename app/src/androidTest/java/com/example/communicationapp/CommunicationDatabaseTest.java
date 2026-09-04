package com.example.communicationapp;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.communicationapp.data.CommunicationDatabase;
import com.example.communicationapp.data.ConversationEntity;
import com.example.communicationapp.data.MessageEntity;
import com.example.communicationapp.data.SmsPartEntity;
import com.example.communicationapp.data.SmsState;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class CommunicationDatabaseTest {
    private CommunicationDatabase database;

    @Before
    public void createDatabase() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, CommunicationDatabase.class)
                .allowMainThreadQueries()
                .build();
    }

    @After
    public void closeDatabase() {
        database.close();
    }

    @Test
    public void duplicateSystemSmsId_isIgnored() {
        database.conversationDao().insert(new ConversationEntity(
                "+15551234567", 1L, "+15551234567", "Cafe", "Hello", 1L, 0));
        MessageEntity first = new MessageEntity(
                "local-1", 10L, "+15551234567", 1L, "+15551234567", "Hello",
                1L, false, false, SmsState.SEND_NONE, SmsState.DELIVERY_NONE, 1, null);
        MessageEntity duplicate = new MessageEntity(
                "local-2", 10L, "+15551234567", 1L, "+15551234567", "Hello",
                1L, false, false, SmsState.SEND_NONE, SmsState.DELIVERY_NONE, 1, null);
        database.messageDao().insert(first);
        database.messageDao().insert(duplicate);

        assertEquals(1, database.messageDao().count());
    }

    @Test
    public void multipartResults_updateAggregateStatus() {
        database.conversationDao().insert(new ConversationEntity(
                "+15551234567", null, "+15551234567", "Cafe", "Long message", 1L, 0));
        database.messageDao().insert(new MessageEntity(
                "local-1", null, "+15551234567", null, "+15551234567", "Long message",
                1L, true, true, SmsState.SEND_PENDING, SmsState.DELIVERY_PENDING, 2, null));
        database.messageDao().insertParts(List.of(
                new SmsPartEntity("local-1", 0, SmsState.PART_PENDING,
                        SmsState.PART_PENDING, null, null),
                new SmsPartEntity("local-1", 1, SmsState.PART_PENDING,
                        SmsState.PART_PENDING, null, null)));

        database.messageDao().applySentResult("local-1", 0, android.app.Activity.RESULT_OK);
        assertEquals(SmsState.SEND_PENDING,
                database.messageDao().findByLocalId("local-1").sendState);
        database.messageDao().applySentResult("local-1", 1, android.app.Activity.RESULT_OK);
        assertEquals(SmsState.SEND_SENT,
                database.messageDao().findByLocalId("local-1").sendState);
    }

    @Test
    public void duplicateIncomingFingerprint_isIgnored() {
        database.conversationDao().insert(new ConversationEntity(
                "+15551234567", 1L, "+15551234567", "Cafe", "Hello", 1L, 1));
        database.messageDao().insert(new MessageEntity(
                "incoming-1", 10L, "+15551234567", 1L, "+15551234567", "Hello",
                1L, false, false, SmsState.SEND_NONE, SmsState.DELIVERY_NONE, 1, null,
                SmsState.DISPATCH_NONE, 0, 1, "same-pdu"));
        database.messageDao().insert(new MessageEntity(
                "incoming-2", 11L, "+15551234567", 1L, "+15551234567", "Hello",
                1L, false, false, SmsState.SEND_NONE, SmsState.DELIVERY_NONE, 1, null,
                SmsState.DISPATCH_NONE, 0, 1, "same-pdu"));

        assertEquals(1, database.messageDao().count());
    }

    @Test
    public void multipartFailure_setsAggregateFailure() {
        database.conversationDao().insert(new ConversationEntity(
                "+15551234567", null, "+15551234567", "Cafe", "Long message", 1L, 0));
        database.messageDao().insert(new MessageEntity(
                "failed-1", null, "+15551234567", null, "+15551234567", "Long message",
                1L, true, true, SmsState.SEND_PENDING, SmsState.DELIVERY_PENDING, 2, null));
        database.messageDao().insertParts(List.of(
                new SmsPartEntity("failed-1", 0, SmsState.PART_PENDING,
                        SmsState.PART_PENDING, null, null),
                new SmsPartEntity("failed-1", 1, SmsState.PART_PENDING,
                        SmsState.PART_PENDING, null, null)));

        database.messageDao().applySentResult(
                "failed-1", 0, android.telephony.SmsManager.RESULT_ERROR_NO_SERVICE);

        assertEquals(SmsState.SEND_FAILED,
                database.messageDao().findByLocalId("failed-1").sendState);
    }
}
