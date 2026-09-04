package com.example.communicationapp;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.communicationapp.data.ConversationEntity;

public class ConversationAdapter extends ListAdapter<ConversationEntity, ConversationAdapter.ViewHolder> {
    public interface OnConversationClickListener {
        void onConversationClick(ConversationEntity conversation);
    }

    private static final DiffUtil.ItemCallback<ConversationEntity> DIFF =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull ConversationEntity oldItem,
                                               @NonNull ConversationEntity newItem) {
                    return oldItem.conversationKey.equals(newItem.conversationKey);
                }

                @Override
                public boolean areContentsTheSame(@NonNull ConversationEntity oldItem,
                                                  @NonNull ConversationEntity newItem) {
                    return oldItem.address.equals(newItem.address)
                            && oldItem.getDisplayName().equals(newItem.getDisplayName())
                            && oldItem.lastMessage.equals(newItem.lastMessage)
                            && oldItem.lastMessageAt == newItem.lastMessageAt
                            && oldItem.unreadCount == newItem.unreadCount;
                }
            };

    private final OnConversationClickListener listener;

    public ConversationAdapter(OnConversationClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConversationEntity conversation = getItem(position);
        holder.name.setText(conversation.getDisplayName());
        holder.lastMessage.setText(conversation.lastMessage);
        holder.time.setText(DateUtils.getRelativeTimeSpanString(
                conversation.lastMessageAt, System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS));
        if (conversation.unreadCount > 0) {
            holder.unread.setText(String.valueOf(conversation.unreadCount));
            holder.unread.setVisibility(View.VISIBLE);
        } else {
            holder.unread.setVisibility(View.GONE);
        }
        holder.itemView.setOnClickListener(v -> listener.onConversationClick(conversation));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView lastMessage;
        final TextView time;
        final TextView unread;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textContactName);
            lastMessage = itemView.findViewById(R.id.textLastMessage);
            time = itemView.findViewById(R.id.textTime);
            unread = itemView.findViewById(R.id.textUnreadCount);
        }
    }
}
