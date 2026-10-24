package sv.edu.unicaes.hospital.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.core.AppError
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.core.mensaje
import sv.edu.unicaes.hospital.ui.theme.BordeCampo
import sv.edu.unicaes.hospital.ui.theme.BordeSuave
import sv.edu.unicaes.hospital.ui.theme.Espacio
import sv.edu.unicaes.hospital.ui.theme.Primario700
import sv.edu.unicaes.hospital.ui.theme.Superficie2

/**
 * Pinta el estado de una pantalla con datos (handoff §10): skeleton al cargar,
 * vacío con CTA, banner de error con "Reintentar" o el contenido.
 */
@Composable
fun <T> ContenidoConEstado(
    estado: UiState<T>,
    mensajeError: String,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
    vacio: @Composable () -> Unit,
    contenido: @Composable (T) -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (estado) {
            UiState.Cargando -> SkeletonLista()
            UiState.Vacio -> vacio()
            is UiState.Error -> ErrorCarga(mensaje = mensajeError, error = estado.error, onReintentar = onReintentar)
            is UiState.Exito -> contenido(estado.datos)
        }
    }
}

@Composable
fun ErrorCarga(
    mensaje: String,
    error: AppError,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Banner(
        tipo = TipoBanner.ERROR,
        texto = "$mensaje ${stringResource(error.mensaje())}",
        accion = stringResource(R.string.estado_reintentar),
        onAccion = onReintentar,
        modifier = modifier.padding(Espacio.md),
    )
}

/** Estado vacío: icono lineal teal, título, ayuda y CTA opcional dentro de un borde discontinuo. */
@Composable
fun EstadoVacio(
    titulo: String,
    detalle: String,
    modifier: Modifier = Modifier,
    @DrawableRes icono: Int? = null,
    accion: String? = null,
    onAccion: () -> Unit = {},
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(Espacio.md)
                .drawBehind {
                    drawRoundRect(
                        color = BordeCampo,
                        style =
                            Stroke(
                                width = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
                            ),
                        cornerRadius = CornerRadius(16.dp.toPx()),
                    )
                }.padding(Espacio.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Espacio.xs),
    ) {
        if (icono != null) {
            Icon(
                painter = painterResource(icono),
                contentDescription = null,
                tint = Primario700,
                modifier = Modifier.size(56.dp),
            )
        }
        Text(text = titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            text = detalle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (accion != null) {
            Button(onClick = onAccion, modifier = Modifier.padding(top = Espacio.xs)) { Text(accion) }
        }
    }
}

/** Skeleton de lista con avatar: bloques #E2E8F0 y #F1F5F9, radio 6. */
@Composable
fun SkeletonLista(
    modifier: Modifier = Modifier,
    filas: Int = 6,
) {
    val descripcion = stringResource(R.string.estado_cargando)
    Column(
        modifier = modifier.fillMaxWidth().padding(Espacio.md).semantics { contentDescription = descripcion },
        verticalArrangement = Arrangement.spacedBy(Espacio.md),
    ) {
        repeat(filas) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Espacio.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(BordeSuave))
                Column(verticalArrangement = Arrangement.spacedBy(Espacio.xs), modifier = Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.7f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(BordeSuave),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth(0.45f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Superficie2),
                    )
                }
            }
        }
    }
}

// Previews: cada pantalla con datos se revisa en sus cuatro estados con un solo @Preview.

enum class EstadoPreview {
    DATOS,
    VACIO,
    CARGANDO,
    ERROR,
    ;

    fun <T> con(datos: T): UiState<T> =
        when (this) {
            DATOS -> UiState.Exito(datos)
            VACIO -> UiState.Vacio
            CARGANDO -> UiState.Cargando
            ERROR -> UiState.Error(AppError.SinConexion)
        }
}

class EstadosPreviewProvider : PreviewParameterProvider<EstadoPreview> {
    override val values = EstadoPreview.entries.asSequence()
}

/** Frame Android del handoff: 412 × 915, fondo #F8FAFC. */
@Preview(showBackground = true, backgroundColor = 0xFFF8FAFC, widthDp = 412, heightDp = 915)
annotation class PreviewPantalla
