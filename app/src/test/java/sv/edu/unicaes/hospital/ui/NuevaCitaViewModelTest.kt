package sv.edu.unicaes.hospital.ui

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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
import sv.edu.unicaes.hospital.data.repository.PacientesRepository
import sv.edu.unicaes.hospital.domain.model.Cita
import sv.edu.unicaes.hospital.domain.model.DatosPaciente
import sv.edu.unicaes.hospital.domain.model.DoctorDisponible
import sv.edu.unicaes.hospital.domain.model.EstadoCita
import sv.edu.unicaes.hospital.domain.model.Paciente
import sv.edu.unicaes.hospital.domain.model.Pagina
import sv.edu.unicaes.hospital.domain.model.TurnoMedico
import sv.edu.unicaes.hospital.ui.modelo.PacienteUi
import sv.edu.unicaes.hospital.ui.modelo.fechaHoraHospital
import sv.edu.unicaes.hospital.ui.modelo.isoConZona
import sv.edu.unicaes.hospital.ui.modelo.proximosDias
import sv.edu.unicaes.hospital.ui.recepcion.NuevaCitaViewModel
import sv.edu.unicaes.hospital.ui.recepcion.ResultadoCita

@OptIn(ExperimentalCoroutinesApi::class)
class NuevaCitaViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    // Mañana: los huecos de hoy dependen de la hora en que corre el test.
    private val manana = proximosDias(2)[1]

    private fun iso(hora: String) = isoConZona(fechaHoraHospital(manana, hora)!!)

    private val turnoAna = TurnoMedico("d1", "Ana Rivas", "Alergista", iso("08:00"), iso("10:00"))
    private val ana = DoctorDisponible("d1", "Ana Rivas", "Alergista")
    private val luis = Paciente(id = "p1", nombres = "Luis", apellidos = "Martínez", documento = "01234567-8")

    private val citaLuis =
        Cita(
            id = "c1",
            fechaHora = iso("09:00"),
            doctor = "Ana Rivas",
            motivo = "Control",
            estado = EstadoCita.PROGRAMADA,
            pacienteId = "p1",
            paciente = "Luis Martínez",
            doctorId = "d1",
        )

    private class FakePacientes(
        val encontrados: List<Paciente> = emptyList(),
        val paciente: Paciente? = null,
    ) : PacientesRepository {
        val busquedas = mutableListOf<String?>()

        override suspend fun listar(
            buscar: String?,
            pagina: Int,
        ): Resultado<Pagina<Paciente>> {
            busquedas += buscar
            return Resultado.Exito(Pagina(encontrados, pagina, 1))
        }

        override suspend fun obtener(id: String): Resultado<Paciente> =
            paciente?.let { Resultado.Exito(it) } ?: Resultado.Fallo(AppError.NoEncontrado)

        override suspend fun crear(datos: DatosPaciente): Resultado<Paciente> = error("no se usa")

        override suspend fun actualizar(
            id: String,
            datos: DatosPaciente,
        ): Resultado<Paciente> = error("no se usa")
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        repo: FakeCitasRepository,
        pacientes: FakePacientes = FakePacientes(),
        vararg argumentos: Pair<String, String>,
    ) = NuevaCitaViewModel(SavedStateHandle(mapOf(*argumentos)), repo, pacientes)

    private fun desdeHueco(repo: FakeCitasRepository) =
        viewModel(
            repo,
            FakePacientes(),
            "fecha" to manana,
            "hora" to "08:00",
            "doctorId" to "d1",
        )

    @Test
    fun `desde un hueco libre llega con dia, hora y doctor elegidos`() {
        val repo =
            FakeCitasRepository(turnos = Resultado.Exito(listOf(turnoAna)), doctores = Resultado.Exito(listOf(ana)))
        val estado = desdeHueco(repo).uiState.value

        assertEquals(manana, estado.dias[estado.dia].clave)
        assertEquals(UiState.Exito(listOf("08:00", "08:30", "09:00", "09:30")), estado.horas)
        assertEquals("08:00", estado.hora)
        assertEquals(UiState.Exito(listOf(ana)), estado.doctores)
        assertEquals("d1", estado.doctorId)
        assertFalse("falta el paciente", estado.puedeGuardar)
    }

    @Test
    fun `crea la cita con el paciente, el doctor y la hora elegidos`() {
        val repo =
            FakeCitasRepository(turnos = Resultado.Exito(listOf(turnoAna)), doctores = Resultado.Exito(listOf(ana)))
        val vm = desdeHueco(repo)

        vm.onPaciente(PacienteUi("p1", "Luis Martínez", "01234567-8"))
        vm.onMotivo("  Control de alergia ")
        vm.guardar()

        assertEquals(listOf(listOf("p1", "d1", iso("08:00"), "Control de alergia")), repo.creadas)
        assertEquals(ResultadoCita.GUARDADA, vm.uiState.value.resultado)
    }

    @Test
    fun `el choque de horario muestra el mensaje de la base de datos`() {
        val choque = AppError.Rechazada("El doctor ya tiene una cita el 30/10/2026 a las 08:00")
        val repo =
            FakeCitasRepository(
                turnos = Resultado.Exito(listOf(turnoAna)),
                doctores = Resultado.Exito(listOf(ana)),
                escritura = Resultado.Fallo(choque),
            )
        val vm = desdeHueco(repo)

        vm.onPaciente(PacienteUi("p1", "Luis Martínez", "01234567-8"))
        vm.guardar()

        assertEquals(choque, vm.uiState.value.error)
        assertNull(vm.uiState.value.resultado)
        assertFalse(vm.uiState.value.guardando)
    }

    @Test
    fun `sin doctores de turno libres no se puede guardar`() {
        val repo =
            FakeCitasRepository(turnos = Resultado.Exito(listOf(turnoAna)), doctores = Resultado.Exito(emptyList()))
        val vm = desdeHueco(repo)
        vm.onPaciente(PacienteUi("p1", "Luis Martínez", "01234567-8"))

        assertEquals(UiState.Vacio, vm.uiState.value.doctores)
        assertNull(vm.uiState.value.doctorId)
        assertFalse(vm.uiState.value.puedeGuardar)
    }

    @Test
    fun `sin turnos ese dia no hay horas`() {
        val vm = viewModel(FakeCitasRepository(), FakePacientes(), "fecha" to manana)
        assertEquals(UiState.Vacio, vm.uiState.value.horas)
        assertNull(vm.uiState.value.doctores)
    }

    @Test
    fun `al reprogramar precarga la cita y su horario cuenta como libre`() {
        val repo =
            FakeCitasRepository(
                cita = Resultado.Exito(citaLuis),
                citas = Resultado.Exito(listOf(citaLuis)),
                turnos = Resultado.Exito(listOf(turnoAna)),
                doctores = Resultado.Exito(listOf(ana)),
            )
        val vm = viewModel(repo, FakePacientes(), "citaId" to "c1")
        val estado = vm.uiState.value

        assertTrue(estado.reprogramar)
        assertEquals("Luis Martínez", estado.paciente?.nombre)
        assertEquals(manana, estado.dias[estado.dia].clave)
        assertEquals("09:00", estado.hora)
        assertTrue((estado.horas as UiState.Exito).datos.contains("09:00"))
        assertEquals("c1", repo.consultasDoctores.last().second)

        vm.onHora("09:30")
        vm.guardar()
        assertEquals(listOf(listOf("c1", iso("09:30"), "d1")), repo.reprogramadas)
        assertEquals(ResultadoCita.GUARDADA, vm.uiState.value.resultado)
    }

    @Test
    fun `cancela la cita que se esta reprogramando`() {
        val repo = FakeCitasRepository(cita = Resultado.Exito(citaLuis), turnos = Resultado.Exito(listOf(turnoAna)))
        val vm = viewModel(repo, FakePacientes(), "citaId" to "c1")

        vm.cancelarCita()

        assertEquals(listOf("c1"), repo.canceladas)
        assertEquals(ResultadoCita.CANCELADA, vm.uiState.value.resultado)
    }

    @Test
    fun `al reprogramar no se puede cambiar el paciente`() {
        val repo = FakeCitasRepository(cita = Resultado.Exito(citaLuis))
        val vm = viewModel(repo, FakePacientes(), "citaId" to "c1")
        vm.onCambiarPaciente()
        assertEquals(
            "p1",
            vm.uiState.value.paciente
                ?.id,
        )
    }

    @Test
    fun `desde la ficha llega con el paciente elegido`() {
        val vm = viewModel(FakeCitasRepository(), FakePacientes(paciente = luis), "pacienteId" to "p1")
        assertEquals(
            "Luis Martínez",
            vm.uiState.value.paciente
                ?.nombre,
        )
        assertEquals(
            "01234567-8",
            vm.uiState.value.paciente
                ?.dui,
        )
    }

    @Test
    fun `busca pacientes desde 2 letras`() =
        runTest(dispatcher) {
            val pacientes = FakePacientes(encontrados = listOf(luis))
            val vm = viewModel(FakeCitasRepository(), pacientes)

            vm.onBusqueda("L")
            advanceTimeBy(400)
            assertNull(vm.uiState.value.resultados)

            vm.onBusqueda("Lu")
            advanceTimeBy(400)
            assertEquals(listOf("Lu"), pacientes.busquedas)
            assertEquals("Luis Martínez", (vm.uiState.value.resultados as UiState.Exito).datos.single().nombre)
        }

    @Test
    fun `bloquea las horas en que el paciente ya tiene otra cita`() {
        // Luis tiene cita a las 08:30 con otro doctor; Ana sigue libre a esa hora.
        val conOtroDoctor = citaLuis.copy(id = "c9", fechaHora = iso("08:30"), doctorId = "d2")
        val repo =
            FakeCitasRepository(
                citas = Resultado.Exito(listOf(conOtroDoctor)),
                turnos = Resultado.Exito(listOf(turnoAna)),
                doctores = Resultado.Exito(listOf(ana)),
            )
        val vm = viewModel(repo, FakePacientes(), "fecha" to manana, "hora" to "08:30")
        assertEquals("08:30", vm.uiState.value.hora)
        assertTrue(
            vm.uiState.value.horasConCitaPaciente
                .isEmpty(),
        )

        vm.onPaciente(PacienteUi("p1", "Luis Martínez", "01234567-8"))

        assertEquals(setOf("08:30"), vm.uiState.value.horasConCitaPaciente)
        assertNull("la hora elegida chocaba y se quita", vm.uiState.value.hora)
        assertNull(vm.uiState.value.doctores)

        vm.onHora("08:30")
        assertNull("no se puede elegir una hora bloqueada", vm.uiState.value.hora)

        vm.onCambiarPaciente()
        assertTrue(
            vm.uiState.value.horasConCitaPaciente
                .isEmpty(),
        )
    }

    @Test
    fun `desde la ficha bloquea las horas del paciente al cargar`() {
        val repo =
            FakeCitasRepository(
                citas = Resultado.Exito(listOf(citaLuis)),
                turnos = Resultado.Exito(listOf(turnoAna, turnoAna.copy(doctorId = "d2", doctor = "Carlos Molina"))),
            )
        val vm = viewModel(repo, FakePacientes(paciente = luis), "pacienteId" to "p1", "fecha" to manana)

        assertEquals(setOf("09:00"), vm.uiState.value.horasConCitaPaciente)
    }
}
