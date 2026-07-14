package com.fengke.robot.face;

import static com.fengke.robot.MainActivity.KEY_LAST_IP;
import static com.fengke.robot.MainActivity.KEY_LAST_PORT;
import static com.fengke.robot.MainActivity.PREFS_AUTO_CONNECT;
import static com.fengke.robot.MainActivity.WS_SERVER_PORT;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fengke.robot.R;
import com.fengke.robot.tcp.TcpServiceHelper;
import com.fengke.robot.tcp.TcpServiceManager;
import com.fengke.robot.util.BaseActivity;
import com.fengke.robot.util.LogManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FaceManagerActivity extends BaseActivity implements TcpServiceManager.ConnectionListener {
    private static final String TAG = "FaceManagerActivity";

    // UI 组件
    private FaceListAdapter adapter;
    private EditText etSearch;
    private AlertDialog progressDialog;
    private TextView progressText;

    // 网络与通讯
    private TcpServiceHelper tcpServiceHelper;
    private String tabletIp = ""; // 平板的 IP 地址，用于穿透访问 HTTP

    // 状态与数据
    private final List<FaceData> allFaceList = new ArrayList<>();
    private String tempInputName = "";
    private File tempPhotoFile;

    // 回调启动器
    private ActivityResultLauncher<Intent> pickPhotoLauncher;
    private ActivityResultLauncher<Intent> takePhotoLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_manager);

        // 1. 初始化 TCP 服务助手 (绑定老代码中的服务)
        tcpServiceHelper = new TcpServiceHelper(this);
        tcpServiceHelper.startAndBindService();
        tcpServiceHelper.setConnectionListener(this);

        SharedPreferences prefs = getSharedPreferences(PREFS_AUTO_CONNECT, Context.MODE_PRIVATE);
        String lastIp = prefs.getString(KEY_LAST_IP, "");
        // 2. 加载平板 IP 配置
        tabletIp = lastIp;

        initViews();
        initAdapter();
        initActivityResultLaunchers();
        initProgressDialog();

        // 3. 如果没有 IP 则弹出要求输入，有则直接加载列表
        if (TextUtils.isEmpty(tabletIp)) {
            showIpConfigDialog(true);
        } else {
            loadFaceListHttp();
        }
    }

    private void initViews() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        etSearch = findViewById(R.id.etSearch);
        Button btnSearch = findViewById(R.id.btnSearch);
        Button btnAdd = findViewById(R.id.btnAdd);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 长按搜索按钮，可以重新配置平板的 IP
        btnSearch.setOnLongClickListener(v -> {
            showIpConfigDialog(false);
            return true;
        });

        // 搜索过滤本地列表
        btnSearch.setOnClickListener(v -> {
            String keyword = etSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
            List<FaceData> filtered = new ArrayList<>();
            for (FaceData fd : allFaceList) {
                if (fd.username.toLowerCase(Locale.ROOT).contains(keyword)) {
                    filtered.add(fd);
                }
            }
            adapter.setData(filtered);
        });

        // 呼出强大的混合添加弹窗
        btnAdd.setOnClickListener(v -> showActionDialog(null));
    }

    private void initAdapter() {
        adapter = new FaceListAdapter();
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setAdapter(adapter);

        // 编辑操作（复用添加逻辑，输入框预填名字即可实现覆盖更新）
        adapter.setOnEditListener(this::showActionDialog);

        // 删除操作（走 TCP 极速指令）
        adapter.setOnDeleteListener(data -> {
            new AlertDialog.Builder(this)
                    .setTitle("危险操作")
                    .setMessage("确定要彻底删除 [" + data.username + "] 吗？")
                    .setPositiveButton("删除", (d, w) -> sendTcpCommand("delete_person", data.username))
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    // ====================== 核心功能一：HTTP 获取列表 ======================
    private void loadFaceListHttp() {
        if (TextUtils.isEmpty(tabletIp)) return;

        new Thread(() -> {
            try {
                URL url = new URL("http://" + tabletIp + ":8000/get_faces");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(10000); // 👈 加上这行，10秒拉不到列表就自动停止

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                JSONArray faces = json.optJSONArray("faces");

                allFaceList.clear();
                if (faces != null) {
                    for (int i = 0; i < faces.length(); i++) {
                        JSONObject obj = faces.getJSONObject(i);
                        FaceData fd = new FaceData();
                        fd.username = obj.optString("name");

                        // ✨ 重点：拼接穿透 HTTP 的高清图片地址
                        String imgPath = obj.optString("image_url");
                        if (!TextUtils.isEmpty(imgPath) && !imgPath.equals("null")) {
                            fd.imageUrl = "http://" + tabletIp + ":8000" + imgPath;
                        }
                        allFaceList.add(fd);
                    }
                }
                runOnUiThread(() -> adapter.setData(allFaceList));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "列表加载失败，请检查平板IP与网络", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // ====================== 核心功能二：HTTP 表单传图 ======================
// ====================== 核心功能二：HTTP 表单传图 ======================
    private void uploadFileHttp(String name, File file) {
        showProgress("正在上传照片并提取特征...");
        new Thread(() -> {
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            java.net.HttpURLConnection conn = null; // 放在外面，确保 finally 能关闭
            try {
                URL url = new URL("http://" + tabletIp + ":8000/add_person");
                conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setUseCaches(false);
                conn.setRequestMethod("POST");

                // 🌟 修复一：强制设置连接和读取超时，防死等
                conn.setConnectTimeout(10000); // 10秒连不上就报错
                conn.setReadTimeout(20000);    // 20秒传不完/等不到响应就报错

                // 🌟 修复二：开启分块传输，杜绝高清大图导致的 OOM 闪退
                conn.setChunkedStreamingMode(1024 * 64);

                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                // 🌟 修复三：统一使用 DataOutputStream，纯字节流传输，杜绝数据损坏
                java.io.DataOutputStream dos = new java.io.DataOutputStream(conn.getOutputStream());

                // 写入 name 字段
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"name\"\r\n\r\n");
                dos.write(name.getBytes("UTF-8")); // 确保中文名字不会乱码
                dos.writeBytes("\r\n");

                // 写入 file 字段
                dos.writeBytes("--" + boundary + "\r\n");
                // 统一写死文件名，避免某些带特殊字符的本地文件名搞崩服务端解析
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"face_image.jpg\"\r\n");
                dos.writeBytes("Content-Type: image/jpeg\r\n\r\n");
                dos.flush();

                // 边读边发，绝不占用过多手机内存
                FileInputStream inputStream = new FileInputStream(file);
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
                dos.flush();
                inputStream.close();

                dos.writeBytes("\r\n");
                dos.writeBytes("--" + boundary + "--\r\n");
                dos.flush();
                dos.close();

                int code = conn.getResponseCode();
                if (code == 200) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "人脸已成功录入/更新！", Toast.LENGTH_SHORT).show();
                        loadFaceListHttp(); // 刷新列表
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "录入失败，照片中可能无人脸", Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "上传超时或网络异常，请重试", Toast.LENGTH_SHORT).show());
            } finally {
                // 🌟 修复四：无论如何，释放网络连接并关闭转圈弹窗
                if (conn != null) {
                    conn.disconnect();
                }
                runOnUiThread(this::hideProgress);
            }
        }).start();
    }
    // ====================== 核心功能三：TCP 极速指令 ======================
    private void sendTcpCommand(String action, String name) {
        if (!tcpServiceHelper.isConnected()) {
            Toast.makeText(this, "未连接到控制服务，请检查网络", Toast.LENGTH_SHORT).show();
            return;
        }
        showProgress("正在请求机器人...");
        try {
            JSONObject cmd = new JSONObject();
            cmd.put("type", "face");
            cmd.put("action", action);
            cmd.put("name", name);
            // 通过老代码的 TCP 助手发送
            tcpServiceHelper.send(cmd.toString());
        } catch (Exception e) {
            hideProgress();
        }
    }

    // --- 监听 TCP 回调 (接收路由器转回来的增删响应) ---
    @Override
    public void onMessage(String message) {
        try {
            JSONObject json = new JSONObject(message);
            String action = json.optString("action");
            if ("add_person".equals(action) || "delete_person".equals(action)) {
                runOnUiThread(() -> {
                    hideProgress();
                    Toast.makeText(this, json.optString("message"), Toast.LENGTH_SHORT).show();
                    loadFaceListHttp(); // 无论是 TCP 抓拍还是删除，结束后立刻刷新列表
                });
            }
        } catch (Exception e) {
            // 忽略非 JSON 数据
        }
    }
    @Override public void onConnected() {}
    @Override public void onDisconnected(String reason) {}
    @Override public void onError(String error) {}


    // ====================== UI 交互区 ======================
    /**
     * 智能混合录入/修改弹窗（完全代码构建，摆脱 XML 依赖）
     */
    private void showActionDialog(@Nullable FaceData data) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 20);

        final EditText etName = new EditText(this);
        etName.setHint("请输入人脸姓名 (同名则覆盖)");
        if (data != null) etName.setText(data.username);
        layout.addView(etName);

        Button btnGallery = new Button(this);
        btnGallery.setText("1. 从手机相册选图上传");
        layout.addView(btnGallery);

        Button btnCamera = new Button(this);
        btnCamera.setText("2. 手机马上拍照上传");
        layout.addView(btnCamera);

        Button btnRobot = new Button(this);
        btnRobot.setText("3. 命令机器人当前视角抓拍");
        btnRobot.setTextColor(0xFFD32F2F); // 红色高亮
        layout.addView(btnRobot);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(data == null ? "录入新人脸" : "更新人脸资料")
                .setView(layout)
                .setNegativeButton("取消", null)
                .create();

        btnGallery.setOnClickListener(v -> {
            tempInputName = etName.getText().toString().trim();
            if (TextUtils.isEmpty(tempInputName)) {
                Toast.makeText(this, "必须输入姓名！", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            pickPhotoLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
        });

        btnCamera.setOnClickListener(v -> {
            tempInputName = etName.getText().toString().trim();
            if (TextUtils.isEmpty(tempInputName)) {
                Toast.makeText(this, "必须输入姓名！", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            takePhoto();
        });

        btnRobot.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(this, "必须输入姓名！", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            // 走 TCP 直接触发机器狗拍照
            sendTcpCommand("add_person", name);
        });

        dialog.show();
    }

    private void showIpConfigDialog(boolean isFirstTime) {
        EditText input = new EditText(this);
        input.setText(tabletIp);
        input.setHint("例如: 192.168.3.155");

        new AlertDialog.Builder(this)
                .setTitle("配置平板网络 IP (端口转发代理)")
                .setMessage(isFirstTime ? "初次使用请填写平板的 WiFi IP 地址" : "修改平板的 IP 地址")
                .setView(input)
                .setCancelable(!isFirstTime)
                .setPositiveButton("保存并连接", (d, w) -> {
                    tabletIp = input.getText().toString().trim();
                    getSharedPreferences("RobotConfig", MODE_PRIVATE).edit().putString("tablet_ip", tabletIp).apply();
                    loadFaceListHttp();
                }).show();
    }

    private void takePhoto() {
        try {
            File dir = new File(getExternalFilesDir(null), "face_photos");
            if (!dir.exists()) dir.mkdirs();
            tempPhotoFile = new File(dir, "temp_" + System.currentTimeMillis() + ".jpg");
            Uri tempPhotoUri = androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", tempPhotoFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, tempPhotoUri);
            takePhotoLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "无法启动相机", Toast.LENGTH_SHORT).show();
        }
    }

    private void initActivityResultLaunchers() {
        pickPhotoLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                try {
                    String path = getRealPathFromUri(result.getData().getData());
                    if (path != null) uploadFileHttp(tempInputName, new File(path));
                } catch (Exception e) {
                    Toast.makeText(this, "解析图片失败", Toast.LENGTH_SHORT).show();
                }
            }
        });

        takePhotoLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && tempPhotoFile != null && tempPhotoFile.exists()) {
                uploadFileHttp(tempInputName, tempPhotoFile);
            }
        });
    }

    private String getRealPathFromUri(Uri uri) {
        try {
            File tempFile = new File(getExternalFilesDir(null), "upload_temp.jpg");
            try (java.io.InputStream in = getContentResolver().openInputStream(uri);
                 java.io.FileOutputStream out = new java.io.FileOutputStream(tempFile)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                return tempFile.getAbsolutePath();
            }
        } catch (Exception e) {
            return null;
        }
    }

    private void initProgressDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_upload_progress, null);
        progressText = view.findViewById(R.id.tvProgress);
        progressDialog = new AlertDialog.Builder(this).setView(view).setCancelable(false).create();
    }

    private void showProgress(String text) {
        progressText.setText(text);
        progressDialog.show();
    }

    private void hideProgress() {
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tcpServiceHelper.unbindAndStopService();
    }
}