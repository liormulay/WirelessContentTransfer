package com.example.wirelesscontenttransfer.viewmodels;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.lifecycle.ViewModel;

import com.example.wirelesscontenttransfer.threads.ConnectedThread;
import com.example.wirelesscontenttransfer.models.Contact;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

import static com.example.wirelesscontenttransfer.views.MainActivity.TAG;

public class WirelessViewModel extends ViewModel {
    private final BluetoothAdapter bluetoothAdapter;
    private BehaviorSubject<Byte[]> bytesSubject = BehaviorSubject.create();
    private BluetoothSocket socket;

    public WirelessViewModel(BluetoothAdapter bluetoothAdapter) {
        this.bluetoothAdapter = bluetoothAdapter;
        bytesSubject.subscribe(bytes -> {
            ArrayList<Contact> contacts = bytesToContacts(bytes);
            for (Contact contact : contacts) {
                Log.d(TAG, "contact: " + contact.getName() + contact.getNumber());
            }
        });
    }

    public Single<List<BluetoothDevice>> getPairedDevices() {
        return Observable.fromIterable(bluetoothAdapter.getBondedDevices())
                .toList()
                .subscribeOn(Schedulers.io());
    }

    public ConnectedThread manageMyConnectedSocket(BluetoothSocket socket) {
        this.socket = socket;
        ConnectedThread mConnectedThread = new ConnectedThread(socket, bytesSubject);
        mConnectedThread.start();
        return mConnectedThread;
    }

    public ConnectedThread manageMyConnectedSocket() {
        return manageMyConnectedSocket(this.socket);
    }

    private byte[] fetchContacts(Context context) {
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER};
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(uri, projection, null, null, null);
        ArrayList<Contact> contacts = new ArrayList<>();
        while (cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            contacts.add(new Contact(name, number));
            break;
        }
        return contactsToBytes(contacts);

    }


    private byte[] contactsToBytes(ArrayList<Contact> contacts) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(bos);
            oos.writeObject(contacts);
            oos.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();

        }
        return new byte[0];
    }

    public static ArrayList<Contact> bytesToContacts(Byte[] data) throws IOException, ClassNotFoundException {
        byte[] buf = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            buf[i] = data[i];
        }
        ByteArrayInputStream in = new ByteArrayInputStream(buf);
        ObjectInputStream is = new ObjectInputStream(in);
        return ((ArrayList<Contact>) is.readObject());
    }

    public void transferContacts(Context context) {
        byte[] bytes = fetchContacts(context);
        ConnectedThread mConnectedThread = manageMyConnectedSocket();
        mConnectedThread.write(bytes);
    }
}
