package com.example.communicationapp;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.communicationapp.data.MessageEntity;
import com.example.communicationapp.data.SmsState;

public class MessageAdapter extends ListAdapter<MessageEntity, RecyclerView.ViewHolder> {
    public interface RetryListener {
        void onRetry(MessageEntity message);
    }
    private static final int SENT = 1;
    private static final int RECEIVED = 2;

    private static final DiffUtil.ItemCallback<MessageEntity> DIFF = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull MessageEntity oldItem,
                                       @NonNull MessageEntity newItem) {
            return oldItem.localId.equals(newItem.localId);
        }

        @Override
        public boolean areContentsTheSame(@NonNull MessageEntity oldItem,
                                          @NonNull MessageEntity newItem) {
            return oldItem.body.equals(newItem.body)
                    && oldItem.date == newItem.date
                    && oldItem.sendState == newItem.sendState
                    && oldItem.deliveryState == newItem.deliveryState
                    && oldItem.read == newItem.read;
        }
    };

    private final RetryListener retryListener;

    public MessageAdapter(RetryListener retryListener) {
        super(DIFF);
        this.retryListener = retryListener;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).outgoing ? SENT : RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == SENT
                ? R.layout.item_message_sent : R.layout.item_message_received;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return viewType == SENT ? new SentViewHolder(view) : new ReceivedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageEntity message = getItem(position);
        String time = DateFormat.format("h:mm a", message.date).toString();
        if (holder instanceof SentViewHolder) {
            SentViewHolder sent = (SentViewHolder) holder;
            sent.message.setText(message.body);
            sent.time.setText(time);
            sent.status.setVisibility(View.VISIBLE);
            if (message.sendState == SmsState.SEND_FAILED
                    || message.deliveryState == SmsState.DELIVERY_FAILED) {
                sent.status.setImageResource(android.R.drawable.stat_notify_error);
                sent.status.setContentDescription(sent.itemView.getContext()
                        .getString(R.string.status_failed));
                sent.status.setOnClickListener(v -> retryListener.onRetry(message));
            } else if (message.deliveryState == SmsState.DELIVERY_COMPLETE) {
                sent.status.setImageResource(android.R.drawable.checkbox_on_background);
                sent.status.setContentDescription(sent.itemView.getContext()
                        .getString(R.string.status_delivered));
            } else if (message.sendState == SmsState.SEND_SENT) {
                sent.status.setImageResource(android.R.drawable.checkbox_off_background);
                sent.status.setContentDescription(sent.itemView.getContext()
                        .getString(R.string.status_sent));
            } else {
                sent.status.setImageResource(android.R.drawable.ic_menu_upload);
                sent.status.setContentDescription(sent.itemView.getContext()
                        .getString(R.string.status_sending));
            }
            if (message.sendState != SmsState.SEND_FAILED
                    && message.deliveryState != SmsState.DELIVERY_FAILED) {
                sent.status.setOnClickListener(null);
            }
        } else {
            ReceivedViewHolder received = (ReceivedViewHolder) holder;
            received.message.setText(message.body);
            received.time.setText(time);
        }
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        final TextView message;
        final TextView time;
        final ImageView status;

        SentViewHolder(View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.textMessage);
            time = itemView.findViewById(R.id.textTime);
            status = itemView.findViewById(R.id.imageStatus);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        final TextView message;
        final TextView time;

        ReceivedViewHolder(View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.textMessage);
            time = itemView.findViewById(R.id.textTime);
        }
    }
}
