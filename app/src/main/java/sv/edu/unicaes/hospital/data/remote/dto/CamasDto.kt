package sv.edu.unicaes.hospital.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import sv.edu.unicaes.hospital.domain.model.Cama
import sv.edu.unicaes.hospital.domain.model.DoctorDisponible
import sv.edu.unicaes.hospital.domain.model.EstadoCama
import sv.edu.unicaes.hospital.domain.model.Habitacion
import sv.edu.unicaes.hospital.domain.model.Ocupante
import sv.edu.unicaes.hospital.domain.model.TipoHabitacion

/** Esquema `Habitacion` de GET /habitaciones (HUNI-66, ver /docs de la API). */
@Serializable
data class HabitacionMapaDto(
    val id: String,
    val codigo: String,
    val planta: Int,
    /** `vip` o `semiprivada`. */
    val tipo: String,
    val camas: List<CamaMapaDto>,
)

@Serializable
data class CamaMapaDto(
    val id: String,
    /** `HAB-201-A`. */
    val codigo: String,
    /** `disponible`, `ocupada`, `limpieza` o `mantenimiento`. */
    val estado: String,
    val ocupante: OcupanteDto? = null,
)

@Serializable
data class OcupanteDto(
    @SerialName("hospitalizacion_id") val hospitalizacionId: String,
    val paciente: PacienteResumenDto,
    @SerialName("fecha_ingreso") val fechaIngreso: String,
)

@Serializable
data class PacienteResumenDto(
    val id: String,
    val nombres: String,
    val apellidos: String,
)

/** Body de POST /hospitalizaciones. Sin `cama_id`, la API asigna la sugerida para `tipo_habitacion`. */
@Serializable
data class CrearHospitalizacionBody(
    @SerialName("paciente_id") val pacienteId: String,
    @SerialName("doctor_tratante_id") val doctorTratanteId: String,
    @SerialName("cama_id") val camaId: String? = null,
    @SerialName("motivo_ingreso") val motivoIngreso: String? = null,
)

@Serializable
data class TrasladoBody(
    @SerialName("cama_id") val camaId: String,
)

@Serializable
data class AltaBody(
    val notas: String? = null,
)

@Serializable
data class CambiarEstadoCamaBody(
    val estado: String,
)

/** Esquema `Personal` de GET /personal, solo lo necesario para elegir al médico tratante. */
@Serializable
data class PersonalDto(
    val id: String,
    val nombres: String,
    val apellidos: String,
    /** Rol de su cuenta (`doctor`, `recepcionista`…); null si no tiene cuenta. */
    val rol: String? = null,
    val especialidad: EspecialidadDto? = null,
    val activo: Boolean = true,
)

@Serializable
data class EspecialidadDto(
    val nombre: String,
)

fun HabitacionMapaDto.aDominio(): Habitacion =
    Habitacion(
        id = id,
        codigo = codigo,
        planta = planta,
        tipo = if (tipo == "vip") TipoHabitacion.VIP else TipoHabitacion.SEMIPRIVADA,
        camas = camas.map { it.aDominio(codigo) },
    )

/** La letra sale del sufijo del código (`HAB-201-A` → `A`); en VIP, una sola cama, queda vacía. */
fun CamaMapaDto.aDominio(codigoHabitacion: String): Cama =
    Cama(
        id = id,
        codigo = codigo,
        letra =
            if (codigoHabitacion.startsWith("VIP")) {
                ""
            } else {
                codigo.removePrefix(codigoHabitacion).removePrefix("-")
            },
        estado = EstadoCama.desdeApi(estado),
        ocupante =
            ocupante?.let {
                Ocupante(
                    hospitalizacionId = it.hospitalizacionId,
                    pacienteId = it.paciente.id,
                    nombre = "${it.paciente.nombres} ${it.paciente.apellidos}".trim(),
                    nombreCorto = nombreCorto(it.paciente.nombres, it.paciente.apellidos),
                    fechaIngreso = it.fechaIngreso,
                )
            },
    )

/** `Luis Alberto`, `Martínez Rivas` → `L. Martínez`. */
internal fun nombreCorto(
    nombres: String,
    apellidos: String,
): String {
    val inicial = nombres.trim().firstOrNull()?.let { "$it." }
    val apellido = apellidos.trim().substringBefore(' ').ifEmpty { null }
    return listOfNotNull(inicial, apellido).joinToString(" ")
}

fun PersonalDto.aDoctor(): DoctorDisponible =
    DoctorDisponible(
        id = id,
        nombre = "$nombres $apellidos".trim(),
        especialidad = especialidad?.nombre,
    )
