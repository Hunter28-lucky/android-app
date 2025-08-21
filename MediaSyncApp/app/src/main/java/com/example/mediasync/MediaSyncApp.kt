package com.example.mediasync

import android.app.Application
import com.google.firebase.FirebaseApp

class MediaSyncApp : Application() {
	override fun onCreate() {
		super.onCreate()
		FirebaseApp.initializeApp(this)
	}
}