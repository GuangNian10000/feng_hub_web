package com.visiontalk.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BleWifiConfigService {

    private static final String TAG = "BleWifiConfigService";

    public static final UUID SERVICE_UUID = UUID.fromString("0000A101-0000-1000-8000-00805F9B34FB");
    public static final UUID CHAR_WRITE_UUID = UUID.fromString("0000A102-0000-1000-8000-00805F9B34FB");
    public static final UUID CHAR_NOTIFY_UUID = UUID.fromString("0000A103-0000-1000-8000-00805F9B34FB");
    public static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private Context context;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGattServer gattServer;
    private BluetoothLeAdvertiser advertiser;
    private BluetoothGattCharacteristic notifyChar;

    private final Set<BluetoothDevice> connectedDevices = new HashSet<>();

    //延长配网超时时间至60秒，预留充足的DHCP分配时间
    private int timeOut = 60;
    private boolean isConfiguring = false;
    private int remainingTime = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());

    //引入真实网络探测逻辑的倒计时任务
    private final Runnable countdownRunnable = new Runnable() {
        @Override
        public void run() {
            if (remainingTime > 0) {
                //每隔5秒探测一次是否分配到真实IP，缓解系统底层查询压力
                if (remainingTime % 5 == 0) {
                    checkNetworkStatus();
                }

                //控制蓝牙通知下发频率，避免GATT通道拥堵
                if (remainingTime % 10 == 0 || remainingTime == timeOut) {
                    sendFeedback(">> 配网中，剩余时间: " + remainingTime + "秒");
                }

                //状态标志位安全校验
                if (isConfiguring) {
                    remainingTime--;
                    handler.postDelayed(this, 1000);
                }
            } else {
                isConfiguring = false;
                //sendFeedback("!! 配网超时，未获取到有效IP，请检查路由DHCP状态");
            }
        }
    };

    public void init(Context ctx) {
        this.context = ctx;
        bluetoothManager = (BluetoothManager) ctx.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            startGattServer();
            startAdvertising();
        } else {
            //增加底层硬件异常拦截日志
            Log.e(TAG, "初始化失败：蓝牙适配器未就绪");
        }
    }

    private void startGattServer() {
        gattServer = bluetoothManager.openGattServer(context, new BluetoothGattServerCallback() {
            @Override
            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    connectedDevices.add(device);
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    connectedDevices.remove(device);
                }
            }

            @Override
            public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                if (CHAR_WRITE_UUID.equals(characteristic.getUuid()) && value != null) {
                    handleIncomingData(new String(value, StandardCharsets.UTF_8));
                    if (responseNeeded) {
                        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
                    }
                }
            }

            @Override
            public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                if (CLIENT_CHARACTERISTIC_CONFIG.equals(descriptor.getUuid())) {
                    if (responseNeeded) {
                        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
                    }
                }
            }
        });

        BluetoothGattService service = new BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic writeChar = new BluetoothGattCharacteristic(CHAR_WRITE_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);

        notifyChar = new BluetoothGattCharacteristic(CHAR_NOTIFY_UUID,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ);
        notifyChar.addDescriptor(new BluetoothGattDescriptor(CLIENT_CHARACTERISTIC_CONFIG, BluetoothGattDescriptor.PERMISSION_WRITE));

        service.addCharacteristic(writeChar);
        service.addCharacteristic(notifyChar);
        gattServer.addService(service);
    }

    private void handleIncomingData(String rawData) {
        try {
            JSONObject json = new JSONObject(rawData);
            String type = json.optString("type");
            if ("wifi".equals(type)) {
                if (isConfiguring) {
                    sendFeedback(">> 下发配网正在进行，请稍后");
                } else {
                    String ssid = json.getString("ssid");
                    String pwd = json.getString("pwd");

                    //对下发参数进行Base64无换行编码，对齐底层守护脚本的解码规则
                    String b64Ssid = Base64.encodeToString(ssid.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
                    String b64Pwd = Base64.encodeToString(pwd.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);

                    Log.i("WIFI_CMD", "CONNECT|" + b64Ssid + "|" + b64Pwd);
                    isConfiguring = true;
                    remainingTime = timeOut;
                    handler.removeCallbacks(countdownRunnable);
                    handler.post(countdownRunnable);
                }
            } else if ("time".equals(type)) {
                long timestamp = json.getLong("timestamp");
                android.app.admin.DevicePolicyManager dpm =
                        (android.app.admin.DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
                dpm.setTime(AdminReceiver.getComponentName(context), timestamp);
                sendFeedback(">> Device Owner 已同步系统时间");
            } else if ("cmd".equals(type)) {
                executeShell(json.getString("content"));
            }
        } catch (Exception e) {
            sendFeedback("!! JSON解析异常: " + e.getMessage());
        }
    }

    //使用原生API探测网络，规避Shell沙箱权限限制
    private void checkNetworkStatus() {
        new Thread(() -> {
            try {
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces != null && interfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = interfaces.nextElement();

                    //匹配核心无线网卡标识
                    if (networkInterface.getName().startsWith("ra")) {
                        Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                        while (addresses.hasMoreElements()) {
                            InetAddress address = addresses.nextElement();

                            //排除IPv6及本地回环，抓取有效IPv4地址
                            if (!address.isLoopbackAddress() && address.getHostAddress().indexOf(':') < 0) {
                                String finalIp = address.getHostAddress();
                                handler.post(() -> {
                                    isConfiguring = false;
                                    handler.removeCallbacks(countdownRunnable);
                                    sendFeedback(">> 配网成功! IP: " + finalIp);
                                });
                                return;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Network interface enumeration failed", e);
            }
        }).start();
    }

    private void executeShell(String command) {
        new Thread(() -> {
            sendFeedback(">> 开始执行: " + command);
            Process process = null;
            DataOutputStream os = null;
            try {
                process = Runtime.getRuntime().exec("sh");
                os = new DataOutputStream(process.getOutputStream());
                os.write((command + "\n").getBytes(StandardCharsets.UTF_8));
                os.writeBytes("exit\n");
                os.flush();

                BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = stdInput.readLine()) != null) {
                    sendFeedback("OUT: " + line);
                }

                BufferedReader errInput = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                while ((line = errInput.readLine()) != null) {
                    sendFeedback("ERR: " + line);
                }

                int exitVal = process.waitFor();
                sendFeedback(">> 执行完毕 (退出码: " + exitVal + ")");
            } catch (Exception e) {
                sendFeedback("!! 执行异常: " + e.getMessage());
            } finally {
                try { if (os != null) os.close(); if (process != null) process.destroy(); } catch (Exception ignored) {}
            }
        }).start();
    }

    private void sendFeedback(String message) {
        Log.i(TAG, message);
        if (notifyChar == null || connectedDevices.isEmpty()) return;

        byte[] value = message.getBytes(StandardCharsets.UTF_8);
        notifyChar.setValue(value);
        for (BluetoothDevice device : connectedDevices) {
            gattServer.notifyCharacteristicChanged(device, notifyChar, false);
        }
    }

    private void startAdvertising() {
        advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (advertiser == null) return;

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(new ParcelUuid(SERVICE_UUID))
                .build();

        advertiser.startAdvertising(settings, data, new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.i(TAG, "BLE广播开启");
            }

            @Override
            public void onStartFailure(int errorCode) {
                Log.e(TAG, "BLE广播开启失败, 错误码: " + errorCode);
            }
        });
    }
}