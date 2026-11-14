package sv.edu.unicaes.hospital.data.local

import android.content.Context
import android.provider.OpenableColumns
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sv.edu.unicaes.hospital.domain.model.ArchivoAdjunto
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Archivos que el usuario elige con el selector del sistema (Photo Picker u OpenDocument). Los ViewModels
 * solo ven [ArchivoAdjunto]; el `content://` se abre al subirlo, sin copiarlo entero a memoria.
 */
interface ArchivosLocales {
    /** Nombre, tipo y tamaño del archivo; null si ya no se puede leer. */
    suspend fun describir(uri: String): ArchivoAdjunto?

    /** Flujo con el contenido; null si el archivo ya no está disponible. */
    fun abrir(uri: String): InputStream?
}

@Singleton
class ArchivosLocalesImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ArchivosLocales {
        override suspend fun describir(uri: String): ArchivoAdjunto? =
            withContext(Dispatchers.IO) {
                runCatching {
                    val contenido = uri.toUri()
                    val resolver = context.contentResolver
                    var nombre: String? = null
                    var tamano = -1L
                    resolver
                        .query(contenido, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                        ?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val iNombre = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                val iTamano = cursor.getColumnIndex(OpenableColumns.SIZE)
                                if (iNombre >= 0 && !cursor.isNull(iNombre)) nombre = cursor.getString(iNombre)
                                if (iTamano >= 0 && !cursor.isNull(iTamano)) tamano = cursor.getLong(iTamano)
                            }
                        }
                    ArchivoAdjunto(
                        uri = uri,
                        nombre = nombre ?: contenido.lastPathSegment.orEmpty().ifBlank { "archivo" },
                        mime = resolver.getType(contenido).orEmpty(),
                        tamanoBytes = tamano,
                    )
                }.getOrNull()
            }

        override fun abrir(uri: String): InputStream? =
            runCatching { context.contentResolver.openInputStream(uri.toUri()) }.getOrNull()
    }
