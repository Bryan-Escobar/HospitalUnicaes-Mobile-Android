package sv.edu.unicaes.hospital.ui.splash

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.ui.components.EstadoError
import sv.edu.unicaes.hospital.ui.components.MarcaHospital
import sv.edu.unicaes.hospital.ui.components.TamanoMarca
import sv.edu.unicaes.hospital.ui.theme.Espacio
import sv.edu.unicaes.hospital.ui.theme.HospitalUnicaesTheme
import sv.edu.unicaes.hospital.ui.theme.MarcaDorado
import sv.edu.unicaes.hospital.ui.theme.MarcaGranate
import sv.edu.unicaes.hospital.ui.theme.MarcaRosado

@Composable
fun SplashScreen(
    onDestino: (Any) -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val estado = uiState) {
        SplashUiState.Cargando -> {
            SplashMarca { Restaurando() }
        }

        is SplashUiState.Error -> {
            EstadoError(error = estado.error, onReintentar = viewModel::resolverDestino)
        }

        is SplashUiState.Listo -> {
            SplashMarca { Restaurando() }
            LaunchedEffect(estado.destino) { onDestino(estado.destino) }
        }
    }
}

/** AND-00 · Fondo granate institucional con el logo al centro y la universidad al pie. */
@Composable
private fun SplashMarca(contenido: @Composable ColumnScope.() -> Unit) {
    IconosClarosEnBarraDeEstado()
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MarcaGranate)
                .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = Espacio.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            MarcaHospital(colorTitulo = Color.White, colorSubtitulo = MarcaRosado, tamano = TamanoMarca.SPLASH)
            contenido()
        }
        Text(
            text = stringResource(R.string.marca_universidad),
            style = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
            color = MarcaDorado,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
        )
    }
}

@Composable
private fun Restaurando() {
    LinearProgressIndicator(
        color = MarcaDorado,
        trackColor = Color.White.copy(alpha = 0.2f),
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        modifier = Modifier.width(160.dp).height(4.dp),
    )
    Text(
        text = stringResource(R.string.splash_restaurando),
        style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
        color = MarcaRosado,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/** El fondo granate necesita iconos blancos en la barra de estado; se restauran al salir. */
@Composable
private fun IconosClarosEnBarraDeEstado() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val controlador = (view.context as? Activity)?.window?.let { WindowCompat.getInsetsController(it, view) }
        val anterior = controlador?.isAppearanceLightStatusBars
        controlador?.isAppearanceLightStatusBars = false
        onDispose { anterior?.let { controlador.isAppearanceLightStatusBars = it } }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun SplashPreview() {
    HospitalUnicaesTheme { SplashMarca { Restaurando() } }
}
