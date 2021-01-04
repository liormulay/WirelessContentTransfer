package com.example.wirelesscontenttransfer.threads;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;

import io.reactivex.rxjava3.subjects.BehaviorSubject;

import static com.example.wirelesscontenttransfer.MyBluetoothService.MY_UUID;
import static com.example.wirelesscontenttransfer.views.MainActivity.TAG;

/**
 * Use this thread to accept connection from device that request it
 */
public class AcceptThread extends Thread {

    private final BluetoothServerSocket mmServerSocket;
    private static final String NAME = "WirelessApp";
    /**
     * Emit {@link BluetoothSocket} when connection succeed
     */
    private final BehaviorSubject<BluetoothSocket> socketSubject;

    public AcceptThread(BluetoothAdapter bluetoothAdapter, BehaviorSubject<BluetoothSocket> socketSubject) {
        this.socketSubject = socketSubject;
        // Use a temporary object that is later assigned to mmServerSocket
        // because mmServerSocket is final.
        BluetoothServerSocket tmp = null;
        try {
            // MY_UUID is the app's UUID string, also used by the client code.
            tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
        } catch (IOException e) {
            Log.e(TAG, "Socket's listen() method failed", e);
        }
        mmServerSocket = tmp;
    }

    public void run() {
        BluetoothSocket socket = null;
        // Keep listening until exception occurs or a socket is returned.
        while (true) {
            try {
                socket = mmServerSocket.accept();
            } catch (IOException e) {
                Log.e(TAG, "Socket's accept() method failed", e);
                break;
            }

            if (socket != null) {
                Log.d(TAG, "socket.toString() " + socket.toString());
                // A connection was accepted. Perform work associated with
                // the connection in a separate thread.
                socketSubject.onNext(socket);
                try {
                    mmServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
            Log.d(TAG, "socket is null");
        }
    }

    // Closes the connect socket and causes the thread to finish.
    public void cancel() {
        try {
            mmServerSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the connect socket", e);
        }
    }
}
