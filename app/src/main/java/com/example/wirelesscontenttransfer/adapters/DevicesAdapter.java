package com.example.wirelesscontenttransfer.adapters;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wirelesscontenttransfer.listeners.AcceptConnectListener;
import com.example.wirelesscontenttransfer.listeners.ConnectListener;
import com.example.wirelesscontenttransfer.R;
import com.example.wirelesscontenttransfer.views.DeviceViewHolder;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class DevicesAdapter extends RecyclerView.Adapter<DeviceViewHolder> {

    private final Context context;

    private List<BluetoothDevice> devices;

    private final BehaviorSubject<Pair<BluetoothDevice, ConnectListener>> clickedSubject;


    private final List<AcceptConnectListener> acceptConnectListeners = new ArrayList<>();

    public DevicesAdapter(Context context, BehaviorSubject<Pair<BluetoothDevice,
            ConnectListener>> clickedSubject) {
        this.context = context;
        this.clickedSubject = clickedSubject;

    }

    public List<AcceptConnectListener> getAcceptConnectListeners() {
        return acceptConnectListeners;
    }

    public void setDevices(List<BluetoothDevice> devices) {
        this.devices = devices;
        notifyDataSetChanged();
    }

    public void addDevice(BluetoothDevice bluetoothDevice) {
        if (devices == null) {
            devices = new ArrayList<>();
        }
        devices.add(bluetoothDevice);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DeviceViewHolder(LayoutInflater.from(context).inflate(R.layout.device_view_holder, parent, false), clickedSubject);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        holder.bindData(devices.get(position));
        acceptConnectListeners.add(holder.getAcceptConnectListener());
    }

    @Override
    public int getItemCount() {
        return devices == null ? 0 : devices.size();
    }
}
