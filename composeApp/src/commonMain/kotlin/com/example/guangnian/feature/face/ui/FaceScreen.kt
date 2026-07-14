package com.example.guangnian.feature.face.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.guangnian.feature.face.data.bean.FaceData
import com.example.guangnian.feature.face.data.bean.FaceConfig
import com.example.guangnian.feature.face.viewmodel.FaceViewModel
import com.example.guangnian.core.utils.rememberImagePicker
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceScreen(
    onBack: () -> Unit
) {
    val viewModel: FaceViewModel = koinViewModel()
    val faces by viewModel.faces.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val config by viewModel.config.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedFaceForDelete by remember { mutableStateOf<FaceData?>(null) }
    var currentActionName by remember { mutableStateOf("") }
    var currentActionDesc by remember { mutableStateOf("") }

    val imagePicker = rememberImagePicker { bytes ->
        if (currentActionName.isNotBlank()) {
            viewModel.uploadFaceImage(currentActionName, currentActionDesc, bytes)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("人脸管理", fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.refresh() }) {
                        Text("刷新", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加人脸")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // 配置区域
            FaceConfigPanel(
                config = config,
                onThresholdChange = viewModel::updateThreshold,
                onFlipChange = viewModel::toggleCameraFlip,
                onOnlyKnownChange = viewModel::toggleOnlyKnown
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            if (loading && faces.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(faces) { face ->
                        FaceItem(
                            face = face,
                            onDelete = { selectedFaceForDelete = face }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddFaceDialog(
            onDismiss = { showAddDialog = false },
            onUploadImage = { name, desc ->
                currentActionName = name
                currentActionDesc = desc
                imagePicker.pickImage()
                showAddDialog = false
            },
            onTakePhoto = { name, desc ->
                currentActionName = name
                currentActionDesc = desc
                imagePicker.takePhoto()
                showAddDialog = false
            },
            onCaptureRobotFrame = { name, desc ->
                viewModel.captureFromRobotFrame(name, desc)
                showAddDialog = false
            }
        )
    }

    selectedFaceForDelete?.let { face ->
        AlertDialog(
            onDismissRequest = { selectedFaceForDelete = null },
            title = { Text("删除人脸", fontSize = 16.sp) },
            text = { Text("确定要删除 [${face.username}] 吗？", fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFace(face)
                    selectedFaceForDelete = null
                }) {
                    Text("删除", color = Color.Red, fontSize = 14.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedFaceForDelete = null }) {
                    Text("取消", fontSize = 14.sp)
                }
            }
        )
    }
}

@Composable
fun FaceConfigPanel(
    config: FaceConfig,
    onThresholdChange: (Float) -> Unit,
    onFlipChange: (Boolean) -> Unit,
    onOnlyKnownChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("识别配置", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = config.camera_flip, 
                        onCheckedChange = onFlipChange,
                        modifier = Modifier.size(32.dp)
                    )
                    Text("摄像头翻转", style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = config.only_known, 
                        onCheckedChange = onOnlyKnownChange,
                        modifier = Modifier.size(32.dp)
                    )
                    Text("只识别已知人脸", style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("识别阈值: ", style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)
                var thresholdText by remember(config.threshold) { mutableStateOf(config.threshold.toString()) }
                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = {
                        thresholdText = it
                        it.toFloatOrNull()?.let { value ->
                            if (value in 0f..1f) onThresholdChange(value)
                        }
                    },
                    modifier = Modifier.width(80.dp).height(48.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        }
    }
}

@Composable
fun FaceItem(
    face: FaceData,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                if (!face.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = face.imageUrl,
                        contentDescription = face.username,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(face.username.take(1).uppercase(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = face.username, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (!face.description.isNullOrEmpty()) {
                    Text(text = face.description ?: "", fontSize = 12.sp, color = Color.Gray)
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color.Red, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun AddFaceDialog(
    onDismiss: () -> Unit,
    onUploadImage: (String, String) -> Unit,
    onTakePhoto: (String, String) -> Unit,
    onCaptureRobotFrame: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("录入新人脸", fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("姓名", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("描述 (可选)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Text("选择录入方式:", style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)
                
                Button(
                    onClick = { if (name.isNotBlank()) onUploadImage(name, desc) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank()
                ) {
                    Text("从相册选取", fontSize = 13.sp)
                }
                
                Button(
                    onClick = { if (name.isNotBlank()) onTakePhoto(name, desc) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank()
                ) {
                    Text("手机拍照", fontSize = 13.sp)
                }
                
                Button(
                    onClick = { if (name.isNotBlank()) onCaptureRobotFrame(name, desc) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    enabled = name.isNotBlank()
                ) {
                    Text("获取机器人当前帧", fontSize = 13.sp)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", fontSize = 14.sp)
            }
        }
    )
}
