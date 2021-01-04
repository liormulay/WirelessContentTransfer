package com.example.wirelesscontenttransfer;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.example.wirelesscontenttransfer.models.Contact;
import com.example.wirelesscontenttransfer.threads.AcceptThread;
import com.example.wirelesscontenttransfer.threads.ConnectThread;
import com.example.wirelesscontenttransfer.threads.ConnectedThread;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.UUID;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

import static com.example.wirelesscontenttransfer.views.MainActivity.TAG;

public class MyBluetoothService {

    private ConnectThread mConnectThread;
    private AcceptThread mInsecureAcceptThread;
    private ConnectedThread mConnectedThread;
    private final BluetoothAdapter bluetoothAdapter;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private BluetoothSocket socket;
    private final BehaviorSubject<Byte[]> bytesSubject;
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public MyBluetoothService(BluetoothAdapter bluetoothAdapter, BehaviorSubject<Byte[]> bytesSubject) {
        this.bluetoothAdapter = bluetoothAdapter;
        this.bytesSubject = bytesSubject;
    }

    public synchronized void start(BehaviorSubject<BluetoothSocket> socketSubject) {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread(bluetoothAdapter, socketSubject);
            mInsecureAcceptThread.start();
            compositeDisposable.add(socketSubject
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::manageMyConnectedSocket));
        }
    }

    public void manageMyConnectedSocket(BluetoothSocket socket) {
        this.socket = socket;
        startConnected(socket);
    }

    private ConnectedThread manageMyConnectedSocket() {
        return startConnected(this.socket);
    }

    private ConnectedThread startConnected(BluetoothSocket socket) {
        mConnectedThread = new ConnectedThread(socket, bytesSubject);
        mConnectedThread.start();
        return mConnectedThread;
    }


    public void transferContacts(ArrayList<Contact> contacts) {
        ConnectedThread mConnectedThread = manageMyConnectedSocket();
        for (Contact contact : contacts) {
            mConnectedThread.write(contactToBytes(contact));
        }
    }

    private static byte[] contactToBytes(Contact contact) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(bos);
            oos.writeObject(contact);
            oos.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();

        }
        return new byte[0];
    }

    /**
     * Try to connect to another device
     * @param device that clicked
     * @param connectSubject emit {@link BluetoothSocket} when connection succeed
     * @param failedSubject emit {@link Exception} when connection failed
     */
    public void statConnect(BluetoothDevice device, BehaviorSubject<BluetoothSocket> connectSubject,
                            BehaviorSubject<Exception> failedSubject) {
        mConnectThread = new ConnectThread(device, bluetoothAdapter, connectSubject, failedSubject);
        mConnectThread.start();
    }

}