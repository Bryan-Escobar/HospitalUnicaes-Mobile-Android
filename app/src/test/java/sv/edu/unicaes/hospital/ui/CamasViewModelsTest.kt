package sv.edu.unicaes.hospital.ui

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import sv.edu.unicaes.hospital.data.repository.HospitalizacionesRepository
import sv.edu.unicaes.hospital.data.repository.PacientesRepository
import sv.edu.unicaes.hospital.domain.model.Cama
import sv.edu.unicaes.hospital.domain.model.DatosPaciente
import sv.edu.unicaes.hospital.domain.model.DoctorDisponible
import sv.edu.unicaes.hospital.domain.model.EstadoCama
import sv.edu.unicaes.hospital.domain.model.Habitacion
import sv.edu.unicaes.hospital.domain.model.Hospitalizacion
import sv.edu.unicaes.hospital.domain.model.Ocupante
import sv.edu.unicaes.hospital.domain.model.Paciente
import sv.edu.unicaes.hospital.domain.model.Pagina
import sv.edu.unicaes.hospital.domain.model.TipoHabitacion
import sv.edu.unicaes.hospital.ui.recepcion.AltaViewModel
import sv.edu.unicaes.hospital.ui.recepcion.IngresoCamaViewModel
import sv.edu.unicaes.hospital.ui.recepcion.MapaCamasViewModel
import sv.edu.unicaes.hospital.ui.recepcion.TrasladoViewModel

/** Ingreso, traslado, alta y mapa de camas contra la API de HUNI-66 (HUNI-67). */
@OptIn(ExperimentalCoroutinesApi::class)
class CamasViewModelsTest {
    private val paciente = Paciente(id = "p1", nombres = "María", apellidos = "López", numeroExpediente = "EXP-1")
    private val doctor = DoctorDisponible("d1", "Ana Rivas", "Pediatría")
    private val hospitalizacion =
        Hospitalizacion(
            id = "h1",
            habitacion = "HAB-201",
            cama = "A",
            tipo = TipoHabitacion.SEMIPRIVADA,
            planta = 2,
            fechaIngreso = "2026-11-10T14:00:00.000Z",
            medicoTratante = "Ana Rivas",
        )

    private fun semiprivada(
        estadoA: EstadoCama,
        estadoB: EstadoCama = EstadoCama.DISPONIBLE,
    ) = Habitacion(
        id = "r1",
        codigo = "HAB-201",
        planta = 2,
        tipo = TipoHabitacion.SEMIPRIVADA,
        camas =
            listOf(
                Cama(
                    id = "c1",
                    codigo = "HAB-201-A",
                    letra = "A",
                    estado = estadoA,
                    ocupante =
                        if (estadoA == EstadoCama.OCUPADA) {
                            Ocupante("h1", "p9", "Luis Martínez", "L. Martínez", "2026-11-10T14:00:00.000Z")
                        } else {
                            null
                        },
                ),
                Cama(id = "c2", codigo = "HAB-201-B", letra = "B", estado = estadoB),
            ),
    )

    private val vip =
        Habitacion(
            id = "r2",
            codigo = "VIP-301",
            planta = 3,
            tipo = TipoHabitacion.VIP,
            camas = listOf(Cama(id = "c3", codigo = "VIP-301-A", letra = "", estado = EstadoCama.DISPONIBLE)),
        )

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

    private fun hospitalizaciones(activa: Hospitalizacion?) =
        object : HospitalizacionesRepository {
            override suspend fun activaDe(pacienteId: String): Resultado<Hospitalizacion?> = Resultado.Exito(activa)
        }

    private val ruta get() = SavedStateHandle(mapOf("pacienteId" to "p1"))

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    // Mapa

    @Test
    fun `el mapa muestra las camas con su estado y ocupante`() {
        val camas = FakeCamasRepository(habitaciones = Resultado.Exito(listOf(semiprivada(EstadoCama.OCUPADA), vip)))

        val estado = MapaCamasViewModel(camas).uiState.value

        val habitaciones = (estado as UiState.Exito).datos
        val ocupada = habitaciones.first().camas.first()
        assertEquals("L. Martínez", ocupada.ocupante)
        assertEquals("p9", ocupada.pacienteId)
        assertEquals("h1", ocupada.hospitalizacionId)
        assertFalse(ocupada.libre)
        assertTrue(
            habitaciones
                .last()
                .camas
                .single()
                .libre,
        )
    }

    @Test
    fun `el mapa se actualiza cuando otro dispositivo cambia la ocupacion`() =
        runTest {
            val camas = FakeCamasRepository(habitaciones = Resultado.Exito(listOf(semiprivada(EstadoCama.DISPONIBLE))))
            val viewModel = MapaCamasViewModel(camas)

            camas.habitaciones = Resultado.Exito(listOf(semiprivada(EstadoCama.OCUPADA)))
            camas.cambiosRealtime.emit(Unit)

            val cama =
                (viewModel.uiState.value as UiState.Exito)
                    .datos
                    .first()
                    .camas
                    .first()
            assertEquals(EstadoCama.OCUPADA, cama.estado)
        }

    @Test
    fun `si falla una recarga en vivo se conserva el ultimo mapa`() =
        runTest {
            val camas = FakeCamasRepository(habitaciones = Resultado.Exito(listOf(vip)))
            val viewModel = MapaCamasViewModel(camas)

            camas.habitaciones = Resultado.Fallo(AppError.SinConexion)
            camas.cambiosRealtime.emit(Unit)

            assertTrue(viewModel.uiState.value is UiState.Exito)
        }

    @Test
    fun `una cama en limpieza no es libre y se puede marcar disponible`() {
        val camas = FakeCamasRepository(habitaciones = Resultado.Exito(listOf(semiprivada(EstadoCama.LIMPIEZA))))
        val viewModel = MapaCamasViewModel(camas)
        val cama =
            (viewModel.uiState.value as UiState.Exito)
                .datos
                .first()
                .camas
                .first()

        assertFalse(cama.libre)
        viewModel.marcarDisponible(cama)

        assertEquals(listOf("c1"), camas.disponibles)
        assertNull(viewModel.aviso.value)
    }

    // Ingreso

    private fun ingreso(
        camas: FakeCamasRepository,
        activa: Hospitalizacion? = null,
    ) = IngresoCamaViewModel(ruta, FakePacientes(Resultado.Exito(paciente)), hospitalizaciones(activa), camas)

    @Test
    fun `el ingreso carga paciente, camas y medicos`() {
        val camas =
            FakeCamasRepository(
                habitaciones = Resultado.Exito(listOf(semiprivada(EstadoCama.DISPONIBLE), vip)),
                doctores = Resultado.Exito(listOf(doctor)),
            )

        val datos = (ingreso(camas).uiState.value.estado as UiState.Exito).datos

        assertEquals("María López", datos.paciente.nombre)
        assertEquals("EXP-1", datos.paciente.etiqueta)
        assertEquals(listOf(doctor), datos.doctores)
        assertEquals(2, datos.habitaciones.size)
        assertFalse(datos.yaHospitalizado)
    }

    @Test
    fun `sin medico tratante no se confirma el ingreso`() {
        val camas = FakeCamasRepository(habitaciones = Resultado.Exito(listOf(vip)))
        val viewModel = ingreso(camas)

        viewModel.confirmar("c3")

        assertTrue(camas.ingresos.isEmpty())
    }

    @Test
    fun `confirmar ingresa en la cama elegida con el medico y el motivo`() {
        val camas = FakeCamasRepository(habitaciones = Resultado.Exito(listOf(vip)))
        val viewModel = ingreso(camas)

        viewModel.elegirDoctor("d1")
        viewModel.cambiarMotivo("Observación")
        viewModel.confirmar("c3")

        assertEquals(listOf(FakeCamasRepository.Ingreso("p1", "d1", "c3", "Observación")), camas.ingresos)
        assertTrue(viewModel.uiState.value.terminado)
    }

    @Test
    fun `si otra persona gana la cama se avisa y se recargan las camas`() {
        val camas =
            FakeCamasRepository(
                habitaciones = Resultado.Exito(listOf(semiprivada(EstadoCama.DISPONIBLE))),
                resultadoIngreso =
                    Resultado.Fallo(AppError.Rechazada("La cama HAB-201-A ya está ocupada", "CAMA_OCUPADA")),
            )
        val viewModel = ingreso(camas)
        val lecturasAntes = camas.lecturasHabitaciones
        camas.habitaciones = Resultado.Exito(listOf(semiprivada(EstadoCama.OCUPADA)))

        viewModel.elegirDoctor("d1")
        viewModel.confirmar("c1")

        val estado = viewModel.uiState.value
        assertEquals("HAB-201 · A", estado.camaEnConflicto)
        assertNull(estado.error)
        assertFalse(estado.terminado)
        assertEquals(lecturasAntes + 1, camas.lecturasHabitaciones)
        val camaPerdida =
            (estado.estado as UiState.Exito)
                .datos.habitaciones
                .first()
                .camas
                .first()
        assertFalse(camaPerdida.libre)
    }

    @Test
    fun `un paciente que ya tiene cama no puede ingresar otra vez`() {
        val camas =
            FakeCamasRepository(
                habitaciones = Resultado.Exito(listOf(vip)),
                resultadoIngreso =
                    Resultado.Fallo(
                        AppError.Rechazada("El paciente ya está hospitalizado", "PACIENTE_YA_HOSPITALIZADO"),
                    ),
            )
        val viewModel = ingreso(camas, activa = hospitalizacion)

        assertTrue((viewModel.uiState.value.estado as UiState.Exito).datos.yaHospitalizado)

        // Si de todos modos la API lo rechaza (otro dispositivo lo ingresó), se muestra su mensaje.
        viewModel.elegirDoctor("d1")
        viewModel.confirmar("c3")
        assertEquals(
            AppError.Rechazada("El paciente ya está hospitalizado", "PACIENTE_YA_HOSPITALIZADO"),
            viewModel.uiState.value.error,
        )
        assertNull(viewModel.uiState.value.camaEnConflicto)
    }

    @Test
    fun `si falla la carga se muestra el error`() {
        val camas = FakeCamasRepository(habitaciones = Resultado.Fallo(AppError.SinConexion))

        assertEquals(UiState.Error(AppError.SinConexion), ingreso(camas).uiState.value.estado)
    }

    // Traslado

    @Test
    fun `el traslado mueve la hospitalizacion activa a la nueva cama`() {
        val camas = FakeCamasRepository(habitaciones = Resultado.Exito(listOf(semiprivada(EstadoCama.OCUPADA), vip)))
        val viewModel =
            TrasladoViewModel(ruta, FakePacientes(Resultado.Exito(paciente)), hospitalizaciones(hospitalizacion), camas)

        viewModel.confirmar("c3")

        assertEquals(listOf("h1" to "c3"), camas.traslados)
        assertTrue(viewModel.uiState.value.terminado)
    }

    @Test
    fun `sin hospitalizacion activa no hay traslado`() {
        val camas = FakeCamasRepository(habitaciones = Resultado.Exito(listOf(vip)))
        val viewModel =
            TrasladoViewModel(ruta, FakePacientes(Resultado.Exito(paciente)), hospitalizaciones(null), camas)

        assertEquals(UiState.Vacio, viewModel.uiState.value.estado)
    }

    @Test
    fun `si la cama destino se pierde el traslado avisa el conflicto`() {
        val camas =
            FakeCamasRepository(
                habitaciones = Resultado.Exito(listOf(vip)),
                resultadoTraslado = Resultado.Fallo(AppError.Rechazada("No disponible", "CAMA_NO_DISPONIBLE")),
            )
        val viewModel =
            TrasladoViewModel(ruta, FakePacientes(Resultado.Exito(paciente)), hospitalizaciones(hospitalizacion), camas)

        viewModel.confirmar("c3")

        assertEquals("VIP-301", viewModel.uiState.value.camaEnConflicto)
        assertFalse(viewModel.uiState.value.terminado)
    }

    // Alta

    @Test
    fun `dar de alta cierra la hospitalizacion activa`() {
        val camas = FakeCamasRepository()
        val viewModel =
            AltaViewModel(ruta, FakePacientes(Resultado.Exito(paciente)), hospitalizaciones(hospitalizacion), camas)

        val ficha = (viewModel.uiState.value.estado as UiState.Exito).datos
        assertEquals("HAB-201 · A", ficha.asignacion?.codigo)

        viewModel.darAlta()

        assertEquals(listOf("h1"), camas.altas)
        assertTrue(viewModel.uiState.value.terminado)
    }

    @Test
    fun `si el alta falla se muestra el error y no se cierra`() {
        val camas =
            FakeCamasRepository(
                resultadoAlta =
                    Resultado.Fallo(
                        AppError.Rechazada("Ya fue dado de alta", "HOSPITALIZACION_FINALIZADA"),
                    ),
            )
        val viewModel =
            AltaViewModel(ruta, FakePacientes(Resultado.Exito(paciente)), hospitalizaciones(hospitalizacion), camas)

        viewModel.darAlta()

        assertFalse(viewModel.uiState.value.terminado)
        assertEquals(
            AppError.Rechazada("Ya fue dado de alta", "HOSPITALIZACION_FINALIZADA"),
            viewModel.uiState.value.error,
        )
    }
}
