package sv.edu.unicaes.hospital.ui

import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.data.repository.CitasRepository
import sv.edu.unicaes.hospital.domain.model.Cita
import sv.edu.unicaes.hospital.domain.model.DoctorDisponible
import sv.edu.unicaes.hospital.domain.model.TurnoMedico

/** Repositorio de citas en memoria: cada respuesta se configura y cada escritura queda registrada. */
class FakeCitasRepository(
    var dePaciente: Resultado<List<Cita>> = Resultado.Exito(emptyList()),
    var citas: Resultado<List<Cita>> = Resultado.Exito(emptyList()),
    var cita: Resultado<Cita>? = null,
    var turnos: Resultado<List<TurnoMedico>> = Resultado.Exito(emptyList()),
    var doctores: Resultado<List<DoctorDisponible>> = Resultado.Exito(emptyList()),
    var escritura: Resultado<Unit> = Resultado.Exito(Unit),
) : CitasRepository {
    val creadas = mutableListOf<List<String?>>()
    val reprogramadas = mutableListOf<List<String>>()
    val canceladas = mutableListOf<String>()
    val consultasDoctores = mutableListOf<Pair<String, String?>>()

    override suspend fun dePaciente(pacienteId: String) = dePaciente

    override suspend fun entre(
        desde: String,
        hasta: String,
    ) = citas

    override suspend fun obtener(id: String) = cita ?: error("sin cita configurada")

    override suspend fun turnosMedicos(
        desde: String,
        hasta: String,
    ) = turnos

    override suspend fun doctoresDisponibles(
        fechaHora: String,
        excluirCitaId: String?,
    ): Resultado<List<DoctorDisponible>> {
        consultasDoctores += fechaHora to excluirCitaId
        return doctores
    }

    override suspend fun crear(
        pacienteId: String,
        doctorId: String,
        fechaHora: String,
        motivo: String?,
    ): Resultado<Unit> {
        creadas += listOf(pacienteId, doctorId, fechaHora, motivo)
        return escritura
    }

    override suspend fun reprogramar(
        citaId: String,
        fechaHora: String,
        doctorId: String,
    ): Resultado<Unit> {
        reprogramadas += listOf(citaId, fechaHora, doctorId)
        return escritura
    }

    override suspend fun cancelar(citaId: String): Resultado<Unit> {
        canceladas += citaId
        return escritura
    }
}
