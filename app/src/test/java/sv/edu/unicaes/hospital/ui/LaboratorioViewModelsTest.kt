package sv.edu.unicaes.hospital.ui

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
import sv.edu.unicaes.hospital.data.local.ArchivosLocales
import sv.edu.unicaes.hospital.data.repository.LaboratorioRepository
import sv.edu.unicaes.hospital.domain.model.ArchivoAdjunto
import sv.edu.unicaes.hospital.domain.model.DatosResultado
import sv.edu.unicaes.hospital.domain.model.EstadoLaboratorio
import sv.edu.unicaes.hospital.domain.model.ExamenOrden
import sv.edu.unicaes.hospital.domain.model.OrdenLaboratorio
import sv.edu.unicaes.hospital.domain.model.Pagina
import sv.edu.unicaes.hospital.domain.model.PrioridadLaboratorio
import sv.edu.unicaes.hospital.ui.recepcion.CargarResultadoViewModel
import sv.edu.unicaes.hospital.ui.recepcion.ColaLaboratorioViewModel
import sv.edu.unicaes.hospital.ui.recepcion.DetalleOrdenViewModel
import sv.edu.unicaes.hospital.ui.recepcion.ESPERA_AVISOS_MS
import sv.edu.unicaes.hospital.ui.recepcion.ProblemaArchivo
import sv.edu.unicaes.hospital.ui.recepcion.tamanoLegible
import java.io.InputStream

/** HUNI-63 · Laboratorio de Recepción: cola por estado, avance de exámenes y carga de resultados. */
@OptIn(ExperimentalCoroutinesApi::class)
class LaboratorioViewModelsTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun examen(
        id: String,
        estado: EstadoLaboratorio,
    ) = ExamenOrden(id = id, nombre = "Examen $id", estado = estado)

    private fun orden(
        id: String,
        estado: EstadoLaboratorio = EstadoLaboratorio.PENDIENTE,
        examenes: List<ExamenOrden> = listOf(examen("e-$id", estado)),
    ) = OrdenLaboratorio(
        id = id,
        fechaSolicitud = "2026-11-09T16:41:00Z",
        prioridad = PrioridadLaboratorio.NORMAL,
        estado = estado,
        pacienteId = "p1",
        paciente = "Luis Martínez",
        doctor = "Carlos Molina",
        examenes = examenes,
    )

    private class FakeLaboratorio(
        var paginas: (EstadoLaboratorio?, Int) -> Resultado<Pagina<OrdenLaboratorio>> = { _, _ ->
            Resultado.Exito(Pagina(emptyList(), 1, 1))
        },
        var detalle: Resultado<OrdenLaboratorio> = Resultado.Fallo(AppError.NoEncontrado),
        var iniciar: Resultado<ExamenOrden> = Resultado.Fallo(AppError.Validacion),
        var registrar: Resultado<ExamenOrden> = Resultado.Fallo(AppError.Validacion),
        val conteos: Map<EstadoLaboratorio, Int> = mapOf(EstadoLaboratorio.PENDIENTE to 3),
    ) : LaboratorioRepository {
        val avisos = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val listados = mutableListOf<Pair<EstadoLaboratorio?, Int>>()
        var obtenciones = 0
        var registrado: Pair<DatosResultado, ArchivoAdjunto?>? = null

        override suspend fun listar(
            estado: EstadoLaboratorio?,
            pagina: Int,
        ): Resultado<Pagina<OrdenLaboratorio>> {
            listados += estado to pagina
            return paginas(estado, pagina)
        }

        override suspend fun contar(estado: EstadoLaboratorio): Resultado<Int> = Resultado.Exito(conteos[estado] ?: 0)

        override suspend fun obtener(id: String): Resultado<OrdenLaboratorio> {
            obtenciones++
            return detalle
        }

        override suspend fun iniciarExamen(
            ordenId: String,
            examenId: String,
        ) = iniciar

        override suspend fun registrarResultado(
            ordenId: String,
            examenId: String,
            datos: DatosResultado,
            archivo: ArchivoAdjunto?,
        ): Resultado<ExamenOrden> {
            registrado = datos to archivo
            return registrar
        }

        override fun cambios(): Flow<Unit> = avisos
    }

    private class FakeArchivos(
        val archivo: ArchivoAdjunto?,
    ) : ArchivosLocales {
        override suspend fun describir(uri: String) = archivo

        override fun abrir(uri: String): InputStream? = null
    }

    private fun <T> UiState<T>.datos(): T = (this as UiState.Exito).datos

    // Cola ----------------------------------------------------------------------------------

    @Test
    fun `la cola empieza en Pendiente con los conteos por estado`() {
        val repo = FakeLaboratorio(paginas = { _, _ -> Resultado.Exito(Pagina(listOf(orden("o1")), 1, 1)) })

        val estado = ColaLaboratorioViewModel(repo).uiState.value

        assertEquals(EstadoLaboratorio.PENDIENTE, estado.filtro)
        assertEquals(listOf(EstadoLaboratorio.PENDIENTE to 1), repo.listados)
        assertEquals(
            "o1",
            estado.ordenes
                .datos()
                .single()
                .id,
        )
        assertEquals(3, estado.conteos[EstadoLaboratorio.PENDIENTE])
        assertEquals(0, estado.conteos[EstadoLaboratorio.COMPLETADO])
    }

    @Test
    fun `cambiar el filtro pide las ordenes de ese estado y sin resultados queda vacio`() {
        val repo = FakeLaboratorio()
        val viewModel = ColaLaboratorioViewModel(repo)

        viewModel.elegirFiltro(EstadoLaboratorio.EN_PROCESO)

        assertEquals(EstadoLaboratorio.EN_PROCESO to 1, repo.listados.last())
        assertEquals(UiState.Vacio, viewModel.uiState.value.ordenes)
    }

    @Test
    fun `un error al cargar se muestra, pero un refresco fallido conserva la lista`() {
        var falla = true
        val repo =
            FakeLaboratorio(paginas = { _, _ ->
                if (falla) Resultado.Fallo(AppError.SinConexion) else Resultado.Exito(Pagina(listOf(orden("o1")), 1, 1))
            })
        val viewModel = ColaLaboratorioViewModel(repo)
        assertEquals(UiState.Error(AppError.SinConexion), viewModel.uiState.value.ordenes)

        falla = false
        viewModel.cargar()
        falla = true
        viewModel.refrescar()

        assertEquals(
            "o1",
            viewModel.uiState.value.ordenes
                .datos()
                .single()
                .id,
        )
        assertFalse(viewModel.uiState.value.refrescando)
    }

    @Test
    fun `cargar mas agrega la pagina siguiente sin repetir ordenes`() {
        val repo =
            FakeLaboratorio(paginas = { _, pagina ->
                if (pagina == 1) {
                    Resultado.Exito(Pagina(listOf(orden("o1"), orden("o2")), 1, 2))
                } else {
                    Resultado.Exito(Pagina(listOf(orden("o2"), orden("o3")), 2, 2))
                }
            })
        val viewModel = ColaLaboratorioViewModel(repo)
        assertTrue(viewModel.uiState.value.hayMas)

        viewModel.cargarMas()

        assertEquals(
            listOf("o1", "o2", "o3"),
            viewModel.uiState.value.ordenes
                .datos()
                .map { it.id },
        )
        assertFalse(viewModel.uiState.value.hayMas)
    }

    @Test
    fun `un aviso de Realtime recarga la cola`() {
        val repo = FakeLaboratorio()
        ColaLaboratorioViewModel(repo)
        val antes = repo.listados.size

        repo.avisos.tryEmit(Unit)
        dispatcher.scheduler.advanceTimeBy(ESPERA_AVISOS_MS + 1)

        assertEquals(antes + 1, repo.listados.size)
    }

    // Detalle -------------------------------------------------------------------------------

    private fun detalle(repo: FakeLaboratorio) = DetalleOrdenViewModel(SavedStateHandle(mapOf("ordenId" to "o1")), repo)

    @Test
    fun `iniciar un examen lo pasa a En Proceso y vuelve a pedir la orden`() {
        val repo = FakeLaboratorio(detalle = Resultado.Exito(orden("o1")))
        val viewModel = detalle(repo)
        val enProceso = examen("e-o1", EstadoLaboratorio.EN_PROCESO)
        repo.iniciar = Resultado.Exito(enProceso)
        repo.detalle = Resultado.Exito(orden("o1", EstadoLaboratorio.EN_PROCESO, listOf(enProceso)))

        viewModel.iniciarExamen("e-o1")

        val estado = viewModel.uiState.value
        assertEquals(
            EstadoLaboratorio.EN_PROCESO,
            estado.orden
                .datos()
                .examenes
                .single()
                .estado,
        )
        assertEquals(EstadoLaboratorio.EN_PROCESO, estado.orden.datos().estado)
        assertTrue(estado.huboCambios)
        assertNull(estado.iniciando)
        assertEquals(2, repo.obtenciones)
    }

    @Test
    fun `si otro usuario ya inicio el examen se muestra el aviso y la orden actual`() {
        val repo = FakeLaboratorio(detalle = Resultado.Exito(orden("o1")))
        val viewModel = detalle(repo)
        val aviso = AppError.Rechazada("Otro usuario ya inició este examen; actualiza la orden")
        repo.iniciar = Resultado.Fallo(aviso)
        repo.detalle = Resultado.Exito(orden("o1", EstadoLaboratorio.EN_PROCESO))

        viewModel.iniciarExamen("e-o1")

        assertEquals(aviso, viewModel.uiState.value.errorAccion)
        assertEquals(
            EstadoLaboratorio.EN_PROCESO,
            viewModel.uiState.value.orden
                .datos()
                .estado,
        )
        assertFalse(viewModel.uiState.value.huboCambios)

        viewModel.descartarError()
        assertNull(viewModel.uiState.value.errorAccion)
    }

    // Resultado -----------------------------------------------------------------------------

    private val pdf = ArchivoAdjunto("content://pdf", "r.pdf", ArchivoAdjunto.MIME_PDF, 300_000)

    private fun formulario(
        repo: FakeLaboratorio,
        archivo: ArchivoAdjunto? = pdf,
    ) = CargarResultadoViewModel(
        SavedStateHandle(mapOf("ordenId" to "o1", "examenId" to "e-o1")),
        repo,
        FakeArchivos(archivo),
    )

    private fun repoEnProceso() = FakeLaboratorio(detalle = Resultado.Exito(orden("o1", EstadoLaboratorio.EN_PROCESO)))

    @Test
    fun `sin texto ni archivo no se puede guardar`() {
        val viewModel = formulario(repoEnProceso())

        assertTrue(viewModel.uiState.value.examenEnProceso)
        assertFalse(viewModel.uiState.value.puedeGuardar)

        viewModel.cambiarValor("13.8")
        assertTrue(viewModel.uiState.value.puedeGuardar)
    }

    @Test
    fun `basta con adjuntar un PDF o una imagen`() {
        val viewModel = formulario(repoEnProceso())

        viewModel.adjuntar("content://pdf")

        assertEquals(pdf, viewModel.uiState.value.archivo)
        assertTrue(viewModel.uiState.value.puedeGuardar)
        viewModel.quitarArchivo()
        assertFalse(viewModel.uiState.value.puedeGuardar)
    }

    @Test
    fun `rechaza archivos de otro tipo, de mas de 10 MB o ilegibles`() {
        val casos =
            mapOf(
                pdf.copy(mime = "image/heic") to ProblemaArchivo.TIPO,
                pdf.copy(tamanoBytes = ArchivoAdjunto.MAXIMO_BYTES + 1) to ProblemaArchivo.TAMANO,
                null to ProblemaArchivo.ILEGIBLE,
            )
        casos.forEach { (archivo, problema) ->
            val viewModel = formulario(repoEnProceso(), archivo)

            viewModel.adjuntar("content://x")

            assertEquals(problema, viewModel.uiState.value.problemaArchivo)
            assertNull(viewModel.uiState.value.archivo)
        }
    }

    @Test
    fun `guardar envia el texto y el archivo y avisa que termino`() {
        val repo = repoEnProceso()
        repo.registrar = Resultado.Exito(examen("e-o1", EstadoLaboratorio.COMPLETADO))
        val viewModel = formulario(repo)
        viewModel.cambiarValor("13.8")
        viewModel.cambiarUnidad("g/dL")
        viewModel.adjuntar("content://pdf")

        viewModel.guardar()

        assertTrue(viewModel.uiState.value.guardado)
        assertEquals(
            DatosResultado(valor = "13.8", unidad = "g/dL", rangoReferencia = "", observaciones = ""),
            repo.registrado?.first,
        )
        assertEquals(pdf, repo.registrado?.second)
    }

    @Test
    fun `un error al guardar se muestra y permite reintentar`() {
        val repo = repoEnProceso()
        repo.registrar = Resultado.Fallo(AppError.Rechazada("Otro usuario ya registró el resultado de este examen"))
        val viewModel = formulario(repo)
        viewModel.cambiarValor("13.8")

        viewModel.guardar()

        assertFalse(viewModel.uiState.value.guardado)
        assertFalse(viewModel.uiState.value.guardando)
        assertTrue(viewModel.uiState.value.error is AppError.Rechazada)
        assertTrue(viewModel.uiState.value.puedeGuardar)
    }

    @Test
    fun `un examen que ya no esta En Proceso no recibe resultado`() {
        val repo = FakeLaboratorio(detalle = Resultado.Exito(orden("o1", EstadoLaboratorio.COMPLETADO)))
        val viewModel = formulario(repo)
        viewModel.cambiarValor("13.8")

        viewModel.guardar()

        assertFalse(viewModel.uiState.value.examenEnProceso)
        assertFalse(viewModel.uiState.value.puedeGuardar)
        assertNull(repo.registrado)
    }

    @Test
    fun `un examen que no es de la orden muestra no encontrado`() {
        val repo =
            FakeLaboratorio(
                detalle = Resultado.Exito(orden("o1", examenes = listOf(examen("otro", EstadoLaboratorio.EN_PROCESO)))),
            )

        assertEquals(UiState.Error(AppError.NoEncontrado), formulario(repo).uiState.value.carga)
    }

    @Test
    fun `tamano legible del archivo`() {
        assertEquals("900 B", tamanoLegible(900))
        assertEquals("310 KB", tamanoLegible(317_440))
        assertEquals("1.5 MB", tamanoLegible(1_572_864))
        assertEquals("", tamanoLegible(-1))
    }
}
