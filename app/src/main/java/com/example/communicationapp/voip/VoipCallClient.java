package com.example.communicationapp.voip;

import android.content.Context;

/** Backend adapter for WebRTC signaling. Configuration is required before calls can start. */
public final class VoipCallClient {
    private static final VoipCallClient INSTANCE = new VoipCallClient();

    private VoipCallClient() {}

    public static VoipCallClient getInstance() {
        return INSTANCE;
    }

    public boolean startCall(Context context, String address) {
        // A real implementation needs the backend URL, auth, signaling schema and ICE servers.
        return false;
    }
}
