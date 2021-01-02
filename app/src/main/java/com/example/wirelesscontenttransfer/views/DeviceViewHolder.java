package com.example.wirelesscontenttransfer.views;

import android.bluetooth.BluetoothDevice;
import android.graphics.Color;
import android.util.Pair;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wirelesscontenttransfer.R;
import com.example.wirelesscontenttransfer.listeners.AcceptConnectListener;
import com.example.wirelesscontenttransfer.listeners.ConnectListener;

import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class DeviceViewHolder extends RecyclerView.ViewHolder {
    private BluetoothDevice bluetoothDevice;
    private final AppCompatTextView deviceTextView;
    private AcceptConnectListener acceptConnectListener;

    public DeviceViewHolder(@NonNull View itemView, BehaviorSubject<Pair<BluetoothDevice, ConnectListener>> clickedSubject) {
        super(itemView);
        deviceTextView = itemView.findViewById(R.id.device_text_view);
        View root = itemView.findViewById(R.id.device_root);
        root.setOnClickListener(v -> clickedSubject.onNext(Pair.create(bluetoothDevice,
                () -> deviceTextView.setTextColor(Color.BLUE))));
    }

    public void bindData(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
        deviceTextView.setText(bluetoothDevice.getName());
        acceptConnectListener = address -> {
            if (address.equals(bluetoothDevice.getAddress())) {
                deviceTextView.setTextColor(Color.BLUE);
                return true;
            }
            return false;
        };
    }

    public AcceptConnectListener getAcceptConnectListener() {
        return acceptConnectListener;
    }
}
