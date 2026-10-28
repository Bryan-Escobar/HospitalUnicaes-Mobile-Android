package sv.edu.unicaes.hospital.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import sv.edu.unicaes.hospital.domain.model.Hospitalizacion
import sv.edu.unicaes.hospital.domain.model.TipoHabitacion

/** Esquema `Hospitalizacion` de /hospitalizaciones (ver /docs de la API). */
@Serializable
data class HospitalizacionDto(
    val id: String,
    val cama: CamaDto,
    @SerialName("doctor_tratante") val doctorTratante: PersonaResumenDto,
    @SerialName("fecha_ingreso") val fechaIngreso: String,
)

@Serializable
data class CamaDto(
    /** `HAB-201-A`: código de la habitación más la letra de la cama. */
    val codigo: String,
    val habitacion: HabitacionDto,
)

@Serializable
data class HabitacionDto(
    val codigo: String,
    val planta: Int,
    /** `vip` o `semiprivada`. */
    val tipo: String,
)

@Serializable
data class PersonaResumenDto(
    val nombres: String,
    val apellidos: String,
)

fun HospitalizacionDto.aDominio(): Hospitalizacion =
    Hospitalizacion(
        id = id,
        habitacion = cama.habitacion.codigo,
        cama = cama.codigo.removePrefix(cama.habitacion.codigo).removePrefix("-"),
        tipo = if (cama.habitacion.tipo == "vip") TipoHabitacion.VIP else TipoHabitacion.SEMIPRIVADA,
        planta = cama.habitacion.planta,
        fechaIngreso = fechaIngreso,
        medicoTratante = "${doctorTratante.nombres} ${doctorTratante.apellidos}".trim(),
    )
