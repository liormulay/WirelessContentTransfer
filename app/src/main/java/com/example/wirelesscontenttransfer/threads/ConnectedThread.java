package com.example.wirelesscontenttransfer.threads;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.reactivex.rxjava3.subjects.BehaviorSubject;

import static android.content.ContentValues.TAG;

public class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private byte[] mmBuffer; // mmBuffer store for the stream
    private BehaviorSubject<Byte[]> bytesSubject;

    public ConnectedThread(BluetoothSocket socket, BehaviorSubject<Byte[]> bytesSubject) {
        mmSocket = socket;
        this.bytesSubject = bytesSubject;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the input and output streams; using temp objects because
        // member streams are final.
        try {
            tmpIn = socket.getInputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating input stream", e);
        }
        try {
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating output stream", e);
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    public void run() {
        mmBuffer = new byte[1024];
        int numBytes; // bytes returned from read()

        // Keep listening to the InputStream until an exception occurs.
        while (true) {
            try {
                // Read from the InputStream.
                numBytes = mmInStream.read(mmBuffer);
                Byte[] bytes = createBytes(mmBuffer, numBytes);
                bytesSubject.onNext(bytes);
            } catch (IOException e) {
                Log.d(TAG, "Input stream was disconnected", e);
                break;
            }
        }
    }

    private Byte[] createBytes(byte[] mmBuffer, int numBytes) {
        Byte[] bytes = new Byte[numBytes];
        for (int i = 0; i < numBytes; i++) {
            bytes[i] = mmBuffer[i];
        }
        return bytes;
    }

    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
            Log.d(TAG, "wrote bytes");
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when sending data", e);

        }
    }

}