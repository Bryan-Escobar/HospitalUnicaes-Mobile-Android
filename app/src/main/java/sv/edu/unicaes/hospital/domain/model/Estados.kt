package sv.edu.unicaes.hospital.domain.model

/** Pendiente → En Proceso → Completado, solo hacia adelante (context.md §3.3). */
enum class EstadoLaboratorio {
    PENDIENTE,
    EN_PROCESO,
    COMPLETADO,
    ;

    /** Siguiente estado permitido, o null si la orden ya está completada. */
    val siguiente: EstadoLaboratorio?
        get() = entries.getOrNull(ordinal + 1)
}

/** Estados de la única ambulancia del hospital (context.md §3.5). */
enum class EstadoAmbulancia {
    DISPONIBLE,
    EN_SERVICIO,
    EN_MANTENIMIENTO,
    ;

    /** Solo se despacha si está Disponible. */
    val puedeDespachar: Boolean get() = this == DISPONIBLE
}

enum class EstadoCita {
    PROGRAMADA,
    ATENDIDA,
    CANCELADA,
}

/** VIP: 3.ª planta, 1 cama. Semiprivada: 2.ª planta, 2 camas (context.md §3.1). */
enum class TipoHabitacion(
    val planta: Int,
    val camasPorHabitacion: Int,
) {
    VIP(planta = 3, camasPorHabitacion = 1),
    SEMIPRIVADA(planta = 2, camasPorHabitacion = 2),
}
