package com.example.communicationapp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity {

    private EditText editTextPhone, editTextMessage;
    private TextInputLayout layoutPhone, layoutMessage;
    private MaterialButton btnSendSMS, btnMakeCall, btnShowNotification, btnShowDialog;

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String CHANNEL_ID = "communication_channel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize views
        editTextPhone = findViewById(R.id.editTextPhone);
        editTextMessage = findViewById(R.id.editTextMessage);
        layoutPhone = findViewById(R.id.layoutPhone);
        layoutMessage = findViewById(R.id.layoutMessage);
        btnSendSMS = findViewById(R.id.btnSendSMS);
        btnMakeCall = findViewById(R.id.btnMakeCall);
        btnShowNotification = findViewById(R.id.btnShowNotification);
        btnShowDialog = findViewById(R.id.btnShowDialog);

        // Create notification channel
        createNotificationChannel();

        // Request permissions
        requestPermissions();

        // Set click listeners
        btnSendSMS.setOnClickListener(v -> sendSMS());

        btnMakeCall.setOnClickListener(v -> makePhoneCall());

        btnShowNotification.setOnClickListener(v -> showNotification());

        btnShowDialog.setOnClickListener(v -> showDialogBox());
    }

    // Request runtime permissions
    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.SEND_SMS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.POST_NOTIFICATIONS
        };

        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    // Send SMS
    private void sendSMS() {
        String phoneNumber = editTextPhone.getText().toString().trim();
        String message = editTextMessage.getText().toString().trim();

        layoutPhone.setError(null);
        layoutMessage.setError(null);

        boolean error = false;
        if (phoneNumber.isEmpty()) {
            layoutPhone.setError("Phone number is required");
            error = true;
        }
        if (message.isEmpty()) {
            layoutMessage.setError("Message is required");
            error = true;
        }

        if (error) return;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                SmsManager smsManager = getSystemService(SmsManager.class);
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                Toast.makeText(this, "SMS sent successfully", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "SMS permission not granted", Toast.LENGTH_SHORT).show();
        }
    }

    // Make phone call
    private void makePhoneCall() {
        String phoneNumber = editTextPhone.getText().toString().trim();

        layoutPhone.setError(null);

        if (phoneNumber.isEmpty()) {
            layoutPhone.setError("Phone number is required");
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(callIntent);
        } else {
            Toast.makeText(this, "Call permission not granted", Toast.LENGTH_SHORT).show();
        }
    }

    // Create notification channel (required for Android 8.0+)
    private void createNotificationChannel() {
        CharSequence name = "Communication Channel";
        String description = "Channel for communication notifications";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    // Show notification
    private void showNotification() {
        String message = editTextMessage.getText().toString().trim();
        if (message.isEmpty()) {
            message = "This is a test notification";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Communication App")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.notify(1, builder.build());
            Toast.makeText(this, "Notification shown", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Notification permission not granted", Toast.LENGTH_SHORT).show();
        }
    }

    // Show dialog box
    private void showDialogBox() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title)
                .setMessage(R.string.dialog_message)
                .setPositiveButton(R.string.dialog_btn_ok, (dialog, which) -> 
                        Toast.makeText(MainActivity.this, R.string.toast_clicked_ok, Toast.LENGTH_SHORT).show())
                .setNegativeButton(R.string.dialog_btn_cancel, (dialog, which) -> {
                        Toast.makeText(MainActivity.this, R.string.toast_clicked_cancel, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                })
                .setNeutralButton(R.string.dialog_btn_info, (dialog, which) -> 
                        Toast.makeText(MainActivity.this, R.string.toast_clicked_info, Toast.LENGTH_SHORT).show())
                .setCancelable(false)
                .show();
    }
}