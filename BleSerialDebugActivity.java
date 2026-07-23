package com.fengke.robot.bluetooth;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.fengke.robot.R;
import com.fengke.robot.util.BaseActivity;
import com.fengke.robot.util.BlePermissionHelper;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BleSerialDebugActivity extends BaseActivity {

    public static final UUID SERVICE_UUID = UUID.fromString("0000A101-0000-1000-8000-00805F9B34FB");
    public static final UUID CHAR_WRITE_UUID = UUID.fromString("0000A102-0000-1000-8000-00805F9B34FB");
    public static final UUID CHAR_NOTIFY_UUID = UUID.fromString("0000A103-0000-1000-8000-00805F9B34FB");
    public static final UUID CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final int MIN_RSSI_THRESHOLD = -95;
    private static final int MAX_RETRY_COUNT = 3;

    private TextView tvLog, tvSelectedTime;
    private EditText etSsid, etPassword, etCustomCmd;
    private ScrollView logScrollView, menuScrollView;
    private LinearLayout leftContentLayout, rootLayout;
    private String selectedTimeStr = "";

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic writeChar;
    private BluetoothDevice targetDevice;

    private int currentRetryCount = 0;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_serial_debug);

        initViews();
        initBluetooth();
        bindEvents();
        setupKeyboardListener();
    }

    private void initViews() {
        rootLayout = findViewById(R.id.root_layout);
        leftContentLayout = findViewById(R.id.left_content_layout);
        menuScrollView = findViewById(R.id.menu_scroll_view);
        tvLog = findViewById(R.id.tv_log);
        etSsid = findViewById(R.id.et_ssid);
        etPassword = findViewById(R.id.et_password);
        etCustomCmd = findViewById(R.id.et_custom_cmd);
        logScrollView = findViewById(R.id.log_scroll);
        tvSelectedTime = findViewById(R.id.tv_selected_time);
    }

    private void setupKeyboardListener() {
        rootLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                rootLayout.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootLayout.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                if (keypadHeight > screenHeight * 0.15) {
                    leftContentLayout.setPadding(0, 0, 0, keypadHeight);
                } else {
                    leftContentLayout.setPadding(0, 0, 0, 0);
                }
            }
        });
    }

    private void initBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        }
    }

    private void bindEvents() {
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_scan).setOnClickListener(v -> startBleScan());
        findViewById(R.id.btn_disconnect).setOnClickListener(v -> disconnectGatt());

        findViewById(R.id.btn_send_wifi).setOnClickListener(v -> {
            String ssid = etSsid.getText().toString().trim();
            String pwd = etPassword.getText().toString().trim();
            if (ssid.isEmpty()) return;
            try {
                JSONObject json = new JSONObject();
                json.put("type", "wifi");
                json.put("ssid", ssid);
                json.put("pwd", pwd);
                sendData(json.toString());
            } catch (Exception e) { appendLog("!! 构造JSON失败"); }
        });

        findViewById(R.id.btn_execute_cmd).setOnClickListener(v -> {
            String cmd = etCustomCmd.getText().toString().trim();
            if (cmd.isEmpty()) return;
            try {
                JSONObject json = new JSONObject();
                json.put("type", "cmd");
                json.put("content", cmd);
                sendData(json.toString());
            } catch (Exception e) { appendLog("!! 构造JSON失败"); }
        });

        findViewById(R.id.btn_ifconfig).setOnClickListener(v -> {
            try {
                JSONObject json = new JSONObject();
                json.put("type", "cmd");
                json.put("content", "ifconfig");
                sendData(json.toString());
            } catch (Exception e) { appendLog("!! 构造JSON失败"); }
        });

        findViewById(R.id.btn_query_time).setOnClickListener(v -> {
            try {
                JSONObject json = new JSONObject();
                json.put("type", "cmd");
                json.put("content", "date");
                sendData(json.toString());
            } catch (Exception e) { appendLog("!! 构造JSON失败"); }
        });

        findViewById(R.id.btn_pick_time).setOnClickListener(v -> showDateTimePicker());

        findViewById(R.id.btn_set_time).setOnClickListener(v -> {
            if (selectedTimeStr.isEmpty()) {
                appendLog("!! 请先选择时间");
                return;
            }
            try {
                JSONObject json = new JSONObject();
                json.put("type", "cmd");
                json.put("content", "date -s " + selectedTimeStr);
                sendData(json.toString());
            } catch (Exception e) { appendLog("!! 构造JSON失败"); }
        });
    }

    private void showDateTimePicker() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            new TimePickerDialog(this, (view1, hourOfDay, minute) -> {
                String displayStr = String.format(java.util.Locale.US, "%04d-%02d-%02d %02d:%02d:00",
                        year, month + 1, dayOfMonth, hourOfDay, minute);
                tvSelectedTime.setText(displayStr);

                selectedTimeStr = String.format(java.util.Locale.US, "%04d%02d%02d.%02d%02d%02d",
                        year, month + 1, dayOfMonth, hourOfDay, minute, 0);
            }, calendar.get(java.util.Calendar.HOUR_OF_DAY),
                    calendar.get(java.util.Calendar.MINUTE), true).show();
        }, calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH),
                calendar.get(java.util.Calendar.DAY_OF_MONTH)).show();
    }

    //处理 Android 12 以下版本的蓝牙权限兼容性。
    private boolean hasConnectPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread(() -> {
                    appendLog(">> 物理连接成功，请求MTU...");
                    currentRetryCount = 0;
                });
                if (hasConnectPermission()) {
                    gatt.requestMtu(512);
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runOnUiThread(() -> {
                    appendLog("!! 连接断开, 状态码: " + status);
                    writeChar = null;

                    if ((status == 62 || status == 133) && currentRetryCount < MAX_RETRY_COUNT) {
                        currentRetryCount++;
                        appendLog(">> 尝试重新连接 (" + currentRetryCount + "/" + MAX_RETRY_COUNT + ")...");
                        executeGattReconnect();
                    } else {
                        if (bluetoothGatt != null) {
                            if (hasConnectPermission()) {
                                bluetoothGatt.close();
                            }
                            bluetoothGatt = null;
                        }
                    }
                });
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            if (hasConnectPermission()) {
                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                if (service != null) {
                    writeChar = service.getCharacteristic(CHAR_WRITE_UUID);
                    BluetoothGattCharacteristic notifyChar = service.getCharacteristic(CHAR_NOTIFY_UUID);
                    if (notifyChar != null && hasConnectPermission()) {
                        gatt.setCharacteristicNotification(notifyChar, true);
                        BluetoothGattDescriptor descriptor = notifyChar.getDescriptor(CCCD_UUID);
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                    runOnUiThread(() -> appendLog(">> 双向链路就绪"));
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (CHAR_NOTIFY_UUID.equals(characteristic.getUuid())) {
                String feedback = new String(characteristic.getValue(), StandardCharsets.UTF_8);
                runOnUiThread(() -> {
                    appendLog(feedback);
                    tryCopyIpAddress(feedback);
                });
            }
        }
    };

    private void tryCopyIpAddress(String text) {
        if (text == null) return;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("inet addr:(\\d+\\.\\d+\\.\\d+\\.\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String ip = matcher.group(1);
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("IP Address", ip);
            clipboard.setPrimaryClip(clip);
            mainHandler.postDelayed(() -> appendLog(">> 已自动复制 IP: " + ip), 2000);
        }
    }

    private void sendData(String data) {
        if (bluetoothGatt == null || writeChar == null) {
            appendLog("!! 连接未就绪");
            return;
        }
        if (hasConnectPermission()) {
            writeChar.setValue(data.getBytes(StandardCharsets.UTF_8));
            bluetoothGatt.writeCharacteristic(writeChar);
        }
    }

    private void startBleScan() {
        if (writeChar != null) {
            appendLog("!! 双向链路已就绪，无需重复扫描连接");
            return;
        }

        if (bluetoothGatt != null) {
            appendLog("!! 蓝牙已连接或正在连接中，如需重新扫描请先断开");
            return;
        }

        if (bluetoothAdapter == null) {
            appendLog("!! 设备不支持蓝牙");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            appendLog("!! 蓝牙未开启，请先在系统设置中开启蓝牙");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (hasConnectPermission()) {
                startActivity(enableBtIntent);
            }
            return;
        }

        if (!BlePermissionHelper.hasBlePermissions(this)) {
            appendLog("!! 缺少蓝牙扫描权限，正在请求...");
            BlePermissionHelper.requestBlePermissions(this);
            return;
        }

        if (bleScanner == null) {
            bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        }

        if (bleScanner == null) {
            appendLog("!! 无法获取蓝牙扫描器，请尝试重启蓝牙");
            return;
        }

        currentRetryCount = 0;
        appendLog(">> 开始扫描目标设备...");

        List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(SERVICE_UUID)).build());
        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        try {
            bleScanner.startScan(filters, settings, scanCallback);
        } catch (Exception e) {
            appendLog("!! 启动扫描失败: " + e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BlePermissionHelper.REQUEST_CODE) {
            if (BlePermissionHelper.hasBlePermissions(this)) {
                appendLog(">> 权限已授予，请再次点击扫描");
            } else {
                appendLog("!! 权限被拒绝，无法进行蓝牙扫描");
                Toast.makeText(this, "需要蓝牙权限才能连接", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            ScanRecord scanRecord = result.getScanRecord();
            if (scanRecord == null || scanRecord.getServiceUuids() == null ||
                    !scanRecord.getServiceUuids().contains(new ParcelUuid(SERVICE_UUID))) {
                return;
            }

            int rssi = result.getRssi();
            appendLog("|>>| 匹配到目标设备: " + result.getDevice().getAddress() + " 信号: " + rssi);

            if (rssi < MIN_RSSI_THRESHOLD) {
                appendLog("!! 信号低于阈值，暂缓连接");
                return;
            }

            //引入兼容性方法，防止低版本系统在此处拦截连接流程。
            if (hasConnectPermission()) {
                try {
                    bleScanner.stopScan(this);
                } catch (SecurityException e) {
                    appendLog("!! 停止扫描权限异常: " + e.getMessage());
                }
                appendLog(">> 停止扫描，准备初始化物理链路...");
                targetDevice = result.getDevice();

                mainHandler.postDelayed(() -> connectGattDevice(), 200);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            appendLog("!! 扫描失败，底层错误码: " + errorCode);
        }
    };

    private void connectGattDevice() {
        if (targetDevice != null && hasConnectPermission()) {
            appendLog(">> 发起底层 connectGatt 请求...");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                bluetoothGatt = targetDevice.connectGatt(BleSerialDebugActivity.this, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
            } else {
                bluetoothGatt = targetDevice.connectGatt(BleSerialDebugActivity.this, false, gattCallback);
            }
        }
    }

    private void executeGattReconnect() {
        if (bluetoothGatt != null && hasConnectPermission()) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        mainHandler.postDelayed(this::connectGattDevice, 1500);
    }

    private void disconnectGatt() {
        if (bluetoothGatt != null && hasConnectPermission()) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
            writeChar = null;
            targetDevice = null;
            appendLog(">> 连接释放");
        }
    }

    private void appendLog(String message) {
        runOnUiThread(() -> {
            tvLog.append(message + "\n");
            logScrollView.post(() -> logScrollView.fullScroll(ScrollView.FOCUS_DOWN));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectGatt();
        mainHandler.removeCallbacksAndMessages(null);
    }
}