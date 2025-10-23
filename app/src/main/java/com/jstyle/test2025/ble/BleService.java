package com.jstyle.test2025.ble;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.jstyle.blesdk2301.callback.BleConnectionListener;
import com.jstyle.blesdk2301.Util.BleSDK;
import com.jstyle.blesdk2301.callback.DataListener2301;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

@SuppressLint("MissingPermission")
public final class BleService extends Service {
    private static final String TAG = "BleService";
    
    // BLE UUIDs for J2301A ring
    private static final UUID NOTIY = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final UUID SERVICE_DATA = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    private static final UUID DATA_Characteristic = UUID.fromString("0000fff6-0000-1000-8000-00805f9b34fb");
    private static final UUID NOTIY_Characteristic = UUID.fromString("0000fff7-0000-1000-8000-00805f9b34fb");

    public static boolean NeedReconnect = true;
    private final IBinder kBinder = new LocalBinder();
    private static BluetoothGatt mGatt;
    private static boolean isConnected = false;
    private static boolean isConnecting = false;
    private static String address;
    private static Context mContext;
    private static BleConnectionListener bleConnectionListener = null;
    private static DataListener2301 dataListener2301 = null;
    
    public static final Handler connHandler = new Handler(Looper.getMainLooper());
    public static Queue<byte[]> queues = new LinkedList<>();

    @Override
    public IBinder onBind(Intent intent) {
        return kBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e(TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    public void initBluetoothDevice(String add, final Context context) {
        Log.e(TAG, "initBluetoothDevice: " + add);
        NeedReconnect = true;
        address = add;
        mContext = context;
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled() || TextUtils.isEmpty(address) || isConnected())
            return;
    }

    @SuppressLint("CheckResult")
    public static void connectedDevice(String address, Context context, BleConnectionListener bb) {
        bleConnectionListener = bb;
        NeedReconnect = true;
        Log.e(TAG, "connectedDevice: " + address);
        
        if (mGatt != null || TextUtils.isEmpty(address) || isConnected()) {
            Log.e(TAG, "mGatt!=NULL return");
            return;
        }
        if (isConnecting) {
            Log.e(TAG, "isConnecting...");
            return;
        }

        disconnect2();
        if (!isConnecting) {
            isConnecting = true;
            Log.e(TAG, "连接中");
            if(null!=bleConnectionListener){
                bleConnectionListener.Connecting();
            }
        }
        
        if (BleManager.getInstance().bluetoothAdapter.isEnabled()) {
            final BluetoothDevice device = BleManager.getInstance().bluetoothAdapter.getRemoteDevice(address.toUpperCase());
            ConnectMAC(context, device);
        }
    }

    private static void ConnectMAC(Context context, BluetoothDevice device) {
        try {
            if (BleManager.getInstance().bluetoothAdapter.isDiscovering()) {
                BleManager.getInstance().bluetoothAdapter.cancelDiscovery();
            }
        } finally {
            if(NeedReconnect){
                connHandler.postDelayed(() -> {
                    try {
                        NeedReconnect = true;
                        Log.e(TAG, "蓝牙设备: 直连中 ..." + device.getAddress());
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            if (BleManager.getInstance().bluetoothAdapter.isLe2MPhySupported()) {
                                mGatt = device.connectGatt(context, false, bleGattCallback, BluetoothDevice.TRANSPORT_LE, BluetoothDevice.PHY_LE_2M_MASK);
                            } else {
                                mGatt = device.connectGatt(context, false, bleGattCallback, BluetoothDevice.TRANSPORT_LE, BluetoothDevice.PHY_LE_1M_MASK);
                            }
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            mGatt = device.connectGatt(context, false, bleGattCallback, BluetoothDevice.TRANSPORT_LE);
                        } else {
                            mGatt = device.connectGatt(context, false, bleGattCallback);
                        }
                    } catch (Exception ignored) {
                        Log.e(TAG, "connectedDevice：" + ignored.toString());
                    }
                }, 500);
            }
        }
    }

    public class LocalBinder extends Binder {
        public BleService getService() {
            return BleService.this;
        }
    }

    private static final BluetoothGattCallback bleGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.e(TAG, "onConnectionStateChange: status " + status + " newstate " + newState);
            
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                isConnecting = false;
                try {
                    if (null != mGatt && mGatt != gatt) {
                        mGatt.disconnect();
                        mGatt.close();
                        mGatt = null;
                    }
                    mGatt = gatt;
                } catch (Throwable e) {
                    Log.e(TAG, "Connection cleanup error", e);
                }
                try {
                    connHandler.postDelayed(gatt::discoverServices, 600);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (null != queues && !queues.isEmpty()) {
                    queues.clear();
                }
                isConnecting = false;
                isConnected = false;
                try {
                    if (mGatt != null) {
                        mGatt.disconnect();
                        mGatt.close();
                        refreshDeviceCache(mGatt);
                        mGatt = null;
                    }
                    if (gatt != null && gatt != mGatt) {
                        gatt.disconnect();
                        gatt.close();
                        refreshDeviceCache(gatt);
                    }
                } catch (Throwable E) {
                    Log.e(TAG, "Disconnect cleanup error", E);
                } finally {
                    queues.clear();
                    if(null!=bleConnectionListener){
                        if (status != BluetoothGatt.GATT_SUCCESS) {
                            bleConnectionListener.ConnectionFailed(status, newState);
                        } else {
                            bleConnectionListener.Ondisconnected();
                        }
                    }
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.e(TAG, "onServicesDiscovered: status" + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (!NeedReconnect) {
                    try {
                        if (gatt != null) {
                            gatt.disconnect();
                            gatt.close();
                            refreshDeviceCache(gatt);
                        }
                    } finally {
                        if(null!=bleConnectionListener){
                            bleConnectionListener.ConnectionFailed(0,0);
                        }
                    }
                    return;
                }
                
                Log.e(TAG, "onServicesDiscovered 连接成功了： " + gatt.getDevice().getName());
                if (null == mGatt || mGatt == gatt) {
                    mGatt = gatt;
                    NeedReconnect = true;
                    isConnected = true;
                    
                    connHandler.postDelayed(() -> {
                        if(null!=bleConnectionListener){
                            bleConnectionListener.ConnectionSucceeded();
                        }
                        setCharacteristicNotification(gatt);
                    }, 30);
                }
            } else {
                Log.e(TAG, "onServicesDiscovered received: " + status);
            }
        }

        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.e(TAG, "onDescriptorWrite: status" + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "连接成功了 onDescriptorWrite");
                if (Build.VERSION.SDK_INT >= 22) {
                    gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                }
                nextQueue();
            }
        }

        @SuppressLint("MissingPermission")
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if (mGatt != null) {
                if (null != characteristic && null != characteristic.getValue()) {
                    // Process received data with SDK
                    byte[] data = characteristic.getValue();
                    if (dataListener2301 != null) {
                        BleSDK.DataParsingWithData(data, dataListener2301);
                    }
                }
            }
        }

        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                nextQueue();
            }
        }
    };

    public static boolean refreshDeviceCache(BluetoothGatt gatt) {
        Log.e(TAG, "refreshDeviceCache");
        try {
            Method localMethod = gatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                return ((Boolean) localMethod.invoke(gatt, new Object[0])).booleanValue();
            }
        } catch (Exception localException) {
            Log.e(TAG, "An exception occured while refreshing device");
        }
        return false;
    }

    public static void writeValue(byte[] value) {
        if (mGatt == null || value == null) return;
        
        connHandler.postDelayed(() -> {
            try {
                BluetoothGattService service = mGatt.getService(SERVICE_DATA);
                if (service == null) return;
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(DATA_Characteristic);
                if (characteristic == null) return;
                
                if (value[0] == (byte) 0x47) {
                    NeedReconnect = false;
                }
                characteristic.setValue(value);
                mGatt.writeCharacteristic(characteristic);
            } catch (Exception e) {
                Log.e(TAG, "Write error", e);
            }
        }, 30);
    }

    public static void setCharacteristicNotification(BluetoothGatt gatt) {
        Log.e(TAG, "开启通知");
        BluetoothGattService service = gatt.getService(SERVICE_DATA);
        if (service == null) return;
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(NOTIY_Characteristic);
        if (characteristic == null) return;
        
        gatt.setCharacteristicNotification(characteristic, true);
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(NOTIY);
        if (descriptor == null) {
            Log.e(TAG, "setCharacteristicNotification descriptor null");
            return;
        }
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(descriptor);
    }

    public static void offerValue(byte[] value) {
        queues.offer(value);
    }

    public static void nextQueue() {
        byte[] data = queues.poll();
        if (data != null) {
            writeValue(data);
        }
    }

    public static boolean isConnecting() {
        return isConnecting;
    }

    public static boolean isConnected() {
        return isConnected;
    }

    public void disconnect() {
        Log.e(TAG, "主动断开设备");
        NeedReconnect = false;
        isConnecting = false;
        
        if (mGatt != null) {
            if (isConnected) {
                mGatt.disconnect();
                if (mGatt != null) {
                    mGatt.close();
                    refreshDeviceCache(mGatt);
                    mGatt = null;
                }
            } else {
                if (null != mGatt) {
                    mGatt.close();
                    refreshDeviceCache(mGatt);
                    mGatt = null;
                }
            }
            isConnected = false;
        }
        
        if(null!=bleConnectionListener){
            bleConnectionListener.Ondisconnected();
        }
    }

    public static void disconnect2() {
        Log.e(TAG, "主动断开设备2");
        isConnecting = false;
        
        if (mGatt != null) {
            if (isConnected) {
                mGatt.disconnect();
                if (mGatt != null) {
                    mGatt.close();
                    refreshDeviceCache(mGatt);
                    mGatt = null;
                }
            } else {
                if (null != mGatt) {
                    mGatt.close();
                    refreshDeviceCache(mGatt);
                    mGatt = null;
                }
            }
            isConnected = false;
        }
    }
    
    // Set data listener to receive health data
    public static void setDataListener(DataListener2301 listener) {
        dataListener2301 = listener;
    }
}