package sv.edu.unicaes.hospital.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import sv.edu.unicaes.hospital.domain.model.Rol
import sv.edu.unicaes.hospital.domain.model.Usuario

/** Envoltura de toda respuesta exitosa de la API: `{ statusCode, success, message, data }`. */
@Serializable
data class ApiRespuesta<T>(
    val data: T,
)

/** Esquema `Me` de GET /auth/me (ver /docs de la API). */
@Serializable
data class UsuarioDto(
    val id: String,
    val email: String? = null,
    val nombres: String,
    val apellidos: String,
    val telefono: String? = null,
    val rol: String,
    val personal: PersonalResumenDto? = null,
)

@Serializable
data class PersonalResumenDto(
    val id: String,
    @SerialName("codigo_empleado") val codigoEmpleado: String,
    val tipo: TipoPersonalDto,
    val especialidad: String? = null,
)

@Serializable
data class TipoPersonalDto(
    val nombre: String,
)

fun UsuarioDto.aDominio(): Usuario =
    Usuario(
        id = id,
        correo = email,
        nombre = "$nombres $apellidos".trim(),
        rol = Rol.desdeApi(rol),
        telefono = telefono,
        tipoPersonal = personal?.tipo?.nombre,
        especialidad = personal?.especialidad,
    )
