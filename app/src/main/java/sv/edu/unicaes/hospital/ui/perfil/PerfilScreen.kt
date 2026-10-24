package sv.edu.unicaes.hospital.ui.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.ui.components.Avatar
import sv.edu.unicaes.hospital.ui.components.BotonPrimario
import sv.edu.unicaes.hospital.ui.components.DialogoConfirmacion
import sv.edu.unicaes.hospital.ui.components.FilaLista
import sv.edu.unicaes.hospital.ui.components.PreviewPantalla
import sv.edu.unicaes.hospital.ui.components.TopBarHospital
import sv.edu.unicaes.hospital.ui.components.iniciales
import sv.edu.unicaes.hospital.ui.modelo.DatosMuestra
import sv.edu.unicaes.hospital.ui.modelo.PerfilUi
import sv.edu.unicaes.hospital.ui.theme.Espacio
import sv.edu.unicaes.hospital.ui.theme.HospitalUnicaesTheme

/** Perfil (igual a iOS-C7): datos de la cuenta, rol y tipo, y cerrar sesión con confirmación. */
@Composable
fun PerfilScreen(
    perfil: PerfilUi,
    onCerrarSesion: () -> Unit,
    onVolver: (() -> Unit)? = null,
) {
    var confirmar by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = { TopBarHospital(titulo = stringResource(R.string.tab_perfil), onVolver = onVolver) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row(
                modifier = Modifier.padding(Espacio.md),
                horizontalArrangement = Arrangement.spacedBy(Espacio.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(iniciales(perfil.nombre), tamano = 56.dp)
                Column {
                    Text(perfil.nombre, style = MaterialTheme.typography.headlineSmall)
                    Text(perfil.rolYTipo, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            FilaLista(titulo = stringResource(R.string.login_correo), soporte = perfil.correo)
            FilaLista(titulo = stringResource(R.string.campo_telefono_simple), soporte = perfil.telefono)
            FilaLista(titulo = stringResource(R.string.perfil_rol_tipo), soporte = perfil.rolYTipo)
            BotonPrimario(
                texto = stringResource(R.string.sesion_cerrar),
                onClick = { confirmar = true },
                destructivo = true,
                modifier = Modifier.padding(Espacio.md),
            )
        }
    }

    if (confirmar) {
        DialogoConfirmacion(
            titulo = stringResource(R.string.perfil_cerrar_titulo),
            texto = stringResource(R.string.perfil_cerrar_texto),
            confirmar = stringResource(R.string.sesion_cerrar),
            onConfirmar = {
                confirmar = false
                onCerrarSesion()
            },
            onCancelar = { confirmar = false },
            destructivo = true,
        )
    }
}

@PreviewPantalla
@Composable
private fun PerfilPreview() {
    HospitalUnicaesTheme { PerfilScreen(perfil = DatosMuestra.perfilDoctor, onCerrarSesion = {}) }
}
