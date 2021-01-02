package com.example.wirelesscontenttransfer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;

    public static final String TAG = "MY_APP_DEBUG_TAG";
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter bluetoothAdapter;

    private MyBluetoothService.ConnectedThread mConnectedThread;
    private ConnectThread mConnectThread;
    private AcceptThread mInsecureAcceptThread;
    private WirelessViewModel wirelessViewModel;
    private RecyclerView pairedRecycler;
    private DevicesAdapter pairedAdapter;
    private RecyclerView availableRecycler;
    private DevicesAdapter availableAdapter;
    private BehaviorSubject<BluetoothDevice> clickSubject = BehaviorSubject.create();
    private BehaviorSubject<BluetoothSocket> connectSubject = BehaviorSubject.create();
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Device doesn't support Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        wirelessViewModel = new WirelessViewModel(bluetoothAdapter);

        askLocationPermission();

        initRecyclers();

        compositeDisposable.add(wirelessViewModel.getPairedDevices()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bluetoothDevices -> pairedAdapter.setDevices(bluetoothDevices)));

        compositeDisposable.add(clickSubject
                .subscribe(device -> {
                    ConnectThread connectThread = new ConnectThread(device, bluetoothAdapter, connectSubject);
                    connectThread.start();
                }));

        compositeDisposable.add(connectSubject
                .subscribe(bluetoothSocket -> {
                    Log.d(TAG, "Connect Success");
                    manageMyConnectedSocket(bluetoothSocket);
                }));

        Log.d(TAG, bluetoothAdapter.startDiscovery() + "");
        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        start();
    }

    private void initRecyclers() {
        pairedRecycler = findViewById(R.id.paired_recycler);
        availableRecycler = findViewById(R.id.available_recycler);
        pairedAdapter = new DevicesAdapter(this, clickSubject);
        availableAdapter = new DevicesAdapter(this, clickSubject);
        initRecycler(pairedRecycler, pairedAdapter);
        initRecycler(availableRecycler, availableAdapter);
    }

    private void initRecycler(RecyclerView recyclerView, DevicesAdapter devicesAdapter) {
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(devicesAdapter);
    }

    private void askLocationPermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                availableAdapter.addDevice(device);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d(TAG, "deviceName " + deviceName);

            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            Toast.makeText(this, "Enabling Bluetooth succeeds", Toast.LENGTH_SHORT).show();
        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Bluetooth was not enabled", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver);
        compositeDisposable.clear();
    }


    private void manageMyConnectedSocket(BluetoothSocket socket) {
        mConnectedThread = new MyBluetoothService.ConnectedThread(socket);
        mConnectedThread.start();
    }


    public synchronized void start() {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mInsecureAcceptThread == null) {
            BehaviorSubject<BluetoothSocket> socketSubject = BehaviorSubject.create();
            mInsecureAcceptThread = new AcceptThread(bluetoothAdapter, socketSubject);
            mInsecureAcceptThread.start();
            compositeDisposable.add(socketSubject
                    .subscribe(this::manageMyConnectedSocket));
        }
    }


}