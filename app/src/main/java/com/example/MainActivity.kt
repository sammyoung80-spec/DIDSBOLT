package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.ui.DidsAppContent
import com.example.ui.DidsBoltViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Set up universal uncaught exception tracing for robust diagnostics
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("CRITICAL_CRASH", "FATAL EXCEPTION ON THREAD: ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        super.onCreate(savedInstanceState)

        // Enable edge-to-edge safety drawing (Safe Drawing/Safe Insets)
        enableEdgeToEdge()

        // Instantiate Room Database context locally
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "didsbolt_sqlite_db"
        )
            .fallbackToDestructiveMigration()
            .build()
        
        val appDao = db.appDao()
        val repository = AppRepository(appDao, MyApplication.supabaseClient)
        val viewModelFactory = ViewModelFactory(repository)

        // Retrieve ViewModel instance bound to MainActivity lifecycles
        val viewModel = ViewModelProvider(this, viewModelFactory)[DidsBoltViewModel::class.java]

        setContent {
            MyApplicationTheme {
                DidsAppContent(viewModel = viewModel)
            }
        }
    }
}

// Custom ViewModel Factory supporting Room database repositories injection
class ViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DidsBoltViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DidsBoltViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
