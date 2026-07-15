package com.example.guangnian.feature.face.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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

    val imagePicker = rememberImagePicker { bytes ->
        if (currentActionName.isNotBlank()) {
            viewModel.uploadFaceImage(currentActionName, bytes)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("人脸管理", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            FaceConfigPanel(
                config = config,
                onThresholdChange = viewModel::updateThreshold,
                onFlipChange = viewModel::toggleCameraFlip,
                onOnlyKnownChange = viewModel::toggleOnlyKnown,
                onAddFace = { showAddDialog = true }
            )

            if (loading && faces.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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
            onUploadImage = { name ->
                currentActionName = name
                imagePicker.pickImage()
                showAddDialog = false
            },
            onTakePhoto = { name ->
                currentActionName = name
                imagePicker.takePhoto()
                showAddDialog = false
            },
            onCaptureRobotFrame = { name ->
                viewModel.captureFromRobotFrame(name)
                showAddDialog = false
            }
        )
    }

    selectedFaceForDelete?.let { face ->
        AlertDialog(
            onDismissRequest = { selectedFaceForDelete = null },
            title = { Text("确认删除") },
            text = { Text("是否确定删除用户 [${face.username}] 的人脸数据？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFace(face)
                    selectedFaceForDelete = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedFaceForDelete = null }) {
                    Text("取消")
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
    onOnlyKnownChange: (Boolean) -> Unit,
    onAddFace: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = config.camera_flip,
                    onCheckedChange = onFlipChange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("摄像头翻转", style = MaterialTheme.typography.bodySmall)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = config.only_known,
                    onCheckedChange = onOnlyKnownChange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("仅识别已知", style = MaterialTheme.typography.bodySmall)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("阈值:", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.width(6.dp))
                var thresholdText by remember(config.threshold) { mutableStateOf(config.threshold.toString()) }
                //移除不支持的contentPadding参数
                //移除高度限制以防文本被Material3组件的默认高度截断
                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = {
                        thresholdText = it
                        it.toFloatOrNull()?.let { value ->
                            if (value in 0f..1f) onThresholdChange(value)
                        }
                    },
                    modifier = Modifier.width(80.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            FilledIconButton(
                onClick = onAddFace,
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "新增", modifier = Modifier.size(20.dp))
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
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
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = face.username, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                face.description?.takeIf { it.isNotBlank() }?.let { desc ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun AddFaceDialog(
    onDismiss: () -> Unit,
    onUploadImage: (String) -> Unit,
    onTakePhoto: (String) -> Unit,
    onCaptureRobotFrame: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("录入新人脸") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("姓名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("选择录入方式", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { if (name.isNotBlank()) onUploadImage(name) },
                            modifier = Modifier.weight(1f),
                            enabled = name.isNotBlank(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("相册选取")
                        }
                        OutlinedButton(
                            onClick = { if (name.isNotBlank()) onTakePhoto(name) },
                            modifier = Modifier.weight(1f),
                            enabled = name.isNotBlank(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("拍照上传")
                        }
                    }

                    Button(
                        onClick = { if (name.isNotBlank()) onCaptureRobotFrame(name) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("获取机器人当前帧")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}