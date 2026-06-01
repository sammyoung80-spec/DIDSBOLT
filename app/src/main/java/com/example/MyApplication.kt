package com.example

import android.app.Application
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

class MyApplication : Application() {
    companion object {
        var supabaseClient: SupabaseClient? = null
            private set
        var isSupabaseEnabled: Boolean = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        
        val url = BuildConfig.SUPABASE_URL
        val key = BuildConfig.SUPABASE_ANON_KEY
        
        val isUrlValid = url.isNotBlank() && url != "YOUR_SUPABASE_URL" && url.startsWith("http")
        val isKeyValid = key.isNotBlank() && key != "YOUR_SUPABASE_ANON_KEY"
        
        isSupabaseEnabled = isUrlValid && isKeyValid
        
        if (isSupabaseEnabled) {
            try {
                supabaseClient = createSupabaseClient(
                    supabaseUrl = url,
                    supabaseKey = key
                ) {
                    install(Postgrest) // Enables database Select/Insert/Update/Delete operations
                    install(Realtime)  // Enables live listening to table change events
                    install(Auth)      // Enables user Auth/Login
                }
            } catch (e: Exception) {
                android.util.Log.e("MyApplication", "Failed to initialize SupabaseClient with URL $url", e)
                isSupabaseEnabled = false
                supabaseClient = null
            }
        } else {
            android.util.Log.i("MyApplication", "Supabase integration not enabled. Using local SQLite DB instead.")
        }
    }
}
