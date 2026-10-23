package sv.edu.unicaes.hospital.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

// Sin color dinámico (Android 12+): la paleta del hospital debe verse igual en todos los equipos.
// El sistema de diseño solo define modo claro por ahora.
private val LightColorScheme =
    lightColorScheme(
        primary = Primario700,
        onPrimary = Superficie,
        primaryContainer = Primario100,
        onPrimaryContainer = Primario900,
        secondary = Primario800,
        onSecondary = Superficie,
        secondaryContainer = Primario50,
        onSecondaryContainer = Primario900,
        tertiary = AzulTexto,
        onTertiary = Superficie,
        tertiaryContainer = AzulFondo,
        onTertiaryContainer = AzulTexto,
        background = Fondo,
        onBackground = Texto1,
        surface = Superficie,
        onSurface = Texto1,
        surfaceVariant = Superficie2,
        onSurfaceVariant = Texto2,
        surfaceContainerLowest = Superficie,
        surfaceContainerLow = Fondo,
        surfaceContainer = Superficie2,
        surfaceContainerHigh = Superficie2,
        surfaceContainerHighest = BordeSuave,
        outline = BordeCampo,
        outlineVariant = Borde,
        error = Error,
        onError = Superficie,
        errorContainer = ErrorFondo,
        onErrorContainer = RojoTexto,
    )

@Composable
fun HospitalUnicaesTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalEstadoColors provides EstadoColors()) {
        MaterialTheme(
            colorScheme = LightColorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content,
        )
    }
}

/** Acceso a los tokens propios del hospital que Material 3 no cubre. */
object HospitalTheme {
    val estados: EstadoColors
        @Composable
        @ReadOnlyComposable
        get() = LocalEstadoColors.current
}
