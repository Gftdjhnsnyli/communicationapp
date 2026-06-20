package com.example.communicationapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import androidx.core.splashscreen.SplashScreen;
import java.util.ArrayList;
import java.util.List;

public class ConversationListActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE = 123;
    private RecyclerView recyclerView;
    private ConversationAdapter adapter;
    private List<Conversation> conversationList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation_list);

        recyclerView = findViewById(R.id.recyclerViewConversations);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new ConversationAdapter(conversationList, conversation -> {
            if (conversation != null && conversation.getThreadId() != null && conversation.getAddress() != null) {
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("thread_id", conversation.getThreadId());
                intent.putExtra("address", conversation.getAddress());
                intent.putExtra("contact_name", conversation.getContactName());
                startActivity(intent);
            } else {
                Toast.makeText(this, "Cannot open chat: Missing information", Toast.LENGTH_SHORT).show();
            }
        });
        recyclerView.setAdapter(adapter);

        ExtendedFloatingActionButton fab = findViewById(R.id.fabNewMessage);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });

        checkPermissions();
    }

    private void checkPermissions() {
        String[] permissions = {
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_CONTACTS
        };

        boolean allGranted = true;
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE);
        } else {
            loadConversations();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadConversations();
            }
        }
    }

    private void loadConversations() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        conversationList.clear();
        Uri uri = Telephony.Threads.CONTENT_URI.buildUpon().appendQueryParameter("simple", "true").build();
        String[] projection = {
            Telephony.Threads._ID,
            Telephony.Threads.SNIPPET,
            Telephony.Threads.DATE,
            Telephony.Threads.RECIPIENT_IDS
        };

        Cursor cursor = getContentResolver().query(uri, projection, null, null, Telephony.Threads.DATE + " DESC");

        if (cursor != null) {
            int idIndex = cursor.getColumnIndex(Telephony.Threads._ID);
            int snippetIndex = cursor.getColumnIndex(Telephony.Threads.SNIPPET);
            int dateIndex = cursor.getColumnIndex(Telephony.Threads.DATE);
            int recipientIndex = cursor.getColumnIndex(Telephony.Threads.RECIPIENT_IDS);

            while (cursor.moveToNext()) {
                String threadId = cursor.getString(idIndex);
                String snippet = cursor.getString(snippetIndex);
                long date = cursor.getLong(dateIndex);
                String recipientId = cursor.getString(recipientIndex);

                String address = getAddressFromRecipientId(recipientId);
                String contactName = getContactName(address);

                conversationList.add(new Conversation(threadId, address, contactName, snippet, date));
            }
            cursor.close();
        }
        adapter.notifyDataSetChanged();
    }

    private String getAddressFromRecipientId(String recipientId) {
        if (recipientId == null || recipientId.isEmpty()) return "";
        
        // Use a safer URI for recipient address lookup
        Uri uri = Uri.parse("content://mms-sms/canonical-address/" + recipientId);
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return recipientId; // Fallback to recipientId if lookup fails
    }

    private String getContactName(String phoneNumber) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        String[] projection = {ContactsContract.PhoneLookup.DISPLAY_NAME};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String name = cursor.getString(0);
                cursor.close();
                return name;
            }
            cursor.close();
        }
        return phoneNumber;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            loadConversations();
        }
    }
}