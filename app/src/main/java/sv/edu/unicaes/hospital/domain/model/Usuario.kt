package sv.edu.unicaes.hospital.domain.model

data class Usuario(
    val id: String,
    val correo: String,
    val nombre: String?,
    val rol: Rol,
)
