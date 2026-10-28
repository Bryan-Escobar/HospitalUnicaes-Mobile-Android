package sv.edu.unicaes.hospital.ui

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import sv.edu.unicaes.hospital.core.AppError
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.data.repository.HospitalizacionesRepository
import sv.edu.unicaes.hospital.data.repository.PacientesRepository
import sv.edu.unicaes.hospital.domain.model.Cita
import sv.edu.unicaes.hospital.domain.model.DatosPaciente
import sv.edu.unicaes.hospital.domain.model.EstadoCita
import sv.edu.unicaes.hospital.domain.model.Hospitalizacion
import sv.edu.unicaes.hospital.domain.model.Paciente
import sv.edu.unicaes.hospital.domain.model.Pagina
import sv.edu.unicaes.hospital.domain.model.Sexo
import sv.edu.unicaes.hospital.domain.model.TipoHabitacion
import sv.edu.unicaes.hospital.ui.modelo.FichaPacienteUi
import sv.edu.unicaes.hospital.ui.recepcion.FichaPacienteViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class FichaPacienteViewModelTest {
    private val paciente =
        Paciente(id = "p1", nombres = "Luis", apellidos = "Martínez", documento = "01234567-8", telefono = "7000-0000")

    private val hospitalizacion =
        Hospitalizacion(
            id = "h1",
            habitacion = "HAB-204",
            cama = "A",
            tipo = TipoHabitacion.SEMIPRIVADA,
            planta = 2,
            fechaIngreso = "2026-10-19T22:40:00Z",
            medicoTratante = "Carlos Molina",
        )

    private val cita = Cita("c1", "2026-10-28T16:30:00+00:00", "Ana Rivas", "Control", EstadoCita.CONFIRMADA)

    private class FakePacientes(
        val resultado: Resultado<Paciente>,
    ) : PacientesRepository {
        override suspend fun listar(
            buscar: String?,
            pagina: Int,
        ): Resultado<Pagina<Paciente>> = error("no se usa")

        override suspend fun obtener(id: String) = resultado

        override suspend fun crear(datos: DatosPaciente): Resultado<Paciente> = error("no se usa")

        override suspend fun actualizar(
            id: String,
            datos: DatosPaciente,
        ): Resultado<Paciente> = error("no se usa")
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        pacienteRes: Resultado<Paciente> = Resultado.Exito(paciente),
        hospRes: Resultado<Hospitalizacion?> = Resultado.Exito(hospitalizacion),
        citasRes: Resultado<List<Cita>> = Resultado.Exito(listOf(cita)),
    ) = FichaPacienteViewModel(
        SavedStateHandle(mapOf("pacienteId" to "p1")),
        FakePacientes(pacienteRes),
        object : HospitalizacionesRepository {
            override suspend fun activaDe(pacienteId: String) = hospRes
        },
        FakeCitasRepository(dePaciente = citasRes),
    )

    private fun FichaPacienteViewModel.ficha(): FichaPacienteUi = (uiState.value as UiState.Exito).datos

    @Test
    fun `arma la ficha con la cama y las citas`() {
        val ficha = viewModel().ficha()

        assertEquals("Luis Martínez", ficha.paciente.nombre)
        assertTrue(ficha.paciente.hospitalizado)
        assertEquals("HAB-204", ficha.asignacion?.habitacion)
        assertEquals("A", ficha.asignacion?.cama)
        assertEquals("Ana Rivas", ficha.citas.single().doctor)
        assertEquals(EstadoCita.CONFIRMADA, ficha.citas.single().estado)
    }

    @Test
    fun `sin hospitalizacion activa no hay cama`() {
        val ficha = viewModel(hospRes = Resultado.Exito(null), citasRes = Resultado.Exito(emptyList())).ficha()

        assertNull(ficha.asignacion)
        assertTrue(ficha.citas.isEmpty())
    }

    @Test
    fun `si falla la cama se muestra el error y no una ficha sin cama`() {
        val vm = viewModel(hospRes = Resultado.Fallo(AppError.SinConexion))
        assertEquals(UiState.Error(AppError.SinConexion), vm.uiState.value)
    }

    @Test
    fun `si fallan las citas se muestra el error`() {
        val vm = viewModel(citasRes = Resultado.Fallo(AppError.SinPermiso))
        assertEquals(UiState.Error(AppError.SinPermiso), vm.uiState.value)
    }

    @Test
    fun `si el paciente no existe se muestra el error`() {
        val vm = viewModel(pacienteRes = Resultado.Fallo(AppError.NoEncontrado))
        assertEquals(UiState.Error(AppError.NoEncontrado), vm.uiState.value)
    }

    @Test
    fun `muestra los datos personales del paciente`() {
        val conDatos =
            paciente.copy(
                fechaNacimiento = "1990-05-14",
                sexo = Sexo.MASCULINO,
                direccion = "Santa Ana",
                numeroExpediente = "EXP-2026-00015",
            )
        val ficha = viewModel(pacienteRes = Resultado.Exito(conDatos)).ficha()

        assertEquals("14/05/1990", ficha.nacimiento)
        assertEquals(Sexo.MASCULINO, ficha.sexo)
        assertEquals("Santa Ana", ficha.direccion)
        assertEquals("EXP-2026-00015", ficha.numeroExpediente)
    }

    @Test
    fun `sin datos opcionales no hay filas vacias`() {
        val ficha = viewModel().ficha()

        assertNull(ficha.nacimiento)
        assertNull(ficha.sexo)
        assertNull(ficha.numeroExpediente)
    }
}
