package com.example

import android.app.Application
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

class MyApplication : Application() {
    companion object {
        lateinit var supabaseClient: SupabaseClient
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
        
        val finalUrl = if (isUrlValid) url else "https://placeholder-project.supabase.co"
        val finalKey = if (isKeyValid) key else "placeholder"
        
        try {
            supabaseClient = createSupabaseClient(
                supabaseUrl = finalUrl,
                supabaseKey = finalKey
            ) {
                install(Postgrest) // Enables database Select/Insert/Update/Delete operations
                install(Realtime)  // Enables live listening to table change events
                install(Auth)      // Enables user Auth/Login
            }
        } catch (e: Exception) {
            android.util.Log.e("MyApplication", "Failed to initialize SupabaseClient with URL $finalUrl", e)
            try {
                // Seed a Dummy client to avoid UninitializedPropertyAccessException
                supabaseClient = createSupabaseClient(
                    supabaseUrl = "https://placeholder-project.supabase.co",
                    supabaseKey = "placeholder"
                ) {
                    install(Postgrest)
                    install(Realtime)
                    install(Auth)
                }
            } catch (ex: Exception) {
                android.util.Log.e("MyApplication", "Failed to create fallback SupabaseClient", ex)
            }
        }
    }
}
