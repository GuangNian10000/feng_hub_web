package com.example.guangnian.feature.face.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
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
    var editingFaceName by remember { mutableStateOf("") }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var showLivePreview by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentFrame by viewModel.currentFrame.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val imagePicker = rememberImagePicker { bytes ->
        if (currentActionName.isNotBlank()) {
            viewModel.uploadFaceImage(currentActionName, bytes)
            currentActionName = ""
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (isSearching) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("搜索人脸姓名...", style = MaterialTheme.typography.bodyMedium) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text("人脸管理", fontWeight = FontWeight.SemiBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSearching) {
                            isSearching = false
                            viewModel.onSearchQueryChanged("")
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            if (isSearching) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isSearching) "关闭搜索" else "返回"
                        )
                    }
                },
                actions = {
                    if (!isSearching) {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                    }
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
                onAddFace = {
                    editingFaceName = ""
                    showAddDialog = true
                },
                onShowLivePreview = { showLivePreview = true }
            )

            if (loading && faces.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                if (loading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
                }
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(viewModel.filteredFaces) { face ->
                        FaceItem(
                            face = face,
                            onDelete = { selectedFaceForDelete = face },
                            onEdit = {
                                editingFaceName = face.username
                                showAddDialog = true
                            },
                            onImageClick = { url -> previewImageUrl = url }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddFaceDialog(
            initialName = editingFaceName,
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

    previewImageUrl?.let { url ->
        ImagePreviewDialog(
            imageUrl = url,
            onDismiss = { previewImageUrl = null }
        )
    }

    if (showLivePreview) {
        LivePreviewDialog(
            currentFrame = currentFrame,
            onDismiss = {
                showLivePreview = false
                viewModel.clearCurrentFrame()
            },
            onRefresh = { viewModel.fetchCurrentFrame() }
        )
    }
}

@Composable
fun FaceConfigPanel(
    config: FaceConfig,
    onThresholdChange: (Float) -> Unit,
    onFlipChange: (Boolean) -> Unit,
    onOnlyKnownChange: (Boolean) -> Unit,
    onAddFace: () -> Unit,
    onShowLivePreview: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ConfigToggle(
                label = "翻转",
                checked = config.camera_flip,
                onCheckedChange = onFlipChange
            )

            ConfigToggle(
                label = "已知",
                checked = config.only_known,
                onCheckedChange = onOnlyKnownChange
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("阈值", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.width(8.dp))
                var thresholdText by remember(config.threshold) { mutableStateOf(config.threshold.toString()) }
                BasicTextField(
                    value = thresholdText,
                    onValueChange = {
                        thresholdText = it
                        it.toFloatOrNull()?.let { value ->
                            if (value in 0f..1f) onThresholdChange(value)
                        }
                    },
                    modifier = Modifier.width(36.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = onShowLivePreview,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                shape = RoundedCornerShape(8.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(width = 1.dp)
            ) {
                Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("实时画面", style = MaterialTheme.typography.labelMedium)
            }

            Button(
                onClick = onAddFace,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("新增人脸", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun ConfigToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .background(
                if (checked) MaterialTheme.colorScheme.primaryContainer 
                else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = if (checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FaceItem(
    face: FaceData,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onImageClick: (String) -> Unit
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
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .clickable(enabled = !face.imageUrl.isNullOrEmpty()) {
                        face.imageUrl?.let { onImageClick(it) }
                    },
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
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
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

            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "编辑",
                    tint = MaterialTheme.colorScheme.primary
                )
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
    initialName: String = "",
    onDismiss: () -> Unit,
    onUploadImage: (String) -> Unit,
    onTakePhoto: (String) -> Unit,
    onCaptureRobotFrame: (String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialName.isEmpty()) "录入新人脸" else "编辑人脸") },
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

@Composable
fun ImagePreviewDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Full Image",
                modifier = Modifier.fillMaxSize(0.9f),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun LivePreviewDialog(
    currentFrame: ByteArray?,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit
) {
    var countdown by remember { mutableStateOf(3) }

    LaunchedEffect(Unit) {
        while (true) {
            onRefresh()
            for (i in 3 downTo 1) {
                countdown = i
                delay(1000)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "实时画面预览",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = countdown.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentFrame != null) {
                        AsyncImage(
                            model = currentFrame,
                            contentDescription = "Current Frame",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("正在获取画面...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("关闭")
                }
            }
        }
    }
}
