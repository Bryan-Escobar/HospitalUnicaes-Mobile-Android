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
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.core.AppError
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.data.repository.PacientesRepository
import sv.edu.unicaes.hospital.domain.model.DatosPaciente
import sv.edu.unicaes.hospital.domain.model.Paciente
import sv.edu.unicaes.hospital.domain.model.Pagina
import sv.edu.unicaes.hospital.ui.recepcion.CampoPaciente
import sv.edu.unicaes.hospital.ui.recepcion.FormularioPaciente
import sv.edu.unicaes.hospital.ui.recepcion.RegistroPacienteViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class RegistroPacienteViewModelTest {
    private val existente =
        Paciente(
            id = "a1",
            nombres = "Carlos",
            apellidos = "Martínez",
            documento = "01234567-8",
            fechaNacimiento = "1990-05-14",
            telefono = "7000-0000",
        )

    private val formValido =
        FormularioPaciente(
            nombres = "María",
            apellidos = "López",
            documento = "09876543-2",
            nacimiento = "01/02/1985",
            telefono = "7222-2222",
        )

    /** Repositorio en memoria que registra lo que se envió. */
    private class FakePacientesRepository(
        var respuestaGuardar: Resultado<Paciente>? = null,
        val pacientes: List<Paciente> = emptyList(),
    ) : PacientesRepository {
        val creados = mutableListOf<DatosPaciente>()
        val actualizados = mutableListOf<Pair<String, DatosPaciente>>()

        override suspend fun listar(
            buscar: String?,
            pagina: Int,
        ): Resultado<Pagina<Paciente>> =
            Resultado.Exito(Pagina(pacientes.filter { buscar == null || it.documento == buscar }, 1, 1))

        override suspend fun obtener(id: String): Resultado<Paciente> =
            pacientes.firstOrNull { it.id == id }?.let { Resultado.Exito(it) } ?: Resultado.Fallo(AppError.NoEncontrado)

        override suspend fun crear(datos: DatosPaciente): Resultado<Paciente> {
            creados += datos
            return respuestaGuardar ?: Resultado.Exito(Paciente(id = "nuevo", nombres = "", apellidos = ""))
        }

        override suspend fun actualizar(
            id: String,
            datos: DatosPaciente,
        ): Resultado<Paciente> {
            actualizados += id to datos
            return respuestaGuardar ?: Resultado.Exito(pacientes.first { it.id == id })
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        repo: PacientesRepository,
        pacienteId: String? = null,
    ) = RegistroPacienteViewModel(SavedStateHandle(mapOf("pacienteId" to pacienteId)), repo)

    @Test
    fun `no llama a la API si la validacion falla`() {
        val repo = FakePacientesRepository()
        val vm = viewModel(repo)

        vm.onFormChange { it.copy(nombres = "María") }
        vm.guardar()

        assertTrue(repo.creados.isEmpty())
        assertEquals(R.string.campo_obligatorio, vm.uiState.value.errores[CampoPaciente.DOCUMENTO])
        assertFalse(vm.uiState.value.guardado)
    }

    @Test
    fun `editar un campo borra su error`() {
        val vm = viewModel(FakePacientesRepository())
        vm.guardar()
        assertTrue(CampoPaciente.NOMBRES in vm.uiState.value.errores)

        vm.onFormChange { it.copy(nombres = "María") }

        assertFalse(CampoPaciente.NOMBRES in vm.uiState.value.errores)
        assertTrue(CampoPaciente.APELLIDOS in vm.uiState.value.errores)
    }

    @Test
    fun `crea el paciente con el formulario valido`() {
        val repo = FakePacientesRepository()
        val vm = viewModel(repo)

        vm.onFormChange { formValido }
        vm.guardar()

        assertEquals("1985-02-01", repo.creados.single().fechaNacimiento)
        assertTrue(vm.uiState.value.guardado)
    }

    @Test
    fun `documento duplicado ofrece abrir la ficha existente`() {
        val repo = FakePacientesRepository(Resultado.Fallo(AppError.Duplicado), pacientes = listOf(existente))
        val vm = viewModel(repo)

        vm.onFormChange { formValido.copy(documento = "01234567-8") }
        vm.guardar()

        val estado = vm.uiState.value
        assertEquals("01234567-8", estado.documentoDuplicado)
        assertEquals("a1", estado.duplicadoId)
        assertFalse(estado.guardando)
        assertFalse(estado.guardado)
    }

    @Test
    fun `errores 400 de la API se marcan en su campo`() {
        val repo = FakePacientesRepository(Resultado.Fallo(AppError.CamposInvalidos(mapOf("telefono" to "Too small"))))
        val vm = viewModel(repo)

        vm.onFormChange { formValido }
        vm.guardar()

        assertEquals(R.string.error_campo_invalido, vm.uiState.value.errores[CampoPaciente.TELEFONO])
        assertNull(vm.uiState.value.errorGeneral)
    }

    @Test
    fun `sin conexion muestra el error general`() {
        val vm = viewModel(FakePacientesRepository(Resultado.Fallo(AppError.SinConexion)))

        vm.onFormChange { formValido }
        vm.guardar()

        assertEquals(AppError.SinConexion, vm.uiState.value.errorGeneral)
    }

    @Test
    fun `en edicion carga el paciente y envia solo lo que cambio`() {
        val repo = FakePacientesRepository(pacientes = listOf(existente))
        val vm = viewModel(repo, pacienteId = "a1")
        assertEquals("14/05/1990", vm.uiState.value.form.nacimiento)

        vm.onFormChange { it.copy(telefono = "7333-3333") }
        vm.guardar()

        assertEquals("a1" to DatosPaciente(telefono = "7333-3333"), repo.actualizados.single())
        assertTrue(vm.uiState.value.guardado)
    }

    @Test
    fun `en edicion sin cambios cierra sin llamar a la API`() {
        val repo = FakePacientesRepository(pacientes = listOf(existente))
        val vm = viewModel(repo, pacienteId = "a1")

        vm.guardar()

        assertTrue(repo.actualizados.isEmpty())
        assertTrue(vm.uiState.value.guardado)
    }
}
