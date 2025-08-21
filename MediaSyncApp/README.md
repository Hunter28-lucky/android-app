# MediaSync

Android app using Compose + CameraX to capture photos and videos, browse gallery, and sync media and contacts to Firebase.

## Features
- Capture photos/videos (CameraX)
- Browse/play media (Compose, MediaStore, Media3)
- Upload to Firebase Storage with metadata in Firestore
- Upload device contacts to Firestore
- Runtime permissions for Camera, Storage/Media, Contacts

## Firebase Setup
1. Create a Firebase project.
2. Add an Android app with package `com.example.mediasync`.
3. Download `google-services.json` and place it at `app/google-services.json`.
4. Enable Authentication (Anonymous) if desired.
5. Enable Cloud Firestore and Firebase Storage.
6. Optionally configure Storage/Firestore rules for development.

## Build & Run
```bash
./gradlew :app:assembleDebug
```

Open in Android Studio to run on a device with Android 7.0+.