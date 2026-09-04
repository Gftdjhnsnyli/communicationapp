package com.example.communicationapp;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.communicationapp.data.MessageEntity;
import com.example.communicationapp.data.SmsRepository;

import java.util.List;

public class ChatViewModel extends ViewModel {
    private final SmsRepository repository;
    private final String conversationKey;
    private final String address;
    private final LiveData<List<MessageEntity>> messages;

    private ChatViewModel(SmsRepository repository, String conversationKey, String address) {
        this.repository = repository;
        this.conversationKey = conversationKey;
        this.address = address;
        messages = repository.observeMessages(conversationKey);
    }

    public LiveData<List<MessageEntity>> getMessages() {
        return messages;
    }

    public void send(String body) {
        send(body, android.telephony.SubscriptionManager.getDefaultSmsSubscriptionId());
    }

    public void send(String body, int subscriptionId) {
        if (body != null && !body.trim().isEmpty()) {
            repository.sendMessage(address, body.trim(), subscriptionId);
        }
    }

    public void refresh() {
        repository.requestSync();
    }

    public void markRead() {
        repository.markConversationRead(conversationKey);
    }

    public static class Factory implements ViewModelProvider.Factory {
        private final SmsRepository repository;
        private final String conversationKey;
        private final String address;

        public Factory(SmsRepository repository, String conversationKey, String address) {
            this.repository = repository;
            this.conversationKey = conversationKey;
            this.address = address;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(ChatViewModel.class)) {
                throw new IllegalArgumentException("Unknown ViewModel class");
            }
            return (T) new ChatViewModel(repository, conversationKey, address);
        }
    }
}
