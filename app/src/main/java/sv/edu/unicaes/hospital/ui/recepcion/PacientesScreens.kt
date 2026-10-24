package sv.edu.unicaes.hospital.ui.recepcion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.ui.components.Avatar
import sv.edu.unicaes.hospital.ui.components.BadgeCita
import sv.edu.unicaes.hospital.ui.components.Banner
import sv.edu.unicaes.hospital.ui.components.BotonPrimario
import sv.edu.unicaes.hospital.ui.components.BotonSecundario
import sv.edu.unicaes.hospital.ui.components.CampoTexto
import sv.edu.unicaes.hospital.ui.components.Chevron
import sv.edu.unicaes.hospital.ui.components.ChipEnVivo
import sv.edu.unicaes.hospital.ui.components.ChipsFiltro
import sv.edu.unicaes.hospital.ui.components.ContenidoConEstado
import sv.edu.unicaes.hospital.ui.components.EstadoPreview
import sv.edu.unicaes.hospital.ui.components.EstadoVacio
import sv.edu.unicaes.hospital.ui.components.EstadosPreviewProvider
import sv.edu.unicaes.hospital.ui.components.Etiqueta
import sv.edu.unicaes.hospital.ui.components.FilaLista
import sv.edu.unicaes.hospital.ui.components.IconoAccion
import sv.edu.unicaes.hospital.ui.components.PreviewPantalla
import sv.edu.unicaes.hospital.ui.components.TarjetaHospital
import sv.edu.unicaes.hospital.ui.components.TipoBanner
import sv.edu.unicaes.hospital.ui.components.TituloSeccion
import sv.edu.unicaes.hospital.ui.components.TopBarHospital
import sv.edu.unicaes.hospital.ui.components.iniciales
import sv.edu.unicaes.hospital.ui.modelo.DatosMuestra
import sv.edu.unicaes.hospital.ui.modelo.FichaPacienteUi
import sv.edu.unicaes.hospital.ui.modelo.PacienteUi
import sv.edu.unicaes.hospital.ui.theme.Espacio
import sv.edu.unicaes.hospital.ui.theme.HospitalUnicaesTheme

/** AND-R2 · Pacientes: búsqueda por nombre o documento y filtros rápidos. */
@Composable
fun PacientesScreen(
    estado: UiState<List<PacienteUi>>,
    onPaciente: (PacienteUi) -> Unit,
    onRegistrar: () -> Unit,
    onReintentar: () -> Unit = {},
) {
    var busqueda by rememberSaveable { mutableStateOf("") }
    var filtro by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = { TopBarHospital(titulo = stringResource(R.string.tab_pacientes)) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onRegistrar,
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(R.string.pacientes_registrar)) },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                placeholder = { Text(stringResource(R.string.pacientes_buscar)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.padding(horizontal = Espacio.md).fillMaxWidth(),
            )
            ChipsFiltro(
                opciones =
                    listOf(
                        stringResource(R.string.filtro_todos),
                        stringResource(R.string.filtro_hospitalizados),
                        stringResource(R.string.pacientes_registrados_hoy),
                    ),
                seleccionada = filtro,
                onSeleccion = { filtro = it },
                modifier = Modifier.padding(vertical = Espacio.xs),
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
                val visibles =
                    pacientes.filter {
                        (busqueda.isBlank() || it.nombre.contains(busqueda, true) || it.dui.contains(busqueda)) &&
                            (filtro != 1 || it.hospitalizado)
                    }
                LazyColumn(contentPadding = PaddingValues(bottom = Espacio.xxl * 2)) {
                    items(visibles, key = { it.id }) { paciente ->
                        FilaPaciente(paciente = paciente, onClick = { onPaciente(paciente) })
                    }
                }
            }
        }
    }
}

@Composable
fun FilaPaciente(
    paciente: PacienteUi,
    onClick: () -> Unit,
    soporte: String = paciente.dui,
) {
    FilaLista(
        titulo = paciente.nombre,
        soporte = soporte,
        onClick = onClick,
        inicio = { Avatar(iniciales(paciente.nombre)) },
        final = { if (paciente.etiqueta != null) Etiqueta(paciente.etiqueta) else Chevron() },
    )
}

/** AND-R3 · Registro / edición de paciente. El 409 por documento duplicado se muestra en línea. */
@Composable
fun RegistroPacienteScreen(
    onCerrar: () -> Unit,
    onGuardar: () -> Unit,
    onAbrirFichaExistente: () -> Unit,
    documentoDuplicado: String? = null,
) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var documento by rememberSaveable { mutableStateOf(documentoDuplicado.orEmpty()) }
    var nacimiento by rememberSaveable { mutableStateOf("") }
    var telefono by rememberSaveable { mutableStateOf("") }
    var sexo by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var direccion by rememberSaveable { mutableStateOf("") }
    var contactoNombre by rememberSaveable { mutableStateOf("") }
    var contactoTelefono by rememberSaveable { mutableStateOf("") }
    var alergias by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopBarHospital(
                titulo = stringResource(R.string.pacientes_registrar),
                onVolver = onCerrar,
                cerrar = true,
                acciones = { TextButton(onClick = onGuardar) { Text(stringResource(R.string.accion_guardar)) } },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(Espacio.md),
            verticalArrangement = Arrangement.spacedBy(Espacio.xs),
        ) {
            CampoTexto(
                stringResource(R.string.campo_nombre_completo),
                nombre,
                { nombre = it },
                placeholder = "María López",
            )
            CampoTexto(
                etiqueta = stringResource(R.string.campo_dui),
                valor = documento,
                onValor = { documento = it },
                placeholder = "00000000-0",
                error = documentoDuplicado?.let { stringResource(R.string.pacientes_duplicado, it) },
            )
            if (documentoDuplicado != null) {
                Banner(
                    tipo = TipoBanner.ERROR,
                    texto = stringResource(R.string.pacientes_duplicado, documentoDuplicado),
                    accion = stringResource(R.string.pacientes_abrir_ficha),
                    onAccion = onAbrirFichaExistente,
                )
            }
            CampoTexto(
                stringResource(R.string.campo_nacimiento),
                nacimiento,
                { nacimiento = it },
                placeholder = "dd/mm/aaaa",
            )
            CampoTexto(stringResource(R.string.campo_telefono), telefono, { telefono = it }, placeholder = "0000-0000")
            CampoTexto(
                etiqueta = stringResource(R.string.campo_sexo),
                valor = sexo,
                onValor = { sexo = it },
                placeholder = stringResource(R.string.campo_seleccionar),
            )
            CampoTexto(
                etiqueta = stringResource(R.string.campo_correo_opcional),
                valor = correo,
                onValor = { correo = it },
                ayuda = stringResource(R.string.pacientes_correo_ayuda),
            )
            CampoTexto(stringResource(R.string.campo_direccion), direccion, { direccion = it })
            TituloSeccion(stringResource(R.string.campo_contacto_emergencia))
            CampoTexto(stringResource(R.string.campo_nombre), contactoNombre, { contactoNombre = it })
            CampoTexto(stringResource(R.string.campo_telefono_simple), contactoTelefono, { contactoTelefono = it })
            CampoTexto(
                etiqueta = stringResource(R.string.campo_alergias),
                valor = alergias,
                onValor = { alergias = it },
                placeholder = stringResource(R.string.campo_ninguna),
            )
            BotonPrimario(
                texto = stringResource(R.string.accion_guardar),
                onClick = onGuardar,
                modifier = Modifier.padding(top = Espacio.sm),
            )
        }
    }
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
                contentPadding = PaddingValues(Espacio.md),
                verticalArrangement = Arrangement.spacedBy(Espacio.sm),
            ) {
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Espacio.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(iniciales(ficha.paciente.nombre), tamano = Espacio.xxl + Espacio.xs)
                        Column {
                            Text(ficha.paciente.nombre, style = MaterialTheme.typography.headlineSmall)
                            Text(
                                text =
                                    stringResource(
                                        R.string.ficha_cabecera,
                                        ficha.paciente.dui,
                                        ficha.edad,
                                        ficha.telefono,
                                    ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item {
                    TarjetaAsignacion(
                        ficha = ficha,
                        onIngresar = onIngresar,
                        onTrasladar = onTrasladar,
                        onAlta = onAlta,
                    )
                }
                item {
                    TarjetaHospital {
                        Text(
                            stringResource(R.string.campo_contacto_emergencia),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(ficha.contactoEmergencia, style = MaterialTheme.typography.titleMedium)
                    }
                }
                item { TituloSeccion(stringResource(R.string.tab_citas)) }
                if (ficha.citas.isEmpty()) {
                    item {
                        EstadoVacio(
                            titulo = stringResource(R.string.ficha_sin_citas_titulo),
                            detalle = stringResource(R.string.ficha_sin_citas_detalle),
                            accion = stringResource(R.string.ficha_agendar),
                            onAccion = onAgendar,
                        )
                    }
                }
                items(ficha.citas, key = { it.id }) { cita ->
                    FilaLista(
                        titulo = "${cita.fecha} · ${cita.hora}",
                        soporte = cita.doctor,
                        final = { BadgeCita(cita.estado) },
                    )
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
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            ChipEnVivo()
        }
        val asignacion = ficha.asignacion
        if (asignacion == null) {
            Text(stringResource(R.string.ficha_sin_cama), style = MaterialTheme.typography.titleMedium)
            BotonPrimario(texto = stringResource(R.string.camas_ingresar), onClick = onIngresar)
        } else {
            Text(
                text = stringResource(R.string.ficha_cama, asignacion.habitacion, asignacion.cama),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.ficha_desde, asignacion.desde, asignacion.tipo.planta),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Espacio.xs),
                modifier = Modifier.padding(top = Espacio.xs),
            ) {
                BotonSecundario(texto = stringResource(R.string.camas_trasladar), onClick = onTrasladar)
                BotonSecundario(texto = stringResource(R.string.camas_dar_alta), onClick = onAlta, destructivo = true)
            }
        }
    }
}

@PreviewPantalla
@Composable
private fun PacientesPreview(
    @PreviewParameter(EstadosPreviewProvider::class) estado: EstadoPreview,
) {
    HospitalUnicaesTheme {
        PacientesScreen(estado = estado.con(DatosMuestra.pacientes), onPaciente = {}, onRegistrar = {})
    }
}

@PreviewPantalla
@Composable
private fun RegistroPacientePreview() {
    HospitalUnicaesTheme {
        RegistroPacienteScreen(
            onCerrar = {},
            onGuardar = {},
            onAbrirFichaExistente = {},
            documentoDuplicado = "04512378-9",
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
