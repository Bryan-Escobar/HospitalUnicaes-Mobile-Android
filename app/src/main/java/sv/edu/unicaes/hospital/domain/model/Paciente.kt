package sv.edu.unicaes.hospital.domain.model

/** Paciente de GET /pacientes. Sus datos personales son privados (context.md §3.4). */
data class Paciente(
    val id: String,
    val nombres: String,
    val apellidos: String,
    val documento: String? = null,
    /** AAAA-MM-DD, como la API. */
    val fechaNacimiento: String? = null,
    val sexo: Sexo? = null,
    val telefono: String? = null,
    val direccion: String? = null,
    val contactoEmergencia: String? = null,
    val telefonoEmergencia: String? = null,
    val numeroExpediente: String = "",
    val activo: Boolean = true,
) {
    val nombreCompleto: String get() = "$nombres $apellidos".trim()
}

/** Datos que recepción puede enviar al crear o editar (POST y PATCH /pacientes). */
data class DatosPaciente(
    val nombres: String? = null,
    val apellidos: String? = null,
    val documento: String? = null,
    val fechaNacimiento: String? = null,
    val sexo: Sexo? = null,
    val telefono: String? = null,
    val direccion: String? = null,
    val contactoEmergencia: String? = null,
    val telefonoEmergencia: String? = null,
)

/** Página de un listado paginado de la API. */
data class Pagina<T>(
    val items: List<T>,
    val pagina: Int,
    val totalPaginas: Int,
) {
    val hayMas: Boolean get() = pagina < totalPaginas
}

enum class Sexo(
    val valorApi: String,
) {
    MASCULINO("masculino"),
    FEMENINO("femenino"),
    OTRO("otro"),
    ;

    companion object {
        fun desdeApi(valor: String?): Sexo? = entries.firstOrNull { it.valorApi == valor }
    }
}
