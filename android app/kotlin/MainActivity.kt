import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.text

class MainActivity : androidx.appcompat.app.AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var takePictureButton: androidx.compose.material3.Button
    private lateinit var startRecordButton: androidx.compose.material3.Button
    private lateinit var stopRecordButton: androidx.compose.material3.Button
    private lateinit var statusTextView: TextView

    // ... (permission launchers, image capture launchers, mediarecorder variables from above)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Define this layout

        imageView = findViewById(R.id.imageViewPreview)
        takePictureButton = findViewById(R.id.buttonTakePicture)
        startRecordButton = findViewById(R.id.buttonStartRecording)
        stopRecordButton = findViewById(R.id.buttonStopRecording)
        statusTextView = findViewById(R.id.textViewStatus)

        checkAndRequestPermissions() // Request permissions on start

        takePictureButton.setOnClickListener {
            captureImage()
        }

        startRecordButton.setOnClickListener {
            startRecordingAudio() // Renamed for clarity from startRecording()
            statusTextView.text = "Recording..."
            startRecordButton.isEnabled = false
            stopRecordButton.isEnabled = true
        }

        stopRecordButton.setOnClickListener {
            stopRecordingAudio() // Renamed for clarity from stopRecording()
            statusTextView.text = "Recording stopped. File: ${audioFile?.name}"
            startRecordButton.isEnabled = true
            stopRecordButton.isEnabled = false
        }
        stopRecordButton.isEnabled = false // Initially disabled
    }

    // ... (Implement checkAndRequestPermissions, captureImage, startRecordingAudio, stopRecordingAudio)

    // Example of displaying the image (after captureImage() succeeds and you have latestTmpUri)
    private fun displayImage(uri: Uri) {
        imageView.setImageURI(uri)
        // If you want to save it permanently now:
        // saveImagePermanently(uri, this)
    }

    // In takeImageResultLauncher's success block:
    // if (isSuccess) {
    //    latestTmpUri?.let { uri ->
    //        displayImage(uri)
    //        Log.d("ImageCapture", "Image saved to: $uri")
    //    }
    // }

    override fun onStop() {
        super.onStop()
        if (isRecording) { // isRecording is the boolean for audio recording
            stopRecordingAudio()
        }
    }
}d