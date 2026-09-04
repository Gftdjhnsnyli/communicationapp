package com.example.communicationapp;

import android.Manifest;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Map;

public class ConversationListActivity extends AppCompatActivity {
    private ConversationListViewModel viewModel;
    private TextView emptyState;

    private final ActivityResultLauncher<Intent> roleLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (hasSmsRole()) requestPermissions();
                else showRoleRequired();
            });

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), this::permissionsCompleted);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation_list);

        RecyclerView recyclerView = findViewById(R.id.recyclerViewConversations);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        ConversationAdapter adapter = new ConversationAdapter(conversation -> {
            Intent chat = new Intent(this, ChatActivity.class)
                    .putExtra(ChatActivity.EXTRA_CONVERSATION_KEY, conversation.conversationKey)
                    .putExtra(ChatActivity.EXTRA_ADDRESS, conversation.address)
                    .putExtra(ChatActivity.EXTRA_CONTACT_NAME, conversation.getDisplayName());
            startActivity(chat);
        });
        recyclerView.setAdapter(adapter);
        emptyState = findViewById(R.id.textEmptyState);

        viewModel = new ViewModelProvider(this).get(ConversationListViewModel.class);
        viewModel.getConversations().observe(this, conversations -> {
            adapter.submitList(conversations);
            emptyState.setVisibility(conversations.isEmpty() ? View.VISIBLE : View.GONE);
        });

        EditText search = findViewById(R.id.searchInput);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence value, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence value, int start, int before, int count) {
                viewModel.setQuery(value.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        ExtendedFloatingActionButton newMessage = findViewById(R.id.fabNewMessage);
        newMessage.setOnClickListener(v -> {
            if (hasSmsRole()) startActivity(new Intent(this, MainActivity.class));
            else ensureSmsRole();
        });
        ensureSmsRole();
    }

    private void ensureSmsRole() {
        RoleManager roleManager = getSystemService(RoleManager.class);
        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)
                && !roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.sms_role_title)
                    .setMessage(R.string.sms_role_mms_warning)
                    .setPositiveButton(R.string.continue_label, (dialog, which) ->
                            roleLauncher.launch(
                                    roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)))
                    .setNegativeButton(R.string.not_now, (dialog, which) -> showRoleRequired())
                    .show();
        } else {
            requestPermissions();
        }
    }

    private boolean hasSmsRole() {
        RoleManager roleManager = getSystemService(RoleManager.class);
        return roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)
                && roleManager.isRoleHeld(RoleManager.ROLE_SMS);
    }

    private void showRoleRequired() {
        emptyState.setText(R.string.sms_access_required);
        emptyState.setVisibility(View.VISIBLE);
    }

    private void requestPermissions() {
        permissionLauncher.launch(new String[]{
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.RECEIVE_MMS,
                Manifest.permission.RECEIVE_WAP_PUSH,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.POST_NOTIFICATIONS
        });
    }

    private void permissionsCompleted(Map<String, Boolean> results) {
        if (Boolean.TRUE.equals(results.get(Manifest.permission.READ_SMS))
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            ((CommunicationApp) getApplication()).startSmsObservation();
            emptyState.setText(R.string.no_conversations);
            com.example.communicationapp.data.SmsRepository.getInstance(this)
                    .refreshContactNames();
            viewModel.refresh();
        } else {
            emptyState.setText(R.string.sms_access_required);
            emptyState.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!hasSmsRole()) {
            showRoleRequired();
        } else if (viewModel != null && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            viewModel.refresh();
        }
    }
}
