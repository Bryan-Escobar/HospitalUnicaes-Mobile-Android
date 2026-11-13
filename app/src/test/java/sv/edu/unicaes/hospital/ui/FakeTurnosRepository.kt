package sv.edu.unicaes.hospital.ui

import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.data.repository.TurnosRepository
import sv.edu.unicaes.hospital.domain.model.DatosTurno
import sv.edu.unicaes.hospital.domain.model.Empleado
import sv.edu.unicaes.hospital.domain.model.TipoTurno
import sv.edu.unicaes.hospital.domain.model.Turno

/** Repositorio de turnos en memoria: cada respuesta se configura y cada escritura queda registrada. */
class FakeTurnosRepository(
    var turnos: Resultado<List<Turno>> = Resultado.Exito(emptyList()),
    var mios: Resultado<List<Turno>> = Resultado.Exito(emptyList()),
    var empleados: Resultado<List<Empleado>> = Resultado.Exito(emptyList()),
    var escritura: Resultado<Unit> = Resultado.Exito(Unit),
) : TurnosRepository {
    val consultas = mutableListOf<Pair<String, String>>()
    val creados = mutableListOf<DatosTurno>()
    val actualizados = mutableListOf<Pair<String, DatosTurno>>()

    override suspend fun entre(
        desde: String,
        hasta: String,
    ): Resultado<List<Turno>> {
        consultas += desde to hasta
        return turnos
    }

    override suspend fun mios(
        desde: String,
        hasta: String,
    ) = mios

    override suspend fun empleados() = empleados

    override suspend fun crear(datos: DatosTurno): Resultado<Unit> {
        creados += datos
        return escritura
    }

    override suspend fun actualizar(
        id: String,
        datos: DatosTurno,
    ): Resultado<Unit> {
        actualizados += id to datos
        return escritura
    }
}

fun turno(
    id: String,
    fecha: String,
    area: String = "Emergencia",
    tipoPersonal: String = "Doctor",
    persona: String = "Ana Rivas",
    inicio: String = "07:00",
    fin: String = "15:00",
    tipoTurno: TipoTurno = TipoTurno.MANANA,
) = Turno(
    id = id,
    personalId = "p-$id",
    persona = persona,
    tipoPersonal = tipoPersonal,
    esMedico = tipoPersonal == "Doctor",
    area = area,
    tipoTurno = tipoTurno,
    fecha = fecha,
    horaInicio = inicio,
    horaFin = fin,
    inicio = "${fecha}T$inicio:00-06:00",
    fin = "${fecha}T$fin:00-06:00",
)
