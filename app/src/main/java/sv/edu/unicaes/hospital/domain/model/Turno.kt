package sv.edu.unicaes.hospital.domain.model

/** Enum `tipo_turno` de la base. */
enum class TipoTurno(
    val valorApi: String,
) {
    MANANA("manana"),
    TARDE("tarde"),
    NOCHE("noche"),
    GUARDIA("guardia"),
    ;

    companion object {
        /** Uno desconocido se trata como de mañana. */
        fun desdeApi(valor: String): TipoTurno = entries.firstOrNull { it.valorApi == valor } ?: MANANA
    }
}

/**
 * Turno de cualquier empleado leído de la vista `v_turnos` (HUNI-51). [fecha] es el día en que empieza
 * (`2026-10-29`) y las horas van en hora del hospital (`07:00`); [inicio] y [fin] son ISO 8601 con zona,
 * y el turno de noche termina al día siguiente.
 */
data class Turno(
    val id: String,
    val personalId: String,
    val persona: String,
    val tipoPersonal: String,
    val esMedico: Boolean,
    val area: String,
    val tipoTurno: TipoTurno,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String,
    val inicio: String,
    val fin: String,
    val notas: String? = null,
)

/** Empleado activo al que recepción le puede asignar un turno. */
data class Empleado(
    val id: String,
    val nombre: String,
    val tipoPersonal: String,
)

/** Lo que recepción manda al crear o ajustar un turno; el trigger `validar_turno` rechaza los solapes. */
data class DatosTurno(
    val personalId: String,
    val area: String,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String,
    val tipoTurno: TipoTurno,
    val notas: String? = null,
)
