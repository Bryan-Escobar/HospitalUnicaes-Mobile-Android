package sv.edu.unicaes.hospital.ui.doctor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.ui.components.HomeConPestanas
import sv.edu.unicaes.hospital.ui.components.Pestana
import sv.edu.unicaes.hospital.ui.components.irAPestana
import sv.edu.unicaes.hospital.ui.modelo.DatosMuestra
import sv.edu.unicaes.hospital.ui.perfil.PerfilScreen
import sv.edu.unicaes.hospital.ui.theme.HospitalUnicaesTheme

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

@Serializable
data class NuevaConsultaRuta(
    val citaId: String,
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

/** Grafo del Doctor. Los estados salen de [DatosMuestra] hasta conectar los ViewModels. */
@Composable
private fun DoctorNavHost(
    navController: NavHostController,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val volver: () -> Unit = { navController.popBackStack() }

    NavHost(navController = navController, startDestination = AgendaRuta, modifier = modifier) {
        composable<AgendaRuta> {
            AgendaScreen(
                estado = UiState.Exito(DatosMuestra.agendaDoctor),
                onCita = { navController.navigate(DetalleCitaRuta(it)) },
            )
        }
        composable<DetalleCitaRuta> { entrada ->
            val id = entrada.toRoute<DetalleCitaRuta>().citaId
            val cita = DatosMuestra.agendaDoctor.citas.firstOrNull { it.id == id }
            val detalle =
                cita?.let { DatosMuestra.detalleCita.copy(cita = it.copy(fecha = "Mié 21 oct · ${it.hora}")) }
                    ?: DatosMuestra.detalleCita
            DetalleCitaScreen(
                estado = UiState.Exito(detalle),
                onVolver = volver,
                onVerExpediente = { navController.navigate(ExpedienteRuta("p-6")) },
                onIniciarConsulta = { navController.navigate(NuevaConsultaRuta(detalle.cita.id)) },
            )
        }
        composable<NuevaConsultaRuta> { entrada ->
            val id = entrada.toRoute<NuevaConsultaRuta>().citaId
            val cita = DatosMuestra.agendaDoctor.citas.firstOrNull { it.id == id } ?: DatosMuestra.detalleCita.cita
            NuevaConsultaScreen(
                paciente = cita.paciente,
                contexto = stringResource(R.string.cita_titulo, cita.hora),
                onCerrar = volver,
                onGuardar = volver,
            )
        }
        composable<MisPacientesRuta> {
            MisPacientesScreen(
                estado = UiState.Exito(DatosMuestra.misPacientes),
                onPaciente = { navController.navigate(ExpedienteRuta(it.id)) },
            )
        }
        composable<ExpedienteRuta> { entrada ->
            val id = entrada.toRoute<ExpedienteRuta>().pacienteId
            val paciente =
                DatosMuestra.misPacientes.firstOrNull { it.id == id } ?: DatosMuestra.expedienteTomas.paciente
            ExpedienteScreen(
                estado = UiState.Exito(DatosMuestra.expedienteTomas.copy(paciente = paciente)),
                onVolver = volver,
                onNuevaConsulta = { navController.navigate(NuevaConsultaRuta(DatosMuestra.detalleCita.cita.id)) },
            )
        }
        composable<MisTurnosRuta> {
            MisTurnosScreen(
                estado = UiState.Exito(DatosMuestra.turnosDoctorSemana to DatosMuestra.turnosDoctorSiguiente),
            )
        }
        composable<PerfilDoctorRuta> {
            PerfilScreen(perfil = DatosMuestra.perfilDoctor, onCerrarSesion = onCerrarSesion)
        }
    }
}

@PreviewScreenSizes
@Composable
private fun DoctorHomePreview() {
    HospitalUnicaesTheme { DoctorHomeScreen(onCerrarSesion = {}) }
}
