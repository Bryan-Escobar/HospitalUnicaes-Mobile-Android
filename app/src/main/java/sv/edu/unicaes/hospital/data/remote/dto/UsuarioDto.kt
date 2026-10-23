package sv.edu.unicaes.hospital.data.remote.dto

import kotlinx.serialization.Serializable
import sv.edu.unicaes.hospital.domain.model.Rol
import sv.edu.unicaes.hospital.domain.model.Usuario

// Ajustar al contrato de GET /me cuando esté documentado en Swagger (HUNI-17).
@Serializable
data class UsuarioDto(
    val id: String,
    val email: String,
    val rol: String,
    val nombre: String? = null,
)

fun UsuarioDto.aDominio(): Usuario =
    Usuario(
        id = id,
        correo = email,
        nombre = nombre,
        rol = Rol.desdeApi(rol),
    )
