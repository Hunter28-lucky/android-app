plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.android")
	id("com.google.gms.google-services")
}

android {
	namespace = "com.example.mediasync"
	compileSdk = 35

	defaultConfig {
		applicationId = "com.example.mediasync"
		minSdk = 24
		targetSdk = 35
		versionCode = 1
		versionName = "1.0"

		vectorDrawables.useSupportLibrary = true
	}

	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}

	kotlinOptions {
		jvmTarget = "17"
		freeCompilerArgs += listOf(
			"-Xjvm-default=all"
		)
	}

	buildFeatures {
		compose = true
	}

	composeOptions {
		kotlinCompilerExtensionVersion = "1.5.14"
	}

	packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}

dependencies {
	// Compose BOM
	implementation(platform("androidx.compose:compose-bom:2024.06.00"))
	implementation("androidx.compose.ui:ui")
	implementation("androidx.compose.material3:material3")
	implementation("androidx.compose.ui:ui-tooling-preview")
	debugImplementation("androidx.compose.ui:ui-tooling")
	implementation("androidx.activity:activity-compose:1.9.2")
	implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
	implementation("androidx.navigation:navigation-compose:2.8.3")
	implementation("androidx.compose.material:material-icons-extended")

	// CameraX
	val cameraxVersion = "1.3.4"
	implementation("androidx.camera:camera-core:$cameraxVersion")
	implementation("androidx.camera:camera-camera2:$cameraxVersion")
	implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
	implementation("androidx.camera:camera-view:$cameraxVersion")
	implementation("androidx.camera:camera-video:$cameraxVersion")

	// Media3 for video playback (optional viewer)
	implementation("androidx.media3:media3-exoplayer:1.4.1")
	implementation("androidx.media3:media3-ui:1.4.1")

	// Coil for image/video thumbnails
	implementation("io.coil-kt:coil-compose:2.6.0")
	implementation("io.coil-kt:coil-video:2.6.0")

	// Firebase
	implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
	implementation("com.google.firebase:firebase-storage-ktx")
	implementation("com.google.firebase:firebase-firestore-ktx")
	implementation("com.google.firebase:firebase-auth-ktx")

	// WorkManager for background uploads (optional future)
	implementation("androidx.work:work-runtime-ktx:2.9.1")

	// Accompanist permissions (simplify runtime permissions)
	implementation("com.google.accompanist:accompanist-permissions:0.34.0")
}