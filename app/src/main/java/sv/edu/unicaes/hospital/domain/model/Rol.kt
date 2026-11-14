package sv.edu.unicaes.hospital.domain.model

/**
 * Valores del enum `rol_usuario` de la base (HUNI-95). Asistentes y enfermeros entran con el rol
 * RECEPCIONISTA: su tipo de personal es una etiqueta aparte que no da permisos.
 */
enum class Rol {
    ADMINISTRADOR,
    DOCTOR,
    RECEPCIONISTA,
    PACIENTE,
    DESCONOCIDO,
    ;

    companion object {
        /** Tolera mayúsculas/minúsculas y valores nuevos de la API sin romper la app. */
        fun desdeApi(valor: String?): Rol =
            when (valor?.trim()?.uppercase()) {
                "ADMINISTRADOR" -> ADMINISTRADOR
                "DOCTOR" -> DOCTOR
                "RECEPCIONISTA" -> RECEPCIONISTA
                "PACIENTE" -> PACIENTE
                else -> DESCONOCIDO
            }
    }
}
