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
import sv.edu.unicaes.hospital.domain.model.ExamenCatalogo
import sv.edu.unicaes.hospital.domain.model.Expediente
import sv.edu.unicaes.hospital.domain.model.HistorialClinico
import sv.edu.unicaes.hospital.domain.model.NuevaConsulta
import sv.edu.unicaes.hospital.domain.model.NuevaReceta
import sv.edu.unicaes.hospital.domain.model.Paciente
import sv.edu.unicaes.hospital.ui.doctor.ContextoConsultaUi
import sv.edu.unicaes.hospital.ui.doctor.NuevaConsultaViewModel
import sv.edu.unicaes.hospital.ui.doctor.alergiasCoincidentes

/** HUNI-61 · Nueva consulta: validación, guardado por pasos y reintento sin repetir lo ya enviado. */
@OptIn(ExperimentalCoroutinesApi::class)
class NuevaConsultaViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val hemograma = ExamenCatalogo("x1", "Hemograma completo")
    private val glucosa = ExamenCatalogo("x2", "Glucosa")

    private val consultas = FakeConsultasRepository()
    private val laboratorio =
        FakeLaboratorioDoctor(catalogo = Resultado.Exito(listOf(hemograma, glucosa)), pasos = consultas.pasos)

    private fun viewModel(
        citaId: String? = null,
        citaHora: String? = null,
    ) = NuevaConsultaViewModel(
        SavedStateHandle(mapOf("pacienteId" to "p1", "citaId" to citaId, "citaHora" to citaHora)),
        consultas,
        laboratorio,
    )

    private fun NuevaConsultaViewModel.conTodo() =
        apply {
            onDiagnostico("Apendicitis aguda")
            onNotas("Dolor en fosa ilíaca derecha")
            agregarReceta(NuevaReceta("Amoxicilina 500 mg", dosis = "1 cápsula", frecuencia = "Cada 8 horas"))
            agregarOrden(hemograma, "Ayuno de 8 horas")
            agregarOrden(glucosa, "")
        }

    @Test
    fun `carga el paciente con sus alergias y el catalogo de examenes`() {
        consultas.historial =
            Resultado.Exito(
                HistorialClinico(
                    paciente = Paciente(id = "p1", nombres = "María", apellidos = "López"),
                    expediente = Expediente(numeroExpediente = "EXP-1", alergias = "Penicilina"),
                    consultas = emptyList(),
                ),
            )

        val estado = viewModel(citaId = "ci1", citaHora = "10:00").uiState.value

        assertEquals(UiState.Exito(ContextoConsultaUi("María López", "Penicilina")), estado.contexto)
        assertEquals(UiState.Exito(listOf(hemograma, glucosa)), estado.catalogo)
        assertEquals("10:00", estado.citaHora)
        assertTrue(estado.puedeGuardar)
    }

    @Test
    fun `sin acceso al paciente no se puede guardar`() {
        consultas.historial = Resultado.Fallo(AppError.SinPermiso)

        val vm = viewModel()
        vm.onDiagnostico("Gripe")
        vm.guardar()

        assertEquals(UiState.Error(AppError.SinPermiso), vm.uiState.value.contexto)
        assertFalse(vm.uiState.value.puedeGuardar)
        assertTrue(consultas.pasos.isEmpty())
    }

    @Test
    fun `el diagnostico es obligatorio y no se llama a la API sin el`() {
        val vm = viewModel()

        assertFalse(vm.uiState.value.diagnosticoFaltante)
        vm.onDiagnostico("   ")
        vm.guardar()

        assertTrue(vm.uiState.value.diagnosticoFaltante)
        assertFalse(vm.uiState.value.guardando)
        assertTrue(consultas.pasos.isEmpty())
    }

    @Test
    fun `guarda consulta, diagnostico, recetas y ordenes en orden y cierra la consulta`() {
        val vm = viewModel(citaId = "ci1").conTodo()

        vm.guardar()

        assertEquals(
            listOf(
                "crear",
                "diagnostico:c1:Apendicitis aguda",
                "receta:c1:Amoxicilina 500 mg",
                "orden:c1:x1",
                "orden:c1:x2",
                "cerrar:c1",
            ),
            consultas.pasos,
        )
        assertEquals(
            NuevaConsulta("p1", "ci1", notas = "Dolor en fosa ilíaca derecha"),
            consultas.consultasCreadas.single(),
        )
        assertEquals(
            "Ayuno de 8 horas",
            laboratorio.ordenesCreadas
                .first()
                .second.indicaciones,
        )
        val estado = vm.uiState.value
        assertTrue(estado.guardada)
        assertFalse(estado.guardando)
        assertNull(estado.error)
        assertTrue(estado.recetas.all { it.enviada } && estado.ordenes.all { it.enviada })
    }

    @Test
    fun `si una orden falla, el reintento sigue desde ahi sin repetir lo enviado`() {
        val vm = viewModel().conTodo()
        laboratorio.crear = Resultado.Fallo(AppError.SinConexion)

        vm.guardar()

        val fallido = vm.uiState.value
        assertEquals(AppError.SinConexion, fallido.error)
        assertTrue(fallido.consultaCreada)
        assertTrue(fallido.diagnosticoEnviado)
        assertFalse(fallido.bloqueada)
        assertFalse(fallido.guardada)
        // Las notas viajaron con la consulta y el diagnóstico ya se registró: no se pueden cambiar.
        vm.onNotas("otra cosa")
        vm.onDiagnostico("otra cosa")
        assertEquals("Dolor en fosa ilíaca derecha", vm.uiState.value.notas)
        assertEquals("Apendicitis aguda", vm.uiState.value.diagnostico)

        laboratorio.crear = null
        consultas.pasos.clear()
        vm.guardar()

        assertEquals(listOf("orden:c1:x1", "orden:c1:x2", "cerrar:c1"), consultas.pasos)
        assertTrue(vm.uiState.value.guardada)
    }

    @Test
    fun `una receta enviada no se vuelve a enviar si falla el cierre`() {
        val vm = viewModel().conTodo()
        consultas.cerrar = Resultado.Fallo(AppError.Servidor(500))

        vm.guardar()
        consultas.cerrar = Resultado.Exito(Unit)
        consultas.pasos.clear()
        vm.guardar()

        assertEquals(listOf("cerrar:c1"), consultas.pasos)
        assertTrue(vm.uiState.value.guardada)
    }

    @Test
    fun `si la cita ya tiene consulta el formulario queda bloqueado`() {
        consultas.crear = Resultado.Fallo(AppError.Duplicado)
        val vm = viewModel(citaId = "ci1").conTodo()

        vm.guardar()
        vm.guardar()

        assertTrue(vm.uiState.value.bloqueada)
        assertFalse(vm.uiState.value.puedeGuardar)
        assertEquals(listOf("crear"), consultas.pasos)
    }

    @Test
    fun `sin conexion al crear se puede reintentar`() {
        consultas.crear = Resultado.Fallo(AppError.SinConexion)
        val vm = viewModel().conTodo()

        vm.guardar()

        assertFalse(vm.uiState.value.bloqueada)
        assertFalse(vm.uiState.value.consultaCreada)
        assertTrue(vm.uiState.value.puedeGuardar)
        // Antes de crear la consulta las notas todavía se pueden corregir.
        vm.onNotas("Notas corregidas")
        assertEquals("Notas corregidas", vm.uiState.value.notas)
    }

    @Test
    fun `un examen se pide una sola vez y solo se quita lo que no se envio`() {
        val vm = viewModel()

        vm.agregarOrden(hemograma, "")
        vm.agregarOrden(hemograma, "otra vez")
        vm.agregarReceta(NuevaReceta("   "))
        vm.agregarReceta(NuevaReceta(" Paracetamol ", dosis = " 500 mg "))

        val estado = vm.uiState.value
        assertEquals(1, estado.ordenes.size)
        assertEquals(setOf("x1"), estado.examenesAgregados)
        assertEquals(NuevaReceta("Paracetamol", dosis = "500 mg"), estado.recetas.single().receta)

        vm.quitarOrden(estado.ordenes.single().id)
        vm.quitarReceta(estado.recetas.single().id)
        assertTrue(
            vm.uiState.value.ordenes
                .isEmpty(),
        )
        assertTrue(
            vm.uiState.value.recetas
                .isEmpty(),
        )
    }

    @Test
    fun `si el catalogo falla se puede reintentar`() {
        laboratorio.catalogo = Resultado.Fallo(AppError.SinConexion)
        val vm = viewModel()
        assertEquals(UiState.Error(AppError.SinConexion), vm.uiState.value.catalogo)

        laboratorio.catalogo = Resultado.Exito(emptyList())
        vm.cargarCatalogo()

        assertEquals(UiState.Vacio, vm.uiState.value.catalogo)
    }

    @Test
    fun `avisa si el medicamento coincide con una alergia registrada`() {
        assertEquals(listOf("Ibuprofeno"), alergiasCoincidentes("ibuprofeno 400 mg", "Penicilina, Ibuprofeno"))
        assertEquals(listOf("penicilina"), alergiasCoincidentes("Penicilina G", "penicilina y mariscos"))
        assertEquals(
            listOf("Ácido acetilsalicílico"),
            alergiasCoincidentes("acido acetilsalicilico", "Ácido acetilsalicílico"),
        )
        assertTrue(alergiasCoincidentes("Paracetamol", "Penicilina").isEmpty())
        assertTrue(alergiasCoincidentes("Paracetamol", "Ninguna").isEmpty())
        assertTrue(alergiasCoincidentes("Paracetamol", null).isEmpty())
    }
}
