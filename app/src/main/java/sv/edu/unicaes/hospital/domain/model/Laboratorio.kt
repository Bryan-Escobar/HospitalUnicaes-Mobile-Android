package sv.edu.unicaes.hospital.domain.model

import java.text.Normalizer

/** Prioridad de una orden de laboratorio (`prioridad` de la API). */
enum class PrioridadLaboratorio(
    val valorApi: String,
) {
    NORMAL("normal"),
    URGENTE("urgente"),
    CRITICA("critica"),
    ;

    companion object {
        fun desdeApi(valor: String): PrioridadLaboratorio = entries.firstOrNull { it.valorApi == valor } ?: NORMAL
    }
}

/** Tipo del archivo adjunto a un resultado (HUNI-63: PDF o imagen). */
enum class TipoArchivoResultado { PDF, IMAGEN }

/**
 * Orden de laboratorio (GET /laboratorio/ordenes). Su [estado] lo deriva la API de sus exámenes:
 * completado cuando todos lo están, en proceso cuando alguno avanzó.
 */
data class OrdenLaboratorio(
    val id: String,
    val fechaSolicitud: String,
    val prioridad: PrioridadLaboratorio,
    val estado: EstadoLaboratorio,
    val pacienteId: String,
    val paciente: String,
    val doctor: String,
    val especialidad: String? = null,
    val indicaciones: String? = null,
    val examenes: List<ExamenOrden> = emptyList(),
)

/** Un examen de la orden; cada uno sigue Pendiente → En Proceso → Completado por su cuenta. */
data class ExamenOrden(
    val id: String,
    val nombre: String,
    val tipoMuestra: String? = null,
    val estado: EstadoLaboratorio,
    val fechaInicio: String? = null,
    val fechaCompletado: String? = null,
    val resultado: ResultadoExamen? = null,
)

data class ResultadoExamen(
    val valor: String? = null,
    val unidad: String? = null,
    val rangoReferencia: String? = null,
    val observaciones: String? = null,
    /** Null si el resultado no trae archivo. */
    val tipoArchivo: TipoArchivoResultado? = null,
    val validadoPor: String? = null,
    val fecha: String,
)

/**
 * Unidades que se pueden adjuntar a un resultado (HUNI-100). El formulario filtra esta lista;
 * lo que se envía tiene que ser una entrada, nunca un texto suelto. La API la deja opcional.
 */
object UnidadesLaboratorio {
    val catalogo: List<String> =
        listOf(
            "%",
            "g/dL",
            "g/L",
            "mg/dL",
            "mg/L",
            "µg/dL",
            "µg/mL",
            "ng/mL",
            "mmol/L",
            "µmol/L",
            "mEq/L",
            "UI/L",
            "mUI/L",
            "U/L",
            "células/µL",
            "10^3/µL",
            "10^6/µL",
            "fL",
            "pg",
            "mm/h",
            "mL",
            "mL/min",
        )

    /** Coincidencias de [consulta]: primero las que empiezan igual, luego las que la contienen. */
    fun coincidencias(consulta: String): List<String> {
        val busqueda = normalizar(consulta)
        if (busqueda.isEmpty()) return catalogo
        return catalogo
            .mapNotNull { unidad ->
                val normal = normalizar(unidad)
                when {
                    normal.startsWith(busqueda) -> 0 to unidad
                    normal.contains(busqueda) -> 1 to unidad
                    else -> null
                }
            }.sortedBy { it.first }
            .map { it.second }
    }

    /** La unidad del catálogo que escribe [texto], o null si no es una entrada. */
    fun canonica(texto: String): String? {
        val busqueda = normalizar(texto)
        if (busqueda.isEmpty()) return null
        return catalogo.firstOrNull { normalizar(it) == busqueda }
    }

    private fun normalizar(texto: String): String =
        Normalizer
            .normalize(texto, Normalizer.Form.NFD)
            .replace(MARCAS, "")
            .lowercase()
            .replace('µ', 'u')
            .replace('μ', 'u')
            .filterNot { it.isWhitespace() }

    private val MARCAS = "\\p{Mn}+".toRegex()
}

/** Lo que Recepción escribe al registrar el resultado. La API exige valor o archivo. */
data class DatosResultado(
    val valor: String? = null,
    val unidad: String? = null,
    val rangoReferencia: String? = null,
    val observaciones: String? = null,
)

/**
 * Archivo elegido con el selector del sistema. [uri] es un `content://` que el repositorio lee al subir;
 * [mime] y [tamanoBytes] vienen del ContentResolver.
 */
data class ArchivoAdjunto(
    val uri: String,
    val nombre: String,
    val mime: String,
    val tamanoBytes: Long,
) {
    val esPdf: Boolean get() = mime == MIME_PDF

    companion object {
        const val MIME_PDF = "application/pdf"

        /** Tipos que acepta la API (HUNI-63). */
        val MIME_PERMITIDOS = setOf(MIME_PDF, "image/jpeg", "image/png", "image/webp")

        /** Límite de la API y del bucket de Storage. */
        const val MAXIMO_BYTES = 10L * 1024 * 1024
    }
}
