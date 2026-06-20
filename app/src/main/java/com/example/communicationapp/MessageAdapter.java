package com.example.communicationapp;

import android.provider.Telephony;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<Message> messages;

    public MessageAdapter(List<Message> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if (message.getType() == Telephony.Sms.MESSAGE_TYPE_SENT) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
            return new SentViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        String timeString = DateFormat.format("h:mm a", message.getDate()).toString();

        if (holder instanceof SentViewHolder) {
            ((SentViewHolder) holder).textMessage.setText(message.getBody());
            ((SentViewHolder) holder).textTime.setText(timeString);
            
            // Delivery report logic
            int status = message.getStatus();
            if (status == Telephony.Sms.STATUS_COMPLETE) {
                ((SentViewHolder) holder).imageStatus.setImageResource(android.R.drawable.checkbox_on_background);
                ((SentViewHolder) holder).imageStatus.setVisibility(View.VISIBLE);
            } else if (status == Telephony.Sms.STATUS_PENDING) {
                ((SentViewHolder) holder).imageStatus.setImageResource(android.R.drawable.ic_menu_send);
                ((SentViewHolder) holder).imageStatus.setVisibility(View.VISIBLE);
            } else {
                ((SentViewHolder) holder).imageStatus.setVisibility(View.GONE);
            }
        } else {
            ((ReceivedViewHolder) holder).textMessage.setText(message.getBody());
            ((ReceivedViewHolder) holder).textTime.setText(timeString);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage, textTime;
        ImageView imageStatus;

        SentViewHolder(View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textMessage);
            textTime = itemView.findViewById(R.id.textTime);
            imageStatus = itemView.findViewById(R.id.imageStatus);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage, textTime;

        ReceivedViewHolder(View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textMessage);
            textTime = itemView.findViewById(R.id.textTime);
        }
    }
}