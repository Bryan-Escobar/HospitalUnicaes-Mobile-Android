package sv.edu.unicaes.hospital.domain.model

/** Hospitalización de un paciente (GET /hospitalizaciones). Privada: la cama no es información pública. */
data class Hospitalizacion(
    val id: String,
    /** Código de la habitación: `HAB-201`, `VIP-301`. */
    val habitacion: String,
    /** Letra de la cama dentro de la habitación: `A`, `B`. */
    val cama: String,
    val tipo: TipoHabitacion,
    val planta: Int,
    /** Fecha y hora ISO 8601 con zona, como la API. */
    val fechaIngreso: String,
    val medicoTratante: String,
)

/** Cita de un paciente leída de la vista `v_citas` de Supabase (HUNI-51). */
data class Cita(
    val id: String,
    /** Fecha y hora ISO 8601 con zona. */
    val fechaHora: String,
    val doctor: String,
    val motivo: String?,
    val estado: EstadoCita,
)
