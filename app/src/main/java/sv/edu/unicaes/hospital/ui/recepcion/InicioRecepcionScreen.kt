package sv.edu.unicaes.hospital.ui.recepcion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.domain.model.EstadoLaboratorio
import sv.edu.unicaes.hospital.ui.components.BadgeEstado
import sv.edu.unicaes.hospital.ui.components.BadgeLaboratorio
import sv.edu.unicaes.hospital.ui.components.ContenidoConEstado
import sv.edu.unicaes.hospital.ui.components.EstadoPreview
import sv.edu.unicaes.hospital.ui.components.EstadoVacio
import sv.edu.unicaes.hospital.ui.components.EstadosPreviewProvider
import sv.edu.unicaes.hospital.ui.components.FilaCita
import sv.edu.unicaes.hospital.ui.components.IconoAccion
import sv.edu.unicaes.hospital.ui.components.PreviewPantalla
import sv.edu.unicaes.hospital.ui.components.TarjetaBandeja
import sv.edu.unicaes.hospital.ui.components.TarjetaLista
import sv.edu.unicaes.hospital.ui.components.TarjetaOcupacion
import sv.edu.unicaes.hospital.ui.components.TarjetaTurno
import sv.edu.unicaes.hospital.ui.components.TituloSeccion
import sv.edu.unicaes.hospital.ui.components.TopBarHospital
import sv.edu.unicaes.hospital.ui.modelo.DatosMuestra
import sv.edu.unicaes.hospital.ui.modelo.InicioRecepcionUi
import sv.edu.unicaes.hospital.ui.theme.Espacio
import sv.edu.unicaes.hospital.ui.theme.HospitalUnicaesTheme
import sv.edu.unicaes.hospital.ui.theme.ParColor

/** AND-R1 · Inicio (bandeja). La solicitud de ambulancia siempre va arriba, con borde rojo. */
@Composable
fun InicioRecepcionScreen(
    estado: UiState<InicioRecepcionUi>,
    onAmbulancia: () -> Unit,
    onLaboratorio: () -> Unit,
    onCitas: () -> Unit,
    onReintentar: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopBarHospital(
                titulo = stringResource(R.string.tab_inicio),
                enVivo = true,
                acciones = {
                    IconoAccion(
                        icono = R.drawable.ic_notifications,
                        descripcion = stringResource(R.string.accion_notificaciones),
                        onClick = {},
                    )
                },
            )
        },
    ) { padding ->
        ContenidoConEstado(
            estado = estado,
            mensajeError = stringResource(R.string.inicio_error),
            onReintentar = onReintentar,
            modifier = Modifier.padding(padding),
            vacio = {
                EstadoVacio(
                    titulo = stringResource(R.string.inicio_vacio_titulo),
                    detalle = stringResource(R.string.inicio_vacio_detalle),
                    icono = R.drawable.ic_home,
                )
            },
        ) { datos ->
            LazyColumn(
                contentPadding =
                    PaddingValues(
                        start = Espacio.md,
                        end = Espacio.md,
                        top = Espacio.xs,
                        bottom = Espacio.md,
                    ),
                verticalArrangement = Arrangement.spacedBy(Espacio.sm),
            ) {
                item { TarjetaTurno(titulo = stringResource(R.string.inicio_mi_turno), turno = datos.turno) }
                item { TituloSeccion(stringResource(R.string.inicio_bandeja)) }
                datos.solicitudAmbulancia?.let { solicitud ->
                    item {
                        TarjetaBandeja(
                            titulo = stringResource(R.string.inicio_solicitud_ambulancia),
                            detalle = "${solicitud.hace.replaceFirstChar { it.uppercase() }} · ${solicitud.tipo}",
                            icono = R.drawable.ic_ambulancia,
                            onClick = onAmbulancia,
                            prioritaria = true,
                        ) {
                            val error = MaterialTheme.colorScheme.error
                            BadgeEstado(
                                texto = stringResource(R.string.inicio_una_nueva),
                                colores = ParColor(texto = Color.White, fondo = error, borde = error),
                                glifo = "●",
                            )
                        }
                    }
                }
                item {
                    TarjetaBandeja(
                        titulo = stringResource(R.string.mas_laboratorio),
                        detalle = stringResource(R.string.inicio_ordenes_por_atender),
                        icono = R.drawable.ic_laboratorio,
                        onClick = onLaboratorio,
                    ) { BadgeLaboratorio(EstadoLaboratorio.PENDIENTE, conteo = datos.laboratorioPendiente) }
                }
                item { TituloSeccion(stringResource(R.string.inicio_ocupacion)) }
                item { TarjetaOcupacion(datos.ocupacion) }
                item { TituloSeccion(texto = stringResource(R.string.inicio_citas_hoy, datos.citasHoyTotal)) }
                item {
                    TarjetaLista(datos.citasHoy) { cita, ultima ->
                        FilaCita(cita = cita, mostrarDoctor = true, divisor = !ultima, onClick = onCitas)
                    }
                }
            }
        }
    }
}

@PreviewPantalla
@Composable
private fun InicioRecepcionPreview(
    @PreviewParameter(EstadosPreviewProvider::class) estado: EstadoPreview,
) {
    HospitalUnicaesTheme {
        InicioRecepcionScreen(
            estado = estado.con(DatosMuestra.inicioRecepcion),
            onAmbulancia = {},
            onLaboratorio = {},
            onCitas = {},
        )
    }
}
