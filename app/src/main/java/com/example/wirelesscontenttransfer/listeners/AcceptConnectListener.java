package com.example.wirelesscontenttransfer.listeners;

public interface AcceptConnectListener {
    /**
     * Notify when device accept connection from another device
     * @param address of the device that connected
     * @return true if the implementer found this address
     */
    boolean onConnect(String address);
}
