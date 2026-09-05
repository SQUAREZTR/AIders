package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.audio.AudioPlaybackState
import com.example.data.model.QuestionBatchItem
import com.example.data.model.QuestionStatus
import com.example.ui.CoachUiState
import com.example.ui.CoachViewModel
import com.example.ui.components.AudioPlayerCard
import com.example.ui.components.CoachResponseCard
import com.example.ui.theme.Amber500
import com.example.ui.theme.Cyan500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Rose500
import java.io.File

@Composable
fun MultiQuestionScreen(
    viewModel: CoachViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Camera launcher
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && tempCameraUri != null) {
            viewModel.addBatchQuestion(imageUri = tempCameraUri)
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            try {
                val dir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
                val file = File(dir, "batch_captured_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                tempCameraUri = uri
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                android.util.Log.e("MultiQuestionScreen", "Error launching camera", e)
            }
        }
    }

    val onCapturePhoto: () -> Unit = {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            try {
                val dir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
                val file = File(dir, "batch_captured_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                tempCameraUri = uri
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                android.util.Log.e("MultiQuestionScreen", "Error launching camera", e)
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Multiple photos picker launcher
    val multiplePhotosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addMultipleBatchQuestions(uris)
        }
    }

    // Single photo changer launcher
    var targetQuestionIdForImageChange by remember { mutableStateOf<String?>(null) }
    val singlePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        targetQuestionIdForImageChange?.let { id ->
            if (uri != null) {
                viewModel.updateBatchQuestionImage(id, uri)
            }
        }
        targetQuestionIdForImageChange = null
    }

    val completedQuestions = uiState.batchQuestions.filter { it.status == QuestionStatus.COMPLETED }
    val selectedItem = uiState.batchQuestions.getOrNull(uiState.selectedBatchIndex)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("multi_question_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 96.dp
        )
    ) {
        // Hero Header
        item {
            MultiQuestionHeader(
                batchCount = uiState.batchQuestions.size,
                completedCount = completedQuestions.size,
                onCapturePhoto = onCapturePhoto,
                onPickMultiplePhotos = {
                    multiplePhotosLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onAddEmptyQuestion = { viewModel.addBatchQuestion() },
                onClearAll = { viewModel.clearBatch() }
            )
        }

        // Empty state with quick sample button
        if (uiState.batchQuestions.isEmpty()) {
            item {
                MultiQuestionEmptyState(
                    onAddSampleBatch = {
                        viewModel.addBatchQuestion(
                            prompt = "Matematik: f(x) = x³ - 3x² + 2 fonksiyonunun yerel ekstremum noktalarını ve dönüm noktasını bul."
                        )
                        viewModel.addBatchQuestion(
                            prompt = "Fizik: Sürtünmeli eğik düzlemde serbest bırakılan m kütleli cismin ivmesi ve serbest cisim diyagramı nasıldır?"
                        )
                        viewModel.addBatchQuestion(
                            prompt = "Kimya: 2A + B -> C tepkimesinde A'nın derişimi 2 katına, B'nin derişimi 3 katına çıktığında tepkime hızı nasıl değişir?"
                        )
                    },
                    onCapturePhoto = onCapturePhoto,
                    onPickMultiplePhotos = {
                        multiplePhotosLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }
        } else {
            // Batch Solving Progress Bar
            if (uiState.isBatchSolving) {
                item {
                    BatchProgressCard(
                        progressText = uiState.batchProgressText,
                        total = uiState.batchQuestions.size,
                        completed = completedQuestions.size
                    )
                }
            }

            // Question Queue List
            item {
                Text(
                    text = "Soru Havuzu (${uiState.batchQuestions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            itemsIndexed(
                items = uiState.batchQuestions,
                key = { _, item -> item.id }
            ) { index, item ->
                BatchQuestionCard(
                    item = item,
                    index = index,
                    isSelected = index == uiState.selectedBatchIndex,
                    isSolving = uiState.isBatchSolving,
                    onSelect = { viewModel.selectBatchQuestion(index) },
                    onPromptChange = { newPrompt -> viewModel.updateBatchQuestionPrompt(item.id, newPrompt) },
                    onChangeImage = {
                        targetQuestionIdForImageChange = item.id
                        singlePhotoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onRemoveImage = { viewModel.updateBatchQuestionImage(item.id, null) },
                    onDelete = { viewModel.removeBatchQuestion(item.id) }
                )
            }

            // CTAs: Solve Only vs Solve & Listen
            item {
                if (uiState.isBatchSolving) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("batch_solving_indicator")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = uiState.batchProgressText.ifBlank { "Sorular Çözülüyor..." },
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 15.sp
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 1. Tümünü Çözdür (Sessiz mod - sadece yazılı adımlar)
                            FilledTonalButton(
                                onClick = { viewModel.solveAllBatchQuestions(autoPlayAudio = false) },
                                enabled = uiState.batchQuestions.isNotEmpty(),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("solve_only_all_batch_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tümünü Çözdür",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            // 2. Çöz & Seslendir (Çöz ve ilk sorudan sesli dinlemeyi başlat)
                            Button(
                                onClick = { viewModel.solveAllBatchQuestions(autoPlayAudio = true) },
                                enabled = uiState.batchQuestions.isNotEmpty(),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .weight(1.15f)
                                    .height(52.dp)
                                    .testTag("solve_and_listen_all_batch_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Çöz & Seslendir",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Text(
                            text = "📝 Çözdür: Yazılı çözümleri hazırlar  •  🎧 Çöz & Seslendir: Sesli anlatımı başlatır",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Results Section (when questions are completed)
            if (completedQuestions.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Çözümler & Sesli Ders Anlatımı",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Question Selector Chips
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(uiState.batchQuestions) { index, qItem ->
                            val isSelected = index == uiState.selectedBatchIndex
                            val isCompleted = qItem.status == QuestionStatus.COMPLETED
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectBatchQuestion(index) },
                                label = {
                                    Text(
                                        text = "Soru ${index + 1} " + if (isCompleted) "✓" else "⏳",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("batch_chip_$index")
                            )
                        }
                    }
                }

                // Continuous Audio Controls
                item {
                    ContinuousAudioBar(
                        isContinuousPlaying = uiState.isContinuousPlaying,
                        currentIndex = uiState.selectedBatchIndex,
                        totalCount = uiState.batchQuestions.size,
                        onToggleContinuous = { viewModel.toggleContinuousBatchAudio() },
                        onPrevious = { viewModel.previousBatchQuestion() },
                        onNext = { viewModel.nextBatchQuestion() },
                        canGoPrevious = uiState.selectedBatchIndex > 0,
                        canGoNext = uiState.selectedBatchIndex < uiState.batchQuestions.size - 1
                    )
                }

                // Selected Question Audio Player
                if (selectedItem?.response != null) {
                    item {
                        AudioPlayerCard(
                            audioState = audioState,
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onSeek = { pos -> viewModel.seekAudio(pos) },
                            onSetSpeed = { speed -> viewModel.setPlaybackSpeed(speed) },
                            onReplay = { viewModel.playBatchQuestionAudio(uiState.selectedBatchIndex) }
                        )
                    }

                    // Solution Details Card
                    item {
                        CoachResponseCard(
                            response = selectedItem.response,
                            imagePath = selectedItem.imageUri?.toString(),
                            onListenClick = {
                                viewModel.playBatchQuestionAudio(uiState.selectedBatchIndex)
                            },
                            isPlaying = audioState.isPlaying
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiQuestionHeader(
    batchCount: Int,
    completedCount: Int,
    onCapturePhoto: () -> Unit,
    onPickMultiplePhotos: () -> Unit,
    onAddEmptyQuestion: () -> Unit,
    onClearAll: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Indigo600, Cyan500)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Çoklu Soru Havuzu",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (batchCount == 0) "Birden fazla soru ekle ve sırayla çöz" else "$batchCount soru eklendi • $completedCount çözüldü",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (batchCount > 0) {
                    IconButton(
                        onClick = onClearAll,
                        modifier = Modifier.testTag("clear_batch_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Tümünü Temizle",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: Kamera, Çoklu Galeri, Boş Soru
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCapturePhoto,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("batch_capture_photo_button")
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Foto Çek", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onPickMultiplePhotos,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("batch_pick_multiple_button")
                ) {
                    Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Çoklu Seç", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onAddEmptyQuestion,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(0.9f)
                        .height(44.dp)
                        .testTag("batch_add_empty_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Soru Yaz", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun MultiQuestionEmptyState(
    onAddSampleBatch: () -> Unit,
    onCapturePhoto: () -> Unit,
    onPickMultiplePhotos: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Birden Fazla Soru Ekle",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Deneme sınavındaki, test kitabındaki veya ödevindeki soruları ister arka arkaya fotoğrafla, ister galeriden topluca seç. Yapay zeka koçun her birini sırayla çözüp seslendirsin!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCapturePhoto,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Fotoğrafla", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onPickMultiplePhotos,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Galeriden Seç", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick demo sample button
            OutlinedButton(
                onClick = onAddSampleBatch,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.tertiary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_sample_batch_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "3 Örnek TYT/AYT Sorusu Yükle (Dene)",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun BatchProgressCard(
    progressText: String,
    total: Int,
    completed: Int
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = "$completed / $total",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val progress = if (total > 0) completed.toFloat() / total.toFloat() else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun BatchQuestionCard(
    item: QuestionBatchItem,
    index: Int,
    isSelected: Boolean,
    isSolving: Boolean,
    onSelect: () -> Unit,
    onPromptChange: (String) -> Unit,
    onChangeImage: () -> Unit,
    onRemoveImage: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("batch_question_card_$index")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Soru #, Status Badge, Delete Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "Soru ${item.questionNumber}",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Status pill
                    StatusPill(status = item.status)
                }

                if (!isSolving) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Soruyu Sil",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Image Preview if attached
            if (item.imageUri != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.05f))
                ) {
                    AsyncImage(
                        model = item.imageUri,
                        contentDescription = "Soru Görseli",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { onChangeImage() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Değiştir",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(4.dp)
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { onRemoveImage() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kaldır",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Text input for question
            OutlinedTextField(
                value = item.prompt,
                onValueChange = onPromptChange,
                placeholder = {
                    Text(
                        text = if (item.imageUri != null) "Görselle ilgili soru/not yazın (isteğe bağlı)..." else "Soru metnini yazın...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("batch_prompt_input_$index")
            )

            // Error message if any
            if (item.errorMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun StatusPill(status: QuestionStatus) {
    val (bgColor, textColor, text) = when (status) {
        QuestionStatus.PENDING -> Triple(Amber500.copy(alpha = 0.15f), Amber500, "Bekliyor ⏳")
        QuestionStatus.PROCESSING -> Triple(Cyan500.copy(alpha = 0.2f), Cyan500, "Çözülüyor...")
        QuestionStatus.COMPLETED -> Triple(Emerald500.copy(alpha = 0.15f), Emerald500, "Çözüldü ✓")
        QuestionStatus.ERROR -> Triple(Rose500.copy(alpha = 0.15f), Rose500, "Hata ⚠️")
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (status == QuestionStatus.PROCESSING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                    color = textColor
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ContinuousAudioBar(
    isContinuousPlaying: Boolean,
    currentIndex: Int,
    totalCount: Int,
    onToggleContinuous: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    canGoPrevious: Boolean,
    canGoNext: Boolean
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isContinuousPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onPrevious,
                    enabled = canGoPrevious,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Önceki Soru",
                        tint = if (canGoPrevious) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }

                Text(
                    text = "${currentIndex + 1} / $totalCount",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                IconButton(
                    onClick = onNext,
                    enabled = canGoNext,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Sonraki Soru",
                        tint = if (canGoNext) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }

            // Continuous podcast-mode button
            Button(
                onClick = onToggleContinuous,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isContinuousPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (isContinuousPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.buttonElevation(2.dp),
                modifier = Modifier.testTag("toggle_continuous_audio_button")
            ) {
                Icon(
                    imageVector = if (isContinuousPlaying) Icons.Default.Stop else Icons.Default.Headphones,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isContinuousPlaying) "Durdur" else "Seri Dinle",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
