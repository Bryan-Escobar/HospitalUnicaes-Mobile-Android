package sv.edu.unicaes.hospital.ui.doctor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.ui.components.HomeConPestanas
import sv.edu.unicaes.hospital.ui.components.Pestana
import sv.edu.unicaes.hospital.ui.components.SincronizarPestana
import sv.edu.unicaes.hospital.ui.components.irAPestana
import sv.edu.unicaes.hospital.ui.perfil.PerfilScreen
import sv.edu.unicaes.hospital.ui.recepcion.RecargarAlVolver
import sv.edu.unicaes.hospital.ui.recepcion.avisarGuardado
import sv.edu.unicaes.hospital.ui.theme.HospitalUnicaesTheme
import sv.edu.unicaes.hospital.ui.turnos.MisTurnosViewModel

// Rutas internas del Doctor (handoff §11.2 · Doctor).

@Serializable
data object AgendaRuta

@Serializable
data object MisPacientesRuta

@Serializable
data object MisTurnosRuta

@Serializable
data object PerfilDoctorRuta

@Serializable
data class DetalleCitaRuta(
    val citaId: String,
)

@Serializable
data class ExpedienteRuta(
    val pacienteId: String,
)

/** Desde el expediente va sin cita; desde la agenda, con la cita que se atiende y su hora (`HH:mm`). */
@Serializable
data class NuevaConsultaRuta(
    val pacienteId: String,
    val citaId: String? = null,
    val citaHora: String? = null,
)

// Doctor · Alergista · Médico de emergencia (AND-D1 a AND-D7)
private val pestanasDoctor =
    listOf(
        Pestana(R.string.tab_agenda, R.drawable.ic_calendar, AgendaRuta),
        Pestana(R.string.tab_pacientes, R.drawable.ic_group, MisPacientesRuta),
        Pestana(R.string.tab_turnos, R.drawable.ic_schedule, MisTurnosRuta),
        Pestana(R.string.tab_perfil, R.drawable.ic_account_box, PerfilDoctorRuta),
    )

@Composable
fun DoctorHomeScreen(onCerrarSesion: () -> Unit) {
    val navController = rememberNavController()
    var seleccionada by rememberSaveable { mutableIntStateOf(0) }

    SincronizarPestana(navController, pestanasDoctor) { seleccionada = it }
    HomeConPestanas(
        pestanas = pestanasDoctor,
        seleccionada = seleccionada,
        onSeleccion = {
            seleccionada = it
            navController.irAPestana(pestanasDoctor[it].ruta)
        },
    ) { modifier ->
        DoctorNavHost(navController = navController, onCerrarSesion = onCerrarSesion, modifier = modifier)
    }
}

/**
 * Grafo del Doctor. Mis pacientes, Expediente y Nueva consulta usan la API (HUNI-61); la agenda y el detalle
 * de cita todavía muestran [sv.edu.unicaes.hospital.ui.modelo.DatosMuestra].
 */
@Composable
private fun DoctorNavHost(
    navController: NavHostController,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val volver: () -> Unit = { navController.popBackStack() }

    NavHost(navController = navController, startDestination = AgendaRuta, modifier = modifier) {
        composable<AgendaRuta> {
            val viewModel: AgendaViewModel = hiltViewModel()
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            AgendaScreen(
                estado = estado,
                onCita = { navController.navigate(DetalleCitaRuta(it)) },
                onReintentar = viewModel::cargar,
            )
        }
        composable<DetalleCitaRuta> {
            val viewModel: DetalleCitaViewModel = hiltViewModel()
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            val detalle = (estado as? UiState.Exito)?.datos
            DetalleCitaScreen(
                estado = estado,
                onVolver = volver,
                onVerExpediente = { detalle?.let { navController.navigate(ExpedienteRuta(it.pacienteId)) } },
                onIniciarConsulta = {
                    detalle?.let { navController.navigate(NuevaConsultaRuta(it.pacienteId, it.cita.id, it.cita.hora)) }
                },
                onReintentar = viewModel::cargar,
            )
        }
        composable<NuevaConsultaRuta> {
            val viewModel: NuevaConsultaViewModel = hiltViewModel()
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(estado.guardada) {
                if (estado.guardada) {
                    avisarGuardado(navController)
                    volver()
                }
            }
            NuevaConsultaScreen(
                estado = estado,
                onCerrar = volver,
                onReintentar = viewModel::cargar,
                onDiagnostico = viewModel::onDiagnostico,
                onNotas = viewModel::onNotas,
                onAgregarReceta = viewModel::agregarReceta,
                onQuitarReceta = viewModel::quitarReceta,
                onAgregarOrden = viewModel::agregarOrden,
                onQuitarOrden = viewModel::quitarOrden,
                onReintentarCatalogo = viewModel::cargarCatalogo,
                onGuardar = viewModel::guardar,
            )
        }
        composable<MisPacientesRuta> {
            val viewModel: MisPacientesViewModel = hiltViewModel()
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            MisPacientesScreen(
                estado = estado,
                onPaciente = { navController.navigate(ExpedienteRuta(it.id)) },
                onReintentar = viewModel::cargar,
            )
        }
        // Al volver de una consulta guardada se recarga para ver la consulta y sus órdenes en Pendiente.
        composable<ExpedienteRuta> { entrada ->
            val viewModel: ExpedienteViewModel = hiltViewModel()
            RecargarAlVolver(entrada, viewModel::cargar)
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            ExpedienteScreen(
                estado = estado,
                onVolver = volver,
                onNuevaConsulta = { navController.navigate(NuevaConsultaRuta(viewModel.pacienteId)) },
                onReintentar = viewModel::cargar,
            )
        }
        composable<MisTurnosRuta> {
            val viewModel: MisTurnosViewModel = hiltViewModel()
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            MisTurnosScreen(estado = estado, onReintentar = viewModel::cargar)
        }
        composable<PerfilDoctorRuta> {
            PerfilScreen(onCerrarSesion = onCerrarSesion)
        }
    }
}

@PreviewScreenSizes
@Composable
private fun DoctorHomePreview() {
    HospitalUnicaesTheme { DoctorHomeScreen(onCerrarSesion = {}) }
}
