package sv.edu.unicaes.hospital.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.data.repository.CamasRepository
import sv.edu.unicaes.hospital.domain.model.DoctorDisponible
import sv.edu.unicaes.hospital.domain.model.Habitacion

/** Repositorio de camas con resultados configurables; registra las escrituras y simula Realtime. */
class FakeCamasRepository(
    var habitaciones: Resultado<List<Habitacion>> = Resultado.Exito(emptyList()),
    var doctores: Resultado<List<DoctorDisponible>> = Resultado.Exito(emptyList()),
    var resultadoIngreso: Resultado<Unit> = Resultado.Exito(Unit),
    var resultadoTraslado: Resultado<Unit> = Resultado.Exito(Unit),
    var resultadoAlta: Resultado<Unit> = Resultado.Exito(Unit),
    var resultadoDisponible: Resultado<Unit> = Resultado.Exito(Unit),
) : CamasRepository {
    data class Ingreso(
        val pacienteId: String,
        val doctorId: String,
        val camaId: String,
        val motivo: String?,
    )

    val ingresos = mutableListOf<Ingreso>()
    val traslados = mutableListOf<Pair<String, String>>()
    val altas = mutableListOf<String>()
    val disponibles = mutableListOf<String>()
    var lecturasHabitaciones = 0

    /** Emitir aquí simula un cambio de ocupación hecho desde otro dispositivo. */
    val cambiosRealtime = MutableSharedFlow<Unit>()

    override suspend fun habitaciones(): Resultado<List<Habitacion>> {
        lecturasHabitaciones++
        return habitaciones
    }

    override suspend fun ingresar(
        pacienteId: String,
        doctorId: String,
        camaId: String,
        motivo: String?,
    ): Resultado<Unit> {
        ingresos += Ingreso(pacienteId, doctorId, camaId, motivo)
        return resultadoIngreso
    }

    override suspend fun trasladar(
        hospitalizacionId: String,
        camaId: String,
    ): Resultado<Unit> {
        traslados += hospitalizacionId to camaId
        return resultadoTraslado
    }

    override suspend fun darAlta(
        hospitalizacionId: String,
        notas: String?,
    ): Resultado<Unit> {
        altas += hospitalizacionId
        return resultadoAlta
    }

    override suspend fun marcarDisponible(camaId: String): Resultado<Unit> {
        disponibles += camaId
        return resultadoDisponible
    }

    override suspend fun doctores(): Resultado<List<DoctorDisponible>> = doctores

    override fun cambios(): Flow<Unit> = cambiosRealtime
}
