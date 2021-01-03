package com.example.wirelesscontenttransfer;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.example.wirelesscontenttransfer.threads.AcceptThread;
import com.example.wirelesscontenttransfer.threads.ConnectThread;
import com.example.wirelesscontenttransfer.threads.ConnectedThread;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

import static com.example.wirelesscontenttransfer.views.MainActivity.TAG;

public class MyBluetoothService {

    private ConnectThread mConnectThread;
    private AcceptThread mInsecureAcceptThread;
    private final BluetoothAdapter bluetoothAdapter;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private BluetoothSocket socket;
    private final BehaviorSubject<Byte[]> bytesSubject;


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
        ConnectedThread mConnectedThread = new ConnectedThread(socket, bytesSubject);
        mConnectedThread.start();
        return mConnectedThread;
    }


    public void transferContacts(byte[] bytes) {
        ConnectedThread mConnectedThread = manageMyConnectedSocket();
        mConnectedThread.write(bytes);
    }


    public void statConnect(BluetoothDevice device, BehaviorSubject<BluetoothSocket> connectSubject,
                            BehaviorSubject<Exception> failedSubject) {
        mConnectThread = new ConnectThread(device, bluetoothAdapter, connectSubject, failedSubject);
        mConnectThread.start();
    }
}