package com.example.placy

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.internal.composableLambda
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.rotationMatrix
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import dev.jordond.compass.Priority
import dev.jordond.compass.geolocation.Geolocator
import dev.jordond.compass.geolocation.GeolocatorResult
import dev.jordond.compass.geolocation.mobile
import kotlinx.coroutines.launch
import io.ktor.websocket.Frame
import kotlinx.datetime.Month
import org.jetbrains.compose.ui.tooling.preview.Preview
import placy.composeapp.generated.resources.Res
import placy.composeapp.generated.resources.*
import ru.sulgik.mapkit.PointF
import ru.sulgik.mapkit.compose.Placemark
import ru.sulgik.mapkit.compose.YandexMap
import ru.sulgik.mapkit.compose.imageProvider
import ru.sulgik.mapkit.compose.rememberCameraPositionState
import ru.sulgik.mapkit.compose.rememberPlacemarkState
import ru.sulgik.mapkit.geometry.Point
import ru.sulgik.mapkit.map.IconStyle
import ru.sulgik.mapkit.map.RotationType
import kotlin.math.max
import kotlin.math.min

val MapCameraSnapshotKey = SemanticsPropertyKey<String>("MapCameraSnapshot")
var SemanticsPropertyReceiver.mapCameraSnapshot by MapCameraSnapshotKey

@Composable
@Preview
fun TempApp() {
    MaterialTheme {
        TempMapScreen()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TempMapScreen() {
    val navController = rememberNavController()
    var marks by remember { mutableStateOf<List<GeoMark>>(emptyList()) }
    var result by remember { mutableStateOf<GeolocatorResult?>(null) }
    val apiService = remember { ApiService(NetworkModule.httpClient) }
    var postViewer by remember { mutableStateOf<PostViewerState>(PostViewerState.Hidden) }
    val scope = rememberCoroutineScope()
    val cameraPositionState = rememberCameraPositionState { position = position.copy(zoom = 12f) }
    val context = LocalPlatformContext.current
    val imageLoader = remember {
        getNextcloudImageLoader(context)
    }
    val geolocator = Geolocator.mobile()

    LaunchedEffect(Unit) {
        marks = apiService.getAllMarks()
    }

    // Навигационный граф
    NavHost(
        navController = navController,
        startDestination = "map"
    ) {
        composable("map") {
            // Основной экран с картой и кнопкой
            Box(modifier = Modifier.fillMaxSize()) {
                // Карта
                YandexMap(
                    modifier = Modifier.fillMaxSize().testTag("map-screen").semantics { contentDescription = "Карта, публикаций: ${marks.size}"; mapCameraSnapshot = cameraPositionState.position.toString() },
                    cameraPositionState = cameraPositionState
                ) {
                    marks.forEach { mark ->
                        Placemark(
                            state = rememberPlacemarkState(
                                Point(mark.latitude, mark.longitude),
                                direction = 25f
                            ),
                            icon = imageProvider(Res.drawable.red_pin),
                            iconStyle = IconStyle(
                                anchor = PointF(0.5f, 1f),
                                rotationType = RotationType.ROTATE,
                                scale = when {
                                    cameraPositionState.position.zoom < 8f -> 0.3f
                                    cameraPositionState.position.zoom < 13f -> 0.4f
                                    else -> 0.5f
                                },
                                zIndex = 1f
                            ),
                            onTap = {
                                postViewer = reducePostViewer(postViewer, PostViewerAction.Select(mark))
                                true
                            }
                        )
                    }
                }

                val visibleMark = when (val state = postViewer) {
                    is PostViewerState.Card -> state.mark
                    is PostViewerState.Photo -> state.mark
                    PostViewerState.Hidden -> null
                }
                if (visibleMark != null) {
                    val imageUrl = "${BuildConfig.SERVER_URL}/remote.php/dav/files/${BuildConfig.USERNAME}/${visibleMark.photoUUID}.jpg"
                    when (postViewer) {
                        is PostViewerState.Photo -> SimpleFullScreenImageViewer(
                            imageUrl = imageUrl,
                            onDismiss = { postViewer = reducePostViewer(postViewer, PostViewerAction.Dismiss) },
                            imageLoader = imageLoader
                        )
                        is PostViewerState.Card -> PostCard(
                            imageUrl = imageUrl,
                            imageLoader = imageLoader,
                            onOpenPhoto = { postViewer = reducePostViewer(postViewer, PostViewerAction.OpenPhoto) },
                            onDismiss = { postViewer = reducePostViewer(postViewer, PostViewerAction.Dismiss) }
                        )
                        PostViewerState.Hidden -> Unit
                    }
                }

// Кнопка камеры
                @OptIn(ExperimentalMaterial3Api::class)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            bottom = WindowInsets.navigationBars
                                .asPaddingValues()
                                .calculateBottomPadding() + 16.dp
                        ),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    IconButton(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        onClick = {
                            scope.launch {
                                val location = geolocator.current(Priority.HighAccuracy)
                                result = location
                                if (location is GeolocatorResult.Success) {
                                    navController.navigate("camera")
                                }
                            }
                        }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_camera_playstore),
                            contentDescription = "Камера",
                            modifier = Modifier.size(72.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }

        composable("camera") {
            // Экран камеры
            CameraView(
                navController = navController, result!!.getOrNull()!!.coordinates.latitude,
                result!!.getOrNull()!!.coordinates.longitude)
        }
    }
}

@Composable
fun PostCard(
    imageUrl: String,
    imageLoader: ImageLoader,
    onOpenPhoto: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp)
                .testTag("post-card")
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Фотография публикации",
                imageLoader = imageLoader,
                modifier = Modifier.fillMaxWidth().height(240.dp).clickable(onClick = onOpenPhoto),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun SimpleFullScreenImageViewer(
    imageUrl: String,
    onDismiss: () -> Unit,
    imageLoader: ImageLoader
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onDismiss)
                .testTag("fullscreen-photo")
        ) {
//            Image(
//                painter = rememberAsyncImagePainter(
//                    model = ImageRequest.Builder(LocalContext.current)
//                        .data(imageUrl)
//                        .crossfade(true)
//                        .build()
//                ),
//                contentDescription = "Полноэкранная фотография",
//                modifier = Modifier.fillMaxSize(),
//                contentScale = ContentScale.Fit
//            )
            AsyncImage(
                model = imageUrl,
                contentDescription = "My Photo from Nextcloud",
                imageLoader = imageLoader,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}