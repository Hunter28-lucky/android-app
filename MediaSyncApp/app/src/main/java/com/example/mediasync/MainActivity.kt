package com.example.mediasync

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mediasync.firebase.FirebaseService
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
	@OptIn(ExperimentalPermissionsApi::class)
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContent {
			val navController = rememberNavController()
			MediaSyncAppScaffold(navController)
		}
	}
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MediaSyncAppScaffold(navController: NavHostController) {
	val permissions = rememberRequiredPermissions()
	Scaffold(
		topBar = { TopAppBar(title = { Text("MediaSync") }) },
		bottomBar = {
			NavigationBar {
				NavigationBarItem(
					selected = currentRoute(navController) == "camera",
					onClick = { navController.navigate("camera") },
					label = { Text("Camera") },
					icon = { Icon(Icons.Filled.CameraAlt, contentDescription = null) }
				)
				NavigationBarItem(
					selected = currentRoute(navController) == "gallery",
					onClick = { navController.navigate("gallery") },
					label = { Text("Gallery") },
					icon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) }
				)
				NavigationBarItem(
					selected = currentRoute(navController) == "contacts",
					onClick = { navController.navigate("contacts") },
					label = { Text("Contacts") },
					icon = { Icon(Icons.Filled.Contacts, contentDescription = null) }
				)
			}
		}
	) { padding ->
		PermissionsGate(permissions) {
			NavHost(
				navController = navController,
				startDestination = "camera",
				modifier = Modifier.padding(padding)
			) {
				composable("camera") { CameraScreen() }
				composable("gallery") { GalleryScreen() }
				composable("contacts") { ContactsScreen() }
			}
		}
	}
}

@Composable
fun currentRoute(navController: NavHostController): String? {
	val backStackEntry by navController.currentBackStackEntryAsState()
	return backStackEntry?.destination?.route
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberRequiredPermissions(): MultiplePermissionsState {
	val permissions = buildList {
		add(Manifest.permission.CAMERA)
		add(Manifest.permission.RECORD_AUDIO)
		if (Build.VERSION.SDK_INT <= 32) {
			add(Manifest.permission.READ_EXTERNAL_STORAGE)
		} else {
			add(Manifest.permission.READ_MEDIA_IMAGES)
			add(Manifest.permission.READ_MEDIA_VIDEO)
		}
		add(Manifest.permission.READ_CONTACTS)
	}
	return rememberMultiplePermissionsState(permissions)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsGate(
	multiplePermissionsState: MultiplePermissionsState,
	content: @Composable () -> Unit
) {
	LaunchedEffect(Unit) { multiplePermissionsState.launchMultiplePermissionRequest() }
	val allGranted = multiplePermissionsState.permissions.all { it.status.isGranted }
	if (allGranted) content() else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Permissions required to continue") }
}

@Composable
fun CameraScreen() {
	val context = LocalContext.current
	var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
	var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
	var activeRecording by remember { mutableStateOf<Recording?>(null) }
	var lastCapturedFile by remember { mutableStateOf<File?>(null) }
	var snackbarHostState = remember { SnackbarHostState() }

	Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
		Column(Modifier.fillMaxSize().padding(padding)) {
			AndroidView(
				factory = { ctx ->
					val previewView = androidx.camera.view.PreviewView(ctx)
					val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
					cameraProviderFuture.addListener({
						val cameraProvider = cameraProviderFuture.get()
						val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
						val selector = CameraSelector.DEFAULT_BACK_CAMERA
						val imageCaptureLocal = ImageCapture.Builder().build()
						val recorder = Recorder.Builder()
							.setQualitySelector(
								QualitySelector.from(
									Quality.FHD,
									FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
								)
							)
							.build()
						val videoCaptureLocal = VideoCapture.withOutput(recorder)
						cameraProvider.unbindAll()
						cameraProvider.bindToLifecycle(
							context as ComponentActivity,
							selector,
							preview,
							imageCaptureLocal,
							videoCaptureLocal
						)
						imageCapture = imageCaptureLocal
						videoCapture = videoCaptureLocal
					}, ContextCompat.getMainExecutor(ctx))
					previewView
				},
				modifier = Modifier.weight(1f)
			)
			Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
				Button(onClick = {
					val photoFile = File(
						context.externalMediaDirs.firstOrNull(),
						"IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}.jpg"
					)
					val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
					imageCapture?.takePicture(
						outputOptions,
						ContextCompat.getMainExecutor(context),
						object : ImageCapture.OnImageSavedCallback {
							override fun onError(exception: ImageCaptureException) {
								lastCapturedFile = null
							}
							override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
								lastCapturedFile = photoFile
							}
						}
					)
				}) { Text("Take Photo") }
				Button(onClick = {
					val recording = activeRecording
					if (recording == null) {
						val videoFile = File(
							context.externalMediaDirs.firstOrNull(),
							"VID_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}.mp4"
						)
						val outputOptions = FileOutputOptions.Builder(videoFile).build()
						activeRecording = videoCapture?.output?.prepareRecording(context, outputOptions)?.start(ContextCompat.getMainExecutor(context)) { event ->
							when (event) {
								is VideoRecordEvent.Finalize -> {
									lastCapturedFile = videoFile
									activeRecording = null
								}
								else -> {}
							}
						}
					} else {
						recording.stop()
						activeRecording = null
					}
				}) { Text(if (activeRecording == null) "Start Video" else "Stop Video") }
				Button(enabled = lastCapturedFile != null, onClick = {
					lastCapturedFile?.let { file ->
						val type = if (file.extension.equals("mp4", true)) "video/mp4" else "image/jpeg"
						FirebaseService.uploadMediaFile(file, type) { ok, err ->
							if (!ok) {
								// ignore snackbar errors for brevity in sample
							} else {
								// uploaded
							}
						}
					}
				}) { Text("Upload Last") }
			}
		}
	}
}

@Composable
fun GalleryScreen() {
	val context = LocalContext.current
	var mediaFiles by remember { mutableStateOf(listOf<File>()) }
	LaunchedEffect(Unit) {
		val dir = context.externalMediaDirs.firstOrNull()
		mediaFiles = dir?.listFiles()?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()
	}
	Column(Modifier.fillMaxSize()) {
		Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
			Button(enabled = mediaFiles.isNotEmpty(), onClick = {
				mediaFiles.forEach { file ->
					val type = if (file.extension.equals("mp4", true)) "video/mp4" else "image/jpeg"
					FirebaseService.uploadMediaFile(file, type) { _, _ -> }
				}
			}) { Text("Upload All") }
		}
		LazyColumn(Modifier.fillMaxSize()) {
			items(mediaFiles) { file ->
				ListItem(
					headlineContent = { Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
					trailingContent = {
						TextButton(onClick = {
							val type = if (file.extension.equals("mp4", true)) "video/mp4" else "image/jpeg"
							FirebaseService.uploadMediaFile(file, type) { _, _ -> }
						}) { Text("Upload") }
					}
				)
				Divider()
			}
		}
	}
}

@Composable
fun ContactsScreen() {
	val context = LocalContext.current
	var contacts by remember { mutableStateOf(listOf<String>()) }
	LaunchedEffect(Unit) { contacts = loadContacts(context) }
	Column(Modifier.fillMaxSize()) {
		Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
			Button(enabled = contacts.isNotEmpty(), onClick = {
				FirebaseService.uploadContactNames(contacts) { _, _ -> }
			}) { Text("Upload Contacts") }
		}
		LazyColumn(Modifier.fillMaxSize()) {
			items(contacts) { name ->
				ListItem(headlineContent = { Text(name) })
				Divider()
			}
		}
	}
}

fun loadContacts(context: android.content.Context): List<String> {
	val list = mutableListOf<String>()
	val projection = arrayOf(android.provider.ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
	context.contentResolver.query(
		android.provider.ContactsContract.Contacts.CONTENT_URI,
		projection,
		null,
		null,
		android.provider.ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " ASC"
	)?.use { cursor ->
		val nameIdx = cursor.getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
		while (cursor.moveToNext()) if (nameIdx >= 0) list.add(cursor.getString(nameIdx))
	}
	return list
}