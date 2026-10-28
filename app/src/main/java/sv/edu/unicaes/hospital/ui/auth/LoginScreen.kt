package sv.edu.unicaes.hospital.ui.auth

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sv.edu.unicaes.hospital.R
import sv.edu.unicaes.hospital.core.mensaje
import sv.edu.unicaes.hospital.ui.components.BotonPrimario
import sv.edu.unicaes.hospital.ui.components.CampoTexto
import sv.edu.unicaes.hospital.ui.components.MarcaHospital
import sv.edu.unicaes.hospital.ui.components.TamanoMarca
import sv.edu.unicaes.hospital.ui.theme.BordeCampo
import sv.edu.unicaes.hospital.ui.theme.Espacio
import sv.edu.unicaes.hospital.ui.theme.HospitalUnicaesTheme
import sv.edu.unicaes.hospital.ui.theme.Texto1
import sv.edu.unicaes.hospital.ui.theme.Texto2

@Composable
fun LoginScreen(
    onIngresado: (destino: Any) -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.destino) {
        uiState.destino?.let(onIngresado)
    }

    LoginContent(
        uiState = uiState,
        onCorreoChange = viewModel::onCorreoChange,
        onContrasenaChange = viewModel::onContrasenaChange,
        onIngresar = viewModel::ingresar,
    )
}

/** AND-01 · Login: marca centrada, campos con etiqueta arriba y botón píldora. */
@Composable
private fun LoginContent(
    uiState: LoginUiState,
    onCorreoChange: (String) -> Unit,
    onContrasenaChange: (String) -> Unit,
    onIngresar: () -> Unit,
    onOlvideContrasena: () -> Unit = {},
) {
    var verContrasena by rememberSaveable { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(start = Espacio.lg, end = Espacio.lg, top = 56.dp, bottom = Espacio.md),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            MarcaHospital(
                colorTitulo = MaterialTheme.colorScheme.onBackground,
                colorSubtitulo = MaterialTheme.colorScheme.onSurfaceVariant,
                tamano = TamanoMarca.LOGIN,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.md)) {
                CampoTexto(
                    etiqueta = stringResource(R.string.login_correo),
                    valor = uiState.correo,
                    onValor = onCorreoChange,
                    placeholder = stringResource(R.string.login_correo_placeholder),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    tipoContenido = ContentType.EmailAddress,
                )
                if (uiState.sugerenciasCorreo.isNotEmpty()) {
                    SugerenciasCorreo(sugerencias = uiState.sugerenciasCorreo, onElegir = onCorreoChange)
                }
                CampoTexto(
                    etiqueta = stringResource(R.string.login_contrasena),
                    valor = uiState.contrasena,
                    onValor = onContrasenaChange,
                    error = uiState.error?.let { stringResource(it.mensaje()) },
                    visualTransformation =
                        if (verContrasena) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onIngresar() }),
                    tipoContenido = ContentType.Password,
                    final = {
                        IconButton(onClick = { verContrasena = !verContrasena }) {
                            Icon(
                                painter =
                                    painterResource(
                                        if (verContrasena) R.drawable.ic_eye_off else R.drawable.ic_eye,
                                    ),
                                contentDescription =
                                    stringResource(
                                        if (verContrasena) {
                                            R.string.login_ocultar_contrasena
                                        } else {
                                            R.string.login_ver_contrasena
                                        },
                                    ),
                                tint = Texto2,
                            )
                        }
                    },
                )
                Text(
                    text = stringResource(R.string.login_olvide),
                    style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary,
                    modifier =
                        Modifier
                            .align(Alignment.End)
                            .clickable(role = Role.Button, onClick = onOlvideContrasena)
                            .padding(vertical = Espacio.sm),
                )
                BotonPrimario(
                    texto =
                        stringResource(
                            if (uiState.cargando) R.string.login_ingresando else R.string.login_ingresar,
                        ),
                    onClick = onIngresar,
                    habilitado = uiState.puedeIngresar,
                )
            }
            Text(
                text = stringResource(R.string.login_pie),
                style = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Dominios para completar el correo con un toque, debajo del campo. */
@Composable
private fun SugerenciasCorreo(
    sugerencias: List<String>,
    onElegir: (String) -> Unit,
) {
    val forma = MaterialTheme.shapes.medium
    val descripcion = stringResource(R.string.login_sugerencias)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, BordeCampo, forma)
                .semantics { contentDescription = descripcion },
    ) {
        sugerencias.forEach { sugerencia ->
            Text(
                text = sugerencia,
                style = TextStyle(fontSize = 15.sp, lineHeight = 20.sp),
                color = Texto1,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = Espacio.tactilMinimo)
                        .clickable(role = Role.Button) { onElegir(sugerencia) }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun LoginPreview() {
    HospitalUnicaesTheme {
        LoginContent(
            uiState =
                LoginUiState(
                    correo = "lucia",
                    sugerenciasCorreo = listOf("lucia@gmail.com", "lucia@hotmail.com"),
                    contrasena = "12345678",
                ),
            onCorreoChange = {},
            onContrasenaChange = {},
            onIngresar = {},
        )
    }
}
