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
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize the Supabase Client using values injected from .env via BuildConfig
        supabaseClient = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Postgrest) // Enables database Select/Insert/Update/Delete operations
            install(Realtime)  // Enables live listening to table change events
            install(Auth)      // Enables user Auth/Login
        }
    }
}
