package sv.edu.unicaes.hospital.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import sv.edu.unicaes.hospital.core.AppError
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.domain.model.Cita
import sv.edu.unicaes.hospital.domain.model.EstadoCita
import sv.edu.unicaes.hospital.domain.model.TurnoMedico
import sv.edu.unicaes.hospital.ui.modelo.fechaHoraHospital
import sv.edu.unicaes.hospital.ui.modelo.isoConZona
import sv.edu.unicaes.hospital.ui.recepcion.CitasViewModel
import sv.edu.unicaes.hospital.ui.recepcion.DIAS_AGENDA

@OptIn(ExperimentalCoroutinesApi::class)
class CitasViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** Mañana a [hora] en la zona del hospital: los huecos de hoy dependen de la hora en que corre el test. */
    private fun manana(
        vm: CitasViewModel,
        hora: String,
    ) = isoConZona(
        fechaHoraHospital(
            vm.uiState.value.dias[1]
                .clave,
            hora,
        )!!,
    )

    @Test
    fun `muestra una semana de dias desde hoy`() {
        val vm = CitasViewModel(FakeCitasRepository())
        assertEquals(DIAS_AGENDA, vm.uiState.value.dias.size)
        assertEquals(0, vm.uiState.value.dia)
    }

    @Test
    fun `sin turnos ni citas el dia queda vacio`() {
        val vm = CitasViewModel(FakeCitasRepository())
        assertEquals(UiState.Vacio, vm.uiState.value.estado)
    }

    @Test
    fun `arma la agenda del dia elegido con citas y huecos`() {
        val repo = FakeCitasRepository()
        val vm = CitasViewModel(repo)
        repo.turnos =
            Resultado.Exito(
                listOf(TurnoMedico("d1", "Ana Rivas", "Alergista", manana(vm, "08:00"), manana(vm, "09:30"))),
            )
        repo.citas =
            Resultado.Exito(
                listOf(
                    Cita(
                        "c1",
                        manana(vm, "08:30"),
                        "Ana Rivas",
                        "Control",
                        EstadoCita.PROGRAMADA,
                        paciente = "Luis Martínez",
                        doctorId = "d1",
                    ),
                ),
            )

        vm.onDia(1)

        val agenda = (vm.uiState.value.estado as UiState.Exito).datos.single()
        assertEquals("08:00–09:30", agenda.horario)
        assertEquals("08:30", agenda.citas.single().hora)
        assertEquals(listOf("08:00", "09:00"), agenda.horasLibres)
    }

    @Test
    fun `si fallan los turnos se muestra el error`() {
        val vm = CitasViewModel(FakeCitasRepository(turnos = Resultado.Fallo(AppError.SinConexion)))
        assertEquals(UiState.Error(AppError.SinConexion), vm.uiState.value.estado)
    }
}
