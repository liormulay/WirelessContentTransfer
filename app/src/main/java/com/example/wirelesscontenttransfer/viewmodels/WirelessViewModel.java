package com.example.wirelesscontenttransfer.viewmodels;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;

import com.example.wirelesscontenttransfer.models.Contact;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static com.example.wirelesscontenttransfer.views.MainActivity.TAG;

public class WirelessViewModel extends ViewModel {
    private final BluetoothAdapter bluetoothAdapter;

    public WirelessViewModel(BluetoothAdapter bluetoothAdapter) {
        this.bluetoothAdapter = bluetoothAdapter;
    }

    public Single<List<BluetoothDevice>> getPairedDevices() {
        return Observable.fromIterable(bluetoothAdapter.getBondedDevices())
                .toList()
                .subscribeOn(Schedulers.io());
    }

    public byte[] fetchContacts(Context context) {
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
}
