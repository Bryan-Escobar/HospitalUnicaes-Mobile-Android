package sv.edu.unicaes.hospital.ui.recepcion

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.core.mensaje
import sv.edu.unicaes.hospital.domain.model.Sexo
import sv.edu.unicaes.hospital.domain.model.TipoHabitacion
import sv.edu.unicaes.hospital.ui.components.AccionTexto
import sv.edu.unicaes.hospital.ui.components.Avatar
import sv.edu.unicaes.hospital.ui.components.BadgeCita
import sv.edu.unicaes.hospital.ui.components.BadgeEstado
import sv.edu.unicaes.hospital.ui.components.Banner
import sv.edu.unicaes.hospital.ui.components.BotonPeligro
import sv.edu.unicaes.hospital.ui.components.BotonPrimario
import sv.edu.unicaes.hospital.ui.components.BotonSecundario
import sv.edu.unicaes.hospital.ui.components.CabeceraPersona
import sv.edu.unicaes.hospital.ui.components.CampoTexto
import sv.edu.unicaes.hospital.ui.components.Chevron
import sv.edu.unicaes.hospital.ui.components.ChipEnVivo
import sv.edu.unicaes.hospital.ui.components.ContenidoConEstado
import sv.edu.unicaes.hospital.ui.components.EstadoPreview
import sv.edu.unicaes.hospital.ui.components.EstadoVacio
import sv.edu.unicaes.hospital.ui.components.EstadosPreviewProvider
import sv.edu.unicaes.hospital.ui.components.FabHospital
import sv.edu.unicaes.hospital.ui.components.FilaLista
import sv.edu.unicaes.hospital.ui.components.IconoAccion
import sv.edu.unicaes.hospital.ui.components.PreviewPantalla
import sv.edu.unicaes.hospital.ui.components.SegmentosHospital
import sv.edu.unicaes.hospital.ui.components.TarjetaHospital
import sv.edu.unicaes.hospital.ui.components.TarjetaLista
import sv.edu.unicaes.hospital.ui.components.TextosFila
import sv.edu.unicaes.hospital.ui.components.TipoBanner
import sv.edu.unicaes.hospital.ui.components.TituloSeccion
import sv.edu.unicaes.hospital.ui.components.TopBarHospital
import sv.edu.unicaes.hospital.ui.components.iniciales
import sv.edu.unicaes.hospital.ui.modelo.DatosMuestra
import sv.edu.unicaes.hospital.ui.modelo.FichaPacienteUi
import sv.edu.unicaes.hospital.ui.modelo.PacienteUi
import sv.edu.unicaes.hospital.ui.theme.Espacio
import sv.edu.unicaes.hospital.ui.theme.HospitalTheme
import sv.edu.unicaes.hospital.ui.theme.HospitalUnicaesTheme
import sv.edu.unicaes.hospital.ui.theme.Primario700
import sv.edu.unicaes.hospital.ui.theme.Texto2

/** Relleno del cuerpo de las pantallas (`.and-body`): 8 arriba, 16 a los lados y abajo. */
internal val RellenoCuerpo = PaddingValues(start = Espacio.md, end = Espacio.md, top = Espacio.xs, bottom = Espacio.md)

/** Relleno del cuerpo cuando hay FAB: deja espacio para que no tape la última fila. */
internal val RellenoCuerpoConFab = PaddingValues(start = Espacio.md, end = Espacio.md, top = Espacio.xs, bottom = 96.dp)

/** Título de tarjeta (`h2` del diseño): 16/24 500. */
internal val EstiloTituloTarjeta = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium)

/** AND-R2 · Pacientes conectada a [PacientesViewModel]. */
@Composable
fun PacientesScreen(
    onPaciente: (PacienteUi) -> Unit,
    onRegistrar: () -> Unit,
    viewModel: PacientesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PacientesScreen(
        estado = uiState.estado,
        busqueda = uiState.busqueda,
        onBusqueda = viewModel::onBusquedaChange,
        onPaciente = onPaciente,
        onRegistrar = onRegistrar,
        onReintentar = viewModel::cargar,
        onCargarMas = viewModel::cargarMas,
        cargandoMas = uiState.cargandoMas,
    )
}

/** AND-R2 · Pacientes: búsqueda por nombre o documento (en la API) y carga por páginas. */
@Composable
fun PacientesScreen(
    estado: UiState<List<PacienteUi>>,
    busqueda: String,
    onBusqueda: (String) -> Unit,
    onPaciente: (PacienteUi) -> Unit,
    onRegistrar: () -> Unit,
    onReintentar: () -> Unit = {},
    onCargarMas: () -> Unit = {},
    cargandoMas: Boolean = false,
) {
    Scaffold(
        topBar = { TopBarHospital(titulo = stringResource(R.string.tab_pacientes)) },
        floatingActionButton = {
            FabHospital(
                texto = stringResource(R.string.pacientes_registrar),
                onClick = onRegistrar,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(start = Espacio.md, end = Espacio.md, top = Espacio.xs),
            verticalArrangement = Arrangement.spacedBy(Espacio.sm),
        ) {
            CampoTexto(
                etiqueta = null,
                valor = busqueda,
                onValor = onBusqueda,
                placeholder = stringResource(R.string.pacientes_buscar),
            )
            ContenidoConEstado(
                estado = estado,
                mensajeError = stringResource(R.string.error_cargar_lista),
                onReintentar = onReintentar,
                vacio = {
                    EstadoVacio(
                        titulo = stringResource(R.string.pacientes_vacio_titulo),
                        detalle = stringResource(R.string.pacientes_vacio_detalle),
                        icono = R.drawable.ic_search,
                        accion = stringResource(R.string.pacientes_registrar),
                        onAccion = onRegistrar,
                    )
                },
            ) { pacientes ->
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    item {
                        TarjetaLista(pacientes) { paciente, ultimo ->
                            FilaPaciente(paciente = paciente, divisor = !ultimo, onClick = { onPaciente(paciente) })
                        }
                    }
                    // Al acercarse al final se compone este item y se pide la siguiente página.
                    item(key = pacientes.size) {
                        LaunchedEffect(pacientes.size) { onCargarMas() }
                        if (cargandoMas) {
                            Box(Modifier.fillMaxWidth().padding(Espacio.md), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Fila de paciente dentro de [TarjetaLista]: avatar, nombre, documento y su etiqueta (o chevron). */
@Composable
fun FilaPaciente(
    paciente: PacienteUi,
    onClick: () -> Unit,
    soporte: String = paciente.dui,
    divisor: Boolean = true,
) {
    FilaLista(
        titulo = paciente.nombre,
        soporte = soporte,
        onClick = onClick,
        divisor = divisor,
        enTarjeta = true,
        inicio = { Avatar(iniciales(paciente.nombre)) },
        final = {
            val etiqueta = paciente.etiqueta
            if (etiqueta == null) {
                Chevron()
            } else {
                val estados = HospitalTheme.estados
                BadgeEstado(
                    texto = etiqueta,
                    colores = if (etiqueta == "Emergencia") estados.personalEmergencia else estados.personalMedico,
                )
            }
        },
    )
}

/** AND-R3 · Registro / edición de paciente conectada a [RegistroPacienteViewModel]. */
@Composable
fun RegistroPacienteScreen(
    onCerrar: () -> Unit,
    onGuardado: () -> Unit,
    onAbrirFicha: (String) -> Unit,
    viewModel: RegistroPacienteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.guardado) { if (uiState.guardado) onGuardado() }
    RegistroPacienteContent(
        uiState = uiState,
        onCerrar = onCerrar,
        onGuardar = viewModel::guardar,
        onFormChange = viewModel::onFormChange,
        onAbrirFicha = onAbrirFicha,
        onReintentar = viewModel::cargar,
    )
}

/**
 * Formulario AND-R3. Los errores de validación se muestran bajo cada campo y el 409 por
 * documento duplicado, además, con un banner que abre la ficha existente.
 */
@Composable
fun RegistroPacienteContent(
    uiState: RegistroPacienteUiState,
    onCerrar: () -> Unit,
    onGuardar: () -> Unit,
    onFormChange: ((FormularioPaciente) -> FormularioPaciente) -> Unit,
    onAbrirFicha: (String) -> Unit,
    onReintentar: () -> Unit = {},
) {
    val form = uiState.form
    val tecladoTelefono = KeyboardOptions(keyboardType = KeyboardType.Phone)
    val tecladoNumerico = KeyboardOptions(keyboardType = KeyboardType.Number)

    @Composable
    fun error(campo: CampoPaciente): String? = uiState.errores[campo]?.let { stringResource(it) }

    Scaffold(
        topBar = {
            TopBarHospital(
                titulo =
                    stringResource(
                        if (uiState.edicion) R.string.pacientes_editar else R.string.pacientes_registrar,
                    ),
                onVolver = onCerrar,
                cerrar = true,
                acciones = {
                    if (uiState.guardando) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(horizontal = Espacio.md).size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else if (uiState.puedeGuardar) {
                        AccionTexto(
                            texto = stringResource(R.string.accion_guardar),
                            onClick = onGuardar,
                            modifier = Modifier.padding(horizontal = Espacio.sm),
                        )
                    }
                },
            )
        },
    ) { padding ->
        ContenidoConEstado(
            estado = uiState.carga,
            mensajeError = stringResource(R.string.ficha_error),
            onReintentar = onReintentar,
            modifier = Modifier.padding(padding),
            vacio = {},
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(RellenoCuerpo),
                verticalArrangement = Arrangement.spacedBy(Espacio.sm),
            ) {
                uiState.errorGeneral?.let { Banner(tipo = TipoBanner.ERROR, texto = stringResource(it.mensaje())) }
                Row(horizontalArrangement = Arrangement.spacedBy(Espacio.sm)) {
                    CampoTexto(
                        stringResource(R.string.campo_nombres),
                        form.nombres,
                        { valor -> onFormChange { it.copy(nombres = valor) } },
                        placeholder = "María",
                        error = error(CampoPaciente.NOMBRES),
                        modifier = Modifier.weight(1f),
                    )
                    CampoTexto(
                        stringResource(R.string.campo_apellidos),
                        form.apellidos,
                        { valor -> onFormChange { it.copy(apellidos = valor) } },
                        placeholder = "López",
                        error = error(CampoPaciente.APELLIDOS),
                        modifier = Modifier.weight(1f),
                    )
                }
                val duplicado = uiState.documentoDuplicado
                CampoTexto(
                    etiqueta = stringResource(R.string.campo_dui),
                    valor = form.documento,
                    onValor = { valor -> onFormChange { it.copy(documento = formatearDui(valor)) } },
                    placeholder = "00000000-0",
                    error =
                        duplicado?.let { stringResource(R.string.pacientes_duplicado, it) }
                            ?: error(CampoPaciente.DOCUMENTO),
                    keyboardOptions = tecladoNumerico,
                )
                val duplicadoId = uiState.duplicadoId
                if (duplicado != null && duplicadoId != null) {
                    Banner(
                        tipo = TipoBanner.ERROR,
                        texto = stringResource(R.string.pacientes_duplicado, duplicado),
                        accion = stringResource(R.string.pacientes_abrir_ficha),
                        onAccion = { onAbrirFicha(duplicadoId) },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Espacio.sm)) {
                    CampoTexto(
                        stringResource(R.string.campo_nacimiento),
                        form.nacimiento,
                        { valor -> onFormChange { it.copy(nacimiento = formatearFecha(valor)) } },
                        placeholder = "dd/mm/aaaa",
                        error = error(CampoPaciente.NACIMIENTO),
                        keyboardOptions = tecladoNumerico,
                        modifier = Modifier.weight(1f),
                    )
                    CampoTexto(
                        stringResource(R.string.campo_telefono),
                        form.telefono,
                        { valor -> onFormChange { it.copy(telefono = formatearTelefono(valor)) } },
                        placeholder = "0000-0000",
                        error = error(CampoPaciente.TELEFONO),
                        keyboardOptions = tecladoTelefono,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(stringResource(R.string.campo_sexo), style = MaterialTheme.typography.labelLarge)
                val sexos = Sexo.entries
                SegmentosHospital(
                    opciones = sexos.map { stringResource(it.etiqueta()) },
                    seleccionada = sexos.indexOf(form.sexo),
                    onSeleccion = { indice -> onFormChange { it.copy(sexo = sexos[indice]) } },
                )
                CampoTexto(
                    etiqueta = stringResource(R.string.campo_direccion),
                    valor = form.direccion,
                    onValor = { valor -> onFormChange { it.copy(direccion = valor) } },
                    placeholder = stringResource(R.string.pacientes_direccion_placeholder),
                    error = error(CampoPaciente.DIRECCION),
                )
                TituloSeccion(stringResource(R.string.campo_contacto_emergencia))
                Row(horizontalArrangement = Arrangement.spacedBy(Espacio.sm)) {
                    CampoTexto(
                        stringResource(R.string.campo_nombre),
                        form.contactoNombre,
                        { valor -> onFormChange { it.copy(contactoNombre = valor) } },
                        placeholder = stringResource(R.string.campo_nombre),
                        error = error(CampoPaciente.CONTACTO_NOMBRE),
                        modifier = Modifier.weight(1f),
                    )
                    CampoTexto(
                        stringResource(R.string.campo_telefono_simple),
                        form.contactoTelefono,
                        { valor -> onFormChange { it.copy(contactoTelefono = formatearTelefono(valor)) } },
                        placeholder = "0000-0000",
                        error = error(CampoPaciente.CONTACTO_TELEFONO),
                        keyboardOptions = tecladoTelefono,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@StringRes
private fun Sexo.etiqueta(): Int =
    when (this) {
        Sexo.MASCULINO -> R.string.sexo_masculino
        Sexo.FEMENINO -> R.string.sexo_femenino
        Sexo.OTRO -> R.string.sexo_otro
    }

/** Chevron ▾ de los campos de selección (18, n600). */
@Composable
internal fun IconoDesplegable() {
    Icon(
        painter = painterResource(R.drawable.ic_expand_more),
        contentDescription = null,
        tint = Texto2,
        modifier = Modifier.size(18.dp),
    )
}

/**
 * AND-R4 · Ficha del paciente (vista recepción). Recepción ve datos personales, citas y
 * la cama asignada, nunca el expediente clínico (context.md §3.4).
 */
@Composable
fun FichaPacienteScreen(
    estado: UiState<FichaPacienteUi>,
    onVolver: () -> Unit,
    onEditar: () -> Unit,
    onIngresar: () -> Unit,
    onTrasladar: () -> Unit,
    onAlta: () -> Unit,
    onAgendar: () -> Unit,
    onReintentar: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopBarHospital(
                titulo = stringResource(R.string.ficha_titulo),
                onVolver = onVolver,
                acciones = {
                    IconoAccion(R.drawable.ic_edit, stringResource(R.string.accion_editar), onEditar)
                },
            )
        },
    ) { padding ->
        ContenidoConEstado(
            estado = estado,
            mensajeError = stringResource(R.string.ficha_error),
            onReintentar = onReintentar,
            modifier = Modifier.padding(padding),
            vacio = {},
        ) { ficha ->
            LazyColumn(
                contentPadding = RellenoCuerpo,
                verticalArrangement = Arrangement.spacedBy(Espacio.sm),
            ) {
                item {
                    CabeceraPersona(
                        nombre = ficha.paciente.nombre,
                        soporte =
                            ficha.edad?.let { edad ->
                                stringResource(R.string.ficha_cabecera, ficha.paciente.dui, edad, ficha.telefono)
                            } ?: stringResource(R.string.ficha_cabecera_sin_edad, ficha.paciente.dui, ficha.telefono),
                    )
                }
                item {
                    TarjetaAsignacion(
                        ficha = ficha,
                        onIngresar = onIngresar,
                        onTrasladar = onTrasladar,
                        onAlta = onAlta,
                    )
                }
                item { TarjetaContacto(ficha.contactoEmergencia) }
                item {
                    TarjetaHospital {
                        Text(stringResource(R.string.tab_citas), style = EstiloTituloTarjeta)
                        if (ficha.citas.isEmpty()) {
                            EstadoVacio(
                                titulo = stringResource(R.string.ficha_sin_citas_titulo),
                                detalle = stringResource(R.string.ficha_sin_citas_detalle),
                                accion = stringResource(R.string.ficha_agendar),
                                onAccion = onAgendar,
                            )
                        }
                        ficha.citas.forEachIndexed { indice, cita ->
                            FilaLista(
                                titulo = "${cita.fecha} · ${cita.hora}",
                                soporte = cita.doctor,
                                divisor = indice < ficha.citas.lastIndex,
                                enTarjeta = true,
                                margenHorizontal = 0.dp,
                                final = { BadgeCita(cita.estado) },
                            )
                        }
                    }
                }
                item { Banner(tipo = TipoBanner.INFO, texto = stringResource(R.string.ficha_nota_privacidad)) }
            }
        }
    }
}

@Composable
private fun TarjetaAsignacion(
    ficha: FichaPacienteUi,
    onIngresar: () -> Unit,
    onTrasladar: () -> Unit,
    onAlta: () -> Unit,
) {
    TarjetaHospital {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.ficha_asignacion),
                style = EstiloTituloTarjeta,
                modifier = Modifier.weight(1f),
            )
            ChipEnVivo()
        }
        val asignacion = ficha.asignacion
        if (asignacion == null) {
            TextosFila(titulo = stringResource(R.string.ficha_sin_cama))
            BotonPrimario(texto = stringResource(R.string.camas_ingresar), onClick = onIngresar)
        } else {
            val tipo =
                stringResource(
                    if (asignacion.tipo ==
                        TipoHabitacion.VIP
                    ) {
                        R.string.ocupacion_vip
                    } else {
                        R.string.habitacion_semiprivada
                    },
                )
            TextosFila(
                titulo = stringResource(R.string.ficha_cama, asignacion.habitacion, asignacion.cama) + " · $tipo",
                soporte = stringResource(R.string.ficha_desde, asignacion.desde, asignacion.tipo.planta),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                BotonSecundario(
                    texto = stringResource(R.string.camas_trasladar),
                    onClick = onTrasladar,
                    modifier = Modifier.weight(1f),
                )
                BotonPeligro(
                    texto = stringResource(R.string.camas_dar_alta),
                    onClick = onAlta,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** "Contacto de emergencia" con nombre, teléfono y botón de llamar. */
@Composable
private fun TarjetaContacto(contacto: String) {
    val nombre = contacto.substringBefore(" · ")
    val telefono = contacto.substringAfter(" · ", missingDelimiterValue = "")
    TarjetaHospital {
        Text(stringResource(R.string.campo_contacto_emergencia), style = EstiloTituloTarjeta)
        if (contacto.isBlank()) {
            TextosFila(titulo = stringResource(R.string.ficha_sin_contacto))
            return@TarjetaHospital
        }
        FilaLista(
            titulo = nombre,
            soporte = telefono.ifEmpty { null },
            divisor = false,
            enTarjeta = true,
            margenHorizontal = 0.dp,
            final = {
                IconoAccion(
                    icono = R.drawable.ic_phone,
                    descripcion = stringResource(R.string.accion_llamar, nombre),
                    onClick = {},
                    tinte = Primario700,
                )
            },
        )
    }
}

@PreviewPantalla
@Composable
private fun PacientesPreview(
    @PreviewParameter(EstadosPreviewProvider::class) estado: EstadoPreview,
) {
    HospitalUnicaesTheme {
        PacientesScreen(
            estado = estado.con(DatosMuestra.pacientes),
            busqueda = "",
            onBusqueda = {},
            onPaciente = {},
            onRegistrar = {},
        )
    }
}

@PreviewPantalla
@Composable
private fun RegistroPacientePreview() {
    HospitalUnicaesTheme {
        RegistroPacienteContent(
            uiState =
                RegistroPacienteUiState(
                    form = FormularioPaciente(nombres = "María", apellidos = "López", documento = "04512378-9"),
                    errores = mapOf(CampoPaciente.TELEFONO to R.string.campo_obligatorio),
                    documentoDuplicado = "04512378-9",
                    duplicadoId = "p-2",
                ),
            onCerrar = {},
            onGuardar = {},
            onFormChange = {},
            onAbrirFicha = {},
        )
    }
}

@PreviewPantalla
@Composable
private fun FichaPacientePreview(
    @PreviewParameter(EstadosPreviewProvider::class) estado: EstadoPreview,
) {
    HospitalUnicaesTheme {
        FichaPacienteScreen(
            estado = estado.con(DatosMuestra.fichaLuis),
            onVolver = {},
            onEditar = {},
            onIngresar = {},
            onTrasladar = {},
            onAlta = {},
            onAgendar = {},
        )
    }
}
