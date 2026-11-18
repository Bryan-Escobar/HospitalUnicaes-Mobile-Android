package sv.edu.unicaes.hospital.ui

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import sv.edu.unicaes.hospital.core.AppError
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.data.repository.HospitalizacionesRepository
import sv.edu.unicaes.hospital.data.repository.PacientesRepository
import sv.edu.unicaes.hospital.domain.model.ConsultaClinica
import sv.edu.unicaes.hospital.domain.model.DatosPaciente
import sv.edu.unicaes.hospital.domain.model.Diagnostico
import sv.edu.unicaes.hospital.domain.model.EstadoLaboratorio
import sv.edu.unicaes.hospital.domain.model.ExamenOrden
import sv.edu.unicaes.hospital.domain.model.Expediente
import sv.edu.unicaes.hospital.domain.model.HistorialClinico
import sv.edu.unicaes.hospital.domain.model.Hospitalizacion
import sv.edu.unicaes.hospital.domain.model.OrdenLaboratorio
import sv.edu.unicaes.hospital.domain.model.OrdenResumen
import sv.edu.unicaes.hospital.domain.model.Paciente
import sv.edu.unicaes.hospital.domain.model.Pagina
import sv.edu.unicaes.hospital.domain.model.PrioridadLaboratorio
import sv.edu.unicaes.hospital.domain.model.RecetaEmitida
import sv.edu.unicaes.hospital.domain.model.TipoHabitacion
import sv.edu.unicaes.hospital.ui.doctor.ExpedienteViewModel
import sv.edu.unicaes.hospital.ui.doctor.MisPacientesViewModel
import sv.edu.unicaes.hospital.ui.modelo.ExpedienteUi
import sv.edu.unicaes.hospital.ui.modelo.PacienteUi
import java.util.Calendar

/** HUNI-61 · Mis pacientes y expediente del doctor con datos de la API. */
@OptIn(ExperimentalCoroutinesApi::class)
class PacientesDoctorViewModelsTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val maria = Paciente(id = "p1", nombres = "María", apellidos = "López", documento = "04512378-9")
    private val tomas =
        Paciente(
            id = "p2",
            nombres = "Tomás",
            apellidos = "Mora",
            numeroExpediente = "EXP-0006",
            fechaNacimiento = "1974-03-02",
            telefono = "7000-4444",
            contactoEmergencia = "Lucía Mora",
            telefonoEmergencia = "7000-5555",
        )

    private val camaTomas =
        Hospitalizacion(
            id = "h1",
            habitacion = "HAB-206",
            cama = "A",
            tipo = TipoHabitacion.SEMIPRIVADA,
            planta = 2,
            fechaIngreso = "2026-10-20T22:40:00Z",
            medicoTratante = "Jorge Peña",
            pacienteId = "p2",
        )

    private class FakePacientes(
        var atendidos: Resultado<List<Paciente>>,
    ) : PacientesRepository {
        override suspend fun listar(
            buscar: String?,
            pagina: Int,
        ): Resultado<Pagina<Paciente>> = error("no se usa")

        override suspend fun atendidos() = atendidos

        override suspend fun obtener(id: String): Resultado<Paciente> = error("no se usa")

        override suspend fun crear(datos: DatosPaciente): Resultado<Paciente> = error("no se usa")

        override suspend fun actualizar(
            id: String,
            datos: DatosPaciente,
        ): Resultado<Paciente> = error("no se usa")
    }

    private class FakeHospitalizaciones(
        var activas: Resultado<List<Hospitalizacion>> = Resultado.Exito(emptyList()),
        var activa: Resultado<Hospitalizacion?> = Resultado.Exito(null),
    ) : HospitalizacionesRepository {
        override suspend fun activaDe(pacienteId: String) = activa

        override suspend fun activas() = activas
    }

    // Mis pacientes ----------------------------------------------------------------------------------

    @Test
    fun `lista los pacientes del doctor y marca la cama de los internados`() {
        val vm =
            MisPacientesViewModel(
                FakePacientes(Resultado.Exito(listOf(maria, tomas))),
                FakeHospitalizaciones(activas = Resultado.Exito(listOf(camaTomas))),
            )

        assertEquals(
            UiState.Exito(
                listOf(
                    PacienteUi("p1", "María López", "04512378-9"),
                    PacienteUi("p2", "Tomás Mora", "EXP-0006", etiqueta = "HAB-206 · A", hospitalizado = true),
                ),
            ),
            vm.uiState.value,
        )
    }

    @Test
    fun `sin hospitalizaciones la lista se muestra igual`() {
        val vm =
            MisPacientesViewModel(
                FakePacientes(Resultado.Exito(listOf(tomas))),
                FakeHospitalizaciones(activas = Resultado.Fallo(AppError.SinConexion)),
            )

        val paciente = (vm.uiState.value as UiState.Exito).datos.single()
        assertFalse(paciente.hospitalizado)
        assertNull(paciente.etiqueta)
    }

    @Test
    fun `sin pacientes muestra vacio y un error se puede reintentar`() {
        val pacientes = FakePacientes(Resultado.Exito(emptyList()))
        val vm = MisPacientesViewModel(pacientes, FakeHospitalizaciones())
        assertEquals(UiState.Vacio, vm.uiState.value)

        pacientes.atendidos = Resultado.Fallo(AppError.SinConexion)
        vm.cargar()
        assertEquals(UiState.Error(AppError.SinConexion), vm.uiState.value)

        pacientes.atendidos = Resultado.Exito(listOf(maria))
        vm.cargar()
        assertTrue(vm.uiState.value is UiState.Exito)
    }

    // Expediente -------------------------------------------------------------------------------------

    private val historialTomas =
        HistorialClinico(
            paciente = tomas,
            expediente = Expediente(numeroExpediente = "EXP-0006", alergias = "Ibuprofeno"),
            consultas =
                listOf(
                    ConsultaClinica(
                        id = "c1",
                        fecha = "2026-10-20T16:00:00Z",
                        doctor = "Jorge Peña",
                        diagnosticos = listOf(Diagnostico("Apendicitis aguda")),
                        recetas = listOf(RecetaEmitida("r1", medicamentos = listOf("Amoxicilina"))),
                        ordenes =
                            listOf(
                                OrdenResumen("o1", "2026-10-20T16:25:00Z", EstadoLaboratorio.PENDIENTE, 2),
                            ),
                    ),
                    ConsultaClinica(id = "c0", fecha = "2026-06-03T15:00:00Z", doctor = "Carlos Molina"),
                ),
        )

    private val ordenTomas =
        OrdenLaboratorio(
            id = "o1",
            fechaSolicitud = "2026-10-20T16:25:00Z",
            prioridad = PrioridadLaboratorio.NORMAL,
            estado = EstadoLaboratorio.PENDIENTE,
            pacienteId = "p2",
            paciente = "Tomás Mora",
            doctor = "Jorge Peña",
            examenes =
                listOf(
                    ExamenOrden("e1", "Hemograma completo", estado = EstadoLaboratorio.PENDIENTE),
                    ExamenOrden("e2", "Glucosa", estado = EstadoLaboratorio.PENDIENTE),
                ),
        )

    private fun expediente(
        consultas: FakeConsultasRepository = FakeConsultasRepository(historial = Resultado.Exito(historialTomas)),
        hospitalizaciones: FakeHospitalizaciones = FakeHospitalizaciones(activa = Resultado.Exito(camaTomas)),
        laboratorio: FakeLaboratorioDoctor = FakeLaboratorioDoctor(delPaciente = Resultado.Exito(listOf(ordenTomas))),
    ) = ExpedienteViewModel(SavedStateHandle(mapOf("pacienteId" to "p2")), consultas, hospitalizaciones, laboratorio)

    @Test
    fun `arma el expediente con consultas, cama, alergias y ordenes`() {
        val datos: ExpedienteUi = (expediente().uiState.value as UiState.Exito).datos

        val edadEsperada = Calendar.getInstance().get(Calendar.YEAR) - 1974
        assertTrue(datos.edad == edadEsperada || datos.edad == edadEsperada - 1)
        assertEquals("Ibuprofeno", datos.alergias)
        assertEquals("HAB-206 · A", datos.hospitalizacion?.codigo)
        assertTrue(datos.paciente.hospitalizado)
        assertEquals("Lucía Mora · 7000-5555", datos.contactoEmergencia)

        val consulta = datos.consultas.first()
        assertEquals("Apendicitis aguda", consulta.diagnostico)
        assertEquals(1, consulta.recetas)
        assertEquals(1, consulta.ordenes)
        // Sin diagnóstico ni motivo el título queda vacío y la pantalla pone "Sin diagnóstico".
        assertEquals("", datos.consultas[1].diagnostico)

        val orden = datos.laboratorio.single()
        assertEquals("Hemograma completo, Glucosa", orden.examen)
        assertEquals(EstadoLaboratorio.PENDIENTE, orden.estado)
    }

    @Test
    fun `sin fecha de nacimiento ni cama el expediente se arma igual`() {
        val sinDatos = historialTomas.copy(paciente = tomas.copy(fechaNacimiento = null))
        val datos =
            (
                expediente(
                    consultas = FakeConsultasRepository(historial = Resultado.Exito(sinDatos)),
                    hospitalizaciones = FakeHospitalizaciones(),
                ).uiState.value as UiState.Exito
            ).datos

        assertNull(datos.edad)
        assertNull(datos.hospitalizacion)
        assertFalse(datos.paciente.hospitalizado)
    }

    @Test
    fun `si el doctor no es tratante muestra sin permiso`() {
        val vm = expediente(consultas = FakeConsultasRepository(historial = Resultado.Fallo(AppError.SinPermiso)))

        assertEquals(UiState.Error(AppError.SinPermiso), vm.uiState.value)
    }

    @Test
    fun `si fallan las ordenes muestra el error y al reintentar recarga todo`() {
        val consultas = FakeConsultasRepository(historial = Resultado.Exito(historialTomas))
        val laboratorio = FakeLaboratorioDoctor(delPaciente = Resultado.Fallo(AppError.SinConexion))
        val vm = expediente(consultas = consultas, laboratorio = laboratorio)
        assertEquals(UiState.Error(AppError.SinConexion), vm.uiState.value)

        laboratorio.delPaciente = Resultado.Exito(emptyList())
        vm.cargar()

        assertTrue(vm.uiState.value is UiState.Exito)
        assertEquals(2, consultas.historiales)
    }
}
