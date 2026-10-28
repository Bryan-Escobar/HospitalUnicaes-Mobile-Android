package sv.edu.unicaes.hospital.ui.sesion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.ui.theme.AmbarBorde
import sv.edu.unicaes.hospital.ui.theme.AmbarFondo
import sv.edu.unicaes.hospital.ui.theme.AmbarTexto
import sv.edu.unicaes.hospital.ui.theme.Espacio
import sv.edu.unicaes.hospital.ui.theme.HospitalUnicaesTheme

/** AND-03 · Paciente y Administrador no usan la app Android (iOS y Web respectivamente). */
@Composable
fun SinAccesoScreen(onCerrarSesion: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = Espacio.xl),
            verticalArrangement = Arrangement.spacedBy(Espacio.md, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Aviso decorativo: el título ya explica el motivo.
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(72.dp)
                        .background(AmbarFondo, CircleShape)
                        .border(2.dp, AmbarBorde, CircleShape),
            ) {
                Text(
                    text = "!",
                    style = TextStyle(fontSize = 32.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
                    color = AmbarTexto,
                )
            }
            Text(
                text = stringResource(R.string.sin_acceso_titulo),
                style = TextStyle(fontSize = 24.sp, lineHeight = 32.sp),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.sin_acceso_detalle),
                style = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            OutlinedButton(
                onClick = onCerrarSesion,
                shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Icon(painterResource(R.drawable.ic_logout), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Espacio.xs))
                Text(
                    text = stringResource(R.string.sesion_cerrar),
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun SinAccesoPreview() {
    HospitalUnicaesTheme { SinAccesoScreen(onCerrarSesion = {}) }
}
