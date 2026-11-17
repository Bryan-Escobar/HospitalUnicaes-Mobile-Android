package sv.edu.unicaes.hospital.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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
import sv.edu.unicaes.hospital.core.CODIGO_AMBULANCIA_NO_DISPONIBLE
import sv.edu.unicaes.hospital.core.Resultado
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.data.repository.AmbulanciaRepository
import sv.edu.unicaes.hospital.domain.model.Ambulancia
import sv.edu.unicaes.hospital.domain.model.EntradaBitacora
import sv.edu.unicaes.hospital.domain.model.EstadoAmbulancia
import sv.edu.unicaes.hospital.domain.model.FiltroBitacora
import sv.edu.unicaes.hospital.domain.model.SolicitudActiva
import sv.edu.unicaes.hospital.domain.model.SolicitudAmbulancia
import sv.edu.unicaes.hospital.ui.modelo.CierreServicioUi
import sv.edu.unicaes.hospital.ui.modelo.EstadoServicio
import sv.edu.unicaes.hospital.ui.recepcion.AmbulanciaViewModel
import sv.edu.unicaes.hospital.ui.recepcion.BitacoraViewModel
import sv.edu.unicaes.hospital.ui.recepcion.RangoBitacora

/** HUNI-71 · Despacho, conflicto 409, cierre y rechazo contra un repositorio falso. */
@OptIn(ExperimentalCoroutinesApi::class)
class AmbulanciaViewModelsTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun solicitud(
        estado: String = "pendiente",
        fechaDespacho: String? = null,
        fechaCierre: String? = null,
    ) = SolicitudAmbulancia(
        id = "s1",
        paciente = "Luis Martínez",
        direccion = "Col. Escalón",
        descripcion = "dolor de pecho",
        tipo = "cardiaca",
        estado = estado,
        fechaSolicitud = "2026-11-17T16:12:00.000Z",
        fechaDespacho = fechaDespacho,
        fechaCierre = fechaCierre,
    )

    private fun unidad(
        estado: EstadoAmbulancia = EstadoAmbulancia.DISPONIBLE,
        activa: SolicitudActiva? = null,
    ) = Ambulancia(id = "a1", placa = "AMB-001", estado = estado, solicitudActiva = activa)

    private class FakeAmbulancia(
        var estadoActual: Resultado<Ambulancia>,
        var pendientesActual: Resultado<List<SolicitudAmbulancia>>,
        var detalle: Resultado<SolicitudAmbulancia>,
        var alDespachar: Resultado<Unit> = Resultado.Exito(Unit),
        var alCerrar: Resultado<Unit> = Resultado.Exito(Unit),
        var alCancelar: Resultado<Unit> = Resultado.Exito(Unit),
        var alActualizar: Resultado<Unit> = Resultado.Exito(Unit),
        var bitacoraActual: Resultado<List<EntradaBitacora>> = Resultado.Exito(emptyList()),
    ) : AmbulanciaRepository {
        val despachadas = mutableListOf<String>()
        val destinos = mutableListOf<Pair<String, String>>()
        val cierres = mutableListOf<Pair<String, String?>>()
        val canceladas = mutableListOf<String>()
        val filtros = mutableListOf<FiltroBitacora>()

        override suspend fun estado(): Resultado<Ambulancia> = estadoActual

        override suspend fun pendientes(): Resultado<List<SolicitudAmbulancia>> = pendientesActual

        override suspend fun obtener(id: String): Resultado<SolicitudAmbulancia> = detalle

        override suspend fun despachar(id: String): Resultado<Unit> {
            despachadas += id
            return alDespachar
        }

        override suspend fun actualizarDestino(
            id: String,
            destino: String,
        ): Resultado<Unit> {
            destinos += id to destino
            return alActualizar
        }

        override suspend fun cerrar(
            id: String,
            descripcion: String?,
        ): Resultado<Unit> {
            cierres += id to descripcion
            return alCerrar
        }

        override suspend fun cancelar(id: String): Resultado<Unit> {
            canceladas += id
            return alCancelar
        }

        override suspend fun bitacora(filtro: FiltroBitacora): Resultado<List<EntradaBitacora>> {
            filtros += filtro
            return bitacoraActual
        }

        override fun cambios(): Flow<Unit> = emptyFlow()
    }

    private fun fakeDisponible() =
        FakeAmbulancia(
            estadoActual = Resultado.Exito(unidad()),
            pendientesActual = Resultado.Exito(listOf(solicitud())),
            detalle = Resultado.Exito(solicitud()),
        )

    @Test
    fun `carga el estado y las solicitudes pendientes`() {
        val datos = (AmbulanciaViewModel(fakeDisponible()).uiState.value.ambulancia as UiState.Exito).datos

        assertEquals(EstadoAmbulancia.DISPONIBLE, datos.estado)
        assertEquals("Luis Martínez", datos.solicitudes.single().paciente)
        assertEquals("Cardíaca", datos.solicitudes.single().tipo)
        assertEquals("Col. Escalón", datos.solicitudes.single().ubicacion)
        assertNull(datos.servicioActual)
    }

    @Test
    fun `despachar recarga y muestra el servicio en curso`() {
        val fake = fakeDisponible()
        val viewModel = AmbulanciaViewModel(fake)
        val pendiente = (viewModel.uiState.value.ambulancia as UiState.Exito).datos.solicitudes.single()
        fake.estadoActual =
            Resultado.Exito(
                unidad(
                    EstadoAmbulancia.EN_SERVICIO,
                    SolicitudActiva("s1", "urgente", "despachada", fechaDespacho = "2026-11-17T16:14:00.000Z"),
                ),
            )
        fake.pendientesActual = Resultado.Exito(emptyList())
        fake.detalle = Resultado.Exito(solicitud(estado = "despachada", fechaDespacho = "2026-11-17T16:14:00.000Z"))

        viewModel.despachar(pendiente)

        val datos = (viewModel.uiState.value.ambulancia as UiState.Exito).datos
        val servicio = checkNotNull(datos.servicioActual)
        assertEquals(listOf("s1"), fake.despachadas)
        assertEquals(EstadoAmbulancia.EN_SERVICIO, datos.estado)
        assertTrue(datos.solicitudes.isEmpty())
        assertTrue(servicio.contains("Luis Martínez"))
        assertTrue(servicio.contains("dolor de pecho"))
        assertFalse(viewModel.uiState.value.enviando)
    }

    @Test
    fun `un 409 deja el aviso de conflicto`() {
        val fake =
            fakeDisponible().also {
                it.alDespachar =
                    Resultado.Fallo(AppError.Rechazada("No disponible", CODIGO_AMBULANCIA_NO_DISPONIBLE))
            }
        val viewModel = AmbulanciaViewModel(fake)
        val pendiente = (viewModel.uiState.value.ambulancia as UiState.Exito).datos.solicitudes.single()
        fake.estadoActual =
            Resultado.Exito(unidad(EstadoAmbulancia.EN_SERVICIO, SolicitudActiva("s1", "urgente", "despachada")))

        viewModel.despachar(pendiente)

        assertTrue(viewModel.uiState.value.conflicto)
        assertNull(viewModel.uiState.value.error)
        val datos = (viewModel.uiState.value.ambulancia as UiState.Exito).datos
        assertEquals(EstadoAmbulancia.EN_SERVICIO, datos.estado)
        assertFalse(datos.estado.puedeDespachar)
    }

    @Test
    fun `un 409 con la unidad disponible no bloquea el despacho`() {
        val fake =
            fakeDisponible().also {
                it.alDespachar =
                    Resultado.Fallo(
                        AppError.Rechazada(
                            "la ambulancia ya fue despachada",
                            CODIGO_AMBULANCIA_NO_DISPONIBLE,
                        ),
                    )
            }
        val viewModel = AmbulanciaViewModel(fake)
        val pendiente = (viewModel.uiState.value.ambulancia as UiState.Exito).datos.solicitudes.single()

        viewModel.despachar(pendiente)

        assertFalse(viewModel.uiState.value.conflicto)
        val error = viewModel.uiState.value.error as AppError.Rechazada
        assertEquals("la ambulancia ya fue despachada", error.detalle)
        val datos = (viewModel.uiState.value.ambulancia as UiState.Exito).datos
        assertEquals(EstadoAmbulancia.DISPONIBLE, datos.estado)
        assertTrue(datos.estado.puedeDespachar)
    }

    @Test
    fun `cerrar manda el destino aparte y el resto en la descripcion`() {
        val activa = SolicitudActiva("s1", "urgente", "despachada")
        val fake =
            FakeAmbulancia(
                estadoActual = Resultado.Exito(unidad(EstadoAmbulancia.EN_SERVICIO, activa)),
                pendientesActual = Resultado.Exito(emptyList()),
                detalle = Resultado.Exito(solicitud(estado = "despachada")),
            )
        val viewModel = AmbulanciaViewModel(fake)

        viewModel.cerrar(CierreServicioUi("Emergencias", "paciente estable"))

        assertEquals("s1" to "Emergencias", fake.destinos.single())
        assertEquals("paciente estable", fake.cierres.single().second)
    }

    @Test
    fun `cerrar sin destino no actualiza la solicitud`() {
        val fake =
            FakeAmbulancia(
                estadoActual =
                    Resultado.Exito(
                        unidad(EstadoAmbulancia.EN_SERVICIO, SolicitudActiva("s1", "urgente", "despachada")),
                    ),
                pendientesActual = Resultado.Exito(emptyList()),
                detalle = Resultado.Exito(solicitud(estado = "despachada")),
            )
        val viewModel = AmbulanciaViewModel(fake)

        viewModel.cerrar(CierreServicioUi("  ", "listo"))

        assertTrue(fake.destinos.isEmpty())
        assertEquals("listo", fake.cierres.single().second)
    }

    @Test
    fun `rechazar cancela la solicitud y la quita de la lista`() {
        val fake = fakeDisponible()
        val viewModel = AmbulanciaViewModel(fake)
        val pendiente = (viewModel.uiState.value.ambulancia as UiState.Exito).datos.solicitudes.single()
        fake.pendientesActual = Resultado.Exito(emptyList())

        viewModel.rechazar(pendiente)

        assertEquals(listOf("s1"), fake.canceladas)
        assertTrue((viewModel.uiState.value.ambulancia as UiState.Exito).datos.solicitudes.isEmpty())
    }

    @Test
    fun `la bitacora pinta el servicio elegido y filtra por hoy`() {
        val entrada =
            EntradaBitacora(
                id = "b1",
                estadoAnterior = EstadoAmbulancia.EN_SERVICIO,
                estadoNuevo = EstadoAmbulancia.DISPONIBLE,
                descripcion = "paciente estable",
                registradoPor = "Karla",
                solicitud =
                    solicitud(
                        estado = "finalizada",
                        fechaDespacho = "2026-11-17T16:14:00.000Z",
                        fechaCierre = "2026-11-17T17:02:00.000Z",
                    ).copy(destino = "Emergencias"),
                creadaEn = "2026-11-17T17:02:00.000Z",
            )
        val fake =
            FakeAmbulancia(
                estadoActual = Resultado.Exito(unidad()),
                pendientesActual = Resultado.Exito(emptyList()),
                detalle = Resultado.Exito(solicitud()),
                bitacoraActual = Resultado.Exito(listOf(entrada)),
            )
        val viewModel = BitacoraViewModel(fake)
        val servicios = (viewModel.uiState.value.servicios as UiState.Exito).datos

        assertEquals(EstadoServicio.FINALIZADO, servicios.single().estado)
        assertEquals("48 min", servicios.single().duracion)
        viewModel.seleccionar("b1")
        assertEquals(
            "Karla",
            viewModel.uiState.value.detalle
                ?.despacho,
        )
        assertEquals(
            "Emergencias",
            viewModel.uiState.value.detalle
                ?.destino,
        )
        assertEquals(
            "paciente estable",
            viewModel.uiState.value.detalle
                ?.observaciones,
        )

        viewModel.elegirRango(RangoBitacora.HOY)
        val filtro = fake.filtros.last()
        assertEquals(filtro.desde, filtro.hasta)
        assertTrue(filtro.desde != null)
    }
}
