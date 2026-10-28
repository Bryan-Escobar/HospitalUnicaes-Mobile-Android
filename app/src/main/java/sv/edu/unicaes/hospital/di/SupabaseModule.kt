package sv.edu.unicaes.hospital.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.ktor.client.engine.okhttp.OkHttp
import sv.edu.unicaes.hospital.BuildConfig
import sv.edu.unicaes.hospital.data.local.DataStoreSessionManager
import sv.edu.unicaes.hospital.data.remote.TokenProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {
    @Provides
    @Singleton
    fun provideSupabaseClient(
        @ApplicationContext context: Context,
    ): SupabaseClient {
        require(BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_KEY.isNotBlank()) {
            "Faltan <variante>.supabaseUrl / <variante>.supabaseKey en local.properties (ver local.properties.example)"
        }
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY,
        ) {
            // Ktor sobre OkHttp: una sola pila de red con Retrofit.
            httpEngine = OkHttp.create()
            install(Auth) {
                // La sesión persiste al cerrar la app y se restaura sola al abrirla.
                sessionManager = DataStoreSessionManager(context)
            }
            install(Realtime)
            // Citas y turnos se leen directo de Supabase con RLS (HUNI-51).
            install(Postgrest)
        }
    }

    @Provides
    @Singleton
    fun provideTokenProvider(supabase: SupabaseClient): TokenProvider =
        TokenProvider { supabase.auth.currentAccessTokenOrNull() }
}
