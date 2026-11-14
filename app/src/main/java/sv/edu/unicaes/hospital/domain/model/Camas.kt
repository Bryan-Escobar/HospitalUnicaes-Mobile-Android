package sv.edu.unicaes.hospital.domain.model

/** Estado de una cama (GET /habitaciones). Solo `DISPONIBLE` admite un ingreso o un traslado. */
enum class EstadoCama(
    val valorApi: String,
) {
    DISPONIBLE("disponible"),
    OCUPADA("ocupada"),
    LIMPIEZA("limpieza"),
    MANTENIMIENTO("mantenimiento"),
    ;

    companion object {
        /** Un valor desconocido se trata como no disponible: nunca se ofrece para asignar. */
        fun desdeApi(valor: String): EstadoCama = entries.firstOrNull { it.valorApi == valor } ?: MANTENIMIENTO
    }
}

/** Habitación con sus camas: 5 VIP en la 3.ª planta y 6 semiprivadas en la 2.ª (context.md §3.1). */
data class Habitacion(
    val id: String,
    /** `HAB-201`, `VIP-301`. */
    val codigo: String,
    val planta: Int,
    val tipo: TipoHabitacion,
    val camas: List<Cama>,
)

data class Cama(
    val id: String,
    /** `HAB-201-A`. */
    val codigo: String,
    /** Letra dentro de la habitación (`A`, `B`); vacía si la habitación tiene una sola cama. */
    val letra: String,
    val estado: EstadoCama,
    val ocupante: Ocupante? = null,
)

/** Paciente que ocupa la cama. Privado: solo lo ven recepción, administración y el tratante. */
data class Ocupante(
    val hospitalizacionId: String,
    val pacienteId: String,
    val nombre: String,
    /** `L. Martínez`: inicial del nombre y primer apellido, para el mapa de camas. */
    val nombreCorto: String,
    /** ISO 8601 con zona, como la API. */
    val fechaIngreso: String,
)
