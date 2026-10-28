package sv.edu.unicaes.hospital.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.exception.NoSessionFoundException
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

private val Context.sesionDataStore: DataStore<Preferences> by preferencesDataStore(name = "sesion")

/**
 * Guarda la sesión de Supabase (access y refresh token) en DataStore, para que
 * siga activa al cerrar y reabrir la app. `signOut` la borra con [deleteSession].
 */
class DataStoreSessionManager(
    context: Context,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : SessionManager {
    private val dataStore = context.applicationContext.sesionDataStore

    override suspend fun saveSession(session: UserSession) {
        dataStore.edit { it[CLAVE_SESION] = json.encodeToString(UserSession.serializer(), session) }
    }

    override suspend fun loadSession(): UserSession {
        val guardada = dataStore.data.first()[CLAVE_SESION] ?: throw NoSessionFoundException()
        return json.decodeFromString(UserSession.serializer(), guardada)
    }

    override suspend fun deleteSession() {
        dataStore.edit { it.remove(CLAVE_SESION) }
    }

    private companion object {
        val CLAVE_SESION = stringPreferencesKey("sesion_supabase")
    }
}
