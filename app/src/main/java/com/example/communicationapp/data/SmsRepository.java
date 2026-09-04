package com.example.communicationapp.data;

import android.Manifest;
import android.app.PendingIntent;
import android.app.role.RoleManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.work.ExistingWorkPolicy;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.communicationapp.SmsStatusReceiver;
import com.example.communicationapp.worker.SendSmsWorker;
import com.example.communicationapp.worker.SmsSyncWorker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SmsRepository {
    public static final int DISPATCH_RESULT_SUCCESS = 0;
    public static final int DISPATCH_RESULT_FAILURE = 1;
    public interface ConversationCallback {
        void onReady(@NonNull ConversationEntity conversation);
    }

    public interface IncomingCallback {
        void onSaved(@NonNull String conversationKey, @NonNull String displayName);
    }

    private static volatile SmsRepository instance;
    private final Context context;
    private final CommunicationDatabase database;
    private final ConversationDao conversations;
    private final MessageDao messages;
    private final ContactsDataSource contacts;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private SmsRepository(Context context) {
        this.context = context.getApplicationContext();
        database = CommunicationDatabase.getInstance(context);
        conversations = database.conversationDao();
        messages = database.messageDao();
        contacts = new ContactsDataSource(context);
    }

    public static SmsRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (SmsRepository.class) {
                if (instance == null) {
                    instance = new SmsRepository(context);
                }
            }
        }
        return instance;
    }

    public LiveData<List<ConversationEntity>> observeConversations(String query) {
        return query == null || query.isBlank()
                ? conversations.observeAll() : conversations.search(escapeSearchQuery(query.trim()));
    }

    public LiveData<List<MessageEntity>> observeMessages(String conversationKey) {
        return messages.observeForConversation(conversationKey);
    }

    public void openConversation(String address, ConversationCallback callback) {
        executor.execute(() -> callback.onReady(ensureConversation(address, null, "", 0)));
    }

    public void refreshContactNames() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) return;
        executor.execute(() -> {
            for (ConversationEntity conversation : conversations.listAll()) {
                conversations.updateContactName(conversation.conversationKey,
                        contacts.getDisplayName(conversation.address));
            }
        });
    }

    public void requestSync() {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SmsSyncWorker.class)
                .setInitialDelay(300, TimeUnit.MILLISECONDS)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(
                "sms-provider-sync", ExistingWorkPolicy.KEEP, request);
    }

    public synchronized void syncFromSystem() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        String[] projection = {
                Telephony.Sms._ID,
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE,
                Telephony.Sms.READ,
                Telephony.Sms.STATUS,
                Telephony.Sms.ERROR_CODE
                , Telephony.Sms.SUBSCRIPTION_ID
        };

        Set<Long> seenSystemIds = new HashSet<>();
        try (Cursor cursor = context.getContentResolver().query(
                Telephony.Sms.CONTENT_URI, projection, null, null,
                Telephony.Sms.DATE + " ASC")) {
            if (cursor == null) return;
            int idColumn = cursor.getColumnIndexOrThrow(Telephony.Sms._ID);
            int threadColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID);
            int addressColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS);
            int bodyColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY);
            int dateColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE);
            int typeColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE);
            int readColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.READ);
            int statusColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.STATUS);
            int errorColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.ERROR_CODE);
            int subscriptionColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.SUBSCRIPTION_ID);

            while (cursor.moveToNext()) {
                long systemId = cursor.getLong(idColumn);
                seenSystemIds.add(systemId);
                long threadId = cursor.getLong(threadColumn);
                String address = safeText(cursor.getString(addressColumn));
                String body = safeText(cursor.getString(bodyColumn));
                long date = cursor.getLong(dateColumn);
                int type = cursor.getInt(typeColumn);
                if (type == Telephony.Sms.MESSAGE_TYPE_DRAFT) continue;
                boolean read = cursor.getInt(readColumn) != 0;
                int providerStatus = cursor.getInt(statusColumn);
                int errorCode = cursor.getInt(errorColumn);
                int subscriptionId = cursor.getInt(subscriptionColumn);
                boolean outgoing = type != Telephony.Sms.MESSAGE_TYPE_INBOX;
                String key = normalizeAddress(address);
                ConversationEntity conversation = ensureConversation(address, threadId, body, date);
                MessageEntity existing = messages.findBySystemId(systemId);

                int sendState = mapSendState(type);
                int deliveryState = mapDeliveryState(providerStatus);

                if (existing == null && outgoing) {
                    existing = messages.findPendingMatch(
                            key, body, date - 120_000L, date + 120_000L, date);
                    if (existing != null) {
                        messages.attachSystemId(existing.localId, systemId, threadId);
                    }
                }

                if (existing == null) {
                    messages.insert(new MessageEntity(
                            UUID.randomUUID().toString(), systemId, key, threadId,
                            address, body, date, outgoing, read, sendState,
                             deliveryState, 1, errorCode == 0 ? null : errorCode));
                    MessageEntity inserted = messages.findBySystemId(systemId);
                    if (inserted != null) {
                        inserted.subscriptionId = subscriptionId;
                        messages.updateSubscription(inserted.localId, subscriptionId);
                    }
                } else {
                    messages.updateFromSystem(
                            systemId, key, threadId, address, body, date, outgoing,
                            read, sendState, deliveryState,
                            errorCode == 0 ? null : errorCode);
                }
                messages.updateSubscription(existing == null
                        ? messages.findBySystemId(systemId).localId : existing.localId,
                        subscriptionId);
                conversations.updateSummary(
                        conversation.conversationKey, threadId, address,
                        conversation.contactName, body, date);
                conversations.refreshUnreadCount(key);
            }
        }
        for (Long cachedSystemId : messages.listSystemIds()) {
            if (!seenSystemIds.contains(cachedSystemId)) {
                messages.deleteBySystemId(cachedSystemId);
            }
        }
        conversations.refreshAllSummaries();
        conversations.deleteEmpty();
    }

    public String sendMessage(String address, String body) {
        return sendMessage(address, body, SubscriptionManager.getDefaultSmsSubscriptionId());
    }

    public String sendMessage(String address, String body, int subscriptionId) {
        String localId = UUID.randomUUID().toString();
        enqueueSend(localId, address, body, subscriptionId);
        return localId;
    }

    public void sendMessageNow(String address, String body) {
        sendMessage(address, body);
    }

    public void enqueueSend(String localId, String address, String body) {
        enqueueSend(localId, address, body, SubscriptionManager.getDefaultSmsSubscriptionId());
    }

    public void enqueueSend(String localId, String address, String body, int subscriptionId) {
        String safeAddress = safeText(address).trim();
        String safeBody = safeText(body).trim();
        if (safeAddress.isBlank() || safeBody.isBlank()) return;
        Data input = new Data.Builder()
                .putString(SendSmsWorker.KEY_MESSAGE_ID, localId)
                .putString(SendSmsWorker.KEY_ADDRESS, safeAddress)
                .putString(SendSmsWorker.KEY_BODY, safeBody)
                .putInt(SendSmsWorker.KEY_SUBSCRIPTION_ID, subscriptionId)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(
                "send-sms-" + localId,
                ExistingWorkPolicy.KEEP,
                new OneTimeWorkRequest.Builder(SendSmsWorker.class).setInputData(input).build());
    }

    public synchronized int dispatchQueuedMessage(String localId, String address, String body,
                                                   int subscriptionId) {
        if (address.isBlank() || body.isBlank()) return DISPATCH_RESULT_FAILURE;
        SmsManager smsManager = context.getSystemService(SmsManager.class);
        if (smsManager != null && subscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            smsManager = SmsManager.getSmsManagerForSubscriptionId(subscriptionId);
        }
        long now = System.currentTimeMillis();
        ConversationEntity conversation = ensureConversation(address, null, body,
                now);
        ArrayList<String> parts = smsManager == null
                ? new ArrayList<>() : smsManager.divideMessage(body);
        if (parts.isEmpty()) parts.add(body);
        List<SmsPartEntity> partRows = new ArrayList<>();
        ArrayList<PendingIntent> sentIntents = new ArrayList<>();
        ArrayList<PendingIntent> deliveredIntents = new ArrayList<>();
        for (int index = 0; index < parts.size(); index++) {
            partRows.add(new SmsPartEntity(localId, index, SmsState.PART_PENDING,
                    SmsState.PART_PENDING, null, null));
            sentIntents.add(statusIntent(localId, index, SmsStatusReceiver.ACTION_SENT));
            deliveredIntents.add(statusIntent(localId, index, SmsStatusReceiver.ACTION_DELIVERED));
        }
        if (messages.findByLocalId(localId) == null) {
            database.runInTransaction(() -> {
                messages.insert(new MessageEntity(
                        localId, null, conversation.conversationKey, conversation.systemThreadId,
                        address, body, now, true, true, SmsState.SEND_PENDING,
                        SmsState.DELIVERY_PENDING, parts.size(), null,
                        SmsState.DISPATCH_QUEUED, 0,
                        subscriptionId == SubscriptionManager.INVALID_SUBSCRIPTION_ID
                                ? null : subscriptionId, null));
                messages.insertParts(partRows);
                conversations.updateSummary(conversation.conversationKey,
                        conversation.systemThreadId, address, conversation.contactName, body, now);
            });
        }
        MessageEntity queued = messages.findByLocalId(localId);
        if (queued == null) return DISPATCH_RESULT_FAILURE;
        if (queued.dispatchState == SmsState.DISPATCH_HANDOFF_STARTED
                || queued.dispatchState == SmsState.DISPATCH_HANDED_OFF) {
            return DISPATCH_RESULT_SUCCESS;
        }
        if (queued.dispatchState == SmsState.DISPATCH_FAILED) return DISPATCH_RESULT_FAILURE;
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED || smsManager == null) {
            messages.markDispatchFailed(localId, SmsManager.RESULT_ERROR_GENERIC_FAILURE);
            return DISPATCH_RESULT_FAILURE;
        }

        if (isDefaultSmsApp()) {
            try {
                ContentValues values = new ContentValues();
                values.put(Telephony.Sms.ADDRESS, address);
                values.put(Telephony.Sms.BODY, body);
                values.put(Telephony.Sms.DATE, now);
                values.put(Telephony.Sms.READ, 1);
                values.put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_OUTBOX);
                Uri uri = context.getContentResolver().insert(
                        Telephony.Sms.Outbox.CONTENT_URI, values);
                if (uri != null) messages.attachSystemId(localId, ContentUris.parseId(uri), null);
            } catch (SecurityException ignored) {
                // Sending can continue even if provider access was revoked during the operation.
            }
        }

        messages.updateDispatch(localId, SmsState.DISPATCH_HANDOFF_STARTED, 1, null);
        try {
            if (parts.size() == 1) {
                smsManager.sendTextMessage(address, null, body,
                        sentIntents.get(0), deliveredIntents.get(0));
            } else {
                smsManager.sendMultipartTextMessage(
                        address, null, parts, sentIntents, deliveredIntents);
            }
            messages.updateDispatch(localId, SmsState.DISPATCH_HANDED_OFF, 0, null);
            return DISPATCH_RESULT_SUCCESS;
        } catch (RuntimeException exception) {
            for (int index = 0; index < parts.size(); index++) {
                messages.applySentResult(localId, index,
                        SmsManager.RESULT_ERROR_GENERIC_FAILURE);
            }
            updateSystemStatus(localId);
            messages.updateDispatch(localId, SmsState.DISPATCH_FAILED, 0,
                    SmsManager.RESULT_ERROR_GENERIC_FAILURE);
            requestSync();
            return DISPATCH_RESULT_FAILURE;
        }
    }

    private PendingIntent statusIntent(String messageId, int partIndex, String action) {
        Intent intent = new Intent(context, SmsStatusReceiver.class)
                .setAction(action)
                .setData(Uri.parse("communicationapp://sms/" + messageId + "/" +
                        action.substring(action.lastIndexOf('.') + 1) + "/" + partIndex))
                .putExtra(SmsStatusReceiver.EXTRA_MESSAGE_ID, messageId)
                .putExtra(SmsStatusReceiver.EXTRA_PART_INDEX, partIndex);
        int requestCode = 31 * messageId.hashCode() + 2 * partIndex +
                (SmsStatusReceiver.ACTION_DELIVERED.equals(action) ? 1 : 0);
        return PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public void handleStatus(String action, String messageId, int partIndex,
                             int resultCode, Runnable completion) {
        executor.execute(() -> {
            try {
                synchronized (this) {
                    if (SmsStatusReceiver.ACTION_SENT.equals(action)) {
                        messages.applySentResult(messageId, partIndex, resultCode);
                    } else if (SmsStatusReceiver.ACTION_DELIVERED.equals(action)) {
                        messages.applyDeliveryResult(messageId, partIndex, resultCode);
                    }
                    updateSystemStatus(messageId);
                }
                requestSync();
            } finally {
                completion.run();
            }
        });
    }

    public synchronized void handleStatusNow(String action, String messageId, int partIndex,
                                             int resultCode) {
        if (SmsStatusReceiver.ACTION_SENT.equals(action)) {
            messages.applySentResult(messageId, partIndex, resultCode);
        } else if (SmsStatusReceiver.ACTION_DELIVERED.equals(action)) {
            messages.applyDeliveryResult(messageId, partIndex, resultCode);
        } else {
            return;
        }
        updateSystemStatus(messageId);
        requestSync();
    }

    private void updateSystemStatus(String messageId) {
        if (!isDefaultSmsApp()) return;
        MessageEntity message = messages.findByLocalId(messageId);
        if (message == null || message.systemSmsId == null) return;
        ContentValues values = new ContentValues();
        if (message.sendState == SmsState.SEND_SENT) {
            values.put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT);
        } else if (message.sendState == SmsState.SEND_FAILED) {
            values.put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_FAILED);
            if (message.errorCode != null) values.put(Telephony.Sms.ERROR_CODE, message.errorCode);
        }
        if (message.deliveryState == SmsState.DELIVERY_COMPLETE) {
            values.put(Telephony.Sms.STATUS, Telephony.Sms.STATUS_COMPLETE);
        } else if (message.deliveryState == SmsState.DELIVERY_FAILED) {
            values.put(Telephony.Sms.STATUS, Telephony.Sms.STATUS_FAILED);
        }
        if (values.size() > 0) {
            context.getContentResolver().update(
                    ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, message.systemSmsId),
                    values, null, null);
        }
    }

    public void receiveIncoming(String address, String body, long date, String fingerprint,
                                int subscriptionId,
                                 IncomingCallback callback, Runnable completion) {
        executor.execute(() -> receiveIncomingInternal(
                address, body, date, fingerprint, subscriptionId, callback, completion));
    }

    private synchronized void receiveIncomingInternal(String address, String body, long date,
                                                       String fingerprint,
                                                       int subscriptionId,
                                                       IncomingCallback callback,
                                                       Runnable completion) {
        try {
            if (fingerprint != null && messages.findByIncomingFingerprint(fingerprint) != null) {
                return;
            }
            Long providerId = null;
            if (isDefaultSmsApp()) {
                ContentValues values = new ContentValues();
                values.put(Telephony.Sms.ADDRESS, address);
                values.put(Telephony.Sms.BODY, body);
                values.put(Telephony.Sms.DATE, date);
                values.put(Telephony.Sms.READ, 0);
                values.put(Telephony.Sms.SEEN, 0);
                Uri uri = context.getContentResolver().insert(
                        Telephony.Sms.Inbox.CONTENT_URI, values);
                if (uri != null) providerId = ContentUris.parseId(uri);
            }
            ConversationEntity conversation = ensureConversation(address, null, body, date);
            long inserted = messages.insert(new MessageEntity(
                    UUID.randomUUID().toString(), providerId, conversation.conversationKey,
                    null, address, body, date, false, false, SmsState.SEND_NONE,
                    SmsState.DELIVERY_NONE, 1, null, SmsState.DISPATCH_NONE,
                    0, subscriptionId == SubscriptionManager.INVALID_SUBSCRIPTION_ID
                            ? null : subscriptionId, fingerprint));
            if (inserted == -1L) return;
            conversations.updateSummary(conversation.conversationKey, null, address,
                    conversation.contactName, body, date);
            conversations.refreshUnreadCount(conversation.conversationKey);
            requestSync();
            callback.onSaved(conversation.conversationKey, conversation.getDisplayName());
        } finally {
            completion.run();
        }
    }

    public void markConversationRead(String conversationKey) {
        executor.execute(() -> {
            database.runInTransaction(() -> {
                messages.markRead(conversationKey);
                conversations.clearUnread(conversationKey);
            });
            if (isDefaultSmsApp()) {
                for (Long systemId : messages.listIncomingSystemIds(conversationKey)) {
                    ContentValues values = new ContentValues();
                    values.put(Telephony.Sms.READ, 1);
                    values.put(Telephony.Sms.SEEN, 1);
                    context.getContentResolver().update(
                            ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, systemId),
                            values, null, null);
                }
            }
        });
    }

    private ConversationEntity ensureConversation(String address, Long threadId,
                                                  String body, long date) {
        String key = normalizeAddress(address);
        ConversationEntity existing = conversations.findByKey(key);
        if (existing != null) return existing;
        String name = contacts.getDisplayName(address);
        ConversationEntity created = new ConversationEntity(
                key, threadId, address, name, body, date, 0);
        conversations.insert(created);
        ConversationEntity stored = conversations.findByKey(key);
        return stored == null ? created : stored;
    }

    public boolean isDefaultSmsApp() {
        RoleManager roles = context.getSystemService(RoleManager.class);
        return roles != null && roles.isRoleAvailable(RoleManager.ROLE_SMS)
                && roles.isRoleHeld(RoleManager.ROLE_SMS);
    }

    public static String normalizeAddress(String address) {
        String safeAddress = safeText(address).trim();
        if (!safeAddress.matches(".*\\d.*") || safeAddress.matches(".*[A-Za-z].*")) {
            return safeAddress;
        }
        String digits = safeAddress.replaceAll("[^0-9]", "");
        return safeAddress.startsWith("+") ? "+" + digits : digits;
    }

    public static String escapeSearchQuery(String query) {
        return safeText(query).replace("\\", "\\\\")
                .replace("%", "\\%").replace("_", "\\_");
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }

    private static int mapSendState(int type) {
        if (type == Telephony.Sms.MESSAGE_TYPE_SENT) return SmsState.SEND_SENT;
        if (type == Telephony.Sms.MESSAGE_TYPE_FAILED) return SmsState.SEND_FAILED;
        if (type == Telephony.Sms.MESSAGE_TYPE_OUTBOX
                || type == Telephony.Sms.MESSAGE_TYPE_QUEUED) {
            return SmsState.SEND_PENDING;
        }
        return SmsState.SEND_NONE;
    }

    private static int mapDeliveryState(int status) {
        if (status == Telephony.Sms.STATUS_COMPLETE) return SmsState.DELIVERY_COMPLETE;
        if (status == Telephony.Sms.STATUS_PENDING) return SmsState.DELIVERY_PENDING;
        if (status == Telephony.Sms.STATUS_FAILED) return SmsState.DELIVERY_FAILED;
        return SmsState.DELIVERY_NONE;
    }
}
