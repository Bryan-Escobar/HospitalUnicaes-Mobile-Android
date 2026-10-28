package sv.edu.unicaes.hospital.ui.recepcion

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
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.ui.components.HomeConPestanas
import sv.edu.unicaes.hospital.ui.components.Pestana
import sv.edu.unicaes.hospital.ui.components.irAPestana
import sv.edu.unicaes.hospital.ui.modelo.DatosMuestra
import sv.edu.unicaes.hospital.ui.perfil.PerfilScreen
import sv.edu.unicaes.hospital.ui.theme.HospitalUnicaesTheme

// Recepcionista · Asistente · Enfermero/a (AND-R1 a AND-R17)
private val pestanasRecepcion =
    listOf(
        Pestana(R.string.tab_inicio, R.drawable.ic_home, InicioRuta),
        Pestana(R.string.tab_pacientes, R.drawable.ic_group, PacientesRuta),
        Pestana(R.string.tab_citas, R.drawable.ic_calendar, CitasRuta),
        Pestana(R.string.tab_camas, R.drawable.ic_bed, CamasRuta),
        Pestana(R.string.tab_mas, R.drawable.ic_menu, MasRuta),
    )

@Composable
fun RecepcionHomeScreen(onCerrarSesion: () -> Unit) {
    val navController = rememberNavController()
    var seleccionada by rememberSaveable { mutableIntStateOf(0) }

    HomeConPestanas(
        pestanas = pestanasRecepcion,
        seleccionada = seleccionada,
        onSeleccion = {
            seleccionada = it
            navController.irAPestana(pestanasRecepcion[it].ruta)
        },
    ) { modifier ->
        RecepcionNavHost(navController = navController, onCerrarSesion = onCerrarSesion, modifier = modifier)
    }
}

/**
 * Grafo de Recepción (handoff §11.2). Mientras no existan los ViewModels de cada pantalla,
 * los estados salen de [DatosMuestra]; cada pantalla ya recibe su UiState listo para conectarse.
 */
@Composable
private fun RecepcionNavHost(
    navController: NavHostController,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val volver: () -> Unit = { navController.popBackStack() }

    NavHost(navController = navController, startDestination = InicioRuta, modifier = modifier) {
        composable<InicioRuta> {
            InicioRecepcionScreen(
                estado = UiState.Exito(DatosMuestra.inicioRecepcion),
                onAmbulancia = { navController.navigate(AmbulanciaRuta) },
                onLaboratorio = { navController.navigate(LaboratorioRuta) },
                onCitas = { navController.irAPestana(CitasRuta) },
            )
        }
        composable<PacientesRuta> { entrada ->
            val viewModel: PacientesViewModel = hiltViewModel()
            RecargarAlVolver(entrada, viewModel::cargar)
            PacientesScreen(
                onPaciente = { navController.navigate(FichaPacienteRuta(it.id)) },
                onRegistrar = { navController.navigate(RegistroPacienteRuta()) },
                viewModel = viewModel,
            )
        }
        composable<RegistroPacienteRuta> {
            RegistroPacienteScreen(
                onCerrar = volver,
                onGuardado = {
                    avisarGuardado(navController)
                    volver()
                },
                onAbrirFicha = { id ->
                    navController.navigate(FichaPacienteRuta(id)) { popUpTo<RegistroPacienteRuta> { inclusive = true } }
                },
            )
        }
        composable<FichaPacienteRuta> { entrada ->
            val id = entrada.toRoute<FichaPacienteRuta>().pacienteId
            val viewModel: FichaPacienteViewModel = hiltViewModel()
            RecargarAlVolver(entrada, viewModel::cargar)
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            FichaPacienteScreen(
                estado = estado,
                onVolver = volver,
                onEditar = { navController.navigate(RegistroPacienteRuta(id)) },
                onIngresar = { navController.navigate(IngresoCamaRuta) },
                onTrasladar = { navController.navigate(TrasladoRuta) },
                onAlta = { navController.navigate(AltaRuta) },
                onAgendar = { navController.navigate(NuevaCitaRuta(pacienteId = id)) },
                onCita = { navController.navigate(NuevaCitaRuta(citaId = it)) },
                onReintentar = viewModel::cargar,
            )
        }
        composable<CitasRuta> { entrada ->
            val viewModel: CitasViewModel = hiltViewModel()
            RecargarAlVolver(entrada, viewModel::cargar)
            CitasScreen(
                onNuevaCita = { fecha -> navController.navigate(NuevaCitaRuta(fecha = fecha)) },
                onCita = { navController.navigate(NuevaCitaRuta(citaId = it.id)) },
                onHueco = {
                    navController.navigate(
                        NuevaCitaRuta(fecha = it.fecha, hora = it.hora, doctorId = it.doctorId),
                    )
                },
                viewModel = viewModel,
            )
        }
        composable<NuevaCitaRuta> {
            NuevaCitaScreen(
                onCerrar = volver,
                onTerminado = {
                    avisarGuardado(navController)
                    volver()
                },
            )
        }
        composable<CamasRuta> {
            MapaCamasScreen(
                estado = UiState.Exito(DatosMuestra.planta2 + DatosMuestra.planta3),
                onIngreso = { navController.navigate(IngresoCamaRuta) },
                onVerFicha = { navController.navigate(FichaPacienteRuta("p-1")) },
                onTrasladar = { navController.navigate(TrasladoRuta) },
                onAlta = { navController.navigate(AltaRuta) },
            )
        }
        composable<IngresoCamaRuta> {
            IngresoCamaScreen(
                paciente = DatosMuestra.pacienteIngreso,
                habitaciones = DatosMuestra.planta2 + DatosMuestra.planta3,
                onVolver = volver,
                onCambiarPaciente = { navController.irAPestana(PacientesRuta) },
                onConfirmar = { volver() },
            )
        }
        composable<TrasladoRuta> {
            TrasladoScreen(
                paciente = DatosMuestra.fichaLuis.paciente.nombre,
                asignacion = DatosMuestra.asignacionLuis,
                habitaciones = DatosMuestra.planta2 + DatosMuestra.planta3,
                onVolver = volver,
                onConfirmar = { volver() },
            )
        }
        composable<AltaRuta> {
            AltaScreen(ficha = DatosMuestra.fichaLuis, onVolver = volver, onDarAlta = volver)
        }
        composable<MasRuta> {
            MasScreen(
                onLaboratorio = { navController.navigate(LaboratorioRuta) },
                onAmbulancia = { navController.navigate(AmbulanciaRuta) },
                onTurnos = { navController.navigate(TurnosPersonalRuta) },
                onPerfil = { navController.navigate(PerfilRecepcionRuta) },
            )
        }
        composable<LaboratorioRuta> {
            ColaLaboratorioScreen(
                estado = UiState.Exito(DatosMuestra.colaLaboratorio),
                conteos = DatosMuestra.conteoLaboratorio,
                onVolver = volver,
                onOrden = { navController.navigate(DetalleOrdenRuta(it.id)) },
            )
        }
        composable<DetalleOrdenRuta> { entrada ->
            val id = entrada.toRoute<DetalleOrdenRuta>().ordenId
            val orden =
                DatosMuestra.colaLaboratorio
                    .firstOrNull { it.id == id }
                    ?.let { if (it.numero == DatosMuestra.ordenOrina.numero) DatosMuestra.ordenOrina else it }
                    ?: DatosMuestra.ordenOrina
            DetalleOrdenScreen(
                estado = UiState.Exito(orden),
                onVolver = volver,
                onPasarAEnProceso = volver,
                onCargarResultado = { navController.navigate(CargarResultadoRuta) },
                onCompletar = volver,
            )
        }
        composable<CargarResultadoRuta> {
            CargarResultadoScreen(orden = DatosMuestra.ordenOrina, onCerrar = volver, onGuardar = volver)
        }
        composable<AmbulanciaRuta> {
            AmbulanciaScreen(
                estado = UiState.Exito(DatosMuestra.ambulancia),
                onVolver = volver,
                onDespachar = {},
                onBitacora = { navController.navigate(BitacoraRuta) },
            )
        }
        composable<BitacoraRuta> {
            BitacoraScreen(
                estado = UiState.Exito(DatosMuestra.bitacora),
                detalle = DatosMuestra.detalleServicio,
                onVolver = volver,
            )
        }
        composable<TurnosPersonalRuta> {
            TurnosPersonalScreen(estado = UiState.Exito(DatosMuestra.turnosPersonal), onVolver = volver)
        }
        composable<PerfilRecepcionRuta> {
            PerfilScreen(onCerrarSesion = onCerrarSesion, onVolver = volver)
        }
    }
}

@PreviewScreenSizes
@Composable
private fun RecepcionHomePreview() {
    HospitalUnicaesTheme { RecepcionHomeScreen(onCerrarSesion = {}) }
}

/** Clave del savedStateHandle con la que un formulario avisa a la pantalla anterior que guardó. */
private const val CLAVE_GUARDADO = "formulario_guardado"

private fun avisarGuardado(navController: NavHostController) {
    navController.previousBackStackEntry?.savedStateHandle?.set(CLAVE_GUARDADO, true)
}

/** Vuelve a cargar la pantalla cuando el formulario que se abrió desde ella guardó cambios. */
@Composable
private fun RecargarAlVolver(
    entrada: NavBackStackEntry,
    recargar: () -> Unit,
) {
    val guardado by entrada.savedStateHandle
        .getStateFlow(CLAVE_GUARDADO, false)
        .collectAsStateWithLifecycle()
    LaunchedEffect(guardado) {
        if (guardado) {
            entrada.savedStateHandle[CLAVE_GUARDADO] = false
            recargar()
        }
    }
}
