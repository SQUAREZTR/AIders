package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.School

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.components.AppBottomNavigationBar
import com.example.ui.components.AudioPlayerCard
import com.example.ui.components.CoachResponseCard
import com.example.ui.components.HistoryDrawerContent
import com.example.ui.components.LessonStatisticsDialog
import com.example.ui.components.MiniStatisticsBanner
import com.example.ui.components.QuickPromptChips
import com.example.ui.navigation.AppNavigationTab
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MultiQuestionScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.Amber500
import com.example.ui.theme.Cyan500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Indigo500
import com.example.ui.theme.Indigo600
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachMainScreen(
    viewModel: CoachViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()
    val historyList by viewModel.historyList.collectAsStateWithLifecycle()
    val statistics by viewModel.statistics.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Take photo with camera launcher
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && tempCameraUri != null) {
            viewModel.onImageSelected(tempCameraUri)
        }
    }

    // Camera permission request launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            try {
                val dir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
                val file = File(dir, "captured_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                tempCameraUri = uri
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                android.util.Log.e("CoachMainScreen", "Error launching camera", e)
            }
        }
    }

    val onCapturePhoto: () -> Unit = {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            try {
                val dir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
                val file = File(dir, "captured_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                tempCameraUri = uri
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                android.util.Log.e("CoachMainScreen", "Error launching camera", e)
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        viewModel.onImageSelected(uri)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                HistoryDrawerContent(
                    historyList = historyList,
                    activeItem = uiState.activeHistoryEntity,
                    onSelectItem = { item -> viewModel.loadHistoryItem(item) },
                    onDeleteItem = { id -> viewModel.deleteHistoryItem(id) },
                    onClearAll = { viewModel.clearAllHistory() },
                    onCloseDrawer = { scope.launch { drawerState.close() } },
                    onOpenStatistics = { viewModel.openStatisticsDialog() }
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            topBar = {
                val (titleText, subtitleText, topIcon) = when (uiState.selectedTab) {
                    AppNavigationTab.SOLVER -> Triple("Soru Koçu", "AI Özel Ders & Sınav Koçu", Icons.Default.School)
                    AppNavigationTab.MULTI_QUESTION -> Triple("Çoklu Soru", "Toplu Soru Çözücü & Test Seti", Icons.Default.Layers)
                    AppNavigationTab.HISTORY -> Triple("Ders Arşivi", "Geçmiş Soru & Konu Anlatımları", Icons.Default.History)
                    AppNavigationTab.STATS -> Triple("Gelişim Karnesi", "Sınav Takip & Koç Analizi", Icons.Default.Insights)
                }

                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Indigo600, Amber500)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = topIcon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = titleText,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = subtitleText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("open_history_drawer_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (historyList.isNotEmpty()) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(text = historyList.size.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Geçmiş Anlatımlar Paneli",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    },
                    actions = {
                        when (uiState.selectedTab) {
                            AppNavigationTab.SOLVER -> {
                                IconButton(
                                    onClick = { viewModel.switchTab(AppNavigationTab.STATS) },
                                    modifier = Modifier.testTag("open_statistics_top_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Insights,
                                        contentDescription = "Ders İstatistikleri & Karne",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.toggleAutoPlay() },
                                    modifier = Modifier.testTag("toggle_autoplay_button")
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isAutoPlayEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Outlined.VolumeOff,
                                        contentDescription = if (uiState.isAutoPlayEnabled) "Otomatik Ses Açık" else "Otomatik Ses Kapalı",
                                        tint = if (uiState.isAutoPlayEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (uiState.currentResponse != null) {
                                    IconButton(
                                        onClick = { viewModel.resetToNewAnalysis() },
                                        modifier = Modifier.testTag("reset_new_analysis_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.RestartAlt,
                                            contentDescription = "Yeni Soru / Analiz",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            AppNavigationTab.MULTI_QUESTION -> {
                                IconButton(
                                    onClick = { viewModel.toggleAutoPlay() },
                                    modifier = Modifier.testTag("toggle_autoplay_button_multi")
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isAutoPlayEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Outlined.VolumeOff,
                                        contentDescription = if (uiState.isAutoPlayEnabled) "Otomatik Ses Açık" else "Otomatik Ses Kapalı",
                                        tint = if (uiState.isAutoPlayEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            else -> {}
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                AppBottomNavigationBar(
                    currentTab = uiState.selectedTab,
                    onTabSelected = { viewModel.switchTab(it) }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (uiState.selectedTab) {
                    AppNavigationTab.SOLVER -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Section: Multi-Question Promo
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                MultiQuestionPromoCard(
                                    onNavigateToMulti = { viewModel.switchTab(AppNavigationTab.MULTI_QUESTION) }
                                )
                            }

                            // Section: Input Hero Card
                            item {
                                InputSectionCard(
                                    prompt = uiState.inputPrompt,
                                    selectedImageUri = uiState.selectedImageUri,
                                    isLoading = uiState.isLoading,
                                    onPromptChange = { viewModel.onPromptChanged(it) },
                                    onCapturePhoto = onCapturePhoto,
                                    onPickPhoto = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    onClearPhoto = { viewModel.clearSelectedImage() },
                                    onSolveOnly = { viewModel.startAnalysis(autoPlay = false) },
                                    onSolveAndListen = { viewModel.startAnalysis(autoPlay = true) },
                                    onSelectQuickPrompt = { prompt ->
                                        viewModel.applyQuickPrompt(prompt)
                                    }
                                )
                            }

                            // Mini Statistics & Streak Summary Banner
                            item {
                                MiniStatisticsBanner(
                                    statistics = statistics,
                                    onClick = { viewModel.switchTab(AppNavigationTab.STATS) }
                                )
                            }

                            // Error message banner
                            if (uiState.errorMessage != null) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = uiState.errorMessage ?: "",
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                            }

                            // Loading Indicator Card
                            if (uiState.isLoading) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(28.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(42.dp),
                                                strokeWidth = 4.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = "Öğretmen Analiz Ediyor & Seslendiriyor...",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Sorunun adımları, tuzaklar ve ses kaydı hazırlanıyor.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Section: Audio Player Bar (when an active response or history item is loaded)
                            if (uiState.currentResponse != null) {
                                item {
                                    AudioPlayerCard(
                                        audioState = audioState,
                                        onTogglePlayPause = { viewModel.togglePlayPause() },
                                        onSeek = { viewModel.seekAudio(it) },
                                        onSetSpeed = { viewModel.setPlaybackSpeed(it) },
                                        onReplay = { viewModel.playActiveAudio() }
                                    )
                                }

                                // Section: Result Study Card
                                item {
                                    CoachResponseCard(
                                        response = uiState.currentResponse!!,
                                        imagePath = uiState.activeHistoryEntity?.imagePath,
                                        onListenClick = { viewModel.playActiveAudio() },
                                        isPlaying = audioState.isPlaying
                                    )
                                }
                            }

                            // Section: First-launch Educational Guidance (when no analysis has been performed yet)
                            if (uiState.currentResponse == null && !uiState.isLoading) {
                                item {
                                    FeatureExplanationCards()
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }

                    AppNavigationTab.MULTI_QUESTION -> {
                        MultiQuestionScreen(viewModel = viewModel)
                    }

                    AppNavigationTab.HISTORY -> {
                        HistoryScreen(viewModel = viewModel)
                    }

                    AppNavigationTab.STATS -> {
                        StatsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    // Lesson Statistics & Progress Dialog
    if (uiState.isStatisticsDialogOpen) {
        LessonStatisticsDialog(
            statistics = statistics,
            onDismiss = { viewModel.closeStatisticsDialog() }
        )
    }
}

@Composable
private fun MultiQuestionPromoCard(
    onNavigateToMulti: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("multi_question_promo_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Birden Fazla Sorun mu Var?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Test veya ödevindeki soruları sırayla çek veya galeriden topluca seç; yapay zeka arka arkaya çözsün!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onNavigateToMulti,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Çoklu Çöz",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun InputSectionCard(
    prompt: String,
    selectedImageUri: Uri?,
    isLoading: Boolean,
    onPromptChange: (String) -> Unit,
    onCapturePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onClearPhoto: () -> Unit,
    onSolveOnly: () -> Unit,
    onSolveAndListen: () -> Unit,
    onSelectQuickPrompt: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("input_section_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Selected Image Preview (if any)
            AnimatedVisibility(visible = selectedImageUri != null) {
                if (selectedImageUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(bottom = 12.dp)
                    ) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Yüklenen Görsel",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = onClearPhoto,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(32.dp)
                                .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Görseli Kaldır",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Input Text Field
            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("question_input_field"),
                placeholder = {
                    Text(
                        text = if (selectedImageUri != null) "Görselle ilgili sormak istediğin ek not veya soru..."
                        else "Soru Sor / Konu Yaz (Örn: Türev ekstremum taktikleri veya sorunu yaz)...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Row: Fotoğraf Çek & Galeri
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCapturePhoto,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("take_photo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (selectedImageUri != null) "Yeniden Çek" else "Foto Çek",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = onPickPhoto,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pick_image_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Galeri",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Actions: Dedicated "Çözdür" (Silent) & "Çöz & Dinle" (Voice) Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Çözdür (Sadece yazılı çözüm ve adımlar)
                FilledTonalButton(
                    onClick = onSolveOnly,
                    enabled = !isLoading && (prompt.isNotBlank() || selectedImageUri != null),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("solve_only_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Çözdür",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // 2. Çöz & Dinle (Çözümü yap ve sesli anlatımı hemen başlat)
                Button(
                    onClick = onSolveAndListen,
                    enabled = !isLoading && (prompt.isNotBlank() || selectedImageUri != null),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .weight(1.15f)
                        .height(48.dp)
                        .testTag("solve_and_listen_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Çöz & Dinle",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Explanatory Mode Hint
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📝 Çözdür: Yalnızca yazılı adımlar  •  🎧 Çöz & Dinle: Sesli özel ders",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Prompt Suggestions
            QuickPromptChips(
                onSelectPrompt = onSelectQuickPrompt
            )
        }
    }
}

@Composable
private fun FeatureExplanationCards() {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Koçun Çalışma Prensibi & Yetenekleri",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        FeatureRow(
            icon = Icons.Default.School,
            iconTint = Indigo500,
            title = "1. Akıllı İçerik Tespiti (Soru mu, Konu mu?)",
            desc = "Yalnızca bir soru fotoğrafı atsanız dahi yapay zeka bunun bir soru olduğunu tespit eder; adım adım çözer, doğru şıkkı ve tuzakları çıkarır. Ders notu attığınızda ise taktikli konu anlatımı sunar."
        )

        FeatureRow(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            iconTint = Emerald500,
            title = "2. 'Çözdür' veya 'Çöz & Dinle' Özgürlüğü",
            desc = "Yalnızca yazılı adımları incelemek için 'Çözdür', masada yanınızda anlatan bir öğretmen gibi dinlemek için 'Çöz & Dinle' butonunu kullanabilirsiniz."
        )

        FeatureRow(
            icon = Icons.Default.AutoAwesome,
            iconTint = Amber500,
            title = "3. Sınav Taktikleri & Püf Noktaları",
            desc = "Düz özet yerine hafıza kodlamaları (akrostişler), sınav tuzakları ve formül pratikleri üretilir."
        )

        FeatureRow(
            icon = Icons.Default.History,
            iconTint = Cyan500,
            title = "4. Çevrimdışı Geçmiş & Sıfır Gecikme",
            desc = "Her analiz yerel SQLite veritabanına ve ses önbelleğine kaydedilir. İstediğiniz zaman geçmişten tekrar açıp dinleyebilirsiniz."
        )
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    desc: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
