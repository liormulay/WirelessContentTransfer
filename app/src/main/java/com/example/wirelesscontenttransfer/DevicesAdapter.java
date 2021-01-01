package com.example.wirelesscontenttransfer;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class DevicesAdapter extends RecyclerView.Adapter<DeviceViewHolder> {

    private final Context context;

    private List<BluetoothDevice> devices;

    private final BehaviorSubject<BluetoothDevice> clickedSubject;

    public DevicesAdapter(Context context, BehaviorSubject<BluetoothDevice> clickedSubject) {
        this.context = context;
        this.clickedSubject = clickedSubject;
    }

    public void setDevices(List<BluetoothDevice> devices) {
        this.devices = devices;
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
    }

    @Override
    public int getItemCount() {
        return devices == null ? 0 : devices.size();
    }
}
