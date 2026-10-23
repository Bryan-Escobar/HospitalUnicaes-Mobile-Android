package sv.edu.unicaes.hospital.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.core.AppError
import sv.edu.unicaes.hospital.core.mensaje
import sv.edu.unicaes.hospital.ui.theme.Espacio

@Composable
fun EstadoCargando(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.estado_cargando),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
                Modifier
                    .padding(top = Espacio.sm)
                    .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Composable
fun EstadoError(
    error: AppError,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EstadoMensaje(
        titulo = stringResource(error.mensaje()),
        accion = stringResource(R.string.estado_reintentar),
        onAccion = onReintentar,
        modifier = modifier,
    )
}

/** Mensaje centrado con acción opcional: vacío, error, sin permiso. */
@Composable
fun EstadoMensaje(
    titulo: String,
    modifier: Modifier = Modifier,
    detalle: String? = null,
    accion: String? = null,
    onAccion: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize().padding(Espacio.lg),
        verticalArrangement = Arrangement.spacedBy(Espacio.xs, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (detalle != null) {
            Text(
                text = detalle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (accion != null) {
            Button(
                onClick = onAccion,
                modifier = Modifier.padding(top = Espacio.xs).heightIn(min = Espacio.tactilMinimo),
            ) { Text(accion) }
        }
    }
}

@Composable
fun PantallaEnConstruccion(
    titulo: String,
    modifier: Modifier = Modifier,
) {
    EstadoMensaje(
        titulo = titulo,
        detalle = stringResource(R.string.estado_en_construccion),
        modifier = modifier,
    )
}
