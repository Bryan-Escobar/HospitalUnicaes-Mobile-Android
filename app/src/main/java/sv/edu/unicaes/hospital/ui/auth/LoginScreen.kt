package sv.edu.unicaes.hospital.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.core.mensaje
import sv.edu.unicaes.hospital.ui.theme.Espacio
import sv.edu.unicaes.hospital.ui.theme.HospitalUnicaesTheme

@Composable
fun LoginScreen(
    onIngresado: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.ingresado) {
        if (uiState.ingresado) onIngresado()
    }

    LoginContent(
        uiState = uiState,
        onCorreoChange = viewModel::onCorreoChange,
        onContrasenaChange = viewModel::onContrasenaChange,
        onIngresar = viewModel::ingresar,
    )
}

@Composable
private fun LoginContent(
    uiState: LoginUiState,
    onCorreoChange: (String) -> Unit,
    onContrasenaChange: (String) -> Unit,
    onIngresar: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .imePadding()
                    .padding(Espacio.lg),
            verticalArrangement = Arrangement.spacedBy(Espacio.md, Alignment.CenterVertically),
        ) {
            Text(
                text = stringResource(R.string.login_titulo),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.login_subtitulo),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = uiState.correo,
                onValueChange = onCorreoChange,
                label = { Text(stringResource(R.string.login_correo)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.contrasena,
                onValueChange = onContrasenaChange,
                label = { Text(stringResource(R.string.login_contrasena)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onIngresar() }),
                modifier = Modifier.fillMaxWidth(),
            )
            uiState.error?.let { error ->
                Text(
                    text = stringResource(error.mensaje()),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            Button(
                onClick = onIngresar,
                enabled = uiState.puedeIngresar,
                modifier = Modifier.fillMaxWidth().heightIn(min = Espacio.tactilMinimo),
            ) {
                Text(stringResource(if (uiState.cargando) R.string.login_ingresando else R.string.login_ingresar))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginPreview() {
    HospitalUnicaesTheme {
        LoginContent(
            uiState = LoginUiState(correo = "recepcion@hospital.sv"),
            onCorreoChange = {},
            onContrasenaChange = {},
            onIngresar = {},
        )
    }
}
