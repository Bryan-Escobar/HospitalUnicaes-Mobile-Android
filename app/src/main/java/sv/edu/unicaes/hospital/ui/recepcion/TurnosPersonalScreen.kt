@file:OptIn(ExperimentalMaterial3Api::class)

package sv.edu.unicaes.hospital.ui.recepcion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import sv.edu.unicaes.hospital.ui.components.Banner
import sv.edu.unicaes.hospital.ui.components.CampoTexto
import sv.edu.unicaes.hospital.ui.components.ChipsFiltro
import sv.edu.unicaes.hospital.ui.components.ContenidoConEstado
import sv.edu.unicaes.hospital.ui.components.EstadoPreview
import sv.edu.unicaes.hospital.ui.components.EstadoVacio
import sv.edu.unicaes.hospital.ui.components.EstadosPreviewProvider
import sv.edu.unicaes.hospital.ui.components.Etiqueta
import sv.edu.unicaes.hospital.ui.components.FilaLista
import sv.edu.unicaes.hospital.ui.components.IconoAccion
import sv.edu.unicaes.hospital.ui.components.PreviewPantalla
import sv.edu.unicaes.hospital.ui.components.TipoBanner
import sv.edu.unicaes.hospital.ui.components.TituloSeccion
import sv.edu.unicaes.hospital.ui.components.TopBarHospital
import sv.edu.unicaes.hospital.ui.modelo.DatosMuestra
import sv.edu.unicaes.hospital.ui.modelo.DiaTurnosUi
import sv.edu.unicaes.hospital.ui.theme.Espacio
import sv.edu.unicaes.hospital.ui.theme.HospitalUnicaesTheme

/** AND-R12 · Turnos del personal por semana y área. Los solapamientos bloquean Guardar. */
@Composable
fun TurnosPersonalScreen(
    estado: UiState<List<DiaTurnosUi>>,
    onVolver: () -> Unit,
    onReintentar: () -> Unit = {},
    hojaAbierta: Boolean = false,
) {
    var area by rememberSaveable { mutableIntStateOf(1) }
    var mostrarHoja by rememberSaveable { mutableStateOf(hojaAbierta) }

    Scaffold(
        topBar = {
            TopBarHospital(
                titulo = stringResource(R.string.tab_turnos),
                onVolver = onVolver,
                acciones = { IconoAccion(R.drawable.ic_calendar, stringResource(R.string.accion_calendario), {}) },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { mostrarHoja = true },
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(R.string.turnos_nuevo)) },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Espacio.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = {}) { Text("‹") }
                Text(
                    text = stringResource(R.string.turnos_semana),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {}) { Text("›") }
            }
            ChipsFiltro(
                opciones =
                    listOf(
                        stringResource(R.string.turnos_area_todas),
                        "Doctor",
                        "Alergista",
                        "Emergencia",
                        "Enfermería",
                        "Recepción",
                    ),
                seleccionada = area,
                onSeleccion = { area = it },
            )
            ContenidoConEstado(
                estado = estado,
                mensajeError = stringResource(R.string.turnos_error),
                onReintentar = onReintentar,
                vacio = {
                    EstadoVacio(
                        titulo = stringResource(R.string.turnos_vacio_titulo),
                        detalle = stringResource(R.string.turnos_vacio_detalle),
                        icono = R.drawable.ic_schedule,
                        accion = stringResource(R.string.turnos_nuevo),
                        onAccion = { mostrarHoja = true },
                    )
                },
            ) { dias ->
                LazyColumn(contentPadding = PaddingValues(bottom = Espacio.xxl * 2)) {
                    dias.forEach { dia ->
                        item { TituloSeccion(dia.dia, modifier = Modifier.padding(horizontal = Espacio.md)) }
                        items(dia.turnos) { turno ->
                            FilaLista(
                                titulo = turno.persona,
                                soporte = "${turno.area} · ${turno.horario}",
                                final = { Etiqueta(turno.tipo) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (mostrarHoja) {
        HojaNuevoTurno(onCerrar = { mostrarHoja = false })
    }
}

/** Hoja inferior "Nuevo turno" con el aviso de solapamiento de ejemplo. */
@Composable
private fun HojaNuevoTurno(onCerrar: () -> Unit) {
    var persona by rememberSaveable { mutableStateOf("Dra. Sofía Hernández · Doctor") }
    var area by rememberSaveable { mutableStateOf("Consulta externa") }
    var fecha by rememberSaveable { mutableStateOf("Jue 22 oct") }
    var inicio by rememberSaveable { mutableStateOf("12:00") }
    var fin by rememberSaveable { mutableStateOf("20:00") }
    // El backend valida el solapamiento; aquí se muestra la respuesta de ejemplo.
    val seSolapa = inicio == "12:00" && fin == "20:00"

    ModalBottomSheet(onDismissRequest = onCerrar) {
        Column(
            modifier = Modifier.padding(horizontal = Espacio.md).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Espacio.xs),
        ) {
            Text(stringResource(R.string.turnos_nuevo), style = MaterialTheme.typography.titleLarge)
            CampoTexto(stringResource(R.string.campo_persona), persona, { persona = it })
            CampoTexto(stringResource(R.string.campo_area), area, { area = it })
            CampoTexto(stringResource(R.string.campo_fecha), fecha, { fecha = it })
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                CampoTexto(
                    stringResource(R.string.campo_inicio),
                    inicio,
                    { inicio = it },
                    modifier = Modifier.weight(1f),
                )
                CampoTexto(stringResource(R.string.campo_fin), fin, { fin = it }, modifier = Modifier.weight(1f))
            }
            if (seSolapa) Banner(tipo = TipoBanner.AVISO, texto = stringResource(R.string.turnos_solapa))
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = Espacio.sm),
                horizontalArrangement = Arrangement.spacedBy(Espacio.xs, Alignment.End),
            ) {
                TextButton(onClick = onCerrar) { Text(stringResource(R.string.accion_cancelar)) }
                Button(onClick = onCerrar, enabled = !seSolapa) { Text(stringResource(R.string.accion_guardar)) }
            }
        }
    }
}

@PreviewPantalla
@Composable
private fun TurnosPersonalPreview(
    @PreviewParameter(EstadosPreviewProvider::class) estado: EstadoPreview,
) {
    HospitalUnicaesTheme {
        TurnosPersonalScreen(estado = estado.con(DatosMuestra.turnosPersonal), onVolver = {})
    }
}
