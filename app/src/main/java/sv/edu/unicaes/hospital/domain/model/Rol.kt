package sv.edu.unicaes.hospital.domain.model

enum class Rol {
    ADMINISTRADOR,
    DOCTOR,
    RECEPCIONISTA,
    ASISTENTE,
    PACIENTE,
    DESCONOCIDO,
    ;

    /** Recepcionista, Asistente y Enfermero/a comparten la app de recepción. */
    val usaAppRecepcion: Boolean get() = this == RECEPCIONISTA || this == ASISTENTE

    companion object {
        /** Tolera mayúsculas/minúsculas y valores nuevos de la API sin romper la app. */
        fun desdeApi(valor: String?): Rol =
            when (valor?.trim()?.uppercase()) {
                "ADMINISTRADOR", "ADMIN" -> ADMINISTRADOR
                "DOCTOR" -> DOCTOR
                "RECEPCIONISTA", "RECEPCION" -> RECEPCIONISTA
                "ASISTENTE", "ENFERMERO" -> ASISTENTE
                "PACIENTE" -> PACIENTE
                else -> DESCONOCIDO
            }
    }
}
