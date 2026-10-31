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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.core.UiState
import sv.edu.unicaes.hospital.domain.model.Rol
import sv.edu.unicaes.hospital.domain.model.Usuario
import sv.edu.unicaes.hospital.ui.components.Avatar
import sv.edu.unicaes.hospital.ui.components.BotonPrimario
import sv.edu.unicaes.hospital.ui.components.DialogoConfirmacion
import sv.edu.unicaes.hospital.ui.components.EstadoCargando
import sv.edu.unicaes.hospital.ui.components.EstadoError
import sv.edu.unicaes.hospital.ui.components.FilaLista
import sv.edu.unicaes.hospital.ui.components.PreviewPantalla
import sv.edu.unicaes.hospital.ui.components.TopBarHospital
import sv.edu.unicaes.hospital.ui.components.iniciales
import sv.edu.unicaes.hospital.ui.modelo.DatosMuestra
import sv.edu.unicaes.hospital.ui.modelo.PerfilUi
import sv.edu.unicaes.hospital.ui.theme.Espacio
import sv.edu.unicaes.hospital.ui.theme.HospitalUnicaesTheme

/** Perfil (igual a iOS-C7) de la cuenta de la sesión, cargado de GET /auth/me. */
@Composable
fun PerfilScreen(
    onCerrarSesion: () -> Unit,
    onVolver: (() -> Unit)? = null,
    viewModel: PerfilViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val estado = uiState) {
        is UiState.Exito -> {
            PerfilContenido(perfil = estado.datos.aPerfilUi(), onCerrarSesion = onCerrarSesion, onVolver = onVolver)
        }

        else -> {
            Scaffold(
                topBar = { TopBarHospital(titulo = stringResource(R.string.tab_perfil), onVolver = onVolver) },
            ) { padding ->
                val modifier = Modifier.padding(padding)
                if (estado is UiState.Error) {
                    EstadoError(error = estado.error, onReintentar = viewModel::cargar, modifier = modifier)
                } else {
                    EstadoCargando(modifier = modifier)
                }
            }
        }
    }
}

/** "Doctor · Medicina interna": el rol y, si dice algo más, la especialidad o el tipo de personal. */
@Composable
private fun Usuario.aPerfilUi(): PerfilUi {
    val rolTexto =
        stringResource(
            when {
                rol == Rol.RECEPCIONISTA -> R.string.rol_recepcion
                rol == Rol.DOCTOR -> R.string.rol_doctor
                rol == Rol.ADMINISTRADOR -> R.string.rol_administrador
                else -> R.string.rol_paciente
            },
        )
    val detalle = (especialidad ?: tipoPersonal)?.takeUnless { it.equals(rolTexto, ignoreCase = true) }
    val sinRegistrar = stringResource(R.string.perfil_sin_registrar)
    return PerfilUi(
        nombre = nombre,
        rolYTipo = listOfNotNull(rolTexto, detalle).joinToString(" · "),
        correo = correo ?: sinRegistrar,
        telefono = telefono ?: sinRegistrar,
    )
}

/** Datos de la cuenta, rol y tipo, y cerrar sesión con confirmación. */
@Composable
private fun PerfilContenido(
    perfil: PerfilUi,
    onCerrarSesion: () -> Unit,
    onVolver: (() -> Unit)?,
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
    HospitalUnicaesTheme { PerfilContenido(perfil = DatosMuestra.perfilDoctor, onCerrarSesion = {}, onVolver = null) }
}
