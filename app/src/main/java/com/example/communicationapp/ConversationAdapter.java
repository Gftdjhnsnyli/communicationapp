package com.example.communicationapp;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    private List<Conversation> conversations;
    private OnConversationClickListener listener;

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
    }

    public ConversationAdapter(List<Conversation> conversations, OnConversationClickListener listener) {
        this.conversations = conversations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Conversation conversation = conversations.get(position);
        holder.textContactName.setText(conversation.getContactName());
        holder.textLastMessage.setText(conversation.getLastMessage());
        
        String timeAgo = DateUtils.getRelativeTimeSpanString(conversation.getDate(), 
                System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString();
        holder.textTime.setText(timeAgo);

        holder.itemView.setOnClickListener(v -> listener.onConversationClick(conversation));
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textContactName, textLastMessage, textTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textContactName = itemView.findViewById(R.id.textContactName);
            textLastMessage = itemView.findViewById(R.id.textLastMessage);
            textTime = itemView.findViewById(R.id.textTime);
        }
    }
}