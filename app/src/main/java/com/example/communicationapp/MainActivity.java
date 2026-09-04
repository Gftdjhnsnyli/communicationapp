package com.example.communicationapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.database.Cursor;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity {
    private EditText phoneInput;
    private EditText messageInput;
    private TextInputLayout phoneLayout;
    private TextInputLayout messageLayout;
    private NewMessageViewModel viewModel;
    private final ActivityResultLauncher<Intent> contactPicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                Uri contact = result.getData() == null ? null : result.getData().getData();
                if (result.getResultCode() != RESULT_OK || contact == null) return;
                String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER};
                try (Cursor cursor = getContentResolver().query(
                        contact, projection, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        phoneInput.setText(cursor.getString(0));
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        phoneInput = findViewById(R.id.editTextPhone);
        messageInput = findViewById(R.id.editTextMessage);
        phoneLayout = findViewById(R.id.layoutPhone);
        messageLayout = findViewById(R.id.layoutMessage);
        viewModel = new ViewModelProvider(this).get(NewMessageViewModel.class);
        phoneLayout.setEndIconOnClickListener(v -> contactPicker.launch(new Intent(
                Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)));

        Uri recipient = Intent.ACTION_SENDTO.equals(getIntent().getAction())
                ? getIntent().getData() : null;
        if (recipient != null && ("mms".equalsIgnoreCase(recipient.getScheme())
                || "mmsto".equalsIgnoreCase(recipient.getScheme()))) {
            android.widget.Toast.makeText(this, R.string.mms_not_supported,
                    android.widget.Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (recipient != null && ("sms".equalsIgnoreCase(recipient.getScheme())
                || "smsto".equalsIgnoreCase(recipient.getScheme()))) {
            String schemePart = recipient.getEncodedSchemeSpecificPart();
            int queryStart = schemePart.indexOf('?');
            phoneInput.setText(Uri.decode(
                    queryStart < 0 ? schemePart : schemePart.substring(0, queryStart)));
            if (queryStart >= 0) {
                Uri query = Uri.parse("https://localhost/?" + schemePart.substring(queryStart + 1));
                String body = query.getQueryParameter("body");
                if (body == null) body = query.getQueryParameter("sms_body");
                if (body != null) messageInput.setText(body);
            }
        }
        CharSequence sharedText = getIntent().getCharSequenceExtra(Intent.EXTRA_TEXT);
        if (sharedText == null) sharedText = getIntent().getCharSequenceExtra("sms_body");
        if (sharedText != null) messageInput.setText(sharedText);

        MaterialButton startChat = findViewById(R.id.btnSendSMS);
        startChat.setOnClickListener(v -> openConversation());
    }

    private void openConversation() {
        String address = phoneInput.getText().toString().trim();
        String body = messageInput.getText().toString().trim();
        phoneLayout.setError(address.isEmpty() ? getString(R.string.error_phone_required) : null);
        messageLayout.setError(null);
        if (address.isEmpty()) return;
        if (address.contains(",") || address.contains(";")) {
            phoneLayout.setError(getString(R.string.multiple_recipients_not_supported));
            return;
        }

        viewModel.openConversation(address, conversation -> {
            Intent chat = new Intent(this, ChatActivity.class)
                    .putExtra(ChatActivity.EXTRA_CONVERSATION_KEY, conversation.conversationKey)
                    .putExtra(ChatActivity.EXTRA_ADDRESS, conversation.address)
                    .putExtra(ChatActivity.EXTRA_CONTACT_NAME, conversation.getDisplayName());
            if (!body.isEmpty()) chat.putExtra(ChatActivity.EXTRA_INITIAL_MESSAGE, body);
            startActivity(chat);
            finish();
        });
    }
}
