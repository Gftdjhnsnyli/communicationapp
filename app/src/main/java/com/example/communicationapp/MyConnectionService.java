package com.example.communicationapp;

import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.ConnectionService;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;

public class MyConnectionService extends ConnectionService {

    public static MyConnection currentConnection;

    @Override
    public Connection onCreateOutgoingConnection(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        MyConnection connection = new MyConnection(this);
        connection.setAddress(request.getAddress(), TelecomManager.PRESENTATION_ALLOWED);
        connection.setVideoState(request.getVideoState());
        connection.setConnectionProperties(Connection.PROPERTY_SELF_MANAGED);
        connection.setInitializing();
        
        // Signal that the connection is active
        connection.setActive();

        currentConnection = connection;
        return connection;
    }

    @Override
    public void onCreateOutgoingConnectionFailed(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        super.onCreateOutgoingConnectionFailed(connectionManagerPhoneAccount, request);
        currentConnection = null;
    }

    @Override
    public Connection onCreateIncomingConnection(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        MyConnection connection = new MyConnection(this);
        connection.setAddress(request.getAddress(), TelecomManager.PRESENTATION_ALLOWED);
        connection.setConnectionProperties(Connection.PROPERTY_SELF_MANAGED);
        currentConnection = connection;
        return connection;
    }

    @Override
    public void onCreateIncomingConnectionFailed(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        super.onCreateIncomingConnectionFailed(connectionManagerPhoneAccount, request);
        currentConnection = null;
    }
}
