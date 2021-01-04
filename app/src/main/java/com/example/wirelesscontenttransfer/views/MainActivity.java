package com.example.wirelesscontenttransfer.views;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wirelesscontenttransfer.R;
import com.example.wirelesscontenttransfer.adapters.DevicesAdapter;
import com.example.wirelesscontenttransfer.listeners.AcceptConnectListener;
import com.example.wirelesscontenttransfer.listeners.ConnectListener;
import com.example.wirelesscontenttransfer.viewmodels.WirelessViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;

    public static final String TAG = "MY_APP_DEBUG_TAG";
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 2;

    private BluetoothAdapter bluetoothAdapter;
    private WirelessViewModel viewModel;
    /**
     * Represent the paired devices
     */
    private RecyclerView pairedRecycler;
    private DevicesAdapter pairedAdapter;
    /**
     * Represent the available devices
     */
    private RecyclerView availableRecycler;
    private DevicesAdapter availableAdapter;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    /**
     * Visible when try to connect to another device
     */
    private ProgressBar progressBar;
    /**
     * Notify when connection happened
     */
    private ConnectListener connectListener;
    /**
     * User click it to be the source that send data
     */
    private AppCompatButton chooseSource;

    /**
     * Notify when connection is failed
     */
    private final BehaviorSubject<Exception> failedSubject = BehaviorSubject.create();
    /**
     * Notify when device accept connection from another device
     */
    private final BehaviorSubject<BluetoothSocket> acceptConnect = BehaviorSubject.create();
    /**
     * Notify when device from the list is clicked
     */
    private final BehaviorSubject<Pair<BluetoothDevice, ConnectListener>> clickSubject = BehaviorSubject.create();
    /**
     * Notify after device succeed to connect to another device
     */
    private final BehaviorSubject<BluetoothSocket> connectSubject = BehaviorSubject.create();
    /**
     * Shown when device is received data
     */
    private AppCompatTextView receivingDataTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Device doesn't support Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            doIfBTEnabled();
        }
        checkReadContactsPermission();
    }

    private void findViews() {
        progressBar = findViewById(R.id.progressBar);
        chooseSource = findViewById(R.id.choose_source);
        pairedRecycler = findViewById(R.id.paired_recycler);
        availableRecycler = findViewById(R.id.available_recycler);
        receivingDataTextView = findViewById(R.id.receiving_data_textView);
    }

    /**
     * Check if device has permission to read his phone book, if not ask permission
     * @return true if device has already permission
     */
    private boolean checkReadContactsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                // Permission is granted
                Toast.makeText(this, "Until you grant the permission, we canot display the names", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void doIfBTEnabled() {
        chooseSource.setOnClickListener(v -> {
            if (checkReadContactsPermission()) {
                viewModel.transferContacts(this);
                chooseSource.setVisibility(View.GONE);
            }
        });
        viewModel = new WirelessViewModel(bluetoothAdapter);

        compositeDisposable.add(viewModel.getReceivedContacts()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(contact -> {
                    chooseSource.setVisibility(View.GONE);
                    receivingDataTextView.setVisibility(View.VISIBLE);
                    Toast.makeText(this, contact.getName() + " " + contact.getNumber(), Toast.LENGTH_SHORT).show();
                }));

        askLocationPermission();
        viewModel.startListening(acceptConnect);
        discoverDevice();

        initRecyclers();


        subscribeToSubjects();

        Log.d(TAG, bluetoothAdapter.startDiscovery() + "");
        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
    }

    /**
     * Make device discovered
     */
    private void discoverDevice() {
        Intent discoverableIntent =
                new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
    }

    private void subscribeToSubjects() {
        compositeDisposable.add(viewModel.getPairedDevices()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bluetoothDevices -> pairedAdapter.setDevices(bluetoothDevices)));

        compositeDisposable.add(clickSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pair -> {
                    progressBar.setVisibility(View.VISIBLE);
                    viewModel.onDeviceClicked(pair.first, connectSubject, failedSubject);
                    connectListener = pair.second;
                }));

        compositeDisposable.add(connectSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bluetoothSocket -> {
                    progressBar.setVisibility(View.GONE);
                    viewModel.onConnect(bluetoothSocket);
                    connectListener.onConnect();
                    chooseSource.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Connect Success");
                }));

        compositeDisposable.add(failedSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }));

        compositeDisposable.add(acceptConnect
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bluetoothSocket -> {
                    markConnectDevice(bluetoothSocket.getRemoteDevice().getAddress());
                    chooseSource.setVisibility(View.VISIBLE);
                }));
    }


    private void initRecyclers() {
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

    /**
     * Create a BroadcastReceiver for ACTION_FOUND.
     * find the devices
     */
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
            doIfBTEnabled();
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

    /**
     * Called when device accept connection from another device in order to mark the appropriate device
     * @param address of the device that is connected to
     */
    private void markConnectDevice(String address) {
        List<AcceptConnectListener> acceptConnectListeners = new ArrayList<>();
        acceptConnectListeners.addAll(pairedAdapter.getAcceptConnectListeners());
        acceptConnectListeners.addAll(availableAdapter.getAcceptConnectListeners());
        for (AcceptConnectListener acceptConnectListener : acceptConnectListeners) {
            if (acceptConnectListener.onConnect(address)) {
                return;
            }
        }

    }


}