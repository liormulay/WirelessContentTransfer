package com.example.wirelesscontenttransfer;

import android.bluetooth.BluetoothDevice;
import android.graphics.Color;
import android.util.Pair;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class DeviceViewHolder extends RecyclerView.ViewHolder {
    private BluetoothDevice bluetoothDevice;
    private final AppCompatTextView device_text_view;

    public DeviceViewHolder(@NonNull View itemView, BehaviorSubject<Pair<BluetoothDevice, OnConnectListener>> clickedSubject) {
        super(itemView);
        device_text_view = itemView.findViewById(R.id.device_text_view);
        View root = itemView.findViewById(R.id.device_root);
        root.setOnClickListener(v -> clickedSubject.onNext(Pair.create(bluetoothDevice, () -> device_text_view.setTextColor(Color.BLUE))));
    }

    public void bindData(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
        device_text_view.setText(bluetoothDevice.getName());
    }

}
