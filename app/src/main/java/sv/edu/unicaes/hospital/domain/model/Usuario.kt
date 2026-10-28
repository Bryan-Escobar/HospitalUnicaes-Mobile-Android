package sv.edu.unicaes.hospital.domain.model

data class Usuario(
    val id: String,
    val correo: String?,
    val nombre: String,
    val rol: Rol,
    val telefono: String? = null,
    /** Tipo de personal del catálogo (Doctor, Recepcionista, Enfermero/a…). */
    val tipoPersonal: String? = null,
    val especialidad: String? = null,
)
