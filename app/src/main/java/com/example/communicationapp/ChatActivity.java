package com.example.communicationapp;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private String threadId;
    private String address;
    private String contactName;
    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private List<Message> messageList = new ArrayList<>();
    private EditText editTextMessage;
    private static final String SENT_ACTION = "SMS_SENT";
    private static final String DELIVERED_ACTION = "SMS_DELIVERED";
    private ContentObserver smsObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        threadId = getIntent().getStringExtra("thread_id");
        address = getIntent().getStringExtra("address");
        contactName = getIntent().getStringExtra("contact_name");

        if (threadId == null || address == null) {
            Toast.makeText(this, "Error: Invalid conversation data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();

        recyclerView = findViewById(R.id.recyclerViewMessages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        
        adapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(adapter);

        editTextMessage = findViewById(R.id.editTextMessage);
        FloatingActionButton btnSend = findViewById(R.id.btnSendMessage);
        btnSend.setOnClickListener(v -> sendMessage());

        // Keyboard listener to scroll messages
        recyclerView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, right1, oldBottom) -> {
            if (bottom < oldBottom) {
                recyclerView.postDelayed(() -> {
                    if (messageList.size() > 0) {
                        recyclerView.smoothScrollToPosition(messageList.size() - 1);
                    }
                }, 100);
            }
        });

        setupSmsObserver();
        loadMessages();
    }

    private void setupToolbar() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageView imageContactToolbar = findViewById(R.id.imageContactToolbar);
        TextView textContactNameToolbar = findViewById(R.id.textContactNameToolbar);

        btnBack.setOnClickListener(v -> finish());
        textContactNameToolbar.setText(contactName);

        Bitmap photo = getContactPhoto(address);
        if (photo != null) {
            imageContactToolbar.setImageBitmap(photo);
        } else {
            imageContactToolbar.setImageResource(R.drawable.images);
        }
    }

    private void setupSmsObserver() {
        smsObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                loadMessages();
            }
        };
    }

    private Bitmap getContactPhoto(String phoneNumber) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        String[] projection = {ContactsContract.PhoneLookup._ID};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                long contactId = cursor.getLong(0);
                cursor.close();
                
                Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
                InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(getContentResolver(), contactUri);
                if (input != null) {
                    return BitmapFactory.decodeStream(input);
                }
            }
            cursor.close();
        }
        return null;
    }

    private void loadMessages() {
        messageList.clear();
        String selection = Telephony.Sms.THREAD_ID + " = ?";
        String[] selectionArgs = {threadId};
        Cursor cursor = getContentResolver().query(Telephony.Sms.CONTENT_URI, null, selection, selectionArgs, Telephony.Sms.DATE + " ASC");

        if (cursor != null) {
            int idIndex = cursor.getColumnIndex(Telephony.Sms._ID);
            int bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY);
            int dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE);
            int typeIndex = cursor.getColumnIndex(Telephony.Sms.TYPE);
            int statusIndex = cursor.getColumnIndex(Telephony.Sms.STATUS);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idIndex);
                String body = cursor.getString(bodyIndex);
                long date = cursor.getLong(dateIndex);
                int type = cursor.getInt(typeIndex);
                int status = cursor.getInt(statusIndex);
                messageList.add(new Message(id, address, body, date, type, status));
            }
            cursor.close();
        }
        adapter.notifyDataSetChanged();
        if (messageList.size() > 0) {
            recyclerView.scrollToPosition(messageList.size() - 1);
        }
    }

    private void sendMessage() {
        String body = editTextMessage.getText().toString().trim();
        if (body.isEmpty()) return;

        SmsManager smsManager = getSystemService(SmsManager.class);
        
        Intent sentIntent = new Intent(SENT_ACTION);
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, sentIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent deliveredIntent = new Intent(DELIVERED_ACTION);
        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0, deliveredIntent, PendingIntent.FLAG_IMMUTABLE);

        smsManager.sendTextMessage(address, null, body, sentPI, deliveredPI);
        editTextMessage.setText("");
        
        // Refresh immediately for local feedback
        loadMessages();
    }

    private BroadcastReceiver smsStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadMessages();
            if (DELIVERED_ACTION.equals(intent.getAction())) {
                Toast.makeText(context, R.string.msg_delivered, Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(smsStatusReceiver, new IntentFilter(SENT_ACTION), Context.RECEIVER_NOT_EXPORTED);
        registerReceiver(smsStatusReceiver, new IntentFilter(DELIVERED_ACTION), Context.RECEIVER_NOT_EXPORTED);
        getContentResolver().registerContentObserver(Telephony.Sms.CONTENT_URI, true, smsObserver);
        loadMessages();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(smsStatusReceiver);
        getContentResolver().unregisterContentObserver(smsObserver);
    }
}