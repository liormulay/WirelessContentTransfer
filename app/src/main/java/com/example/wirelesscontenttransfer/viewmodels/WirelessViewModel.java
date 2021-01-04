package com.example.wirelesscontenttransfer.viewmodels;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import androidx.lifecycle.ViewModel;

import com.example.wirelesscontenttransfer.MyBluetoothService;
import com.example.wirelesscontenttransfer.models.Contact;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class WirelessViewModel extends ViewModel {
    private final BluetoothAdapter bluetoothAdapter;
    /**
     * Mange the bluetooth business
     */
    private final MyBluetoothService myBluetoothService;
    /**
     * Emit the bytes that accepted from source
     */
    private final BehaviorSubject<Byte[]> bytesSubject = BehaviorSubject.create();

    public WirelessViewModel(BluetoothAdapter bluetoothAdapter) {
        this.bluetoothAdapter = bluetoothAdapter;
        myBluetoothService = new MyBluetoothService(bluetoothAdapter, bytesSubject);
    }

    /**
     * Emit the {@link Contact} that accepted from source
     */
    public Observable<Contact> getReceivedContacts() {
        return bytesSubject
                .subscribeOn(Schedulers.io())
                .map(WirelessViewModel::bytesToContact);
    }

    /**
     * Start listening for bluetooth requests
     * @param socketSubject emit {@link BluetoothSocket} when connection succeed
     */
    public void startListening(BehaviorSubject<BluetoothSocket> socketSubject) {
        myBluetoothService.start(socketSubject);
    }

    /**
     * Called when device is clicked
     * @param device that clicked
     * @param connectSubject emit {@link BluetoothSocket} when connection succeed
     * @param failedSubject emit {@link Exception} when connection failed
     */
    public void onDeviceClicked(BluetoothDevice device, BehaviorSubject<BluetoothSocket> connectSubject,
                                BehaviorSubject<Exception> failedSubject) {
        myBluetoothService.statConnect(device, connectSubject, failedSubject);
    }

    /**
     * Return the bounded devices
     * @return {@link Single} that emit them
     */
    public Single<List<BluetoothDevice>> getPairedDevices() {
        return Observable.fromIterable(bluetoothAdapter.getBondedDevices())
                .toList()
                .subscribeOn(Schedulers.io());
    }

    /**
     *
     * @param context
     * @return the entire phone book
     */
    private ArrayList<Contact> fetchContacts(Context context) {
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER};
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(uri, projection, null, null, null);
        ArrayList<Contact> contacts = new ArrayList<>();
        while (cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            contacts.add(new Contact(name, number));
        }
        return contacts;

    }

    /**
     * Convert {@link Byte} array to {@link Contact}
     * @param data the {@link Byte} array
     * @return the parsed {@link Contact}
     */
    private static Contact bytesToContact(Byte[] data) {
        byte[] buf = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            buf[i] = data[i];
        }
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(buf);
            ObjectInputStream is = new ObjectInputStream(in);
            Object object = is.readObject();
            if (object instanceof Contact)
                return (Contact) object;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return new Contact("exception in contact's read","");
    }

    /**
     * Call this when you want to send contact to another device
     * @param context
     */
    public void transferContacts(Context context) {
        ArrayList<Contact> contacts = fetchContacts(context);
        myBluetoothService.transferContacts(contacts);

    }

    /**
     * Called when device is connect
     * @param bluetoothSocket
     */
    public void onConnect(BluetoothSocket bluetoothSocket) {
        myBluetoothService.manageMyConnectedSocket(bluetoothSocket);
    }
}
