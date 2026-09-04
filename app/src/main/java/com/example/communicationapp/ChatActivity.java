package com.example.communicationapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.communicationapp.data.SmsRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class ChatActivity extends AppCompatActivity {
    public static final String EXTRA_CONVERSATION_KEY = "conversation_key";
    public static final String EXTRA_ADDRESS = "address";
    public static final String EXTRA_CONTACT_NAME = "contact_name";
    public static final String EXTRA_INITIAL_MESSAGE = "initial_message";

    private ChatViewModel viewModel;
    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private String address;
    private String conversationKey;
    private int selectedSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private static volatile String visibleConversationKey;

    public static boolean isConversationVisible(String key) {
        return key != null && key.equals(visibleConversationKey);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        address = getIntent().getStringExtra(EXTRA_ADDRESS);
        conversationKey = getIntent().getStringExtra(EXTRA_CONVERSATION_KEY);
        String contactName = getIntent().getStringExtra(EXTRA_CONTACT_NAME);
        if (address == null || address.isBlank()) {
            Toast.makeText(this, R.string.invalid_conversation, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (conversationKey == null || conversationKey.isBlank()) {
            conversationKey = SmsRepository.normalizeAddress(address);
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.textContactNameToolbar)).setText(
                contactName == null || contactName.isBlank() ? address : contactName);
        ((ImageButton) findViewById(R.id.btnCall)).setOnClickListener(v -> showCallOptions());

        recyclerView = findViewById(R.id.recyclerViewMessages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new MessageAdapter(message -> new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.retry_message_title)
                .setMessage(R.string.retry_message_prompt)
                .setPositiveButton(R.string.retry, (dialog, which) -> viewModel.send(
                        message.body, message.subscriptionId == null
                                ? selectedSubscriptionId : message.subscriptionId))
                .setNegativeButton(android.R.string.cancel, null)
                .show());
        recyclerView.setAdapter(adapter);

        ChatViewModel.Factory factory = new ChatViewModel.Factory(
                SmsRepository.getInstance(this), conversationKey, address);
        viewModel = new ViewModelProvider(this, factory).get(ChatViewModel.class);
        viewModel.getMessages().observe(this, messages -> {
            boolean stayAtBottom = adapter.getItemCount() == 0 || !recyclerView.canScrollVertically(1);
            adapter.submitList(messages, () -> {
                if (stayAtBottom && !messages.isEmpty()) {
                    recyclerView.scrollToPosition(messages.size() - 1);
                }
            });
            if (isConversationVisible(conversationKey)) viewModel.markRead();
        });

        EditText messageInput = findViewById(R.id.editTextMessage);
        String draftKey = "draft:" + conversationKey;
        String savedDraft = getPreferences(MODE_PRIVATE).getString(draftKey, "");
        if (savedInstanceState == null && !savedDraft.isBlank()) messageInput.setText(savedDraft);
        messageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence value, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence value, int start, int before, int count) {
                getPreferences(MODE_PRIVATE).edit()
                        .putString(draftKey, value.toString())
                        .apply();
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        configureSimSelector();
        FloatingActionButton send = findViewById(R.id.btnSendMessage);
        send.setOnClickListener(v -> {
            String body = messageInput.getText().toString().trim();
            if (!body.isEmpty()) {
                viewModel.send(body, selectedSubscriptionId);
                messageInput.setText("");
            }
        });

        String initialMessage = getIntent().getStringExtra(EXTRA_INITIAL_MESSAGE);
        if (savedInstanceState == null && initialMessage != null && !initialMessage.isBlank()) {
            messageInput.setText(initialMessage);
            messageInput.setSelection(initialMessage.length());
            getIntent().removeExtra(EXTRA_INITIAL_MESSAGE);
        }
    }

    private void configureSimSelector() {
        selectedSubscriptionId = SubscriptionManager.getDefaultSmsSubscriptionId();
        MaterialButton button = findViewById(R.id.btnSim);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) return;
        SubscriptionManager manager = getSystemService(SubscriptionManager.class);
        if (manager == null) return;
        List<SubscriptionInfo> subscriptions;
        try {
            subscriptions = manager.getActiveSubscriptionInfoList();
        } catch (SecurityException denied) {
            return;
        }
        if (subscriptions == null || subscriptions.size() < 2) return;
        if (subscriptions.stream().noneMatch(
                item -> item.getSubscriptionId() == selectedSubscriptionId)) {
            selectedSubscriptionId = subscriptions.get(0).getSubscriptionId();
        }
        updateSimButton(button, subscriptions);
        button.setVisibility(View.VISIBLE);
        button.setOnClickListener(v -> {
            String[] labels = subscriptions.stream()
                    .map(item -> item.getDisplayName().toString())
                    .toArray(String[]::new);
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.choose_sim)
                    .setItems(labels, (dialog, which) -> {
                        selectedSubscriptionId = subscriptions.get(which).getSubscriptionId();
                        updateSimButton(button, subscriptions);
                    })
                    .show();
        });
    }

    private void updateSimButton(MaterialButton button, List<SubscriptionInfo> subscriptions) {
        for (SubscriptionInfo subscription : subscriptions) {
            if (subscription.getSubscriptionId() == selectedSubscriptionId) {
                button.setText(subscription.getDisplayName());
                return;
            }
        }
        button.setText(R.string.sim_default);
    }

    private void showCallOptions() {
        Intent dial = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", address, null));
        startActivity(dial);
    }

    @Override
    protected void onResume() {
        super.onResume();
        visibleConversationKey = conversationKey;
        NotificationManagerCompat.from(this).cancel(SmsReceiver.notificationId(conversationKey));
        if (viewModel != null) {
            viewModel.markRead();
            viewModel.refresh();
        }
    }

    @Override
    protected void onPause() {
        if (conversationKey != null && conversationKey.equals(visibleConversationKey)) {
            visibleConversationKey = null;
        }
        super.onPause();
    }
}
