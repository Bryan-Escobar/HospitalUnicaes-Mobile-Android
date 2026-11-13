package sv.edu.unicaes.hospital.ui.recepcion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import sv.edu.unicaes.hospital.ui.components.HomeConPestanas
import sv.edu.unicaes.hospital.ui.components.Pestana
import sv.edu.unicaes.hospital.ui.components.SincronizarPestana
import sv.edu.unicaes.hospital.ui.components.irAPestana
import sv.edu.unicaes.hospital.ui.modelo.ConDatos
import sv.edu.unicaes.hospital.ui.modelo.DatosMuestra
import sv.edu.unicaes.hospital.ui.modelo.MuestraViewModel
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

    SincronizarPestana(navController, pestanasRecepcion) { seleccionada = it }
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
 * Grafo de Recepción (handoff §11.2). Cada pantalla tiene su ViewModel; los que todavía no
 * tienen endpoint extienden [MuestraViewModel] y sirven [DatosMuestra].
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
            val viewModel: InicioRecepcionViewModel = hiltViewModel()
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            InicioRecepcionScreen(
                estado = estado,
                onAmbulancia = { navController.navigate(AmbulanciaRuta) },
                onLaboratorio = { navController.navigate(LaboratorioRuta) },
                onCitas = { navController.irAPestana(CitasRuta) },
                onReintentar = viewModel::cargar,
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
                onIngresar = { navController.navigate(IngresoCamaRuta(id)) },
                onTrasladar = { navController.navigate(TrasladoRuta(id)) },
                onAlta = { navController.navigate(AltaRuta(id)) },
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
            val viewModel: MapaCamasViewModel = hiltViewModel()
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            val aviso by viewModel.aviso.collectAsStateWithLifecycle()
            MapaCamasScreen(
                estado = estado,
                // El ingreso parte de un paciente: se elige en Pacientes y se ingresa desde su ficha.
                onIngreso = { navController.irAPestana(PacientesRuta) },
                onVerFicha = { cama -> cama.pacienteId?.let { navController.navigate(FichaPacienteRuta(it)) } },
                onTrasladar = { cama -> cama.pacienteId?.let { navController.navigate(TrasladoRuta(it)) } },
                onAlta = { cama -> cama.pacienteId?.let { navController.navigate(AltaRuta(it)) } },
                onReintentar = viewModel::cargar,
                onMarcarDisponible = viewModel::marcarDisponible,
                aviso = aviso,
                onCerrarAviso = viewModel::cerrarAviso,
            )
        }
        composable<IngresoCamaRuta> {
            val viewModel: IngresoCamaViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            AlTerminar(uiState.terminado) {
                avisarGuardado(navController)
                volver()
            }
            FormularioCamasConEstado(
                titulo = stringResource(R.string.ingreso_titulo),
                estado = uiState.estado,
                onVolver = volver,
                onReintentar = viewModel::cargar,
            ) { ingreso ->
                IngresoCamaScreen(
                    paciente = ingreso.paciente,
                    habitaciones = ingreso.habitaciones,
                    onVolver = volver,
                    onCambiarPaciente = { navController.irAPestana(PacientesRuta) },
                    onConfirmar = viewModel::confirmar,
                    camaEnConflicto = uiState.camaEnConflicto,
                    doctores = ingreso.doctores,
                    doctorId = uiState.doctorId,
                    onDoctor = viewModel::elegirDoctor,
                    motivo = uiState.motivo,
                    onMotivo = viewModel::cambiarMotivo,
                    yaHospitalizado = ingreso.yaHospitalizado,
                    enviando = uiState.enviando,
                    error = uiState.error,
                    onDescartarConflicto = viewModel::descartarConflicto,
                )
            }
        }
        composable<TrasladoRuta> {
            val viewModel: TrasladoViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            AlTerminar(uiState.terminado) {
                avisarGuardado(navController)
                volver()
            }
            FormularioCamasConEstado(
                titulo = stringResource(R.string.camas_trasladar),
                estado = uiState.estado,
                onVolver = volver,
                onReintentar = viewModel::cargar,
            ) { traslado ->
                TrasladoScreen(
                    paciente = traslado.paciente,
                    asignacion = traslado.asignacion,
                    habitaciones = traslado.habitaciones,
                    onVolver = volver,
                    onConfirmar = viewModel::confirmar,
                    camaEnConflicto = uiState.camaEnConflicto,
                    enviando = uiState.enviando,
                    error = uiState.error,
                    onDescartarConflicto = viewModel::descartarConflicto,
                )
            }
        }
        composable<AltaRuta> {
            val viewModel: AltaViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            AlTerminar(uiState.terminado) {
                avisarGuardado(navController)
                volver()
            }
            FormularioCamasConEstado(
                titulo = stringResource(R.string.camas_dar_alta),
                estado = uiState.estado,
                onVolver = volver,
                onReintentar = viewModel::cargar,
            ) { ficha ->
                AltaScreen(
                    ficha = ficha,
                    onVolver = volver,
                    onDarAlta = viewModel::darAlta,
                    enviando = uiState.enviando,
                    error = uiState.error,
                )
            }
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
            val viewModel: ColaLaboratorioViewModel = hiltViewModel()
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            ColaLaboratorioScreen(
                estado = estado,
                conteos = viewModel.conteos,
                onVolver = volver,
                onOrden = { navController.navigate(DetalleOrdenRuta(it.id)) },
                onReintentar = viewModel::cargar,
            )
        }
        composable<DetalleOrdenRuta> {
            val viewModel: DetalleOrdenViewModel = hiltViewModel()
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            DetalleOrdenScreen(
                estado = estado,
                onVolver = volver,
                onPasarAEnProceso = volver,
                onCargarResultado = { navController.navigate(CargarResultadoRuta) },
                onCompletar = volver,
                onReintentar = viewModel::cargar,
            )
        }
        composable<CargarResultadoRuta> {
            ConDatos(hiltViewModel<CargarResultadoViewModel>()) { orden ->
                CargarResultadoScreen(orden = orden, onCerrar = volver, onGuardar = volver)
            }
        }
        composable<AmbulanciaRuta> {
            val viewModel: AmbulanciaViewModel = hiltViewModel()
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            AmbulanciaScreen(
                estado = estado,
                onVolver = volver,
                onDespachar = {},
                onBitacora = { navController.navigate(BitacoraRuta) },
                onReintentar = viewModel::cargar,
            )
        }
        composable<BitacoraRuta> {
            val viewModel: BitacoraViewModel = hiltViewModel()
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            BitacoraScreen(
                estado = estado,
                detalle = viewModel.detalle,
                onVolver = volver,
                onReintentar = viewModel::cargar,
            )
        }
        composable<TurnosPersonalRuta> {
            val viewModel: TurnosPersonalViewModel = hiltViewModel()
            val estado by viewModel.uiState.collectAsStateWithLifecycle()
            TurnosPersonalScreen(estado = estado, onVolver = volver, onReintentar = viewModel::cargar)
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

/** Ejecuta [accion] una sola vez cuando un formulario termina (ingreso, traslado, alta). */
@Composable
private fun AlTerminar(
    terminado: Boolean,
    accion: () -> Unit,
) {
    LaunchedEffect(terminado) {
        if (terminado) accion()
    }
}
