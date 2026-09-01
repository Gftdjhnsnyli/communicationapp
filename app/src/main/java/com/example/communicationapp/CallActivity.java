package com.example.communicationapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.InputStream;

public class CallActivity extends AppCompatActivity {

    private String address;
    private boolean isIncoming;
    private TextView textCallStatus;
    private LinearLayout incomingCallButtons;
    private FloatingActionButton btnHangup;

    private BroadcastReceiver callStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra("state");
            if ("active".equals(state)) {
                textCallStatus.setText(R.string.call_status_active);
                incomingCallButtons.setVisibility(View.GONE);
                btnHangup.setVisibility(View.VISIBLE);
            } else if ("disconnected".equals(state)) {
                textCallStatus.setText(R.string.call_status_disconnected);
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        address = getIntent().getStringExtra("address");
        isIncoming = getIntent().getBooleanExtra("is_incoming", false);

        ImageView imageContact = findViewById(R.id.imageContactCall);
        TextView textContactName = findViewById(R.id.textContactNameCall);
        textCallStatus = findViewById(R.id.textCallStatus);
        incomingCallButtons = findViewById(R.id.incomingCallButtons);
        btnHangup = findViewById(R.id.btnHangup);

        textContactName.setText(getContactName(address));
        Bitmap photo = getContactPhoto(address);
        if (photo != null) {
            imageContact.setImageBitmap(photo);
        }

        if (isIncoming) {
            textCallStatus.setText(R.string.call_status_ringing);
            incomingCallButtons.setVisibility(View.VISIBLE);
            btnHangup.setVisibility(View.GONE);
        } else {
            textCallStatus.setText(R.string.call_status_active); // Simplified
            incomingCallButtons.setVisibility(View.GONE);
            btnHangup.setVisibility(View.VISIBLE);
        }

        ExtendedFloatingActionButton btnAnswer = findViewById(R.id.btnAnswer);
        ExtendedFloatingActionButton btnReject = findViewById(R.id.btnReject);

        btnAnswer.setOnClickListener(v -> {
            if (MyConnectionService.currentConnection != null) {
                MyConnectionService.currentConnection.onAnswer();
            }
        });

        btnReject.setOnClickListener(v -> {
            if (MyConnectionService.currentConnection != null) {
                MyConnectionService.currentConnection.onReject();
            }
            finish();
        });

        btnHangup.setOnClickListener(v -> {
            if (MyConnectionService.currentConnection != null) {
                MyConnectionService.currentConnection.onDisconnect();
            }
            finish();
        });

        registerReceiver(callStateReceiver, new IntentFilter("com.example.communicationapp.CALL_STATE_CHANGED"), Context.RECEIVER_NOT_EXPORTED);
    }

    private String getContactName(String phoneNumber) {
        if (phoneNumber == null) return "Unknown";
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        String[] projection = {ContactsContract.PhoneLookup.DISPLAY_NAME};
        try (android.database.Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        }
        return phoneNumber;
    }

    private Bitmap getContactPhoto(String phoneNumber) {
        if (phoneNumber == null) return null;
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        String[] projection = {ContactsContract.PhoneLookup._ID};
        try (android.database.Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                long contactId = cursor.getLong(0);
                Uri contactUri = android.content.ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
                InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(getContentResolver(), contactUri);
                if (input != null) {
                    return BitmapFactory.decodeStream(input);
                }
            }
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(callStateReceiver);
    }
}
