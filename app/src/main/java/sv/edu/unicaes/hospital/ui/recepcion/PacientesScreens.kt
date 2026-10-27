package sv.edu.unicaes.hospital.ui.recepcion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.core.UiState
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
import sv.edu.unicaes.hospital.ui.components.ChipsFiltro
import sv.edu.unicaes.hospital.ui.components.ContenidoConEstado
import sv.edu.unicaes.hospital.ui.components.EstadoPreview
import sv.edu.unicaes.hospital.ui.components.EstadoVacio
import sv.edu.unicaes.hospital.ui.components.EstadosPreviewProvider
import sv.edu.unicaes.hospital.ui.components.FabHospital
import sv.edu.unicaes.hospital.ui.components.FilaLista
import sv.edu.unicaes.hospital.ui.components.IconoAccion
import sv.edu.unicaes.hospital.ui.components.PreviewPantalla
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
                onValor = { busqueda = it },
                placeholder = stringResource(R.string.pacientes_buscar),
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
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    item {
                        TarjetaLista(visibles) { paciente, ultimo ->
                            FilaPaciente(paciente = paciente, divisor = !ultimo, onClick = { onPaciente(paciente) })
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
                acciones = {
                    AccionTexto(
                        texto = stringResource(R.string.accion_guardar),
                        onClick = onGuardar,
                        modifier = Modifier.padding(horizontal = Espacio.sm),
                    )
                },
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
                    .padding(RellenoCuerpo),
            verticalArrangement = Arrangement.spacedBy(Espacio.sm),
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
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.sm)) {
                CampoTexto(
                    stringResource(R.string.campo_nacimiento),
                    nacimiento,
                    { nacimiento = it },
                    placeholder = "dd/mm/aaaa",
                    modifier = Modifier.weight(1f),
                )
                CampoTexto(
                    stringResource(R.string.campo_telefono),
                    telefono,
                    { telefono = it },
                    placeholder = "0000-0000",
                    modifier = Modifier.weight(1f),
                )
            }
            CampoTexto(
                etiqueta = stringResource(R.string.campo_sexo),
                valor = sexo,
                onValor = { sexo = it },
                placeholder = stringResource(R.string.campo_seleccionar),
                final = { IconoDesplegable() },
            )
            CampoTexto(
                etiqueta = stringResource(R.string.campo_correo_opcional),
                valor = correo,
                onValor = { correo = it },
                placeholder = stringResource(R.string.pacientes_correo_ayuda),
            )
            CampoTexto(
                etiqueta = stringResource(R.string.campo_direccion),
                valor = direccion,
                onValor = { direccion = it },
                placeholder = stringResource(R.string.pacientes_direccion_placeholder),
            )
            TituloSeccion(stringResource(R.string.campo_contacto_emergencia))
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.sm)) {
                CampoTexto(
                    stringResource(R.string.campo_nombre),
                    contactoNombre,
                    { contactoNombre = it },
                    placeholder = stringResource(R.string.campo_nombre),
                    modifier = Modifier.weight(1f),
                )
                CampoTexto(
                    stringResource(R.string.campo_telefono_simple),
                    contactoTelefono,
                    { contactoTelefono = it },
                    placeholder = "0000-0000",
                    modifier = Modifier.weight(1f),
                )
            }
            CampoTexto(
                etiqueta = stringResource(R.string.campo_alergias),
                valor = alergias,
                onValor = { alergias = it },
                placeholder = stringResource(R.string.campo_ninguna),
            )
        }
    }
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
                            stringResource(
                                R.string.ficha_cabecera,
                                ficha.paciente.dui,
                                ficha.edad,
                                ficha.telefono,
                            ),
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
