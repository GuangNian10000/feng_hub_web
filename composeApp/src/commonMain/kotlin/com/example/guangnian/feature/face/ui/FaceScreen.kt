package com.example.guangnian.feature.face.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import com.example.guangnian.feature.face.data.bean.FaceData
import com.example.guangnian.feature.face.data.bean.FaceConfig
import com.example.guangnian.feature.face.viewmodel.FaceViewModel
import com.example.guangnian.core.utils.rememberImagePicker
import com.example.guangnian.core.designsystem.components.ModernDialogColors
import com.example.guangnian.core.designsystem.components.ModernDialogScaffold
import com.example.guangnian.core.designsystem.components.ModernDialogSection
import com.example.guangnian.core.designsystem.components.modernOutlinedTextFieldColors
import org.koin.compose.viewmodel.koinViewModel

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
    var editingFaceName by remember { mutableStateOf("") }
    var editingFaceDesc by remember { mutableStateOf("") }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var showLivePreview by remember { mutableStateOf(false) }
    val currentFrame by viewModel.currentFrame.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val processRunning by viewModel.processRunning.collectAsState()

    val scaffoldState = rememberScaffoldState()

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            scaffoldState.snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val imagePicker = rememberImagePicker { bytes ->
        if (currentActionName.isNotBlank()) {
            viewModel.uploadFaceImage(currentActionName, bytes, currentActionDesc)
            currentActionName = ""
            currentActionDesc = ""
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        scaffoldState = scaffoldState,
        topBar = {
            FaceTopBar(
                faceCount = faces.size,
                loading = loading,
                onBack = onBack,
                onRefresh = viewModel::refresh
            )
        },
        backgroundColor = ModernDialogColors.Canvas
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(ModernDialogColors.Canvas)
                .padding(16.dp)
        ) {
            val openAddDialog = {
                editingFaceName = ""
                editingFaceDesc = ""
                showAddDialog = true
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FaceConfigPanel(
                    config = config,
                    faceCount = faces.size,
                    processRunning = processRunning,
                    compact = false,
                    onThresholdChange = viewModel::updateThreshold,
                    onFlipChange = viewModel::toggleCameraFlip,
                    onOnlyKnownChange = viewModel::toggleOnlyKnown,
                    onAddFace = openAddDialog,
                    onShowLivePreview = { showLivePreview = true },
                    onStartProcess = viewModel::startProcess,
                    onStopProcess = viewModel::stopProcess,
                    onRestartProcess = viewModel::restartProcess,
                    modifier = Modifier.width(268.dp).fillMaxHeight()
                )

                FaceDirectory(
                    faces = faces,
                    loading = loading,
                    onAddFace = openAddDialog,
                    onDelete = { selectedFaceForDelete = it },
                    onEdit = {
                        editingFaceName = it.username
                        editingFaceDesc = it.description ?: ""
                        showAddDialog = true
                    },
                    onImageClick = { previewImageUrl = it },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
    }

    if (showAddDialog) {
        AddFaceDialog(
            initialName = editingFaceName,
            initialDesc = editingFaceDesc,
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
        DeleteFaceDialog(
            face = face,
            onDismiss = { selectedFaceForDelete = null },
            onConfirm = {
                viewModel.deleteFace(face)
                selectedFaceForDelete = null
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
private fun FaceTopBar(
    faceCount: Int,
    loading: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    Surface(color = ModernDialogColors.Surface, elevation = 0.dp) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = ModernDialogColors.PrimaryText
                    )
                }
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text(
                        "人脸识别",
                        color = ModernDialogColors.PrimaryText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
//                    Text(
//                        "共 $faceCount 个人脸样本",
//                        color = ModernDialogColors.SecondaryText,
//                        fontSize = 11.sp
//                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onRefresh, enabled = !loading) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = ModernDialogColors.Accent)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新", tint = ModernDialogColors.SecondaryText, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Divider(color = ModernDialogColors.Divider)
        }
    }
}

@Composable
fun FaceConfigPanel(
    config: FaceConfig,
    faceCount: Int,
    processRunning: Boolean,
    compact: Boolean,
    onThresholdChange: (Float) -> Unit,
    onFlipChange: (Boolean) -> Unit,
    onOnlyKnownChange: (Boolean) -> Unit,
    onAddFace: () -> Unit,
    onShowLivePreview: () -> Unit,
    onStartProcess: () -> Unit = {},
    onStopProcess: () -> Unit = {},
    onRestartProcess: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.border(1.dp, ModernDialogColors.Border, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = ModernDialogColors.Surface,
        elevation = 0.dp
    ) {
        if (compact) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("识别配置", color = ModernDialogColors.PrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                ConfigToggle("翻转", config.camera_flip, onFlipChange, Modifier.width(130.dp))
                ConfigToggle("仅已知", config.only_known, onOnlyKnownChange, Modifier.width(130.dp))
                ThresholdControl(
                    threshold = config.threshold,
                    onThresholdChange = onThresholdChange,
                    modifier = Modifier.width(160.dp)
                )
                FaceActionButton("画面", Icons.Default.Videocam, false, onShowLivePreview)
                FaceActionButton("录入", Icons.Default.PersonAdd, true, onAddFace)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState())) {
                Text("识别配置", color = ModernDialogColors.PrimaryText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("人脸样本: $faceCount", color = ModernDialogColors.SecondaryText, fontSize = 11.sp)
                Spacer(Modifier.height(16.dp))
                
                ModernDialogSection("进程状态")
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(if (processRunning) Color(0xFF28A745) else Color(0xFFDC3545), CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(if (processRunning) "运行中" else "已停止", color = ModernDialogColors.PrimaryText, fontSize = 13.sp)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (processRunning) {
                        FaceActionButton("停止", Icons.Default.Stop, false, onStopProcess, Modifier.weight(1f))
                    } else {
                        FaceActionButton("启动", Icons.Default.PlayArrow, true, onStartProcess, Modifier.weight(1f))
                    }
                    FaceActionButton("重启", Icons.Default.Refresh, false, onRestartProcess, Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                
                ModernDialogSection("匹配阈值")
                Spacer(Modifier.height(6.dp))
                ThresholdControl(config.threshold, onThresholdChange, Modifier.fillMaxWidth())
                Spacer(Modifier.height(14.dp))
                ModernDialogSection("识别行为")
                Spacer(Modifier.height(6.dp))
                ConfigToggle("摄像头画面翻转", config.camera_flip, onFlipChange, Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                ConfigToggle("仅识别已知人脸", config.only_known, onOnlyKnownChange, Modifier.fillMaxWidth())
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(16.dp))
                FaceActionButton("查看实时画面", Icons.Default.Videocam, false, onShowLivePreview, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                FaceActionButton("录入新的人脸", Icons.Default.PersonAdd, true, onAddFace, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun ConfigToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable { onCheckedChange(!checked) }
            .background(ModernDialogColors.Canvas)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = ModernDialogColors.PrimaryText,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(4.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.8f),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ModernDialogColors.Accent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFADB3BD)
            )
        )
    }
}

@Composable
private fun ThresholdControl(
    threshold: Float,
    onThresholdChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderValue by remember(threshold) { mutableFloatStateOf(threshold.coerceIn(0f, 1f)) }

    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("相似度", color = ModernDialogColors.SecondaryText, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text(
                "${(sliderValue * 100).toInt()}%",
                color = ModernDialogColors.Accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onThresholdChange(sliderValue) },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = ModernDialogColors.Accent,
                activeTrackColor = ModernDialogColors.Accent,
                inactiveTrackColor = ModernDialogColors.Border
            )
        )
    }
}

@Composable
private fun FaceActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (primary) {
        Button(
            onClick = onClick,
            modifier = modifier.height(38.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = ModernDialogColors.Accent,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, fontSize = 11.sp)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(38.dp),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, ModernDialogColors.Border),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = ModernDialogColors.PrimaryText,
                backgroundColor = Color.White
            )
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, fontSize = 11.sp)
        }
    }
}

@Composable
private fun FaceDirectory(
    faces: List<FaceData>,
    loading: Boolean,
    onAddFace: () -> Unit,
    onDelete: (FaceData) -> Unit,
    onEdit: (FaceData) -> Unit,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().height(36.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("人脸列表", color = ModernDialogColors.PrimaryText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Text("${faces.size}", color = ModernDialogColors.SecondaryText, fontSize = 11.sp)
        }
        if (loading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = ModernDialogColors.Accent,
                backgroundColor = ModernDialogColors.Divider
            )
        } else {
            Divider(color = ModernDialogColors.Divider, thickness = 1.dp)
            Spacer(Modifier.height(4.dp))
        }

        if (faces.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = ModernDialogColors.Border,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "人脸库为空",
                        color = ModernDialogColors.SecondaryText,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    FaceActionButton("录入人脸", Icons.Default.PersonAdd, true, onAddFace)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(280.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(faces, key = { it.faceId ?: "${it.id}-${it.username}" }) { face ->
                    FaceItem(
                        face = face,
                        onDelete = { onDelete(face) },
                        onEdit = { onEdit(face) },
                        onImageClick = onImageClick
                    )
                }
            }
        }
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
        modifier = Modifier.fillMaxWidth().height(100.dp).border(1.dp, ModernDialogColors.Border, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        backgroundColor = Color.White,
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 80.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ModernDialogColors.Canvas)
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
                        tint = ModernDialogColors.SecondaryText,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = face.username,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ModernDialogColors.PrimaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                face.description?.takeIf { it.isNotBlank() }?.let { desc ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = desc,
                        fontSize = 11.sp,
                        color = ModernDialogColors.SecondaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "编辑",
                        tint = ModernDialogColors.Accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = Color(0xFFD14343),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddFaceDialog(
    initialName: String = "",
    initialDesc: String = "",
    onDismiss: () -> Unit,
    onUploadImage: (String, String) -> Unit,
    onTakePhoto: (String, String) -> Unit,
    onCaptureRobotFrame: (String, String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var desc by remember(initialDesc) { mutableStateOf(initialDesc) }
    val isFormValid = name.isNotBlank() && desc.isNotBlank()

    ModernDialogScaffold(
        title = if (initialName.isEmpty()) "录入人脸" else "更新人脸",
        onDismiss = onDismiss,
        maxWidth = 560.dp,
        maxHeight = 520.dp,
        titleFontSize = 16.sp,
        headerHeight = 44.dp,
        actions = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = ModernDialogColors.SecondaryText, fontSize = 13.sp)
            }
        }
    ) {
        ModernDialogSection("身份信息")

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("姓名 (必填)", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                singleLine = true,
                shape = RoundedCornerShape(6.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                colors = modernOutlinedTextFieldColors()
            )

            OutlinedTextField(
                value = desc,
                onValueChange = { desc = it },
                label = { Text("备注/描述 (必填)", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                singleLine = true,
                shape = RoundedCornerShape(6.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                colors = modernOutlinedTextFieldColors()
            )
        }

        Spacer(Modifier.height(8.dp))
        ModernDialogSection("录入来源")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FaceCaptureSourceButton(
                text = "相册选取",
                icon = Icons.Default.PhotoLibrary,
                enabled = isFormValid,
                onClick = { onUploadImage(name, desc) },
                modifier = Modifier.weight(1f)
            )
            FaceCaptureSourceButton(
                text = "拍照上传",
                icon = Icons.Default.CameraAlt,
                enabled = isFormValid,
                onClick = { onTakePhoto(name, desc) },
                modifier = Modifier.weight(1f)
            )
            FaceCaptureSourceButton(
                text = "机器人画面",
                icon = Icons.Default.CenterFocusStrong,
                enabled = isFormValid,
                primary = true,
                onClick = { onCaptureRobotFrame(name, desc) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FaceCaptureSourceButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false
) {
    val content: @Composable RowScope.() -> Unit = {
        Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 11.sp, maxLines = 1)
    }

    if (primary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = ModernDialogColors.Accent,
                contentColor = Color.White,
                disabledBackgroundColor = ModernDialogColors.Accent.copy(alpha = 0.38f),
                disabledContentColor = Color.White.copy(alpha = 0.7f)
            ),
            elevation = ButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
            content = content
        )
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, if (enabled) ModernDialogColors.Border else ModernDialogColors.Border.copy(alpha = 0.38f)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = ModernDialogColors.PrimaryText,
                backgroundColor = Color.White,
                disabledContentColor = ModernDialogColors.PrimaryText.copy(alpha = 0.38f)
            ),
            content = content
        )
    }
}

@Composable
private fun DeleteFaceDialog(
    face: FaceData,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    ModernDialogScaffold(
        title = "确认删除",
        onDismiss = onDismiss,
        maxWidth = 420.dp,
        maxHeight = 280.dp,
        actions = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = ModernDialogColors.SecondaryText, fontSize = 13.sp)
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFD14343),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("删除", fontSize = 13.sp)
            }
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = Color(0xFFFFE8E8)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD14343), modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "确定删除 \"${face.username}\" 吗？",
                    color = ModernDialogColors.PrimaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "删除后将无法恢复该数据。",
                    color = ModernDialogColors.SecondaryText,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun ImagePreviewDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF101216)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "人脸原图",
                modifier = Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 36.dp),
                contentScale = ContentScale.Fit
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color(0x99000000), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "关闭预览", tint = Color.White)
            }
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

    ModernDialogScaffold(
        title = "实时画面",
        onDismiss = onDismiss,
        maxWidth = 520.dp,
        maxHeight = 520.dp,
        titleFontSize = 14.sp,
        headerHeight = 36.dp,
        showFooter = false,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(6.dp).background(Color(0xFF28A745), CircleShape))
            Spacer(Modifier.width(6.dp))
            Text("机器人实时画面同步中", color = ModernDialogColors.SecondaryText, fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            Text(
                text = "${countdown}s",
                color = ModernDialogColors.Accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF171A1F)),
            contentAlignment = Alignment.Center
        ) {
            if (currentFrame != null) {
                AsyncImage(
                    model = currentFrame,
                    contentDescription = "机器人当前画面",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}
