package sv.edu.unicaes.hospital.domain.model

/** Duración de las citas que agenda recepción; `crear_cita` usa la misma por defecto. */
const val DURACION_CITA_MINUTOS = 30

/** Cita leída de la vista `v_citas` de Supabase (HUNI-51). */
data class Cita(
    val id: String,
    /** Fecha y hora ISO 8601 con zona. */
    val fechaHora: String,
    val doctor: String,
    val motivo: String?,
    val estado: EstadoCita,
    val pacienteId: String = "",
    val paciente: String = "",
    val doctorId: String = "",
    val especialidad: String? = null,
    val duracionMinutos: Int = DURACION_CITA_MINUTOS,
)

/** Turno de un médico de la vista `v_turnos`; [inicio] y [fin] son ISO 8601 con zona (el de noche cruza el día). */
data class TurnoMedico(
    val doctorId: String,
    val doctor: String,
    val especialidad: String?,
    val inicio: String,
    val fin: String,
)

/** Médico de turno y sin otra cita en el horario pedido (RPC `doctores_disponibles`). */
data class DoctorDisponible(
    val id: String,
    val nombre: String,
    val especialidad: String?,
)
