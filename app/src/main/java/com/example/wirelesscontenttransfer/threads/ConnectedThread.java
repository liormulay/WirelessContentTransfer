package com.example.wirelesscontenttransfer.threads;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.reactivex.rxjava3.subjects.BehaviorSubject;

import static android.content.ContentValues.TAG;

/**
 * Use this thread to transfer data between devices
 */
public class ConnectedThread extends Thread {
    /**
     * Use this to read data
     */
    private final InputStream mmInStream;
    /**
     * use this to write data
     */
    private final OutputStream mmOutStream;
    /**
     * Emit the bytes that accepted from source
     */
    private final BehaviorSubject<Byte[]> bytesSubject;

    public ConnectedThread(BluetoothSocket socket, BehaviorSubject<Byte[]> bytesSubject) {
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
        // mmBuffer store for the stream
        byte[] mmBuffer = new byte[1024];
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

    /**
     * Create array of {@link Byte} objects
     * @param mmBuffer the array of primitive bytes
     * @param numBytes the size of bytes that need
     * @return array of {@link Byte} objects
     */
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