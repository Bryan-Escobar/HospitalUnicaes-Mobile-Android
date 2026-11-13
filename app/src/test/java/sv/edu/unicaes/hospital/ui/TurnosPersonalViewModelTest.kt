package sv.edu.unicaes.hospital.ui

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
import sv.edu.unicaes.hospital.domain.model.TipoTurno
import sv.edu.unicaes.hospital.ui.modelo.AreaTurnosUi
import sv.edu.unicaes.hospital.ui.modelo.semanaDe
import sv.edu.unicaes.hospital.ui.recepcion.TurnosPersonalViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class TurnosPersonalViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val semana = semanaDe(0)
    private val lunes = semana.first()

    /** VM con los [turnos] de la semana y el lunes elegido. */
    private fun vmEnLunes(repo: FakeTurnosRepository): TurnosPersonalViewModel =
        TurnosPersonalViewModel(repo).also { it.onDia(0) }

    @Test
    fun `pide la semana de lunes a domingo`() {
        val repo = FakeTurnosRepository()
        TurnosPersonalViewModel(repo)
        assertEquals(semana.first() to semana.last(), repo.consultas.single())
    }

    @Test
    fun `sin turnos el dia queda vacio`() {
        val vm = vmEnLunes(FakeTurnosRepository())
        assertEquals(UiState.Vacio, vm.uiState.value.estado)
    }

    @Test
    fun `el error de carga se muestra con reintentar`() {
        val vm = vmEnLunes(FakeTurnosRepository(turnos = Resultado.Fallo(AppError.SinConexion)))
        assertEquals(UiState.Error(AppError.SinConexion), vm.uiState.value.estado)
    }

    @Test
    fun `agrupa los turnos del dia por area y tipo de personal`() {
        val repo =
            FakeTurnosRepository(
                turnos =
                    Resultado.Exito(
                        listOf(
                            turno("1", lunes, area = "Emergencia", tipoPersonal = "Enfermero/a", persona = "José"),
                            turno("2", lunes, area = "Consulta externa", persona = "Roberto"),
                            turno("3", lunes, area = "Emergencia", tipoPersonal = "Doctor", persona = "Fernando"),
                            turno("4", semana[1], area = "Pediatría"),
                        ),
                    ),
            )
        val vm = vmEnLunes(repo)

        val areas = (vm.uiState.value.estado as UiState.Exito<List<AreaTurnosUi>>).datos
        assertEquals(listOf("Consulta externa", "Emergencia"), areas.map { it.area })
        assertEquals(listOf("Fernando", "José"), areas[1].turnos.map { it.persona })
        assertEquals(listOf("Doctor", "Enfermería"), vm.uiState.value.tipos)
    }

    @Test
    fun `filtra por tipo de personal`() {
        val repo =
            FakeTurnosRepository(
                turnos =
                    Resultado.Exito(
                        listOf(
                            turno("1", lunes, tipoPersonal = "Enfermero/a"),
                            turno("2", lunes, tipoPersonal = "Doctor"),
                        ),
                    ),
            )
        val vm = vmEnLunes(repo)
        vm.onTipo(2)

        val areas = (vm.uiState.value.estado as UiState.Exito<List<AreaTurnosUi>>).datos
        assertEquals(listOf("1"), areas.flatMap { it.turnos }.map { it.id })
    }

    @Test
    fun `cambiar de semana vuelve a pedir los turnos`() {
        val repo = FakeTurnosRepository()
        val vm = TurnosPersonalViewModel(repo)
        vm.onSemana(1)
        val siguiente = semanaDe(1)
        assertEquals(siguiente.first() to siguiente.last(), repo.consultas.last())
        assertEquals(1, vm.uiState.value.semana)
    }

    @Test
    fun `registra un turno nuevo con el dia elegido`() {
        val repo = FakeTurnosRepository()
        val vm = vmEnLunes(repo)
        vm.abrirNuevo()
        vm.onPersona("p-9")
        vm.onArea(" Recepción ")
        vm.onTipoTurno(TipoTurno.NOCHE)
        assertTrue(
            vm.uiState.value.hoja!!
                .puedeGuardar,
        )

        vm.guardar()

        val datos = repo.creados.single()
        assertEquals("p-9", datos.personalId)
        assertEquals("Recepción", datos.area)
        assertEquals(lunes, datos.fecha)
        assertEquals("19:00" to "07:00", datos.horaInicio to datos.horaFin)
        assertNull(vm.uiState.value.hoja)
        assertEquals(2, repo.consultas.size)
    }

    @Test
    fun `no deja guardar sin persona o con horas invalidas`() {
        val vm = vmEnLunes(FakeTurnosRepository())
        vm.abrirNuevo()
        vm.onArea("Emergencia")
        assertFalse(
            vm.uiState.value.hoja!!
                .puedeGuardar,
        )
        vm.onPersona("p-1")
        vm.onFin("25:00")
        assertFalse(
            vm.uiState.value.hoja!!
                .puedeGuardar,
        )
        vm.onFin("07:00")
        assertFalse(
            "inicio y fin iguales",
            vm.uiState.value.hoja!!
                .puedeGuardar,
        )
    }

    @Test
    fun `el solape deja la hoja abierta con el mensaje de la base`() {
        val solape = AppError.Rechazada("El empleado ya tiene un turno el 22/10/2026 de 14:00 a 22:00")
        val repo = FakeTurnosRepository(escritura = Resultado.Fallo(solape))
        val vm = vmEnLunes(repo)
        vm.abrirNuevo()
        vm.onPersona("p-1")
        vm.onArea("Emergencia")

        vm.guardar()

        val hoja = vm.uiState.value.hoja!!
        assertEquals(solape, hoja.error)
        assertFalse(hoja.guardando)
        vm.onInicio("08:00")
        assertNull(
            "al corregir se limpia el aviso",
            vm.uiState.value.hoja!!
                .error,
        )
    }

    @Test
    fun `ajusta un turno existente sin cambiar sus horas al elegir el tipo`() {
        val repo =
            FakeTurnosRepository(turnos = Resultado.Exito(listOf(turno("1", lunes, inicio = "08:00", fin = "14:00"))))
        val vm = vmEnLunes(repo)
        vm.abrirAjuste("1")
        vm.onTipoTurno(TipoTurno.GUARDIA)
        vm.onFin("16:00")

        vm.guardar()

        val (id, datos) = repo.actualizados.single()
        assertEquals("1", id)
        assertEquals("08:00" to "16:00", datos.horaInicio to datos.horaFin)
        assertEquals(TipoTurno.GUARDIA, datos.tipoTurno)
    }
}
