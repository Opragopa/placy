package com.example.placy

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.ismoy.imagepickerkmp.domain.config.ImagePickerConfig
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.presentation.ui.components.ImagePickerLauncher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun CameraView(navController: NavController, latitude: Double, longitude: Double) {
    var cameraState by remember { mutableStateOf<CameraState>(CameraState.Loading) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val nextcloudClient = remember { NextcloudClient(NetworkModule.httpClient) }
    val api = remember { ApiService(NetworkModule.httpClient) }
    val publisher = remember { PhotoPublisher(nextcloudClient::uploadImage, api::saveMark) }

    LaunchedEffect(Unit) {
        // Небольшая задержка перед открытием камеры
        delay(300)
        cameraState = CameraState.Ready
    }

    when (cameraState) {
        CameraState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
                Text("Подготовка камеры...")
            }
        }

        CameraState.Ready -> {
            Box (
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(
                        top = WindowInsets.statusBars
                            .asPaddingValues()
                            .calculateTopPadding(),
                        bottom = WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding()
                    )
            ) {
                ImagePickerLauncher(
                    config = ImagePickerConfig(
                        onPhotoCaptured = { result ->
                            cameraState = CameraState.Uploading
                            scope.launch {
                                try {
                                    val photoId = UUID.randomUUID().toString()
                                    val bytes = result.loadBytes()
                                    when (publisher.publish(bytes, photoId, latitude, longitude)) {
                                        PublishResult.Success -> navController.popBackStack()
                                        PublishResult.UploadFailed -> cameraState = CameraState.Error("Не удалось загрузить фотографию")
                                        PublishResult.MetadataFailed -> cameraState = CameraState.Error("Не удалось сохранить публикацию")
                                    }
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    cameraState = CameraState.Error("Не удалось обработать фотографию")
                                }
                            }
                        },
                        onError = { error ->
                            navController.popBackStack() // Возвращаемся при ошибке камеры
                        },
                        onDismiss = {
                            navController.popBackStack() // Возвращаемся при отмене
                        }
                    )
                )
            }

            // Показываем индикатор загрузки во время загрузки фото
            if (cameraState == CameraState.Uploading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                    Text("Загрузка фото...")
                }
            }
        }

        is CameraState.Error -> {
            val errorMessage = (cameraState as CameraState.Error).message
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Ошибка: $errorMessage")
            }
        }

        CameraState.Uploading -> {
            // Уже обрабатывается выше
        }
    }
}

sealed class CameraState {
    object Loading : CameraState()
    object Ready : CameraState()
    object Uploading : CameraState()
    data class Error(val message: String) : CameraState()
}