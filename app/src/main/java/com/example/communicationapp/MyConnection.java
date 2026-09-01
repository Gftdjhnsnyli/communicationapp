package com.example.communicationapp;

import android.content.Context;
import android.content.Intent;
import android.telecom.Connection;
import android.telecom.DisconnectCause;

public class MyConnection extends Connection {

    private final Context context;

    public MyConnection(Context context) {
        this.context = context;
        setConnectionProperties(PROPERTY_SELF_MANAGED);
        setAudioModeIsVoip(true);
        // Ensure we start our UI for outgoing calls too if needed, 
        // though placeCall handles the initial trigger.
    }

    @Override
    public void onStateChanged(int state) {
        super.onStateChanged(state);
        if (state == STATE_ACTIVE || state == STATE_DIALING) {
            Intent intent = new Intent(context, CallActivity.class);
            intent.putExtra("is_incoming", false);
            intent.putExtra("address", getAddress().getSchemeSpecificPart());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(intent);
        }
    }

    @Override
    public void onShowIncomingCallUi() {
        // This is called when we need to show our incoming call UI.
        Intent intent = new Intent(context, CallActivity.class);
        intent.putExtra("is_incoming", true);
        intent.putExtra("address", getAddress().getSchemeSpecificPart());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    public void onAnswer() {
        setActive();
        // Notify CallActivity if it's already running
        Intent intent = new Intent("com.example.communicationapp.CALL_STATE_CHANGED");
        intent.putExtra("state", "active");
        context.sendBroadcast(intent);
    }

    @Override
    public void onReject() {
        setDisconnected(new DisconnectCause(DisconnectCause.REJECTED));
        destroy();
    }

    @Override
    public void onDisconnect() {
        setDisconnected(new DisconnectCause(DisconnectCause.LOCAL));
        destroy();
    }

    @Override
    public void onAbort() {
        setDisconnected(new DisconnectCause(DisconnectCause.CANCELED));
        destroy();
    }
}
