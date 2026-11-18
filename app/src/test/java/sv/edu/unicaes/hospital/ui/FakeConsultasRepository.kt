package sv.edu.unicaes.hospital.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import sv.edu.unicaes.hospital.core.AppError
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.data.repository.ConsultasRepository
import sv.edu.unicaes.hospital.data.repository.LaboratorioRepository
import sv.edu.unicaes.hospital.domain.model.ArchivoAdjunto
import sv.edu.unicaes.hospital.domain.model.DatosResultado
import sv.edu.unicaes.hospital.domain.model.EstadoLaboratorio
import sv.edu.unicaes.hospital.domain.model.ExamenCatalogo
import sv.edu.unicaes.hospital.domain.model.ExamenOrden
import sv.edu.unicaes.hospital.domain.model.Expediente
import sv.edu.unicaes.hospital.domain.model.HistorialClinico
import sv.edu.unicaes.hospital.domain.model.NuevaConsulta
import sv.edu.unicaes.hospital.domain.model.NuevaOrden
import sv.edu.unicaes.hospital.domain.model.NuevaReceta
import sv.edu.unicaes.hospital.domain.model.OrdenLaboratorio
import sv.edu.unicaes.hospital.domain.model.Paciente
import sv.edu.unicaes.hospital.domain.model.Pagina
import sv.edu.unicaes.hospital.domain.model.PrioridadLaboratorio

/**
 * Historial y consulta en memoria. Cada escritura queda en [pasos] en el orden en que llegó, para comprobar
 * que el guardado sigue la secuencia de la API y que un reintento no repite lo ya enviado.
 */
class FakeConsultasRepository(
    var historial: Resultado<HistorialClinico> =
        Resultado.Exito(
            HistorialClinico(
                paciente = Paciente(id = "p1", nombres = "María", apellidos = "López"),
                expediente = Expediente(numeroExpediente = "EXP-0001"),
                consultas = emptyList(),
            ),
        ),
    var crear: Resultado<String> = Resultado.Exito("c1"),
    var diagnostico: Resultado<Unit> = Resultado.Exito(Unit),
    var receta: Resultado<Unit> = Resultado.Exito(Unit),
    var cerrar: Resultado<Unit> = Resultado.Exito(Unit),
) : ConsultasRepository {
    val pasos = mutableListOf<String>()
    val consultasCreadas = mutableListOf<NuevaConsulta>()
    var historiales = 0

    override suspend fun historial(pacienteId: String): Resultado<HistorialClinico> {
        historiales++
        return historial
    }

    override suspend fun crear(consulta: NuevaConsulta): Resultado<String> {
        pasos += "crear"
        consultasCreadas += consulta
        return crear
    }

    override suspend fun agregarDiagnostico(
        consultaId: String,
        descripcion: String,
    ): Resultado<Unit> {
        pasos += "diagnostico:$consultaId:$descripcion"
        return diagnostico
    }

    override suspend fun emitirReceta(
        consultaId: String,
        receta: NuevaReceta,
    ): Resultado<Unit> {
        pasos += "receta:$consultaId:${receta.medicamento}"
        return this.receta
    }

    override suspend fun cerrar(consultaId: String): Resultado<Unit> {
        pasos += "cerrar:$consultaId"
        return cerrar
    }
}

/** Laboratorio del doctor en memoria: catálogo, órdenes del paciente y órdenes creadas (en [pasos]). */
class FakeLaboratorioDoctor(
    var catalogo: Resultado<List<ExamenCatalogo>> = Resultado.Exito(emptyList()),
    var delPaciente: Resultado<List<OrdenLaboratorio>> = Resultado.Exito(emptyList()),
    var crear: Resultado<OrdenLaboratorio>? = null,
    private val pasos: MutableList<String> = mutableListOf(),
) : LaboratorioRepository {
    val ordenesCreadas = mutableListOf<Pair<String, NuevaOrden>>()

    override suspend fun listar(
        estado: EstadoLaboratorio?,
        pagina: Int,
    ): Resultado<Pagina<OrdenLaboratorio>> = error("no se usa")

    override suspend fun dePaciente(pacienteId: String) = delPaciente

    override suspend fun examenes() = catalogo

    override suspend fun crearOrden(
        consultaId: String,
        orden: NuevaOrden,
    ): Resultado<OrdenLaboratorio> {
        pasos += "orden:$consultaId:${orden.examenIds.joinToString()}"
        ordenesCreadas += consultaId to orden
        return crear ?: Resultado.Exito(
            OrdenLaboratorio(
                id = "o${ordenesCreadas.size}",
                fechaSolicitud = "2026-11-17T16:00:00Z",
                prioridad = PrioridadLaboratorio.NORMAL,
                estado = EstadoLaboratorio.PENDIENTE,
                pacienteId = "p1",
                paciente = "María López",
                doctor = "Jorge Peña",
            ),
        )
    }

    override suspend fun contar(estado: EstadoLaboratorio): Resultado<Int> = error("no se usa")

    override suspend fun obtener(id: String): Resultado<OrdenLaboratorio> = Resultado.Fallo(AppError.NoEncontrado)

    override suspend fun iniciarExamen(
        ordenId: String,
        examenId: String,
    ): Resultado<ExamenOrden> = error("no se usa")

    override suspend fun registrarResultado(
        ordenId: String,
        examenId: String,
        datos: DatosResultado,
        archivo: ArchivoAdjunto?,
    ): Resultado<ExamenOrden> = error("no se usa")

    override fun cambios(): Flow<Unit> = emptyFlow()
}
