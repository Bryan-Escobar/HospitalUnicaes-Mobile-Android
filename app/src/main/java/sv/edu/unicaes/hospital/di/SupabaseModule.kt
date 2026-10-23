package sv.edu.unicaes.hospital.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.realtime.Realtime
import io.ktor.client.engine.okhttp.OkHttp
import sv.edu.unicaes.hospital.BuildConfig
import sv.edu.unicaes.hospital.data.remote.TokenProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        require(BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_KEY.isNotBlank()) {
            "Faltan <variante>.supabaseUrl / <variante>.supabaseKey en local.properties (ver local.properties.example)"
        }
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY,
        ) {
            // Ktor sobre OkHttp: una sola pila de red con Retrofit.
            httpEngine = OkHttp.create()
            install(Auth)
            install(Realtime)
        }
    }

    @Provides
    @Singleton
    fun provideTokenProvider(supabase: SupabaseClient): TokenProvider =
        TokenProvider { supabase.auth.currentAccessTokenOrNull() }
}
