package com.turksat46.carlydashboard

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.hardware.camera2.CameraCharacteristics
import android.media.MediaPlayer
import android.media.session.MediaController
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.OrientationEventListener
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.camera.camera2.internal.Camera2CameraInfoImpl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.impl.CameraFilters
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
//import androidx.compose.foundation.layout.wrapContentHeight // Not used directly, but conceptually
//import androidx.compose.foundation.layout.wrapContentWidth // Not used directly, but conceptually
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.graphics.toColor
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.palette.graphics.Palette
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.FirebaseApp
import com.google.firebase.ml.modeldownloader.CustomModel
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions
import com.google.firebase.ml.modeldownloader.DownloadType
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader
import com.turksat46.carlydashboard.other.MediaInfoHolder
import com.turksat46.carlydashboard.ui.theme.CarlyDashboardTheme
import kotlinx.coroutines.delay
import org.opencv.android.OpenCVLoader
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.core.graphics.createBitmap
import com.turksat46.carlydashboard.dashcam.DashcamRecorder
import com.turksat46.carlydashboard.other.NavigationInfo
import com.turksat46.carlydashboard.other.NavigationInfoHolder



// Define this enum, preferably outside the MainActivity class or in a separate file
enum class InfoScreenLayoutMode {
    DEFAULT,
    MEDIA_MAXIMIZED,
    NAV_MAXIMIZED
}

class MainActivity : ComponentActivity(), Detector.DetectorListener {

    private lateinit var detector: Detector
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var orientationEventListener: OrientationEventListener? = null

    private lateinit var laneSoftPlayer: MediaPlayer
    private lateinit var laneHardPlayer: MediaPlayer

    private lateinit var dashcamRecorder: DashcamRecorder
    private var videoCaptureUseCase: VideoCapture<Recorder>? = null // Halte eine Referenz

    // --- UI States ---
    private val boundingBoxesState = mutableStateOf<List<BoundingBox>>(emptyList())
    private val laneBitmapState = mutableStateOf<Bitmap?>(null)
    private val laneDeviationState = mutableStateOf<Double?>(null)
    private val inferenceTimeState = mutableStateOf(0L)
    private val isEnergySavingEnabled = mutableStateOf(true)
    private val isGpuEnabledState = mutableStateOf(true)
    private val isLaneDetectionEnabledState = mutableStateOf(true)
    private val isDetectorInitialized = mutableStateOf(false)
    private val speedState = mutableStateOf(0.0f)
    private val physicalOrientationState = mutableStateOf(Configuration.ORIENTATION_PORTRAIT)
    private val warningLevelState = mutableStateOf(WarningLevel.None)
    private val isDebugModeState = mutableStateOf(false)

    private val infoPageActive = mutableStateOf(false) // Controls visibility of InfoScreenContent overlay
    private val showSettingsPanelState = mutableStateOf(false) // Controls visibility of SettingsPanel overlay

    private val showSettingsButton = mutableStateOf(true)

    private val detectedSignResourceIdsState = mutableStateOf<List<Int>>(emptyList())

    private var lastAnalyzedBitmapWidth = mutableStateOf(1)
    private var lastAnalyzedBitmapHeight = mutableStateOf(1)


    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        createLocationCallback()
        createOrientationListener()
        initializeMediaPlayers()
        dashcamRecorder = DashcamRecorder(this, cameraExecutor)

        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Internal OpenCV library not found.")
            showError("OpenCV konnte nicht geladen werden!")
        } else {
            Log.i("OpenCV", "OpenCV loaded successfully!")
        }

        setContent {
            CarlyDashboardTheme {
                FirebaseApp.initializeApp(this)

                val permissionsState = rememberMultiplePermissionsState(
                    permissions = listOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
                LaunchedEffect(Unit) {
                    if (!permissionsState.allPermissionsGranted) {
                        permissionsState.launchMultiplePermissionRequest()
                    }
                }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when {
                        !permissionsState.allPermissionsGranted -> {
                            LoadingIndicator("Kamera- & Standortberechtigung erforderlich.")
                        }
                        !isDetectorInitialized.value -> {
                            warningView() // Shows "Wichtiger Hinweis" and "Initialisiere..."
                        }
                        else -> {
                            // Main layout when permissions are granted and detector is initialized
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Layer 1: Camera Preview, Detection Results, Warning Flash
                                CameraPreviewAndDetectionLayer(
                                    boundingBoxes = boundingBoxesState.value,
                                    laneBitmap = laneBitmapState.value,
                                    warningLevel = warningLevelState.value,
                                    analyzedBitmapWidth = lastAnalyzedBitmapWidth.value,
                                    analyzedBitmapHeight = lastAnalyzedBitmapHeight.value,
                                    onBitmapAnalyzed = ::analyzeBitmap,
                                    analysisExecutor = cameraExecutor,
                                    physicalOrientation = physicalOrientationState.value
                                )

                                // Layer 2: Info Screen Content (Android Auto Style Overlay)
                                // This is drawn on top of camera, below main controls
                                AnimatedVisibility(
                                    visible = infoPageActive.value,
                                    enter = fadeIn(animationSpec = tween(600)),
                                    exit = fadeOut(animationSpec = tween(300))
                                ) {
                                    InfoScreenContent(
                                        physicalOrientation = physicalOrientationState.value,
                                        onClose = { infoPageActive.value = false },
                                    )
                                }

                                // Layer 3: Persistent Controls and Info (Speed, Signs, Buttons)
                                // This is the "chrome" that's always on top of camera and info content
                                InfoAndControlsOverlay(
                                    inferenceTime = inferenceTimeState.value,
                                    speed = speedState.value,
                                    laneDeviation = laneDeviationState.value,
                                    erkannteSchilderResourceIds = detectedSignResourceIdsState.value,
                                    isDebuggingEnabled = isDebugModeState.value,
                                    physicalOrientation = physicalOrientationState.value,
                                    showSettingsButton = showSettingsButton.value,
                                    onSettingsToggle = { showSettingsPanelState.value = !showSettingsPanelState.value },
                                    onShowInfoScreenRequest = {
                                        if ( infoPageActive.value){
                                            showSettingsButton.value = true
                                            infoPageActive.value = false
                                        }
                                        else{
                                            showSettingsButton.value = false
                                            infoPageActive.value = true
                                        }
                                    },
                                    isInfoScreenActive = infoPageActive.value
                                )

                                // Layer 4: Settings Panel Overlay (Topmost conditional overlay)
                                val settingsIconSize = 60.dp // from InfoAndControlsOverlay
                                val settingsIconPadding = 16.dp // from InfoAndControlsOverlay
                                val topOffsetForSettingsPanel = settingsIconSize + settingsIconPadding + 8.dp // 8dp buffer

                                AnimatedVisibility(
                                    visible = showSettingsPanelState.value,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(
                                            top = if (physicalOrientationState.value == Configuration.ORIENTATION_LANDSCAPE) settingsIconPadding else topOffsetForSettingsPanel,
                                            end = settingsIconPadding,
                                            bottom = (if (physicalOrientationState.value == Configuration.ORIENTATION_LANDSCAPE) 16 else 100).dp // Avoid info button area
                                        )
                                        .fillMaxHeight()
                                        .widthIn(max = 300.dp),
                                    enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                                    exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                                ) {
                                    SettingsPanel(
                                        isEnergySavingEnabled = isEnergySavingEnabled.value,
                                        isGpuEnabled = isGpuEnabledState.value,
                                        isLaneDetectionEnabled = isLaneDetectionEnabledState.value,
                                        isDebuggingEnabled = isDebugModeState.value,
                                        onGpuToggle = ::toggleGpu,
                                        onLaneDetectionToggle = ::toggleLaneDetection,
                                        onDebugViewToggle = ::toggleDebugView,
                                        onDismissRequest = { showSettingsPanelState.value = false },
                                        onEnergySavingToggle = {isEnergySavingEnabled.value = !isEnergySavingEnabled.value},
                                    )
                                }
                            }
                        }
                    }
                }

                val conditions = CustomModelDownloadConditions.Builder().build()
                FirebaseModelDownloader.getInstance()
                    .getModel("street-sign-detection", DownloadType.LOCAL_MODEL_UPDATE_IN_BACKGROUND, conditions)
                    .addOnSuccessListener { model: CustomModel? ->
                        val modelFile = model?.file
                        if(modelFile != null){
                            initializeDetector(modelFile)
                        } else {
                            Log.e("FirebaseModel", "Model file is null after download.")
                            showError("Fehler beim Laden des Modells.")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirebaseModel", "Model download failed.", e)
                        showError("Modell konnte nicht heruntergeladen werden.")
                    }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner, permissionsState.allPermissionsGranted) {
                    val observer = LifecycleEventObserver { _, event ->
                        handleLifecycleEvent(event, permissionsState.allPermissionsGranted)
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    if (permissionsState.allPermissionsGranted) {
                        orientationEventListener?.enable()
                        startLocationUpdates()
                    }
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                        orientationEventListener?.disable()
                        stopLocationUpdates()
                    }
                }

                LaunchedEffect(warningLevelState.value) {
                    if (warningLevelState.value != WarningLevel.None) {
                        delay(350L)
                        if (warningLevelState.value != WarningLevel.None) {
                            warningLevelState.value = WarningLevel.None
                        }
                    }
                }
            }
        }
    }

    private fun initializeDetector(modelFile: File) {
        if (!isDetectorInitialized.value) {
            cameraExecutor.execute {
                try {
                    Log.w("FilePath", "Model file path: ${modelFile.path}"+modelFile.extension)
                    detector = Detector(
                        context = baseContext,
                        modelPath = modelFile.path,
                        labelPath = Constants.LABELS_PATH,
                        detectorListener = this,
                        modelFile = modelFile
                    )
                    runOnUiThread {
                        isDetectorInitialized.value = true
                        Log.d("MainActivity", "Detector initialized successfully.")
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error initializing Detector", e)
                    runOnUiThread { showError("Fehler beim Initialisieren des Detektors.") }
                }
            }
        }
    }

    private fun initializeMediaPlayers() {
        try {
            laneSoftPlayer = MediaPlayer.create(this, R.raw.lanesoftwarning)
            laneHardPlayer = MediaPlayer.create(this, R.raw.lanehardwarning)
            laneSoftPlayer.setVolume(1f, 1f)
            laneHardPlayer.setVolume(1f, 1f)
        } catch (e: Exception) {
            Log.e("MediaPlayer", "Error initializing media players", e)
            showError("Warn Töne konnten nicht geladen werden.")
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun handleLifecycleEvent(event: Lifecycle.Event, hasPermissions: Boolean) {
        if (hasPermissions) {
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    orientationEventListener?.enable()
                    startLocationUpdates()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    orientationEventListener?.disable()
                    stopLocationUpdates()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    releaseMediaPlayers()
                }
                else -> {}
            }
        } else {
            orientationEventListener?.disable()
            stopLocationUpdates()
            releaseMediaPlayers()
        }
    }

    private fun releaseMediaPlayers() {
        if (::laneSoftPlayer.isInitialized) {
            try { laneSoftPlayer.release() } catch (e: Exception) {Log.e("MediaPlayer", "Error releasing soft player", e)}
        }
        if (::laneHardPlayer.isInitialized) {
            try { laneHardPlayer.release() } catch (e: Exception) {Log.e("MediaPlayer", "Error releasing hard player", e)}
        }
    }

    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    if (location.hasSpeed()) {
                        speedState.value = location.speed
                    }
                }
            }
            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                if (!locationAvailability.isLocationAvailable) {
                    Log.w("Location", "Location not available.")
                    speedState.value = 0.0f
                }
            }
        }
    }

    private fun createOrientationListener() {
        orientationEventListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                val newOrientation = when (orientation) {
                    in 45..134 -> Configuration.ORIENTATION_LANDSCAPE
                    in 225..314 -> Configuration.ORIENTATION_LANDSCAPE
                    else -> Configuration.ORIENTATION_PORTRAIT
                }
                if (newOrientation != physicalOrientationState.value) {
                    physicalOrientationState.value = newOrientation
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setMinUpdateIntervalMillis(500).build()
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            Log.d("Location", "Location updates requested.")
        } catch (e: SecurityException) { Log.e("Location", "Failed to start location updates.", e) }
    }

    private fun stopLocationUpdates() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            Log.d("Location", "Location updates stopped.")
        } catch (e: Exception) { Log.e("Location", "Error stopping location updates", e) }
    }

    private fun toggleGpu(enabled: Boolean) {
        if (isGpuEnabledState.value != enabled) {
            isGpuEnabledState.value = enabled
            boundingBoxesState.value = emptyList()
            inferenceTimeState.value = 0L
            laneBitmapState.value = null
            laneDeviationState.value = null
            cameraExecutor.submit {
                if (::detector.isInitialized) {
                    try {
                        Log.d("MainActivity", "Restarting detector with GPU=${enabled}...")
                        detector.restart(enabled)
                        Log.d("MainActivity", "Detector restarted.")
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error restarting detector", e)
                        runOnUiThread { showError("Fehler beim Umschalten der GPU.") }
                    }
                }
            }
        }
    }



    private fun toggleLaneDetection(enabled: Boolean) {
        isLaneDetectionEnabledState.value = enabled
        if (!enabled) {
            laneBitmapState.value = null
            laneDeviationState.value = null
            warningLevelState.value = WarningLevel.None
        }
    }

    private fun toggleDebugView(enabled: Boolean) {
        isDebugModeState.value = enabled
        LaneDetector.drawDebugMask = enabled
        LaneDetector.drawDebugEdges = enabled
        LaneDetector.drawDebugHoughLines = enabled
    }

    private fun analyzeBitmap(bitmap: Bitmap) {
        lastAnalyzedBitmapWidth.value = bitmap.width
        lastAnalyzedBitmapHeight.value = bitmap.height

        if (::detector.isInitialized && !cameraExecutor.isShutdown && !isEnergySavingEnabled.value) {
            detector.detect(bitmap)

            if (isLaneDetectionEnabledState.value) {
                val laneDetectionResult: Pair<Bitmap?, Double?>? = try {
                    LaneDetector.detectLanes(bitmap)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error calling LaneDetector.detectLanes", e)
                    null
                }
                runOnUiThread {
                    laneBitmapState.value = laneDetectionResult?.first
                    laneDeviationState.value = laneDetectionResult?.second
                    handleLaneDeviation(laneDetectionResult?.second)
                }
            } else {
                if (laneBitmapState.value != null || laneDeviationState.value != null) {
                    runOnUiThread {
                        laneBitmapState.value = null
                        laneDeviationState.value = null
                        warningLevelState.value = WarningLevel.None
                    }
                }
            }
        }
    }

    private fun handleLaneDeviation(deviation: Double?) {
        try {
            if (deviation != null) {
                val absDeviation = abs(deviation)
                var newWarningLevel = WarningLevel.None
                var playHard = false
                var playSoft = false

                if (absDeviation > 0.30) {
                    Log.d("MainActivity", "Lane deviation: $deviation critical!")
                    newWarningLevel = WarningLevel.Hard
                    playHard = true
                } else if (absDeviation > 0.14) {
                    Log.d("MainActivity", "Lane deviation: $deviation moderate.")
                    if (warningLevelState.value != WarningLevel.Hard) {
                        newWarningLevel = WarningLevel.Soft
                        playSoft = true
                    } else {
                        newWarningLevel = WarningLevel.Hard
                    }
                }

                if (playHard && ::laneHardPlayer.isInitialized) {
                    if (!laneHardPlayer.isPlaying) {
                        try {
                            laneHardPlayer.seekTo(0)
                            laneHardPlayer.start()
                        } catch (e: IllegalStateException){ Log.e("MediaPlayer", "Hard player start failed", e)}
                    }
                    if (::laneSoftPlayer.isInitialized && laneSoftPlayer.isPlaying) {
                        try { laneSoftPlayer.pause() } catch(e: IllegalStateException){ Log.e("MediaPlayer", "Soft player pause failed", e)}
                    }
                } else if (playSoft && ::laneSoftPlayer.isInitialized) {
                    if (::laneHardPlayer.isInitialized && !laneHardPlayer.isPlaying && !laneSoftPlayer.isPlaying) {
                        try {
                            laneSoftPlayer.seekTo(0)
                            laneSoftPlayer.start()
                        } catch (e: IllegalStateException){ Log.e("MediaPlayer", "Soft player start failed", e)}
                    }
                }

                if (warningLevelState.value != newWarningLevel) {
                    warningLevelState.value = newWarningLevel
                }
            }
        } catch (e: Exception) {
            Log.w("MainActivity", "Error checking lane deviation or playing sound.", e)
        }
    }

    override fun onEmptyDetect() {
        runOnUiThread {
            if (!isDestroyed && !isFinishing) {
                boundingBoxesState.value = emptyList()
                detectedSignResourceIdsState.value = emptyList() // Clear signs on empty detect
            }
        }
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        val freshresourceIds = boundingBoxes.map { bbox ->
            ProcessDetectorData.getDrawableResourceIdForClass(bbox.cls)
        }
        runOnUiThread {
            if (!isDestroyed && !isFinishing) {
                boundingBoxesState.value = boundingBoxes
                inferenceTimeState.value = inferenceTime
                detectedSignResourceIdsState.value = freshresourceIds.distinct()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        orientationEventListener?.disable()
        releaseMediaPlayers()
        if (::cameraExecutor.isInitialized && !cameraExecutor.isShutdown) {
            cameraExecutor.shutdown()
            Log.d("MainActivity", "CameraExecutor shutdown.")
        }
        if (::detector.isInitialized) {
            try {
                detector.close()
                Log.d("MainActivity", "Detector closed.")
            } catch (e: Exception) { Log.e("MainActivity", "Error closing detector", e) }
        }
        Log.d("MainActivity", "onDestroy completed.")
    }
}

// -------------------- Composables --------------------

@Composable
fun LoadingIndicator(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(text)
        }
    }
}

@Composable
fun warningView(){
    Box(modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center){
        Column (horizontalAlignment = Alignment.CenterHorizontally){
            Icon(Icons.Filled.Warning, "Warnung", Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text("Wichtiger Hinweis", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Die Nutzung von Car.ly Dashboard während der Fahrt sollte stets unter Beachtung der geltenden Verkehrsregeln und unter Vermeidung von Ablenkung erfolgen. Achte darauf, dein Smartphone sicher zu befestigen und deine Aufmerksamkeit dem Straßenverkehr zu widmen.",
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator()
            Text("Initialisiere...", fontSize = 12.sp, color = Color.Gray)
        }
    }
}


@Composable
fun CameraPreviewAndDetectionLayer(
    boundingBoxes: List<BoundingBox>,
    laneBitmap: Bitmap?,
    warningLevel: WarningLevel,
    analyzedBitmapWidth: Int,
    analyzedBitmapHeight: Int,
    onBitmapAnalyzed: (Bitmap) -> Unit,
    analysisExecutor: ExecutorService,
    physicalOrientation: Int
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    val overlayView = remember(context) { OverlayView(context, null) }


    LaunchedEffect(cameraProviderFuture, previewView) {
        previewView?.let { pv ->
            val cameraProvider = try { cameraProviderFuture.get() } catch (e: Exception) { null }
            cameraProvider?.let {
                bindCameraUseCases(context, it, lifecycleOwner, pv, onBitmapAnalyzed, analysisExecutor)
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "WarningPulse")
    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = if(warningLevel == WarningLevel.None) 0.05f else if (warningLevel == WarningLevel.Soft) 0.4f else 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "WarningAlpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }.also { previewView = it }
            },
            modifier = Modifier.fillMaxSize()
        )

        AndroidView(
            factory = { overlayView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.setResults(boundingBoxes, analyzedBitmapWidth, analyzedBitmapHeight)
                view.setLaneBitmap(laneBitmap, analyzedBitmapWidth, analyzedBitmapHeight)
            }
        )

        if (warningLevel != WarningLevel.None) {
            val baseColor = if (warningLevel == WarningLevel.Soft) Color.Yellow else Color.Red
            val gradient = Brush.horizontalGradient(
                colors = listOf(baseColor.copy(alpha = animatedAlpha), Color.Transparent, baseColor.copy(alpha = animatedAlpha))
            )
            Box(modifier = Modifier
                .fillMaxSize()
                .background(gradient))
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun InfoScreenContent(
    physicalOrientation: Int,
    onClose: () -> Unit,
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    // Approximate heights for chrome elements to calculate padding
    // These are estimations and might need adjustment or dynamic calculation for perfect fit
    val speedCardHeightDp = with(density) { (if (physicalOrientation == Configuration.ORIENTATION_LANDSCAPE) 60.sp else 70.sp).toDp() + 20.dp } // text + padding
    val signBubbleHeightDp = (if (physicalOrientation == Configuration.ORIENTATION_LANDSCAPE) 60.dp else 70.dp)
    val spacingBelowSpeedCard = 10.dp
    val settingsButtonSize = 60.dp
    val settingsButtonPadding = 16.dp

    val topChromeHeight = if (physicalOrientation == Configuration.ORIENTATION_LANDSCAPE) {
        (speedCardHeightDp + signBubbleHeightDp + spacingBelowSpeedCard).coerceAtLeast(settingsButtonSize + settingsButtonPadding)
    } else {
        speedCardHeightDp + signBubbleHeightDp + spacingBelowSpeedCard // Speed/Signs are centered top
    } + settingsButtonPadding // Add general top padding for settings button area clearance


    val infoButtonSize = 64.dp
    val infoButtonPadding = 16.dp
    val laneIndicatorHeight = 18.dp
    val laneIndicatorBottomPadding = 24.dp // Base padding for lane indicator

    val bottomChromeHeight = if (physicalOrientation == Configuration.ORIENTATION_PORTRAIT) {
        // In portrait, lane indicator is above info button
        infoButtonSize + infoButtonPadding + laneIndicatorHeight + laneIndicatorBottomPadding
    } else {
        // In landscape, info button and lane indicator might be more side-by-side conceptually,
        // but we need to clear the tallest element from the bottom edge.
        (infoButtonSize + infoButtonPadding).coerceAtLeast(laneIndicatorHeight + laneIndicatorBottomPadding)
    }

    // Observe NavigationInfo
    val navigationInfoState by NavigationInfoHolder.currentNavigationInfo.observeAsState()
    val navInfo = navigationInfoState // Das ist jetzt ein NavigationInfo? Objekt


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f)) // Slightly more opaque
            .clickable(enabled = false) {} // Consume clicks
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar for an explicit close button for this panel, if desired,
            // even if the main toggle button also closes it.

            // Content Panels Area - Padded to avoid InfoAndControlsOverlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = 30.dp, // Add a small buffer
                        bottom = 40.dp,
                        start = 100.dp,
                        end = 16.dp
                    )
            ) {
                if (physicalOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        InfoPanelMedia(modifier = Modifier.weight(1.3f))
                        Column(modifier = Modifier.weight(0.7f).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp) ) {
                            InfoPanel("Benachrichtigung", modifier = Modifier.weight(0.8f)) { Text("Simple Benachrichtigung") }
                            InfoPanelNavigation(navInfo = navInfo, modifier = Modifier.weight(1.2f))
                        }
                    }
                } else { // Portrait
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        InfoPanelNavigation(navInfo = navInfo, modifier = Modifier
                            .weight(0.7f)
                            .fillMaxWidth())
                        InfoPanelMedia(modifier = Modifier
                            .weight(1.3f)
                            .fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
fun InfoPanel(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.8f)), // Slightly more opaque
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.Start, // Text linksbündig
            verticalArrangement = Arrangement.SpaceBetween // Verteilt Inhalt
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Notifications, // Ein passendes Icon
                    contentDescription = "Benachrichtigung",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                content()
            }
        }
    }
}

// Neue Composable für das Navigations-Panel
@Composable
fun InfoPanelNavigation(navInfo: NavigationInfo?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp) // Mehr Padding für besseres Aussehen
                .fillMaxSize(),
            horizontalAlignment = Alignment.Start, // Text linksbündig
            verticalArrangement = Arrangement.SpaceBetween // Verteilt Inhalt
        ) {
            Column { // Für obere Nav-Infos
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.baseline_navigation_24), // Ein passendes Icon
                        contentDescription = "Navigation",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Navigation",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(12.dp))

                if (navInfo?.isActive == true) {
                    navInfo.nextInstruction?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleLarge, // Größer für die Hauptanweisung
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    navInfo.currentRoad?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.LightGray,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    navInfo.etaDetails?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Cyan.copy(alpha = 0.9f), // Akzentfarbe für ETA
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Text(
                        "Keine aktive Navigation von Google Maps erkannt.",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp)
                    )
                }
            }

            // Platz für einen Button, falls gewünscht
            if (navInfo?.isActive == true) {
                Button(
                    onClick = {
                       /*
                        //Versuche, Google Maps zu öffnen (falls es nicht schon im Vordergrund ist)
                        val gmmIntentUri = Uri.parse("google.navigation:q=") // Leere Query öffnet Maps
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        try {
                            startActivity(LocalContext.current, mapIntent, null)
                        } catch (e: Exception) {
                            Log.e("InfoPanelNavigation", "Konnte Google Maps nicht starten", e)
                        }

                        */


                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                ) {
                    Text("Maps öffnen")
                }
            }
        }
    }
}

// InfoPanelMedia Composable
@RequiresApi(Build.VERSION_CODES.O) // Already there
@Composable
fun InfoPanelMedia(modifier: Modifier = Modifier) {
    val currentTrackState by MediaInfoHolder.currentTrack.observeAsState()
    val isPlaying by MediaInfoHolder.isPlaying.observeAsState(false)
    val sourceApp by MediaInfoHolder.currentSourceApp.observeAsState() // No longer direct, get from trackInfo.packageName

    val context = LocalContext.current
    val trackInfo = currentTrackState // Get the TrackInfo object
    //val sourceApp = trackInfo?.packageName // Get source app from TrackInfo

    var paletteState by remember { mutableStateOf<Palette?>(null) }
    LaunchedEffect(trackInfo?.albumArt) {
        if (trackInfo?.albumArt != null) {
            paletteState = Palette.from(trackInfo.albumArt).generate()
        } else {
            paletteState = null // Clear palette if no art
        }
    }
    val palette = paletteState

    // Create MediaController from token if available
    val mediaController = remember(trackInfo?.sessionToken, context) {
        trackInfo?.sessionToken?.let { token ->
            try {
                MediaController(context, token)
            } catch (e: Exception) {
                Log.e("InfoPanelMedia", "Error creating MediaController from token", e)
                null
            }
        }
    }

    // Start the service, if not already running (idempotent)
    LaunchedEffect(Unit) {
        try {
            val serviceIntent = Intent(context, MyMediaSessionListenerService::class.java)
            context.startService(serviceIntent)
        } catch (e: Exception) {
            Log.e("InfoPanelMedia", "Error starting MyMediaSessionListenerService", e)
        }
    }

    MediaPermissionChecker {
        val cardContainerColor = palette?.getDarkMutedColor(android.graphics.Color.TRANSPARENT)?.let {
            if (it != android.graphics.Color.TRANSPARENT) Color(it).copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.6f)
        } ?: Color.Black.copy(alpha = 0.6f)

        Card(
            modifier = modifier.fillMaxHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = cardContainerColor
            ),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) { // Changed from fillMaxWidth(0.5f)
                trackInfo?.albumArt?.let { art ->
                    Image(
                        bitmap = art.asImageBitmap(),
                        contentDescription = "Album Art Background",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(radius = 0.dp) // Slight blur for background
                            .animateContentSize(),
                        alpha = 0.9f // Slightly transparent
                    )
                    Box( // Darkening overlay for better text contrast
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    val dominantSwatch = palette?.vibrantSwatch
                    //val titleTextColor = dominantSwatch?.titleTextColor?.let { Color(it).copy(alpha = 1f) } ?: Color.White
                    val titleTextColor = Color.White
                    val bodyTextColor = dominantSwatch?.bodyTextColor?.let { Color(it).copy(alpha = 0.8f) } ?: Color.LightGray.copy(alpha = 0.9f)
                    val labelTextColor = titleTextColor.copy(alpha = 0.7f)

                    val textInfoBackgroundColor = palette?.getDarkVibrantColor(android.graphics.Color.TRANSPARENT)?.let {
                        if (it != android.graphics.Color.TRANSPARENT) Color(it).copy(alpha = 0.3f)
                        else Color.Black.copy(alpha = 0.9f)
                    } ?: Color.Black.copy(alpha = 0.4f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(
                                textInfoBackgroundColor.copy(alpha = 0.8f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            //.fillMaxWidth() // Allow text to take width
                    ) {
                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = trackInfo?.title ?: "Keine Wiedergabe",
                            style = MaterialTheme.typography.titleLarge,
                            color = titleTextColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                        if (trackInfo?.artist != null) {
                            Text(
                                text = trackInfo.artist,
                                style = MaterialTheme.typography.bodyLarge,
                                color = bodyTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                        }
                        if (sourceApp != null && trackInfo?.title != null) { // Show source only if there's a track
                            Text(
                                text = "via $sourceApp", // You might want to map package name to app name
                                style = MaterialTheme.typography.labelMedium,
                                color = labelTextColor,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }



                    if (trackInfo != null) {
                        val transportControls = remember(mediaController) { mediaController?.transportControls }
                        val canControl = transportControls != null

                        val controlBgColor = palette?.getDarkVibrantColor(android.graphics.Color.TRANSPARENT)?.let { colorInt ->
                            if (colorInt != android.graphics.Color.TRANSPARENT) Color(colorInt).copy(alpha = 0.7f)
                            else Color.Black.copy(alpha = 0.55f)
                        } ?: Color.Black.copy(alpha = 0.55f)

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 8.dp)){
                            MediaControls(
                                isPlaying = isPlaying,
                                backgroundColor = controlBgColor,
                                onPlay = { transportControls?.play() },
                                onPause = { transportControls?.pause() },
                                onSkipNext = { transportControls?.skipToNext() },
                                onSkipPrevious = { transportControls?.skipToPrevious() },
                                canControl = canControl
                            )
                            Spacer(Modifier.height(8.dp))
                        }


                    } else {
                        Text(
                            "Öffne eine Medien-App auf deinem Handy.",
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
// MediaControls Composable - Modified to accept lambdas and control flag
@Composable
fun MediaControls(
    isPlaying: Boolean,
    backgroundColor: Color,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    canControl: Boolean,

) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(28.dp)), // More rounded
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconTintColor = if (canControl) Color.White else Color.Gray.copy(alpha = 0.7f)
        IconButton(onClick = onSkipPrevious, enabled = canControl) {
            Icon(
                ImageVector.vectorResource(R.drawable.baseline_skip_previous_24),
                "Vorheriger",
                tint = iconTintColor,
                modifier = Modifier.size(30.dp) // Slightly smaller
            )
        }
        IconButton(
            onClick = if (isPlaying) onPause else onPlay,
            enabled = canControl,
            modifier = Modifier
                .size(52.dp) // Larger central button
                .background(
                    if (canControl) Color.White.copy(alpha = 0.1f) else Color.Transparent,
                    CircleShape
                )
        ) {
            Icon(
                imageVector = if (isPlaying) ImageVector.vectorResource(R.drawable.baseline_pause_24) else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = iconTintColor,
                modifier = Modifier.size(if (isPlaying) 30.dp else 36.dp) // Pause icon can be smaller
            )
        }
        IconButton(onClick = onSkipNext, enabled = canControl) {
            Icon(
                ImageVector.vectorResource(R.drawable.baseline_skip_next_24),
                "Nächster",
                tint = iconTintColor,
                modifier = Modifier.size(30.dp) // Slightly smaller
            )
        }
    }
}

// In deiner Activity oder einem Composable
fun requestNotificationListenerPermission(context: Context) {
    val cn = ComponentName(context, MyMediaSessionListenerService::class.java)
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    val enabled = flat != null && flat.contains(cn.flattenToString())

    if (!enabled) {
        Toast.makeText(context, "Bitte Zugriff auf Benachrichtigungen für open::Dashboard aktivieren.", Toast.LENGTH_LONG).show()
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        // Prüfen, ob der Intent aufgelöst werden kann, bevor er gestartet wird
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "Systemeinstellung nicht gefunden.", Toast.LENGTH_SHORT).show()
            // Fallback: Generelle App-Info-Seite
            val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", context.packageName, null)
            fallbackIntent.data = uri
            if (fallbackIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(fallbackIntent)
            }
        }
    }
}

// Composable, um den Status zu prüfen und den Button anzuzeigen
@Composable
fun MediaPermissionChecker(onPermissionGranted: @Composable () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }

    // Funktion, um den Berechtigungsstatus zu prüfen
    fun checkPermission(): Boolean {
        val cn = ComponentName(context, MyMediaSessionListenerService::class.java)
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(cn.flattenToString())
    }

    // Prüfe die Berechtigung beim Start und wenn die App in den Vordergrund kommt
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                hasPermission = checkPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    // Initial check
    LaunchedEffect(Unit) {
        hasPermission = checkPermission()
    }


    if (hasPermission) {
        onPermissionGranted()
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Medienwiedergabe-Informationen", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Um Informationen über die aktuelle Medienwiedergabe (z.B. von Spotify) oder der Navigation anzuzeigen, " +
                        "benötigt open::Dashboard Zugriff auf deine Benachrichtigungen. " +
                        "Bitte aktiviere den Zugriff in den Systemeinstellungen.",
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = { requestNotificationListenerPermission(context) }) {
                Text("Zu den Einstellungen")
            }
        }
    }
}


@Composable
fun SettingsPanel(
    isEnergySavingEnabled: Boolean,
    isGpuEnabled: Boolean,
    isLaneDetectionEnabled: Boolean,
    isDebuggingEnabled: Boolean,
    onEnergySavingToggle: (Boolean) -> Unit,
    onGpuToggle: (Boolean) -> Unit,
    onLaneDetectionToggle: (Boolean) -> Unit,
    onDebugViewToggle: (Boolean) -> Unit,
    onDismissRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(), // Will be constrained by AnimatedVisibility
        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.92f)) // Even more opaque
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxHeight()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ){
                Text(
                    "Einstellungen",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = onDismissRequest, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Schließen", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }

            Divider(color = Color.Gray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            Column (verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsToggleRow("Energiesparmodus", isEnergySavingEnabled, onEnergySavingToggle)
                SettingsToggleRow("GPU Beschleunigung", isGpuEnabled, onGpuToggle)
                SettingsToggleRow("Spurerkennung", isLaneDetectionEnabled, onLaneDetectionToggle)
                SettingsToggleRow("Debugging Ansicht", isDebuggingEnabled, onDebugViewToggle)
            }

            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { onDismissRequest() }, // For now, just dismiss
                modifier = Modifier.fillMaxWidth()
            ){
                Text("Alle Einstellungen")
            }

            Text(
                "Änderungen werden sofort angewendet.",
                color = Color.LightGray,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun SettingsToggleRow(label: String, isChecked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            label,
            color = Color.White,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = isChecked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                uncheckedThumbColor = Color.LightGray,
                uncheckedTrackColor = Color.DarkGray.copy(alpha = 0.5f)
            ),
            modifier = Modifier.size(width = 52.dp, height = 28.dp)
        )
    }
}

@Composable
fun InfoAndControlsOverlay(
    inferenceTime: Long,
    speed: Float,
    laneDeviation: Double?,
    erkannteSchilderResourceIds: List<Int>,
    isDebuggingEnabled: Boolean,
    physicalOrientation: Int,
    onSettingsToggle: () -> Unit,
    onShowInfoScreenRequest: () -> Unit,
    isInfoScreenActive: Boolean,
    showSettingsButton: Boolean
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (isDebuggingEnabled && inferenceTime > 0) {
            val inferenceAlignment = if (physicalOrientation == Configuration.ORIENTATION_LANDSCAPE)
                Alignment.BottomStart else Alignment.TopStart // Adjust if it overlaps speed card too much

            Text(
                text = "$inferenceTime ms",
                modifier = Modifier
                    .align(inferenceAlignment)
                    .padding(
                        start = 16.dp,
                        top = if (physicalOrientation == Configuration.ORIENTATION_LANDSCAPE) 0.dp else 16.dp,
                        bottom = if (physicalOrientation == Configuration.ORIENTATION_LANDSCAPE) 16.dp else 0.dp
                    )
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                color = Color.White,
                fontSize = 12.sp
            )
        }

        val speedSignsAlignment = if (physicalOrientation == Configuration.ORIENTATION_LANDSCAPE || isInfoScreenActive)
            Alignment.TopStart else Alignment.TopCenter
        val speedSignsPadding = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp) // Added end padding for TopCenter


        Column(
            modifier = Modifier
                .align(speedSignsAlignment)
                .then(speedSignsPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val speedKmh = (speed * 3.6f).roundToInt()
                    Text(
                        text = "$speedKmh",
                        color = Color.White,
                        fontSize = if (physicalOrientation == Configuration.ORIENTATION_LANDSCAPE) 60.sp else 70.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = "km/h", color = Color.LightGray, fontSize = if (physicalOrientation == Configuration.ORIENTATION_LANDSCAPE) 14.sp else 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            SignBubblePlaceholder(resourceIds = erkannteSchilderResourceIds,
                modifier = Modifier.height(if (physicalOrientation == Configuration.ORIENTATION_LANDSCAPE) 60.dp else 70.dp))
        }

        AnimatedVisibility(showSettingsButton, enter = fadeIn(tween(300)), exit = fadeOut(tween(300)), modifier = Modifier.align(Alignment.TopEnd)) {
            Column(modifier = Modifier.align(Alignment.TopEnd)){
                IconButton(
                    modifier = Modifier
                        .padding(top = 16.dp, end = 16.dp)
                        .size(60.dp),
                    onClick = onSettingsToggle
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Einstellungen",
                        tint = Color.White,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .padding(12.dp)
                    )
                }
                IconButton(
                    modifier = Modifier
                        .padding(top = 16.dp, end = 16.dp)
                        .size(60.dp),
                    onClick = onSettingsToggle
                ) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.outline_screen_record_24),
                        contentDescription = "Dashcam starten",
                        tint = Color.White,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .padding(12.dp)
                    )
                }
            }
        }


        val infoIcon = if (isInfoScreenActive) Icons.Filled.Close else Icons.AutoMirrored.Filled.List
        IconButton(
            onClick = onShowInfoScreenRequest,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .size(64.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)

        ) {
            Icon(
                imageVector = infoIcon,
                contentDescription = if (isInfoScreenActive) "Info Bildschirm schließen" else "Info Bildschirm anzeigen",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        laneDeviation?.let { deviation ->
            val laneIndicatorBottomPadding = if (physicalOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                24.dp // Less padding if info button is far away
            } else {
                24.dp + 64.dp + 16.dp // Space for info button (size + padding) + base padding
            }
            LaneCenterIndicator(
                deviation = deviation,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = laneIndicatorBottomPadding)
                    .fillMaxWidth(if (physicalOrientation == Configuration.ORIENTATION_LANDSCAPE) 0.35f else 0.5f) // Narrower in landscape
                    .height(18.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(9.dp))
            )
        }
    }
}

@Composable
fun SignBubblePlaceholder(
    resourceIds: List<Int>,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    // Use a fixed default height or try to extract from modifier if a specific height is set
    val defaultHeight = 70.dp
    var actualHeight = defaultHeight

    // This is a basic way to check for a height modifier. More complex modifiers might not be caught.
    // It's often better to pass height explicitly if it needs to be dynamic and precise.
    modifier.foldIn(Unit) { _, element ->
        if (element is ModifierValueElement<*> && element.value is Dp) {
            // Attempt to find if a Modifier.height(Dp) or Modifier.size(Dp) was passed
            // This is very simplified and might not work for all cases (e.g. complex chained modifiers)
            // A more robust solution might involve specific height parameter or more advanced modifier introspection.
        }
    }
    if (modifier.toString().contains("height")) { // Very naive check
        // If a height modifier is present, we assume it dictates the actualHeight
        // This part needs a more robust way to get height from Modifier, or explicit height param
    }


    val iconSize = actualHeight * 0.7f
    val paddingHorizontal = 15.dp


    if (resourceIds.isEmpty()) {
        Box(
            modifier = modifier
                .height(actualHeight) // Apply the determined/default height
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(actualHeight / 2))
                .padding(
                    horizontal = paddingHorizontal,
                    vertical = (actualHeight - (iconSize * 0.8f)) / 2
                ),
            contentAlignment = Alignment.Center
        ){
            Icon(
                ImageVector.vectorResource(R.drawable.baseline_disabled_visible_24),
                contentDescription = "Keine Schilder erkannt",
                modifier = Modifier.size(iconSize * 0.8f),
                tint = Color.Gray
            )
        }
        return
    }

    Box(
        modifier = modifier
            .height(actualHeight) // Apply the determined/default height

            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(actualHeight / 2))
            .padding(vertical = (actualHeight - iconSize) / 2, horizontal = paddingHorizontal),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            resourceIds.take(3).forEach { resourceId ->
                val iconVector = ImageVector.vectorResource(id = resourceId)
                Icon(
                    imageVector = iconVector,
                    contentDescription = "Verkehrszeichen",
                    modifier = Modifier.size(iconSize),
                    tint = Color.Unspecified
                )
            }
        }
    }
}


@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SignBubblePreviewSingle() {
    SignBubblePlaceholder(resourceIds = listOf(R.drawable.speed_50))
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun SignBubblePreviewDouble() {
    SignBubblePlaceholder(resourceIds = listOf(R.drawable.speed_30, R.drawable.stop))
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun SignBubblePreviewTriple() {
    SignBubblePlaceholder(resourceIds = listOf(R.drawable.speed_30, R.drawable.stop, R.drawable.no_overtaking))
}


@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun SignBubblePreviewEmpty() {
    SignBubblePlaceholder(resourceIds = listOf())
}


@Composable
fun LaneCenterIndicator(
    deviation: Double,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Divider(
            color = Color.White.copy(alpha = 0.4f),
            thickness = 1.dp,
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .align(Alignment.Center)
        )
        val horizontalBias = deviation.coerceIn(-1.0, 1.0).toFloat()
        Box(
            modifier = Modifier
                .align(BiasAlignment(horizontalBias = horizontalBias, verticalBias = 0f))
                .width(8.dp)
                .fillMaxHeight(0.75f)
                .background(Color.Yellow, CircleShape)
        )
    }
}

@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
private fun bindCameraUseCases(
    context: Context,
    cameraProvider: ProcessCameraProvider,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    onBitmapAnalyzed: (Bitmap) -> Unit,
    executor: ExecutorService
) {

    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
        .build()

    val preview = Preview.Builder().build().also {
        it.setSurfaceProvider(previewView.surfaceProvider)
    }

    val imageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
        .build()

    imageAnalysis.setAnalyzer(executor) { imageProxy ->
        val bitmap = imageProxyToBitmap(imageProxy)
        if (bitmap != null) {
            onBitmapAnalyzed(bitmap)
        }
        imageProxy.close()
    }

    try {
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner, cameraSelector, preview, imageAnalysis
        )
        Log.d("bindCameraUseCases", "Camera use cases bound.")
    } catch (exc: Exception) {
        Log.e("bindCameraUseCases", "Use case binding failed", exc)
    }
}


// Helper for Modifier.value access (very simplified, for preview/concept)
// In a real app, Modifier introspection is complex. Prefer explicit parameters.
interface ModifierValueElement<T> : Modifier.Element {
    val value: T
}